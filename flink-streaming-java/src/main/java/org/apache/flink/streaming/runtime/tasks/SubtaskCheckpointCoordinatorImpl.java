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

package org.apache.flink.streaming.runtime.tasks;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.core.fs.CloseableRegistry;
import org.apache.flink.runtime.checkpoint.CheckpointException;
import org.apache.flink.runtime.checkpoint.CheckpointMetaData;
import org.apache.flink.runtime.checkpoint.CheckpointMetricsBuilder;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter.ChannelStateWriteResult;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriterImpl;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.io.network.api.CancelCheckpointMarker;
import org.apache.flink.runtime.io.network.api.CheckpointBarrier;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.state.CheckpointStorageLocationReference;
import org.apache.flink.runtime.state.CheckpointStorageWorkerView;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.streaming.api.operators.OperatorSnapshotFutures;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.IOUtils;
import org.apache.flink.util.function.BiFunctionWithException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.apache.flink.util.IOUtils.closeQuietly;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

// 负责一个StreamTask 中 ,各个算子的 snapshot
class SubtaskCheckpointCoordinatorImpl implements SubtaskCheckpointCoordinator {

    private static final Logger LOG =
            LoggerFactory.getLogger(SubtaskCheckpointCoordinatorImpl.class);
    private static final int DEFAULT_MAX_RECORD_ABORTED_CHECKPOINTS = 128;

    private static final int CHECKPOINT_EXECUTION_DELAY_LOG_THRESHOLD_MS = 30_000;

    private final boolean enableCheckpointAfterTasksFinished;

    private final CachingCheckpointStorageWorkerView checkpointStorage;
    private final String taskName;
    private final ExecutorService asyncOperationsThreadPool;
    private final Environment env;
    private final AsyncExceptionHandler asyncExceptionHandler;
    private final ChannelStateWriter channelStateWriter;
    private final StreamTaskActionExecutor actionExecutor;
    private final BiFunctionWithException<
                    ChannelStateWriter, Long, CompletableFuture<Void>, CheckpointException>
            prepareInputSnapshot;
    /** The IDs of the checkpoint for which we are notified aborted. */
    private final Set<Long> abortedCheckpointIds;

    private long lastCheckpointId;

    /** Lock that guards state of AsyncCheckpointRunnable registry. * */
    private final Object lock;

    @GuardedBy("lock")
    private final Map<Long, AsyncCheckpointRunnable> checkpoints;

    /** Indicates if this registry is closed. */
    @GuardedBy("lock")
    private boolean closed;

    SubtaskCheckpointCoordinatorImpl(
            CheckpointStorageWorkerView checkpointStorage,
            String taskName,
            StreamTaskActionExecutor actionExecutor,
            CloseableRegistry closeableRegistry,
            ExecutorService asyncOperationsThreadPool,
            Environment env,
            AsyncExceptionHandler asyncExceptionHandler,
            boolean unalignedCheckpointEnabled,
            boolean enableCheckpointAfterTasksFinished,
            BiFunctionWithException<
                            ChannelStateWriter, Long, CompletableFuture<Void>, CheckpointException>
                    prepareInputSnapshot)
            throws IOException {
        this(
                checkpointStorage,
                taskName,
                actionExecutor,
                closeableRegistry,
                asyncOperationsThreadPool,
                env,
                asyncExceptionHandler,
                unalignedCheckpointEnabled,
                enableCheckpointAfterTasksFinished,
                prepareInputSnapshot,
                DEFAULT_MAX_RECORD_ABORTED_CHECKPOINTS);
    }

    SubtaskCheckpointCoordinatorImpl(
            CheckpointStorageWorkerView checkpointStorage,
            String taskName,
            StreamTaskActionExecutor actionExecutor,
            CloseableRegistry closeableRegistry,
            ExecutorService asyncOperationsThreadPool,
            Environment env,
            AsyncExceptionHandler asyncExceptionHandler,
            boolean unalignedCheckpointEnabled,
            boolean enableCheckpointAfterTasksFinished,
            BiFunctionWithException<
                            ChannelStateWriter, Long, CompletableFuture<Void>, CheckpointException>
                    prepareInputSnapshot,
            int maxRecordAbortedCheckpoints)
            throws IOException {
        this(
                checkpointStorage,
                taskName,
                actionExecutor,
                closeableRegistry,
                asyncOperationsThreadPool,
                env,
                asyncExceptionHandler,
                prepareInputSnapshot,
                maxRecordAbortedCheckpoints,
                unalignedCheckpointEnabled
                        ? openChannelStateWriter(taskName, checkpointStorage, env)
                        : ChannelStateWriter.NO_OP,
                enableCheckpointAfterTasksFinished);
    }

    @VisibleForTesting
    SubtaskCheckpointCoordinatorImpl(
            CheckpointStorageWorkerView checkpointStorage,
            String taskName,
            StreamTaskActionExecutor actionExecutor,
            CloseableRegistry closeableRegistry,
            ExecutorService asyncOperationsThreadPool,
            Environment env,
            AsyncExceptionHandler asyncExceptionHandler,
            BiFunctionWithException<
                            ChannelStateWriter, Long, CompletableFuture<Void>, CheckpointException>
                    prepareInputSnapshot,
            int maxRecordAbortedCheckpoints,
            ChannelStateWriter channelStateWriter,
            boolean enableCheckpointAfterTasksFinished)
            throws IOException {
        this.checkpointStorage =
                new CachingCheckpointStorageWorkerView(checkNotNull(checkpointStorage));
        this.taskName = checkNotNull(taskName);
        this.checkpoints = new HashMap<>();
        this.lock = new Object();
        this.asyncOperationsThreadPool = checkNotNull(asyncOperationsThreadPool);
        this.env = checkNotNull(env);
        this.asyncExceptionHandler = checkNotNull(asyncExceptionHandler);
        this.actionExecutor = checkNotNull(actionExecutor);
        this.channelStateWriter = checkNotNull(channelStateWriter);
        this.prepareInputSnapshot = prepareInputSnapshot;
        this.abortedCheckpointIds =
                createAbortedCheckpointSetWithLimitSize(maxRecordAbortedCheckpoints);
        this.lastCheckpointId = -1L;
        closeableRegistry.registerCloseable(this);
        this.closed = false;
        this.enableCheckpointAfterTasksFinished = enableCheckpointAfterTasksFinished;
    }

    // 非对齐的 checkpoint启动才会调用 到这里
    private static ChannelStateWriter openChannelStateWriter(
            String taskName, CheckpointStorageWorkerView checkpointStorage, Environment env) {
        ChannelStateWriterImpl writer =
                new ChannelStateWriterImpl(
                        taskName, env.getTaskInfo().getIndexOfThisSubtask(), checkpointStorage);

        // 让 ChannelStateWriteRequestExecutorImpl 跑起来
        writer.open();
        return writer;
    }

    @Override
    public void abortCheckpointOnBarrier(
            long checkpointId, CheckpointException cause, OperatorChain<?, ?> operatorChain)
            throws IOException {
        LOG.debug("Aborting checkpoint via cancel-barrier {} for task {}", checkpointId, taskName);
        lastCheckpointId = Math.max(lastCheckpointId, checkpointId);
        Iterator<Long> iterator = abortedCheckpointIds.iterator();
        while (iterator.hasNext()) {
            long next = iterator.next();
            if (next < lastCheckpointId) {
                iterator.remove();
            } else {
                break;
            }
        }

        checkpointStorage.clearCacheFor(checkpointId);

        channelStateWriter.abort(checkpointId, cause, true);

        // notify the coordinator that we decline this checkpoint
        env.declineCheckpoint(checkpointId, cause);

        // notify all downstream operators that they should not wait for a barrier from us
        actionExecutor.runThrowing(
                () -> operatorChain.broadcastEvent(new CancelCheckpointMarker(checkpointId)));
    }

    @Override
    public CheckpointStorageWorkerView getCheckpointStorage() {
        return checkpointStorage;
    }

    @Override
    public ChannelStateWriter getChannelStateWriter() {
        return channelStateWriter;
    }

    @Override
    public void checkpointState(
            CheckpointMetaData metadata,
            CheckpointOptions options,
            CheckpointMetricsBuilder metrics,
            OperatorChain<?, ?> operatorChain,
            boolean isTaskFinished,
            Supplier<Boolean> isRunning)
            throws Exception {

        checkNotNull(options);
        checkNotNull(metrics);

        // 检查上一个checkpoint的 id是否比metadata的checkpoint id 小
        // 否则存在checkpoint barrier乱序的可能，终止掉metadata.getCheckpointId()对应的checkpoint操作
        if (lastCheckpointId >= metadata.getCheckpointId()) {
            LOG.info(
                    "Out of order checkpoint barrier (aborted previously?): {} >= {}",
                    lastCheckpointId,
                    metadata.getCheckpointId());
            channelStateWriter.abort(metadata.getCheckpointId(), new CancellationException(), true);
            checkAndClearAbortedStatus(metadata.getCheckpointId());
            return;
        }

        logCheckpointProcessingDelay(metadata);

        // 第0步，更新lastCheckpointId变量
        // 如果当前checkpoint被取消，广播CancelCheckpointMarker到下游，表明这个checkpoint被终止
        lastCheckpointId = metadata.getCheckpointId();
        if (checkAndClearAbortedStatus(metadata.getCheckpointId())) {
            // broadcast cancel checkpoint marker to avoid downstream back-pressure due to
            // checkpoint barrier align.
            operatorChain.broadcastEvent(new CancelCheckpointMarker(metadata.getCheckpointId()));
            LOG.info(
                    "Checkpoint {} has been notified as aborted, would not trigger any checkpoint.",
                    metadata.getCheckpointId());
            return;
        }

        // if checkpoint has been previously unaligned, but was forced to be aligned (pointwise
        // connection), revert it here so that it can jump over output data

        if (options.getAlignment() == CheckpointOptions.AlignmentType.FORCED_ALIGNED) {
            options = options.withUnalignedSupported();
            initInputsCheckpoint(metadata.getCheckpointId(), options);
        }


        // 第1步,   调用operatorChain的prepareSnapshotPreBarrier方法，执行checkpoint操作前的预处理逻辑
        //         大都是空实现
        operatorChain.prepareSnapshotPreBarrier(metadata.getCheckpointId());


        /*
           第2步,   向下游发送 CheckpointBarriar 事件

                    1 把 CheckpointBarrier 事件会添加到 PipelinedSubpartition 成员优先级队列buffers  的优先级部分

                    2 如果是  非对齐的checkpoint ,会触发 ResultSubpartition的 实际上的持久化, 调用栈如下:
                             OperatorChain.broadcastEvent(AbstractEvent, boolean)
                             RecordWriterOutput.broadcastEvent
                             RecordWriter.broadcastEvent(AbstractEvent, boolean)
                             ResultPartitionWriter.broadcastEvent
                             BufferWritingResultPartition.broadcastEvent
                             PipelinedSubpartition.add(BufferConsumer, int)
                             PipelinedSubpartition.add(BufferConsumer, int, boolean)
                             PipelinedSubpartition.addBuffer
                             PipelinedSubpartition.processPriorityBuffer
                             ChannelStateWriter.addOutputData    做实际的持久化
         */
        operatorChain.broadcastEvent(
                new CheckpointBarrier(metadata.getCheckpointId(), metadata.getTimestamp(), options),
                options.isUnalignedCheckpoint());

        // 第3步,    是一个关键点，如果启用了unaligned checkpoint, 将所有input channel中checkpoint barrier后的buffer
        // 写入到checkpoint中
        if (options.isUnalignedCheckpoint()) {
            // output data already written while broadcasting event
            channelStateWriter.finishOutput(metadata.getCheckpointId());
        }


        // 第4步,   用于存储所有本subtask中 所有算子的 所有状态类型的 快照操作
        //         如果是非对齐且阻塞的 checkpoint 则不包括 inputChannel 和 ResultSubpartition 类型的状态？
        Map<OperatorID, OperatorSnapshotFutures> snapshotFutures = new HashMap<>(operatorChain.getNumberOfOperators());

        try {
            //  第5步, 把 operatorChain链中的 所有 快照逻辑 填充到 snapshotFutures中
            if (takeSnapshotSync(
                    snapshotFutures, metadata, metrics, options, operatorChain, isRunning)) {

                //  第6步, 用线程池异步的执行 snapshotFutures , 并且向CheckpointCoodinator 汇报ack
                finishAndReportAsync(
                        snapshotFutures,
                        metadata,
                        metrics,
                        operatorChain.isTaskDeployedAsFinished(),
                        isTaskFinished,
                        isRunning);
            } else {
                cleanup(snapshotFutures, metadata, metrics, new Exception("Checkpoint declined"));
            }
        } catch (Exception ex) {
            cleanup(snapshotFutures, metadata, metrics, ex);
            throw ex;
        }
    }

    @Override
    public void notifyCheckpointComplete(
            long checkpointId, OperatorChain<?, ?> operatorChain, Supplier<Boolean> isRunning)
            throws Exception {
        try {
            if (!isRunning.get()) {
                LOG.debug(
                        "Ignoring notification of complete checkpoint {} for not-running task {}",
                        checkpointId,
                        taskName);
            } else {
                LOG.debug(
                        "Notification of completed checkpoint {} for task {}",
                        checkpointId,
                        taskName);
                operatorChain.notifyCheckpointComplete(checkpointId);
            }
        } finally {
            env.getTaskStateManager().notifyCheckpointComplete(checkpointId);
        }
    }

    @Override
    public void notifyCheckpointAborted(
            long checkpointId, OperatorChain<?, ?> operatorChain, Supplier<Boolean> isRunning)
            throws Exception {

        try {
            if (!isRunning.get()) {
                LOG.debug(
                        "Ignoring notification of aborted checkpoint {} for not-running task {}",
                        checkpointId,
                        taskName);
            } else {
                LOG.debug(
                        "Notification of aborted checkpoint {} for task {}",
                        checkpointId,
                        taskName);

                boolean canceled = cancelAsyncCheckpointRunnable(checkpointId);

                if (!canceled) {
                    if (checkpointId > lastCheckpointId) {
                        // only record checkpoints that have not triggered on task side.
                        abortedCheckpointIds.add(checkpointId);
                    }
                }

                channelStateWriter.abort(
                        checkpointId,
                        new CancellationException("checkpoint aborted via notification"),
                        false);

                operatorChain.notifyCheckpointAborted(checkpointId);
            }
        } finally {
            env.getTaskStateManager().notifyCheckpointAborted(checkpointId);
        }
    }

    @Override
    public void initInputsCheckpoint(long id, CheckpointOptions checkpointOptions)
            throws CheckpointException {
        if (checkpointOptions.isUnalignedCheckpoint()) {
            // ChannelStateWriterImpl  对象
            channelStateWriter.start(id, checkpointOptions);

            prepareInflightDataSnapshot(id);
        }
    }

    public void waitForPendingCheckpoints() throws Exception {
        if (!enableCheckpointAfterTasksFinished) {
            return;
        }

        List<AsyncCheckpointRunnable> asyncCheckpointRunnables;
        synchronized (lock) {
            asyncCheckpointRunnables = new ArrayList<>(checkpoints.values());
        }

        // Waits for each checkpoint independently.
        asyncCheckpointRunnables.forEach(
                ar -> {
                    try {
                        ar.getFinishedFuture().get();
                    } catch (Exception e) {
                        LOG.debug(
                                "Async runnable for checkpoint "
                                        + ar.getCheckpointId()
                                        + " throws exception and exit",
                                e);
                    }
                });
    }

    @Override
    public void close() throws IOException {
        List<AsyncCheckpointRunnable> asyncCheckpointRunnables = null;
        synchronized (lock) {
            if (!closed) {
                closed = true;
                asyncCheckpointRunnables = new ArrayList<>(checkpoints.values());
                checkpoints.clear();
            }
        }
        IOUtils.closeAllQuietly(asyncCheckpointRunnables);
        channelStateWriter.close();
    }

    @VisibleForTesting
    int getAsyncCheckpointRunnableSize() {
        synchronized (lock) {
            return checkpoints.size();
        }
    }

    @VisibleForTesting
    int getAbortedCheckpointSize() {
        return abortedCheckpointIds.size();
    }

    private boolean checkAndClearAbortedStatus(long checkpointId) {
        return abortedCheckpointIds.remove(checkpointId);
    }

    private void registerAsyncCheckpointRunnable(
            long checkpointId, AsyncCheckpointRunnable asyncCheckpointRunnable) throws IOException {
        synchronized (lock) {
            if (closed) {
                LOG.debug(
                        "Cannot register Closeable, this subtaskCheckpointCoordinator is already closed. Closing argument.");
                closeQuietly(asyncCheckpointRunnable);
                checkState(
                        !checkpoints.containsKey(checkpointId),
                        "SubtaskCheckpointCoordinator was closed without releasing asyncCheckpointRunnable for checkpoint %s",
                        checkpointId);
            } else if (checkpoints.containsKey(checkpointId)) {
                closeQuietly(asyncCheckpointRunnable);
                throw new IOException(
                        String.format(
                                "Cannot register Closeable, async checkpoint %d runnable has been register. Closing argument.",
                                checkpointId));
            } else {
                checkpoints.put(checkpointId, asyncCheckpointRunnable);
            }
        }
    }

    private boolean unregisterAsyncCheckpointRunnable(long checkpointId) {
        synchronized (lock) {
            return checkpoints.remove(checkpointId) != null;
        }
    }

    /**
     * Cancel the async checkpoint runnable with given checkpoint id. If given checkpoint id is not
     * registered, return false, otherwise return true.
     */
    private boolean cancelAsyncCheckpointRunnable(long checkpointId) {
        AsyncCheckpointRunnable asyncCheckpointRunnable;
        synchronized (lock) {
            asyncCheckpointRunnable = checkpoints.remove(checkpointId);
        }
        closeQuietly(asyncCheckpointRunnable);
        return asyncCheckpointRunnable != null;
    }

    private void cleanup(
            Map<OperatorID, OperatorSnapshotFutures> operatorSnapshotsInProgress,
            CheckpointMetaData metadata,
            CheckpointMetricsBuilder metrics,
            Exception ex) {

        channelStateWriter.abort(metadata.getCheckpointId(), ex, true);
        for (OperatorSnapshotFutures operatorSnapshotResult :
                operatorSnapshotsInProgress.values()) {
            if (operatorSnapshotResult != null) {
                try {
                    operatorSnapshotResult.cancel();
                } catch (Exception e) {
                    LOG.warn("Could not properly cancel an operator snapshot result.", e);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "{} - did NOT finish synchronous part of checkpoint {}. Alignment duration: {} ms, snapshot duration {} ms",
                    taskName,
                    metadata.getCheckpointId(),
                    metrics.getAlignmentDurationNanosOrDefault() / 1_000_000,
                    metrics.getSyncDurationMillis());
        }
    }

    private void prepareInflightDataSnapshot(long checkpointId) throws CheckpointException {
        prepareInputSnapshot
                .apply(channelStateWriter, checkpointId)
                .whenComplete(
                        (unused, ex) -> {
                            if (ex != null) {
                                channelStateWriter.abort(
                                        checkpointId,
                                        ex,
                                        false /* result is needed and cleaned by getWriteResult */);
                            } else {
                                channelStateWriter.finishInput(checkpointId);
                            }
                        });
    }

    // 用线程池异步的执行 snapshotFutures , 并且向CheckpointCoodinator 汇报ack
    private void finishAndReportAsync(
            Map<OperatorID, OperatorSnapshotFutures> snapshotFutures,
            CheckpointMetaData metadata,
            CheckpointMetricsBuilder metrics,
            boolean isTaskDeployedAsFinished,
            boolean isTaskFinished,
            Supplier<Boolean> isRunning)
            throws IOException {
        AsyncCheckpointRunnable asyncCheckpointRunnable =
                new AsyncCheckpointRunnable(
                        snapshotFutures,
                        metadata,
                        metrics,
                        System.nanoTime(),
                        taskName,
                        unregisterConsumer(),
                        env,
                        asyncExceptionHandler,
                        isTaskDeployedAsFinished,
                        isTaskFinished,
                        isRunning);

        registerAsyncCheckpointRunnable(
                asyncCheckpointRunnable.getCheckpointId(), asyncCheckpointRunnable);

        // 核心, 用 线程池 执行  AsyncCheckpointRunnable的 run 方法
        asyncOperationsThreadPool.execute(asyncCheckpointRunnable);
    }

    private Consumer<AsyncCheckpointRunnable> unregisterConsumer() {
        return asyncCheckpointRunnable ->
                unregisterAsyncCheckpointRunnable(asyncCheckpointRunnable.getCheckpointId());
    }

    private boolean takeSnapshotSync(
            Map<OperatorID, OperatorSnapshotFutures> operatorSnapshotsInProgress,
            CheckpointMetaData checkpointMetaData,
            CheckpointMetricsBuilder checkpointMetrics,
            CheckpointOptions checkpointOptions,
            OperatorChain<?, ?> operatorChain,
            Supplier<Boolean> isRunning)
            throws Exception {

        checkState(!operatorChain.isClosed(),
                "OperatorChain and Task should never be closed at this point");

        long checkpointId = checkpointMetaData.getCheckpointId();
        long started = System.nanoTime();

        // 1  ChannelStateWriteResult 类中维护着 某checkpointId 下,
        //    该subtask 的所有InputChannel 和 ResultSubpartition 的状态句柄

        // 2  如果 checkpoint 是非对齐的模式, 才需要 InputChannel 和 ResultSubpartition 的状态句柄
        //    否则, 不需要, 返回一个内容为空的ChannelStateWriteResult 对象

        // 3  因为只有当checkpoint 是非对齐模式又需要保证 exactly-once 语义时,我们才需要
        //    将inputChannel 和 ResultSubpartition的状态保存
        ChannelStateWriteResult channelStateWriteResult =
                checkpointOptions.isUnalignedCheckpoint()
                        ? channelStateWriter.getAndRemoveWriteResult(checkpointId)
                        : ChannelStateWriteResult.EMPTY;

        //  创建 checkpoint 输出流的工厂,  该输出流是用来 持久化状态的
        CheckpointStreamFactory storage =
                checkpointStorage.resolveCheckpointStorageLocation(
                        checkpointId, checkpointOptions.getTargetLocation());

        try {
            //  做算子链 的  状态快照;  内部会填充 operatorSnapshotsInProgress   map集合
            //  这里还没有实际触发 快照的动作, 仅仅把快照操作封装成future对象 ,用operatorSnapshotsInProgress 暂存
            operatorChain.snapshotState(
                    operatorSnapshotsInProgress,
                    checkpointMetaData,
                    checkpointOptions,
                    isRunning,
                    channelStateWriteResult,
                    storage);

        } finally {
            checkpointStorage.clearCacheFor(checkpointId);
        }

        LOG.debug(
                "{} - finished synchronous part of checkpoint {}. Alignment duration: {} ms, snapshot duration {} ms, is unaligned checkpoint : {}",
                taskName,
                checkpointId,
                checkpointMetrics.getAlignmentDurationNanosOrDefault() / 1_000_000,
                checkpointMetrics.getSyncDurationMillis(),
                checkpointOptions.isUnalignedCheckpoint());

        checkpointMetrics.setSyncDurationMillis((System.nanoTime() - started) / 1_000_000);
        checkpointMetrics.setUnalignedCheckpoint(checkpointOptions.isUnalignedCheckpoint());
        return true;
    }

    private Set<Long> createAbortedCheckpointSetWithLimitSize(int maxRecordAbortedCheckpoints) {
        return Collections.newSetFromMap(
                new LinkedHashMap<Long, Boolean>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                        return size() > maxRecordAbortedCheckpoints;
                    }
                });
    }

    // Caches checkpoint output stream factories to prevent multiple output stream per checkpoint.
    // This could result from requesting output stream by different entities (this and
    // channelStateWriter)
    // We can't just pass a stream to the channelStateWriter because it can receive checkpoint call
    // earlier than this class
    // in some unaligned checkpoints scenarios
    private static class CachingCheckpointStorageWorkerView implements CheckpointStorageWorkerView {
        private final Map<Long, CheckpointStreamFactory> cache = new ConcurrentHashMap<>();
        private final CheckpointStorageWorkerView delegate;

        private CachingCheckpointStorageWorkerView(CheckpointStorageWorkerView delegate) {
            this.delegate = delegate;
        }

        void clearCacheFor(long checkpointId) {
            cache.remove(checkpointId);
        }

        @Override
        public CheckpointStreamFactory resolveCheckpointStorageLocation(
                long checkpointId, CheckpointStorageLocationReference reference) {
            return cache.computeIfAbsent(
                    checkpointId,
                    id -> {
                        try {
                            return delegate.resolveCheckpointStorageLocation(
                                    checkpointId, reference);
                        } catch (IOException e) {
                            throw new FlinkRuntimeException(e);
                        }
                    });
        }

        @Override
        public CheckpointStreamFactory.CheckpointStateOutputStream createTaskOwnedStateStream()
                throws IOException {
            return delegate.createTaskOwnedStateStream();
        }
    }

    private static void logCheckpointProcessingDelay(CheckpointMetaData checkpointMetaData) {
        long delay = System.currentTimeMillis() - checkpointMetaData.getReceiveTimestamp();
        if (delay >= CHECKPOINT_EXECUTION_DELAY_LOG_THRESHOLD_MS) {
            LOG.warn(
                    "Time from receiving all checkpoint barriers/RPC to executing it exceeded threshold: {}ms",
                    delay);
        }
    }
}
