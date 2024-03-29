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

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ClusterOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutor;
import org.apache.flink.runtime.jobgraph.JobType;
import org.apache.flink.runtime.jobmaster.slotpool.LocationPreferenceSlotSelectionStrategy;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotProvider;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotProviderImpl;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotRequestBulkChecker;
import org.apache.flink.runtime.jobmaster.slotpool.PhysicalSlotRequestBulkCheckerImpl;
import org.apache.flink.runtime.jobmaster.slotpool.PreviousAllocationSlotSelectionStrategy;
import org.apache.flink.runtime.jobmaster.slotpool.SlotPool;
import org.apache.flink.runtime.jobmaster.slotpool.SlotSelectionStrategy;
import org.apache.flink.runtime.scheduler.strategy.PipelinedRegionSchedulingStrategy;
import org.apache.flink.runtime.scheduler.strategy.SchedulingStrategyFactory;
import org.apache.flink.util.clock.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * Components to create a {@link DefaultScheduler}. Currently only supports {@link
 * PipelinedRegionSchedulingStrategy}.
 */
public class DefaultSchedulerComponents {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSchedulerComponents.class);

    private final SchedulingStrategyFactory schedulingStrategyFactory;
    private final Consumer<ComponentMainThreadExecutor> startUpAction;
    private final ExecutionSlotAllocatorFactory allocatorFactory;

    private DefaultSchedulerComponents(
            final SchedulingStrategyFactory schedulingStrategyFactory,
            final Consumer<ComponentMainThreadExecutor> startUpAction,
            final ExecutionSlotAllocatorFactory allocatorFactory) {

        this.schedulingStrategyFactory = schedulingStrategyFactory;
        this.startUpAction = startUpAction;
        this.allocatorFactory = allocatorFactory;
    }

    SchedulingStrategyFactory getSchedulingStrategyFactory() {
        return schedulingStrategyFactory;
    }

    Consumer<ComponentMainThreadExecutor> getStartUpAction() {
        return startUpAction;
    }

    ExecutionSlotAllocatorFactory getAllocatorFactory() {
        return allocatorFactory;
    }

    static DefaultSchedulerComponents createSchedulerComponents(
            final JobType jobType,
            final boolean isApproximateLocalRecoveryEnabled,
            final Configuration jobMasterConfiguration,
            final SlotPool slotPool,
            final Time slotRequestTimeout) {

        checkArgument(
                !isApproximateLocalRecoveryEnabled,
                "Approximate local recovery can not be used together with PipelinedRegionScheduler for now! ");
        return createPipelinedRegionSchedulerComponents(
                jobType, jobMasterConfiguration, slotPool, slotRequestTimeout);
    }

    private static DefaultSchedulerComponents createPipelinedRegionSchedulerComponents(
            final JobType jobType,
            final Configuration jobMasterConfiguration,
            final SlotPool slotPool,
            final Time slotRequestTimeout) {

        final SlotSelectionStrategy slotSelectionStrategy =
                selectSlotSelectionStrategy(jobType, jobMasterConfiguration);

        // 调用工厂方法创建PhysicalSlotRequestBulkCheckerImpl 对象, 传入 SlotPool 和 SystemClock
        // SlotPool 中有槽位资源 , SystemClock 仅仅是flink 自己封装的 单例的系统时间工具类
        final PhysicalSlotRequestBulkChecker bulkChecker =
                PhysicalSlotRequestBulkCheckerImpl.createFromSlotPool(
                        slotPool, SystemClock.getInstance());

        final PhysicalSlotProvider physicalSlotProvider =
                new PhysicalSlotProviderImpl(slotSelectionStrategy, slotPool);

        final ExecutionSlotAllocatorFactory allocatorFactory =
                new SlotSharingExecutionSlotAllocatorFactory(
                        physicalSlotProvider,
                        jobType == JobType.STREAMING,
                        bulkChecker,
                        slotRequestTimeout);
        return new DefaultSchedulerComponents(
                new PipelinedRegionSchedulingStrategy.Factory(),
                bulkChecker::start,
                allocatorFactory);
    }

    @VisibleForTesting
    static SlotSelectionStrategy selectSlotSelectionStrategy(
            final JobType jobType, final Configuration configuration) {
        final boolean evenlySpreadOutSlots =
                configuration.getBoolean(ClusterOptions.EVENLY_SPREAD_OUT_SLOTS_STRATEGY);

        final SlotSelectionStrategy locationPreferenceSlotSelectionStrategy;

        locationPreferenceSlotSelectionStrategy =
                evenlySpreadOutSlots
                        ? LocationPreferenceSlotSelectionStrategy.createEvenlySpreadOut()
                        : LocationPreferenceSlotSelectionStrategy.createDefault();

        final boolean isLocalRecoveryEnabled =
                configuration.getBoolean(CheckpointingOptions.LOCAL_RECOVERY);
        if (isLocalRecoveryEnabled) {
            if (jobType == JobType.STREAMING) {
                return PreviousAllocationSlotSelectionStrategy.create(
                        locationPreferenceSlotSelectionStrategy);
            } else {
                LOG.warn(
                        "Batch job does not support local recovery. Falling back to use "
                                + locationPreferenceSlotSelectionStrategy.getClass());
                return locationPreferenceSlotSelectionStrategy;
            }
        } else {
            return locationPreferenceSlotSelectionStrategy;
        }
    }
}
