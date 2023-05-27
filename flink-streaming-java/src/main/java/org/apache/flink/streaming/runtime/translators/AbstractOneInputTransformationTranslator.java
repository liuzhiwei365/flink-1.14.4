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

package org.apache.flink.streaming.runtime.translators;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.graph.SimpleTransformationTranslator;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.operators.StreamOperatorFactory;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * A utility base class for one input {@link Transformation transformations} that provides a
 * function for configuring common graph properties.
 */
abstract class AbstractOneInputTransformationTranslator<IN, OUT, OP extends Transformation<OUT>>
        extends SimpleTransformationTranslator<OUT, OP> {

    protected Collection<Integer> translateInternal(
            final Transformation<OUT> transformation,
            final StreamOperatorFactory<OUT> operatorFactory,
            final TypeInformation<IN> inputType,
            @Nullable final KeySelector<IN, ?> stateKeySelector,
            @Nullable final TypeInformation<?> stateKeyType,
            final Context context) {
        checkNotNull(transformation);
        checkNotNull(operatorFactory);
        checkNotNull(inputType);
        checkNotNull(context);

        // 最初级流图
        final StreamGraph streamGraph = context.getStreamGraph();
        final String slotSharingGroup = context.getSlotSharingGroup();
        final int transformationId = transformation.getId();
        final ExecutionConfig executionConfig = streamGraph.getExecutionConfig();

        // 构建 streamNode 并将之 放入到 streamGraph 的成员变量 streamNodes 中, 它是一个 Map<Integer, StreamNode> 对象
        streamGraph.addOperator(
                transformationId,
                slotSharingGroup,
                transformation.getCoLocationGroupKey(),
                operatorFactory,
                inputType,
                transformation.getOutputType(),
                transformation.getName());

        // 将key选择器 设置给 相应的 streamNode
        if (stateKeySelector != null) {
            TypeSerializer<?> keySerializer = stateKeyType.createSerializer(executionConfig);
            streamGraph.setOneInputStateKey(transformationId, stateKeySelector, keySerializer);
        }

        int parallelism =
                transformation.getParallelism() != ExecutionConfig.PARALLELISM_DEFAULT
                        ? transformation.getParallelism()
                        : executionConfig.getParallelism();

        // 设置本顶点 的并行度 和最大并行度 属性 （这两个属性会在 streamNode 对象中保存）
        streamGraph.setParallelism(transformationId, parallelism);
        streamGraph.setMaxParallelism(transformationId, transformation.getMaxParallelism());

        final List<Transformation<?>> parentTransformations = transformation.getInputs();
        // 因为这是 OneInputTransformationTranslator,所以 父transformation 只能有一个
        checkState(
                parentTransformations.size() == 1,
                "Expected exactly one input transformation but found "
                        + parentTransformations.size());

        // 上述逻辑把 streamNode 的 属性设置好了, 然后 我们 去设置 上游与 本顶点的 边
        for (Integer inputId : context.getStreamNodeIds(parentTransformations.get(0))) {
            // inputId 代表上游id , transformationId 代表 本顶点id
            streamGraph.addEdge(inputId, transformationId, 0);
        }

        return Collections.singleton(transformationId);
    }
}
