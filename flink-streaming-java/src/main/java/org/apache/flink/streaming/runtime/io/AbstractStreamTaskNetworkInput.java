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

package org.apache.flink.streaming.runtime.io;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.runtime.checkpoint.channel.InputChannelInfo;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.io.network.api.EndOfData;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.serialization.RecordDeserializer;
import org.apache.flink.runtime.io.network.partition.consumer.BufferOrEvent;
import org.apache.flink.runtime.io.network.partition.consumer.EndOfChannelStateEvent;
import org.apache.flink.runtime.plugable.DeserializationDelegate;
import org.apache.flink.runtime.plugable.NonReusingDeserializationDelegate;
import org.apache.flink.streaming.runtime.io.checkpointing.CheckpointedInputGate;
import org.apache.flink.streaming.runtime.streamrecord.StreamElement;
import org.apache.flink.streaming.runtime.streamrecord.StreamElementSerializer;
import org.apache.flink.streaming.runtime.watermarkstatus.StatusWatermarkValve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * Base class for network-based StreamTaskInput where each channel has a designated {@link
 * RecordDeserializer} for spanning records. Specific implementation bind it to a specific {@link
 * RecordDeserializer}.
 */
public abstract class AbstractStreamTaskNetworkInput<
                T, R extends RecordDeserializer<DeserializationDelegate<StreamElement>>>
        implements StreamTaskInput<T> {
    protected final CheckpointedInputGate checkpointedInputGate;
    protected final DeserializationDelegate<StreamElement> deserializationDelegate;
    protected final TypeSerializer<T> inputSerializer;
    protected final Map<InputChannelInfo, R> recordDeserializers;

    // 维护每个输入channel 的编号
    protected final Map<InputChannelInfo, Integer> flattenedChannelIndices = new HashMap<>();
    /** Valve that controls how watermarks and watermark statuses are forwarded. */
    // StatusWatermarkValve 对象维护各个channel的水位线，并在读取数据后向下游发送新的水位线
    protected final StatusWatermarkValve statusWatermarkValve;

    protected final int inputIndex;
    private InputChannelInfo lastChannel = null;
    // AdaptiveSpanningRecordDeserializer：适用于数据大小适中且跨段的记录的反序列化
    // SpillingAdaptiveSpanningRecordDeserializer：适用于数据大小相对较大且跨段的记录的反序列化，它支持将溢出的数据写入临时文件
    private R currentRecordDeserializer = null;

    public AbstractStreamTaskNetworkInput(
            CheckpointedInputGate checkpointedInputGate,
            TypeSerializer<T> inputSerializer,
            StatusWatermarkValve statusWatermarkValve,
            int inputIndex,
            Map<InputChannelInfo, R> recordDeserializers) {
        super();
        this.checkpointedInputGate = checkpointedInputGate;
        deserializationDelegate =
                new NonReusingDeserializationDelegate<>(
                        new StreamElementSerializer<>(inputSerializer));
        this.inputSerializer = inputSerializer;

        for (InputChannelInfo i : checkpointedInputGate.getChannelInfos()) {
            flattenedChannelIndices.put(i, flattenedChannelIndices.size());
        }

        this.statusWatermarkValve = checkNotNull(statusWatermarkValve);
        this.inputIndex = inputIndex;
        this.recordDeserializers = checkNotNull(recordDeserializers);
    }

    @Override
    public DataInputStatus emitNext(DataOutput<T> output) throws Exception {

        while (true) {
            // 如果 currentRecordDeserializer 不为null
            if (currentRecordDeserializer != null) {
                RecordDeserializer.DeserializationResult result;
                try {
                    // 该方法名取的非常不好, 应该取为 getNextRecordDeserializationResult
                    // 序列化后的数据会存在于 deserializationDelegate 的成员变量 instance中
                    // 后面processElement 方法会 拿到 instance, 然后进行处理
                    result = currentRecordDeserializer.getNextRecord(deserializationDelegate);
                } catch (IOException e) {
                    throw new IOException(
                            String.format("Can't get next record for channel %s", lastChannel), e);
                }
                if (result.isBufferConsumed()) {
                    currentRecordDeserializer = null;
                }

                if (result.isFullRecord()) {
                    // 并生产新的水位线（内部有channels数组，保存所有channel的水位线，并将最小的水位线发生到下游）
                    // 真正处理用户数据和 watermark
                    // 最终都会利用 output 来将数据发向下游
                    processElement(deserializationDelegate.getInstance(), output);
                    return DataInputStatus.MORE_AVAILABLE;
                }
            }
            // 从checkpointedInputGate中读取数据，同时会处理barrier对齐，并  checkpoint操作
            // 这里只拉取数据,真正的处理在 processElement 中
            Optional<BufferOrEvent> bufferOrEvent = checkpointedInputGate.pollNext();
            if (bufferOrEvent.isPresent()) {
                // return to the mailbox after receiving a checkpoint barrier to avoid processing of
                // data after the barrier before checkpoint is performed for unaligned checkpoint
                // mode

                if (bufferOrEvent.get().isBuffer()) {
                    // 如果是普通buffer(一个buffer中可能含有多条数据),buffer 会赋值给currentRecordDeserializer
                    // 会重重设置 currentRecordDeserializer引用, 并且将buffer 赋给其相关成员
                    // 重点来了, 这样的话, 由于是死循环的逻辑, 又得重走一遍  currentRecordDeserializer 不为空 序列化的逻辑
                    processBuffer(bufferOrEvent.get());
                } else {
                    // event 对应的是内部运行时的事件和状态控制
                    return processEvent(bufferOrEvent.get());
                }
            } else {
                if (checkpointedInputGate.isFinished()) {
                    checkState(
                            checkpointedInputGate.getAvailableFuture().isDone(),
                            "Finished BarrierHandler should be available");
                    return DataInputStatus.END_OF_INPUT;
                }
                return DataInputStatus.NOTHING_AVAILABLE;
            }
        }
    }

    private void processElement(StreamElement recordOrMark, DataOutput<T> output) throws Exception {
        if (recordOrMark.isRecord()) {
            // DataOutput  最终将数据记录交给用户算子
            output.emitRecord(recordOrMark.asRecord());
        } else if (recordOrMark.isWatermark()) {
            // 处理 的StreamElement 如果是一个WaterMark, 会更新指定的channel上 去 更新 WaterMark;
            // 然后做watermark 的对齐
            // 把各个channel中 最小的watermark 让output 发给下游
            // 处理watermark推进的类
            statusWatermarkValve.inputWatermark(
                    recordOrMark.asWatermark(), flattenedChannelIndices.get(lastChannel), output);

        } else if (recordOrMark.isLatencyMarker()) {
            output.emitLatencyMarker(recordOrMark.asLatencyMarker());
        } else if (recordOrMark.isWatermarkStatus()) {
            statusWatermarkValve.inputWatermarkStatus(
                    recordOrMark.asWatermarkStatus(),
                    flattenedChannelIndices.get(lastChannel),
                    output);
        } else {
            throw new UnsupportedOperationException("Unknown type of StreamElement");
        }
    }

    //处理运行时事件, 这里的事件不包含watermark （watermark和用户数据一起）
    protected DataInputStatus processEvent(BufferOrEvent bufferOrEvent) {
        // Event received
        final AbstractEvent event = bufferOrEvent.getEvent();
        if (event.getClass() == EndOfData.class) {
            if (checkpointedInputGate.hasReceivedEndOfData()) {
                return DataInputStatus.END_OF_DATA;
            }
        } else if (event.getClass() == EndOfPartitionEvent.class) {
            // release the record deserializer immediately,
            // which is very valuable in case of bounded stream
            releaseDeserializer(bufferOrEvent.getChannelInfo());
            if (checkpointedInputGate.isFinished()) {
                return DataInputStatus.END_OF_INPUT;
            }
        } else if (event.getClass() == EndOfChannelStateEvent.class) {
            if (checkpointedInputGate.allChannelsRecovered()) {
                return DataInputStatus.END_OF_RECOVERY;
            }
        }
        return DataInputStatus.MORE_AVAILABLE;
    }

    protected void processBuffer(BufferOrEvent bufferOrEvent) throws IOException {
        lastChannel = bufferOrEvent.getChannelInfo();
        checkState(lastChannel != null);

        // 每个channel通道中 包含一个序列化器
        currentRecordDeserializer = getActiveSerializer(bufferOrEvent.getChannelInfo());
        checkState(
                currentRecordDeserializer != null,
                "currentRecordDeserializer has already been released");
        // 把buffer 添加到 序列化器中
        currentRecordDeserializer.setNextBuffer(bufferOrEvent.getBuffer());
    }

    protected R getActiveSerializer(InputChannelInfo channelInfo) {
        return recordDeserializers.get(channelInfo);
    }

    @Override
    public int getInputIndex() {
        return inputIndex;
    }

    @Override
    public CompletableFuture<?> getAvailableFuture() {
        if (currentRecordDeserializer != null) {
            return AVAILABLE;
        }
        return checkpointedInputGate.getAvailableFuture();
    }

    @Override
    public void close() throws IOException {
        // release the deserializers . this part should not ever fail
        for (InputChannelInfo channelInfo : new ArrayList<>(recordDeserializers.keySet())) {
            releaseDeserializer(channelInfo);
        }
    }

    protected void releaseDeserializer(InputChannelInfo channelInfo) {
        R deserializer = recordDeserializers.get(channelInfo);
        if (deserializer != null) {
            // recycle buffers and clear the deserializer.
            deserializer.clear();
            recordDeserializers.remove(channelInfo);
        }
    }
}
