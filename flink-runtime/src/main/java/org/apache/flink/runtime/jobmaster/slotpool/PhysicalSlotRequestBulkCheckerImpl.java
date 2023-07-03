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

package org.apache.flink.runtime.jobmaster.slotpool;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.clusterframework.types.AllocationID;
import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutor;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutor.DummyComponentMainThreadExecutor;
import org.apache.flink.runtime.jobmanager.scheduler.NoResourceAvailableException;
import org.apache.flink.runtime.jobmaster.SlotInfo;
import org.apache.flink.util.clock.Clock;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.flink.util.Preconditions.checkNotNull;

/** Default implementation of {@link PhysicalSlotRequestBulkChecker}. */
public class PhysicalSlotRequestBulkCheckerImpl implements PhysicalSlotRequestBulkChecker {

    private ComponentMainThreadExecutor componentMainThreadExecutor =
            new DummyComponentMainThreadExecutor(
                    "PhysicalSlotRequestBulkCheckerImpl is not initialized with proper main thread executor, "
                            + "call to PhysicalSlotRequestBulkCheckerImpl#start is required");

    private final Supplier<Set<SlotInfo>> slotsRetriever;

    private final Clock clock;

    PhysicalSlotRequestBulkCheckerImpl(
            final Supplier<Set<SlotInfo>> slotsRetriever, final Clock clock) {
        this.slotsRetriever = checkNotNull(slotsRetriever);
        this.clock = checkNotNull(clock);
    }

    @Override
    public void start(final ComponentMainThreadExecutor mainThreadExecutor) {
        this.componentMainThreadExecutor = mainThreadExecutor;
    }

    @Override
    public void schedulePendingRequestBulkTimeoutCheck(
            final PhysicalSlotRequestBulk bulk, Time timeout) {

        // 目前 bulk对象 一定是 SharingPhysicalSlotRequestBulk 子类的对象
        PhysicalSlotRequestBulkWithTimestamp bulkWithTimestamp =
                new PhysicalSlotRequestBulkWithTimestamp(bulk);

        bulkWithTimestamp.markUnfulfillable(clock.relativeTimeMillis());

        // 核心
        schedulePendingRequestBulkWithTimestampCheck(bulkWithTimestamp, timeout);
    }

    private void schedulePendingRequestBulkWithTimestampCheck(
            final PhysicalSlotRequestBulkWithTimestamp bulk, final Time timeout) {
        componentMainThreadExecutor.schedule(
                () -> {
                    TimeoutCheckResult result = checkPhysicalSlotRequestBulkTimeout(bulk, timeout);

                    switch (result) {
                        case PENDING:
                            // 再次调度超时检查
                            schedulePendingRequestBulkWithTimestampCheck(bulk, timeout);
                            break;
                        case TIMEOUT:
                            Throwable cancellationCause =
                                    new NoResourceAvailableException(
                                            "Slot request bulk is not fulfillable! Could not allocate the required slot within slot request timeout",
                                            new TimeoutException(
                                                    "Timeout has occurred: " + timeout));
                            // bulk对象的结构是 PhysicalSlotRequestBulkWithTimestamp 内部包了一个 SharingPhysicalSlotRequestBulk
                            // 会调用 logicalSlotRequestCanceller 来取消 pending 和fulfilled 结构中的 ((逻辑槽位请求))
                            // logicalSlotRequestCanceller 也就是 SlotSharingExecutionSlotAllocator.cancelLogicalSlotRequest方法对象
                            //
                            bulk.cancel(cancellationCause);
                            break;
                        case FULFILLED:
                        default:
                            // no action to take
                            break;
                    }
                },
                timeout.getSize(),
                timeout.getUnit());
    }

    /**
     * Check the slot request bulk and timeout its requests if it has been unfulfillable for too
     * long.
     *
     * @param slotRequestBulk bulk of slot requests
     * @param slotRequestTimeout indicates how long a pending request can be unfulfillable
     * @return result of the check, indicating the bulk is fulfilled, still pending, or timed out
     */
    //检查插槽请求批量;  如果,太长时间无法满足,则使其请求超时
    @VisibleForTesting
    TimeoutCheckResult checkPhysicalSlotRequestBulkTimeout(
            final PhysicalSlotRequestBulkWithTimestamp slotRequestBulk,//大量插槽请求
            final Time slotRequestTimeout) {//指示挂起的请求无法完成的时间长度

        // 如果slotRequestBulk 中 PendingRequests 请求 为空
        if (slotRequestBulk.getPendingRequests().isEmpty()) {
            return TimeoutCheckResult.FULFILLED;
        }

        //返回 slotsRetriever 提供的槽位资源 ,是否可以满足  slotRequestBulk 中 所有的 PendingRequests 请求
        final boolean fulfillable = isSlotRequestBulkFulfillable(slotRequestBulk, slotsRetriever);

        if (fulfillable) {
            // 将 unfulfillableTimestamp 设置为  Long.MAX_VALUE
            slotRequestBulk.markFulfillable();
        } else {
            final long currentTimestamp = clock.relativeTimeMillis();

            // 如果 unfulfillableTimestamp 现在为Long.MAX_VALUE 则重置为  currentTimestamp
            // 否则不处理 (只有初始的第一次会重置,今后都不会重置)
            slotRequestBulk.markUnfulfillable(currentTimestamp);

            // 获取原来的 unfulfillableTimestamp
            final long unfulfillableSince = slotRequestBulk.getUnfulfillableSince();
            if (unfulfillableSince + slotRequestTimeout.toMilliseconds() <= currentTimestamp) {
                // 原来的 unfulfillableTimestamp 加上 超时间隔 还是 小于 currentTimestamp ,则返回结构超时
                // (只有初始化的时候, 第一次 unfulfillableTimestamp 才会与 currentTimestamp  相等)
                return TimeoutCheckResult.TIMEOUT;
            }
        }

        return TimeoutCheckResult.PENDING;
    }

    /**
     * Returns whether the given bulk of slot requests are possible to be fulfilled at the same time
     * with all the reusable slots in the slot pool. A reusable slot means the slot is available or
     * will not be occupied indefinitely.
     *
     * @param slotRequestBulk bulk of slot requests to check
     * @param slotsRetriever supplies slots to be used for the fulfill-ability check
     * @return true if the slot requests are possible to be fulfilled, otherwise false
     */
    //返回 slotsRetriever 提供的槽位资源 ,是否可以满足  slotRequestBulk 中 所有的 PendingRequests 请求
    @VisibleForTesting
    static boolean isSlotRequestBulkFulfillable(
            final PhysicalSlotRequestBulk slotRequestBulk,
            final Supplier<Set<SlotInfo>> slotsRetriever) {

        // 拿到已经分配成功的物理槽位
        final Set<AllocationID> assignedSlots =
                slotRequestBulk.getAllocationIdsOfFulfilledRequests();

        final Set<SlotInfo> reusableSlots = getReusableSlots(slotsRetriever, assignedSlots);

        // reusableSlots 能够满足所有的   物理槽位请求吗
        return areRequestsFulfillableWithSlots(slotRequestBulk.getPendingRequests(), reusableSlots);
    }

    private static Set<SlotInfo> getReusableSlots(
            final Supplier<Set<SlotInfo>> slotsRetriever, final Set<AllocationID> slotsToExclude) {

        return slotsRetriever.get().stream()
                // 除去被无限期占用的
                .filter(slotInfo -> !slotInfo.willBeOccupiedIndefinitely())
                // 除去已经被分配的
                .filter(slotInfo -> !slotsToExclude.contains(slotInfo.getAllocationId()))
                .collect(Collectors.toSet());
    }

    /**
     * Tries to match pending requests to all registered slots (available or allocated).
     *
     * <p>NOTE: The complexity of the method is currently quadratic (number of pending requests x
     * number of all slots).
     */
    private static boolean areRequestsFulfillableWithSlots(
            final Collection<ResourceProfile> requestResourceProfiles, final Set<SlotInfo> slots) {

        // copy 一份 slots
        final Set<SlotInfo> remainingSlots = new HashSet<>(slots);

        for (ResourceProfile requestResourceProfile : requestResourceProfiles) {

            final Optional<SlotInfo> matchedSlot =
                    // 依次找到每个符合 请求要求（资源一致） 的 SlotInfo
                    findMatchingSlotForRequest(requestResourceProfile, remainingSlots);
            if (matchedSlot.isPresent()) {
                    // 将符合要求的 SlotInfo 从 remainingSlots 中移除
                remainingSlots.remove(matchedSlot.get());
            } else {
                // 必须所有的 请求都能找到符合要求的 SlotInfo ,该方法才能返回true
                // 又一个不满足都得直接返回 false
                return false;
            }
        }
        return true;
    }

    private static Optional<SlotInfo> findMatchingSlotForRequest(
            final ResourceProfile requestResourceProfile, final Collection<SlotInfo> slots) {

        return slots.stream()
                .filter(slot -> slot.getResourceProfile().isMatching(requestResourceProfile))// 匹配就是资源符合要求
                .findFirst();
    }

    // 本类的静态工厂方法，
    public static PhysicalSlotRequestBulkCheckerImpl createFromSlotPool(
            final SlotPool slotPool, final Clock clock) {
        return new PhysicalSlotRequestBulkCheckerImpl(() -> getAllSlotInfos(slotPool), clock);
    }

    private static Set<SlotInfo> getAllSlotInfos(SlotPool slotPool) {
        return Stream.concat(
                        slotPool.getAvailableSlotsInformation().stream(),
                        slotPool.getAllocatedSlotsInformation().stream())
                .collect(Collectors.toSet());
    }

    enum TimeoutCheckResult {
        PENDING,

        FULFILLED,

        TIMEOUT
    }
}
