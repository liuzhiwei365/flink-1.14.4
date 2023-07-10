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

package org.apache.flink.runtime.checkpoint;

import org.apache.flink.runtime.OperatorIDPair;
import org.apache.flink.runtime.executiongraph.Execution;
import org.apache.flink.runtime.executiongraph.ExecutionJobVertex;
import org.apache.flink.runtime.executiongraph.ExecutionVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.state.OperatorStateHandle;
import org.apache.flink.util.FlinkRuntimeException;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/** The default implementation of he {@link CheckpointPlan}. */
public class DefaultCheckpointPlan implements CheckpointPlan {

    // 存储了触发Checkpoint操作的 节点
    // 1 一般来说, 是Source 的 节点
    // 2 但是万一 Source 执行完了,finish了, 那么这时候Source的下游节点有可能成了实际意义上的Source,
    //   那么这时候这些下游的节点 就会被存储到这里了
    private final List<Execution> tasksToTrigger;

    // 存储了 物理执行图中 所有的 节点,  都需要向CheckpointCoordinator 汇报ack (Acknowledge)信息
    private final List<Execution> tasksToWaitFor;

    // 其实 tasksToCommitTo 中存储的 ExecutionVertex 与 tasksToWaitFor中存储的 Execution 是一一对应的
    // ExecutionVertex 是执行图中的顶点,  而Execution 是针对 执行顶点的 一次执行尝试
    private final List<ExecutionVertex> tasksToCommitTo;

    private final List<Execution> finishedTasks;

    private final boolean mayHaveFinishedTasks;

    // 维护 所有子任务 都finish 的 JobVertex
    private final Map<JobVertexID, ExecutionJobVertex> fullyFinishedOrFinishedOnRestoreVertices;

    // 每个 JobVertex 下, 已经 finish 的 subtask 数量
    private final IdentityHashMap<ExecutionJobVertex, Integer> vertexOperatorsFinishedTasksCount;

    DefaultCheckpointPlan(
            List<Execution> tasksToTrigger,
            List<Execution> tasksToWaitFor,
            List<ExecutionVertex> tasksToCommitTo,
            List<Execution> finishedTasks,
            List<ExecutionJobVertex> fullyFinishedJobVertex,
            boolean mayHaveFinishedTasks) {

        this.tasksToTrigger = checkNotNull(tasksToTrigger);
        this.tasksToWaitFor = checkNotNull(tasksToWaitFor);
        this.tasksToCommitTo = checkNotNull(tasksToCommitTo);
        this.finishedTasks = checkNotNull(finishedTasks);
        this.mayHaveFinishedTasks = mayHaveFinishedTasks;

        this.fullyFinishedOrFinishedOnRestoreVertices = new HashMap<>();
        fullyFinishedJobVertex.forEach(
                jobVertex ->
                        fullyFinishedOrFinishedOnRestoreVertices.put(
                                jobVertex.getJobVertexId(), jobVertex));

        this.vertexOperatorsFinishedTasksCount = new IdentityHashMap<>();
    }

    @Override
    public List<Execution> getTasksToTrigger() {
        return tasksToTrigger;
    }

    @Override
    public List<Execution> getTasksToWaitFor() {
        return tasksToWaitFor;
    }

    @Override
    public List<ExecutionVertex> getTasksToCommitTo() {
        return tasksToCommitTo;
    }

    @Override
    public List<Execution> getFinishedTasks() {
        return finishedTasks;
    }

    @Override
    public Collection<ExecutionJobVertex> getFullyFinishedJobVertex() {
        return fullyFinishedOrFinishedOnRestoreVertices.values();
    }

    @Override
    public boolean mayHaveFinishedTasks() {
        return mayHaveFinishedTasks;
    }

    @Override
    public void reportTaskFinishedOnRestore(ExecutionVertex task) {
        fullyFinishedOrFinishedOnRestoreVertices.putIfAbsent(
                task.getJobvertexId(), task.getJobVertex());
    }

    @Override
    public void reportTaskHasFinishedOperators(ExecutionVertex task) {
        vertexOperatorsFinishedTasksCount.compute(
                task.getJobVertex(), (k, v) -> v == null ? 1 : v + 1);
    }

    @Override
    public void fulfillFinishedTaskStatus(Map<OperatorID, OperatorState> operatorStates) {
        if (!mayHaveFinishedTasks) {
            return;
        }
        //存储部分完成的 JobVertex
        Map<JobVertexID, ExecutionJobVertex> partlyFinishedVertex = new HashMap<>();
        for (Execution task : finishedTasks) {
            JobVertexID jobVertexId = task.getVertex().getJobvertexId();
            if (!fullyFinishedOrFinishedOnRestoreVertices.containsKey(jobVertexId)) {
                partlyFinishedVertex.put(jobVertexId, task.getVertex().getJobVertex());
            }
        }

        checkNoPartlyFinishedVertexUsedUnionListState(partlyFinishedVertex, operatorStates);
        checkNoPartlyOperatorsFinishedVertexUsedUnionListState(
                partlyFinishedVertex, operatorStates);

        // 填充 已经完成的算子 的算子状态
        fulfillFullyFinishedOrFinishedOnRestoreOperatorStates(operatorStates);
        // 填充（占位） 部分完成的算子 的  算子子任务状态
        fulfillSubtaskStateForPartiallyFinishedOperators(operatorStates);
    }

    /**
     * If a job vertex using {@code UnionListState} has part of tasks FINISHED where others are
     * still in RUNNING state, the checkpoint would be aborted since it might cause incomplete
     * {@code UnionListState}.
     */
    private void checkNoPartlyFinishedVertexUsedUnionListState(
            Map<JobVertexID, ExecutionJobVertex> partlyFinishedVertex,
            Map<OperatorID, OperatorState> operatorStates) {
        for (ExecutionJobVertex vertex : partlyFinishedVertex.values()) {
            if (hasUsedUnionListState(vertex, operatorStates)) {
                throw new FlinkRuntimeException(
                        String.format(
                                "The vertex %s (id = %s) has used"
                                        + " UnionListState, but part of its tasks are FINISHED.",
                                vertex.getName(), vertex.getJobVertexId()));
            }
        }
    }

    /**
     * If a job vertex using {@code UnionListState} has all the tasks in RUNNING state, but part of
     * the tasks have reported that the operators are finished, the checkpoint would be aborted.
     * This is to force the fast tasks wait for the slow tasks so that their final checkpoints would
     * be the same one, otherwise if the fast tasks finished, the slow tasks would be blocked
     * forever since all the following checkpoints would be aborted.
     */
    private void checkNoPartlyOperatorsFinishedVertexUsedUnionListState(
            Map<JobVertexID, ExecutionJobVertex> partlyFinishedVertex,
            Map<OperatorID, OperatorState> operatorStates) {
        for (Map.Entry<ExecutionJobVertex, Integer> entry :
                vertexOperatorsFinishedTasksCount.entrySet()) {
            ExecutionJobVertex vertex = entry.getKey();

            // If the vertex is partly finished, then it must not used UnionListState
            // due to it passed the previous check.
            if (partlyFinishedVertex.containsKey(vertex.getJobVertexId())) {
                continue;
            }

            if (entry.getValue() != vertex.getParallelism()
                    && hasUsedUnionListState(vertex, operatorStates)) {
                throw new FlinkRuntimeException(
                        String.format(
                                "The vertex %s (id = %s) has used"
                                        + " UnionListState, but part of its tasks has called operators' finish method.",
                                vertex.getName(), vertex.getJobVertexId()));
            }
        }
    }

    private boolean hasUsedUnionListState(
            ExecutionJobVertex vertex, Map<OperatorID, OperatorState> operatorStates) {
        for (OperatorIDPair operatorIDPair : vertex.getOperatorIDs()) {
            OperatorState operatorState =
                    operatorStates.get(operatorIDPair.getGeneratedOperatorID());
            if (operatorState == null) {
                continue;
            }

            for (OperatorSubtaskState operatorSubtaskState : operatorState.getStates()) {
                boolean hasUnionListState =
                        Stream.concat(
                                        operatorSubtaskState.getManagedOperatorState().stream(),
                                        operatorSubtaskState.getRawOperatorState().stream())
                                .filter(Objects::nonNull)
                                .flatMap(
                                        operatorStateHandle ->
                                                operatorStateHandle.getStateNameToPartitionOffsets()
                                                        .values().stream())
                                .anyMatch(
                                        stateMetaInfo ->
                                                stateMetaInfo.getDistributionMode()
                                                        == OperatorStateHandle.Mode.UNION);

                if (hasUnionListState) {
                    return true;
                }
            }
        }

        return false;
    }

    private void fulfillFullyFinishedOrFinishedOnRestoreOperatorStates(
            Map<OperatorID, OperatorState> operatorStates) {
        // Completes the operator state for the fully finished operators
        for (ExecutionJobVertex jobVertex : fullyFinishedOrFinishedOnRestoreVertices.values()) {
            for (OperatorIDPair operatorID : jobVertex.getOperatorIDs()) {
                OperatorState operatorState =
                        operatorStates.get(operatorID.getGeneratedOperatorID());
                checkState(
                        operatorState == null || !operatorState.hasSubtaskStates(),
                        "There should be no states or only coordinator state reported for fully finished operators");

                operatorState =
                        new FullyFinishedOperatorState(
                                operatorID.getGeneratedOperatorID(),
                                jobVertex.getParallelism(),
                                jobVertex.getMaxParallelism());
                operatorStates.put(operatorID.getGeneratedOperatorID(), operatorState);
            }
        }
    }

    // 填充 部分完成的算子 的  算子子任务状态
    private void fulfillSubtaskStateForPartiallyFinishedOperators(
            Map<OperatorID, OperatorState> operatorStates) {

        // finishedTasks 是运行时已经完成的节点; 根据流计算的特性, 一定是前面的节点finish, 后面的才能finish
        for (Execution finishedTask : finishedTasks) {
            ExecutionJobVertex jobVertex = finishedTask.getVertex().getJobVertex();

            // JobVertex 中所有的算子
            for (OperatorIDPair operatorIDPair : jobVertex.getOperatorIDs()) {
                OperatorState operatorState =
                        operatorStates.get(operatorIDPair.getGeneratedOperatorID());

                // FullyFinishedOperatorState.isFullyFinished() 是true 和 OperatorState.isFullyFinished() 是false
                if (operatorState != null && operatorState.isFullyFinished()) {
                    // 如果是 FullyFinishedOperatorState对象, 直接跳过
                    continue;
                }
                //  如果operatorState为null , 则构造、 填充
                if (operatorState == null) {
                    operatorState =
                            new OperatorState(
                                    operatorIDPair.getGeneratedOperatorID(),
                                    jobVertex.getParallelism(),
                                    jobVertex.getMaxParallelism());
                    operatorStates.put(operatorIDPair.getGeneratedOperatorID(), operatorState);
                }

                //  FinishedOperatorSubtaskState.INSTANCE 并无实际的意义,只是占位
                //  会被放入 operatorSubtaskStates Map 成员中
                operatorState.putState(
                        finishedTask.getParallelSubtaskIndex(),
                        FinishedOperatorSubtaskState.INSTANCE);
            }
        }
    }
}
