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

package org.apache.flink.streaming.api.graph;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.operators.ResourceSpec;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.core.memory.ManagedMemoryUseCase;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.jobgraph.tasks.TaskInvokable;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;
import org.apache.flink.streaming.api.operators.CoordinatedOperatorFactory;
import org.apache.flink.streaming.api.operators.SimpleOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkState;

/** Class representing the operators in the streaming programs, with all their properties. */
@Internal
public class StreamNode {

    private final int id;
    private int parallelism;

    // 1 动态扩容的上限   2 划分状态时键组的个数
    private int maxParallelism;

    private ResourceSpec minResources = ResourceSpec.DEFAULT;
    private ResourceSpec preferredResources = ResourceSpec.DEFAULT;
    private final Map<ManagedMemoryUseCase, Integer> managedMemoryOperatorScopeUseCaseWeights =
            new HashMap<>();
    private final Set<ManagedMemoryUseCase> managedMemorySlotScopeUseCases = new HashSet<>();
    private long bufferTimeout;
    private final String operatorName;
    private @Nullable String slotSharingGroup;
    private @Nullable String coLocationGroup;

    // 只有当 该 node 的输入都是 keyed stream , 于key 相关的选择器 和 序列化器才会有值
    // 多个 key 选择器
    private KeySelector<?, ?>[] statePartitioners = new KeySelector[0];
    // key 的 序列化器 , 有key的情况 仅针对 keyed stream
    private TypeSerializer<?> stateKeySerializer;

    //  算子工厂 可以创建算子; taskManager 在运行任务的时候 会调用工厂类 创建算子
    private StreamOperatorFactory<?> operatorFactory;

    private TypeSerializer<?>[] typeSerializersIn = new TypeSerializer[0];
    private TypeSerializer<?> typeSerializerOut;

    private List<StreamEdge> inEdges = new ArrayList<StreamEdge>();
    private List<StreamEdge> outEdges = new ArrayList<StreamEdge>();

    //  StreamTask 等 在 TaskManager 中执行的类
    private final Class<? extends TaskInvokable> jobVertexClass;

    // 维护输入输出格式
    private InputFormat<?, ?> inputFormat;
    private OutputFormat<?> outputFormat;

    private String transformationUID;
    private String userHash;

    private final Map<Integer, StreamConfig.InputRequirement> inputRequirements = new HashMap<>();

    @VisibleForTesting
    public StreamNode(
            Integer id,
            @Nullable String slotSharingGroup,
            @Nullable String coLocationGroup,
            StreamOperator<?> operator,
            String operatorName,
            Class<? extends TaskInvokable> jobVertexClass) {
        this(
                id,
                slotSharingGroup,
                coLocationGroup,
                SimpleOperatorFactory.of(operator),
                operatorName,
                jobVertexClass);
    }

    public StreamNode(
            Integer id,
            @Nullable String slotSharingGroup,
            @Nullable String coLocationGroup,
            StreamOperatorFactory<?> operatorFactory,
            String operatorName,
            Class<? extends TaskInvokable> jobVertexClass) {
        this.id = id;
        this.operatorName = operatorName;
        this.operatorFactory = operatorFactory;
        this.jobVertexClass = jobVertexClass;
        this.slotSharingGroup = slotSharingGroup;
        this.coLocationGroup = coLocationGroup;
    }

    public void addInEdge(StreamEdge inEdge) {
        checkState(
                outEdges.stream().noneMatch(inEdge::equals),
                "Adding not unique edge = %s to existing outEdges = %s",
                inEdge,
                inEdges);
        if (inEdge.getTargetId() != getId()) {
            throw new IllegalArgumentException("Destination id doesn't match the StreamNode id");
        } else {
            inEdges.add(inEdge);
        }
    }

    public void addOutEdge(StreamEdge outEdge) {
        checkState(
                outEdges.stream().noneMatch(outEdge::equals),
                "Adding not unique edge = %s to existing outEdges = %s",
                outEdge,
                outEdges);
        if (outEdge.getSourceId() != getId()) {
            throw new IllegalArgumentException("Source id doesn't match the StreamNode id");
        } else {
            outEdges.add(outEdge);
        }
    }

    public List<StreamEdge> getOutEdges() {
        return outEdges;
    }

    public List<StreamEdge> getInEdges() {
        return inEdges;
    }

    public List<Integer> getOutEdgeIndices() {
        List<Integer> outEdgeIndices = new ArrayList<Integer>();

        for (StreamEdge edge : outEdges) {
            outEdgeIndices.add(edge.getTargetId());
        }

        return outEdgeIndices;
    }

    public List<Integer> getInEdgeIndices() {
        List<Integer> inEdgeIndices = new ArrayList<Integer>();

        for (StreamEdge edge : inEdges) {
            inEdgeIndices.add(edge.getSourceId());
        }

        return inEdgeIndices;
    }

    public int getId() {
        return id;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(Integer parallelism) {
        this.parallelism = parallelism;
    }

    /**
     * Get the maximum parallelism for this stream node.
     *
     * @return Maximum parallelism
     */
    int getMaxParallelism() {
        return maxParallelism;
    }

    /**
     * Set the maximum parallelism for this stream node.
     *
     * @param maxParallelism Maximum parallelism to be set
     */
    void setMaxParallelism(int maxParallelism) {
        this.maxParallelism = maxParallelism;
    }

    public ResourceSpec getMinResources() {
        return minResources;
    }

    public ResourceSpec getPreferredResources() {
        return preferredResources;
    }

    public void setResources(ResourceSpec minResources, ResourceSpec preferredResources) {
        this.minResources = minResources;
        this.preferredResources = preferredResources;
    }

    public void setManagedMemoryUseCaseWeights(
            Map<ManagedMemoryUseCase, Integer> operatorScopeUseCaseWeights,
            Set<ManagedMemoryUseCase> slotScopeUseCases) {
        managedMemoryOperatorScopeUseCaseWeights.putAll(operatorScopeUseCaseWeights);
        managedMemorySlotScopeUseCases.addAll(slotScopeUseCases);
    }

    public Map<ManagedMemoryUseCase, Integer> getManagedMemoryOperatorScopeUseCaseWeights() {
        return Collections.unmodifiableMap(managedMemoryOperatorScopeUseCaseWeights);
    }

    public Set<ManagedMemoryUseCase> getManagedMemorySlotScopeUseCases() {
        return Collections.unmodifiableSet(managedMemorySlotScopeUseCases);
    }

    public long getBufferTimeout() {
        return bufferTimeout;
    }

    public void setBufferTimeout(Long bufferTimeout) {
        this.bufferTimeout = bufferTimeout;
    }

    @VisibleForTesting
    public StreamOperator<?> getOperator() {
        return (StreamOperator<?>) ((SimpleOperatorFactory) operatorFactory).getOperator();
    }

    public StreamOperatorFactory<?> getOperatorFactory() {
        return operatorFactory;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setSerializersIn(TypeSerializer<?>... typeSerializersIn) {
        checkArgument(typeSerializersIn.length > 0);
        this.typeSerializersIn = typeSerializersIn;
    }

    public TypeSerializer<?>[] getTypeSerializersIn() {
        return typeSerializersIn;
    }

    public TypeSerializer<?> getTypeSerializerIn(int index) {
        return typeSerializersIn[index];
    }

    public TypeSerializer<?> getTypeSerializerOut() {
        return typeSerializerOut;
    }

    public void setSerializerOut(TypeSerializer<?> typeSerializerOut) {
        this.typeSerializerOut = typeSerializerOut;
    }

    public Class<? extends TaskInvokable> getJobVertexClass() {
        return jobVertexClass;
    }

    public InputFormat<?, ?> getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(InputFormat<?, ?> inputFormat) {
        this.inputFormat = inputFormat;
    }

    public OutputFormat<?> getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat<?> outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setSlotSharingGroup(@Nullable String slotSharingGroup) {
        this.slotSharingGroup = slotSharingGroup;
    }

    @Nullable
    public String getSlotSharingGroup() {
        return slotSharingGroup;
    }

    public void setCoLocationGroup(@Nullable String coLocationGroup) {
        this.coLocationGroup = coLocationGroup;
    }

    public @Nullable String getCoLocationGroup() {
        return coLocationGroup;
    }

    public boolean isSameSlotSharingGroup(StreamNode downstreamVertex) {
        return (slotSharingGroup == null && downstreamVertex.slotSharingGroup == null)
                || (slotSharingGroup != null
                        && slotSharingGroup.equals(downstreamVertex.slotSharingGroup));
    }

    @Override
    public String toString() {
        return operatorName + "-" + id;
    }

    public KeySelector<?, ?>[] getStatePartitioners() {
        return statePartitioners;
    }

    // 三个调用点:
    // StreamGraph 的 setOneInputStateKey 方法    数组长度为1
    // StreamGraph 的 setTwoInputStateKey 方法    数组长度为2
    // StreamGraph 的 setMultipleInputStateKey 方法  数组长度大于2
    public void setStatePartitioners(KeySelector<?, ?>... statePartitioners) {
        checkArgument(statePartitioners.length > 0);
        this.statePartitioners = statePartitioners;
    }

    public TypeSerializer<?> getStateKeySerializer() {
        return stateKeySerializer;
    }

    public void setStateKeySerializer(TypeSerializer<?> stateKeySerializer) {
        this.stateKeySerializer = stateKeySerializer;
    }

    public String getTransformationUID() {
        return transformationUID;
    }

    void setTransformationUID(String transformationId) {
        this.transformationUID = transformationId;
    }

    public String getUserHash() {
        return userHash;
    }

    public void setUserHash(String userHash) {
        this.userHash = userHash;
    }

    public void addInputRequirement(
            int inputIndex, StreamConfig.InputRequirement inputRequirement) {
        inputRequirements.put(inputIndex, inputRequirement);
    }

    public Map<Integer, StreamConfig.InputRequirement> getInputRequirements() {
        return inputRequirements;
    }

    // 目前的flink版本中
    // SourceOperatorFactory 和 CollectSinkOperatorFactory 都有 OperatorCoordinator.Provider 算子协调者提供者
    public Optional<OperatorCoordinator.Provider> getCoordinatorProvider(
            String operatorName, OperatorID operatorID) {
        if (operatorFactory instanceof CoordinatedOperatorFactory) {
            return Optional.of(
                    ((CoordinatedOperatorFactory) operatorFactory)
                            .getCoordinatorProvider(operatorName, operatorID));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StreamNode that = (StreamNode) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
