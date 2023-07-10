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

import org.apache.flink.runtime.io.network.NetworkSequenceViewReader;
import org.apache.flink.runtime.io.network.TaskEventPublisher;
import org.apache.flink.runtime.io.network.netty.NettyMessage.AckAllUserRecordsProcessed;
import org.apache.flink.runtime.io.network.netty.NettyMessage.AddCredit;
import org.apache.flink.runtime.io.network.netty.NettyMessage.CancelPartitionRequest;
import org.apache.flink.runtime.io.network.netty.NettyMessage.CloseRequest;
import org.apache.flink.runtime.io.network.netty.NettyMessage.NewBufferSize;
import org.apache.flink.runtime.io.network.netty.NettyMessage.PartitionRequest;
import org.apache.flink.runtime.io.network.netty.NettyMessage.ResumeConsumption;
import org.apache.flink.runtime.io.network.netty.NettyMessage.TaskEventRequest;
import org.apache.flink.runtime.io.network.partition.PartitionNotFoundException;
import org.apache.flink.runtime.io.network.partition.ResultPartitionProvider;
import org.apache.flink.runtime.io.network.partition.consumer.InputChannelID;

import org.apache.flink.runtime.io.network.partition.consumer.RemoteInputChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.SimpleChannelInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Channel handler to initiate data transfers and dispatch backwards flowing task events. */
class PartitionRequestServerHandler extends SimpleChannelInboundHandler<NettyMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(PartitionRequestServerHandler.class);

    private final ResultPartitionProvider partitionProvider;

    private final TaskEventPublisher taskEventPublisher;

    private final PartitionRequestQueue outboundQueue;

    PartitionRequestServerHandler(
            ResultPartitionProvider partitionProvider,
            TaskEventPublisher taskEventPublisher,
            PartitionRequestQueue outboundQueue) {

        this.partitionProvider = partitionProvider;
        this.taskEventPublisher = taskEventPublisher;
        this.outboundQueue = outboundQueue;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

// flink 反压机制:
// 引入 信用值 Credit ,   反映下游 inputChannel 的处理能力
//     积压值 Backlog ,   反映上游ResultPartition 的数据堆积情况

// 1  在下游 RemoteInputChannel 初始化时,会发送 PartitionRequest 请求给上游 (PartitionRequest 包含 credit值 )

// 2  上游接受, 并在 PartitionRequestServerHandler.channelRead0 方法中处理
//        上游创建 CreditBasedSequenceNumberingViewReader 对象,
//        将 initialCredit 赋值为credit,  numCreditsAvailable 赋值为 credit

// 3  上游调用 PartitionRequestQueue.writeAndFlushNextMessageIfPossible 方法向下游写数据的时候,
//    reader.getNextBuffer()方法获取 buffer的时候 会顺便消耗  numCreditsAvailable 值

// 4  reader.getNextBuffer()方法如果没有获取到buffer, 则发送 BufferResponse对象（包含Backlog值）给下游

// 5  下游的CreditBasedPartitionRequestClientHandler.decodeMsg方法 会处理上游发送的 BufferResponse对象

// 6  处理BufferResponse对象会路由到
//         CreditBasedPartitionRequestClientHandler.decodeBufferOrEvent
//               RemoteInputChannel.onEmptyBuffer
//                     RemoteInputChannel.onSenderBacklog
//               RemoteInputChannel.onBuffer
//                     RemoteInputChannel.onSenderBacklog
//     onSenderBacklog 方法 会申请 backlog + initialCredit 个  FloatingBuffer
//     将 RemoteInputChannel的 unannouncedCredit成员 赋值为  backlog + initialCredit

//    NettyPartitionRequestClient.sendToChannel
//    ChannelPipeline.fireUserEventTriggered
//    CreditBasedPartitionRequestClientHandler.userEventTriggered
//         把 AddCreditMessage 对象添加到 clientOutboundMessages队列中, 最终构建AddCredit对象 发送给上游

// 7  上游 处理 AddCredit对象（包含 unannouncedCredit值）,unannouncedCredit值会赋值给reader对象的
//    numCreditsAvailable 成员 （ 而上游处理每一条buffer的时候, 都会消耗numCreditsAvailable ）
//



    // 在服务端的handler中, channelRead0 是采用业务线程池进行处理的,  从而保证netty自身的线程不被阻塞的
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyMessage msg) throws Exception {
        try {
            Class<?> msgClazz = msg.getClass();

            // ----------------------------------------------------------------
            // Intermediate result partition requests
            // ----------------------------------------------------------------
            if (msgClazz == PartitionRequest.class) {
                PartitionRequest request = (PartitionRequest) msg;
                LOG.debug("Read channel on {}: {}.", ctx.channel().localAddress(), request);

                try {
                    NetworkSequenceViewReader reader;
                    reader =
                            new CreditBasedSequenceNumberingViewReader(
                                    request.receiverId, request.credit, outboundQueue);

                    // 1 创建指定 subPartition 的 ResultSubpartitionView对象(读视图),并赋值给 reader 的成员变量
                    // 2 让 reader 排队
                    reader.requestSubpartitionView(
                            partitionProvider, request.partitionId, request.queueIndex);

                    // 也就是说下游的task 给我们发送PartitionRequest后,
                    // 我们会一一创建并维护相应的reader, 用于后续的缓存的数据消费
                    outboundQueue.notifyReaderCreated(reader);
                } catch (PartitionNotFoundException notFound) {
                    respondWithError(ctx, notFound, request.receiverId);
                }
            }
            // ----------------------------------------------------------------
            // Task events
            // ----------------------------------------------------------------
            else if (msgClazz == TaskEventRequest.class) {
                // 用于迭代计算的场景
                TaskEventRequest request = (TaskEventRequest) msg;

                if (!taskEventPublisher.publish(request.partitionId, request.event)) {
                    respondWithError(
                            ctx,
                            new IllegalArgumentException("Task event receiver not found."),
                            request.receiverId); // receiverId 就是 inputChannel
                }
            } else if (msgClazz == CancelPartitionRequest.class) {
                // 把相应的reader 从 outboundQueue 的维护中去除
                CancelPartitionRequest request = (CancelPartitionRequest) msg;

                outboundQueue.cancel(request.receiverId);
            } else if (msgClazz == CloseRequest.class) {
                // 释放所有的reader
                outboundQueue.close();
            } else if (msgClazz == AddCredit.class) {
                // ResultPartition 接收并处理AddCredit请求的过程
                AddCredit request = (AddCredit) msg;
                // 1 reader.addCredit 最终 增加 CreditBasedSequenceNumberingViewReader对象的 numCreditsAvailable 成员变量
                // 2 然后 重新让 reader 去排队
                outboundQueue.addCreditOrResumeConsumption(
                        request.receiverId, reader -> reader.addCredit(request.credit));

            } else if (msgClazz == ResumeConsumption.class) {
                ResumeConsumption request = (ResumeConsumption) msg;
                // 设置 SubPartition 的 isBlocked 为 false
                outboundQueue.addCreditOrResumeConsumption(
                        request.receiverId, NetworkSequenceViewReader::resumeConsumption);
            } else if (msgClazz == AckAllUserRecordsProcessed.class) {
                // 所有用户 数据被处理完了
                AckAllUserRecordsProcessed request = (AckAllUserRecordsProcessed) msg;

                outboundQueue.acknowledgeAllRecordsProcessed(request.receiverId);
            } else if (msgClazz == NewBufferSize.class) {
                NewBufferSize request = (NewBufferSize) msg;

                outboundQueue.notifyNewBufferSize(request.receiverId, request.bufferSize);
            } else {
                LOG.warn("Received unexpected client request: {}", msg);
            }
        } catch (Throwable t) {
            respondWithError(ctx, t);
        }
    }

    private void respondWithError(ChannelHandlerContext ctx, Throwable error) {
        ctx.writeAndFlush(new NettyMessage.ErrorResponse(error));
    }

    private void respondWithError(
            ChannelHandlerContext ctx, Throwable error, InputChannelID sourceId) {
        LOG.debug("Responding with error: {}.", error.getClass());

        ctx.writeAndFlush(new NettyMessage.ErrorResponse(error, sourceId));
    }
}
