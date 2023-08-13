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

package org.apache.flink.runtime.scheduler.adaptive.allocator;

import org.apache.flink.runtime.clusterframework.types.AllocationID;
import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.instance.SlotSharingGroupId;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobmanager.scheduler.SlotSharingGroup;
import org.apache.flink.runtime.jobmaster.LogicalSlot;
import org.apache.flink.runtime.jobmaster.SlotInfo;
import org.apache.flink.runtime.jobmaster.SlotRequestId;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlot;
import org.apache.flink.runtime.scheduler.strategy.ExecutionVertexID;
import org.apache.flink.runtime.util.ResourceCounter;
import org.apache.flink.util.Preconditions;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** {@link SlotAllocator} implementation that supports slot sharing. */
public class SlotSharingSlotAllocator implements SlotAllocator {

    private final ReserveSlotFunction reserveSlotFunction;
    private final FreeSlotFunction freeSlotFunction;
    private final IsSlotAvailableAndFreeFunction isSlotAvailableAndFreeFunction;

    private SlotSharingSlotAllocator(
            ReserveSlotFunction reserveSlot,
            FreeSlotFunction freeSlotFunction,
            IsSlotAvailableAndFreeFunction isSlotAvailableAndFreeFunction) {
        this.reserveSlotFunction = reserveSlot;
        this.freeSlotFunction = freeSlotFunction;
        this.isSlotAvailableAndFreeFunction = isSlotAvailableAndFreeFunction;
    }

    public static SlotSharingSlotAllocator createSlotSharingSlotAllocator(
            ReserveSlotFunction reserveSlot,
            FreeSlotFunction freeSlotFunction,
            IsSlotAvailableAndFreeFunction isSlotAvailableAndFreeFunction) {
        return new SlotSharingSlotAllocator(
                reserveSlot, freeSlotFunction, isSlotAvailableAndFreeFunction);
    }

    // 计算需要的槽位总数
    @Override
    public ResourceCounter calculateRequiredSlots(
            Iterable<JobInformation.VertexInformation> vertices) {
        int numTotalRequiredSlots = 0;
        //  getMaxParallelismForSlotSharingGroups 是核心
        for (Integer requiredSlots : getMaxParallelismForSlotSharingGroups(vertices).values()) {
            numTotalRequiredSlots += requiredSlots;
        }
        return ResourceCounter.withResource(ResourceProfile.UNKNOWN, numTotalRequiredSlots);
    }

    private static Map<SlotSharingGroupId, Integer> getMaxParallelismForSlotSharingGroups(
            Iterable<JobInformation.VertexInformation> vertices) {
        //  保存 每个 共享槽位组 对应 的 最大并行度
        //  由于存在 多个算子链 共享槽位的情况,   也就是说多个算子链 如果看成一个整体的化,它们共同需要 "最大并行度的算子链 的并行度" 个槽位
        //  就可以完美覆盖槽位需求  (说成算子链共享槽位 , 其实比 说成算子共享槽位 更加准确 )
        final Map<SlotSharingGroupId, Integer> maxParallelismForSlotSharingGroups = new HashMap<>();

        for (JobInformation.VertexInformation vertex : vertices) {
            maxParallelismForSlotSharingGroups.compute(
                    vertex.getSlotSharingGroup().getSlotSharingGroupId(),
                    (slotSharingGroupId, currentMaxParallelism) ->
                            currentMaxParallelism == null
                                    ? vertex.getParallelism()
                                    : Math.max(currentMaxParallelism, vertex.getParallelism()));
        }
        return maxParallelismForSlotSharingGroups;
    }

    // 返回槽位分配结果  和  每个JobVertex 的并行度决定结果
    @Override
    public Optional<VertexParallelismWithSlotSharing> determineParallelism(
            JobInformation jobInformation, Collection<? extends SlotInfo> freeSlots) {
        // 如果插槽共享组的最大并行度不相等, 这可能会浪费插槽

        // 槽位个数 / 共享组个数
        final int slotsPerSlotSharingGroup =
                freeSlots.size() / jobInformation.getSlotSharingGroups().size();

        if (slotsPerSlotSharingGroup == 0) {
            // => less slots than slot-sharing groups
            return Optional.empty();
        }

        final Iterator<? extends SlotInfo> slotIterator = freeSlots.iterator();


        // 代表了整个作业分配的所有槽位（算法的结果1 ）
        final Collection<ExecutionSlotSharingGroupAndSlot> assignments = new ArrayList<>();
        // 维护了每个JobVertexID 分配的 subTask 个数 （算法的结果2 ）
        final Map<JobVertexID, Integer> allVertexParallelism = new HashMap<>();


        //遍历job 全局的槽位共享组
        for (SlotSharingGroup slotSharingGroup : jobInformation.getSlotSharingGroups()) {
            // 指定共享组中所有的 VertexInformation
            final List<JobInformation.VertexInformation> containedJobVertices =
                    slotSharingGroup.getJobVertexIds().stream()
                            .map(jobInformation::getVertexInformation)// JobVertexID -> VertexInformation
                            .collect(Collectors.toList());

            // step1：
            // 用 Math.min(jobVertex.getParallelism(), slotsPerSlotSharingGroup) 来更新 JobVertx 的 并行度设置
            // ** 如果用户配置的并行度过高,现有的槽位 不足以满足，则按照现有的算法来 决定并行度
            final Map<JobVertexID, Integer> vertexParallelism =
                    determineParallelism(containedJobVertices, slotsPerSlotSharingGroup);

            // step2：
            // 返回的迭代器元素的个数 对应 "指定的槽位共享组" 分配的槽位个数
            final Iterable<ExecutionSlotSharingGroup> sharedSlotToVertexAssignment =
                                                                  createExecutionSlotSharingGroups(vertexParallelism);

            for (ExecutionSlotSharingGroup executionSlotSharingGroup : sharedSlotToVertexAssignment) {
                // 取出供给的一个槽位 来分配给 executionSlotSharingGroup
                final SlotInfo slotInfo = slotIterator.next();

                assignments.add(
                        new ExecutionSlotSharingGroupAndSlot(executionSlotSharingGroup, slotInfo));
            }
            allVertexParallelism.putAll(vertexParallelism);
        }

        /*

          A0->B0->C0         D0->E0->F0->G0         H0->J0           Slot0

          A1->B1->C1         D0->E0->F0->G0         H1->J2           Slot1

          A2->B2->C2         D0->E0->F0->G0                          Slot2

          A3->B3->C3                                                 Slot3

          如果 A、B、C、D、E、F、G、H、J  是共享槽位共享组
          且 A、B、C 的算子并行度为 4
             D、E、F、G 的算子并行度 为 3
             H、J算子的并行度 为 2
          则, 总共分配4 个槽位 即可满足需求

          而 assignments 中元素的个数 = 4 + 3 + 2 = 9 个

     */
        return Optional.of(new VertexParallelismWithSlotSharing(allVertexParallelism, assignments));
    }

    private static Map<JobVertexID, Integer> determineParallelism(
            Collection<JobInformation.VertexInformation> containedJobVertices, int availableSlots) {

        final Map<JobVertexID, Integer> vertexParallelism = new HashMap<>();
        for (JobInformation.VertexInformation jobVertex : containedJobVertices) {

            // 如果用户配置的并行度过高,现有的槽位 不足以满足，则按照现有的算法来 决定并行度
            // 否则，就按照用户
            final int parallelism = Math.min(jobVertex.getParallelism(), availableSlots);

            vertexParallelism.put(jobVertex.getJobVertexID(), parallelism);
        }

        return vertexParallelism;
    }

    /*

          A0->B0->C0         D0->E0->F0->G0         H0->J0           Slot0

          A1->B1->C1         D0->E0->F0->G0         H1->J2           Slot1

          A2->B2->C2         D0->E0->F0->G0                          Slot2

          A3->B3->C3                                                 Slot3

          如果 A、B、C、D、E、F、G、H、J  是共享槽位共享组
          且 A、B、C 的算子并行度为 4
             D、E、F、G 的算子并行度 为 3
             H、J算子的并行度 为 2
          则, 总共分配4 个槽位 即可满足需求

          所以本方法返回的迭代器,元素也必定是 4
     */
    // 本方法针对的是 单个的 槽位共享组
    // 1 所以 算法将同一个 subTaskIndex 编号的 ExecutionVertexID 放入到一个Set中, 因为它们将来会运行在同一个槽位中
    // 2 这样一来,本算法返回的迭代器中元素个数 必定等于 槽位数
    // 3 迭代器每个元素 内部含有 Set<ExecutionVertexID> 集合
    private static Iterable<ExecutionSlotSharingGroup> createExecutionSlotSharingGroups(
            Map<JobVertexID, Integer> containedJobVertices) {

        final Map<Integer, Set<ExecutionVertexID>> sharedSlotToVertexAssignment = new HashMap<>();

        //遍历JobVertex
        for (Map.Entry<JobVertexID, Integer> jobVertex : containedJobVertices.entrySet()) {
            // 遍历 JobVertex下所有的 subTaskIndex
            for (int i = 0; i < jobVertex.getValue(); i++) {
                sharedSlotToVertexAssignment
                        .computeIfAbsent(i, ignored -> new HashSet<>())
                         // ExecutionVertexID(jobVertexId, subtaskIndex)
                        .add(new ExecutionVertexID(jobVertex.getKey(), i));
            }
        }

        return sharedSlotToVertexAssignment.values().stream()
                .map(ExecutionSlotSharingGroup::new)
                .collect(Collectors.toList());
    }

    // 返回 ReservedSlots ,内部持有一个 ExecutionVertexID 到 LogicalSlot 分配的逻辑槽位的映射
    @Override
    public Optional<ReservedSlots> tryReserveResources(VertexParallelism vertexParallelism) {
        Preconditions.checkArgument(
                vertexParallelism instanceof VertexParallelismWithSlotSharing,
                String.format(
                        "%s expects %s as argument.",
                        SlotSharingSlotAllocator.class.getSimpleName(),
                        VertexParallelismWithSlotSharing.class.getSimpleName()));

        final VertexParallelismWithSlotSharing vertexParallelismWithSlotSharing =
                (VertexParallelismWithSlotSharing) vertexParallelism;

        final Collection<AllocationID> expectedSlots =
                calculateExpectedSlots(vertexParallelismWithSlotSharing.getAssignments());

        // 校验 vertexParallelismWithSlotSharing 中分配的槽位都是可获取 和 闲置的
        if (areAllExpectedSlotsAvailableAndFree(expectedSlots)) {

            final Map<ExecutionVertexID, LogicalSlot> assignedSlots = new HashMap<>();

            // 遍历所有的槽位
            for (ExecutionSlotSharingGroupAndSlot executionSlotSharingGroup :
                                                          vertexParallelismWithSlotSharing.getAssignments()) {

                // 把槽位信息 解析成 SharedSlot
                final SharedSlot sharedSlot =
                        reserveSharedSlot(executionSlotSharingGroup.getSlotInfo());

                // 遍历 即将运行在同一个槽位的 执行顶点
                for (ExecutionVertexID executionVertexId :
                        executionSlotSharingGroup
                                .getExecutionSlotSharingGroup()
                                .getContainedExecutionVertices()) {
                    final LogicalSlot logicalSlot = sharedSlot.allocateLogicalSlot();
                    assignedSlots.put(executionVertexId, logicalSlot);
                }
            }

            // 用 ReservedSlots 简单包装一个 assignedSlots map
            return Optional.of(ReservedSlots.create(assignedSlots));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    private Collection<AllocationID> calculateExpectedSlots(
            Iterable<? extends ExecutionSlotSharingGroupAndSlot> assignments) {
        final Collection<AllocationID> requiredSlots = new ArrayList<>();

        for (ExecutionSlotSharingGroupAndSlot assignment : assignments) {
            requiredSlots.add(assignment.getSlotInfo().getAllocationId());
        }
        return requiredSlots;
    }

    private boolean areAllExpectedSlotsAvailableAndFree(
            Iterable<? extends AllocationID> requiredSlots) {
        for (AllocationID requiredSlot : requiredSlots) {
            if (!isSlotAvailableAndFreeFunction.isSlotAvailableAndFree(requiredSlot)) {
                return false;
            }
        }

        return true;
    }

    private SharedSlot reserveSharedSlot(SlotInfo slotInfo) {
        final PhysicalSlot physicalSlot =
                reserveSlotFunction.reserveSlot(
                        slotInfo.getAllocationId(), ResourceProfile.UNKNOWN);

        return new SharedSlot(
                new SlotRequestId(),
                physicalSlot,
                slotInfo.willBeOccupiedIndefinitely(),
                () ->
                        freeSlotFunction.freeSlot(
                                slotInfo.getAllocationId(), null, System.currentTimeMillis()));
    }

    static class ExecutionSlotSharingGroup {
        private final Set<ExecutionVertexID> containedExecutionVertices;

        public ExecutionSlotSharingGroup(Set<ExecutionVertexID> containedExecutionVertices) {
            this.containedExecutionVertices = containedExecutionVertices;
        }

        public Collection<ExecutionVertexID> getContainedExecutionVertices() {
            return containedExecutionVertices;
        }
    }

    static class ExecutionSlotSharingGroupAndSlot {
        private final ExecutionSlotSharingGroup executionSlotSharingGroup;
        private final SlotInfo slotInfo;

        public ExecutionSlotSharingGroupAndSlot(
                ExecutionSlotSharingGroup executionSlotSharingGroup, SlotInfo slotInfo) {
            this.executionSlotSharingGroup = executionSlotSharingGroup;
            this.slotInfo = slotInfo;
        }

        public ExecutionSlotSharingGroup getExecutionSlotSharingGroup() {
            return executionSlotSharingGroup;
        }

        public SlotInfo getSlotInfo() {
            return slotInfo;
        }
    }
}
