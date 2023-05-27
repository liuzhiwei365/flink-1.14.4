/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.resourcemanager.slotmanager;

import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.instance.InstanceID;
import org.apache.flink.util.Preconditions;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link SlotMatchingStrategy} which picks a matching slot from a TaskExecutor with the least
 * utilization.
 */

// 找到所有匹配slot中所在TM资源使用率最低的返回
public enum LeastUtilizationSlotMatchingStrategy implements SlotMatchingStrategy {
    INSTANCE;

    @Override
    public <T extends TaskManagerSlotInformation> Optional<T> findMatchingSlot(
            ResourceProfile requestedProfile,
            Collection<T> freeSlots, // 泛形是 TaskManagerSlotInformation
            Function<InstanceID, Integer>
                    numberRegisteredSlotsLookup) { // DeclarativeSlotManager::getNumberRegisteredSlotsOf

        // InstanceID 这里可以认为是 TaskManager ID , Integer 可以认为是 每个 TaskManager ID 的 空闲slot 数
        final Map<InstanceID, Integer> numSlotsPerTaskExecutor =
                freeSlots.stream()
                        .collect(
                                Collectors.groupingBy(
                                        TaskManagerSlotInformation::getInstanceId,
                                        Collectors.reducing(0, i -> 1, Integer::sum)));

        return freeSlots.stream()
                // 先过滤出满足要求的 TaskManagerSlotInformation
                .filter(taskManagerSlot -> taskManagerSlot.isMatchingRequirement(requestedProfile))
                // 再拿到资源使用率最小的 TaskManagerSlotInformation 对象
                .min(
                        Comparator.comparingDouble(
                                taskManagerSlot ->
                                        calculateUtilization(
                                                taskManagerSlot.getInstanceId(),
                                                numberRegisteredSlotsLookup,
                                                numSlotsPerTaskExecutor)));
    }

    // 计算每个TaskManager 的slot 使用率
    private static double calculateUtilization(
            InstanceID instanceId,
            Function<? super InstanceID, Integer> numberRegisteredSlotsLookup,
            Map<InstanceID, Integer> numSlotsPerTaskExecutor) {

        final int numberRegisteredSlots = numberRegisteredSlotsLookup.apply(instanceId);

        Preconditions.checkArgument(
                numberRegisteredSlots > 0,
                "The TaskExecutor %s has no slots registered.",
                instanceId);

        final int numberFreeSlots = numSlotsPerTaskExecutor.getOrDefault(instanceId, 0);

        Preconditions.checkArgument(
                numberRegisteredSlots >= numberFreeSlots,
                "The TaskExecutor %s has fewer registered slots than free slots.",
                instanceId);

        /*
            = （所有注册的slot  - 所有空闲的slot ）/ 所有注册的slot
            =  正在使用的slot / 所有注册的slot
            =  slot 使用率
        */
        return (double) (numberRegisteredSlots - numberFreeSlots) / numberRegisteredSlots;
    }
}
