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

package org.apache.flink.runtime.scheduler;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.clusterframework.types.SlotProfile;
import org.apache.flink.runtime.jobmaster.LogicalSlot;
import org.apache.flink.runtime.jobmaster.SlotRequestId;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlot;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotProvider;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotRequest;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotRequestBulkChecker;
import org.apache.flink.runtime.scheduler.SharedSlotProfileRetriever.SharedSlotProfileRetrieverFactory;
import org.apache.flink.runtime.scheduler.strategy.ExecutionVertexID;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Allocates {@link LogicalSlot}s from physical shared slots.
 *
 * <p>The allocator maintains a shared slot for each {@link ExecutionSlotSharingGroup}. It allocates
 * a physical slot for the shared slot and then allocates logical slots from it for scheduled tasks.
 * The physical slot is lazily allocated for a shared slot, upon any hosted subtask asking for the
 * shared slot. Each subsequent sharing subtask allocates a logical slot from the existing shared
 * slot. The shared/physical slot can be released only if all the requested logical slots are
 * released or canceled.
 */
class SlotSharingExecutionSlotAllocator implements ExecutionSlotAllocator {
    private static final Logger LOG =
            LoggerFactory.getLogger(SlotSharingExecutionSlotAllocator.class);

    private final PhysicalSlotProvider slotProvider;

    private final boolean slotWillBeOccupiedIndefinitely;

    private final SlotSharingStrategy slotSharingStrategy;

    private final Map<ExecutionSlotSharingGroup, SharedSlot> sharedSlots;

    private final SharedSlotProfileRetrieverFactory sharedSlotProfileRetrieverFactory;

    private final PhysicalSlotRequestBulkChecker bulkChecker;

    private final Time allocationTimeout;

    private final Function<ExecutionVertexID, ResourceProfile> resourceProfileRetriever;

    SlotSharingExecutionSlotAllocator(
            PhysicalSlotProvider slotProvider,
            boolean slotWillBeOccupiedIndefinitely,
            SlotSharingStrategy slotSharingStrategy,
            SharedSlotProfileRetrieverFactory sharedSlotProfileRetrieverFactory,
            PhysicalSlotRequestBulkChecker bulkChecker,
            Time allocationTimeout,
            Function<ExecutionVertexID, ResourceProfile> resourceProfileRetriever) {
        this.slotProvider = checkNotNull(slotProvider);
        this.slotWillBeOccupiedIndefinitely = slotWillBeOccupiedIndefinitely;
        this.slotSharingStrategy = checkNotNull(slotSharingStrategy);
        this.sharedSlotProfileRetrieverFactory = checkNotNull(sharedSlotProfileRetrieverFactory);
        this.bulkChecker = checkNotNull(bulkChecker);
        this.allocationTimeout = checkNotNull(allocationTimeout);
        this.resourceProfileRetriever = checkNotNull(resourceProfileRetriever);
        this.sharedSlots = new IdentityHashMap<>();
    }

    /**
     * Creates logical {@link SlotExecutionVertexAssignment}s from physical shared slots.
     *
     * <p>The allocation has the following steps:
     *
     * <ol>
     *   <li>Map the executions to {@link ExecutionSlotSharingGroup}s using {@link
     *       SlotSharingStrategy}
     *   <li>Check which {@link ExecutionSlotSharingGroup}s already have shared slot
     *   <li>For all involved {@link ExecutionSlotSharingGroup}s which do not have a shared slot
     *       yet:
     *   <li>Create a {@link SlotProfile} future using {@link SharedSlotProfileRetriever} and then
     *   <li>Allocate a physical slot from the {@link PhysicalSlotProvider}
     *   <li>Create a shared slot based on the returned physical slot futures
     *   <li>Allocate logical slot futures for the executions from all corresponding shared slots.
     *   <li>If a physical slot request fails, associated logical slot requests are canceled within
     *       the shared slot
     *   <li>Generate {@link SlotExecutionVertexAssignment}s based on the logical slot futures and
     *       returns the results.
     * </ol>
     *
     * @param executionVertexIds Execution vertices to allocate slots for
     *
     *  从物理的共享槽位 创建逻辑的多个 SlotExecutionVertexAssignment
     *
     */
    // SlotExecutionVertexAssignment 中包含了  执行顶点  和  它分配到的逻辑槽位的future对象
    @Override
    public List<SlotExecutionVertexAssignment> allocateSlotsFor(
            List<ExecutionVertexID> executionVertexIds) {

        SharedSlotProfileRetriever sharedSlotProfileRetriever =
                sharedSlotProfileRetrieverFactory.createFromBulk(new HashSet<>(executionVertexIds));

        // 用ExecutionSlotSharingGroup 将 这些ExecutionVertexID 分组
        Map<ExecutionSlotSharingGroup, List<ExecutionVertexID>> executionsByGroup =
                executionVertexIds.stream()
                        .collect(
                                // 这种写法可以借鉴
                                Collectors.groupingBy(
                                        slotSharingStrategy::getExecutionSlotSharingGroup));

        // slots  和 executionsByGroup 这两者都是 以 ExecutionSlotSharingGroup 对象作为key
        Map<ExecutionSlotSharingGroup, SharedSlot> slots =
                executionsByGroup.keySet().stream()
                        // 针对每个ExecutionSlotSharingGroup  分配 共享槽位
                        .map(group -> getOrAllocateSharedSlot(group, sharedSlotProfileRetriever))
                        .collect(
                                Collectors.toMap(
                                        SharedSlot::getExecutionSlotSharingGroup,
                                        Function.identity()));

        // 从 SharedSlot 来分配 逻辑槽位
        Map<ExecutionVertexID, SlotExecutionVertexAssignment> assignments =
                allocateLogicalSlotsFromSharedSlots(slots, executionsByGroup);

        // 创建 SharingPhysicalSlotRequestBulk 对象
        // 如果 一个物理的槽位请求失败, 与之相关联的 在一个共享槽位中的 所有逻辑槽位请求都会被取消
        SharingPhysicalSlotRequestBulk bulk = createBulk(slots, executionsByGroup);

        // 调度 检查 bulk 对象
        bulkChecker.schedulePendingRequestBulkTimeoutCheck(bulk, allocationTimeout);

        return executionVertexIds.stream().map(assignments::get).collect(Collectors.toList());
    }

    @Override
    public void cancel(ExecutionVertexID executionVertexId) {
        cancelLogicalSlotRequest(executionVertexId, null);
    }

    // 取消逻辑槽位请求
    private void cancelLogicalSlotRequest(ExecutionVertexID executionVertexId, Throwable cause) {

        // 先拿到执行顶点的 执行共享槽位组
        ExecutionSlotSharingGroup executionSlotSharingGroup =
                slotSharingStrategy.getExecutionSlotSharingGroup(executionVertexId);
        checkNotNull(
                executionSlotSharingGroup,
                "There is no ExecutionSlotSharingGroup for ExecutionVertexID " + executionVertexId);
        // 通过执行共享槽位组 拿到 共享槽位
        SharedSlot slot = sharedSlots.get(executionSlotSharingGroup);
        if (slot != null) {
            // 共享槽位 取消相关执行顶点的 逻辑槽位请求
            // 取消的逻辑 最终调用 java 原生的 CompletableFuture 的 cancel 方法 ,就是取消相关future的对象
            slot.cancelLogicalSlotRequest(executionVertexId, cause);
        } else {
            LOG.debug(
                    "There is no SharedSlot for ExecutionSlotSharingGroup of ExecutionVertexID {}",
                    executionVertexId);
        }
    }

    // 从 SharedSlot 来分配 逻辑槽位
    private static Map<ExecutionVertexID, SlotExecutionVertexAssignment> allocateLogicalSlotsFromSharedSlots(
        Map<ExecutionSlotSharingGroup, SharedSlot> slots,
        Map<ExecutionSlotSharingGroup, List<ExecutionVertexID>> executionsByGroup) {

        Map<ExecutionVertexID, SlotExecutionVertexAssignment> assignments = new HashMap<>();

        for (Map.Entry<ExecutionSlotSharingGroup, List<ExecutionVertexID>> entry :
                executionsByGroup.entrySet()) {
            ExecutionSlotSharingGroup group = entry.getKey();
            List<ExecutionVertexID> executionIds = entry.getValue();

            for (ExecutionVertexID executionId : executionIds) {
                CompletableFuture<LogicalSlot> logicalSlotFuture =
                        // 相同 group组里面 的 ExecutionVertexID 在分配逻辑槽位的时候都利用 该组对应的SharedSlot 来分配
                        slots.get(group).allocateLogicalSlot(executionId);

                //SlotExecutionVertexAssignment 将 executionId 和 将其分配的逻辑槽位 封装在一起
                SlotExecutionVertexAssignment assignment =
                        new SlotExecutionVertexAssignment(executionId, logicalSlotFuture);
                assignments.put(executionId, assignment);
            }
        }

        return assignments;
    }

    private SharedSlot getOrAllocateSharedSlot(
            ExecutionSlotSharingGroup executionSlotSharingGroup,
            SharedSlotProfileRetriever sharedSlotProfileRetriever) {
        return sharedSlots.computeIfAbsent(
                executionSlotSharingGroup,
                group -> {
                    SlotRequestId physicalSlotRequestId = new SlotRequestId();

                    //拿到该 执行槽位共享组的 资源描述
                    ResourceProfile physicalSlotResourceProfile =
                            getPhysicalSlotResourceProfile(group);

                    SlotProfile slotProfile =
                            sharedSlotProfileRetriever.getSlotProfile(
                                    group, physicalSlotResourceProfile);

                    PhysicalSlotRequest physicalSlotRequest =
                            new PhysicalSlotRequest(
                                    physicalSlotRequestId,
                                    slotProfile,
                                    slotWillBeOccupiedIndefinitely);

                    CompletableFuture<PhysicalSlot> physicalSlotFuture =
                            slotProvider
                                    .allocatePhysicalSlot(physicalSlotRequest)
                                    .thenApply(PhysicalSlotRequest.Result::getPhysicalSlot);

                    return new SharedSlot(
                            physicalSlotRequestId,
                            physicalSlotResourceProfile,
                            group,
                            physicalSlotFuture,
                            slotWillBeOccupiedIndefinitely,
                            this::releaseSharedSlot);
                });
    }

    private void releaseSharedSlot(ExecutionSlotSharingGroup executionSlotSharingGroup) {
        SharedSlot slot = sharedSlots.remove(executionSlotSharingGroup);
        Preconditions.checkNotNull(slot);
        Preconditions.checkState(
                slot.isEmpty(),
                "Trying to remove a shared slot with physical request id %s which has assigned logical slots",
                slot.getPhysicalSlotRequestId());
        slotProvider.cancelSlotRequest(
                slot.getPhysicalSlotRequestId(),
                new FlinkException(
                        "Slot is being returned from SlotSharingExecutionSlotAllocator."));
    }

    private ResourceProfile getPhysicalSlotResourceProfile(
            ExecutionSlotSharingGroup executionSlotSharingGroup) {
        if (!executionSlotSharingGroup.getResourceProfile().equals(ResourceProfile.UNKNOWN)) {
            return executionSlotSharingGroup.getResourceProfile();
        } else {
            return executionSlotSharingGroup.getExecutionVertexIds().stream()
                    .reduce(
                            ResourceProfile.ZERO,
                            (r, e) -> r.merge(resourceProfileRetriever.apply(e)),
                            ResourceProfile::merge);
        }
    }

    private SharingPhysicalSlotRequestBulk createBulk(
            Map<ExecutionSlotSharingGroup, SharedSlot> slots,
            Map<ExecutionSlotSharingGroup, List<ExecutionVertexID>> executions) {

        // 得到每个 执行共享槽位组的真实物理资源
        Map<ExecutionSlotSharingGroup, ResourceProfile> pendingRequests =
                executions.keySet().stream()
                        .collect(
                                Collectors.toMap(
                                        group -> group,
                                        group ->
                                                slots.get(group).getPhysicalSlotResourceProfile()));
        SharingPhysicalSlotRequestBulk bulk =
                new SharingPhysicalSlotRequestBulk(
                        executions, pendingRequests, this::cancelLogicalSlotRequest);

        registerPhysicalSlotRequestBulkCallbacks(slots, executions.keySet(), bulk);
        return bulk;
    }

    private static void registerPhysicalSlotRequestBulkCallbacks(
            Map<ExecutionSlotSharingGroup, SharedSlot> slots,
            Iterable<ExecutionSlotSharingGroup> executions, //传入,为了遍历
            SharingPhysicalSlotRequestBulk bulk) {

        for (ExecutionSlotSharingGroup group : executions) {
            // 先拿到物理槽位
            CompletableFuture<PhysicalSlot> slotContextFuture =
                    slots.get(group).getSlotContextFuture();

            slotContextFuture.thenAccept(
                    // markFulfilled 是本方法的核心 ,所谓注册就是从 pending 结构中移除,而放到 filfilled 结构中
                    physicalSlot -> bulk.markFulfilled(group, physicalSlot.getAllocationId()));
            slotContextFuture.exceptionally(
                    t -> {
                        // clear the bulk to stop the fulfillability check
                        // 物理槽位申请失败, 清空 整个 pending 结构 的 执行槽位共享组
                        bulk.clearPendingRequests();
                        return null;
                    });
        }
    }
}
