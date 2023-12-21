/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.util;

import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.metrics.DescriptiveStatisticsHistogram;
import org.apache.flink.streaming.runtime.streamrecord.LatencyMarker;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link LatencyStats} objects are used to track and report on the behavior of latencies across
 * measurements.
 */
public class LatencyStats {
    private final Map<String, DescriptiveStatisticsHistogram> latencyStats = new HashMap<>();
    private final MetricGroup metricGroup;
    private final int historySize;
    private final int subtaskIndex;
    private final OperatorID operatorId;
    private final Granularity granularity;

    public LatencyStats(
            MetricGroup metricGroup,
            int historySize,
            int subtaskIndex,
            OperatorID operatorID,
            Granularity granularity) {
        this.metricGroup = metricGroup;
        this.historySize = historySize;
        this.subtaskIndex = subtaskIndex;
        this.operatorId = operatorID;
        this.granularity = granularity;
    }

    //
    // 1 LatencyStats中的延迟最终会转化为直方图表示，通过直方图就可以统计出延时的最大值、最小值、均值、分位值（quantile）等指标
    // 2 延迟是由当前时间戳减去LatencyMarker携带的时间戳得到的，所以在Sink端统计到的就是全链路延迟了
    // 3
    public void reportLatency(LatencyMarker marker) {
        final String uniqueName =
                granularity.createUniqueHistogramName(marker, operatorId, subtaskIndex);

        DescriptiveStatisticsHistogram latencyHistogram = this.latencyStats.get(uniqueName);
        if (latencyHistogram == null) {
            latencyHistogram = new DescriptiveStatisticsHistogram(this.historySize);
            this.latencyStats.put(uniqueName, latencyHistogram);
            granularity
                    .createSourceMetricGroups(metricGroup, marker, operatorId, subtaskIndex)
                    .addGroup("operator_id", String.valueOf(operatorId))
                    .addGroup("operator_subtask_index", String.valueOf(subtaskIndex))
                    .histogram("latency", latencyHistogram);
        }

        long now = System.currentTimeMillis();
        latencyHistogram.update(now - marker.getMarkedTime());
    }

    /*

    在创建LatencyStats之前，先要根据metrics.latency.granularity配置项来确定延迟监控的粒度，分为以下3档：
        single：不区分源  不区分子任务
        operator（默认值）： 区分源 不区分子任务
        subtask： 区分源  区分子任务

        一般情况下采用默认的operator粒度即可，这样在Sink端观察到的latency metric就是我们最想要的全链路（端到端）延迟，
        以下也是以该粒度讲解。subtask粒度太细，会增大所有并行度的负担，不建议使用
     */
    /** Granularity for latency metrics. */
    public enum Granularity {
        SINGLE {
            @Override
            String createUniqueHistogramName(
                    LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex) {
                return String.valueOf(operatorId) + operatorSubtaskIndex;
            }

            @Override
            MetricGroup createSourceMetricGroups(
                    MetricGroup base,
                    LatencyMarker marker,
                    OperatorID operatorId,
                    int operatorSubtaskIndex) {
                return base;
            }
        },
        OPERATOR {
            @Override
            String createUniqueHistogramName(
                    LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex) {
                return String.valueOf(marker.getOperatorId()) + operatorId + operatorSubtaskIndex;
            }

            @Override
            MetricGroup createSourceMetricGroups(
                    MetricGroup base,
                    LatencyMarker marker,
                    OperatorID operatorId,
                    int operatorSubtaskIndex) {
                return base.addGroup("source_id", String.valueOf(marker.getOperatorId()));
            }
        },
        SUBTASK {
            @Override
            String createUniqueHistogramName(
                    LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex) {
                return String.valueOf(marker.getOperatorId())
                        + marker.getSubtaskIndex()
                        + operatorId
                        + operatorSubtaskIndex;
            }

            @Override
            MetricGroup createSourceMetricGroups(
                    MetricGroup base,
                    LatencyMarker marker,
                    OperatorID operatorId,
                    int operatorSubtaskIndex) {
                return base.addGroup("source_id", String.valueOf(marker.getOperatorId()))
                        .addGroup("source_subtask_index", String.valueOf(marker.getSubtaskIndex()));
            }
        };

        abstract String createUniqueHistogramName(
                LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex);

        abstract MetricGroup createSourceMetricGroups(
                MetricGroup base,
                LatencyMarker marker,
                OperatorID operatorId,
                int operatorSubtaskIndex);
    }
}
