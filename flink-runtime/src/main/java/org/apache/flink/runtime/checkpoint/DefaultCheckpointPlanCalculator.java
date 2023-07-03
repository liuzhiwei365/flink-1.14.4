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

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.executiongraph.Execution;
import org.apache.flink.runtime.executiongraph.ExecutionJobVertex;
import org.apache.flink.runtime.executiongraph.ExecutionVertex;
import org.apache.flink.runtime.executiongraph.InternalExecutionGraphAccessor;
import org.apache.flink.runtime.jobgraph.DistributionPattern;
import org.apache.flink.runtime.jobgraph.IntermediateResultPartitionID;
import org.apache.flink.runtime.jobgraph.JobEdge;
import org.apache.flink.runtime.jobgraph.JobVertexID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Default implementation for {@link CheckpointPlanCalculator}. If all tasks are running, it
 * directly marks all the sources as tasks to trigger, otherwise it would try to find the running
 * tasks without running processors as tasks to trigger.
 */
public class DefaultCheckpointPlanCalculator implements CheckpointPlanCalculator {

    private final JobID jobId;

    private final CheckpointPlanCalculatorContext context;

    private final List<ExecutionJobVertex> jobVerticesInTopologyOrder = new ArrayList<>();

    private final List<ExecutionVertex> allTasks = new ArrayList<>();

    private final List<ExecutionVertex> sourceTasks = new ArrayList<>();

    private final boolean allowCheckpointsAfterTasksFinished;

    public DefaultCheckpointPlanCalculator(
            JobID jobId,
            CheckpointPlanCalculatorContext context,
            Iterable<ExecutionJobVertex> jobVerticesInTopologyOrderIterable,
            boolean allowCheckpointsAfterTasksFinished) {

        this.jobId = checkNotNull(jobId);
        this.context = checkNotNull(context);
        this.allowCheckpointsAfterTasksFinished = allowCheckpointsAfterTasksFinished;

        checkNotNull(jobVerticesInTopologyOrderIterable);
        jobVerticesInTopologyOrderIterable.forEach(
                jobVertex -> {
                    jobVerticesInTopologyOrder.add(jobVertex);
                    allTasks.addAll(Arrays.asList(jobVertex.getTaskVertices()));

                    if (jobVertex.getJobVertex().isInputVertex()) {
                        sourceTasks.addAll(Arrays.asList(jobVertex.getTaskVertices()));
                    }
                });
    }

    @Override
    public CompletableFuture<CheckpointPlan> calculateCheckpointPlan() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (context.hasFinishedTasks() && !allowCheckpointsAfterTasksFinished) {
                            throw new CheckpointException(
                                    "Some tasks of the job have already finished and checkpointing with finished tasks is not enabled.",
                                    CheckpointFailureReason.NOT_ALL_REQUIRED_TASKS_RUNNING);
                        }

                        checkAllTasksInitiated();

                        // 其他都是各种检查逻辑 ,只有这里核心
                        CheckpointPlan result =
                                context.hasFinishedTasks()
                                        ? calculateAfterTasksFinished() // 如果是结束时,已经有task完成
                                        : calculateWithAllTasksRunning(); // 如果是初始时,还没有task 完成

                        checkTasksStarted(result.getTasksToWaitFor());

                        return result;
                    } catch (Throwable throwable) {
                        throw new CompletionException(throwable);
                    }
                },
                context.getMainExecutor());
    }

    /**
     * Checks if all tasks are attached with the current Execution already. This method should be
     * called from JobMaster main thread executor.
     *
     * @throws CheckpointException if some tasks do not have attached Execution.
     */
    private void checkAllTasksInitiated() throws CheckpointException {
        for (ExecutionVertex task : allTasks) {
            if (task.getCurrentExecutionAttempt() == null) {
                throw new CheckpointException(
                        String.format(
                                "task %s of job %s is not being executed at the moment. Aborting checkpoint.",
                                task.getTaskNameWithSubtaskIndex(), jobId),
                        CheckpointFailureReason.NOT_ALL_REQUIRED_TASKS_RUNNING);
            }
        }
    }

    /**
     * Checks if all tasks to trigger have already been in RUNNING state. This method should be
     * called from JobMaster main thread executor.
     *
     * @throws CheckpointException if some tasks to trigger have not turned into RUNNING yet.
     */
    private void checkTasksStarted(List<Execution> toTrigger) throws CheckpointException {
        for (Execution execution : toTrigger) {
            if (execution.getState() != ExecutionState.RUNNING) {
                throw new CheckpointException(
                        String.format(
                                "Checkpoint triggering task %s of job %s is not being executed at the moment. "
                                        + "Aborting checkpoint.",
                                execution.getVertex().getTaskNameWithSubtaskIndex(), jobId),
                        CheckpointFailureReason.NOT_ALL_REQUIRED_TASKS_RUNNING);
            }
        }
    }

    /**
     * Computes the checkpoint plan when all tasks are running. It would simply marks all the source
     * tasks as need to trigger and all the tasks as need to wait and commit.
     *
     * @return The plan of this checkpoint.
     */
    private CheckpointPlan calculateWithAllTasksRunning() {
        List<Execution> executionsToTrigger =
                sourceTasks.stream()
                        .map(ExecutionVertex::getCurrentExecutionAttempt)
                        .collect(Collectors.toList());

        List<Execution> tasksToWaitFor = createTaskToWaitFor(allTasks);

        return new DefaultCheckpointPlan(
                Collections.unmodifiableList(executionsToTrigger),
                Collections.unmodifiableList(tasksToWaitFor),
                Collections.unmodifiableList(allTasks),
                Collections.emptyList(),
                Collections.emptyList(),
                allowCheckpointsAfterTasksFinished);
    }

    /**
     * Calculates the checkpoint plan after some tasks have finished. We iterate the job graph to
     * find the task that is still running, but do not has precedent running tasks.
     *
     * @return The plan of this checkpoint.
     */
    private CheckpointPlan calculateAfterTasksFinished() {

        //  JobVertexID ->  各个subtask是否 running 的 bit 位
        Map<JobVertexID, BitSet> taskRunningStatusByVertex = collectTaskRunningStatus();

        List<Execution> tasksToTrigger = new ArrayList<>();
        List<Execution> tasksToWaitFor = new ArrayList<>();
        List<ExecutionVertex> tasksToCommitTo = new ArrayList<>();
        List<Execution> finishedTasks = new ArrayList<>();
        List<ExecutionJobVertex> fullyFinishedJobVertex = new ArrayList<>();

        for (ExecutionJobVertex jobVertex : jobVerticesInTopologyOrder) {
            BitSet taskRunningStatus = taskRunningStatusByVertex.get(jobVertex.getJobVertexId());

            // 如果 BitSet 中 所有的 bit 位都在 finish状态 （ finish状态为0 , running状态为1 ）
            if (taskRunningStatus.cardinality() == 0) {
                fullyFinishedJobVertex.add(jobVertex);

                for (ExecutionVertex task : jobVertex.getTaskVertices()) {
                    finishedTasks.add(task.getCurrentExecutionAttempt());
                }

                continue;
            }

            List<JobEdge> prevJobEdges = jobVertex.getJobVertex().getInputs();

            // 为true 则表示, jobVertex 下存在子任务能进入 trigger 集合
            boolean someTasksMustBeTriggered =
                    someTasksMustBeTriggered(taskRunningStatusByVertex, prevJobEdges);

            for (int i = 0; i < jobVertex.getTaskVertices().length; ++i) {
                ExecutionVertex task = jobVertex.getTaskVertices()[i];

                if (taskRunningStatus.get(task.getParallelSubtaskIndex())) {
                    // 如果 task 是running 状态
                    tasksToWaitFor.add(task.getCurrentExecutionAttempt());
                    tasksToCommitTo.add(task);

                    if (someTasksMustBeTriggered) {
                        // task 有资格进入 trigger 集合
                        boolean hasRunningPrecedentTasks =
                                hasRunningPrecedentTasks(
                                        task, prevJobEdges, taskRunningStatusByVertex);

                        if (!hasRunningPrecedentTasks) {
                            // 如果说 task 的上游没有处于running 的 task ; 而自己又是running 状态;
                            // 那么, 则说明  此时 task 自己就是源节点 ; 应该放到 trigger 的集合中
                            tasksToTrigger.add(task.getCurrentExecutionAttempt());
                        }
                    }
                } else {
                    // 如果 task 是 finish 状态
                    finishedTasks.add(task.getCurrentExecutionAttempt());
                }
            }
        }

        return new DefaultCheckpointPlan(
                Collections.unmodifiableList(tasksToTrigger),
                Collections.unmodifiableList(tasksToWaitFor),
                Collections.unmodifiableList(tasksToCommitTo),
                Collections.unmodifiableList(finishedTasks),
                Collections.unmodifiableList(fullyFinishedJobVertex),
                allowCheckpointsAfterTasksFinished);
    }


    private boolean someTasksMustBeTriggered(
            Map<JobVertexID, BitSet> runningTasksByVertex, List<JobEdge> prevJobEdges) {

        for (JobEdge jobEdge : prevJobEdges) {
            // DistributionPattern 有  ALL_TO_ALL  和  POINTWISE  两种模式
            DistributionPattern distributionPattern = jobEdge.getDistributionPattern();

            // jobEdge.getSource().getProducer() 是 上游 JobVertex
            BitSet upstreamRunningStatus =
                    runningTasksByVertex.get(jobEdge.getSource().getProducer().getID());

            //  重点:
            // 只有当上游没有 活跃的JobVertex , 本ExecutionVertex （subtask）才可能进入 trigger 集合 (作为源)
            //  其实 hasActiveUpstreamVertex 这个方法名取的不够好

            // 如果 是 all to all 模式 , 上游所有边 必须都是finish状态, 本 JobVertex下的sub task才有机会进入 trigger集合
            // 如果 是 point wise 模式 , 上游 存在边是 finish存在, 本 JobVertex下的sub task才有机会进入 trigger集合
            if (hasActiveUpstreamVertex(distributionPattern, upstreamRunningStatus)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Every task must have active upstream tasks if
     *
     * <ol>
     *   <li>ALL_TO_ALL connection and some predecessors are still running.
     *   <li>POINTWISE connection and all predecessors are still running.
     * </ol>
     *
     * @param distribution The distribution pattern between the upstream vertex and the current
     *     vertex.
     * @param upstreamRunningTasks The running tasks of the upstream vertex.
     * @return Whether every task of the current vertex is connected to some active predecessors.
     */
    // 如果 是 all to all 模式 , 上游所有边 必须都是finish状态, 本 JobVertex下的sub task才有机会进入 trigger集合
    // 如果 是 point wise 模式 , 上游 存在边是 finish存在, 本 JobVertex下的sub task就有机会进入 trigger集合
    private boolean hasActiveUpstreamVertex(
            DistributionPattern distribution, BitSet upstreamRunningTasks) {

        //（ running状态为1 finish状态为0 ）

        // 上游是ALL_TO_ALL连接方式 且 存在子任务是running状态
        // 或者   上游是POINTWISE连接方式 且 全部子任务都是running状态
        return (distribution == DistributionPattern.ALL_TO_ALL
                        && upstreamRunningTasks.cardinality() > 0)
                || (distribution == DistributionPattern.POINTWISE
                        && upstreamRunningTasks.cardinality() == upstreamRunningTasks.size());
    }

    private boolean hasRunningPrecedentTasks(
            ExecutionVertex vertex,
            List<JobEdge> prevJobEdges,
            Map<JobVertexID, BitSet> taskRunningStatusByVertex) {

        InternalExecutionGraphAccessor executionGraphAccessor = vertex.getExecutionGraphAccessor();

        for (int i = 0; i < prevJobEdges.size(); ++i) {
            if (prevJobEdges.get(i).getDistributionPattern() == DistributionPattern.POINTWISE) {
                for (IntermediateResultPartitionID consumedPartitionId :
                        vertex.getConsumedPartitionGroup(i)) {
                    ExecutionVertex precedentTask =
                            executionGraphAccessor
                                    .getResultPartitionOrThrow(consumedPartitionId)
                                    .getProducer();
                    BitSet precedentVertexRunningStatus =
                            taskRunningStatusByVertex.get(precedentTask.getJobvertexId());

                    if (precedentVertexRunningStatus.get(precedentTask.getParallelSubtaskIndex())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Collects the task running status for each job vertex.
     *
     * @return The task running status for each job vertex.
     */
    @VisibleForTesting
    Map<JobVertexID, BitSet> collectTaskRunningStatus() {
        Map<JobVertexID, BitSet> runningStatusByVertex = new HashMap<>();

        for (ExecutionJobVertex vertex : jobVerticesInTopologyOrder) {
            BitSet runningTasks = new BitSet(vertex.getTaskVertices().length);

            for (int i = 0; i < vertex.getTaskVertices().length; ++i) {
                if (!vertex.getTaskVertices()[i].getCurrentExecutionAttempt().isFinished()) {
                    // 执行属于 running 状态,则设置
                    runningTasks.set(i);
                }
            }

            runningStatusByVertex.put(vertex.getJobVertexId(), runningTasks);
        }

        return runningStatusByVertex;
    }

    private List<Execution> createTaskToWaitFor(List<ExecutionVertex> tasks) {
        List<Execution> tasksToAck = new ArrayList<>(tasks.size());
        for (ExecutionVertex task : tasks) {
            tasksToAck.add(task.getCurrentExecutionAttempt());
        }

        return tasksToAck;
    }
}
