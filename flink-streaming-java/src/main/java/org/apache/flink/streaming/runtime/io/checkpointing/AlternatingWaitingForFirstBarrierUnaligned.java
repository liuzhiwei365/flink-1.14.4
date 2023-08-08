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

package org.apache.flink.streaming.runtime.io.checkpointing;

import org.apache.flink.runtime.checkpoint.CheckpointException;
import org.apache.flink.runtime.checkpoint.channel.InputChannelInfo;
import org.apache.flink.runtime.io.network.api.CheckpointBarrier;
import org.apache.flink.runtime.io.network.partition.consumer.CheckpointableInput;

import java.io.IOException;

/**
 * We either timed out before seeing any barriers or started unaligned. We might've seen some
 * announcements if we started aligned.
 */
// 我们要么在看到任何barrier 之前就超时了,要么开始不对齐。   如果我们开始对齐, 我们可能会看到一些公告
final class AlternatingWaitingForFirstBarrierUnaligned implements BarrierHandlerState {

    private final boolean alternating;
    private final ChannelState channelState;

    AlternatingWaitingForFirstBarrierUnaligned(boolean alternating, ChannelState channelState) {
        this.alternating = alternating;
        this.channelState = channelState;
    }

    @Override
    public BarrierHandlerState alignmentTimeout(
            Controller controller, CheckpointBarrier checkpointBarrier) {
        // ignore already processing unaligned checkpoints
        return this;
    }

    @Override
    public BarrierHandlerState announcementReceived(
            Controller controller, InputChannelInfo channelInfo, int sequenceNumber)
            throws IOException {
        channelState.getInputs()[channelInfo.getGateIdx()].convertToPriorityEvent(
                channelInfo.getInputChannelIdx(), sequenceNumber);
        return this;
    }

    @Override
    public BarrierHandlerState barrierReceived(
            Controller controller,
            InputChannelInfo channelInfo,
            CheckpointBarrier checkpointBarrier,
            boolean markChannelBlocked)
            throws CheckpointException, IOException {

        // 我们收到一个 对齐的  barrier ，我们应该预订，将此通道保持为阻塞状态
        //  因为这种情况 它正被基于信用的网络阻止
        if (markChannelBlocked
                && !checkpointBarrier.getCheckpointOptions().isUnalignedCheckpoint()) {
            channelState.blockChannel(channelInfo);
        }

        CheckpointBarrier unalignedBarrier = checkpointBarrier.asUnaligned();

        // 构造写出器, 实际的持久化 InputChannel 状态
        //           非对齐 的 checkpoint 才会有效
        controller.initInputsCheckpoint(unalignedBarrier);

        for (CheckpointableInput input : channelState.getInputs()) {
            // 这里会调用栈：
            //     StreamTaskSourceInput.checkpointStarted
            //     StreamTaskSourceInput.blockConsumption
            //     阻塞input 的消费 （全局阻塞, input 的所有inputChannel）
            input.checkpointStarted(unalignedBarrier);
        }

        // 调用栈：
        //     SingleCheckpointBarrierHandler.ControllerImpl.triggerGlobalCheckpoint
        //     SingleCheckpointBarrierHandler.triggerCheckpoint
        //     CheckpointBarrierHandler.notifyCheckpoint
        //     StreamTask.triggerCheckpointOnBarrier
        //     StreamTask.performCheckpoint
        //     SubtaskCheckpointCoordinatorImpl.checkpointState
        //
        controller.triggerGlobalCheckpoint(unalignedBarrier);

        if (controller.allBarriersReceived()) {
            for (CheckpointableInput input : channelState.getInputs()) {
                // 恢复input 的消费
                input.checkpointStopped(unalignedBarrier.getId());
            }
            return stopCheckpoint();
        }
        return new AlternatingCollectingBarriersUnaligned(alternating, channelState);
    }

    @Override
    public BarrierHandlerState abort(long cancelledId) throws IOException {
        return stopCheckpoint();
    }

    @Override
    public BarrierHandlerState endOfPartitionReceived(
            Controller controller, InputChannelInfo channelInfo)
            throws IOException, CheckpointException {
        channelState.channelFinished(channelInfo);

        // Do nothing since we have no pending checkpoint.
        return this;
    }

    private BarrierHandlerState stopCheckpoint() throws IOException {
        channelState.unblockAllChannels();
        if (alternating) {
            return new AlternatingWaitingForFirstBarrier(channelState.emptyState());
        } else {
            return new AlternatingWaitingForFirstBarrierUnaligned(false, channelState.emptyState());
        }
    }
}
