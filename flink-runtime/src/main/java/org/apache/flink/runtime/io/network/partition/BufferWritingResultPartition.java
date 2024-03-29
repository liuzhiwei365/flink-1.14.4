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

package org.apache.flink.runtime.io.network.partition;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.BufferBuilder;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferConsumer;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.metrics.TimerGauge;
import org.apache.flink.runtime.metrics.groups.TaskIOMetricGroup;
import org.apache.flink.util.function.SupplierWithException;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkElementIndex;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * A {@link ResultPartition} which writes buffers directly to {@link ResultSubpartition}s. This is
 * in contrast to implementations where records are written to a joint structure, from which the
 * subpartitions draw the data after the write phase is finished, for example the sort-based
 * partitioning.
 *
 * <p>To avoid confusion: On the read side, all subpartitions return buffers (and backlog) to be
 * transported through the network.
 */
// 最常用的ResultPartition的实现
public abstract class BufferWritingResultPartition extends ResultPartition {

    //  数组长度 与下游并行度一致
    protected final ResultSubpartition[] subpartitions;

    // 如果是单播模式, 需要 与下游并行度一样数目的 BufferBuilder, 每个subpartition 独享一个BufferBuilder
    private final BufferBuilder[] unicastBufferBuilders;

    /** For broadcast mode, a single BufferBuilder is shared by all subpartitions. */
    // 如果是广播模式,只需要一个 BufferBuilder 就够了, 所有的 subpartition 共享
    private BufferBuilder broadcastBufferBuilder;

    // 统计阻塞申请  BufferBuilder 的背压时长
    // 参见 requestNewBufferBuilderFromPool 方法
    private TimerGauge backPressuredTimeMsPerSecond = new TimerGauge();

    public BufferWritingResultPartition(
            String owningTaskName,
            int partitionIndex,
            ResultPartitionID partitionId,
            ResultPartitionType partitionType,
            ResultSubpartition[] subpartitions,
            int numTargetKeyGroups,
            ResultPartitionManager partitionManager,
            @Nullable BufferCompressor bufferCompressor,
            SupplierWithException<BufferPool, IOException> bufferPoolFactory) {

        super(
                owningTaskName,
                partitionIndex,
                partitionId,
                partitionType,
                subpartitions.length,
                numTargetKeyGroups,
                partitionManager,
                bufferCompressor,
                bufferPoolFactory);

        this.subpartitions = checkNotNull(subpartitions);
        this.unicastBufferBuilders =
                new BufferBuilder[subpartitions.length]; // 有下游并行度 个 bufferBuilder
    }

    @Override
    public void setup() throws IOException {
        super.setup();

        checkState(
                bufferPool.getNumberOfRequiredMemorySegments() >= getNumberOfSubpartitions(),
                "Bug in result partition setup logic: Buffer pool has not enough guaranteed buffers for"
                        + " this result partition.");
    }

    // 得到所有缓冲区的大小
    @Override
    public int getNumberOfQueuedBuffers() {
        int totalBuffers = 0;

        for (ResultSubpartition subpartition : subpartitions) {
            totalBuffers += subpartition.unsynchronizedGetNumberOfQueuedBuffers();
        }

        return totalBuffers;
    }
    // 得到指定编号的缓冲区的大小
    @Override
    public int getNumberOfQueuedBuffers(int targetSubpartition) {
        checkArgument(targetSubpartition >= 0 && targetSubpartition < numSubpartitions);
        return subpartitions[targetSubpartition].unsynchronizedGetNumberOfQueuedBuffers();
    }

    protected void flushSubpartition(int targetSubpartition, boolean finishProducers) {
        if (finishProducers) {
            finishBroadcastBufferBuilder();
            finishUnicastBufferBuilder(targetSubpartition);
        }

        subpartitions[targetSubpartition].flush();
    }

    protected void flushAllSubpartitions(boolean finishProducers) {
        if (finishProducers) {
            finishBroadcastBufferBuilder();
            finishUnicastBufferBuilders();
        }

        for (ResultSubpartition subpartition : subpartitions) {
            subpartition.flush();
        }
    }

    @Override
    public void emitRecord(ByteBuffer record, int targetSubpartition) throws IOException {
        // 1 如果 buffer 不能容纳下此条数据, 会先写入一部分
        // 2 如果有需要,会申请新的 buffer,并交给  unicastBufferBuilders 成员维护
        // 3 如果不需要申请新的 buffer, 直接用 unicastBufferBuilders 成员维护的已有的buffer

        // 注： BufferBuilder 的类名字取的不好
        BufferBuilder buffer = appendUnicastDataForNewRecord(record, targetSubpartition);

        while (record.hasRemaining()) {
            // 1 如果此条数据只写过一部分, 那么剩下的部分会再次写出;
            // 2 这个过程会申请新的buffer, buffer 还不够用的话,会一直反复申请,只到把该条数据消耗完
            // 3 申请的buffer 会交给相关 unicastBufferBuilders 成员维护缓存
            finishUnicastBufferBuilder(targetSubpartition);
            buffer = appendUnicastDataForRecordContinuation(record, targetSubpartition);
        }

        if (buffer.isFull()) {
            finishUnicastBufferBuilder(targetSubpartition);
        }

    }

    @Override
    public void broadcastRecord(ByteBuffer record) throws IOException {
        BufferBuilder buffer = appendBroadcastDataForNewRecord(record);

        while (record.hasRemaining()) {
            // full buffer, partial record
            finishBroadcastBufferBuilder();
            buffer = appendBroadcastDataForRecordContinuation(record);
        }

        if (buffer.isFull()) {
            // full buffer, full record
            finishBroadcastBufferBuilder();
        }

        // partial buffer, full record
    }

    @Override
    public void broadcastEvent(AbstractEvent event, boolean isPriorityEvent) throws IOException {
        checkInProduceState();
        finishBroadcastBufferBuilder();
        finishUnicastBufferBuilders();

        try (BufferConsumer eventBufferConsumer =
                EventSerializer.toBufferConsumer(event, isPriorityEvent)) {
            for (ResultSubpartition subpartition : subpartitions) {
                // Retain the buffer so that it can be recycled by each channel of targetPartition
                subpartition.add(eventBufferConsumer.copy(), 0);
            }
        }
    }

    @Override
    public void setMetricGroup(TaskIOMetricGroup metrics) {
        super.setMetricGroup(metrics);
        backPressuredTimeMsPerSecond = metrics.getBackPressuredTimePerSecond();
    }

    @Override
    public ResultSubpartitionView createSubpartitionView(
            int subpartitionIndex, BufferAvailabilityListener availabilityListener)
            throws IOException {
        checkElementIndex(subpartitionIndex, numSubpartitions, "Subpartition not found.");
        checkState(!isReleased(), "Partition released.");

        ResultSubpartition subpartition = subpartitions[subpartitionIndex];
        ResultSubpartitionView readView = subpartition.createReadView(availabilityListener);

        LOG.debug("Created {}", readView);

        return readView;
    }

    @Override
    public void finish() throws IOException {
        finishBroadcastBufferBuilder();
        finishUnicastBufferBuilders();

        for (ResultSubpartition subpartition : subpartitions) {
            subpartition.finish();
        }

        super.finish();
    }

    @Override
    protected void releaseInternal() {
        // Release all subpartitions
        for (ResultSubpartition subpartition : subpartitions) {
            try {
                subpartition.release();
            }
            // Catch this in order to ensure that release is called on all subpartitions
            catch (Throwable t) {
                LOG.error("Error during release of result subpartition: " + t.getMessage(), t);
            }
        }
    }

    @Override
    public void close() {
        // We can not close these buffers in the release method because of the potential race
        // condition. This close method will be only called from the Task thread itself.
        if (broadcastBufferBuilder != null) {
            broadcastBufferBuilder.close();
            broadcastBufferBuilder = null;
        }
        for (int i = 0; i < unicastBufferBuilders.length; ++i) {
            if (unicastBufferBuilders[i] != null) {
                unicastBufferBuilders[i].close();
                unicastBufferBuilders[i] = null;
            }
        }
        super.close();
    }


    private BufferBuilder appendUnicastDataForNewRecord(
            final ByteBuffer record, final int targetSubpartition) throws IOException {
        if (targetSubpartition < 0 || targetSubpartition > unicastBufferBuilders.length) {
            throw new ArrayIndexOutOfBoundsException(targetSubpartition);
        }
        BufferBuilder buffer = unicastBufferBuilders[targetSubpartition];

        if (buffer == null) {
            // 如果BufferBuilder 不存在, 重新申请 BufferBuilder
            buffer = requestNewUnicastBufferBuilder(targetSubpartition);

            // 1 把 buffer 添加到 buffers 队列中  （会更新 Backlog值 ）
            // 2 appendUnicastDataForNewRecord方法 和 appendUnicastDataForRecordContinuation方法
            //   都有addToSubpartition的调用
            addToSubpartition(buffer, targetSubpartition, 0);
        }

        // 将序列化的记录 追加到 buffer中
        buffer.appendAndCommit(record);

        return buffer;
    }

    // 第三个入参: 是buffer中第一个残缺record 的结束位置; 没有残缺record 则为0 (存储一个这样的值,对后面解析来说,信息上是完备的)
    private void addToSubpartition(BufferBuilder buffer, int targetSubpartition, int i)
            throws IOException {
        // 用申请得到的 BufferBuilder 构建 BufferConsumer对象,并且添加到指定的 targetSubpartition中
        // 注：
        //  ResultSubpartition有两个实现： PipilinedSubpartition 和 BoundedBlockingSubpartition
        //  add 方法中 会调用 increaseBuffersInBacklog 来更新 积压指标
        int desirableBufferSize =
                subpartitions[targetSubpartition].add(
                        buffer.createBufferConsumerFromBeginning(), i);

        if (desirableBufferSize > 0) {
            // !! If some of partial data has written already to this buffer, the result size can
            // not be less than written value.
            buffer.trim(desirableBufferSize);
        }
    }

    private BufferBuilder appendUnicastDataForRecordContinuation(
            final ByteBuffer remainingRecordBytes, final int targetSubpartition)
            throws IOException {
        final BufferBuilder buffer = requestNewUnicastBufferBuilder(targetSubpartition);

        final int partialRecordBytes = buffer.appendAndCommit(remainingRecordBytes);
        addToSubpartition(buffer, targetSubpartition, partialRecordBytes);

        return buffer;
    }

    private BufferBuilder appendBroadcastDataForNewRecord(final ByteBuffer record)
            throws IOException {
        BufferBuilder buffer = broadcastBufferBuilder;

        if (buffer == null) {
            buffer = requestNewBroadcastBufferBuilder();
            createBroadcastBufferConsumers(buffer, 0);
        }

        buffer.appendAndCommit(record);

        return buffer;
    }

    private BufferBuilder appendBroadcastDataForRecordContinuation(
            final ByteBuffer remainingRecordBytes) throws IOException {
        final BufferBuilder buffer = requestNewBroadcastBufferBuilder();
        // !! Be aware, in case of partialRecordBytes != 0, partial length and data has to
        // `appendAndCommit` first
        // before consumer is created. Otherwise it would be confused with the case the buffer
        // starting
        // with a complete record.
        // !! The next two lines can not change order.
        final int partialRecordBytes = buffer.appendAndCommit(remainingRecordBytes);
        createBroadcastBufferConsumers(buffer, partialRecordBytes);

        return buffer;
    }

    private void createBroadcastBufferConsumers(BufferBuilder buffer, int partialRecordBytes)
            throws IOException {
        try (final BufferConsumer consumer = buffer.createBufferConsumerFromBeginning()) {
            for (ResultSubpartition subpartition : subpartitions) {
                subpartition.add(consumer.copy(), partialRecordBytes);
            }
        }
    }

    // 申请 新的 单播 BufferBuilder
    private BufferBuilder requestNewUnicastBufferBuilder(int targetSubpartition)
            throws IOException {
        checkInProduceState();
        ensureUnicastMode();
        //从 BufferPool 中申请
        final BufferBuilder bufferBuilder = requestNewBufferBuilderFromPool(targetSubpartition);
        unicastBufferBuilders[targetSubpartition] = bufferBuilder;

        return bufferBuilder;
    }

    private BufferBuilder requestNewBroadcastBufferBuilder() throws IOException {
        checkInProduceState();
        ensureBroadcastMode();

        final BufferBuilder bufferBuilder = requestNewBufferBuilderFromPool(0);
        broadcastBufferBuilder = bufferBuilder;
        return bufferBuilder;
    }

    private BufferBuilder requestNewBufferBuilderFromPool(int targetSubpartition)
            throws IOException {
        BufferBuilder bufferBuilder = bufferPool.requestBufferBuilder(targetSubpartition);
        if (bufferBuilder != null) {
            return bufferBuilder;
        }

        backPressuredTimeMsPerSecond.markStart();
        try {
            bufferBuilder = bufferPool.requestBufferBuilderBlocking(targetSubpartition);
            backPressuredTimeMsPerSecond.markEnd();
            return bufferBuilder;
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for buffer");
        }
    }

    private void finishUnicastBufferBuilder(int targetSubpartition) {
        final BufferBuilder bufferBuilder = unicastBufferBuilders[targetSubpartition];
        if (bufferBuilder != null) {
            numBytesOut.inc(bufferBuilder.finish());
            numBuffersOut.inc();
            unicastBufferBuilders[targetSubpartition] = null;
            bufferBuilder.close();
        }
    }

    private void finishUnicastBufferBuilders() {
        for (int channelIndex = 0; channelIndex < numSubpartitions; channelIndex++) {
            finishUnicastBufferBuilder(channelIndex);
        }
    }

    private void finishBroadcastBufferBuilder() {
        if (broadcastBufferBuilder != null) {
            numBytesOut.inc(broadcastBufferBuilder.finish() * numSubpartitions);
            numBuffersOut.inc(numSubpartitions);
            broadcastBufferBuilder.close();
            broadcastBufferBuilder = null;
        }
    }

    private void ensureUnicastMode() {
        finishBroadcastBufferBuilder();
    }

    private void ensureBroadcastMode() {
        finishUnicastBufferBuilders();
    }

    @VisibleForTesting
    public TimerGauge getBackPressuredTimeMsPerSecond() {
        return backPressuredTimeMsPerSecond;
    }

    @VisibleForTesting
    public ResultSubpartition[] getAllPartitions() {
        return subpartitions;
    }
}
