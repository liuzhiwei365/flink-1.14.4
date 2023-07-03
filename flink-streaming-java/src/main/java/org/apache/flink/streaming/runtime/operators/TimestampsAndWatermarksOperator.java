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

package org.apache.flink.streaming.runtime.operators;

import org.apache.flink.api.common.eventtime.NoWatermarksGenerator;
import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeCallback;
import org.apache.flink.streaming.runtime.watermarkstatus.WatermarkStatus;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * A stream operator that may do one or both of the following: extract timestamps from events and
 * generate watermarks.
 *
 * <p>These two responsibilities run in the same operator rather than in two different ones, because
 * the implementation of the timestamp assigner and the watermark generator is frequently in the
 * same class (and should be run in the same instance), even though the separate interfaces support
 * the use of different classes.
 *
 * @param <T> The type of the input elements
 */

// WatermarkAssignerOperator 是针对 sql 和 表 的
// TimestampsAndWatermarksOperator  是针对  普通 flink任务

// 定时器调用 onProcessingTime 方法 ，是利用 ProcessingTimeCallback 这个接口来调的
// 它们都是 负责 watermark 的生成逻辑

// 本类只负责watermark 的生成逻辑 , 不负责watermark 对齐逻辑
// StatusWatermarkValve 负责 下游算子 的 watermark 对齐
public class TimestampsAndWatermarksOperator<T> extends AbstractStreamOperator<T>
        implements OneInputStreamOperator<T, T>, ProcessingTimeCallback {

    private static final long serialVersionUID = 1L;

    private final WatermarkStrategy<T> watermarkStrategy;

    /** The timestamp assigner. */
    private transient TimestampAssigner<T> timestampAssigner;

    /** The watermark generator, initialized during runtime. */
    private transient WatermarkGenerator<T> watermarkGenerator;

    /** The watermark output gateway, initialized during runtime. */
    private transient WatermarkOutput wmOutput;

    /** The interval (in milliseconds) for periodic watermark probes. Initialized during runtime. */
    private transient long watermarkInterval;

    /** Whether to emit intermediate watermarks or only one final watermark at the end of input. */
    private final boolean emitProgressiveWatermarks;

    public TimestampsAndWatermarksOperator(
            WatermarkStrategy<T> watermarkStrategy, boolean emitProgressiveWatermarks) {
        this.watermarkStrategy = checkNotNull(watermarkStrategy);
        this.emitProgressiveWatermarks = emitProgressiveWatermarks;
        this.chainingStrategy = ChainingStrategy.DEFAULT_CHAINING_STRATEGY;
    }

    @Override
    public void open() throws Exception {
        super.open();

        // watermarkStrategy 最开始是由用户调用 dataStream api 时传入的
        // 用户传入后，会被封装到  TimestampsAndWatermarksTransformer 中
        // 作业提交后，在构建StreamGraph 的时候 会从TimestampsAndWatermarksTransformer 取出
        // 然后放入 TimestampsAndWatermarksOperator中 (也就是本算子中),
        // 再用SimpleOperatorFactory 把 本算子包裹 赋值给 StreamNode

        // 时间戳分配器 用于从用户数据中提取时间 , 这个时间后续会赋值给 StreamRecord
        timestampAssigner = watermarkStrategy.createTimestampAssigner(this::getMetricGroup);

        // 水印生成器  当每条用户数据来的时候  和  周期性时间点  分别有回调逻辑 ； 且不同实现类 逻辑差别很大
        watermarkGenerator =
                emitProgressiveWatermarks
                        ? watermarkStrategy.createWatermarkGenerator(this::getMetricGroup)
                        : new NoWatermarksGenerator<>();

        // WatermarkEmitter 对普通 output 做了包装 和加强 ，它能 向下游发送 WatermarkStatus 和 Watermark
        wmOutput = new WatermarkEmitter(output);

        watermarkInterval = getExecutionConfig().getAutoWatermarkInterval();
        if (watermarkInterval > 0 && emitProgressiveWatermarks) {
            // 获取当前系统时间
            final long now = getProcessingTimeService().getCurrentProcessingTime();
            // 注册定时器,  过了 watermarkInterval 间隔的时间后, 触发定时器异步调用 onProcessingTime 向下游发送watermark

            // 该定时器是 构造 StreamTask 的时候创建的 由timeService 成员变量维护, StreamTask构造 OperatorChain 的时候 各种传递
            // 最终传递给了 父类 AbstractStreamOperator 的 processingTimeService成员

            // 这个地方的实现类是  ProcessingTimeServiceImpl  //最终 内部会用 jdk 的 ScheduledThreadPoolExecutor 对象
            // 的 schedule 方法来定时
            getProcessingTimeService().registerTimer(now + watermarkInterval, this);
        }
    }

    // 算子的主逻辑,处理每条数据
    @Override
    public void processElement(final StreamRecord<T> element) throws Exception {
        final T event = element.getValue();
        // StreamRecord 类中又一个 timestamp 成员
        final long previousTimestamp =
                element.hasTimestamp() ? element.getTimestamp() : Long.MIN_VALUE;

        final long newTimestamp = timestampAssigner.extractTimestamp(event, previousTimestamp);

        // 给用户数据 打上时间戳
        element.setTimestamp(newTimestamp);
        // 并且发往下游
        output.collect(element);

        //
        watermarkGenerator.onEvent(event, newTimestamp, wmOutput);
    }

    // onProcessingTime 是定时触发调用的
    // 而第一次定时是在本类算子 的初始化 open()方法
    @Override
    public void onProcessingTime(long timestamp) throws Exception {
        //  WatermarkGenerator的子类 BoundedOutOfOrdernessWatermarks的 生成Watermark 的逻辑就是:
        //   用 maxTimestamp - outOfOrdernessMillis - 1 作为其构造参数

        // 向下游发送watermark
        watermarkGenerator.onPeriodicEmit(wmOutput);

        // 重新注册定时器
        final long now = getProcessingTimeService().getCurrentProcessingTime();
        getProcessingTimeService().registerTimer(now + watermarkInterval, this);
    }

    /**
     * Override the base implementation to completely ignore watermarks propagated from upstream,
     * except for the "end of time" watermark.
     */
    @Override
    public void processWatermark(org.apache.flink.streaming.api.watermark.Watermark mark)
            throws Exception {
        // if we receive a Long.MAX_VALUE watermark we forward it since it is used
        // to signal the end of input and to not block watermark progress downstream
        if (mark.getTimestamp() == Long.MAX_VALUE) {
            wmOutput.emitWatermark(Watermark.MAX_WATERMARK);
        }
    }

    @Override
    public void finish() throws Exception {
        super.finish();
        watermarkGenerator.onPeriodicEmit(wmOutput);
    }

    // ------------------------------------------------------------------------

    /**
     * Implementation of the {@code WatermarkEmitter}, based on the components that are available
     * inside a stream operator.
     */
    public static final class WatermarkEmitter implements WatermarkOutput {

        private final Output<?> output;

        private long currentWatermark;

        private boolean idle;

        public WatermarkEmitter(Output<?> output) {
            this.output = output;
            this.currentWatermark = Long.MIN_VALUE;
        }

        @Override
        public void emitWatermark(Watermark watermark) {
            final long ts = watermark.getTimestamp();

            if (ts <= currentWatermark) {
                return;
            }

            currentWatermark = ts;

            markActive();

            output.emitWatermark(new org.apache.flink.streaming.api.watermark.Watermark(ts));
        }

        @Override
        public void markIdle() {
            if (!idle) {
                idle = true;
                output.emitWatermarkStatus(WatermarkStatus.IDLE);
            }
        }

        @Override
        public void markActive() {
            if (idle) {
                idle = false;
                output.emitWatermarkStatus(WatermarkStatus.ACTIVE);
            }
        }
    }
}
