/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.io.network.api.serialization;

import org.apache.flink.core.io.IOReadableWritable;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.util.CloseableIterator;

import javax.annotation.Nullable;

import java.io.IOException;

import static org.apache.flink.runtime.io.network.api.serialization.RecordDeserializer.DeserializationResult.INTERMEDIATE_RECORD_FROM_BUFFER;
import static org.apache.flink.runtime.io.network.api.serialization.RecordDeserializer.DeserializationResult.LAST_RECORD_FROM_BUFFER;
import static org.apache.flink.runtime.io.network.api.serialization.RecordDeserializer.DeserializationResult.PARTIAL_RECORD;

/** @param <T> The type of the record to be deserialized. */
public class SpillingAdaptiveSpanningRecordDeserializer<T extends IOReadableWritable>
        implements RecordDeserializer<T> {
    public static final int DEFAULT_THRESHOLD_FOR_SPILLING = 5 * 1024 * 1024; // 5 MiBytes
    public static final int DEFAULT_FILE_BUFFER_SIZE = 2 * 1024 * 1024;
    private static final int MIN_THRESHOLD_FOR_SPILLING = 100 * 1024; // 100 KiBytes
    private static final int MIN_FILE_BUFFER_SIZE = 50 * 1024; // 50 KiBytes

    // 用于表示二进制形式的int值 的 字节数 ,也就是4
    static final int LENGTH_BYTES = Integer.BYTES;

    //前者将数据存储进内存
    private final NonSpanningWrapper nonSpanningWrapper;
    //后者存储前者存不下的部分内存数据以及将 超量数据 溢出到磁盘
    private final SpanningWrapper spanningWrapper;

    @Nullable private Buffer currentBuffer;

    public SpillingAdaptiveSpanningRecordDeserializer(String[] tmpDirectories) {
        this(tmpDirectories, DEFAULT_THRESHOLD_FOR_SPILLING, DEFAULT_FILE_BUFFER_SIZE);
    }

    public SpillingAdaptiveSpanningRecordDeserializer(
            String[] tmpDirectories, int thresholdForSpilling, int fileBufferSize) {
        nonSpanningWrapper = new NonSpanningWrapper();
        spanningWrapper =
                new SpanningWrapper(
                        tmpDirectories,
                        Math.max(thresholdForSpilling, MIN_THRESHOLD_FOR_SPILLING),
                        Math.max(fileBufferSize, MIN_FILE_BUFFER_SIZE));
    }

    @Override
    public void setNextBuffer(Buffer buffer) throws IOException {
        currentBuffer = buffer;

        int offset = buffer.getMemorySegmentOffset();
        MemorySegment segment = buffer.getMemorySegment();
        int numBytes = buffer.getSize();

        // check if some spanning record deserialization is pending
        if (spanningWrapper.getNumGatheredBytes() > 0) {
            // 如果存在  跨越内存段的 记录 的 反序列化正在进行
            spanningWrapper.addNextChunkFromMemorySegment(segment, offset, numBytes);
        } else {
            nonSpanningWrapper.initializeFromMemorySegment(segment, offset, numBytes + offset);
        }
    }

    @Override
    public CloseableIterator<Buffer> getUnconsumedBuffer() throws IOException {
        return nonSpanningWrapper.hasRemaining()
                ? nonSpanningWrapper.getUnconsumedSegment()
                : spanningWrapper.getUnconsumedSegment();
    }

    @Override
    public DeserializationResult getNextRecord(T target) throws IOException {
        //始终 首先检查非跨越包装器
        //这应该是小记录的大多数情况
        //对于大型唱片来说，无论如何，这部分作品与之相比都很小
        final DeserializationResult result = readNextRecord(target);
        if (result.isBufferConsumed()) {
            currentBuffer.recycleBuffer();
            currentBuffer = null;
        }
        return result;
    }

    // target 可以是 ReusingDeserializationDelegate,也可以是NonReusingDeserializationDelegate
    private DeserializationResult readNextRecord(T target) throws IOException {
        if (nonSpanningWrapper.hasCompleteLength()) {
            // 如果 nonSpanningWrapper 超过4 个字节长度
            return readNonSpanningRecord(target);

        } else if (nonSpanningWrapper.hasRemaining()) {
            // 如果 nonSpanningWrapper 没有超过4个字节长度
            // 将 nonSpanningWrapper 剩下来的字节转移 到 spanningWrapper 的 lengthBuffer成员变量
            nonSpanningWrapper.transferTo(spanningWrapper.lengthBuffer);
            return PARTIAL_RECORD;

        } else if (spanningWrapper.hasFullRecord()) {
            // 如果 spanningWrapper 中有完整的记录
            target.read(spanningWrapper.getInputView());
            // 把剩下的字节转移到 nonSpanningWrapper 中
            spanningWrapper.transferLeftOverTo(nonSpanningWrapper);
            return nonSpanningWrapper.hasRemaining()
                    ? INTERMEDIATE_RECORD_FROM_BUFFER
                    : LAST_RECORD_FROM_BUFFER;

        } else {
            return PARTIAL_RECORD;
        }
    }

    private DeserializationResult readNonSpanningRecord(T target) throws IOException {

        int recordLen = nonSpanningWrapper.readInt();
        if (nonSpanningWrapper.canReadRecord(recordLen)) {
            return nonSpanningWrapper.readInto(target);
        } else {
            //spanningWrapper 为跨区包装器,只有在nonSpanningWrapper 无法序列化出完整的StreamRecord时,
            //才会转移到 spanningWrapper中等待buffer 数据接入,最终会将多个buffer 拼接以 序列化出完整的StreamRecord
            spanningWrapper.transferFrom(nonSpanningWrapper, recordLen);
            return PARTIAL_RECORD;
        }
    }

    @Override
    public void clear() {
        if (currentBuffer != null && !currentBuffer.isRecycled()) {
            currentBuffer.recycleBuffer();
            currentBuffer = null;
        }
        nonSpanningWrapper.clear();
        spanningWrapper.clear();
    }
}
