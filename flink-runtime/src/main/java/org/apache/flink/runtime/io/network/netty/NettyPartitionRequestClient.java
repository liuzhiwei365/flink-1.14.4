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

import org.apache.flink.runtime.event.TaskEvent;
import org.apache.flink.runtime.io.network.ConnectionID;
import org.apache.flink.runtime.io.network.NetworkClientHandler;
import org.apache.flink.runtime.io.network.PartitionRequestClient;
import org.apache.flink.runtime.io.network.netty.exception.LocalTransportException;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.consumer.RemoteInputChannel;
import org.apache.flink.runtime.util.AtomicDisposableReferenceCounter;

import org.apache.flink.shaded.netty4.io.netty.channel.Channel;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFuture;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFutureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.runtime.io.network.netty.NettyMessage.PartitionRequest;
import static org.apache.flink.runtime.io.network.netty.NettyMessage.TaskEventRequest;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Partition request client for remote partition requests.
 *
 * <p>This client is shared by all remote input channels, which request a partition from the same
 * {@link ConnectionID}.
 */
public class NettyPartitionRequestClient implements PartitionRequestClient {

    private static final Logger LOG = LoggerFactory.getLogger(NettyPartitionRequestClient.class);

    private final Channel tcpChannel;

    private final NetworkClientHandler clientHandler;

    private final ConnectionID connectionId;

    private final PartitionRequestClientFactory clientFactory;

    /** If zero, the underlying TCP channel can be safely closed. */
    private final AtomicDisposableReferenceCounter closeReferenceCounter =
            new AtomicDisposableReferenceCounter();

    NettyPartitionRequestClient(
            Channel tcpChannel,
            NetworkClientHandler clientHandler,
            ConnectionID connectionId,
            PartitionRequestClientFactory clientFactory) {

        this.tcpChannel = checkNotNull(tcpChannel);
        this.clientHandler = checkNotNull(clientHandler);
        this.connectionId = checkNotNull(connectionId);
        this.clientFactory = checkNotNull(clientFactory);
    }

    boolean disposeIfNotUsed() {
        return closeReferenceCounter.disposeIfNotUsed();
    }

    /**
     * Increments the reference counter.
     *
     * <p>Note: the reference counter has to be incremented before returning the instance of this
     * client to ensure correct closing logic.
     */
    boolean incrementReferenceCounter() {
        return closeReferenceCounter.increment();
    }

    /**
     * Requests a remote intermediate result partition queue.
     *
     * <p>The request goes to the remote producer, for which this partition request client instance
     * has been created.
     */
    @Override
    public void requestSubpartition(
            final ResultPartitionID partitionId, // ResultPartition编号
            final int subpartitionIndex, // SubPartition编号
            final RemoteInputChannel inputChannel, // inputChannel自己
            int delayMs)
            throws IOException {

        checkNotClosed();

        LOG.debug(
                "Requesting subpartition {} of partition {} with {} ms delay.",
                subpartitionIndex,
                partitionId,
                delayMs);

        clientHandler.addInputChannel(inputChannel);

        // PartitionRequest  继承了NettyMessage,可以基于Netty实现的tcp网络中作为传输的消息
        // 其中,该消息内部含有 InitialCredit(部分体现了数据的消费能力)
        final PartitionRequest request =
                new PartitionRequest(
                        partitionId,
                        subpartitionIndex,
                        inputChannel.getInputChannelId(),
                        inputChannel.getInitialCredit());

        final ChannelFutureListener listener =
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            clientHandler.removeInputChannel(inputChannel);
                            inputChannel.onError(
                                    new LocalTransportException(
                                            String.format(
                                                    "Sending the partition request to '%s (#%d)' failed.",
                                                    connectionId.getAddress(),
                                                    connectionId.getConnectionIndex()),
                                            future.channel().localAddress(),
                                            future.cause()));
                        }
                    }
                };

        // 将创建好的 PartitionRequest 立即或者延迟 发送请求（PartitionRequest）  到 生产端所在的task 的节点

        //   ****
        //  上游的 处理
        //  PartitionRequestServerHandler.channelRead0
        if (delayMs == 0) {
            ChannelFuture f = tcpChannel.writeAndFlush(request);
            f.addListener(listener);
        } else {
            final ChannelFuture[] f = new ChannelFuture[1];
            tcpChannel
                    .eventLoop()
                    .schedule(
                            new Runnable() {
                                @Override
                                public void run() {
                                    f[0] = tcpChannel.writeAndFlush(request);
                                    f[0].addListener(listener);
                                }
                            },
                            delayMs,
                            TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Sends a task event backwards to an intermediate result partition producer.
     *
     * <p>Backwards task events flow between readers and writers and therefore will only work when
     * both are running at the same time, which is only guaranteed to be the case when both the
     * respective producer and consumer task run pipelined.
     */
    //将任务事件向后发送到 中间结果分区 生产者
    @Override
    public void sendTaskEvent(
            ResultPartitionID partitionId, TaskEvent event, final RemoteInputChannel inputChannel)
            throws IOException {
        checkNotClosed();

        tcpChannel
                .writeAndFlush(
                        new TaskEventRequest(event, partitionId, inputChannel.getInputChannelId()))
                .addListener(
                        new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (!future.isSuccess()) {
                                    inputChannel.onError(
                                            new LocalTransportException(
                                                    String.format(
                                                            "Sending the task event to '%s (#%d)' failed.",
                                                            connectionId.getAddress(),
                                                            connectionId.getConnectionIndex()),
                                                    future.channel().localAddress(),
                                                    future.cause()));
                                }
                            }
                        });
    }

    @Override
    public void notifyCreditAvailable(RemoteInputChannel inputChannel) {
        // 最终会把  NettyMessage.AddCredit 对象发给上游
        // 而 NettyMessage.AddCredit 对象中的 credit 值, 是 RemoteInputChannel中 unannouncedCredit成员变量的值
        sendToChannel(new AddCreditMessage(inputChannel));
    }

    @Override
    public void notifyNewBufferSize(RemoteInputChannel inputChannel, int bufferSize) {
        sendToChannel(new NewBufferSizeMessage(inputChannel, bufferSize));
    }

    @Override
    public void resumeConsumption(RemoteInputChannel inputChannel) {
        sendToChannel(new ResumeConsumptionMessage(inputChannel));
    }

    @Override
    public void acknowledgeAllRecordsProcessed(RemoteInputChannel inputChannel) {
        sendToChannel(new AcknowledgeAllRecordsProcessedMessage(inputChannel));
    }

    private void sendToChannel(ClientOutboundMessage message) {
        /*
         *   DefaultChannelPipeline#fireUserEventTriggered(java.lang.Object)
         *   AbstractChannelHandlerContext#invokeUserEventTriggered(org.apache.flink.shaded.netty4.io.netty.channel.AbstractChannelHandlerContext,
         * java.lang.Object)
         *   AbstractChannelHandlerContext#invokeUserEventTriggered(java.lang.Object)
         *   CreditBasedPartitionRequestClientHandler#userEventTriggered(org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext,
         * java.lang.Object)
         *
         */
        // CreditBasedPartitionRequestClientHandler 的 userEventTriggered会 响应 fireUserEventTriggered方法
        tcpChannel.eventLoop().execute(() -> tcpChannel.pipeline().fireUserEventTriggered(message));
    }

    @Override
    public void close(RemoteInputChannel inputChannel) throws IOException {

        clientHandler.removeInputChannel(inputChannel);

        if (closeReferenceCounter.decrement()) {
            // Close the TCP connection. Send a close request msg to ensure
            // that outstanding backwards task events are not discarded.
            tcpChannel
                    .writeAndFlush(new NettyMessage.CloseRequest())
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

            // Make sure to remove the client from the factory
            clientFactory.destroyPartitionRequestClient(connectionId, this);
        } else {
            clientHandler.cancelRequestFor(inputChannel.getInputChannelId());
        }
    }

    private void checkNotClosed() throws IOException {
        if (closeReferenceCounter.isDisposed()) {
            final SocketAddress localAddr = tcpChannel.localAddress();
            final SocketAddress remoteAddr = tcpChannel.remoteAddress();
            throw new LocalTransportException(
                    String.format("Channel to '%s' closed.", remoteAddr), localAddr);
        }
    }

    private static class AddCreditMessage extends ClientOutboundMessage {

        private AddCreditMessage(RemoteInputChannel inputChannel) {
            super(checkNotNull(inputChannel));
        }

        @Override
        Object buildMessage() {
            // 获得UnannouncedCredit 指标
            int credits = inputChannel.getAndResetUnannouncedCredit();
            return credits > 0
                    ? new NettyMessage.AddCredit(credits, inputChannel.getInputChannelId())
                    : null;
        }
    }

    private static class NewBufferSizeMessage extends ClientOutboundMessage {
        private final int bufferSize;

        private NewBufferSizeMessage(RemoteInputChannel inputChannel, int bufferSize) {
            super(checkNotNull(inputChannel));
            this.bufferSize = bufferSize;
        }

        @Override
        Object buildMessage() {
            return new NettyMessage.NewBufferSize(bufferSize, inputChannel.getInputChannelId());
        }
    }

    private static class ResumeConsumptionMessage extends ClientOutboundMessage {

        private ResumeConsumptionMessage(RemoteInputChannel inputChannel) {
            super(checkNotNull(inputChannel));
        }

        @Override
        Object buildMessage() {
            return new NettyMessage.ResumeConsumption(inputChannel.getInputChannelId());
        }
    }

    private static class AcknowledgeAllRecordsProcessedMessage extends ClientOutboundMessage {

        private AcknowledgeAllRecordsProcessedMessage(RemoteInputChannel inputChannel) {
            super(checkNotNull(inputChannel));
        }

        @Override
        Object buildMessage() {
            return new NettyMessage.AckAllUserRecordsProcessed(inputChannel.getInputChannelId());
        }
    }
}
