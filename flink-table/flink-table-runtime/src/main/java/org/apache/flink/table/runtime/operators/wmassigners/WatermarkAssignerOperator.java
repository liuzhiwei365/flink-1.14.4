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

package org.apache.flink.table.runtime.operators.wmassigners;

import org.apache.flink.api.common.functions.util.FunctionUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeCallback;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.streaming.runtime.watermarkstatus.WatermarkStatus;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.generated.WatermarkGenerator;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * A stream operator that extracts timestamps from stream elements and generates periodic
 * watermarks.
 */

// WatermarkAssignerOperator 是针对 sql 和 表 的;  注意看 泛形 是 RowData
// TimestampsAndWatermarksOperator  是针对  普通 flink任务
public class WatermarkAssignerOperator extends AbstractStreamOperator<RowData>
        implements OneInputStreamOperator<RowData, RowData>, ProcessingTimeCallback {

    private static final long serialVersionUID = 1L;

    private final int rowtimeFieldIndex;

    private final long idleTimeout;

    private final WatermarkGenerator watermarkGenerator;

    private transient long lastWatermark;

    private transient long watermarkInterval;

    private transient long currentWatermark;

    private transient long lastRecordTime;

    private transient WatermarkStatus currentStatus = WatermarkStatus.ACTIVE;

    /**
     * Create a watermark assigner operator.
     *
     * @param rowtimeFieldIndex the field index to extract event timestamp
     * @param watermarkGenerator the watermark generator
     * @param idleTimeout (idleness checking timeout)
     */
    public WatermarkAssignerOperator(
            int rowtimeFieldIndex,
            WatermarkGenerator watermarkGenerator,
            long idleTimeout,
            ProcessingTimeService processingTimeService) {

        this.rowtimeFieldIndex = rowtimeFieldIndex;
        this.watermarkGenerator = watermarkGenerator;

        this.idleTimeout = idleTimeout;
        this.chainingStrategy = ChainingStrategy.ALWAYS;

        this.processingTimeService = checkNotNull(processingTimeService);
    }

    @Override
    public void open() throws Exception {
        super.open();

        // watermark and timestamp should start from 0
        this.currentWatermark = 0;
        this.watermarkInterval = getExecutionConfig().getAutoWatermarkInterval();
        this.lastRecordTime = getProcessingTimeService().getCurrentProcessingTime();

        if (watermarkInterval > 0) {
            long now = getProcessingTimeService().getCurrentProcessingTime();
            getProcessingTimeService().registerTimer(now + watermarkInterval, this);
        }

        FunctionUtils.setFunctionRuntimeContext(watermarkGenerator, getRuntimeContext());
        FunctionUtils.openFunction(watermarkGenerator, new Configuration());
    }

    @Override
    public void processElement(StreamRecord<RowData> element) throws Exception {
        // 如果 该算子的状态是 IDLE, 随便来一条数据后, 则激活了 watermarkAssignerOperator的状态 为 Active
        // 并把  该状态通过 output组件 发给下游
        if (idleTimeout > 0 && currentStatus.equals(WatermarkStatus.IDLE)) {

            emitWatermarkStatus(WatermarkStatus.ACTIVE);
            // 用当前系统时间 来重置  lastRecordTime（上一次来数据 的时间）
            lastRecordTime = getProcessingTimeService().getCurrentProcessingTime();
        }

        RowData row = element.getValue();
        if (row.isNullAt(rowtimeFieldIndex)) {
            throw new RuntimeException(
                    "RowTime field should not be null,"
                            + " please convert it to a non-null long value.");
        }

        // 按照用户指定的方式 , 把 watermark 从真实的数据中抽取出来 （使用watermark的话, 一定是基于）
        // 默认   会用 数据中的时间戳再减去 1  得到 watermark 的时间戳
        Long watermark = watermarkGenerator.currentWatermark(row);

        // 用新生成的 watermark 时间戳 更新 currentWatermark
        if (watermark != null) {
            currentWatermark = Math.max(currentWatermark, watermark);
        }
        // 数据用于抽取 watermark 后，还是得正常发送
        output.collect(element);

        // current_wm - last_wm > interval
        //  当  当前的watermark  减去 上一次推动的watermark  大于一定的间隙才能推动watermark , 避免频繁地推动
        if (currentWatermark - lastWatermark > watermarkInterval) {
            advanceWatermark();
        }
    }

    private void advanceWatermark() {
        if (currentWatermark > lastWatermark) {
            lastWatermark = currentWatermark;
            // emit watermark
            output.emitWatermark(new Watermark(currentWatermark));
        }
    }

    @Override
    public void onProcessingTime(long timestamp) throws Exception {
        advanceWatermark();

        if (idleTimeout > 0 && currentStatus.equals(WatermarkStatus.ACTIVE)) {
            final long currentTime = getProcessingTimeService().getCurrentProcessingTime();
            if (currentTime - lastRecordTime > idleTimeout) {
                // mark the channel as idle to ignore watermarks from this channel
                emitWatermarkStatus(WatermarkStatus.IDLE);
            }
        }

        // register next timer
        long now = getProcessingTimeService().getCurrentProcessingTime();
        getProcessingTimeService().registerTimer(now + watermarkInterval, this);
    }

    /**
     * Override the base implementation to completely ignore watermarks propagated from upstream (we
     * rely only on the {@link WatermarkGenerator} to emit watermarks from here).
     */
    @Override
    public void processWatermark(Watermark mark) throws Exception {
        // if we receive a Long.MAX_VALUE watermark we forward it since it is used
        // to signal the end of input and to not block watermark progress downstream
        if (mark.getTimestamp() == Long.MAX_VALUE && currentWatermark != Long.MAX_VALUE) {
            if (idleTimeout > 0 && currentStatus.equals(WatermarkStatus.IDLE)) {
                // mark the channel active
                emitWatermarkStatus(WatermarkStatus.ACTIVE);
            }
            currentWatermark = Long.MAX_VALUE;
            output.emitWatermark(mark);
        }
    }

    @Override
    public void processWatermarkStatus(WatermarkStatus watermarkStatus) throws Exception {
        emitWatermarkStatus(watermarkStatus);
    }

    private void emitWatermarkStatus(WatermarkStatus watermarkStatus) {
        this.currentStatus = watermarkStatus;
        output.emitWatermarkStatus(watermarkStatus);
    }

    @Override
    public void finish() throws Exception {
        // all records have been processed, emit a final watermark
        processWatermark(Watermark.MAX_WATERMARK);
    }

    @Override
    public void close() throws Exception {
        FunctionUtils.closeFunction(watermarkGenerator);
        super.close();
    }
}
