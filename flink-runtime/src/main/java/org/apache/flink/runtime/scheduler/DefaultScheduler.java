/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flink.runtime.scheduler;

import org.apache.flink.api.common.JobStatus;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.checkpoint.CheckpointRecoveryFactory;
import org.apache.flink.runtime.checkpoint.CheckpointsCleaner;
import org.apache.flink.runtime.clusterframework.types.AllocationID;
import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutor;
import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.executiongraph.ExecutionJobVertex;
import org.apache.flink.runtime.executiongraph.ExecutionVertex;
import org.apache.flink.runtime.executiongraph.JobStatusListener;
import org.apache.flink.runtime.executiongraph.TaskExecutionStateTransition;
import org.apache.flink.runtime.executiongraph.failover.flip1.ExecutionFailureHandler;
import org.apache.flink.runtime.executiongraph.failover.flip1.FailoverStrategy;
import org.apache.flink.runtime.executiongraph.failover.flip1.FailureHandlingResult;
import org.apache.flink.runtime.executiongraph.failover.flip1.RestartBackoffTimeStrategy;
import org.apache.flink.runtime.jobgraph.IntermediateResultPartitionID;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobmanager.scheduler.CoLocationGroup;
import org.apache.flink.runtime.jobmanager.scheduler.NoResourceAvailableException;
import org.apache.flink.runtime.jobmanager.scheduler.SlotSharingGroup;
import org.apache.flink.runtime.jobmaster.LogicalSlot;
import org.apache.flink.runtime.metrics.groups.JobManagerJobMetricGroup;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinatorHolder;
import org.apache.flink.runtime.scheduler.exceptionhistory.FailureHandlingResultSnapshot;
import org.apache.flink.runtime.scheduler.strategy.ExecutionVertexID;
import org.apache.flink.runtime.scheduler.strategy.SchedulingStrategy;
import org.apache.flink.runtime.scheduler.strategy.SchedulingStrategyFactory;
import org.apache.flink.runtime.scheduler.strategy.SchedulingTopology;
import org.apache.flink.runtime.shuffle.ShuffleMaster;
import org.apache.flink.runtime.taskmanager.TaskManagerLocation;
import org.apache.flink.runtime.topology.Vertex;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.IterableUtils;
import org.apache.flink.util.concurrent.FutureUtils;
import org.apache.flink.util.concurrent.ScheduledExecutor;

import org.slf4j.Logger;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/** The future default scheduler. */
public class DefaultScheduler extends SchedulerBase implements SchedulerOperations {

    private final Logger log;

    private final ClassLoader userCodeLoader;

    private final ExecutionSlotAllocator executionSlotAllocator;

    private final ExecutionFailureHandler executionFailureHandler;

    private final ScheduledExecutor delayExecutor;

    private final SchedulingStrategy schedulingStrategy;

    private final ExecutionVertexOperations executionVertexOperations;

    private final Set<ExecutionVertexID> verticesWaitingForRestart;

    private final ShuffleMaster<?> shuffleMaster;

    private final Time rpcTimeout;

    private final Map<AllocationID, Long> reservedAllocationRefCounters;

    // once an execution vertex is assigned an allocation/slot, it will reserve the allocation
    // until it is assigned a new allocation, or it finishes and does not need the allocation
    // anymore. The reserved allocation information is needed for local recovery.
    private final Map<ExecutionVertexID, AllocationID> reservedAllocationByExecutionVertex;

    DefaultScheduler(
            final Logger log,
            final JobGraph jobGraph,
            final Executor ioExecutor,
            final Configuration jobMasterConfiguration,
            final Consumer<ComponentMainThreadExecutor> startUpAction,
            final ScheduledExecutor delayExecutor,
            final ClassLoader userCodeLoader,
            final CheckpointsCleaner checkpointsCleaner,
            final CheckpointRecoveryFactory checkpointRecoveryFactory,
            final JobManagerJobMetricGroup jobManagerJobMetricGroup,
            final SchedulingStrategyFactory schedulingStrategyFactory,
            final FailoverStrategy.Factory failoverStrategyFactory,
            final RestartBackoffTimeStrategy restartBackoffTimeStrategy,
            final ExecutionVertexOperations executionVertexOperations,
            final ExecutionVertexVersioner executionVertexVersioner,
            final ExecutionSlotAllocatorFactory executionSlotAllocatorFactory,
            long initializationTimestamp,
            final ComponentMainThreadExecutor mainThreadExecutor,
            final JobStatusListener jobStatusListener,
            final ExecutionGraphFactory executionGraphFactory,
            final ShuffleMaster<?> shuffleMaster,
            final Time rpcTimeout)
            throws Exception {

        super(
                log,
                jobGraph,
                ioExecutor,
                jobMasterConfiguration,
                userCodeLoader,
                checkpointsCleaner,
                checkpointRecoveryFactory,
                jobManagerJobMetricGroup,
                executionVertexVersioner,
                initializationTimestamp,
                mainThreadExecutor,
                jobStatusListener,
                executionGraphFactory);

        this.log = log;

        this.delayExecutor = checkNotNull(delayExecutor);
        this.userCodeLoader = checkNotNull(userCodeLoader);
        this.executionVertexOperations = checkNotNull(executionVertexOperations);
        this.shuffleMaster = checkNotNull(shuffleMaster);
        this.rpcTimeout = checkNotNull(rpcTimeout);

        this.reservedAllocationRefCounters = new HashMap<>();
        this.reservedAllocationByExecutionVertex = new HashMap<>();

        final FailoverStrategy failoverStrategy =
                failoverStrategyFactory.create(
                        getSchedulingTopology(), getResultPartitionAvailabilityChecker());
        log.info(
                "Using failover strategy {} for {} ({}).",
                failoverStrategy,
                jobGraph.getName(),
                jobGraph.getJobID());

        enrichResourceProfile();

        this.executionFailureHandler =
                new ExecutionFailureHandler(
                        getSchedulingTopology(), failoverStrategy, restartBackoffTimeStrategy);

        // 这里的实现 只有 PipelinedRegionSchedulingStrategy 策略
        this.schedulingStrategy =
                schedulingStrategyFactory.createInstance(this, getSchedulingTopology());

        this.executionSlotAllocator =
                checkNotNull(executionSlotAllocatorFactory)
                        .createInstance(new DefaultExecutionSlotAllocationContext());

        this.verticesWaitingForRestart = new HashSet<>();
        startUpAction.accept(mainThreadExecutor);
    }

    // ------------------------------------------------------------------------
    // SchedulerNG
    // ------------------------------------------------------------------------

    @Override
    protected long getNumberOfRestarts() {
        return executionFailureHandler.getNumberOfRestarts();
    }

    @Override
    protected void cancelAllPendingSlotRequestsInternal() {
        IterableUtils.toStream(getSchedulingTopology().getVertices())
                .map(Vertex::getId)
                .forEach(executionSlotAllocator::cancel);
    }

    @Override
    protected void startSchedulingInternal() {
        log.info(
                "Starting scheduling with scheduling strategy [{}]",
                schedulingStrategy.getClass().getName());
        /*
           transitionToRunning的调用逻辑如下
               transitionToRunning
               transitionState(JobStatus.CREATED, JobStatus.RUNNING)
               transitionState(current, newState, null)
               notifyJobStatusChange(newState, error)
                   回调所有 jobStatusListeners 的jobStatusChanges方法
                        其中:
                          1  CheckpointCoordinatorDeActivator 会调用 CheckpointCoordinator的
                               startCheckpointScheduler 来开启checkpoint的调度

                          2  JobMaster.JobManagerJobStatusListener 内部类
        */
        // step1  开始周期性的 checkpoint 调度   （CheckpointCoordinator 的功能）
        transitionToRunning();

        // step2  以  region 为单位, 开始调度启动 tasks
        schedulingStrategy.startScheduling();
    }

    @Override
    protected void updateTaskExecutionStateInternal(
            final ExecutionVertexID executionVertexId,
            final TaskExecutionStateTransition taskExecutionState) {

        // once a task finishes, it will release the assigned allocation/slot and no longer
        // needs it. Therefore, it should stop reserving the slot so that other tasks are
        // possible to use the slot. Ideally, the `stopReserveAllocation` should happen
        // along with the release slot process. However, that process is hidden in the depth
        // of the ExecutionGraph, so we currently do it in DefaultScheduler after that process
        // is done.
        if (taskExecutionState.getExecutionState() == ExecutionState.FINISHED) {
            stopReserveAllocation(executionVertexId);
        }

        schedulingStrategy.onExecutionStateChange(
                executionVertexId, taskExecutionState.getExecutionState());
        maybeHandleTaskFailure(taskExecutionState, executionVertexId);
    }

    private void maybeHandleTaskFailure(
            final TaskExecutionStateTransition taskExecutionState,
            final ExecutionVertexID executionVertexId) {

        if (taskExecutionState.getExecutionState() == ExecutionState.FAILED) {
            final Throwable error = taskExecutionState.getError(userCodeLoader);
            handleTaskFailure(executionVertexId, error);
        }
    }

    // 处理某个 task失败 , 重启的 ExecutionVertexID 只有一个
    private void handleTaskFailure(
            final ExecutionVertexID executionVertexId, @Nullable final Throwable error) {
        final long timestamp = System.currentTimeMillis();
        setGlobalFailureCause(error, timestamp);
        // 通知 算子协调者 任务失败
        notifyCoordinatorsAboutTaskFailure(executionVertexId, error);
        // 处理执行失败,构造返回失败处理结果
        final FailureHandlingResult failureHandlingResult =
                executionFailureHandler.getFailureHandlingResult(
                        executionVertexId, error, timestamp);
        // 根据失败处理的结果,决定是否重启
        maybeRestartTasks(failureHandlingResult);
    }

    private void notifyCoordinatorsAboutTaskFailure(
            final ExecutionVertexID executionVertexId, @Nullable final Throwable error) {
        final ExecutionJobVertex jobVertex =
                getExecutionJobVertex(executionVertexId.getJobVertexId());
        final int subtaskIndex = executionVertexId.getSubtaskIndex();

        jobVertex.getOperatorCoordinators().forEach(c -> c.subtaskFailed(subtaskIndex, error));
    }

    // 处理 全局 失败, 重启计算拓扑中的所有节点
    @Override
    public void handleGlobalFailure(final Throwable error) {
        final long timestamp = System.currentTimeMillis();
        setGlobalFailureCause(error, timestamp);

        log.info("Trying to recover from a global failure.", error);
        final FailureHandlingResult failureHandlingResult =
                executionFailureHandler.getGlobalFailureHandlingResult(error, timestamp);
        maybeRestartTasks(failureHandlingResult);
    }

    private void maybeRestartTasks(final FailureHandlingResult failureHandlingResult) {
        if (failureHandlingResult.canRestart()) {
            restartTasksWithDelay(failureHandlingResult);
        } else {
            failJob(failureHandlingResult.getError(), failureHandlingResult.getTimestamp());
        }
    }

    // 重启任务（延迟）
    //    task失败 和 全局失败,  都会用这个方法来重启
    //    前者会重启一个节点,  后者重启所有节点
    private void restartTasksWithDelay(final FailureHandlingResult failureHandlingResult) {
        // 得到要重启的顶点
        final Set<ExecutionVertexID> verticesToRestart =
                failureHandlingResult.getVerticesToRestart();

        final Set<ExecutionVertexVersion> executionVertexVersions =
                new HashSet<>(
                        executionVertexVersioner
                                .recordVertexModifications(verticesToRestart)
                                .values());

        // 是否是全局失败, 全局失败,后续得全局恢复; task 失败，后续得 单个task恢复
        final boolean globalRecovery = failureHandlingResult.isGlobalFailure();

        addVerticesToRestartPending(verticesToRestart);

        final CompletableFuture<?> cancelFuture = cancelTasksAsync(verticesToRestart);

        final FailureHandlingResultSnapshot failureHandlingResultSnapshot =
                FailureHandlingResultSnapshot.create(
                        failureHandlingResult,
                        id -> this.getExecutionVertex(id).getCurrentExecutionAttempt());

        delayExecutor.schedule(
                () ->
                        FutureUtils.assertNoException(
                                cancelFuture.thenRunAsync(
                                        () -> {
                                            archiveFromFailureHandlingResult(
                                                    failureHandlingResultSnapshot);
                                            // 核心
                                            restartTasks(executionVertexVersions, globalRecovery);
                                        },
                                        getMainThreadExecutor())),
                failureHandlingResult.getRestartDelayMS(),
                TimeUnit.MILLISECONDS);
    }

    private void addVerticesToRestartPending(final Set<ExecutionVertexID> verticesToRestart) {
        verticesWaitingForRestart.addAll(verticesToRestart);
        transitionExecutionGraphState(JobStatus.RUNNING, JobStatus.RESTARTING);
    }

    private void removeVerticesFromRestartPending(final Set<ExecutionVertexID> verticesToRestart) {
        verticesWaitingForRestart.removeAll(verticesToRestart);
        if (verticesWaitingForRestart.isEmpty()) {
            transitionExecutionGraphState(JobStatus.RESTARTING, JobStatus.RUNNING);
        }
    }

    private void restartTasks(
            final Set<ExecutionVertexVersion> executionVertexVersions,
            final boolean isGlobalRecovery) {
        // 获取所有需要重启的 且 没有发生版本修改的计算节点
        final Set<ExecutionVertexID> verticesToRestart =
                executionVertexVersioner.getUnmodifiedExecutionVertices(executionVertexVersions);

        removeVerticesFromRestartPending(verticesToRestart);

        resetForNewExecutions(verticesToRestart);

        try {
            //核心, 重启所有的任务的时候得 先恢复状态 （状态句柄）
            restoreState(verticesToRestart, isGlobalRecovery);
        } catch (Throwable t) {
            handleGlobalFailure(t);
            return;
        }
        // 重启计算节点
        schedulingStrategy.restartTasks(verticesToRestart);
    }

    private CompletableFuture<?> cancelTasksAsync(final Set<ExecutionVertexID> verticesToRestart) {
        // clean up all the related pending requests to avoid that immediately returned slot
        // is used to fulfill the pending requests of these tasks
        verticesToRestart.stream().forEach(executionSlotAllocator::cancel);

        final List<CompletableFuture<?>> cancelFutures =
                verticesToRestart.stream()
                        .map(this::cancelExecutionVertex)
                        .collect(Collectors.toList());

        return FutureUtils.combineAll(cancelFutures);
    }

    private CompletableFuture<?> cancelExecutionVertex(final ExecutionVertexID executionVertexId) {
        final ExecutionVertex vertex = getExecutionVertex(executionVertexId);

        notifyCoordinatorOfCancellation(vertex);

        return executionVertexOperations.cancel(vertex);
    }

    @Override
    protected void notifyPartitionDataAvailableInternal(
            final IntermediateResultPartitionID partitionId) {
        schedulingStrategy.onPartitionConsumable(partitionId);
    }

    // ------------------------------------------------------------------------
    // SchedulerOperations
    // ------------------------------------------------------------------------

    @Override
    public void allocateSlotsAndDeploy(
            final List<ExecutionVertexDeploymentOption> executionVertexDeploymentOptions) {
        validateDeploymentOptions(executionVertexDeploymentOptions);

        final Map<ExecutionVertexID, ExecutionVertexDeploymentOption> deploymentOptionsByVertex =
                groupDeploymentOptionsByVertexId(executionVertexDeploymentOptions);

        // 得到一个region中所有要部署的 ExecutionVertexID
        final List<ExecutionVertexID> verticesToDeploy =
                executionVertexDeploymentOptions.stream()
                        .map(ExecutionVertexDeploymentOption::getExecutionVertexId)
                        .collect(Collectors.toList());

        // 计算所有ExecutionVertexID 当前应该有的版本号
        final Map<ExecutionVertexID, ExecutionVertexVersion> requiredVersionByVertex =
                executionVertexVersioner.recordVertexModifications(verticesToDeploy);

        // 1 先在 executionGraph 中 Map<JobVertexID, ExecutionJobVertex> tasks 成员
        // 2 然后 得到 ExecutionJobVertex 里面 的 ExecutionVertex[] taskVertices;
        // 3 得到 相关 ExecutionVertex 里面的 Execution currentExecution  成员
        // 4 最后 改变 currentExecution 对象 的 ExecutionState state  方法
        transitionToScheduled(verticesToDeploy);

        // 给每个执行顶点 分配 逻辑槽位 （ 利用 ExecutionSlotAllocator 分配器（ SlotSharingExecutionSlotAllocator是实现类））
        final List<SlotExecutionVertexAssignment> slotExecutionVertexAssignments =
                allocateSlots(executionVertexDeploymentOptions);

        final List<DeploymentHandle> deploymentHandles =
                createDeploymentHandles(
                        requiredVersionByVertex,
                        deploymentOptionsByVertex,
                        slotExecutionVertexAssignments);

        waitForAllSlotsAndDeploy(deploymentHandles);
    }

    private void validateDeploymentOptions(
            final Collection<ExecutionVertexDeploymentOption> deploymentOptions) {
        deploymentOptions.stream()
                .map(ExecutionVertexDeploymentOption::getExecutionVertexId)
                .map(this::getExecutionVertex)
                .forEach(
                        v ->
                                checkState(
                                        v.getExecutionState() == ExecutionState.CREATED,
                                        "expected vertex %s to be in CREATED state, was: %s",
                                        v.getID(),
                                        v.getExecutionState()));
    }

    private static Map<ExecutionVertexID, ExecutionVertexDeploymentOption>
            groupDeploymentOptionsByVertexId(
                    final Collection<ExecutionVertexDeploymentOption>
                            executionVertexDeploymentOptions) {
        return executionVertexDeploymentOptions.stream()
                .collect(
                        Collectors.toMap(
                                ExecutionVertexDeploymentOption::getExecutionVertexId,
                                Function.identity()));
    }

    // 分配逻辑槽位
    private List<SlotExecutionVertexAssignment> allocateSlots(
            final List<ExecutionVertexDeploymentOption> executionVertexDeploymentOptions) {

        //给  List<ExecutionVertexID> executionVertexIds 分配槽位
        return executionSlotAllocator.allocateSlotsFor(
                executionVertexDeploymentOptions.stream()
                        .map(ExecutionVertexDeploymentOption::getExecutionVertexId)
                        .collect(Collectors.toList()));
    }

    private static List<DeploymentHandle> createDeploymentHandles(
            final Map<ExecutionVertexID, ExecutionVertexVersion> requiredVersionByVertex,
            final Map<ExecutionVertexID, ExecutionVertexDeploymentOption> deploymentOptionsByVertex,
            final List<SlotExecutionVertexAssignment> slotExecutionVertexAssignments) {

        return slotExecutionVertexAssignments.stream()
                .map(
                        slotExecutionVertexAssignment -> {
                            final ExecutionVertexID executionVertexId =
                                    slotExecutionVertexAssignment.getExecutionVertexId();
                            return new DeploymentHandle(
                                    requiredVersionByVertex.get(executionVertexId),
                                    deploymentOptionsByVertex.get(executionVertexId),
                                    slotExecutionVertexAssignment);
                        })
                .collect(Collectors.toList());
    }

    private void waitForAllSlotsAndDeploy(final List<DeploymentHandle> deploymentHandles) {
        FutureUtils.assertNoException(
                // 分配计算资源和注册shuffle 信息
                assignAllResourcesAndRegisterProducedPartitions(deploymentHandles)
                        .handle(deployAll(deploymentHandles)));
    }

    // 分配计算资源和注册shuffle 信息
    private CompletableFuture<Void> assignAllResourcesAndRegisterProducedPartitions(
            final List<DeploymentHandle> deploymentHandles) {
        final List<CompletableFuture<Void>> resultFutures = new ArrayList<>();
        for (DeploymentHandle deploymentHandle : deploymentHandles) {
            final CompletableFuture<Void> resultFuture =
                    deploymentHandle
                            .getSlotExecutionVertexAssignment()
                            .getLogicalSlotFuture()
                            .handle(assignResource(deploymentHandle)) // 分配计算资源
                            .thenCompose(
                                    registerProducedPartitions(deploymentHandle)) // 注册shuffle信息
                            .handle(
                                    (ignore, throwable) -> {
                                        if (throwable != null) {
                                            handleTaskDeploymentFailure(
                                                    deploymentHandle.getExecutionVertexId(),
                                                    throwable);
                                        }
                                        return null;
                                    });

            resultFutures.add(resultFuture);
        }
        return FutureUtils.waitForAll(resultFutures);
    }

    private BiFunction<Void, Throwable, Void> deployAll(
            final List<DeploymentHandle> deploymentHandles) {
        return (ignored, throwable) -> {
            propagateIfNonNull(throwable);
            // 遍历 部署所有的执行顶点 （任务）
            for (final DeploymentHandle deploymentHandle : deploymentHandles) {
                final SlotExecutionVertexAssignment slotExecutionVertexAssignment =
                        deploymentHandle.getSlotExecutionVertexAssignment();
                final CompletableFuture<LogicalSlot> slotAssigned =
                        slotExecutionVertexAssignment.getLogicalSlotFuture();
                checkState(slotAssigned.isDone());

                FutureUtils.assertNoException(
                        // deployOrHandleError 核心
                        slotAssigned.handle(deployOrHandleError(deploymentHandle)));
            }
            return null;
        };
    }

    private static void propagateIfNonNull(final Throwable throwable) {
        if (throwable != null) {
            throw new CompletionException(throwable);
        }
    }

    private BiFunction<LogicalSlot, Throwable, LogicalSlot> assignResource(
            final DeploymentHandle deploymentHandle) {
        final ExecutionVertexVersion requiredVertexVersion =
                deploymentHandle.getRequiredVertexVersion();
        final ExecutionVertexID executionVertexId = deploymentHandle.getExecutionVertexId();

        return (logicalSlot, throwable) -> {
            if (executionVertexVersioner.isModified(requiredVertexVersion)) {
                if (throwable == null) {
                    log.debug(
                            "Refusing to assign slot to execution vertex {} because this deployment was "
                                    + "superseded by another deployment",
                            executionVertexId);
                    releaseSlotIfPresent(logicalSlot);
                }
                return null;
            }

            // throw exception only if the execution version is not outdated.
            // this ensures that canceling a pending slot request does not fail
            // a task which is about to cancel in #restartTasksWithDelay(...)
            if (throwable != null) {
                throw new CompletionException(maybeWrapWithNoResourceAvailableException(throwable));
            }

            final ExecutionVertex executionVertex = getExecutionVertex(executionVertexId);
            executionVertex.tryAssignResource(logicalSlot); // 分配slot

            startReserveAllocation(executionVertexId, logicalSlot.getAllocationId());

            return logicalSlot;
        };
    }

    private void startReserveAllocation(
            ExecutionVertexID executionVertexId, AllocationID newAllocation) {

        // stop the previous allocation reservation if there is one
        stopReserveAllocation(executionVertexId);

        reservedAllocationByExecutionVertex.put(executionVertexId, newAllocation);
        reservedAllocationRefCounters.compute(
                newAllocation, (ignored, oldCount) -> oldCount == null ? 1 : oldCount + 1);
    }

    private void stopReserveAllocation(ExecutionVertexID executionVertexId) {
        final AllocationID priorAllocation =
                reservedAllocationByExecutionVertex.remove(executionVertexId);
        if (priorAllocation != null) {
            reservedAllocationRefCounters.compute(
                    priorAllocation, (ignored, oldCount) -> oldCount > 1 ? oldCount - 1 : null);
        }
    }

    private Function<LogicalSlot, CompletableFuture<Void>> registerProducedPartitions(
            final DeploymentHandle deploymentHandle) {
        final ExecutionVertexID executionVertexId = deploymentHandle.getExecutionVertexId();

        return logicalSlot -> {
            // a null logicalSlot means the slot assignment is skipped, in which case
            // the produced partition registration process can be skipped as well
            if (logicalSlot != null) {
                final ExecutionVertex executionVertex = getExecutionVertex(executionVertexId);
                final boolean notifyPartitionDataAvailable =
                        deploymentHandle.getDeploymentOption().notifyPartitionDataAvailable();

                final CompletableFuture<Void> partitionRegistrationFuture =
                        executionVertex
                                .getCurrentExecutionAttempt()
                                .registerProducedPartitions(
                                        logicalSlot.getTaskManagerLocation(),
                                        notifyPartitionDataAvailable);

                return FutureUtils.orTimeout(
                        partitionRegistrationFuture,
                        rpcTimeout.toMilliseconds(),
                        TimeUnit.MILLISECONDS,
                        getMainThreadExecutor());
            } else {
                return FutureUtils.completedVoidFuture();
            }
        };
    }

    private void releaseSlotIfPresent(@Nullable final LogicalSlot logicalSlot) {
        if (logicalSlot != null) {
            logicalSlot.releaseSlot(null);
        }
    }

    private void handleTaskDeploymentFailure(
            final ExecutionVertexID executionVertexId, final Throwable error) {
        executionVertexOperations.markFailed(getExecutionVertex(executionVertexId), error);
    }

    private static Throwable maybeWrapWithNoResourceAvailableException(final Throwable failure) {
        final Throwable strippedThrowable = ExceptionUtils.stripCompletionException(failure);
        if (strippedThrowable instanceof TimeoutException) {
            return new NoResourceAvailableException(
                    "Could not allocate the required slot within slot request timeout. "
                            + "Please make sure that the cluster has enough resources.",
                    failure);
        } else {
            return failure;
        }
    }

    private BiFunction<Object, Throwable, Void> deployOrHandleError(
            final DeploymentHandle deploymentHandle) {
        final ExecutionVertexVersion requiredVertexVersion =
                deploymentHandle.getRequiredVertexVersion();
        final ExecutionVertexID executionVertexId = requiredVertexVersion.getExecutionVertexId();

        return (ignored, throwable) -> {
            if (executionVertexVersioner.isModified(requiredVertexVersion)) {
                log.debug(
                        "Refusing to deploy execution vertex {} because this deployment was "
                                + "superseded by another deployment",
                        executionVertexId);
                return null;
            }

            if (throwable == null) {
                // 正常情况
                deployTaskSafe(executionVertexId);
            } else {
                // 如果有异常
                handleTaskDeploymentFailure(executionVertexId, throwable);
            }
            return null;
        };
    }

    private void deployTaskSafe(final ExecutionVertexID executionVertexId) {
        try {
            final ExecutionVertex executionVertex = getExecutionVertex(executionVertexId);
            // 部署执行顶点
            executionVertexOperations.deploy(executionVertex);
        } catch (Throwable e) {
            handleTaskDeploymentFailure(executionVertexId, e);
        }
    }

    private void notifyCoordinatorOfCancellation(ExecutionVertex vertex) {
        // this method makes a best effort to filter out duplicate notifications, meaning cases
        // where
        // the coordinator was already notified for that specific task
        // we don't notify if the task is already FAILED, CANCELLING, or CANCELED

        final ExecutionState currentState = vertex.getExecutionState();
        if (currentState == ExecutionState.FAILED
                || currentState == ExecutionState.CANCELING
                || currentState == ExecutionState.CANCELED) {
            return;
        }

        for (OperatorCoordinatorHolder coordinator :
                vertex.getJobVertex().getOperatorCoordinators()) {
            coordinator.subtaskFailed(vertex.getParallelSubtaskIndex(), null);
        }
    }

    private class DefaultExecutionSlotAllocationContext implements ExecutionSlotAllocationContext {

        @Override
        public ResourceProfile getResourceProfile(final ExecutionVertexID executionVertexId) {
            return getExecutionVertex(executionVertexId).getResourceProfile();
        }

        @Override
        public AllocationID getPriorAllocationId(final ExecutionVertexID executionVertexId) {
            return getExecutionVertex(executionVertexId).getLatestPriorAllocation();
        }

        @Override
        public SchedulingTopology getSchedulingTopology() {
            return DefaultScheduler.this.getSchedulingTopology();
        }

        @Override
        public Set<SlotSharingGroup> getLogicalSlotSharingGroups() {
            return getJobGraph().getSlotSharingGroups();
        }

        @Override
        public Set<CoLocationGroup> getCoLocationGroups() {
            return getJobGraph().getCoLocationGroups();
        }

        @Override
        public Collection<Collection<ExecutionVertexID>> getConsumedResultPartitionsProducers(
                ExecutionVertexID executionVertexId) {
            return inputsLocationsRetriever.getConsumedResultPartitionsProducers(executionVertexId);
        }

        @Override
        public Optional<CompletableFuture<TaskManagerLocation>> getTaskManagerLocation(
                ExecutionVertexID executionVertexId) {
            return inputsLocationsRetriever.getTaskManagerLocation(executionVertexId);
        }

        @Override
        public Optional<TaskManagerLocation> getStateLocation(ExecutionVertexID executionVertexId) {
            return stateLocationRetriever.getStateLocation(executionVertexId);
        }

        @Override
        public Set<AllocationID> getReservedAllocations() {
            return reservedAllocationRefCounters.keySet();
        }
    }

    private void enrichResourceProfile() {
        Set<SlotSharingGroup> ssgs = new HashSet<>();
        getJobGraph().getVertices().forEach(jv -> ssgs.add(jv.getSlotSharingGroup()));
        ssgs.forEach(
                ssg ->
                        SsgNetworkMemoryCalculationUtils.enrichNetworkMemory(
                                ssg, this::getExecutionJobVertex, shuffleMaster));
    }
}
