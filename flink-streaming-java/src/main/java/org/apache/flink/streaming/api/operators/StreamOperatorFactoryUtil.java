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

package org.apache.flink.streaming.api.operators;

import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.operators.coordination.OperatorEventDispatcher;
import org.apache.flink.streaming.api.graph.StreamConfig;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeServiceAware;
import org.apache.flink.streaming.runtime.tasks.StreamTask;

import java.util.Optional;
import java.util.function.Supplier;

/** A utility to instantiate new operators with a given factory. */
public class StreamOperatorFactoryUtil {
    /**
     * Creates a new operator using a factory and makes sure that all special factory traits are
     * properly handled.
     *
     * @param operatorFactory the operator factory.
     * @param containingTask the containing task.
     * @param configuration the configuration of the operator.
     * @param output the output of the operator.
     * @param operatorEventDispatcher the operator event dispatcher for communication between
     *     operator and coordinators.
     * @return a newly created and configured operator, and the {@link ProcessingTimeService}
     *     instance it can access.
     */
    public static <OUT, OP extends StreamOperator<OUT>>
            Tuple2<OP, Optional<ProcessingTimeService>> createOperator(
                    StreamOperatorFactory<OUT> operatorFactory,
                    StreamTask<OUT, ?> containingTask,
                    StreamConfig configuration,
                    Output<StreamRecord<OUT>> output,
                    OperatorEventDispatcher operatorEventDispatcher) {

        // 构造 信箱执行器
        MailboxExecutor mailboxExecutor =
                containingTask
                        .getMailboxExecutorFactory()
                        .createExecutor(configuration.getChainIndex());

        if (operatorFactory instanceof YieldingOperatorFactory) {
            ((YieldingOperatorFactory<?>) operatorFactory).setMailboxExecutor(mailboxExecutor);
        }

        // 构造 定时服务
        // StreamTask 构造的时候 创建的成员变量 timeService 会被 封装进入 ProcessingTimeServiceImpl对象, 然后返回
        final Supplier<ProcessingTimeService> processingTimeServiceFactory =
                () ->
                        containingTask
                                .getProcessingTimeServiceFactory()
                                .createProcessingTimeService(mailboxExecutor);

        final ProcessingTimeService processingTimeService;
        // operatorFactory工厂 是否实现了 ProcessingTimeServiceAware 可感知接口
        // 如果没有  就不把 定时服务设置给该工厂了

        //  WatermarkAssignerOperatorFactory 和 SourceOperatorFactory 都
        // 实现了ProcessingTimeServiceAware该接口
        //  所以会将 上面创建的 processingTimeServiceFactory 设置给 该算子
        //  定时器工厂设置给算子工厂
        //  算子工厂在 调用 createStreamOperator 方法的时候 会利用 最终来源于 StreamTask的 timeService 传递给该算子
        //  于是我们在该算子中可以使用 定时器的功能了

        if (operatorFactory instanceof ProcessingTimeServiceAware) {

            processingTimeService = processingTimeServiceFactory.get();

            ((ProcessingTimeServiceAware) operatorFactory)
                    .setProcessingTimeService(processingTimeService);
        } else {
            processingTimeService = null;
        }

        // TODO: what to do with ProcessingTimeServiceAware?
        // 如果是 WatermarkAssignerOperatorFactory, 创建 WatermarkAssignerOperator 对象
        OP op =
                operatorFactory.createStreamOperator(
                        new StreamOperatorParameters<>(
                                containingTask,
                                configuration,
                                output,
                                processingTimeService != null
                                        ? () -> processingTimeService
                                        : processingTimeServiceFactory,
                                operatorEventDispatcher));
        return new Tuple2<>(op, Optional.ofNullable(processingTimeService));
    }
}
