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

package org.apache.flink.runtime.io.network.netty;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.io.network.NetworkSequenceViewReader;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.netty.NettyMessage.ErrorResponse;
import org.apache.flink.runtime.io.network.partition.ProducerFailedException;
import org.apache.flink.runtime.io.network.partition.ResultSubpartitionView;
import org.apache.flink.runtime.io.network.partition.consumer.InputChannel.BufferAndAvailability;
import org.apache.flink.runtime.io.network.partition.consumer.InputChannelID;

import org.apache.flink.shaded.netty4.io.netty.channel.Channel;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFuture;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFutureListener;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static org.apache.flink.runtime.io.network.netty.NettyMessage.BufferResponse;
import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * A nonEmptyReader of partition queues, which listens for channel writability changed events before
 * writing and flushing {@link Buffer} instances.
 */
// PartitionRequestQueue 是一个 netty 的 ChannelHandler 对象
//  它是Server端 pipeline 处理器的其中一环节
// 参见 NettyProtocol.getServerChannelHandlers   方法

//  PartitionRequestServerHandler处理完成后的数据会传递给  PartitionRequestQueue 继续处理
class PartitionRequestQueue extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PartitionRequestQueue.class);

    private final ChannelFutureListener writeListener =
            new WriteAndFlushNextMessageIfPossibleListener();

    /** The readers which are already enqueued available for transferring data. */
    private final ArrayDeque<NetworkSequenceViewReader> availableReaders = new ArrayDeque<>();

    /** All the readers created for the consumers' partition requests. */
    // 下游的 InputChannel 的 代言
    // 1 当 CreditBasedSequenceNumberingViewReader 变为可用状态时，会通过ResultSubpartitionView读取
    // ResultSubpartition  中的buffer数据并发送到 NettyMessageEncoder处理器中进行编码处理,最后发送到tcp 通道
    // 2  CreditBasedSequenceNumberingViewReader  维护者 Credit信息,会影响决定自己是否可用
    private final ConcurrentMap<InputChannelID, NetworkSequenceViewReader> allReaders =
            new ConcurrentHashMap<>();

    private boolean fatalError;

    private ChannelHandlerContext ctx;

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        if (this.ctx == null) {
            this.ctx = ctx;
        }

        super.channelRegistered(ctx);
    }

    void notifyReaderNonEmpty(final NetworkSequenceViewReader reader) {
        // The notification might come from the same thread. For the initial writes this
        // might happen before the reader has set its reference to the view, because
        // creating the queue and the initial notification happen in the same method call.
        // This can be resolved by separating the creation of the view and allowing
        // notifications.

        // TODO This could potentially have a bad performance impact as in the
        // worst case (network consumes faster than the producer) each buffer
        // will trigger a separate event loop task being scheduled.


        //  ****        PartitionRequestQueue.userEventTriggered 方法会响应
        //
        //  责任链模式
        //  DefaultChannelPipeline.fireUserEventTriggered
        //  AbstractChannelHandlerContext.invokeUserEventTriggered
        //  ChannelInboundHandler.userEventTriggered
        //  PartitionRequestQueue.userEventTriggered

        ctx.executor().execute(() -> ctx.pipeline().fireUserEventTriggered(reader));
    }

    /**
     * Try to enqueue the reader once receiving credit notification from the consumer or receiving
     * non-empty reader notification from the producer.
     *
     * <p>NOTE: Only one thread would trigger the actual enqueue after checking the reader's
     * availability, so there is no race condition here.
     */
    //  在检查reader 的可用性后, 只有一个线程会触发实际的排队, 因此这里不存在竞争条件

    // 当收到下游的  增加 Credit 的 通知
    // 或者
    // reader中数据非空时会 通知调用

    // 将 指定的 reader 添加到  availableReaders 队列中排队
    private void enqueueAvailableReader(final NetworkSequenceViewReader reader) throws Exception {
        if (reader.isRegisteredAsAvailable()) {
            return;
        }

        // 先获取 如下两个信息：1 ResultSubpartitionView是否可用  2 Backlog 值
        ResultSubpartitionView.AvailabilityWithBacklog availabilityWithBacklog =
                reader.getAvailabilityAndBacklog();

        if (!availabilityWithBacklog.isAvailable()) {
            // 当reader 不可用
            int backlog = availabilityWithBacklog.getBacklog();
            // 当有积压 且 credit 消耗完了为0
            if (backlog > 0 && reader.needAnnounceBacklog()) {
                // 向下游发送有数据挤压的消息
                announceBacklog(reader, backlog);
            }
            return;
        }

        boolean triggerWrite = availableReaders.isEmpty();
        // 向队列注册  reader
        registerAvailableReader(reader);

        if (triggerWrite) {
            // 如果这是队列中第一个元素（前行代码注册了一个）, 调用 writeAndFlushNextMessageIfPossible 发送数据

            // *** 本方法是由 netty的 channelRead0 方法进入的,大家都  channelRead0是由业务线程池处理的
            // *** 所以本方法也势必会被多线程同时调用, 不过第一个进入本方法的线程会率先占领 writeAndFlushNextMessageIfPossible 的逻辑
            // *** 先后进入的线程在前面的线程大概率只能仅仅走到 向队列注册 reader的逻辑；不过没关系,后来的线程注册的 reader 同样也能被
            // *** 率先占领 writeAndFlushNextMessageIfPossible 逻辑的线程处理
            writeAndFlushNextMessageIfPossible(ctx.channel());
        }
    }

    /**
     * Accesses internal state to verify reader registration in the unit tests.
     *
     * <p><strong>Do not use anywhere else!</strong>
     *
     * @return readers which are enqueued available for transferring data
     */
    @VisibleForTesting
    ArrayDeque<NetworkSequenceViewReader> getAvailableReaders() {
        return availableReaders;
    }

    public void notifyReaderCreated(final NetworkSequenceViewReader reader) {
        allReaders.put(reader.getReceiverId(), reader);
    }

    public void cancel(InputChannelID receiverId) {
        ctx.pipeline().fireUserEventTriggered(receiverId);
    }

    public void close() throws IOException {
        if (ctx != null) {
            ctx.channel().close();
        }

        releaseAllResources();
    }

    /**
     * Adds unannounced credits from the consumer or resumes data consumption after an exactly-once
     * checkpoint and enqueues the corresponding reader for this consumer (if not enqueued yet).
     *
     * @param receiverId The input channel id to identify the consumer.
     * @param operation The operation to be performed (add credit or resume data consumption).
     */
    // 添加信用值,并且恢复消费 （针对指定的 下游指定的inputChannel 的信用）
    // 所谓恢复消费, 就是把 指定 reader 重新添加到 availableReaders 队列  ,并把  isRegisteredAsAvailable 设置为 true
    void addCreditOrResumeConsumption(
            InputChannelID receiverId, Consumer<NetworkSequenceViewReader> operation)
            throws Exception {
        if (fatalError) {
            return;
        }

        NetworkSequenceViewReader reader = obtainReader(receiverId);

        operation.accept(reader);// numCreditsAvailable += creditDeltas
        enqueueAvailableReader(reader);
    }

    void acknowledgeAllRecordsProcessed(InputChannelID receiverId) {
        if (fatalError) {
            return;
        }

        obtainReader(receiverId).acknowledgeAllRecordsProcessed();
    }

    void notifyNewBufferSize(InputChannelID receiverId, int newBufferSize) {
        if (fatalError) {
            return;
        }

        // It is possible to receive new buffer size before the reader would be created since the
        // downstream task could calculate buffer size even using the data from one channel but it
        // sends new buffer size into all upstream even if they don't ready yet. In this case, just
        // ignore the new buffer size.
        NetworkSequenceViewReader reader = allReaders.get(receiverId);
        if (reader != null) {
            reader.notifyNewBufferSize(newBufferSize);
        }
    }

    NetworkSequenceViewReader obtainReader(InputChannelID receiverId) {
        NetworkSequenceViewReader reader = allReaders.get(receiverId);
        if (reader == null) {
            throw new IllegalStateException(
                    "No reader for receiverId = " + receiverId + " exists.");
        }

        return reader;
    }

    /**
     * Announces remaining backlog to the consumer after the available data notification or data
     * consumption resumption.
     */
    //通知下游的 inputChannel,有数据积压
    private void announceBacklog(NetworkSequenceViewReader reader, int backlog) {
        checkArgument(backlog > 0, "Backlog must be positive.");

        NettyMessage.BacklogAnnouncement announcement =
                new NettyMessage.BacklogAnnouncement(
                        backlog, reader.getReceiverId()); // 参数中的receiverId 就是 inputChannel
        ctx.channel()
                .writeAndFlush(announcement)
                .addListener(
                        (ChannelFutureListener)
                                future -> {
                                    if (!future.isSuccess()) {
                                        onChannelFutureFailure(future);
                                    }
                                });
    }

    //  DefaultChannelPipeline.fireUserEventTriggered
    //  AbstractChannelHandlerContext.invokeUserEventTriggered
    //  ChannelInboundHandler.userEventTriggered
    //  PartitionRequestQueue.userEventTriggered
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof NetworkSequenceViewReader) {
            enqueueAvailableReader((NetworkSequenceViewReader) msg);
        } else if (msg.getClass() == InputChannelID.class) {
            // Release partition view that get a cancel request.
            InputChannelID toCancel = (InputChannelID) msg;

            // remove reader from queue of available readers
            availableReaders.removeIf(reader -> reader.getReceiverId().equals(toCancel));

            // remove reader from queue of all readers and release its resource
            final NetworkSequenceViewReader toRelease = allReaders.remove(toCancel);
            if (toRelease != null) {
                releaseViewReader(toRelease);
            }
        } else {
            ctx.fireUserEventTriggered(msg);
        }
    }

    // PartitionRequestQueue会监听NettyChannel的可写入状态，当Channel可写入时，就会从availableReaders队列中
    // 取出NetworkSequenceViewReader，读取数据并写入网络。可写入状态是Netty通过水位线进行控制的，NettyServer在
    // 启动的时候会配置水位线，如果Netty输出缓冲中的字节数超过了高水位值，我们会等到其降到低水位值以下才继续写入数据。
    // 通过水位线机制确保不往网络中写入太多数据
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // 当前channel的读写状态发生变化 （低于水位线时, 被重新触发调用）
        writeAndFlushNextMessageIfPossible(ctx.channel());
    }

    // 将 buffer  数据发给下游的 总入口
    private void writeAndFlushNextMessageIfPossible(final Channel channel) throws IOException {
        if (fatalError || !channel.isWritable()) {
            // 有错误,或者改channel不可写,直接返回
            return;
        }

        // The logic here is very similar to the combined input gate and local
        // input channel logic. You can think of this class acting as the input
        // gate and the consumed views as the local input channels.

        BufferAndAvailability next = null;
        try {
            while (true) {
                // 类似于轮训
                // 从 availableReaders队列中 获取 CreditBasedSequenceNumberingViewReader 对象
                // CreditBasedSequenceNumberingViewReader  可以看做是下游 InputChannel的 代言
                NetworkSequenceViewReader reader = pollAvailableReader();

                if (reader == null) {
                    return;
                }

                // 获取buffer 数据  （ 内部会用 ResultSubpartitionView.getNextBuffer 来获取buffer ;view 的作用就是 方便 reader 读取buffer)
                next = reader.getNextBuffer();
                if (next == null) {
                    // reader中没有获取数据
                    if (!reader.isReleased()) {
                        // 但是 reader 又没被释放, 自旋式再次获取
                        continue;
                    }

                    Throwable cause = reader.getFailureCause();
                    if (cause != null) {
                        ErrorResponse msg =
                                new ErrorResponse(new ProducerFailedException(cause), reader.getReceiverId());
                        ctx.writeAndFlush(msg);
                    }
                } else {

                    if (next.moreAvailable()) {
                        // 如果还有数据,把这个reader 重新排队,有利于均匀的处理每个subResultPartition
                        registerAvailableReader(reader);
                    }
                    // BufferResponse 是 NettyMessage对象
                    // 注:
                    //    SequenceNumber 是 每个Subpartition 中从0 递增的 buffer 的 序号 （为了保证下游数据的顺序性,checkpoint对齐的地方有作用）
                    //    ReceiverId  就是下游的 InputChannelID
                    //    Backlog     反映了数据积压的情况, 发给下游的话,下游能根据这个值去申请buffer,以平衡上下游的数据生产和消费

                    // 下游 CreditBasedPartitionRequestClientHandler.decodeMsg 会解码处理
                    BufferResponse msg =
                            new BufferResponse(
                                    next.buffer(),
                                    next.getSequenceNumber(),
                                    reader.getReceiverId(),
                                    next.buffersInBacklog());

                    // 封装数据发给下游tcp 通道 ,并添加监听器, 如果数据发送成功,监听器会再此调用 本方法继续向下游发送数据，依次往复
                    // 这里是netty的 api
                    channel.writeAndFlush(msg).addListener(writeListener);

                    return;
                }
            }
        } catch (Throwable t) {
            if (next != null) {
                next.buffer().recycleBuffer();
            }

            throw new IOException(t.getMessage(), t);
        }
    }

    private void registerAvailableReader(NetworkSequenceViewReader reader) {
        availableReaders.add(reader);
        reader.setRegisteredAsAvailable(true);
    }

    @Nullable
    private NetworkSequenceViewReader pollAvailableReader() {
        NetworkSequenceViewReader reader = availableReaders.poll();
        if (reader != null) {
            // 该元素已经从队列中拿出,将isRegisteredAvailable 成员变量设置为false,表明没在排队
            reader.setRegisteredAsAvailable(false);
        }
        return reader;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        releaseAllResources();

        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleException(ctx.channel(), cause);
    }

    private void handleException(Channel channel, Throwable cause) throws IOException {
        LOG.error("Encountered error while consuming partitions", cause);

        fatalError = true;
        releaseAllResources();

        if (channel.isActive()) {
            channel.writeAndFlush(new ErrorResponse(cause))
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void releaseAllResources() throws IOException {
        // note: this is only ever executed by one thread: the Netty IO thread!
        for (NetworkSequenceViewReader reader : allReaders.values()) {
            releaseViewReader(reader);
        }

        availableReaders.clear();
        allReaders.clear();
    }

    private void releaseViewReader(NetworkSequenceViewReader reader) throws IOException {
        reader.setRegisteredAsAvailable(false);
        reader.releaseAllResources();
    }

    private void onChannelFutureFailure(ChannelFuture future) throws Exception {
        if (future.cause() != null) {
            handleException(future.channel(), future.cause());
        } else {
            handleException(
                    future.channel(), new IllegalStateException("Sending cancelled by user."));
        }
    }

    // This listener is called after an element of the current nonEmptyReader has been
    // flushed. If successful, the listener triggers further processing of the
    // queues.
    private class WriteAndFlushNextMessageIfPossibleListener implements ChannelFutureListener {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            try {
                if (future.isSuccess()) {
                    writeAndFlushNextMessageIfPossible(future.channel());
                } else {
                    onChannelFutureFailure(future);
                }
            } catch (Throwable t) {
                handleException(future.channel(), t);
            }
        }
    }
}
