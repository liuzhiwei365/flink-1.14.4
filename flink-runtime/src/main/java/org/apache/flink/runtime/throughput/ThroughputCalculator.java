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

package org.apache.flink.runtime.throughput;

import org.apache.flink.util.clock.Clock;

/** Class for measuring the throughput based on incoming data size and measurement period. */
public class ThroughputCalculator {
    private static final long NOT_TRACKED = -1;
    private final Clock clock;
    private final ThroughputEMA throughputEMA;

    // 当前累计的数量总量, 计算吞吐量后会被重新设置为 0
    private long currentAccumulatedDataSize;
    private long currentMeasurementTime;
    private long measurementStartTime = NOT_TRACKED;

    public ThroughputCalculator(Clock clock, int numberOfSamples) {
        this.clock = clock;
        this.throughputEMA = new ThroughputEMA(numberOfSamples);
    }

    public void incomingDataSize(long receivedDataSize) {
        // Force resuming measurement.
        if (measurementStartTime == NOT_TRACKED) {
            measurementStartTime = clock.absoluteTimeMillis();
        }
        currentAccumulatedDataSize += receivedDataSize;
    }

    /**
     * Mark when the time should not be taken into account.
     *
     * @param absoluteTimeMillis Current absolute time received outside to avoid performance drop on
     *     calling {@link Clock#absoluteTimeMillis()} inside of the method.
     */
    public void pauseMeasurement(long absoluteTimeMillis) {
        if (measurementStartTime != NOT_TRACKED) {
            currentMeasurementTime += absoluteTimeMillis - measurementStartTime;
        }
        measurementStartTime = NOT_TRACKED;
    }

    /**
     * Mark when the time should be included to the throughput calculation.
     *
     * @param absoluteTimeMillis Current absolute time received outside to avoid performance drop on
     *     calling {@link Clock#absoluteTimeMillis()} inside of the method.
     */
    public void resumeMeasurement(long absoluteTimeMillis) {
        if (measurementStartTime == NOT_TRACKED) {
            measurementStartTime = absoluteTimeMillis;
        }
    }

    /** @return Calculated throughput based on the collected data for the last period. */
    public long calculateThroughput() {
        if (measurementStartTime != NOT_TRACKED) {
            // 获取当前时间
            long absoluteTimeMillis = clock.absoluteTimeMillis();
            // 获取计量吞吐量期间时长 （单位毫秒）
            currentMeasurementTime += absoluteTimeMillis - measurementStartTime;
            // 设置下一个计量起始时间
            measurementStartTime = absoluteTimeMillis;
        }

        // 计算每毫秒 的 吞吐量, 方法参数为这段时间累积的数据量 和 时长
        long throughput =
                throughputEMA.calculateThroughput(currentAccumulatedDataSize, currentMeasurementTime);

        // 变量重置
        currentAccumulatedDataSize = currentMeasurementTime = 0;

        return throughput;
    }
}
