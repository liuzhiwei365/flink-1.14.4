/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.checkpoint;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.OperatorIDPair;
import org.apache.flink.runtime.checkpoint.channel.InputChannelInfo;
import org.apache.flink.runtime.checkpoint.channel.ResultSubpartitionInfo;
import org.apache.flink.runtime.executiongraph.Execution;
import org.apache.flink.runtime.executiongraph.ExecutionJobVertex;
import org.apache.flink.runtime.executiongraph.IntermediateResult;
import org.apache.flink.runtime.jobgraph.IntermediateDataSet;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.jobgraph.OperatorInstanceID;
import org.apache.flink.runtime.state.*;
import org.apache.flink.util.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * This class encapsulates the operation of assigning restored state when restoring from a
 * checkpoint.
 */
// 这个是位于Master节点的作业恢复时负责rescale的类,主要是根据新作业的并发重新分配状态.
// 针对operator state主要采用broadcast的方式使得每个task都能接触算子全部的状态;
// 针对key state,采用均分KG （key group）的方法来重新划分state的归属
@Internal
public class StateAssignmentOperation {

    private static final Logger LOG = LoggerFactory.getLogger(StateAssignmentOperation.class);

    private final Set<ExecutionJobVertex> tasks;
    private final Map<OperatorID, OperatorState> operatorStates;

    private final long restoreCheckpointId;
    private final boolean allowNonRestoredState;

    /** The state assignments for each ExecutionJobVertex that will be filled in multiple passes. */
    private final Map<ExecutionJobVertex, TaskStateAssignment> vertexAssignments;
    /**
     * Stores the assignment of a consumer. {@link IntermediateResult} only allows to traverse
     * producer.
     */
    private final Map<IntermediateDataSetID, TaskStateAssignment> consumerAssignment =
            new HashMap<>();

    public StateAssignmentOperation(
            long restoreCheckpointId,
            Set<ExecutionJobVertex> tasks,
            Map<OperatorID, OperatorState> operatorStates,
            boolean allowNonRestoredState) {

        this.restoreCheckpointId = restoreCheckpointId;
        this.tasks = Preconditions.checkNotNull(tasks);
        this.operatorStates = Preconditions.checkNotNull(operatorStates);
        this.allowNonRestoredState = allowNonRestoredState;
        vertexAssignments = new HashMap<>(tasks.size());
    }

    // 状态重新分配的算法
    // 分配完成后, 相关状态内容会封装进入每个 ExecutionJobVertex 的 Execution 中; 以方便后续启动 subTask
    public void assignStates() {

        // step1  复制一份 operatorStates , 本方法的主题和目的，就是要对operatorStates 做重新划分
        //        operatorStates 来源于 CheckpointCoordinator.extractOperatorStates 方法
        Map<OperatorID, OperatorState> localOperators = new HashMap<>(operatorStates);

        checkStateMappingCompleteness(allowNonRestoredState, operatorStates, tasks);

        // step2  遍历,初始化各成员变量 和 数据结构 ; 为真正的算法做准备   ----->>>>
        for (ExecutionJobVertex executionJobVertex : tasks) {

            // 一个ExecutionJobVertex 包含多个算子, 它们以后会运行在同一个 sub task中
            List<OperatorIDPair> operatorIDPairs = executionJobVertex.getOperatorIDs();
            // operatorStates 用来存储 本ExecutionJobVertex 下 所有的算子状态
            Map<OperatorID, OperatorState> operatorStates = new HashMap<>(operatorIDPairs.size());
            //  简单填充 并行度信息 到刚刚创建的 map集合中
            for (OperatorIDPair operatorIDPair : operatorIDPairs) {
                OperatorID operatorID =
                        operatorIDPair
                                .getUserDefinedOperatorID()
                                .filter(localOperators::containsKey)
                                .orElse(operatorIDPair.getGeneratedOperatorID());

                OperatorState operatorState = localOperators.remove(operatorID);
                if (operatorState == null) {
                    // 如果某个 operatorID 不存在自己的OperatorState , 则构造一个空的
                    operatorState = new OperatorState(
                                    operatorID,
                                    executionJobVertex.getParallelism(),
                                    executionJobVertex.getMaxParallelism());
                }
                operatorStates.put(operatorIDPair.getGeneratedOperatorID(), operatorState);
            }

            //stateAssignment 对象中 operatorStates 就是即将被重分配的 老的状态 ***
            final TaskStateAssignment stateAssignment =
                    new TaskStateAssignment(
                            executionJobVertex,
                            operatorStates,
                            consumerAssignment,
                            vertexAssignments);
            vertexAssignments.put(executionJobVertex, stateAssignment);
            for (final IntermediateResult producedDataSet : executionJobVertex.getInputs()) {
                consumerAssignment.put(producedDataSet.getId(), stateAssignment);
            }
        } //<-----------------------  初始化 结束


        // step3  以ExecutionJobVertex 为单位, 将状态重新分布  (状态重新布局)
        for (TaskStateAssignment stateAssignment : vertexAssignments.values()) {
            if (stateAssignment.hasNonFinishedState) {
                // 一个ExecutionJobVertex 对应 一个TaskStateAssignment
                //  核心   ****
                assignAttemptState(stateAssignment);
            }
        }

        // step4  以ExecutionJobVertex 为单位
        // 将算法重新布局的 状态分布, 封装进入每个 ExecutionJobVertex 的 Execution 中; 以方便后续启动 subTask
        for (TaskStateAssignment stateAssignment : vertexAssignments.values()) {
            if (stateAssignment.hasNonFinishedState || stateAssignment.isFullyFinished) {
                assignTaskStateToExecutionJobVertices(stateAssignment);
            }
        }
    }

    // job启动时,尝试着重新分布状态, 重新分布的结果在  taskStateAssignment 中
    private void assignAttemptState(TaskStateAssignment taskStateAssignment) {

        // 校验
        checkParallelismPreconditions(taskStateAssignment);

        /**
         * 键组的解释:
         *
         *   hash(key) % maxParalleism 一定是一个 [ 0 , maxParalleism ) 范围内的数 (key为数据的key)
         *
         *   比如最大并行度 maxParalleism = 10 那么总共有10个键组: kg-0,kg-1,kg-2 ... kg-9 (键组个数只与最大并行度一致)
         *
         *   那么KeyGroupRange 就 相当于 把上面的10个键组按 range的方式分区
         *
         *   比如: KeyGroupRange1 = [kg-0,kg-1,kg-2,kg-3] KeyGroupRange2 = [kg-4,kg-5,kg-6,kg-7]  KeyGroupRange3 = [kg-8,kg-9]
         *
         *   简书的链接             https://www.jianshu.com/p/595ba0a2760b?hmsr=toutiao.io
         */

        // 计算在新的并行度下,  每个sub task 的 键组range划分
        // 该List 长度 等于 当前新的并行度 , 一个并行度 对应一个 KeyGroupRange
        List<KeyGroupRange> keyGroupPartitions =
                createKeyGroupPartitions(
                        taskStateAssignment.executionJobVertex.getMaxParallelism(),
                        taskStateAssignment.newParallelism);


        // 重新分布 管理算子状态  （RoundRobin 重分布策略）
        reDistributePartitionableStates(
                taskStateAssignment.oldState,
                taskStateAssignment.newParallelism,
                OperatorSubtaskState::getManagedOperatorState,
                RoundRobinOperatorStateRepartitioner.INSTANCE,
                taskStateAssignment.subManagedOperatorState);

        // 重新分布 原始算子状态  （RoundRobin 重分布策略）
        reDistributePartitionableStates(
                taskStateAssignment.oldState,
                taskStateAssignment.newParallelism,
                OperatorSubtaskState::getRawOperatorState,
                RoundRobinOperatorStateRepartitioner.INSTANCE,
                taskStateAssignment.subRawOperatorState);

        /*
           1 reDistributeResultSubpartitionStates 与 reDistributeInputChannelStates 的核心逻辑十分相似
           2 除了最终使用的  SubtaskStateMapper 的时候, 使用的策略不同
           3 重分布 ResultSubpartition 一定使用 SubtaskStateMapper.ARBITRARY 策略
           4 重分布 InputChannel 一定使用 FIRST FULL RANGE ROUND_ROBIN 四种的其中一种
        */
        // 重新分布InputChannel 状态 （FIRST FULL RANGE ROUND_ROBIN 四种的其中一种, 具体取决于上游的分区策略）
        // InputChannel 的重分布策略针对不同的 分区器 有不同的实现, 对应关系如下：
        //     BinaryHashPartitioner                                    FULL
        //     GlobalPartitioner                                        FIRST
        //     KeyGroupStreamPartitioner                                RANGE
        //     ShufflePartitioner                                       ROUND_ROBIN
        //     RebalancePartitioner                                     ROUND_ROBIN
        reDistributeInputChannelStates(taskStateAssignment);

        // 重新分布ResultSubpartition 状态 （SubtaskStateMapper.ARBITRARY 策略）
        reDistributeResultSubpartitionStates(taskStateAssignment);

        // 重新分布 管理Keyed状态  和 原始keyed状态 （key group range 重分布策略）
        reDistributeKeyedStates(keyGroupPartitions, taskStateAssignment);
    }

    private void assignTaskStateToExecutionJobVertices(TaskStateAssignment assignment) {
        ExecutionJobVertex executionJobVertex = assignment.executionJobVertex;

        List<OperatorIDPair> operatorIDs = executionJobVertex.getOperatorIDs();
        final int newParallelism = executionJobVertex.getParallelism();


        // 依次 将 TaskStateAssignment 中的状态布局 封装到每个 Execution对象中; 以方便后续启动 每个subTask
        for (int subTaskIndex = 0; subTaskIndex < newParallelism; subTaskIndex++) {
            Execution currentExecutionAttempt =
                    executionJobVertex.getTaskVertices()[subTaskIndex].getCurrentExecutionAttempt();

            // 把完成的状态 分配给 task
            if (assignment.isFullyFinished) {
                // 特殊情况
                assignFinishedStateToTask(currentExecutionAttempt);
            } else {
                // 一般情况
                assignNonFinishedStateToTask(
                        assignment, operatorIDs, subTaskIndex, currentExecutionAttempt);
            }
        }
    }

    private void assignFinishedStateToTask(Execution currentExecutionAttempt) {
        // FINISHED_ON_RESTORE 是空实现,里面集合成员 啥元素也没有
        JobManagerTaskRestore taskRestore =
                new JobManagerTaskRestore(
                        restoreCheckpointId, TaskStateSnapshot.FINISHED_ON_RESTORE);
        currentExecutionAttempt.setInitialState(taskRestore);
    }

    private void assignNonFinishedStateToTask(
            TaskStateAssignment assignment,
            List<OperatorIDPair> operatorIDs,
            int subTaskIndex,
            Execution currentExecutionAttempt) {
        TaskStateSnapshot taskState = new TaskStateSnapshot(operatorIDs.size(), false);

        // 遍历算子链中所有的算子
        for (OperatorIDPair operatorID : operatorIDs) {
            // OperatorInstanceID 内部包含 subtaskId(新的状态结构的句柄 的 subtaskId) 和 operatorId
            OperatorInstanceID instanceID =
                    OperatorInstanceID.of(subTaskIndex, operatorID.getGeneratedOperatorID());

            // OperatorSubtaskState 封装了 指定算子下 指定 subTask下 的 全部状态句柄集合
            OperatorSubtaskState operatorSubtaskState = assignment.getSubtaskState(instanceID);

            taskState.putSubtaskStateByOperatorID(
                    operatorID.getGeneratedOperatorID(), operatorSubtaskState);
        }

        JobManagerTaskRestore taskRestore =
                new JobManagerTaskRestore(restoreCheckpointId, taskState);

        // taskRestore 代表了  指定算子链下（也就是多个算子）下指定 subTask下 的 全部状态句柄集合
        // 我们 将其 赋值给 Execution 的 taskRestore 成员变量, 以方便 sub task 的执行和启动
        currentExecutionAttempt.setInitialState(taskRestore);
    }

    public void checkParallelismPreconditions(TaskStateAssignment taskStateAssignment) {
        for (OperatorState operatorState : taskStateAssignment.oldState.values()) {
            checkParallelismPreconditions(operatorState, taskStateAssignment.executionJobVertex);
        }
    }

    private void reDistributeKeyedStates(
            List<KeyGroupRange> keyGroupPartitions, TaskStateAssignment stateAssignment) {
        stateAssignment.oldState.forEach(
                // 针对单个算子
                (operatorID, operatorState) -> {

                    // 遍历新的并行度, 让老的状态资源去满足

                    for (int subTaskIndex = 0; subTaskIndex < stateAssignment.newParallelism; subTaskIndex++) {
                        OperatorInstanceID instanceID =
                                OperatorInstanceID.of(subTaskIndex, operatorID);

                        // 1     前面的List 存储的是subManagedKeyedState的结果
                        //       后面的List 存储的是subRawKeyedState的结果

                        // 2    计算指定 新的subTask 在新的规划下, 所应该分配的所有 管理状态和 原始状态的 状态句柄集合
                        Tuple2<List<KeyedStateHandle>, List<KeyedStateHandle>> subKeyedStates =
                                reAssignSubKeyedStates(
                                        operatorState,
                                        keyGroupPartitions,
                                        subTaskIndex,
                                        stateAssignment.newParallelism,
                                        operatorState.getParallelism());

                        stateAssignment.subManagedKeyedState.put(instanceID, subKeyedStates.f0);
                        stateAssignment.subRawKeyedState.put(instanceID, subKeyedStates.f1);
                    }

                });
    }

    // TODO rewrite based on operator id
    private Tuple2<List<KeyedStateHandle>, List<KeyedStateHandle>> reAssignSubKeyedStates(
            OperatorState operatorState, // 一个OperatorState对象
            List<KeyGroupRange> keyGroupPartitions, // 新的规划
            int subTaskIndex,            // 新并行度中的一个编号
            int newParallelism,
            int oldParallelism) {

        List<KeyedStateHandle> subManagedKeyedState;
        List<KeyedStateHandle> subRawKeyedState;

        if (newParallelism == oldParallelism) { // 当并行度没有改变时,直接返回,不需要重新分配状态
            if (operatorState.getState(subTaskIndex) != null) {
                subManagedKeyedState = operatorState.getState(subTaskIndex).getManagedKeyedState().asList();
                subRawKeyedState = operatorState.getState(subTaskIndex).getRawKeyedState().asList();
            } else {
                subManagedKeyedState = emptyList();
                subRawKeyedState = emptyList();
            }
        } else {
            // 针对管理keyed状态
            subManagedKeyedState =
                    getManagedKeyedStateHandles(operatorState, keyGroupPartitions.get(subTaskIndex));
            // 针对原始keyed状态  （逻辑和 管理keyed状态 一致）
            subRawKeyedState =
                    getRawKeyedStateHandles(operatorState, keyGroupPartitions.get(subTaskIndex));
        }

        if (subManagedKeyedState.isEmpty() && subRawKeyedState.isEmpty()) {
            return new Tuple2<>(emptyList(), emptyList());
        } else {
            return new Tuple2<>(subManagedKeyedState, subRawKeyedState);
        }
    }

    //  重新分布 管理算子状态
    //  和 重新分布 原始算子状态
    //  都会调用本方法
    public static <T extends StateObject> void reDistributePartitionableStates(
            Map<OperatorID, OperatorState> oldOperatorStates,
            int newParallelism,
            Function<OperatorSubtaskState, StateObjectCollection<T>> extractHandle,
            OperatorStateRepartitioner<T> stateRepartitioner,
            Map<OperatorInstanceID, List<T>> result) {

        // 1  外层List 的 容量 与 OperatorState对象 的并行度有关 （也就是老并行度）
        // 2  外层List 的 每个元素 是 OperatorSubtaskState对象的 某类状态的  状态句柄列表
        // 3  oldStates 反映的是在原来的并行度下的 某种状态的 状态分布

        // 4  T 的类型是 OperatorStateHandle  算子状态句柄
        Map<OperatorID, List<List<T>>> oldStates =
                splitManagedAndRawOperatorStates(
                        oldOperatorStates,
                        extractHandle); // 例如:  OperatorSubtaskState::getManagedOperatorState

        oldOperatorStates.forEach(
                (operatorID, oldOperatorState) ->
                        result.putAll(
                                //  利用状态重分区器, 这里的分区器一定是 RoundRobinOperatorStateRepartitioner.INSTANCE
                                applyRepartitioner(
                                        operatorID,
                                        stateRepartitioner,                 // 重分区的 分区器
                                        oldStates.get(operatorID),          // List<List<OperatorStateHandle>>
                                        oldOperatorState.getParallelism(),  // 旧并行度
                                        newParallelism)));                  // 新并行度
    }

    // 1 reDistributeResultSubpartitionStates 与 reDistributeInputChannelStates 的核心逻辑十分相似
    // 2 除了最终使用的  SubtaskStateMapper 的时候, 使用的策略不同
    // 3 重分布 ResultSubpartition 一定使用 SubtaskStateMapper.ARBITRARY 策略
    // 4 重分布 InputChannel 一定使用 FIRST FULL RANGE ROUND_ROBIN 四种的其中一种
    public <I, T extends AbstractChannelStateHandle<I>> void reDistributeResultSubpartitionStates(
            TaskStateAssignment assignment) {
        if (!assignment.hasOutputState) {
            return;
        }

        checkForUnsupportedToplogyChanges(
                assignment.oldState,
                OperatorSubtaskState::getResultSubpartitionState,
                assignment.outputOperatorID);

        final OperatorState outputState = assignment.oldState.get(assignment.outputOperatorID);
        final List<List<ResultSubpartitionStateHandle>> outputOperatorState =
                splitBySubtasks(outputState, OperatorSubtaskState::getResultSubpartitionState);

        final ExecutionJobVertex executionJobVertex = assignment.executionJobVertex;
        final List<IntermediateDataSet> outputs =
                executionJobVertex.getJobVertex().getProducedDataSets();

        if (outputState.getParallelism() == executionJobVertex.getParallelism()) {
            assignment.resultSubpartitionStates.putAll(
                    toInstanceMap(assignment.outputOperatorID, outputOperatorState));
            return;
        }
        // Parallelism of this vertex changed, distribute ResultSubpartitionStateHandle
        // according to output mapping.
        for (int partitionIndex = 0; partitionIndex < outputs.size(); partitionIndex++) {
            final List<List<ResultSubpartitionStateHandle>> partitionState =
                    outputs.size() == 1
                            ? outputOperatorState
                            : getPartitionState(
                                    outputOperatorState,
                                    ResultSubpartitionInfo::getPartitionIdx,
                                    partitionIndex);
            final MappingBasedRepartitioner<ResultSubpartitionStateHandle> repartitioner =
                    new MappingBasedRepartitioner<>(
                            assignment.getOutputMapping(partitionIndex).getRescaleMappings());
            final Map<OperatorInstanceID, List<ResultSubpartitionStateHandle>> repartitioned =
                    applyRepartitioner(
                            assignment.outputOperatorID,
                            repartitioner,
                            partitionState,
                            outputOperatorState.size(),
                            executionJobVertex.getParallelism());
            addToSubtasks(assignment.resultSubpartitionStates, repartitioned);
        }
    }

    // 重分布 inputChannel
    // 参见 SubtaskStateMapper.getNewToOldSubtasksMapping 方法 是重分布inputChannel 的算法核心逻辑
    public void reDistributeInputChannelStates(TaskStateAssignment stateAssignment) {
        // short cut
        if (!stateAssignment.hasInputState) {
            return;
        }

        checkForUnsupportedToplogyChanges(
                stateAssignment.oldState,
                OperatorSubtaskState::getInputChannelState,
                stateAssignment.inputOperatorID);

        final ExecutionJobVertex executionJobVertex = stateAssignment.executionJobVertex;
        final List<IntermediateResult> inputs = executionJobVertex.getInputs();// 与InputGate 数量对应

        // check for rescaling: no rescaling = simple reassignment

        //  1 inputOperatorID 其实是算子链中的第一个 第一个OperatorID
        //  2 InputChannel的状态 和其他状态不同, 它只在 头算子的状态中存在
        //  3 OperatorState 内部各类状态的句柄
        final OperatorState inputState =
                stateAssignment.oldState.get(stateAssignment.inputOperatorID);

        //  从 inputState 中 抽取 InputChannel 类型的句柄 ; 外层长度为老并行度
        final List<List<InputChannelStateHandle>> inputOperatorState =
                splitBySubtasks(inputState, OperatorSubtaskState::getInputChannelState);

        // short cut for  " 如果老的并行度 等于新的并行度 "
        if (inputState.getParallelism() == executionJobVertex.getParallelism()) {
            // 交给成员变量inputChannelStates 维护
            stateAssignment.inputChannelStates.putAll(
                    toInstanceMap(stateAssignment.inputOperatorID, inputOperatorState));
            return;
        }

        // 遍历 InputGate 个数, 每个InputGate 下有多个 InputChannel
        for (int gateIndex = 0; gateIndex < inputs.size(); gateIndex++) {

            // 针对指定的 input gate, 根据一定的策略提前规划:  每个新的并行度 编号 对应的 老的并行度集合
            //  **************重新分布 InputChannel 的 最核心逻辑****************
            final RescaleMappings mapping =
                    stateAssignment.getInputMapping(gateIndex).getRescaleMappings();

            final List<List<InputChannelStateHandle>> gateState =
                    inputs.size() == 1
                            ? inputOperatorState
                            // 如果size 不为 1 , 则需要抽取只属于本 input gate 的 状态
                            : getPartitionState(
                                    inputOperatorState, InputChannelInfo::getGateIdx, gateIndex);

            final MappingBasedRepartitioner<InputChannelStateHandle> repartitioner =
                    new MappingBasedRepartitioner(mapping);

            // OperatorInstanceID 中包含 operatorId 和 subtaskId
            // 最终 指定算子下指定新的subTask下   所应该分配的所有状态句柄
            final Map<OperatorInstanceID, List<InputChannelStateHandle>> repartitioned =
                    applyRepartitioner(
                            stateAssignment.inputOperatorID,
                            repartitioner,
                            gateState,
                            inputOperatorState.size(),// 老并行度
                            stateAssignment.newParallelism);//新并行度

            // 交给成员变量inputChannelStates 维护
            addToSubtasks(stateAssignment.inputChannelStates, repartitioned);
        }
    }

    private static <K, V> void addToSubtasks(Map<K, List<V>> target, Map<K, List<V>> toAdd) {
        toAdd.forEach(
                (key, values) ->
                        target.computeIfAbsent(key, (unused) -> new ArrayList<>(values.size()))
                                .addAll(values));
    }

    private <T extends AbstractChannelStateHandle<?>> void checkForUnsupportedToplogyChanges(
            Map<OperatorID, OperatorState> oldOperatorStates,
            Function<OperatorSubtaskState, StateObjectCollection<T>> extractHandle,
            OperatorID expectedOperatorID) {
        final List<OperatorID> unexpectedState =
                oldOperatorStates.entrySet().stream()
                        .filter(idAndState -> !idAndState.getKey().equals(expectedOperatorID))
                        .filter(idAndState -> hasChannelState(idAndState.getValue(), extractHandle))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
        if (!unexpectedState.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot recover from unaligned checkpoint when topology changes, such that "
                            + "data exchanges with persisted data are now chained.\n"
                            + "The following operators contain channel state: "
                            + unexpectedState);
        }
    }

    private <T extends AbstractChannelStateHandle<?>> boolean hasChannelState(
            OperatorState operatorState,
            Function<OperatorSubtaskState, StateObjectCollection<T>> extractHandle) {
        return operatorState.getSubtaskStates().values().stream()
                .anyMatch(subState -> !isEmpty(extractHandle.apply(subState)));
    }

    private <T extends AbstractChannelStateHandle<?>> boolean isEmpty(StateObjectCollection<T> s) {
        return s.stream().allMatch(state -> state.getOffsets().isEmpty());
    }

    private static <T extends AbstractChannelStateHandle<I>, I> List<List<T>> getPartitionState(
            List<List<T>> subtaskStates, Function<I, Integer> partitionExtractor, int partitionId) {
        return subtaskStates.stream()
                .map(
                        subtaskState ->
                                subtaskState.stream()
                                        .filter(
                                                state ->
                                                        partitionExtractor.apply(state.getInfo())
                                                                == partitionId)
                                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }


    private static <T extends StateObject>
            Map<OperatorID, List<List<T>>> splitManagedAndRawOperatorStates(
                    Map<OperatorID, OperatorState> operatorStates,
                    Function<OperatorSubtaskState, StateObjectCollection<T>> extractHandle) {

        // 通过 operatorStates map 对象, 得到一个新的 map
        // key 还是原来的 key, value 是  原来的value通过 splitBySubtasks 方法计算得到
        return operatorStates.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                operatorIdAndState ->
                                        splitBySubtasks(
                                                operatorIdAndState.getValue(), extractHandle)));
    }



    // 用抽取函数 extractHandle 抽取 operatorState 中某类状态  （返回的List长度为 算子的并行度）
    private static <T extends StateObject> List<List<T>> splitBySubtasks(
            OperatorState operatorState,
            Function<OperatorSubtaskState, StateObjectCollection<T>> extractHandle) {


        //返回每个 sub task 下 的 某类状态对象的 集合, 比如管理状态
        List<List<T>> statePerSubtask = new ArrayList<>(operatorState.getParallelism());

        // 1  用extractHandle 抽取某类状态对象,并转化成list, 最后填充到 statePerSubtask 中
        // 2  所谓抽取就是 在这里就是针对 OperatorSubtaskState 对象, 提取其 某个成员变量
        // 3  具体的提取方法 由 传入的 函数对象 extractHandle 决定
        for (int subTaskIndex = 0; subTaskIndex < operatorState.getParallelism(); subTaskIndex++) {

            OperatorSubtaskState subtaskState = operatorState.getState(subTaskIndex);
            statePerSubtask.add(
                    subtaskState == null
                            ? emptyList()
                            // extractHandle 是 OperatorSubtaskState::getManagedOperatorState
                            //  或者 OperatorSubtaskState::getRawOperatorState

                            // StateObjectCollection<OperatorStateHandle>
                            // 最终 转换得到的list类型 是 List<OperatorStateHandle>
                            : extractHandle.apply(subtaskState).asList());
        }

        return statePerSubtask;
    }

    /**
     * Collect {@link KeyGroupsStateHandle managedKeyedStateHandles} which have intersection with
     * given {@link KeyGroupRange} from {@link TaskState operatorState}.
     *
     * @param operatorState all state handles of a operator
     * @param subtaskKeyGroupRange the KeyGroupRange of a subtask
     * @return all managedKeyedStateHandles which have intersection with given KeyGroupRange
     */
    public static List<KeyedStateHandle> getManagedKeyedStateHandles(
            OperatorState operatorState,
            KeyGroupRange subtaskKeyGroupRange) { // 新的Range规划中的一个KeyGroupRange

        final int parallelism = operatorState.getParallelism();

        // 保存新的规划下 , 本 subtaskKeyGroupRange 所应该分配的 所有状态句柄 （由老的状态句柄分割和重组得到）
        List<KeyedStateHandle> subtaskKeyedStateHandles = null;

        for (int i = 0; i < parallelism; i++) { // 老的并行度
            if (operatorState.getState(i) != null) {

                Collection<KeyedStateHandle> keyedStateHandles =
                        operatorState.getState(i).getManagedKeyedState();

                if (subtaskKeyedStateHandles == null) {
                    subtaskKeyedStateHandles =
                            new ArrayList<>(parallelism * keyedStateHandles.size());
                }

                // 拿着新的Range规划中的一个KeyGroupRange, 与老的所有分区的keyedStateHandles 依次去做交集
                // 最终的结果全果放入subtaskKeyedStateHandles 中
                extractIntersectingState(
                        keyedStateHandles, subtaskKeyGroupRange, subtaskKeyedStateHandles);
            }
        }

        return subtaskKeyedStateHandles != null ? subtaskKeyedStateHandles : emptyList();
    }

    /**
     * Collect {@link KeyGroupsStateHandle rawKeyedStateHandles} which have intersection with given
     * {@link KeyGroupRange} from {@link TaskState operatorState}.
     *
     * @param operatorState all state handles of a operator
     * @param subtaskKeyGroupRange the KeyGroupRange of a subtask
     * @return all rawKeyedStateHandles which have intersection with given KeyGroupRange
     */
    public static List<KeyedStateHandle> getRawKeyedStateHandles(
            OperatorState operatorState, KeyGroupRange subtaskKeyGroupRange) {

        final int parallelism = operatorState.getParallelism();

        List<KeyedStateHandle> extractedKeyedStateHandles = null;

        for (int i = 0; i < parallelism; i++) {
            if (operatorState.getState(i) != null) {

                Collection<KeyedStateHandle> rawKeyedState =
                        operatorState.getState(i).getRawKeyedState();

                if (extractedKeyedStateHandles == null) {
                    extractedKeyedStateHandles =
                            new ArrayList<>(parallelism * rawKeyedState.size());
                }

                extractIntersectingState(
                        rawKeyedState, subtaskKeyGroupRange, extractedKeyedStateHandles);
            }
        }

        return extractedKeyedStateHandles != null ? extractedKeyedStateHandles : emptyList();
    }

    /**
     * Extracts certain key group ranges from the given state handles and adds them to the
     * collector.
     */
    @VisibleForTesting
    public static void extractIntersectingState(
            // 一个subTask 下会存储多个状态句柄(因为可能由多个状态名称)
            Collection<? extends KeyedStateHandle> originalSubtaskStateHandles,
            KeyGroupRange rangeToExtract,
            List<KeyedStateHandle> extractedStateCollector) {

        // 这里的迭代的是  老的 指定分区中 的所有的  KeyedState句柄
        for (KeyedStateHandle keyedStateHandle : originalSubtaskStateHandles) {

            if (keyedStateHandle != null) {

                // 拿着 新的 rangeToExtract标准, 让老的状态句柄集合来满足,如果满足,则将满足的部分剥离出来
                // 然后,重塑成新的  KeyedStateHandle 对象,也就是  intersectedKeyedStateHandle
                KeyedStateHandle intersectedKeyedStateHandle =
                        keyedStateHandle.getIntersection(rangeToExtract);

                // 收集 intersectedKeyedStateHandle
                if (intersectedKeyedStateHandle != null) {
                    extractedStateCollector.add(intersectedKeyedStateHandle);
                }
            }
        }
    }



    // 键组分区 是 被分配给相同task的 键组的集合
    public static List<KeyGroupRange> createKeyGroupPartitions(
            int numberKeyGroups, int parallelism) {
        Preconditions.checkArgument(numberKeyGroups >= parallelism);

        List<KeyGroupRange> result = new ArrayList<>(parallelism);

        for (int i = 0; i < parallelism; ++i) {
            result.add(
                     // 规划指定 subtask （i） 的 键组 编号的 range
                    KeyGroupRangeAssignment.computeKeyGroupRangeForOperatorIndex(
                            numberKeyGroups, parallelism, i));
        }
        return result;
    }

    /**
     * Verifies conditions in regards to parallelism and maxParallelism that must be met when
     * restoring state.
     *
     * @param operatorState state to restore
     * @param executionJobVertex task for which the state should be restored
     */
    private static void checkParallelismPreconditions(
            OperatorState operatorState, ExecutionJobVertex executionJobVertex) {
        // ----------------------------------------max parallelism
        // preconditions-------------------------------------

        if (operatorState.getMaxParallelism() < executionJobVertex.getParallelism()) {
            throw new IllegalStateException(
                    "The state for task "
                            + executionJobVertex.getJobVertexId()
                            + " can not be restored. The maximum parallelism ("
                            + operatorState.getMaxParallelism()
                            + ") of the restored state is lower than the configured parallelism ("
                            + executionJobVertex.getParallelism()
                            + "). Please reduce the parallelism of the task to be lower or equal to the maximum parallelism.");
        }

        // check that the number of key groups have not changed or if we need to override it to
        // satisfy the restored state
        if (operatorState.getMaxParallelism() != executionJobVertex.getMaxParallelism()) {

            if (executionJobVertex.canRescaleMaxParallelism(operatorState.getMaxParallelism())) {
                LOG.debug(
                        "Rescaling maximum parallelism for JobVertex {} from {} to {}",
                        executionJobVertex.getJobVertexId(),
                        executionJobVertex.getMaxParallelism(),
                        operatorState.getMaxParallelism());

                executionJobVertex.setMaxParallelism(operatorState.getMaxParallelism());
            } else {
                // if the max parallelism cannot be rescaled, we complain on mismatch
                throw new IllegalStateException(
                        "The maximum parallelism ("
                                + operatorState.getMaxParallelism()
                                + ") with which the latest "
                                + "checkpoint of the execution job vertex "
                                + executionJobVertex
                                + " has been taken and the current maximum parallelism ("
                                + executionJobVertex.getMaxParallelism()
                                + ") changed. This "
                                + "is currently not supported.");
            }
        }
    }

    /**
     * Verifies that all operator states can be mapped to an execution job vertex.
     *
     * @param allowNonRestoredState if false an exception will be thrown if a state could not be
     *     mapped
     * @param operatorStates operator states to map
     * @param tasks task to map to
     */
    private static void checkStateMappingCompleteness(
            boolean allowNonRestoredState,
            Map<OperatorID, OperatorState> operatorStates,
            Set<ExecutionJobVertex> tasks) {

        Set<OperatorID> allOperatorIDs = new HashSet<>();
        for (ExecutionJobVertex executionJobVertex : tasks) {
            for (OperatorIDPair operatorIDPair : executionJobVertex.getOperatorIDs()) {
                allOperatorIDs.add(operatorIDPair.getGeneratedOperatorID());
                operatorIDPair.getUserDefinedOperatorID().ifPresent(allOperatorIDs::add);
            }
        }
        for (Map.Entry<OperatorID, OperatorState> operatorGroupStateEntry :
                operatorStates.entrySet()) {
            OperatorState operatorState = operatorGroupStateEntry.getValue();
            // ----------------------------------------find operator for
            // state---------------------------------------------

            if (!allOperatorIDs.contains(operatorGroupStateEntry.getKey())) {
                if (allowNonRestoredState) {
                    LOG.info(
                            "Skipped checkpoint state for operator {}.",
                            operatorState.getOperatorID());
                } else {
                    throw new IllegalStateException(
                            "There is no operator for the state " + operatorState.getOperatorID());
                }
            }
        }
    }

    // 利用 OperatorStateRepartitioner 进行状态重分区
    public static <T> Map<OperatorInstanceID, List<T>> applyRepartitioner(
            OperatorID operatorID,
            OperatorStateRepartitioner<T> opStateRepartitioner,
            List<List<T>> chainOpParallelStates,
            int oldParallelism,
            int newParallelism) {

        // 1 applyRepartitioner 方法的主要逻辑是
        //     把 老的状态结构 chainOpParallelStates  装换成 新的状态结构 states
        // 2 chainOpParallelStates  外层List容量与老的并行度一致
        //    states                外层List容量与新的并行度一致
        // 3  T 为某种状态句柄, List<List<T>>  表示 每个新的并行度编号 所要分配的全部状态句柄
        List<List<T>> states =
                applyRepartitioner(
                        opStateRepartitioner,   // 封装了状态重分布策略
                        chainOpParallelStates,
                        oldParallelism,
                        newParallelism);

        return toInstanceMap(operatorID, states);
    }


    private static <T> Map<OperatorInstanceID, List<T>> toInstanceMap(
            OperatorID operatorID, List<List<T>> states) {
        Map<OperatorInstanceID, List<T>> result = new HashMap<>(states.size());

        for (int subtaskIndex = 0; subtaskIndex < states.size(); subtaskIndex++) {
            checkNotNull(states.get(subtaskIndex) != null, "states.get(subtaskIndex) is null");
            // OperatorInstanceID 与 operatorID 相比, 内部多了一个 subtaskIndex
            result.put(OperatorInstanceID.of(subtaskIndex, operatorID), states.get(subtaskIndex));
        }

        return result;
    }

    /**
     * Repartitions the given operator state using the given {@link OperatorStateRepartitioner} with
     * respect to the new parallelism.
     *
     * @param opStateRepartitioner partitioner to use
     * @param chainOpParallelStates state to repartition
     * @param oldParallelism parallelism with which the state is currently partitioned
     * @param newParallelism parallelism with which the state should be partitioned
     * @return repartitioned state
     */
    // TODO rewrite based on operator id

    // 状态重分区 , 核心方法封装在OperatorStateRepartitioner 中
    public static <T> List<List<T>> applyRepartitioner(
            OperatorStateRepartitioner<T> opStateRepartitioner,
            List<List<T>> chainOpParallelStates,       // List<List<OperatorStateHandle>> , 且外层List容量与老的并行度一致
            int oldParallelism,
            int newParallelism) {

        if (chainOpParallelStates == null) {
            return emptyList();
        }

        // 1  OperatorStateRepartitioner 有两个实现：
        //    MappingBasedRepartitioner    和    RoundRobinOperatorStateRepartitioner

        //    inputChannel 和 ResultSubpartition 状态的重分区用的是 MappingBasedRepartitioner
        //    算子状态重分区用的是  RoundRobinOperatorStateRepartitioner


        //  拿到每个新 并行度编号 应该拿到的 所有状态句柄
        return opStateRepartitioner.repartitionState(
                chainOpParallelStates, oldParallelism, newParallelism);
    }
}
