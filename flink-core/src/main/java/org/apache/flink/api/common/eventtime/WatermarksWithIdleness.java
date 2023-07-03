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

package org.apache.flink.api.common.eventtime;

import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.util.clock.Clock;
import org.apache.flink.util.clock.SystemClock;

import java.time.Duration;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

// 将空闲检测  添加到另一个水印生成器 的 水印生成器 (可以理解为其他 水印生成器的一个 代理 )
// 如果检测空闲, 则会向下游发送 WatermarkStatus.IDLE 水印状态
@Public
public class WatermarksWithIdleness<T> implements WatermarkGenerator<T> {

    private final WatermarkGenerator<T> watermarks;

    private final IdlenessTimer idlenessTimer;

    /**
     * Creates a new WatermarksWithIdleness generator to the given generator idleness detection with
     * the given timeout.
     *
     * @param watermarks The original watermark generator.
     * @param idleTimeout The timeout for the idleness detection.
     */
    public WatermarksWithIdleness(WatermarkGenerator<T> watermarks, Duration idleTimeout) {
        this(watermarks, idleTimeout, SystemClock.getInstance());
    }

    @VisibleForTesting
    WatermarksWithIdleness(WatermarkGenerator<T> watermarks, Duration idleTimeout, Clock clock) {
        checkNotNull(idleTimeout, "idleTimeout");
        checkArgument(
                !(idleTimeout.isZero() || idleTimeout.isNegative()),
                "idleTimeout must be greater than zero");
        this.watermarks = checkNotNull(watermarks, "watermarks");
        this.idlenessTimer = new IdlenessTimer(clock, idleTimeout);
    }

    @Override
    public void onEvent(T event, long eventTimestamp, WatermarkOutput output) {
        watermarks.onEvent(event, eventTimestamp, output);
        idlenessTimer.activity();
    }

    @Override
    public void onPeriodicEmit(WatermarkOutput output) {
        if (idlenessTimer.checkIfIdle()) {
            //  如果在特定时间端内（超时持续时间）内没有用户数据到来, 则此生成器将流标记为空闲
            //  会向下游发送 WatermarkStatus.IDLE 水印状态
            output.markIdle();
        } else {
            watermarks.onPeriodicEmit(output);
        }
    }

    // ------------------------------------------------------------------------

    @VisibleForTesting
    static final class IdlenessTimer {

        /** The clock used to measure elapsed time. */
        private final Clock clock;

        /** Counter to detect change. No problem if it overflows. */
        private long counter;

        /** The value of the counter at the last activity check. */
        private long lastCounter;

        /**
         * The first time (relative to {@link Clock#relativeTimeNanos()}) when the activity check
         * found that no activity happened since the last check. Special value: 0 = no timer.
         */
        private long startOfInactivityNanos;

        /** The duration before the output is marked as idle. */
        private final long maxIdleTimeNanos;

        IdlenessTimer(Clock clock, Duration idleTimeout) {
            this.clock = clock;

            long idleNanos;
            try {
                idleNanos = idleTimeout.toNanos();
            } catch (ArithmeticException ignored) {
                // long integer overflow
                idleNanos = Long.MAX_VALUE;
            }

            this.maxIdleTimeNanos = idleNanos;
        }

        public void activity() {
            counter++;
        }

        // 检测是否有空闲, 是否在 固定的时间段内, 用户数据一直没来
        public boolean checkIfIdle() {
            if (counter != lastCounter) {
                // 说明在此期间有 用户数据到来 ,没有空闲, 所以返回false
                lastCounter = counter;
                startOfInactivityNanos = 0L;
                return false;
            } else
            if (startOfInactivityNanos == 0L) {
                // 计时器还没有开始计时 , 没法检测,不能算作检测到空闲
                startOfInactivityNanos = clock.relativeTimeNanos();// clock 是 java 的 SystemClock
                return false;
            } else {
                // 代码走到这里, 说明目前还没有数据到来
                // 如果 超过了最大空闲的时间尺度, 所以我们认为 检测到了空闲
                // 否则,仍然认为没有空闲
                return clock.relativeTimeNanos() - startOfInactivityNanos > maxIdleTimeNanos;
            }
        }
    }
}
