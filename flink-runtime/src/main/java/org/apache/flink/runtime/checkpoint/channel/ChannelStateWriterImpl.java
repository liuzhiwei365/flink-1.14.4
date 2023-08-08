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

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.state.CheckpointStorageWorkerView;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.util.CloseableIterator;
import org.apache.flink.util.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.flink.runtime.checkpoint.channel.ChannelStateWriteRequest.completeInput;
import static org.apache.flink.runtime.checkpoint.channel.ChannelStateWriteRequest.completeOutput;
import static org.apache.flink.runtime.checkpoint.channel.ChannelStateWriteRequest.write;

/**
 * {@link ChannelStateWriter} implemented using {@link
 * CheckpointStreamFactory.CheckpointStateOutputStream CheckpointStateOutputStreams}. Internally, it
 * has by default
 *
 * <ul>
 *   <li>one stream per checkpoint; having multiple streams would mean more files written and more
 *       connections opened (and more latency on restore)
 *   <li>one thread; having multiple threads means more connections, couples with the implementation
 *       and increases complexity
 * </ul>
 *
 * <p>Thread-safety: this class is thread-safe when used with a thread-safe {@link
 * ChannelStateWriteRequestExecutor executor} (e.g. default {@link
 * ChannelStateWriteRequestExecutorImpl}.
 */
@Internal
@ThreadSafe
public class ChannelStateWriterImpl implements ChannelStateWriter {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelStateWriterImpl.class);
    private static final int DEFAULT_MAX_CHECKPOINTS =
            1000; // includes max-concurrent-checkpoints + checkpoints to be aborted (scheduled via
    // mailbox)

    private final String taskName;
    // 实现类是  ChannelStateWriteRequestExecutorImpl , 内部有个双向队列
    private final ChannelStateWriteRequestExecutor executor;

    // ChannelStateWriteResult 类中维护着 某checkpointId 下,
    // 该subtask 的所有InputChannel 和 ResultSubpartition 的状态句柄
    private final ConcurrentMap<Long, ChannelStateWriteResult> results;
    private final int maxCheckpoints;

    /**
     * Creates a {@link ChannelStateWriterImpl} with {@link #DEFAULT_MAX_CHECKPOINTS} as {@link
     * #maxCheckpoints}.
     */
    public ChannelStateWriterImpl(
            String taskName, int subtaskIndex, CheckpointStorageWorkerView streamFactoryResolver) {
        this(taskName, subtaskIndex, streamFactoryResolver, DEFAULT_MAX_CHECKPOINTS);
    }

    /**
     * Creates a {@link ChannelStateWriterImpl} with {@link ChannelStateSerializerImpl default}
     * {@link ChannelStateSerializer}, and a {@link ChannelStateWriteRequestExecutorImpl}.
     *
     * @param taskName
     * @param streamFactoryResolver a factory to obtain output stream factory for a given checkpoint
     * @param maxCheckpoints maximum number of checkpoints to be written currently or finished but
     *     not taken yet.
     */
    ChannelStateWriterImpl(
            String taskName,
            int subtaskIndex,
            CheckpointStorageWorkerView streamFactoryResolver,
            int maxCheckpoints) {
        this(
                taskName,
                new ConcurrentHashMap<>(maxCheckpoints),
                new ChannelStateWriteRequestExecutorImpl(
                        taskName,
                        new ChannelStateWriteRequestDispatcherImpl(
                                taskName,
                                subtaskIndex,
                                streamFactoryResolver,
                                new ChannelStateSerializerImpl())),
                maxCheckpoints);
    }

    ChannelStateWriterImpl(
            String taskName,
            ConcurrentMap<Long, ChannelStateWriteResult> results,
            ChannelStateWriteRequestExecutor executor,
            int maxCheckpoints) {
        this.taskName = taskName;
        this.results = results;
        this.maxCheckpoints = maxCheckpoints;
        this.executor = executor;
    }

    @Override
    public void start(long checkpointId, CheckpointOptions checkpointOptions) {
        LOG.debug("{} starting checkpoint {} ({})", taskName, checkpointId, checkpointOptions);
        //
        ChannelStateWriteResult result = new ChannelStateWriteResult();
        ChannelStateWriteResult put =
                results.computeIfAbsent(
                        checkpointId,
                        id -> {
                            Preconditions.checkState(
                                    results.size() < maxCheckpoints,
                                    String.format(
                                            "%s can't start %d, results.size() > maxCheckpoints: %d > %d",
                                            taskName,
                                            checkpointId,
                                            results.size(),
                                            maxCheckpoints));
                            enqueue(
                                    new CheckpointStartRequest(
                                            checkpointId,
                                            result,
                                            checkpointOptions.getTargetLocation()),
                                    false);
                            return result;
                        });
        Preconditions.checkArgument(
                put == result,
                taskName + " result future already present for checkpoint " + checkpointId);
    }

    //异步持久化 InputChannel 状态
    @Override
    public void addInputData(
            long checkpointId,
            InputChannelInfo info,
            int startSeqNum,
            CloseableIterator<Buffer> iterator) {
        LOG.trace(
                "{} adding input data, checkpoint {}, channel: {}, startSeqNum: {}",
                taskName,
                checkpointId,
                info,
                startSeqNum);
        // 1 构建 InputChannel的 buffer 状态的 写出请求  （写入请求里面封装了具体的 buffer 的 checkpoint写出逻辑 ）
        // 2 放入 ChannelStateWriteRequestExecutorImpl.deque 队列里面排队
        // 3 ChannelStateWriteRequestExecutorImpl.loop()方法  会不断的到队列中拿请求元素执行
        enqueue(write(checkpointId, info, iterator), false);
    }

    //异步持久化 ResultSubpartition 状态,  参见addInputData 方法
    @Override
    public void addOutputData(
            long checkpointId, ResultSubpartitionInfo info, int startSeqNum, Buffer... data) {
        LOG.trace(
                "{} adding output data, checkpoint {}, channel: {}, startSeqNum: {}, num buffers: {}",
                taskName,
                checkpointId,
                info,
                startSeqNum,
                data == null ? 0 : data.length);
        enqueue(write(checkpointId, info, data), false);
    }

    @Override
    public void finishInput(long checkpointId) {
        LOG.debug("{} finishing input data, checkpoint {}", taskName, checkpointId);
        // 添加  CheckpointInProgressRequest 对象 到队列
        // CheckpointInProgressRequest 封装的 实际逻辑是 ChannelStateCheckpointWriter::completeInput
        //    它会刷写然后关流 ,并且持有  inputChannel 通道状态句柄, 即InputChannelStateHandle 对象
        enqueue(completeInput(checkpointId), false);
    }

    @Override
    public void finishOutput(long checkpointId) {
        LOG.debug("{} finishing output data, checkpoint {}", taskName, checkpointId);
        // 添加  CheckpointInProgressRequest 对象 到队列
        // CheckpointInProgressRequest 封装的 实际逻辑是 ChannelStateCheckpointWriter::completeOutput
        //    它会刷写然后关流 ,并且持有  ResultSubpartition 通道状态句柄,  即ResultSubpartitionStateHandle 对象
        enqueue(completeOutput(checkpointId), false);
    }

    @Override
    public void abort(long checkpointId, Throwable cause, boolean cleanup) {
        LOG.debug("{} aborting, checkpoint {}", taskName, checkpointId);
        enqueue(ChannelStateWriteRequest.abort(checkpointId, cause), true); // abort already started
        enqueue(
                ChannelStateWriteRequest.abort(checkpointId, cause),
                false); // abort enqueued but not started
        if (cleanup) {
            results.remove(checkpointId);
        }
    }

    @Override
    public ChannelStateWriteResult getAndRemoveWriteResult(long checkpointId) {
        LOG.debug("{} requested write result, checkpoint {}", taskName, checkpointId);
        ChannelStateWriteResult result = results.remove(checkpointId);
        Preconditions.checkArgument(
                result != null,
                taskName + " channel state write result not found for checkpoint " + checkpointId);
        return result;
    }

    public void open() {
        executor.start();
    }

    @Override
    public void close() throws IOException {
        LOG.debug("close, dropping checkpoints {}", results.keySet());
        results.clear();
        executor.close();
    }

    private void enqueue(ChannelStateWriteRequest request, boolean atTheFront) {
        // state check and previous errors check are performed inside the worker
        try {
            if (atTheFront) {
                executor.submitPriority(request);
            } else {
                executor.submit(request);
            }
        } catch (Exception e) {
            RuntimeException wrapped = new RuntimeException("unable to send request to worker", e);
            try {
                request.cancel(e);
            } catch (Exception cancelException) {
                wrapped.addSuppressed(cancelException);
            }
            throw wrapped;
        }
    }
}
