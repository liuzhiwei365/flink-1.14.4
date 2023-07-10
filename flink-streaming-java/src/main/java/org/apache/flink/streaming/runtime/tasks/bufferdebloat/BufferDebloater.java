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

package org.apache.flink.streaming.runtime.tasks.bufferdebloat;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.runtime.io.network.partition.consumer.IndexedInputGate;

import java.time.Duration;

import static org.apache.flink.configuration.TaskManagerOptions.BUFFER_DEBLOAT_TARGET;
import static org.apache.flink.configuration.TaskManagerOptions.BUFFER_DEBLOAT_THRESHOLD_PERCENTAGES;
import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * Class for automatic calculation of the buffer size based on the current throughput and
 * configuration.
 */
public class BufferDebloater {
    private static final long MILLIS_IN_SECOND = 1000;

    private final Duration targetTotalBufferSize;
    private final IndexedInputGate[] inputGates;
    private final long maxBufferSize;
    private final long minBufferSize;
    private final double bufferDebloatThresholdFactor;

    private int lastBufferSize;
    private Duration lastEstimatedTimeToConsumeBuffers = Duration.ZERO;

    public BufferDebloater(Configuration taskConfig, IndexedInputGate[] inputGates) {
        this.inputGates = inputGates;
        this.targetTotalBufferSize = taskConfig.get(BUFFER_DEBLOAT_TARGET);
        this.maxBufferSize = taskConfig.get(TaskManagerOptions.MEMORY_SEGMENT_SIZE).getBytes();
        this.minBufferSize = taskConfig.get(TaskManagerOptions.MIN_MEMORY_SEGMENT_SIZE).getBytes();

        this.bufferDebloatThresholdFactor =
                taskConfig.getInteger(BUFFER_DEBLOAT_THRESHOLD_PERCENTAGES) / 100.0;

        this.lastBufferSize = (int) maxBufferSize;

        // Right now the buffer size can not be grater than integer max value according to
        // MemorySegment and buffer implementation.
        checkArgument(maxBufferSize <= Integer.MAX_VALUE);
        checkArgument(maxBufferSize > 0);
        checkArgument(minBufferSize > 0);
        checkArgument(maxBufferSize >= minBufferSize);
        checkArgument(targetTotalBufferSize.toMillis() > 0.0);
    }

    // 重新计算 buffer 数量, 并发给上游
    public void recalculateBufferSize(long currentThroughput) {
        // 每毫秒吞吐量的统计值
        long desiredTotalBufferSizeInBytes =
                (currentThroughput * targetTotalBufferSize.toMillis()) / MILLIS_IN_SECOND;

        // 测量目前所有 inputGate下 所有inputChannel使用的 buffer数量总和
        int totalNumber = 0;
        for (IndexedInputGate inputGate : inputGates) {
            totalNumber += Math.max(1, inputGate.getBuffersInUseCount());
        }

        // 重新计算每个 buffer 的大小 （ 以平均每个buffer均摊的每毫秒吞吐量大小 作为上游 buffer size ）
        int newSize =
                (int)Math.max(
                                minBufferSize,//默认 1kb
                                Math.min(
                                        desiredTotalBufferSizeInBytes / totalNumber,// 平均每个buffer均摊的 每毫秒 吞吐量
                                        maxBufferSize));// 默认 32kb

        // 估算buffer数据完全消费的所需时间
        lastEstimatedTimeToConsumeBuffers =
                Duration.ofMillis(
                        newSize * totalNumber * MILLIS_IN_SECOND / Math.max(1, currentThroughput));

        // 如果新计算出的大小和旧的很接近，不更新buffer大小
        // 旧buffer大小乘以taskmanager.network.memory.buffer-debloat.threshold-percentages计算出变化量
        // 如果newSize和旧buffer大小差异值小于变化量，则不更新buffer大小
        boolean skipUpdate = skipUpdate(newSize);

        if (skipUpdate) {
            // 如果不需要更新,返回
            return;
        }

        lastBufferSize = newSize;
        for (IndexedInputGate inputGate : inputGates) {
            if (!inputGate.isFinished()) {
                // 把计算好的 buffer 的大小 通知上游
                inputGate.announceBufferSize(newSize);
            }
        }
    }

    @VisibleForTesting
    boolean skipUpdate(int newSize) {
        if (newSize == lastBufferSize) {
            return true;
        }

        // According to logic of this class newSize can not be less than min or greater than max
        // buffer size but if considering this method independently the behaviour for the small or
        // big value should be the same as for min and max buffer size correspondingly.
        if (newSize <= minBufferSize || newSize >= maxBufferSize) {
            return false;
        }

        int delta = (int) (lastBufferSize * bufferDebloatThresholdFactor);
        return Math.abs(newSize - lastBufferSize) < delta;
    }

    public int getLastBufferSize() {
        return lastBufferSize;
    }

    public Duration getLastEstimatedTimeToConsumeBuffers() {
        return lastEstimatedTimeToConsumeBuffers;
    }
}
