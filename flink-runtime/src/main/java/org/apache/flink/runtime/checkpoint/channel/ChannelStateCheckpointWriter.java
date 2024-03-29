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

package org.apache.flink.runtime.checkpoint.channel;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter.ChannelStateWriteResult;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.logger.NetworkActionsLogger;
import org.apache.flink.runtime.state.AbstractChannelStateHandle;
import org.apache.flink.runtime.state.AbstractChannelStateHandle.StateContentMetaInfo;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.runtime.state.CheckpointStreamFactory.CheckpointStateOutputStream;
import org.apache.flink.runtime.state.InputChannelStateHandle;
import org.apache.flink.runtime.state.ResultSubpartitionStateHandle;
import org.apache.flink.runtime.state.StreamStateHandle;
import org.apache.flink.runtime.state.memory.ByteStreamStateHandle;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.function.RunnableWithException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.flink.runtime.state.CheckpointedStateScope.EXCLUSIVE;
import static org.apache.flink.util.ExceptionUtils.findThrowable;
import static org.apache.flink.util.ExceptionUtils.rethrow;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/** Writes channel state for a specific checkpoint-subtask-attempt triple. */

// flink 在做快照时 ，各个channel 通道也会做

// 本类负责 做 channel 的快照保存
@NotThreadSafe
class ChannelStateCheckpointWriter {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelStateCheckpointWriter.class);

    private final DataOutputStream dataStream;
    private final CheckpointStateOutputStream checkpointStream;
    private final ChannelStateWriteResult result;
    private final Map<InputChannelInfo, StateContentMetaInfo> inputChannelOffsets = new HashMap<>();
    private final Map<ResultSubpartitionInfo, StateContentMetaInfo> resultSubpartitionOffsets =
            new HashMap<>();
    private final ChannelStateSerializer serializer;
    private final long checkpointId;
    private boolean allInputsReceived = false;
    private boolean allOutputsReceived = false;
    private final RunnableWithException onComplete;
    private final int subtaskIndex;
    private String taskName;

    ChannelStateCheckpointWriter(
            String taskName,
            int subtaskIndex,
            CheckpointStartRequest startCheckpointItem,
            CheckpointStreamFactory streamFactory,
            ChannelStateSerializer serializer,
            RunnableWithException onComplete)
            throws Exception {
        this(
                taskName,
                subtaskIndex,
                startCheckpointItem.getCheckpointId(),
                startCheckpointItem.getTargetResult(),
                streamFactory.createCheckpointStateOutputStream(EXCLUSIVE),
                serializer,
                onComplete);
    }

    @VisibleForTesting
    ChannelStateCheckpointWriter(
            String taskName,
            int subtaskIndex,
            long checkpointId,
            ChannelStateWriteResult result,
            CheckpointStateOutputStream stream,
            ChannelStateSerializer serializer,
            RunnableWithException onComplete) {
        this(
                taskName,
                subtaskIndex,
                checkpointId,
                result,
                serializer,
                onComplete,
                stream,
                new DataOutputStream(stream));
    }

    @VisibleForTesting
    ChannelStateCheckpointWriter(
            String taskName,
            int subtaskIndex,
            long checkpointId,
            ChannelStateWriteResult result,
            ChannelStateSerializer serializer,
            RunnableWithException onComplete,
            CheckpointStateOutputStream checkpointStateOutputStream,
            DataOutputStream dataStream) {
        this.taskName = taskName;
        this.subtaskIndex = subtaskIndex;
        this.checkpointId = checkpointId;
        this.result = checkNotNull(result);
        this.checkpointStream = checkNotNull(checkpointStateOutputStream);
        this.serializer = checkNotNull(serializer);
        this.dataStream = checkNotNull(dataStream);
        this.onComplete = checkNotNull(onComplete);
        runWithChecks(() -> serializer.writeHeader(dataStream));
    }

    // 保存 inputChannel 中 暂存的buffer
    void writeInput(InputChannelInfo info, Buffer buffer) {
        write(
                inputChannelOffsets,
                info,
                buffer,
                !allInputsReceived,
                "ChannelStateCheckpointWriter#writeInput");
    }

    // 保存 ResultSubpartition中 暂存的buffer
    void writeOutput(ResultSubpartitionInfo info, Buffer buffer) {
        write(
                resultSubpartitionOffsets,
                info,
                buffer,
                !allOutputsReceived,
                "ChannelStateCheckpointWriter#writeOutput");
    }

    private <K> void write(
            Map<K, StateContentMetaInfo> offsets,
            K key,
            Buffer buffer,
            boolean precondition,
            String action) {
        try {
            if (result.isDone()) {
                return;
            }
            runWithChecks(
                    () -> {
                        checkState(precondition);
                        long offset = checkpointStream.getPos();
                        try (AutoCloseable ignored =
                                NetworkActionsLogger.measureIO(action, buffer)) { // 内部会记录本次写出的花费时间

                            // 核心,  将buffer 写出到 checkpoint 目录 , 该
                            serializer.writeData(dataStream, buffer);
                        }

                        // 更新  offsets
                        long size = checkpointStream.getPos() - offset;
                        offsets.computeIfAbsent(key, unused -> new StateContentMetaInfo())
                                .withDataAdded(offset, size);

                        // 日志
                        NetworkActionsLogger.tracePersist( action, buffer, taskName, key, checkpointId);
                    });
        } finally {
            buffer.recycleBuffer();
        }
    }

    void completeInput() throws Exception {
        LOG.debug("complete input, output completed: {}", allOutputsReceived);
        // 刷写然后关流 ,并且保存  inputChannel 通道状态句柄
        complete(!allInputsReceived, () -> allInputsReceived = true);
    }

    void completeOutput() throws Exception {
        LOG.debug("complete output, input completed: {}", allInputsReceived);
        // 刷写然后关流 ,并且保存  ResultSubpartition 通道状态句柄
        complete(!allOutputsReceived, () -> allOutputsReceived = true);
    }

    // 刷写然后关流 ,并且得到通道状态句柄
    private void complete(boolean precondition, RunnableWithException complete) throws Exception {
        if (result.isDone()) {
            // likely after abort - only need to set the flag run onComplete callback
            doComplete(precondition, complete, onComplete);
        } else {
            runWithChecks(
                    () ->
                            doComplete(
                                    precondition,
                                    complete,
                                    onComplete,
                                    this::finishWriteAndResult)); //核心  刷写然后关流 ,并且得到通道状态句柄
        }
    }

    // 主要流程:
    //    1 刷写,完成写入
    //    2 关流
    //    3 获取 状态句柄
    //    4 触发 future 对象 , 结果保存在  result 成员变量中
    private void finishWriteAndResult() throws IOException {
        // short cut
        if (inputChannelOffsets.isEmpty() && resultSubpartitionOffsets.isEmpty()) {
            dataStream.close();
            result.inputChannelStateHandles.complete(emptyList());
            result.resultSubpartitionStateHandles.complete(emptyList());
            return;
        }
        // dataStream 和 checkpointStream 本质上是同一个流 ,可参见相关构造方法

        // 刷写
        dataStream.flush();

        // 关流, 并且获取 底层的 状态流句柄
        StreamStateHandle underlying = checkpointStream.closeAndGetHandle();

        //  构建上层的  AbstractChannelStateHandle  通道状态句柄
        //      AbstractChannelStateHandle有两个实现：   InputChannelStateHandle 和 ResultSubpartitionStateHandle
        //  触发 所有相关的异步 future 对象 , 结果保存在  result 成员变量中
        complete(
                underlying,
                result.inputChannelStateHandles,
                inputChannelOffsets,
                HandleFactory.INPUT_CHANNEL);
        complete(
                underlying,
                result.resultSubpartitionStateHandles,
                resultSubpartitionOffsets,
                HandleFactory.RESULT_SUBPARTITION);
    }

    private void doComplete(
            boolean precondition,
            RunnableWithException complete,
            RunnableWithException... callbacks)
            throws Exception {
        Preconditions.checkArgument(precondition);
        complete.run();
        if (allInputsReceived && allOutputsReceived) {
            for (RunnableWithException callback : callbacks) {
                callback.run();
            }
        }
    }

    private <I, H extends AbstractChannelStateHandle<I>> void complete(
            StreamStateHandle underlying,
            CompletableFuture<Collection<H>> future,
            Map<I, StateContentMetaInfo> offsets,
            HandleFactory<I, H> handleFactory)
            throws IOException {
        final Collection<H> handles = new ArrayList<>();
        for (Map.Entry<I, StateContentMetaInfo> e : offsets.entrySet()) {
            handles.add(createHandle(handleFactory, underlying, e.getKey(), e.getValue()));
        }
        future.complete(handles);
        LOG.debug(
                "channel state write completed, checkpointId: {}, handles: {}",
                checkpointId,
                handles);
    }

    private <I, H extends AbstractChannelStateHandle<I>> H createHandle(
            HandleFactory<I, H> handleFactory,
            StreamStateHandle underlying,
            I channelInfo,
            StateContentMetaInfo contentMetaInfo)
            throws IOException {
        Optional<byte[]> bytes =
                underlying.asBytesIfInMemory(); // todo: consider restructuring channel state and
        // removing this method:
        // https://issues.apache.org/jira/browse/FLINK-17972
        if (bytes.isPresent()) {
            StreamStateHandle extracted =
                    new ByteStreamStateHandle(
                            randomUUID().toString(),
                            serializer.extractAndMerge(bytes.get(), contentMetaInfo.getOffsets()));

            return handleFactory.create(
                    subtaskIndex,
                    channelInfo,
                    extracted,
                    singletonList(serializer.getHeaderLength()),
                    extracted.getStateSize());
        } else {
            return handleFactory.create(
                    subtaskIndex,
                    channelInfo,
                    underlying,
                    contentMetaInfo.getOffsets(),
                    contentMetaInfo.getSize());
        }
    }

    private void runWithChecks(RunnableWithException r) {
        try {
            checkState(!result.isDone(), "result is already completed", result);
            r.run();
        } catch (Exception e) {
            fail(e);
            if (!findThrowable(e, IOException.class).isPresent()) {
                rethrow(e);
            }
        }
    }

    public void fail(Throwable e) {
        result.fail(e);
        try {
            checkpointStream.close();
        } catch (Exception closeException) {
            String message = "Unable to close checkpointStream after a failure";
            if (findThrowable(closeException, IOException.class).isPresent()) {
                LOG.warn(message, closeException);
            } else {
                throw new RuntimeException(message, closeException);
            }
        }
    }

    private interface HandleFactory<I, H extends AbstractChannelStateHandle<I>> {
        H create(
                int subtaskIndex,
                I info,
                StreamStateHandle underlying,
                List<Long> offsets,
                long size);

        HandleFactory<InputChannelInfo, InputChannelStateHandle> INPUT_CHANNEL =
                InputChannelStateHandle::new;

        HandleFactory<ResultSubpartitionInfo, ResultSubpartitionStateHandle> RESULT_SUBPARTITION =
                ResultSubpartitionStateHandle::new;
    }
}
