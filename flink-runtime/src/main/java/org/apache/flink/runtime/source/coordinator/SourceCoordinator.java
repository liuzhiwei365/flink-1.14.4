/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.apache.flink.runtime.source.coordinator;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.connector.source.ReaderInfo;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SourceSplit;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputViewStreamWrapper;
import org.apache.flink.core.memory.DataOutputViewStreamWrapper;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;
import org.apache.flink.runtime.source.event.ReaderRegistrationEvent;
import org.apache.flink.runtime.source.event.RequestSplitEvent;
import org.apache.flink.runtime.source.event.SourceEventWrapper;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.TemporaryClassLoaderContext;
import org.apache.flink.util.function.ThrowingRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.apache.flink.runtime.source.coordinator.SourceCoordinatorSerdeUtils.readAndVerifyCoordinatorSerdeVersion;
import static org.apache.flink.runtime.source.coordinator.SourceCoordinatorSerdeUtils.readBytes;
import static org.apache.flink.runtime.source.coordinator.SourceCoordinatorSerdeUtils.writeCoordinatorSerdeVersion;
import static org.apache.flink.util.IOUtils.closeAll;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * The default implementation of the {@link OperatorCoordinator} for the {@link Source}.
 *
 * <p>The <code>SourceCoordinator</code> provides an event loop style thread model to interact with
 * the Flink runtime. The coordinator ensures that all the state manipulations are made by its event
 * loop thread. It also helps keep track of the necessary split assignments history per subtask to
 * simplify the {@link SplitEnumerator} implementation.
 *
 * <p>The coordinator maintains a {@link
 * org.apache.flink.api.connector.source.SplitEnumeratorContext SplitEnumeratorContxt} and shares it
 * with the enumerator. When the coordinator receives an action request from the Flink runtime, it
 * sets up the context, and calls corresponding method of the SplitEnumerator to take actions.
 */
@Internal
public class SourceCoordinator<SplitT extends SourceSplit, EnumChkT>
        implements OperatorCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(SourceCoordinator.class);

    /** The name of the operator this SourceCoordinator is associated with. */
    private final String operatorName;
    /** The Source that is associated with this SourceCoordinator. */
    private final Source<?, SplitT, EnumChkT> source;
    /** The serializer that handles the serde of the SplitEnumerator checkpoints. */
    private final SimpleVersionedSerializer<EnumChkT> enumCheckpointSerializer;

    /** The context containing the states of the coordinator. */
    private final SourceCoordinatorContext<SplitT> context;

    // 分片枚举器, 专门维护 source的 分片元数据信息
    // 在本算子协调者 启动或者重置的时候 才会创建
    private SplitEnumerator<SplitT, EnumChkT> enumerator;
    /** A flag marking whether the coordinator has started. */
    private boolean started;

    public SourceCoordinator(
            String operatorName,
            Source<?, SplitT, EnumChkT> source,
            SourceCoordinatorContext<SplitT> context) {
        this.operatorName = operatorName;
        this.source = source;
        this.enumCheckpointSerializer = source.getEnumeratorCheckpointSerializer();
        this.context = context;
    }

    // 算子协调者的功能本来就是协调 算子在各个 subtask 中的状态
    // 而作为 具体实现 SourceCoordinator, 其主要功能就是 维护和协调各个 subtask 的分片信息
    @Override
    public void start() throws Exception {
        LOG.info("Starting split enumerator for source {}.", operatorName);

        started = true;

        if (enumerator == null) {
            final ClassLoader userCodeClassLoader =
                    context.getCoordinatorContext().getUserCodeClassloader();
            try (TemporaryClassLoaderContext ignored =
                    TemporaryClassLoaderContext.of(userCodeClassLoader)) {
                // 创建分配枚举器
                enumerator = source.createEnumerator(context);
            } catch (Throwable t) {
                ExceptionUtils.rethrowIfFatalErrorOrOOM(t);
                LOG.error("Failed to create Source Enumerator for source {}", operatorName, t);
                context.failJob(t);
                return;
            }
        }

        // 我们依靠单线程 的 协调者执行器, 来保证  其他方法调用前 在 分片枚举器一定已经启动好了
        runInEventLoop(() -> enumerator.start(), "starting the SplitEnumerator.");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing SourceCoordinator for source {}.", operatorName);
        if (started) {
            closeAll(context, enumerator);
        }
        LOG.info("Source coordinator for source {} closed.", operatorName);
    }

    // 算子通过 OperatorEventGateway 接口 调用 sendEventToCoordinator 方法
    // 会被 OperatorCoordinator 接口 的  handleEventFromOperator 方法 相应本方法 只是本子类 的 实现
    @Override
    public void handleEventFromOperator(int subtask, OperatorEvent event) {
        runInEventLoop(
                () -> {
                    if (event instanceof RequestSplitEvent) {

                        LOG.info("Source {} received split request from parallel task {}", operatorName, subtask);

                        // 如果是mysql-cdc 实现：
                        //    MySqlSourceEnumerator#handleSplitRequest
                        //         MySqlSourceEnumerator#assignSplits
                        //
                        enumerator.handleSplitRequest(subtask, ((RequestSplitEvent) event).hostName());


                    } else if (event instanceof SourceEventWrapper) {

                        final SourceEvent sourceEvent = ((SourceEventWrapper) event).getSourceEvent();
                        LOG.debug("Source {} received custom event from parallel task {}: {}", operatorName, subtask, sourceEvent);
                        enumerator.handleSourceEvent(subtask, sourceEvent);

                    } else if (event instanceof ReaderRegistrationEvent) {

                        final ReaderRegistrationEvent registrationEvent = (ReaderRegistrationEvent) event;
                        LOG.info("Source {} registering reader for parallel task {} @ {}", operatorName, subtask, registrationEvent.location());
                        handleReaderRegistrationEvent(registrationEvent);

                    } else {
                        throw new FlinkException("Unrecognized Operator Event: " + event);
                    }
                },
                "handling operator event %s from subtask %d",
                event,
                subtask);
    }

    @Override
    public void subtaskFailed(int subtaskId, @Nullable Throwable reason) {
        runInEventLoop(
                () -> {
                    LOG.info(
                            "Removing registered reader after failure for subtask {} of source {}.",
                            subtaskId,
                            operatorName);
                    context.unregisterSourceReader(subtaskId);
                    context.subtaskNotReady(subtaskId);
                },
                "handling subtask %d failure",
                subtaskId);
    }

    @Override
    public void subtaskReset(int subtaskId, long checkpointId) {
        // 用 SourceCoordinatorContext对象的 ScheduledExecutorService成员来执行
        runInEventLoop(
                () -> {
                    LOG.info(
                            "Recovering subtask {} to checkpoint {} for source {} to checkpoint.",
                            subtaskId,
                            checkpointId,
                            operatorName);

                    // 恢复指定checkpointId指定自任务 的 最新成功的 分片信息
                    final List<SplitT> splitsToAddBack =
                            context.getAndRemoveUncheckpointedAssignment(subtaskId, checkpointId);
                    LOG.debug(
                            "Adding splits back to the split enumerator of source {}: {}",
                            operatorName,
                            splitsToAddBack);
                    // 把恢复的分片信息放回 分片枚举器
                    enumerator.addSplitsBack(splitsToAddBack, subtaskId);
                },
                "handling subtask %d recovery to checkpoint %d",
                subtaskId,
                checkpointId);
    }

    @Override
    public void subtaskReady(int subtask, SubtaskGateway gateway) {
        assert subtask == gateway.getSubtask();

        runInEventLoop(
                () -> context.subtaskReady(gateway),
                "making event gateway to subtask %d available",
                subtask);
    }

    @Override
    public void checkpointCoordinator(long checkpointId, CompletableFuture<byte[]> result) {
        runInEventLoop(
                () -> {
                    LOG.debug(
                            "Taking a state snapshot on operator {} for checkpoint {}",
                            operatorName,
                            checkpointId);
                    try {
                        context.onCheckpoint(checkpointId);
                        // 把 checkpointId 下的 分片信息序列化
                        result.complete(toBytes(checkpointId));
                    } catch (Throwable e) {
                        ExceptionUtils.rethrowIfFatalErrorOrOOM(e);
                        result.completeExceptionally(
                                new CompletionException(
                                        String.format(
                                                "Failed to checkpoint SplitEnumerator for source %s",
                                                operatorName),
                                        e));
                    }
                },
                "taking checkpoint %d",
                checkpointId);
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {
        runInEventLoop(
                () -> {
                    LOG.info(
                            "Marking checkpoint {} as completed for source {}.",
                            checkpointId,
                            operatorName);
                    context.onCheckpointComplete(checkpointId);
                    enumerator.notifyCheckpointComplete(checkpointId);
                },
                "notifying the enumerator of completion of checkpoint %d",
                checkpointId);
    }

    @Override
    public void notifyCheckpointAborted(long checkpointId) {
        runInEventLoop(
                () -> {
                    LOG.info(
                            "Marking checkpoint {} as aborted for source {}.",
                            checkpointId,
                            operatorName);
                    enumerator.notifyCheckpointAborted(checkpointId);
                },
                "calling notifyCheckpointAborted()");
    }

    @Override
    public void resetToCheckpoint(final long checkpointId, @Nullable final byte[] checkpointData)
            throws Exception {

        checkState(!started, "The coordinator can only be reset if it was not yet started");
        assert enumerator == null;

        // the checkpoint data is null if there was no completed checkpoint before
        // in that case we don't restore here, but let a fresh SplitEnumerator be created
        // when "start()" is called.
        if (checkpointData == null) {
            return;
        }

        LOG.info("Restoring SplitEnumerator of source {} from checkpoint.", operatorName);

        final ClassLoader userCodeClassLoader =
                context.getCoordinatorContext().getUserCodeClassloader();

        try (TemporaryClassLoaderContext ignored =
                TemporaryClassLoaderContext.of(userCodeClassLoader)) {
            final EnumChkT enumeratorCheckpoint = deserializeCheckpoint(checkpointData);
            // 核心,  恢复分片枚举器
            enumerator = source.restoreEnumerator(context, enumeratorCheckpoint);
        }
    }

    // 用成员变量 coordinatorExecutor 来执行
    private void runInEventLoop(
            final ThrowingRunnable<Throwable> action,
            final String actionName,
            final Object... actionNameFormatParameters) {

        ensureStarted();

        // 分片枚举器 没有构造好, 其他的逻辑就没有必须做, 因为其他的逻辑都是围绕着 分片枚举器展开的
        if (enumerator == null) {
            return;
        }

        context.runInCoordinatorThread(
                () -> {
                    try {
                        action.run();
                    } catch (Throwable t) {
                        // if we have a JVM critical error, promote it immediately, there is a good
                        // chance the
                        // logging or job failing will not succeed any more
                        ExceptionUtils.rethrowIfFatalErrorOrOOM(t);

                        final String actionString =
                                String.format(actionName, actionNameFormatParameters);
                        LOG.error(
                                "Uncaught exception in the SplitEnumerator for Source {} while {}. Triggering job failover.",
                                operatorName,
                                actionString,
                                t);
                        context.failJob(t);
                    }
                });
    }

    // ---------------------------------------------------
    @VisibleForTesting
    SplitEnumerator<SplitT, EnumChkT> getEnumerator() {
        return enumerator;
    }

    @VisibleForTesting
    SourceCoordinatorContext<SplitT> getContext() {
        return context;
    }

    // --------------------- Serde -----------------------

    /**
     * Serialize the coordinator state. The current implementation may not be super efficient, but
     * it should not matter that much because most of the state should be rather small. Large states
     * themselves may already be a problem regardless of how the serialization is implemented.
     *
     * @return A byte array containing the serialized state of the source coordinator.
     * @throws Exception When something goes wrong in serialization.
     */
    private byte[] toBytes(long checkpointId) throws Exception {

        //  把 checkpointId 下的 checkpoint信息  序列化分片信息
        //  如果是 kafka 场景,   new KafkaSourceEnumState(assignedPartitions)
        return writeCheckpointBytes(
                enumerator.snapshotState(checkpointId), enumCheckpointSerializer);
    }

    static <EnumChkT> byte[] writeCheckpointBytes(
            final EnumChkT enumeratorCheckpoint,
            final SimpleVersionedSerializer<EnumChkT> enumeratorCheckpointSerializer)
            throws Exception {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputViewStreamWrapper(baos)) {

            writeCoordinatorSerdeVersion(out);
            out.writeInt(enumeratorCheckpointSerializer.getVersion());
            byte[] serialziedEnumChkpt =
                    enumeratorCheckpointSerializer.serialize(enumeratorCheckpoint);
            out.writeInt(serialziedEnumChkpt.length);
            out.write(serialziedEnumChkpt);
            out.flush();
            return baos.toByteArray();
        }
    }

    /**
     * Restore the state of this source coordinator from the state bytes.
     *
     * @param bytes The checkpoint bytes that was returned from {@link #toBytes(long)}
     * @throws Exception When the deserialization failed.
     */
    private EnumChkT deserializeCheckpoint(byte[] bytes) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                DataInputStream in = new DataInputViewStreamWrapper(bais)) {
            final int coordinatorSerdeVersion = readAndVerifyCoordinatorSerdeVersion(in);
            int enumSerializerVersion = in.readInt();
            int serializedEnumChkptSize = in.readInt();
            byte[] serializedEnumChkpt = readBytes(in, serializedEnumChkptSize);

            if (coordinatorSerdeVersion != SourceCoordinatorSerdeUtils.VERSION_0
                    && bais.available() > 0) {
                throw new IOException("Unexpected trailing bytes in enumerator checkpoint data");
            }

            return enumCheckpointSerializer.deserialize(enumSerializerVersion, serializedEnumChkpt);
        }
    }

    // --------------------- private methods -------------

    private void handleReaderRegistrationEvent(ReaderRegistrationEvent event) {
        context.registerSourceReader(new ReaderInfo(event.subtaskId(), event.location()));
        enumerator.addReader(event.subtaskId());
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("The coordinator has not started yet.");
        }
    }
}
