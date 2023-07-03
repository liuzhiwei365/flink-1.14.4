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

package org.apache.flink.streaming.api.datastream;

import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.operators.util.OperatorValidationUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.operators.StreamSource;
import org.apache.flink.streaming.api.transformations.LegacySourceTransformation;
import org.apache.flink.streaming.api.transformations.SourceTransformation;

/**
 * The DataStreamSource represents the starting point of a DataStream.
 *
 * @param <T> Type of the elements in the DataStream created from the this source.
 */
@Public
public class DataStreamSource<T> extends SingleOutputStreamOperator<T> {

    private final boolean isParallel;

    public DataStreamSource(
            StreamExecutionEnvironment environment,
            TypeInformation<T> outTypeInfo,
            StreamSource<T, ?> operator,
            boolean isParallel,
            String sourceName) {
        this(
                environment,
                outTypeInfo,
                operator,
                isParallel,
                sourceName,
                Boundedness.CONTINUOUS_UNBOUNDED);
    }

    /** The constructor used to create legacy sources. */
    public DataStreamSource(
            StreamExecutionEnvironment environment,
            TypeInformation<T> outTypeInfo,
            StreamSource<T, ?> operator,
            boolean isParallel,
            String sourceName,
            Boundedness boundedness) {
        super(
                environment,
                // LegacySourceTransformation 功能比 SourceTransformation少
                // 入参 operator 就是一个SourceFunction
                new LegacySourceTransformation<>(
                        sourceName,
                        operator,
                        outTypeInfo,
                        environment.getParallelism(),
                        boundedness));

        this.isParallel = isParallel;
        if (!isParallel) {
            setParallelism(1);
        }
    }

    /**
     * Constructor for "deep" sources that manually set up (one or more) custom configured complex
     * operators.
     */
    public DataStreamSource(SingleOutputStreamOperator<T> operator) {
        super(operator.environment, operator.getTransformation());
        this.isParallel = true;
    }

    /** Constructor for new Sources (FLIP-27). */
    public DataStreamSource(
            StreamExecutionEnvironment environment,
            Source<T, ?, ?> source,
            WatermarkStrategy<T> watermarkStrategy,
            TypeInformation<T> outTypeInfo,
            String sourceName) {
        super(
                environment,
                // SourceTransformation 比 LegacySourceTransformation 复杂
                // Source 接口有创建分片枚举器 , 分片序列化 等功能 ; 比如 KafkaSource

                // 还要传入 WatermarkStrategy 策略 ,在构建StreamGraph 的时候,
                // 会 使用 SourceTransformationTranslator 来 翻译 该 SourceTransformation
                // 这样, 构造的StreamNode 中 包含的成员 是 SourceOperatorFactory 对象
                // 我们的  WatermarkStrategy 策略信息就会在 SourceOperatorFactory 对象中

                // 而在taskMananger 执行任务的时候, SourceOperatorFactory 工厂调用 createStreamOperator来 创建 SourceOperator对象
                // 其内部也含有 各类水印的 组件
                new SourceTransformation<>(
                        sourceName,
                        source,
                        watermarkStrategy,
                        outTypeInfo,
                        environment.getParallelism()));
        this.isParallel = true;
    }

    @VisibleForTesting
    boolean isParallel() {
        return isParallel;
    }

    @Override
    public DataStreamSource<T> setParallelism(int parallelism) {
        OperatorValidationUtils.validateParallelism(parallelism, isParallel);
        super.setParallelism(parallelism);
        return this;
    }
}
