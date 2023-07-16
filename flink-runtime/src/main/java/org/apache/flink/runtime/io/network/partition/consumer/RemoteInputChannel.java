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

package org.apache.flink.runtime.io.network.partition.consumer;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.metrics.Counter;
import org.apache.flink.runtime.checkpoint.CheckpointException;
import org.apache.flink.runtime.checkpoint.CheckpointFailureReason;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.event.TaskEvent;
import org.apache.flink.runtime.execution.CancelTaskException;
import org.apache.flink.runtime.io.network.ConnectionID;
import org.apache.flink.runtime.io.network.ConnectionManager;
import org.apache.flink.runtime.io.network.PartitionRequestClient;
import org.apache.flink.runtime.io.network.api.CheckpointBarrier;
import org.apache.flink.runtime.io.network.api.EventAnnouncement;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.Buffer.DataType;
import org.apache.flink.runtime.io.network.buffer.BufferProvider;
import org.apache.flink.runtime.io.network.logger.NetworkActionsLogger;
import org.apache.flink.runtime.io.network.partition.PartitionNotFoundException;
import org.apache.flink.runtime.io.network.partition.PrioritizedDeque;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.util.ExceptionUtils;

import org.apache.flink.shaded.guava30.com.google.common.collect.Iterators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/** An input channel, which requests a remote partition queue. */
public class RemoteInputChannel extends InputChannel {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteInputChannel.class);

    private static final int NONE = -1;

    /** ID to distinguish this channel from other channels sharing the same TCP connection. */
    private final InputChannelID id = new InputChannelID();

    /** The connection to use to request the remote partition. */
    private final ConnectionID connectionId;

    /** The connection manager to use connect to the remote partition provider. */
    // 保持与上游 partition 的连接
    private final ConnectionManager connectionManager;

    /**
     * The received buffers. Received buffers are enqueued by the network I/O thread and the queue
     * is consumed by the receiving task thread.
     */
    // 接收到的缓冲区    接收到的缓冲区由网络I/O线程排队, 队列由接收任务线程 消费
    // SequenceBuffer 是对 Buffer 的包装 , 多了一个 序列号
    // 优先级队列, 队列分 为优先级部分  和 普通部分, 优先部分在前, 普通部分在后

    // getNextBuffer 方法中会消费该队列
    private final PrioritizedDeque<SequenceBuffer> receivedBuffers = new PrioritizedDeque<>();

    /**
     * Flag indicating whether this channel has been released. Either called by the receiving task
     * thread or the task manager actor.
     */
    // 本 channel 是否已经本释放
    private final AtomicBoolean isReleased = new AtomicBoolean();

    /** Client to establish a (possibly shared) TCP connection and request the partition. */
    // 与上游建立 tcp 连接的客户端
    private volatile PartitionRequestClient partitionRequestClient;

    /** The next expected sequence number for the next buffer. */
    //用来保证buffer 的顺序, 在buffer中包含了 sequenceNumber,而在RemoteInputChannel本地维护 expectedSequenceNumber
    //只有两边的序列号相同才可以证明buffer 数据的顺序是正常且可以进行处理
    private int expectedSequenceNumber = 0;

    /** The initial number of exclusive buffers assigned to this channel. */
    // 分配给本channel 的 初始独占的 buffer 数量
    private final int initialCredit;

    /** The number of available buffers that have not been announced to the producer yet. */
    // 还没有 向生产者 公布的 可用 buffer  数量
    private final AtomicInteger unannouncedCredit = new AtomicInteger(0);

    // 内部的  bufferQueue 成员 维护exclusive buffers 和 floating buffers
    private final BufferManager bufferManager;

    @GuardedBy("receivedBuffers")
    private int lastBarrierSequenceNumber = NONE;

    @GuardedBy("receivedBuffers")
    private long lastBarrierId = NONE;

    // channel 状态的 持久化器 ,用来更新 本RemoteInputChannel的内部状态
    private final ChannelStatePersister channelStatePersister;

    public RemoteInputChannel(
            SingleInputGate inputGate,
            int channelIndex,
            ResultPartitionID partitionId,
            ConnectionID connectionId,
            ConnectionManager connectionManager,
            int initialBackOff,
            int maxBackoff,
            int networkBuffersPerChannel,
            Counter numBytesIn,
            Counter numBuffersIn,
            ChannelStateWriter stateWriter) {

        super(
                inputGate,
                channelIndex,
                partitionId,
                initialBackOff,
                maxBackoff,
                numBytesIn,
                numBuffersIn);
        checkArgument(networkBuffersPerChannel >= 0, "Must be non-negative.");

        this.initialCredit = networkBuffersPerChannel;
        this.connectionId = checkNotNull(connectionId);
        this.connectionManager = checkNotNull(connectionManager);
        this.bufferManager = new BufferManager(inputGate.getMemorySegmentProvider(), this, 0);
        this.channelStatePersister = new ChannelStatePersister(stateWriter, getChannelInfo());
    }

    @VisibleForTesting
    void setExpectedSequenceNumber(int expectedSequenceNumber) {
        this.expectedSequenceNumber = expectedSequenceNumber;
    }

    /**
     * Setup includes assigning exclusive buffers to this input channel, and this method should be
     * called only once after this input channel is created.
     */
    @Override
    void setup() throws IOException {
        checkState(
                bufferManager.unsynchronizedGetAvailableExclusiveBuffers() == 0,
                "Bug in input channel setup logic: exclusive buffers have already been set for this input channel.");

        bufferManager.requestExclusiveBuffers(initialCredit);
    }

    // ------------------------------------------------------------------------
    // Consume
    // ------------------------------------------------------------------------

    /** Requests a remote subpartition. */
    @VisibleForTesting
    @Override
    // 请求远程的 一个子分区
    public void requestSubpartition(int subpartitionIndex)
            throws IOException, InterruptedException {
        if (partitionRequestClient == null) {
            LOG.debug(
                    "{}: Requesting REMOTE subpartition {} of partition {}. {}",
                    this,
                    subpartitionIndex,
                    partitionId,
                    channelStatePersister);
            // Create a client and request the partition
            try {
                // 创建 PartitionRequestClient 对象

                // NettyConnectionManager.createPartitionRequestClient
                // PartitionRequestClientFactory.createPartitionRequestClient
                // PartitionRequestClientFactory.connectWithRetries
                // PartitionRequestClientFactory.connect

                partitionRequestClient =
                        connectionManager.createPartitionRequestClient(connectionId);
            } catch (IOException e) {
                // IOExceptions indicate that we could not open a connection to the remote
                // TaskExecutor
                throw new PartitionConnectionException(partitionId, e);
            }

            // 向ResultPartition中注册InitialCredit 信息
            partitionRequestClient.requestSubpartition(partitionId, subpartitionIndex, this, 0);
        }
    }

    /** Retriggers a remote subpartition request. */
    // 触发 请求远程的 一个子分区
    void retriggerSubpartitionRequest(int subpartitionIndex) throws IOException {
        checkPartitionRequestQueueInitialized();

        if (increaseBackoff()) {
            partitionRequestClient.requestSubpartition(
                    partitionId, subpartitionIndex, this, getCurrentBackoff());
        } else {
            failPartitionRequest();
        }
    }

    @Override
    Optional<BufferAndAvailability> getNextBuffer() throws IOException {
        checkPartitionRequestQueueInitialized();

        final SequenceBuffer next;
        final DataType nextDataType;

        synchronized (receivedBuffers) {
            // 消费队列头的第一个元素
            next = receivedBuffers.poll();
            // 看一下下一个元素的类型, 如果不存在下一个元素，则返回DataType.NONE类型
            nextDataType =
                    receivedBuffers.peek() != null
                            ? receivedBuffers.peek().buffer.getDataType()
                            : DataType.NONE;
        }

        if (next == null) {
            if (isReleased.get()) {
                throw new CancelTaskException(
                        "Queried for a buffer after channel has been released.");
            }
            return Optional.empty();
        }

        NetworkActionsLogger.traceInput(
                "RemoteInputChannel#getNextBuffer",
                next.buffer,
                inputGate.getOwningTaskName(),
                channelInfo,
                channelStatePersister,
                next.sequenceNumber);
        // 统计消费过的字节总数
        numBytesIn.inc(next.buffer.getSize());
        // 统计消费过的buffer 总数
        numBuffersIn.inc();
        // 包装成  BufferAndAvailability对象返回
        return Optional.of(
                new BufferAndAvailability(next.buffer, nextDataType, 0, next.sequenceNumber));
    }

    // ------------------------------------------------------------------------
    // Task events
    // ------------------------------------------------------------------------

    @Override
    void sendTaskEvent(TaskEvent event) throws IOException {
        checkState(
                !isReleased.get(),
                "Tried to send task event to producer after channel has been released.");
        checkPartitionRequestQueueInitialized();

        partitionRequestClient.sendTaskEvent(partitionId, event, this);
    }

    // ------------------------------------------------------------------------
    // Life cycle
    // ------------------------------------------------------------------------

    @Override
    public boolean isReleased() {
        return isReleased.get();
    }

    /** Releases all exclusive and floating buffers, closes the partition request client. */
    @Override
    void releaseAllResources() throws IOException {
        if (isReleased.compareAndSet(false, true)) {

            final ArrayDeque<Buffer> releasedBuffers;
            synchronized (receivedBuffers) {
                releasedBuffers =
                        receivedBuffers.stream()
                                .map(sb -> sb.buffer)
                                .collect(Collectors.toCollection(ArrayDeque::new));
                receivedBuffers.clear();
            }
            bufferManager.releaseAllBuffers(releasedBuffers);

            // The released flag has to be set before closing the connection to ensure that
            // buffers received concurrently with closing are properly recycled.
            if (partitionRequestClient != null) {
                partitionRequestClient.close(this);
            } else {
                connectionManager.closeOpenChannelConnections(connectionId);
            }
        }
    }

    @Override
    int getBuffersInUseCount() {
        return getNumberOfQueuedBuffers()
                + Math.max(0, bufferManager.getNumberOfRequiredBuffers() - initialCredit);
    }

    @Override
    void announceBufferSize(int newBufferSize) {
        try {
            notifyNewBufferSize(newBufferSize);
        } catch (Throwable t) {
            ExceptionUtils.rethrow(t);
        }
    }

    private void failPartitionRequest() {
        setError(new PartitionNotFoundException(partitionId));
    }

    @Override
    public String toString() {
        return "RemoteInputChannel [" + partitionId + " at " + connectionId + "]";
    }

    // ------------------------------------------------------------------------
    // Credit-based
    // ------------------------------------------------------------------------

    /**
     * Enqueue this input channel in the pipeline for notifying the producer of unannounced credit.
     */

    private void notifyCreditAvailable() throws IOException {
        checkPartitionRequestQueueInitialized();
        //  最终会把  NettyMessage.AddCredit 对象发给上游  ResultSubpartition
        //  上报 InputChannel 的  unAnnouncedCredit 指标
        partitionRequestClient.notifyCreditAvailable(this);
    }

    private void notifyNewBufferSize(int newBufferSize) throws IOException {
        checkState(!isReleased.get(), "Channel released.");
        checkPartitionRequestQueueInitialized();

        partitionRequestClient.notifyNewBufferSize(this, newBufferSize);
    }

    @VisibleForTesting
    public int getNumberOfAvailableBuffers() {
        return bufferManager.getNumberOfAvailableBuffers();
    }

    @VisibleForTesting
    public int getNumberOfRequiredBuffers() {
        return bufferManager.unsynchronizedGetNumberOfRequiredBuffers();
    }

    @VisibleForTesting
    public int getSenderBacklog() {
        return getNumberOfRequiredBuffers() - initialCredit;
    }

    @VisibleForTesting
    boolean isWaitingForFloatingBuffers() {
        return bufferManager.unsynchronizedIsWaitingForFloatingBuffers();
    }

    @VisibleForTesting
    public Buffer getNextReceivedBuffer() {
        final SequenceBuffer sequenceBuffer = receivedBuffers.poll();
        return sequenceBuffer != null ? sequenceBuffer.buffer : null;
    }

    @VisibleForTesting
    BufferManager getBufferManager() {
        return bufferManager;
    }

    @VisibleForTesting
    PartitionRequestClient getPartitionRequestClient() {
        return partitionRequestClient;
    }

    /**
     * The unannounced credit is increased by the given amount and might notify increased credit to
     * the producer.
     */
    @Override
    public void notifyBufferAvailable(int numAvailableBuffers) throws IOException {
        // 1 unannouncedCredit 表示还没有 向生产者 公布的 可用 buffer  数量
        // 2 如果 unannouncedCredit 大于 0 , 表明上游发生了积压, 上游的credit消耗完了,这时下游正准备把 该值通知给上游
        // 3  getAndAdd 方法的意思是  先获取值判断是否为0 , 再把numAvailableBuffers 加给 unannouncedCredit
        if (numAvailableBuffers > 0 && unannouncedCredit.getAndAdd(numAvailableBuffers) == 0) {
            notifyCreditAvailable();
        }
    }

    @Override
    public void resumeConsumption() throws IOException {
        checkState(!isReleased.get(), "Channel released.");
        checkPartitionRequestQueueInitialized();

        if (initialCredit == 0) {
            // this unannounced credit can be a positive value because credit assignment and the
            // increase of this value is not an atomic operation and as a result, this unannounced
            // credit value can be get increased even after this channel has been blocked and all
            // floating credits are released, it is important to clear this unannounced credit and
            // at the same time reset the sender's available credits to keep consistency
            unannouncedCredit.set(0);
        }

        // notifies the producer that this channel is ready to
        // unblock from checkpoint and resume data consumption
        // 恢复消费 ,向指定的channel 发送恢复消费的消息
        partitionRequestClient.resumeConsumption(this);
    }

    @Override
    public void acknowledgeAllRecordsProcessed() throws IOException {
        checkState(!isReleased.get(), "Channel released.");
        checkPartitionRequestQueueInitialized();

        partitionRequestClient.acknowledgeAllRecordsProcessed(this);
    }

    private void onBlockingUpstream() {
        if (initialCredit == 0) {

            // 释放分配的浮动缓冲区，以便它们可以被其他通道使用，如果没有配置独占缓冲区，这很重要，
            // 因为阻塞的通道不能传输任何数据，因此分配的浮动缓冲器不能被回收，因此，其他通道可能
            // 无法为数据传输分配新的缓冲区
            // （极端情况是我们只有 1个浮动缓冲区 和 0个独占缓冲区）
            bufferManager.releaseFloatingBuffers();
        }
    }

    // ------------------------------------------------------------------------
    // Network I/O notifications (called by network I/O thread)
    // ------------------------------------------------------------------------

    /**
     * Gets the currently unannounced credit.
     *
     * @return Credit which was not announced to the sender yet.
     */
    public int getUnannouncedCredit() {
        return unannouncedCredit.get();
    }

    /**
     * Gets the unannounced credit and resets it to <tt>0</tt> atomically.
     *
     * @return Credit which was not announced to the sender yet.
     */
    public int getAndResetUnannouncedCredit() {
        return unannouncedCredit.getAndSet(0);
    }

    /**
     * Gets the current number of received buffers which have not been processed yet.
     *
     * @return Buffers queued for processing.
     */
    public int getNumberOfQueuedBuffers() {
        synchronized (receivedBuffers) {
            return receivedBuffers.size();
        }
    }

    @Override
    public int unsynchronizedGetNumberOfQueuedBuffers() {
        return Math.max(0, receivedBuffers.size());
    }

    public int unsynchronizedGetExclusiveBuffersUsed() {
        return Math.max(
                0, initialCredit - bufferManager.unsynchronizedGetAvailableExclusiveBuffers());
    }

    public int unsynchronizedGetFloatingBuffersAvailable() {
        return Math.max(0, bufferManager.unsynchronizedGetFloatingBuffersAvailable());
    }

    public InputChannelID getInputChannelId() {
        return id;
    }

    public int getInitialCredit() {
        return initialCredit;
    }

    public BufferProvider getBufferProvider() throws IOException {
        if (isReleased.get()) {
            return null;
        }

        return inputGate.getBufferProvider();
    }

    /**
     * Requests buffer from input channel directly for receiving network data. It should always
     * return an available buffer in credit-based mode unless the channel has been released.
     *
     * @return The available buffer.
     */
    @Nullable
    public Buffer requestBuffer() {
        return bufferManager.requestBuffer();
    }

    /**
     * Receives the backlog from the producer's buffer response. If the number of available buffers
     * is less than backlog + initialCredit, it will request floating buffers from the buffer
     * manager, and then notify unannounced credits to the producer.
     *
     * @param backlog The number of unsent buffers in the producer's sub partition.
     */
    // 接受到上游数据挤压的消息
    //  1 然后申请更多的 FloatingBuffer
    //  2 通知上游 申请成功的buffer 数量, 并把这个数据作为 credit值发给上游
    public void onSenderBacklog(int backlog) throws IOException {
        notifyBufferAvailable(bufferManager.requestFloatingBuffers(backlog + initialCredit));
    }

    /**
     * Handles the input buffer. This method is taking over the ownership of the buffer and is fully
     * responsible for cleaning it up both on the happy path and in case of an error.
     */

    // 1  本方法主要涉及到两个队列 inputChannelsWithData 和 receivedBuffers
    // 2  inputChannelsWithData 是 inputGate 级别的;  receivedBuffers 是 inputChannel级别的
    // 3  本方法主要逻辑就是 当netty网络来一个Buffer后, 在各类情况下,对 以上两个队列排队 (本方法是那两个队列的生产端)
    // 4  通过队列,让Buffer的  生产和消费做到了 解耦
    // 5  inputChannelsWithData 与 receivedBuffers 是 嵌套的关系, subtask的主逻辑在消费 Buffer的时候
    //    会先向从inputChannelsWithData 取出 inputChannel , 再取出 inputChannel对象的 receivedBuffers队列的Buffer元素
    //
    // 本类核心方法
    public void onBuffer(Buffer buffer, int sequenceNumber, int backlog) throws IOException {
        boolean recycleBuffer = true;

        try {
            if (expectedSequenceNumber != sequenceNumber) {
                // 如果数据出现了乱序,报错
                onError(new BufferReorderingException(expectedSequenceNumber, sequenceNumber));
                return;
            }

            if (buffer.getDataType().isBlockingUpstream()) {
                onBlockingUpstream();
                checkArgument(backlog == 0, "Illegal number of backlog: %s, should be 0.", backlog);
            }

            final boolean wasEmpty;
            boolean firstPriorityEvent = false;
            synchronized (receivedBuffers) {
                NetworkActionsLogger.traceInput(
                        "RemoteInputChannel#onBuffer",
                        buffer,
                        inputGate.getOwningTaskName(),
                        channelInfo,
                        channelStatePersister,
                        sequenceNumber);
                //如果本channel 已经被释放, 不处理
                if (isReleased.get()) {
                    return;
                }

                wasEmpty = receivedBuffers.isEmpty();

                SequenceBuffer sequenceBuffer = new SequenceBuffer(buffer, sequenceNumber);
                DataType dataType = buffer.getDataType();

                // 不管优先级如何,最终sequenceBuffer 都会被添加到receivedBuffers中
                // firstPriorityEvent 反映的是, 是否是添加到receivedBuffers 队列中的第一个优先元素
                if (dataType.hasPriority()) {
                    firstPriorityEvent = addPriorityBuffer(sequenceBuffer);
                    recycleBuffer = false;
                } else {
                    receivedBuffers.add(sequenceBuffer);
                    recycleBuffer = false;
                    if (dataType.requiresAnnouncement()) {
                        // 目前只有 TIMEOUTABLE_ALIGNED_CHECKPOINT_BARRIER 类型的 dataType 会走到这里,序列化后也就是 CheckpointBarrier
                        // 如果需要先声明,则先声明, 再添加到receivedBuffers 中
                        firstPriorityEvent = addPriorityBuffer(announce(sequenceBuffer));
                    }
                }
                channelStatePersister
                        .checkForBarrier(sequenceBuffer.buffer)  // 返回 barrierId （如果buffer 反序列化后与 barrier相关的话,否则不处理）
                        .filter(id -> id > lastBarrierId) // 如果这次 barrierId 大于上次的,是可以接受的
                        .ifPresent(
                                id -> {
                                    // 跟新 Barrier 相关的状态
                                    lastBarrierId = id;
                                    lastBarrierSequenceNumber = sequenceBuffer.sequenceNumber;
                                });
                // 可能持久化 inputChannel的 buffer状态
                channelStatePersister.maybePersist(buffer);
                ++expectedSequenceNumber;
            }

            // 如果本 RemoteInputChannel  是第一次添加 优先事件  (之前receivedBuffers成员队列没有 优先元素)
            // 则把inputcahnnel 添加到  inputChannelsWithData优先级队列 的优先部分; 也就是该inputCahnnel通道将来会被优先处理消费
            if (firstPriorityEvent) {
                notifyPriorityEvent(sequenceNumber);
            }

            // 如果本 RemoteInputChannel  是第一次添加 Buffer  (之前receivedBuffers成员队列没有元素),
            // 则也把本InputChannel 添加到 inputChannelsWithData优先级队列 的非优先部分
            if (wasEmpty) {
                notifyChannelNonEmpty();
            }

            // 处理上游积压
            if (backlog >= 0) {
                onSenderBacklog(backlog);
            }
        } finally {
            if (recycleBuffer) {
                buffer.recycleBuffer();
            }
        }
    }

    /** @return {@code true} if this was first priority buffer added. */
    private boolean addPriorityBuffer(SequenceBuffer sequenceBuffer) {
        receivedBuffers.addPriorityElement(sequenceBuffer);
        // 判断优先级部分的元素总数 是否为 1
        return receivedBuffers.getNumPriorityElements() == 1;
    }

    private SequenceBuffer announce(SequenceBuffer sequenceBuffer) throws IOException {
        checkState(
                !sequenceBuffer.buffer.isBuffer(),
                "Only a CheckpointBarrier can be announced but found %s",
                sequenceBuffer.buffer);
        checkAnnouncedOnlyOnce(sequenceBuffer);
        AbstractEvent event =
                EventSerializer.fromBuffer(sequenceBuffer.buffer, getClass().getClassLoader());
        checkState(
                event instanceof CheckpointBarrier,
                "Only a CheckpointBarrier can be announced but found %s",
                sequenceBuffer.buffer);
        CheckpointBarrier barrier = (CheckpointBarrier) event;
        return new SequenceBuffer(
                EventSerializer.toBuffer(
                        new EventAnnouncement(barrier, sequenceBuffer.sequenceNumber), true),
                sequenceBuffer.sequenceNumber);
    }

    private void checkAnnouncedOnlyOnce(SequenceBuffer sequenceBuffer) {
        Iterator<SequenceBuffer> iterator = receivedBuffers.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            if (iterator.next().sequenceNumber == sequenceBuffer.sequenceNumber) {
                count++;
            }
        }
        checkState(
                count == 1,
                "Before enqueuing the announcement there should be exactly single occurrence of the buffer, but found [%d]",
                count);
    }

    /**
     * Spills all queued buffers on checkpoint start. If barrier has already been received (and
     * reordered), spill only the overtaken buffers.
     */
    public void checkpointStarted(CheckpointBarrier barrier) throws CheckpointException {
        synchronized (receivedBuffers) {
            if (barrier.getId() < lastBarrierId) {
                throw new CheckpointException(
                        String.format(
                                "Sequence number for checkpoint %d is not known (it was likely been overwritten by a newer checkpoint %d)",
                                barrier.getId(), lastBarrierId),
                        CheckpointFailureReason
                                .CHECKPOINT_SUBSUMED); // currently, at most one active unaligned
                // checkpoint is possible
            } else if (barrier.getId() > lastBarrierId) {
                // This channel has received some obsolete barrier, older compared to the
                // checkpointId
                // which we are processing right now, and we should ignore that obsoleted checkpoint
                // barrier sequence number.
                resetLastBarrier();
            }

            channelStatePersister.startPersisting(
                    barrier.getId(), getInflightBuffersUnsafe(barrier.getId()));
        }
    }

    public void checkpointStopped(long checkpointId) {
        synchronized (receivedBuffers) {
            channelStatePersister.stopPersisting(checkpointId);
            if (lastBarrierId == checkpointId) {
                resetLastBarrier();
            }
        }
    }

    @VisibleForTesting
    List<Buffer> getInflightBuffers(long checkpointId) {
        synchronized (receivedBuffers) {
            return getInflightBuffersUnsafe(checkpointId);
        }
    }

    @Override
    public void convertToPriorityEvent(int sequenceNumber) throws IOException {
        boolean firstPriorityEvent;
        synchronized (receivedBuffers) {
            checkState(channelStatePersister.hasBarrierReceived());

            // 当前 receivedBuffers 队列中, 优先元素的数量
            int numPriorityElementsBeforeRemoval = receivedBuffers.getNumPriorityElements();

            // 从receivedBuffers 队列中 拿到第一个满足条件的元素, 从队列中删除, 并作为方法的返回值
            SequenceBuffer toPrioritize =
                    receivedBuffers.getAndRemove(
                            sequenceBuffer -> sequenceBuffer.sequenceNumber == sequenceNumber);
            checkState(lastBarrierSequenceNumber == sequenceNumber);
            checkState(!toPrioritize.buffer.isBuffer());
            checkState(
                    numPriorityElementsBeforeRemoval == receivedBuffers.getNumPriorityElements(),
                    "Attempted to convertToPriorityEvent an event [%s] that has already been prioritized [%s]",
                    toPrioritize,
                    numPriorityElementsBeforeRemoval);
            // set the priority flag (checked on poll)
            // don't convert the barrier itself (barrier controller might not have been switched yet)

            // 将 buffer 反序列化成 事件
            AbstractEvent e =
                    EventSerializer.fromBuffer(
                            toPrioritize.buffer, this.getClass().getClassLoader());
            // buffer 有读写指针, 设置读指针为 0
            toPrioritize.buffer.setReaderIndex(0);
            // 将反序列化得到的事件 重新序列化,不过加入了 优先 的标志
            toPrioritize =
                    new SequenceBuffer(
                            EventSerializer.toBuffer(e, true), toPrioritize.sequenceNumber);

            // 将已经有了优先标志的 元素,重新添加到 receivedBuffers 队列中, 并判断该元素是不是队列中唯一的一个 优先元素
            firstPriorityEvent =
                    addPriorityBuffer(
                            toPrioritize);
            // note that only position of the element is changed
            // converting the event itself would require switching the controller sooner
        }
        if (firstPriorityEvent) {
            notifyPriorityEventForce();
            // forcibly notify about the priority event
            // instead of passing barrier SQN to be checked
            // because this SQN might have be seen by the input gate during the announcement
        }
    }

    private void notifyPriorityEventForce() {
        inputGate.notifyPriorityEventForce(this);
    }

    /**
     * Returns a list of buffers, checking the first n non-priority buffers, and skipping all
     * events.
     */
    private List<Buffer> getInflightBuffersUnsafe(long checkpointId) {
        assert Thread.holdsLock(receivedBuffers);

        checkState(checkpointId == lastBarrierId || lastBarrierId == NONE);

        final List<Buffer> inflightBuffers = new ArrayList<>();
        Iterator<SequenceBuffer> iterator = receivedBuffers.iterator();
        // skip all priority events (only buffers are stored anyways)
        Iterators.advance(iterator, receivedBuffers.getNumPriorityElements());

        while (iterator.hasNext()) {
            SequenceBuffer sequenceBuffer = iterator.next();
            if (sequenceBuffer.buffer.isBuffer()) {
                if (shouldBeSpilled(sequenceBuffer.sequenceNumber)) {
                    inflightBuffers.add(sequenceBuffer.buffer.retainBuffer());
                } else {
                    break;
                }
            }
        }

        return inflightBuffers;
    }

    private void resetLastBarrier() {
        lastBarrierId = NONE;
        lastBarrierSequenceNumber = NONE;
    }

    /**
     * @return if given {@param sequenceNumber} should be spilled given {@link
     *     #lastBarrierSequenceNumber}. We might not have yet received {@link CheckpointBarrier} and
     *     we might need to spill everything. If we have already received it, there is a bit nasty
     *     corner case of {@link SequenceBuffer#sequenceNumber} overflowing that needs to be handled
     *     as well.
     */
    private boolean shouldBeSpilled(int sequenceNumber) {
        if (lastBarrierSequenceNumber == NONE) {
            return true;
        }
        checkState(
                receivedBuffers.size() < Integer.MAX_VALUE / 2,
                "Too many buffers for sequenceNumber overflow detection code to work correctly");

        boolean possibleOverflowAfterOvertaking = Integer.MAX_VALUE / 2 < lastBarrierSequenceNumber;
        boolean possibleOverflowBeforeOvertaking =
                lastBarrierSequenceNumber < -Integer.MAX_VALUE / 2;

        if (possibleOverflowAfterOvertaking) {
            return sequenceNumber < lastBarrierSequenceNumber && sequenceNumber > 0;
        } else if (possibleOverflowBeforeOvertaking) {
            return sequenceNumber < lastBarrierSequenceNumber || sequenceNumber > 0;
        } else {
            return sequenceNumber < lastBarrierSequenceNumber;
        }
    }

    public void onEmptyBuffer(int sequenceNumber, int backlog) throws IOException {
        boolean success = false;

        synchronized (receivedBuffers) {
            if (!isReleased.get()) {
                if (expectedSequenceNumber == sequenceNumber) {
                    expectedSequenceNumber++;
                    success = true;
                } else {
                    onError(new BufferReorderingException(expectedSequenceNumber, sequenceNumber));
                }
            }
        }

        if (success && backlog >= 0) {
            onSenderBacklog(backlog);
        }
    }

    public void onFailedPartitionRequest() {
        inputGate.triggerPartitionStateCheck(partitionId);
    }

    public void onError(Throwable cause) {
        setError(cause);
    }

    private void checkPartitionRequestQueueInitialized() throws IOException {
        checkError();
        checkState(
                partitionRequestClient != null,
                "Bug: partitionRequestClient is not initialized before processing data and no error is detected.");
    }

    private static class BufferReorderingException extends IOException {

        private static final long serialVersionUID = -888282210356266816L;

        private final int expectedSequenceNumber;

        private final int actualSequenceNumber;

        BufferReorderingException(int expectedSequenceNumber, int actualSequenceNumber) {
            this.expectedSequenceNumber = expectedSequenceNumber;
            this.actualSequenceNumber = actualSequenceNumber;
        }

        @Override
        public String getMessage() {
            return String.format(
                    "Buffer re-ordering: expected buffer with sequence number %d, but received %d.",
                    expectedSequenceNumber, actualSequenceNumber);
        }
    }

    private static final class SequenceBuffer {
        final Buffer buffer;
        final int sequenceNumber;

        private SequenceBuffer(Buffer buffer, int sequenceNumber) {
            this.buffer = buffer;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public String toString() {
            return String.format(
                    "SequenceBuffer(isEvent = %s, dataType = %s, sequenceNumber = %s)",
                    !buffer.isBuffer(), buffer.getDataType(), sequenceNumber);
        }
    }
}
