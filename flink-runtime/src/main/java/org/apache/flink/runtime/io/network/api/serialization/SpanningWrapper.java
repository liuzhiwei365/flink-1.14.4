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

package org.apache.flink.runtime.io.network.api.serialization;

import org.apache.flink.core.fs.RefCountedFile;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataInputViewStreamWrapper;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.core.memory.MemorySegmentFactory;
import org.apache.flink.runtime.io.disk.FileBasedBufferIterator;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.FreeingBufferRecycler;
import org.apache.flink.runtime.io.network.buffer.NetworkBuffer;
import org.apache.flink.util.CloseableIterator;
import org.apache.flink.util.StringUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.flink.core.memory.MemorySegmentFactory.wrapCopy;
import static org.apache.flink.core.memory.MemorySegmentFactory.wrapInt;
import static org.apache.flink.runtime.io.network.api.serialization.NonSpanningWrapper.singleBufferIterator;
import static org.apache.flink.runtime.io.network.api.serialization.SpillingAdaptiveSpanningRecordDeserializer.LENGTH_BYTES;
import static org.apache.flink.util.CloseableIterator.empty;
import static org.apache.flink.util.FileUtils.writeCompletely;
import static org.apache.flink.util.IOUtils.closeQuietly;

final class SpanningWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(SpanningWrapper.class);

    private final byte[] initialBuffer = new byte[1024];

    private String[] tempDirs;

    private final Random rnd = new Random();

    private final DataInputDeserializer serializationReadBuffer;

    final ByteBuffer lengthBuffer;// 初始化的时候只申请4个字节

    private final int fileBufferSize;

    private FileChannel spillingChannel;

    private byte[] buffer;

    private int recordLength;

    private int accumulatedRecordBytes;

    private MemorySegment leftOverData;

    private int leftOverStart;

    private int leftOverLimit;

    private RefCountedFile spillFile;

    private DataInputViewStreamWrapper spillFileReader;

    private final int thresholdForSpilling;

    SpanningWrapper(String[] tempDirectories, int threshold, int fileBufferSize) {
        tempDirs = tempDirectories;
        // 只申请4个字节
        lengthBuffer = ByteBuffer.allocate(LENGTH_BYTES);
        // 设置为大端存储
        lengthBuffer.order(ByteOrder.BIG_ENDIAN);
        recordLength = -1;
        serializationReadBuffer = new DataInputDeserializer();
        buffer = initialBuffer;
        thresholdForSpilling = threshold;
        this.fileBufferSize = fileBufferSize;
    }

    // 将NonSpanningWrapper中剩下的数据 转移到 SpanningWrapper 中
    void transferFrom(NonSpanningWrapper partial, int nextRecordLength) throws IOException {
        updateLength(nextRecordLength);
        // 利用 updateLength 中创建的文件通道 溢写 partial数据; 或者 将 partial数据写到扩容的buffer
        accumulatedRecordBytes =
                isAboveSpillingThreshold() ? spill(partial) : partial.copyContentTo(buffer);
        // 清空 partial数据
        partial.clear();
    }

    private boolean isAboveSpillingThreshold() {
        return recordLength > thresholdForSpilling;
    }

    void addNextChunkFromMemorySegment(MemorySegment segment, int offset, int numBytes)
            throws IOException {
        int limit = offset + numBytes;

        // numBytesRead 大概率等于4
        int numBytesRead = isReadingLength() ? readLength(segment, offset, numBytes) : 0;

        offset += numBytesRead;
        numBytes -= numBytesRead;
        if (numBytes == 0) {
            return;
        }

        int toCopy = min(recordLength - accumulatedRecordBytes, numBytes);
        if (toCopy > 0) {
            // 将segment 的数据拷贝到 buffer 或者 spillFile 中
            copyFromSegment(segment, offset, toCopy);
        }
        if (numBytes > toCopy) {
            // 这说明 入参 segment中的 内容没有被消费完
            // numBytes > recordLength - accumulatedRecordBytes
            leftOverData = segment;
            leftOverStart = offset + toCopy;
            leftOverLimit = limit;
        }
    }

    private void copyFromSegment(MemorySegment segment, int offset, int length) throws IOException {
        if (spillingChannel == null) {
            copyIntoBuffer(segment, offset, length);
        } else {
            copyIntoFile(segment, offset, length);
        }
    }

    private void copyIntoFile(MemorySegment segment, int offset, int length) throws IOException {
        writeCompletely(spillingChannel, segment.wrap(offset, length));
        accumulatedRecordBytes += length;
        if (hasFullRecord()) {
            spillingChannel.close();
            spillFileReader =
                    new DataInputViewStreamWrapper(
                            new BufferedInputStream(
                                    new FileInputStream(spillFile.getFile()), fileBufferSize));
        }
    }

    private void copyIntoBuffer(MemorySegment segment, int offset, int length) {
        // 将 segment 的 内容copy 到 buffer 中
        segment.get(offset, buffer, accumulatedRecordBytes, length);
        // 更新 累计记录字节数
        accumulatedRecordBytes += length;
        if (hasFullRecord()) {
            serializationReadBuffer.setBuffer(buffer, 0, recordLength);
        }
    }

    // 把 入参segment 读到  成员变量 lengthBuffer 中 ; lengthBuffer 是 ByteBuffer 对象
    private int readLength(MemorySegment segment, int segmentPosition, int segmentRemaining)
            throws IOException {
        // 大多数情况下 bytesToRead = 4
        int bytesToRead = min(lengthBuffer.remaining(), segmentRemaining);

        // 将本 segment的内容拷贝到  lengthBuffer 对象中
        segment.get(segmentPosition, lengthBuffer, bytesToRead);
        if (!lengthBuffer.hasRemaining()) {
            // lengthBuffer 总共只有4个字节
            updateLength(lengthBuffer.getInt(0));
        }
        return bytesToRead;
    }

    private void updateLength(int length) throws IOException {
        lengthBuffer.clear();
        recordLength = length;
        if (isAboveSpillingThreshold()) {
            // 如果记录大小 超过 溢出阈值
            // 先创建随机访问文件,再利用随机访问文件拿到 通道对象
            spillingChannel = createSpillingChannel();
        } else {
            // buffer字节数组 扩容 （如果需要的化）
            ensureBufferCapacity(length);
        }
    }

    CloseableIterator<Buffer> getUnconsumedSegment() throws IOException {
        if (isReadingLength()) {
            return singleBufferIterator(wrapCopy(lengthBuffer.array(), 0, lengthBuffer.position()));
        } else if (isAboveSpillingThreshold()) {
            return createSpilledDataIterator();
        } else if (recordLength == -1) {
            return empty(); // no remaining partial length or data
        } else {
            return singleBufferIterator(copyDataBuffer());
        }
    }

    @SuppressWarnings("unchecked")
    private CloseableIterator<Buffer> createSpilledDataIterator() throws IOException {
        if (spillingChannel != null && spillingChannel.isOpen()) {
            spillingChannel.force(false);
        }
        return CloseableIterator.flatten(
                toSingleBufferIterator(wrapInt(recordLength)),
                new FileBasedBufferIterator(
                        spillFile, min(accumulatedRecordBytes, recordLength), fileBufferSize),
                leftOverData == null
                        ? empty()
                        : toSingleBufferIterator(
                                wrapCopy(leftOverData.getArray(), leftOverStart, leftOverLimit)));
    }

    private MemorySegment copyDataBuffer() throws IOException {
        int leftOverSize = leftOverLimit - leftOverStart;
        int unconsumedSize = LENGTH_BYTES + accumulatedRecordBytes + leftOverSize;
        DataOutputSerializer serializer = new DataOutputSerializer(unconsumedSize);
        serializer.writeInt(recordLength);
        serializer.write(buffer, 0, accumulatedRecordBytes);
        if (leftOverData != null) {
            serializer.write(leftOverData, leftOverStart, leftOverSize);
        }
        MemorySegment segment = MemorySegmentFactory.allocateUnpooledSegment(unconsumedSize);
        segment.put(0, serializer.getSharedBuffer(), 0, segment.size());
        return segment;
    }

    /** Copies the leftover data and transfers the "ownership" (i.e. clears this wrapper). */
    void transferLeftOverTo(NonSpanningWrapper nonSpanningWrapper) {
        nonSpanningWrapper.clear();
        if (leftOverData != null) {
            //  拿 本SpanningWrapper 对象的 MemorySegment 信息赋给 nonSpanningWrapper的相关引用, 达到类似高效 copy的效果
            nonSpanningWrapper.initializeFromMemorySegment(
                    leftOverData, leftOverStart, leftOverLimit);
        }
        clear();
    }

    boolean hasFullRecord() {
        // 已经累计的记录字节 大于 记录长度
        return recordLength >= 0 && accumulatedRecordBytes >= recordLength;
    }

    // 如果跨越内存段的 记录 的 反序列化  没有正在进行
    // 则 一定 accumulatedRecordBytes ==0  recordLength== -1  lengthBuffer.position()==0  这三点严格成立
    int getNumGatheredBytes() {
        return accumulatedRecordBytes
                + (recordLength >= 0 ? LENGTH_BYTES : lengthBuffer.position());
    }

    public void clear() {
        buffer = initialBuffer;
        serializationReadBuffer.releaseArrays();

        recordLength = -1;
        lengthBuffer.clear();
        leftOverData = null;
        leftOverStart = 0;
        leftOverLimit = 0;
        accumulatedRecordBytes = 0;

        if (spillingChannel != null) {
            closeQuietly(spillingChannel);
        }
        if (spillFileReader != null) {
            closeQuietly(spillFileReader);
        }
        if (spillFile != null) {
            // It's important to avoid AtomicInteger access inside `release()` on the hot path
            closeQuietly(() -> spillFile.release());
        }

        spillingChannel = null;
        spillFileReader = null;
        spillFile = null;
    }

    public DataInputView getInputView() {
        return spillFileReader == null ? serializationReadBuffer : spillFileReader;
    }

    private void ensureBufferCapacity(int minLength) {
        if (buffer.length < minLength) {
            // 构造一个新数组扩容
            byte[] newBuffer = new byte[max(minLength, buffer.length * 2)];
            System.arraycopy(buffer, 0, newBuffer, 0, accumulatedRecordBytes);
            buffer = newBuffer;
        }
    }

    @SuppressWarnings("resource")
    private FileChannel createSpillingChannel() throws IOException {
        if (spillFile != null) {
            throw new IllegalStateException("Spilling file already exists.");
        }

        // try to find a unique file name for the spilling channel
        int maxAttempts = 10;
        int initialDirIndex = rnd.nextInt(tempDirs.length);
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int dirIndex = (initialDirIndex + attempt) % tempDirs.length;
            String directory = tempDirs[dirIndex];
            File file = new File(directory, randomString(rnd) + ".inputchannel");
            try {
                if (file.createNewFile()) {
                    spillFile = new RefCountedFile(file);

                    // 创建随机访问文件对象
                    return new RandomAccessFile(file, "rw").getChannel();
                }
            } catch (IOException e) {
                // if there is no tempDir left to try
                if (tempDirs.length <= 1) {
                    throw e;
                }
                LOG.warn(
                        "Caught an IOException when creating spill file: "
                                + directory
                                + ". Attempt "
                                + attempt,
                        e);
                tempDirs = ArrayUtils.remove(tempDirs, dirIndex);
            }
        }

        throw new IOException(
                "Could not find a unique file channel name in '"
                        + Arrays.toString(tempDirs)
                        + "' for spilling large records during deserialization.");
    }

    private static String randomString(Random random) {
        final byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return StringUtils.byteToHexString(bytes);
    }

    private int spill(NonSpanningWrapper partial) throws IOException {
        ByteBuffer buffer = partial.wrapIntoByteBuffer();
        int length = buffer.remaining();
        writeCompletely(spillingChannel, buffer);
        return length;
    }

    private boolean isReadingLength() {
        return lengthBuffer.position() > 0;
    }

    private static CloseableIterator<Buffer> toSingleBufferIterator(MemorySegment segment) {
        NetworkBuffer buffer =
                new NetworkBuffer(
                        segment,
                        FreeingBufferRecycler.INSTANCE,
                        Buffer.DataType.DATA_BUFFER,
                        segment.size());
        return CloseableIterator.ofElement(buffer, Buffer::recycleBuffer);
    }
}
