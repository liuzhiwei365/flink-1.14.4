/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.functions.sink;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.state.CheckpointListener;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.CompositeTypeSerializerConfigSnapshot;
import org.apache.flink.api.common.typeutils.CompositeTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.CompositeTypeSerializerUtil;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.TypeSerializerConfigSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializerSchemaCompatibility;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.util.FlinkRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.time.Clock;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * This is a recommended base class for all of the {@link SinkFunction} that intend to implement
 * exactly-once semantic. It does that by implementing two phase commit algorithm on top of the
 * {@link CheckpointedFunction} and {@link CheckpointListener}. User should provide custom {@code
 * TXN} (transaction handle) and implement abstract methods handling this transaction handle.
 *
 * @param <IN> Input type for {@link SinkFunction}.
 * @param <TXN> Transaction to store all of the information required to handle a transaction.
 * @param <CONTEXT> Context that will be shared across all invocations for the given {@link
 *     TwoPhaseCommitSinkFunction} instance. Context is created once
 */
@PublicEvolving
public abstract class TwoPhaseCommitSinkFunction<IN, TXN, CONTEXT> extends RichSinkFunction<IN>
        implements CheckpointedFunction, CheckpointListener {

    private static final Logger LOG = LoggerFactory.getLogger(TwoPhaseCommitSinkFunction.class);

    protected final LinkedHashMap<Long, TransactionHolder<TXN>> pendingCommitTransactions =
            new LinkedHashMap<>();

    protected transient Optional<CONTEXT> userContext;

    protected transient ListState<State<TXN, CONTEXT>> state;

    private final Clock clock;

    private final ListStateDescriptor<State<TXN, CONTEXT>> stateDescriptor;

    /**
     * Current Transaction Holder, including two states: 1. Normal Transaction: created when a new
     * snapshot is taken during normal task running 2. null: After task/function is finished.
     */
    @Nullable private TransactionHolder<TXN> currentTransactionHolder;

    /** Specifies the maximum time a transaction should remain open. */
    private long transactionTimeout = Long.MAX_VALUE;

    /**
     * If true, any exception thrown in {@link #recoverAndCommit(Object)} will be caught instead of
     * propagated.
     */
    private boolean ignoreFailuresAfterTransactionTimeout;

    /**
     * If a transaction's elapsed time reaches this percentage of the transactionTimeout, a warning
     * message will be logged. Value must be in range [0,1]. Negative value disables warnings.
     */
    private double transactionTimeoutWarningRatio = -1;

    /** Whether this sink function as well as its task is finished. */
    private boolean finished = false;

    /**
     * Use default {@link ListStateDescriptor} for internal state serialization. Helpful utilities
     * for using this constructor are {@link TypeInformation#of(Class)}, {@link
     * org.apache.flink.api.common.typeinfo.TypeHint} and {@link TypeInformation#of(TypeHint)}.
     * Example:
     *
     * <pre>{@code
     * TwoPhaseCommitSinkFunction(TypeInformation.of(new TypeHint<State<TXN, CONTEXT>>() {}));
     * }</pre>
     *
     * @param transactionSerializer {@link TypeSerializer} for the transaction type of this sink
     * @param contextSerializer {@link TypeSerializer} for the context type of this sink
     */
    public TwoPhaseCommitSinkFunction(
            TypeSerializer<TXN> transactionSerializer, TypeSerializer<CONTEXT> contextSerializer) {
        this(transactionSerializer, contextSerializer, Clock.systemUTC());
    }

    @VisibleForTesting
    TwoPhaseCommitSinkFunction(
            TypeSerializer<TXN> transactionSerializer,
            TypeSerializer<CONTEXT> contextSerializer,
            Clock clock) {
        this.stateDescriptor =
                new ListStateDescriptor<>(
                        "state", new StateSerializer<>(transactionSerializer, contextSerializer));
        this.clock = clock;
    }

    protected Optional<CONTEXT> initializeUserContext() {
        return Optional.empty();
    }

    protected Optional<CONTEXT> getUserContext() {
        return userContext;
    }

    @Nullable
    protected TXN currentTransaction() {
        return currentTransactionHolder == null ? null : currentTransactionHolder.handle;
    }

    @Nonnull
    protected Stream<Map.Entry<Long, TXN>> pendingTransactions() {
        return pendingCommitTransactions.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().handle));
    }

    // ------ methods that should be implemented in child class to support two phase commit
    // algorithm ------

    /** Write value within a transaction. */
    protected abstract void invoke(TXN transaction, IN value, Context context) throws Exception;

    /**
     * Method that starts a new transaction.
     *
     * @return newly created transaction.
     */
    protected abstract TXN beginTransaction() throws Exception;

    /**
     * Pre commit previously created transaction. Pre commit must make all of the necessary steps to
     * prepare the transaction for a commit that might happen in the future. After this point the
     * transaction might still be aborted, but underlying implementation must ensure that commit
     * calls on already pre committed transactions will always succeed.
     *
     * <p>Usually implementation involves flushing the data.
     */
    protected abstract void preCommit(TXN transaction) throws Exception;

    /**
     * Commit a pre-committed transaction. If this method fail, Flink application will be restarted
     * and {@link TwoPhaseCommitSinkFunction#recoverAndCommit(Object)} will be called again for the
     * same transaction.
     */
    protected abstract void commit(TXN transaction);

    /**
     * Invoked on recovered transactions after a failure. User implementation must ensure that this
     * call will eventually succeed. If it fails, Flink application will be restarted and it will be
     * invoked again. If it does not succeed eventually, a data loss will occur. Transactions will
     * be recovered in an order in which they were created.
     */
    protected void recoverAndCommit(TXN transaction) {
        commit(transaction);
    }

    /** Abort a transaction. */
    protected abstract void abort(TXN transaction);

    /** Abort a transaction that was rejected by a coordinator after a failure. */
    protected void recoverAndAbort(TXN transaction) {
        abort(transaction);
    }

    /**
     * Callback for subclasses which is called after restoring (each) user context.
     *
     * @param handledTransactions transactions which were already committed or aborted and do not
     *     need further handling
     */
    protected void finishRecoveringContext(Collection<TXN> handledTransactions) {}

    /**
     * This method is called at the end of data processing.
     *
     * <p>The method is expected to flush all remaining buffered data. Exceptions will cause the
     * pipeline to be recognized as failed, because the last data items are not processed properly.
     * You may use this method to flush remaining buffered elements in the state into the current
     * transaction which will be committed in the last checkpoint.
     */
    protected void finishProcessing(@Nullable TXN transaction) {}

    // ------ entry points for above methods implementing {@CheckPointedFunction} and
    // {@CheckpointListener} ------

    /** This should not be implemented by subclasses. */
    @Override
    public final void invoke(IN value) throws Exception {}

    @Override
    public final void invoke(IN value, Context context) throws Exception {
        TXN currentTransaction = currentTransaction();
        checkNotNull(
                currentTransaction,
                "Two phase commit sink function with null transaction should not be invoked! ");
        invoke(currentTransaction, value, context);
    }

    @Override
    public final void finish() throws Exception {
        finished = true;
        finishProcessing(currentTransaction());
    }

    @Override
    public final void notifyCheckpointComplete(long checkpointId) throws Exception {


        /*
           以下情况在这里是可能的

          (1) 从最近的检查点正好有一个事务已触发并完成。这应该是常见的情况。在这种情况下,只需提交该事务即可。

          (2) 存在多个待提交的事务,比如
              master无法持久化checkpoint（由于存储系统中的临时中断），但是我们可以仍然会进行下一次checkpoint
              某个task不能持久化某次checkpoint , 但是还没有触发 failure, 这时有可能又开始下一次checkpoint

          (3) 多个事务处于挂起状态，但checkpoint完成通知与最新情况无关。这是可能的，因为通知消息可以延迟（在极端情况下，
             直到到达下一个checkpoint之后被触发）,并且因为可能同时存在重叠的checkpoint（在上一个完全完成之前启动一个新的）

         */
        // 获取所有待提交的事务
        Iterator<Map.Entry<Long, TransactionHolder<TXN>>> pendingTransactionIterator =
                pendingCommitTransactions.entrySet().iterator();
        Throwable firstError = null;

        while (pendingTransactionIterator.hasNext()) {
            Map.Entry<Long, TransactionHolder<TXN>> entry = pendingTransactionIterator.next();
            Long pendingTransactionCheckpointId = entry.getKey();
            TransactionHolder<TXN> pendingTransaction = entry.getValue();
            // 只提交在checkpointId之前的事务
            if (pendingTransactionCheckpointId > checkpointId) {
                continue;
            }

            LOG.info(
                    "{} - checkpoint {} complete, committing transaction {} from checkpoint {}",
                    name(),
                    checkpointId,
                    pendingTransaction,
                    pendingTransactionCheckpointId);

            logWarningIfTimeoutAlmostReached(pendingTransaction);
            try {
                // 逐个提交之前preCommit过的事务 , 目前只有kafka的实现
                //               FlinkKafkaProducer.commit
                //               FlinkKafkaInternalProducer.commitTransaction
                //               KafkaProducer.commitTransaction
                commit(pendingTransaction.handle);
            } catch (Throwable t) {
                if (firstError == null) {
                    firstError = t;
                }
            }

            LOG.debug("{} - committed checkpoint transaction {}", name(), pendingTransaction);

            // 将提交过的事务从待提交事务列表中清除
            pendingTransactionIterator.remove();
        }

        if (firstError != null) {
            throw new FlinkRuntimeException(
                    "Committing one of transactions failed, logging first encountered failure",
                    firstError);
        }
    }

    @Override
    public void notifyCheckpointAborted(long checkpointId) {}

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        long checkpointId = context.getCheckpointId();
        LOG.debug(
                "{} - checkpoint {} triggered, flushing transaction '{}'",
                name(),
                context.getCheckpointId(),
                currentTransactionHolder);

        if (currentTransactionHolder != null) {
            // 核心, 预提交
            //    目前只有kafka的实现, FlinkKafkaProducer.preCommit
            //                         FlinkKafkaProducer.flush
            //                           KafkaProducer.flush
            preCommit(currentTransactionHolder.handle);
            // 在未提交,但是已经与提交的 事务列表(pendingCommitTransactions)中记录该事务
            pendingCommitTransactions.put(checkpointId, currentTransactionHolder);
            LOG.debug("{} - stored pending transactions {}", name(), pendingCommitTransactions);
        }

        if (!finished) {
            // 开启新的事务
            currentTransactionHolder = beginTransactionInternal();
        } else {
            currentTransactionHolder = null;
        }
        LOG.debug("{} - started new transaction '{}'", name(), currentTransactionHolder);

        // 清空state，然后记录当前事务 和 未提但是已经预提交的 事务
        state.clear();
        state.add(
                new State<>(
                        this.currentTransactionHolder,
                        new ArrayList<>(pendingCommitTransactions.values()),
                        userContext));
    }

    /*
      1  获取状态存储变量state。
      2  提交所有已经执行过preCommit的事务。
      3  终止所有尚未preCommit的事务。
      4  创建一个新事务。
     */
    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {

        // step1 获取状态存储变量state
        state = context.getOperatorStateStore().getListState(stateDescriptor);

        boolean recoveredUserContext = false;
        if (context.isRestored()) {
            LOG.info("{} - restoring state", name());
            for (State<TXN, CONTEXT> operatorState : state.get()) {
                userContext = operatorState.getContext();
                List<TransactionHolder<TXN>> recoveredTransactions =
                        operatorState.getPendingCommitTransactions();
                List<TXN> handledTransactions = new ArrayList<>(recoveredTransactions.size() + 1);

                // step2 提交所有已经执行过preCommit的事务
                for (TransactionHolder<TXN> recoveredTransaction : recoveredTransactions) {
                    // If this fails to succeed eventually, there is actually data loss
                    recoverAndCommitInternal(recoveredTransaction);
                    handledTransactions.add(recoveredTransaction.handle);
                    LOG.info("{} committed recovered transaction {}", name(), recoveredTransaction);
                }

                {
                    /*
                       代码块说明：
                        局部代码块：用于限定变量生命周期，及早释放，提高内存利用率。
                        静态代码块：主要用于对静态属性进行初始化。
                        实例（构造）代码块：调用构造方法都会执行，并且在构造方法前执行。
                        同步代码块：一种多线程保护机制。

                        这里是局部代码块
                     */
                    // step3 终止所有尚未preCommit的事务
                    if (operatorState.getPendingTransaction() != null) {
                        TXN transaction = operatorState.getPendingTransaction().handle;
                        recoverAndAbort(transaction);
                        handledTransactions.add(transaction);
                        LOG.info(
                                "{} aborted recovered transaction {}",
                                name(),
                                operatorState.getPendingTransaction());
                    }
                }

                if (userContext.isPresent()) {
                    finishRecoveringContext(handledTransactions);
                    recoveredUserContext = true;
                }
            }
        }

        // if in restore we didn't get any userContext or we are initializing from scratch
        if (!recoveredUserContext) {
            LOG.info("{} - no state to restore", name());
            userContext = initializeUserContext();
        }
        this.pendingCommitTransactions.clear();

        // step4 创建一个新事务
        currentTransactionHolder = beginTransactionInternal();
        LOG.debug("{} - started new transaction '{}'", name(), currentTransactionHolder);
    }

    /**
     * This method must be the only place to call {@link #beginTransaction()} to ensure that the
     * {@link TransactionHolder} is created at the same time.
     */
    private TransactionHolder<TXN> beginTransactionInternal() throws Exception {
        return new TransactionHolder<>(beginTransaction(), clock.millis());
    }

    /**
     * This method must be the only place to call {@link #recoverAndCommit(Object)} to ensure that
     * the configuration parameters {@link #transactionTimeout} and {@link
     * #ignoreFailuresAfterTransactionTimeout} are respected.
     */
    private void recoverAndCommitInternal(TransactionHolder<TXN> transactionHolder) {
        try {
            logWarningIfTimeoutAlmostReached(transactionHolder);
            recoverAndCommit(transactionHolder.handle);
        } catch (final Exception e) {
            final long elapsedTime = clock.millis() - transactionHolder.transactionStartTime;
            if (ignoreFailuresAfterTransactionTimeout && elapsedTime > transactionTimeout) {
                LOG.error(
                        "Error while committing transaction {}. "
                                + "Transaction has been open for longer than the transaction timeout ({})."
                                + "Commit will not be attempted again. Data loss might have occurred.",
                        transactionHolder.handle,
                        transactionTimeout,
                        e);
            } else {
                throw e;
            }
        }
    }

    private void logWarningIfTimeoutAlmostReached(TransactionHolder<TXN> transactionHolder) {
        final long elapsedTime = transactionHolder.elapsedTime(clock);
        if (transactionTimeoutWarningRatio >= 0
                && elapsedTime > transactionTimeout * transactionTimeoutWarningRatio) {
            LOG.warn(
                    "Transaction {} has been open for {} ms. "
                            + "This is close to or even exceeding the transaction timeout of {} ms.",
                    transactionHolder.handle,
                    elapsedTime,
                    transactionTimeout);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();

        TXN currentTransaction = currentTransaction();
        if (currentTransaction != null) {
            abort(currentTransaction);
        }

        currentTransactionHolder = null;
    }

    /**
     * Sets the transaction timeout. Setting only the transaction timeout has no effect in itself.
     *
     * @param transactionTimeout The transaction timeout in ms.
     * @see #ignoreFailuresAfterTransactionTimeout()
     * @see #enableTransactionTimeoutWarnings(double)
     */
    protected TwoPhaseCommitSinkFunction<IN, TXN, CONTEXT> setTransactionTimeout(
            long transactionTimeout) {
        checkArgument(transactionTimeout >= 0, "transactionTimeout must not be negative");
        this.transactionTimeout = transactionTimeout;
        return this;
    }

    /**
     * If called, the sink will only log but not propagate exceptions thrown in {@link
     * #recoverAndCommit(Object)} if the transaction is older than a specified transaction timeout.
     * The start time of an transaction is determined by {@link System#currentTimeMillis()}. By
     * default, failures are propagated.
     */
    protected TwoPhaseCommitSinkFunction<IN, TXN, CONTEXT> ignoreFailuresAfterTransactionTimeout() {
        this.ignoreFailuresAfterTransactionTimeout = true;
        return this;
    }

    /**
     * Enables logging of warnings if a transaction's elapsed time reaches a specified ratio of the
     * <code>transactionTimeout</code>. If <code>warningRatio</code> is 0, a warning will be always
     * logged when committing the transaction.
     *
     * @param warningRatio A value in the range [0,1].
     * @return
     */
    protected TwoPhaseCommitSinkFunction<IN, TXN, CONTEXT> enableTransactionTimeoutWarnings(
            double warningRatio) {
        checkArgument(
                warningRatio >= 0 && warningRatio <= 1, "warningRatio must be in range [0,1]");
        this.transactionTimeoutWarningRatio = warningRatio;
        return this;
    }

    private String name() {
        return String.format(
                "%s %s/%s",
                this.getClass().getSimpleName(),
                getRuntimeContext().getIndexOfThisSubtask() + 1,
                getRuntimeContext().getNumberOfParallelSubtasks());
    }

    /** State POJO class coupling pendingTransaction, context and pendingCommitTransactions. */
    @VisibleForTesting
    @Internal
    public static final class State<TXN, CONTEXT> {
        @Nullable protected TransactionHolder<TXN> pendingTransaction;
        protected List<TransactionHolder<TXN>> pendingCommitTransactions = new ArrayList<>();
        protected Optional<CONTEXT> context;

        public State() {}

        public State(
                @Nullable TransactionHolder<TXN> pendingTransaction,
                List<TransactionHolder<TXN>> pendingCommitTransactions,
                Optional<CONTEXT> context) {
            this.pendingTransaction = pendingTransaction;
            this.pendingCommitTransactions =
                    requireNonNull(pendingCommitTransactions, "pendingCommitTransactions is null");
            this.context = requireNonNull(context, "context is null");
        }

        @Nullable
        public TransactionHolder<TXN> getPendingTransaction() {
            return pendingTransaction;
        }

        public void setPendingTransaction(@Nullable TransactionHolder<TXN> pendingTransaction) {
            this.pendingTransaction = pendingTransaction;
        }

        public List<TransactionHolder<TXN>> getPendingCommitTransactions() {
            return pendingCommitTransactions;
        }

        public void setPendingCommitTransactions(
                List<TransactionHolder<TXN>> pendingCommitTransactions) {
            this.pendingCommitTransactions = pendingCommitTransactions;
        }

        public Optional<CONTEXT> getContext() {
            return context;
        }

        public void setContext(Optional<CONTEXT> context) {
            this.context = context;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            State<?, ?> state = (State<?, ?>) o;

            return Objects.equals(pendingTransaction, state.pendingTransaction)
                    && Objects.equals(pendingCommitTransactions, state.pendingCommitTransactions)
                    && Objects.equals(context, state.context);
        }

        @Override
        public int hashCode() {
            int result = pendingTransaction != null ? pendingTransaction.hashCode() : 0;
            result =
                    31 * result
                            + (pendingCommitTransactions != null
                                    ? pendingCommitTransactions.hashCode()
                                    : 0);
            result = 31 * result + (context != null ? context.hashCode() : 0);
            return result;
        }
    }

    /**
     * Adds metadata (currently only the start time of the transaction) to the transaction object.
     */
    @VisibleForTesting
    @Internal
    public static final class TransactionHolder<TXN> {

        private final TXN handle;

        /**
         * The system time when {@link #handle} was created. Used to determine if the current
         * transaction has exceeded its timeout specified by {@link #transactionTimeout}.
         */
        private final long transactionStartTime;

        @VisibleForTesting
        public TransactionHolder(TXN handle, long transactionStartTime) {
            this.handle = handle;
            this.transactionStartTime = transactionStartTime;
        }

        long elapsedTime(Clock clock) {
            return clock.millis() - transactionStartTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TransactionHolder<?> that = (TransactionHolder<?>) o;

            return transactionStartTime == that.transactionStartTime
                    && Objects.equals(handle, that.handle);
        }

        @Override
        public int hashCode() {
            int result = handle != null ? handle.hashCode() : 0;
            result = 31 * result + (int) (transactionStartTime ^ (transactionStartTime >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "TransactionHolder{"
                    + "handle="
                    + handle
                    + ", transactionStartTime="
                    + transactionStartTime
                    + '}';
        }
    }

    /** Custom {@link TypeSerializer} for the sink state. */
    @VisibleForTesting
    @Internal
    public static final class StateSerializer<TXN, CONTEXT>
            extends TypeSerializer<State<TXN, CONTEXT>> {

        private static final long serialVersionUID = 1L;

        private final TypeSerializer<TXN> transactionSerializer;
        private final TypeSerializer<CONTEXT> contextSerializer;
        private final boolean supportNullPendingTransaction;

        public StateSerializer(
                TypeSerializer<TXN> transactionSerializer,
                TypeSerializer<CONTEXT> contextSerializer) {
            this(transactionSerializer, contextSerializer, true);
        }

        public StateSerializer(
                TypeSerializer<TXN> transactionSerializer,
                TypeSerializer<CONTEXT> contextSerializer,
                boolean supportNullPendingTransaction) {
            this.transactionSerializer = checkNotNull(transactionSerializer);
            this.contextSerializer = checkNotNull(contextSerializer);
            this.supportNullPendingTransaction = supportNullPendingTransaction;
        }

        @Override
        public boolean isImmutableType() {
            return transactionSerializer.isImmutableType() && contextSerializer.isImmutableType();
        }

        @Override
        public TypeSerializer<State<TXN, CONTEXT>> duplicate() {
            return new StateSerializer<>(
                    transactionSerializer.duplicate(),
                    contextSerializer.duplicate(),
                    supportNullPendingTransaction);
        }

        @Override
        public State<TXN, CONTEXT> createInstance() {
            return null;
        }

        @Override
        public State<TXN, CONTEXT> copy(State<TXN, CONTEXT> from) {
            final TransactionHolder<TXN> pendingTransaction = from.getPendingTransaction();
            final TransactionHolder<TXN> copiedPendingTransaction =
                    pendingTransaction == null
                            ? null
                            : new TransactionHolder<>(
                                    transactionSerializer.copy(pendingTransaction.handle),
                                    pendingTransaction.transactionStartTime);

            final List<TransactionHolder<TXN>> copiedPendingCommitTransactions = new ArrayList<>();
            for (TransactionHolder<TXN> txn : from.getPendingCommitTransactions()) {
                final TXN txnHandleCopy = transactionSerializer.copy(txn.handle);
                copiedPendingCommitTransactions.add(
                        new TransactionHolder<>(txnHandleCopy, txn.transactionStartTime));
            }

            final Optional<CONTEXT> copiedContext = from.getContext().map(contextSerializer::copy);
            return new State<>(
                    copiedPendingTransaction, copiedPendingCommitTransactions, copiedContext);
        }

        @Override
        public State<TXN, CONTEXT> copy(State<TXN, CONTEXT> from, State<TXN, CONTEXT> reuse) {
            return copy(from);
        }

        @Override
        public int getLength() {
            return -1;
        }

        @Override
        public void serialize(State<TXN, CONTEXT> record, DataOutputView target)
                throws IOException {
            final TransactionHolder<TXN> pendingTransaction = record.getPendingTransaction();
            serializePendingTransaction(pendingTransaction, target);

            final List<TransactionHolder<TXN>> pendingCommitTransactions =
                    record.getPendingCommitTransactions();
            target.writeInt(pendingCommitTransactions.size());
            for (TransactionHolder<TXN> pendingTxn : pendingCommitTransactions) {
                transactionSerializer.serialize(pendingTxn.handle, target);
                target.writeLong(pendingTxn.transactionStartTime);
            }

            Optional<CONTEXT> context = record.getContext();
            if (context.isPresent()) {
                target.writeBoolean(true);
                contextSerializer.serialize(context.get(), target);
            } else {
                target.writeBoolean(false);
            }
        }

        @Override
        public State<TXN, CONTEXT> deserialize(DataInputView source) throws IOException {
            final TransactionHolder<TXN> pendingTxn = deserializePendingTransaction(source);

            int numPendingCommitTxns = source.readInt();
            List<TransactionHolder<TXN>> pendingCommitTxns = new ArrayList<>(numPendingCommitTxns);
            for (int i = 0; i < numPendingCommitTxns; i++) {
                final TXN pendingCommitTxnHandle = transactionSerializer.deserialize(source);
                final long pendingCommitTxnStartTime = source.readLong();
                pendingCommitTxns.add(
                        new TransactionHolder<>(pendingCommitTxnHandle, pendingCommitTxnStartTime));
            }

            Optional<CONTEXT> context = Optional.empty();
            boolean hasContext = source.readBoolean();
            if (hasContext) {
                context = Optional.of(contextSerializer.deserialize(source));
            }

            return new State<>(pendingTxn, pendingCommitTxns, context);
        }

        private void serializePendingTransaction(
                @Nullable TransactionHolder<TXN> pendingTransaction, DataOutputView target)
                throws IOException {
            if (supportNullPendingTransaction) {
                target.writeBoolean(pendingTransaction != null);
            } else {
                checkState(
                        pendingTransaction != null,
                        "Received null pending transaction while the serializer "
                                + "does not support null pending transaction.");
            }

            if (pendingTransaction != null) {
                transactionSerializer.serialize(pendingTransaction.handle, target);
                target.writeLong(pendingTransaction.transactionStartTime);
            }
        }

        private TransactionHolder<TXN> deserializePendingTransaction(DataInputView source)
                throws IOException {
            boolean hasPendingTransaction =
                    supportNullPendingTransaction ? source.readBoolean() : true;

            if (!hasPendingTransaction) {
                return null;
            }

            TXN pendingTxnHandle = transactionSerializer.deserialize(source);
            final long pendingTxnStartTime = source.readLong();
            return new TransactionHolder<>(pendingTxnHandle, pendingTxnStartTime);
        }

        @Override
        public State<TXN, CONTEXT> deserialize(State<TXN, CONTEXT> reuse, DataInputView source)
                throws IOException {
            return deserialize(source);
        }

        @Override
        public void copy(DataInputView source, DataOutputView target) throws IOException {
            TransactionHolder<TXN> pendingTransaction = deserializePendingTransaction(source);
            serializePendingTransaction(pendingTransaction, target);

            int numPendingCommitTxns = source.readInt();
            target.writeInt(numPendingCommitTxns);
            for (int i = 0; i < numPendingCommitTxns; i++) {
                TXN pendingCommitTxnHandle = transactionSerializer.deserialize(source);
                transactionSerializer.serialize(pendingCommitTxnHandle, target);
                final long pendingCommitTxnStartTime = source.readLong();
                target.writeLong(pendingCommitTxnStartTime);
            }

            boolean hasContext = source.readBoolean();
            target.writeBoolean(hasContext);
            if (hasContext) {
                CONTEXT context = contextSerializer.deserialize(source);
                contextSerializer.serialize(context, target);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StateSerializer<?, ?> that = (StateSerializer<?, ?>) o;

            if (!transactionSerializer.equals(that.transactionSerializer)) {
                return false;
            }
            return contextSerializer.equals(that.contextSerializer);
        }

        @Override
        public int hashCode() {
            int result = transactionSerializer.hashCode();
            result = 31 * result + contextSerializer.hashCode();
            return result;
        }

        @Override
        public StateSerializerSnapshot<TXN, CONTEXT> snapshotConfiguration() {
            return new StateSerializerSnapshot<>(this);
        }
    }

    /**
     * {@link TypeSerializerConfigSnapshot} for sink state. This has to be public so that it can be
     * deserialized/instantiated, should not be used anywhere outside {@code
     * TwoPhaseCommitSinkFunction}.
     *
     * @deprecated this snapshot class is no longer in use, and is maintained only for backwards
     *     compatibility purposes. It is fully replaced by {@link StateSerializerSnapshot}.
     */
    @Internal
    @Deprecated
    public static final class StateSerializerConfigSnapshot<TXN, CONTEXT>
            extends CompositeTypeSerializerConfigSnapshot<State<TXN, CONTEXT>> {

        private static final int VERSION = 1;

        /** This empty nullary constructor is required for deserializing the configuration. */
        public StateSerializerConfigSnapshot() {}

        public StateSerializerConfigSnapshot(
                TypeSerializer<TXN> transactionSerializer,
                TypeSerializer<CONTEXT> contextSerializer) {
            super(transactionSerializer, contextSerializer);
        }

        @Override
        public int getVersion() {
            return VERSION;
        }

        @Override
        public TypeSerializerSchemaCompatibility<State<TXN, CONTEXT>> resolveSchemaCompatibility(
                TypeSerializer<State<TXN, CONTEXT>> newSerializer) {

            final TypeSerializerSnapshot<?>[] nestedSnapshots =
                    getNestedSerializersAndConfigs().stream()
                            .map(t -> t.f1)
                            .toArray(TypeSerializerSnapshot[]::new);

            return CompositeTypeSerializerUtil.delegateCompatibilityCheckToNewSnapshot(
                    newSerializer, new StateSerializerSnapshot<>(), nestedSnapshots);
        }
    }

    /** Snapshot for the {@link StateSerializer}. */
    @Internal
    public static final class StateSerializerSnapshot<TXN, CONTEXT>
            extends CompositeTypeSerializerSnapshot<
                    State<TXN, CONTEXT>, StateSerializer<TXN, CONTEXT>> {

        private static final int VERSION = 4;

        private static final int FIRST_VERSION_SUPPORTING_NULL_TRANSACTIONS = 3;

        private boolean supportsNullTransaction = true;

        @SuppressWarnings("WeakerAccess")
        public StateSerializerSnapshot() {
            super(StateSerializer.class);
        }

        StateSerializerSnapshot(StateSerializer<TXN, CONTEXT> serializerInstance) {
            super(serializerInstance);
            this.supportsNullTransaction = serializerInstance.supportNullPendingTransaction;
        }

        @Override
        protected int getCurrentOuterSnapshotVersion() {
            return VERSION;
        }

        @Override
        protected void readOuterSnapshot(
                int readOuterSnapshotVersion, DataInputView in, ClassLoader userCodeClassLoader)
                throws IOException {
            if (readOuterSnapshotVersion < FIRST_VERSION_SUPPORTING_NULL_TRANSACTIONS) {
                supportsNullTransaction = false;
            } else if (readOuterSnapshotVersion == FIRST_VERSION_SUPPORTING_NULL_TRANSACTIONS) {
                supportsNullTransaction = true;
            } else {
                supportsNullTransaction = in.readBoolean();
            }
        }

        @Override
        protected void writeOuterSnapshot(DataOutputView out) throws IOException {
            out.writeBoolean(supportsNullTransaction);
        }

        @Override
        protected OuterSchemaCompatibility resolveOuterSchemaCompatibility(
                StateSerializer<TXN, CONTEXT> newSerializer) {
            if (supportsNullTransaction != newSerializer.supportNullPendingTransaction) {
                return OuterSchemaCompatibility.COMPATIBLE_AFTER_MIGRATION;
            }

            return OuterSchemaCompatibility.COMPATIBLE_AS_IS;
        }

        @Override
        protected StateSerializer<TXN, CONTEXT> createOuterSerializerWithNestedSerializers(
                TypeSerializer<?>[] nestedSerializers) {
            @SuppressWarnings("unchecked")
            final TypeSerializer<TXN> transactionSerializer =
                    (TypeSerializer<TXN>) nestedSerializers[0];

            @SuppressWarnings("unchecked")
            final TypeSerializer<CONTEXT> contextSerializer =
                    (TypeSerializer<CONTEXT>) nestedSerializers[1];

            return new StateSerializer<>(
                    transactionSerializer, contextSerializer, supportsNullTransaction);
        }

        @Override
        protected TypeSerializer<?>[] getNestedSerializers(
                StateSerializer<TXN, CONTEXT> outerSerializer) {
            return new TypeSerializer<?>[] {
                outerSerializer.transactionSerializer, outerSerializer.contextSerializer
            };
        }
    }
}
