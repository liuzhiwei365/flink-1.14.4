/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state;

import org.apache.flink.core.fs.CloseableRegistry;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * A class to execute a {@link SnapshotStrategy}. It can execute a strategy either synchronously or
 * asynchronously. It takes care of common logging and resource cleaning.
 *
 * @param <T> type of the snapshot result.
 */
public final class SnapshotStrategyRunner<T extends StateObject, SR extends SnapshotResources> {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotStrategyRunner.class);

    private static final String LOG_SYNC_COMPLETED_TEMPLATE =
            "{} ({}, synchronous part) in thread {} took {} ms.";
    private static final String LOG_ASYNC_COMPLETED_TEMPLATE =
            "{} ({}, asynchronous part) in thread {} took {} ms.";

    /**
     * Descriptive name of the snapshot strategy that will appear in the log outputs and {@link
     * #toString()}.
     */
    @Nonnull private final String description;

    @Nonnull private final SnapshotStrategy<T, SR> snapshotStrategy;
    @Nonnull private final CloseableRegistry cancelStreamRegistry;

    @Nonnull private final SnapshotExecutionType executionType;

    public SnapshotStrategyRunner(
            @Nonnull String description,
            @Nonnull SnapshotStrategy<T, SR> snapshotStrategy,
            @Nonnull CloseableRegistry cancelStreamRegistry,
            @Nonnull SnapshotExecutionType executionType) {
        this.description = description;
        this.snapshotStrategy = snapshotStrategy;
        this.cancelStreamRegistry = cancelStreamRegistry;
        this.executionType = executionType;
    }

    @Nonnull
    public final RunnableFuture<SnapshotResult<T>> snapshot(
            long checkpointId,
            long timestamp,
            @Nonnull CheckpointStreamFactory streamFactory,
            @Nonnull CheckpointOptions checkpointOptions)
            throws Exception {
        long startTime = System.currentTimeMillis();

        // --------------------------同步部分-----------------------------

        // SR 可以是 如下类型
        // DefaultOperatorStateBackendSnapshotResources
        //                        HeapSnapshotResources
        //                 RocksDBFullSnapshotResources
        //          IncrementalRocksDBSnapshotResources
        //                        FullSnapshotResources
        //  深拷贝一份状态的快照资源, 后面的 asyncSnapshot 方法会用到该资源, 为下一步写入快照数据做好资源准备
        SR snapshotResources = snapshotStrategy.syncPrepareResources(checkpointId);
        logCompletedInternal(LOG_SYNC_COMPLETED_TEMPLATE, streamFactory, startTime);


        // --------------------------异步部分-----------------------------

        // SnapshotStrategy 有5 个实现类:
        // DefaultOperatorStateBackendSnapshotstrategy
        // HeapSnapshotstrategy
        // RocksFullSnapshotStrategy
        // RocksIncrementalSnapshotstrategy
        // SavepointSnapshotStrategy

        //  1 将快照写入由给定 CheckpointStreamFactory 提供的输出流中 (实际写入)
        //  2 并返回  为快照提供状态句柄 的 操作
        SnapshotStrategy.SnapshotResultSupplier<T> asyncSnapshot =
                snapshotStrategy.asyncSnapshot(
                        snapshotResources,
                        checkpointId,
                        timestamp,
                        streamFactory,
                        checkpointOptions);

        FutureTask<SnapshotResult<T>> asyncSnapshotTask =
                new AsyncSnapshotCallable<SnapshotResult<T>>() {

                    // AsyncSnapshotCallable 的call 方法 会实际回调  callInternal
                    @Override
                    protected SnapshotResult<T> callInternal() throws Exception {
                        //  ****   触发核心逻辑  ****
                        //  用输出流 来持久化  状态, 并且返回 状态句柄  （这个状态句柄 回被SnapshotResul包裹）

                        // 再查看源代码的跳转时, 得特别注意, 这里的 asyncSnapshot 也 必须是 DefaultOperatorStateBackendSnapshotstrategy,
                        // HeapSnapshotstrategy,RocksFullSnapshotStrategy,RocksIncrementalSnapshotstrategy,SavepointSnapshotStrategy
                        // 这 五个实现类返回的 匿名方法
                        return asyncSnapshot.get(snapshotCloseableRegistry);
                    }

                    // AsyncSnapshotCallable 的call 方法的 finally 块中会调用cleanUp清理方法
                    // 清理方法会回调 cleanupProvidedResources
                    @Override
                    protected void cleanupProvidedResources() {
                        if (snapshotResources != null) {
                            snapshotResources.release();
                        }
                    }

                    // 打印日志,无实际作用
                    @Override
                    protected void logAsyncSnapshotComplete(long startTime) {
                        logCompletedInternal(
                                LOG_ASYNC_COMPLETED_TEMPLATE, streamFactory, startTime);
                    }
                }.toAsyncSnapshotFutureTask(cancelStreamRegistry);



        if (executionType == SnapshotExecutionType.SYNCHRONOUS) {
            // 触发 AsyncSnapshotCallable 的 callInternal 方法
            // 在 callInternal 方法中会  用输出流 来持久化  状态, 并且返回状态句柄
            asyncSnapshotTask.run();
        }

        return asyncSnapshotTask;
    }

    // 只有日志的作用
    private void logCompletedInternal(
            @Nonnull String template, @Nonnull Object checkpointOutDescription, long startTime) {

        long duration = (System.currentTimeMillis() - startTime);

        LOG.debug(template, description, checkpointOutDescription, Thread.currentThread(), duration);
    }

    @Override
    public String toString() {
        return "SnapshotStrategy {" + description + "}";
    }
}
