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

import java.io.IOException;

import static org.apache.flink.util.Preconditions.checkState;

/** Actions to be taken when processing aligned checkpoints. */
// 处理 对齐的checkpoint时 要采取的操作
abstract class AbstractAlignedBarrierHandlerState implements BarrierHandlerState {

    protected final ChannelState state;

    protected AbstractAlignedBarrierHandlerState(ChannelState state) {
        this.state = state;
    }

    @Override
    public final BarrierHandlerState alignmentTimeout(
            Controller controller, CheckpointBarrier checkpointBarrier)
            throws IOException, CheckpointException {
        throw new IllegalStateException(
                "Alignment should not be timed out if we are not alternating.");
    }

    @Override
    public final BarrierHandlerState announcementReceived(
            Controller controller, InputChannelInfo channelInfo, int sequenceNumber) {
        return this;
    }

    // 每来一个 barrier, 都有可能触发 subtask 的 checkpoint
    @Override
    public final BarrierHandlerState barrierReceived(
            Controller controller,
            InputChannelInfo channelInfo,
            CheckpointBarrier checkpointBarrier,
            boolean markChannelBlocked)
            throws IOException, CheckpointException {
        checkState(!checkpointBarrier.getCheckpointOptions().isUnalignedCheckpoint());

        if (markChannelBlocked) {
            // 阻塞消费
            state.blockChannel(channelInfo);
        }

        if (controller.allBarriersReceived()) {
            // 如果每个inputChannel 的barrier 都到齐了
            // 开始本 sub task 的 checkpoint
            return triggerGlobalCheckpoint(controller, checkpointBarrier);
        }

        // 调整状态机
        return convertAfterBarrierReceived(state);
    }

    protected WaitingForFirstBarrier triggerGlobalCheckpoint(
            Controller controller, CheckpointBarrier checkpointBarrier) throws IOException {
        controller.triggerGlobalCheckpoint(checkpointBarrier);
        // 恢复所有 先前阻塞的 inputChannel
        state.unblockAllChannels();
        // 调整状态机为 WaitingForFirstBarrier
        return new WaitingForFirstBarrier(state.getInputs());
    }

    protected abstract BarrierHandlerState convertAfterBarrierReceived(ChannelState state);

    @Override
    public final BarrierHandlerState abort(long cancelledId) throws IOException {
        state.unblockAllChannels();
        return new WaitingForFirstBarrier(state.getInputs());
    }
}
