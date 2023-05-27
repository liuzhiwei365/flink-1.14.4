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
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.OperatorStateHandle;
import org.apache.flink.runtime.state.OperatorStreamStateHandle;
import org.apache.flink.runtime.state.StreamStateHandle;
import org.apache.flink.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Current default implementation of {@link OperatorStateRepartitioner} that redistributes state in
 * round robin fashion.
 */
@Internal
public class RoundRobinOperatorStateRepartitioner
        implements OperatorStateRepartitioner<OperatorStateHandle> {

    public static final OperatorStateRepartitioner<OperatorStateHandle> INSTANCE =
            new RoundRobinOperatorStateRepartitioner();
    private static final boolean OPTIMIZE_MEMORY_USE = false;

    @Override
    // 别忘了,这是针对对单个的 operator id的
    public List<List<OperatorStateHandle>> repartitionState(
            List<List<OperatorStateHandle>> previousParallelSubtaskStates,
            int oldParallelism,
            int newParallelism) {

        Preconditions.checkNotNull(previousParallelSubtaskStates);
        Preconditions.checkArgument(newParallelism > 0);
        Preconditions.checkArgument(
                previousParallelSubtaskStates.size() == oldParallelism,
                "This method still depends on the order of the new and old operators");

        // Assemble result from all merge maps
        List<List<OperatorStateHandle>> result = new ArrayList<>(newParallelism);

        List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList;

        // We only round-robin repartition UNION state if new parallelism equals to the old one.
        if (newParallelism == oldParallelism) {
            // 找出所有 Union 类型的 State
            Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    unionStates = collectUnionStates(previousParallelSubtaskStates);
            // 找出所有 Broadcast 类型的 State
            Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    partlyFinishedBroadcastStates =
                            collectPartlyFinishedBroadcastStates(previousParallelSubtaskStates);

            // unionStates 和BroadcastStates为空，表示都是 SPLIT_DISTRIBUTE 模式，直接按照老的 State 进行分配,不用进行状态重组
            if (unionStates.isEmpty() && partlyFinishedBroadcastStates.isEmpty()) {
                return previousParallelSubtaskStates;
            }

            // Initialize
            mergeMapList = initMergeMapList(previousParallelSubtaskStates);

            // 单独处理UnionState
            repartitionUnionState(unionStates, mergeMapList);

            // 单独处理BroadcastState
            repartitionBroadcastState(partlyFinishedBroadcastStates, mergeMapList);

        } else {
            // 当新旧并行度不一样的时候

            // GroupByStateNameResults 可以看成是不同并行度情况下的中间适配
            GroupByStateNameResults nameToStateByMode =
                    groupByStateMode(previousParallelSubtaskStates);

            // 集合gc 回收
            if (OPTIMIZE_MEMORY_USE) {
                previousParallelSubtaskStates.clear();
            }

            // 拿着中间适配的结构 GroupByStateNameResults的对象,来做真正的状态重新分区逻辑
            mergeMapList = repartition(nameToStateByMode, newParallelism);
        }

        //   List<List<OperatorStateHandle>> result = new ArrayList<>(newParallelism);
        //   List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList;
        //   mergeMapList 是比 result 更加精确的结构

        for (int i = 0; i < mergeMapList.size(); ++i) {
            result.add(i, new ArrayList<>(mergeMapList.get(i).values()));
        }

        return result;
    }

    /**
     * Init the the list of StreamStateHandle -> OperatorStateHandle map with given
     * parallelSubtaskStates when parallelism not changed.
     */
    private List<Map<StreamStateHandle, OperatorStateHandle>> initMergeMapList(
            List<List<OperatorStateHandle>> parallelSubtaskStates) {

        int parallelism = parallelSubtaskStates.size();

        final List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList =
                new ArrayList<>(parallelism);

        for (List<OperatorStateHandle> previousParallelSubtaskState : parallelSubtaskStates) {
            mergeMapList.add(
                    previousParallelSubtaskState.stream()
                            .collect(
                                    Collectors.toMap(
                                            OperatorStateHandle::getDelegateStateHandle,
                                            Function.identity())));
        }

        return mergeMapList;
    }

    private Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
            collectUnionStates(List<List<OperatorStateHandle>> parallelSubtaskStates) {
        return collectStates(parallelSubtaskStates, OperatorStateHandle.Mode.UNION).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().entries));
    }

    private Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
            collectPartlyFinishedBroadcastStates(
                    List<List<OperatorStateHandle>> parallelSubtaskStates) {
        return collectStates(parallelSubtaskStates, OperatorStateHandle.Mode.BROADCAST).entrySet()
                .stream()
                .filter(e -> e.getValue().isPartiallyReported())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().entries));
    }

    /** Collect the states from given parallelSubtaskStates with the specific {@code mode}. */
    private Map<String, StateEntry> collectStates(
            List<List<OperatorStateHandle>> parallelSubtaskStates, OperatorStateHandle.Mode mode) {

        Map<String, StateEntry> states = new HashMap<>(parallelSubtaskStates.size());

        for (int i = 0; i < parallelSubtaskStates.size(); ++i) {
            final int subtaskIndex = i;

            List<OperatorStateHandle> subTaskState = parallelSubtaskStates.get(i);

            for (OperatorStateHandle operatorStateHandle : subTaskState) {
                if (operatorStateHandle == null) {
                    continue;
                }

                final Set<Map.Entry<String, OperatorStateHandle.StateMetaInfo>>
                        partitionOffsetEntries =
                                operatorStateHandle.getStateNameToPartitionOffsets().entrySet();

                partitionOffsetEntries.stream()
                        .filter(
                                entry ->
                                        entry.getValue()
                                                .getDistributionMode()
                                                .equals(mode)) // 过滤出想要的模式
                        .forEach(
                                entry -> {
                                    StateEntry stateEntry =
                                            states.computeIfAbsent(
                                                    entry.getKey(), // 状态名
                                                    k ->
                                                            new StateEntry(
                                                                    parallelSubtaskStates
                                                                                    .size() // 旧的并行度
                                                                            * partitionOffsetEntries
                                                                                    .size(), // 每个OperatorStateHandle 分区偏移量的条目数
                                                                    parallelSubtaskStates.size()));
                                    stateEntry.addEntry(
                                            subtaskIndex,
                                            Tuple2.of(
                                                    operatorStateHandle.getDelegateStateHandle(),
                                                    entry.getValue())); // value的类型为
                                    // OperatorStateHandle.StateMetaInfo
                                });
            }
        }

        return states;
    }

    /** Group by the different named states. */
    @SuppressWarnings("unchecked, rawtype")
    private GroupByStateNameResults groupByStateMode(
            List<List<OperatorStateHandle>> previousParallelSubtaskStates) {

        // 存储方法结果
        EnumMap<
                        OperatorStateHandle.Mode,
                        Map<
                                String,
                                List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>>
                nameToStateByMode = new EnumMap<>(OperatorStateHandle.Mode.class);

        // 初始化
        for (OperatorStateHandle.Mode mode : OperatorStateHandle.Mode.values()) {
            nameToStateByMode.put(mode, new HashMap<>());
        }

        // List<List<OperatorStateHandle>>
        for (List<OperatorStateHandle> previousParallelSubtaskState :
                previousParallelSubtaskStates) { // 算子旧并行度

            for (OperatorStateHandle operatorStateHandle :
                    previousParallelSubtaskState) { // 算子状态句柄( 该for循环肯定只 遍历一次;
                // previousParallelSubtaskState的容量一定是一)

                if (operatorStateHandle == null) {
                    continue;
                }

                final Set<Map.Entry<String, OperatorStateHandle.StateMetaInfo>>
                        partitionOffsetEntries =
                                operatorStateHandle.getStateNameToPartitionOffsets().entrySet();

                // 泛型中的String代表单个状态的名字 ,StateMetaInfo主要维护偏移量;因为多个状态存储的在一起,必须通过偏移量切分
                for (Map.Entry<String, OperatorStateHandle.StateMetaInfo> e :
                        partitionOffsetEntries) { // 单个状态 ( 一个算子状态句柄可能有多个状态,用户可以写多个)

                    // 3 层 循环 遍历 单个状态

                    OperatorStateHandle.StateMetaInfo metaInfo = e.getValue();

                    // nameToState的总容量为  (旧并行度 * 状态的个数 * 状态的个数)
                    Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                            nameToState = nameToStateByMode.get(metaInfo.getDistributionMode());

                    List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>
                            stateLocations =
                                    nameToState.computeIfAbsent( // get  方法的加强版
                                            e.getKey(), // 单个状态的名字

                                            // 1 该ArrayList的容量为什么这样设置得好好思考一下, 因为三层for循环的第二层一定
                                            // 的遍历次数一定是一
                                            // 2 对于每个name来说 , 一般情况下容量使用都不会超过并行实例数;
                                            // 但这里再乘以状态个数;猜想是为了解决union类型的状态合并
                                            k ->
                                                    new ArrayList<>(
                                                            previousParallelSubtaskStates
                                                                            .size() // 旧并行度
                                                                    * partitionOffsetEntries
                                                                            .size())); // 单个并行实例中
                    // 状态的个数

                    stateLocations.add(
                            Tuple2.of(operatorStateHandle.getDelegateStateHandle(), e.getValue()));
                }
            }
        }

        return new GroupByStateNameResults(nameToStateByMode);
    }

    /** Repartition all named states. */
    // 该方法是针对单个 operator id 的
    private List<Map<StreamStateHandle, OperatorStateHandle>> repartition(
            GroupByStateNameResults nameToStateByMode, int newParallelism) {

        // 用来存储方法返回结果
        List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList =
                new ArrayList<>(newParallelism);

        // 初始化结构对象,放入一些空的 map
        for (int i = 0; i < newParallelism; ++i) {
            mergeMapList.add(new HashMap<>());
        }

        // 处理 SPLIT_DISTRIBUTE 类型
        Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                nameToDistributeState =
                        nameToStateByMode.getByMode(OperatorStateHandle.Mode.SPLIT_DISTRIBUTE);

        repartitionSplitState(nameToDistributeState, newParallelism, mergeMapList);

        // 处理 UNION 类型
        Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                nameToUnionState = nameToStateByMode.getByMode(OperatorStateHandle.Mode.UNION);

        repartitionUnionState(nameToUnionState, mergeMapList);

        // 处理 BROADCAST 类型
        Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                nameToBroadcastState =
                        nameToStateByMode.getByMode(OperatorStateHandle.Mode.BROADCAST);

        repartitionBroadcastState(nameToBroadcastState, mergeMapList);

        return mergeMapList;
    }

    /** Repartition SPLIT_DISTRIBUTE state. */
    private void repartitionSplitState(
            Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    nameToDistributeState,
            int newParallelism,
            List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList) {

        int startParallelOp = 0;
        // Iterate all named states and repartition one named state at a time per iteration
        for (Map.Entry<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                e : nameToDistributeState.entrySet()) { // 针对任一 name

            List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>> current = // 旧并行度
                    e.getValue();

            // Determine actual number of partitions for this named state
            // 举个例子,有可能10个状态分片  散落在 3 个并行度中 ,现在并行度扩展到 4
            int totalPartitions = 0;
            for (Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo> offsets : current) {
                totalPartitions += offsets.f1.getOffsets().length;
            }

            // Repartition the state across the parallel operator instances
            int lstIdx = 0;
            int offsetIdx = 0;
            int baseFraction = totalPartitions / newParallelism;
            int remainder = totalPartitions % newParallelism;

            int newStartParallelOp = startParallelOp;

            for (int i = 0; i < newParallelism; ++i) {

                // Preparation: calculate the actual index considering wrap around
                int parallelOpIdx = (i + startParallelOp) % newParallelism;

                // Now calculate the number of partitions we will assign to the parallel instance in
                // this round ...
                int numberOfPartitionsToAssign = baseFraction;

                // ... and distribute odd partitions while we still have some, one at a time
                if (remainder > 0) {
                    ++numberOfPartitionsToAssign;
                    --remainder;
                } else if (remainder == 0) {
                    // We are out of odd partitions now and begin our next redistribution round with
                    // the current
                    // parallel operator to ensure fair load balance
                    newStartParallelOp = parallelOpIdx;
                    --remainder;
                }

                // Now start collection the partitions for the parallel instance into this list

                while (numberOfPartitionsToAssign > 0) {
                    Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo> handleWithOffsets =
                            current.get(lstIdx);

                    long[] offsets = handleWithOffsets.f1.getOffsets();
                    int remaining = offsets.length - offsetIdx;
                    // Repartition offsets
                    long[] offs;
                    if (remaining > numberOfPartitionsToAssign) {
                        offs =
                                Arrays.copyOfRange(
                                        offsets, offsetIdx, offsetIdx + numberOfPartitionsToAssign);
                        offsetIdx += numberOfPartitionsToAssign;
                    } else {
                        if (OPTIMIZE_MEMORY_USE) {
                            handleWithOffsets.f1 = null; // GC
                        }
                        offs = Arrays.copyOfRange(offsets, offsetIdx, offsets.length);
                        offsetIdx = 0;
                        ++lstIdx;
                    }

                    numberOfPartitionsToAssign -= remaining;

                    // As a last step we merge partitions that use the same StreamStateHandle in a
                    // single
                    // OperatorStateHandle
                    Map<StreamStateHandle, OperatorStateHandle> mergeMap =
                            mergeMapList.get(parallelOpIdx);
                    OperatorStateHandle operatorStateHandle = mergeMap.get(handleWithOffsets.f0);
                    if (operatorStateHandle == null) {
                        operatorStateHandle =
                                new OperatorStreamStateHandle(
                                        new HashMap<>(nameToDistributeState.size()),
                                        handleWithOffsets.f0);
                        mergeMap.put(handleWithOffsets.f0, operatorStateHandle);
                    }
                    operatorStateHandle
                            .getStateNameToPartitionOffsets()
                            .put(
                                    e.getKey(),
                                    new OperatorStateHandle.StateMetaInfo(
                                            offs, OperatorStateHandle.Mode.SPLIT_DISTRIBUTE));
                }
            }
            startParallelOp = newStartParallelOp;
            e.setValue(null);
        }
    }

    /** Repartition UNION state. */
    private void repartitionUnionState(
            Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    unionState,
            List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList) {

        for (Map<StreamStateHandle, OperatorStateHandle> mergeMap : mergeMapList) { // 新并行度
            for (Map.Entry<
                            String,
                            List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    e : unionState.entrySet()) { // 所有union类型的 状态个数

                for (Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>
                        handleWithMetaInfo : e.getValue()) { // 旧并行度

                    // 针对单个name,将旧的并行度中每一个分区的信息 都全部汇总  给新的任一分区(新的并行度)中
                    OperatorStateHandle operatorStateHandle = mergeMap.get(handleWithMetaInfo.f0);
                    if (operatorStateHandle == null) {
                        operatorStateHandle =
                                new OperatorStreamStateHandle(
                                        new HashMap<>(unionState.size()), handleWithMetaInfo.f0);
                        mergeMap.put(handleWithMetaInfo.f0, operatorStateHandle);
                    }
                    operatorStateHandle
                            .getStateNameToPartitionOffsets()
                            .put(e.getKey(), handleWithMetaInfo.f1);
                }
            }
        }
    }

    /** Repartition BROADCAST state. */
    private void repartitionBroadcastState(
            Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    broadcastState,
            List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList) {

        int newParallelism = mergeMapList.size();
        for (int i = 0; i < newParallelism; ++i) {

            final Map<StreamStateHandle, OperatorStateHandle> mergeMap = mergeMapList.get(i);

            // for each name, pick the i-th entry
            for (Map.Entry<
                            String,
                            List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    e : broadcastState.entrySet()) { // 旧的分区信息

                int previousParallelism = e.getValue().size(); // 旧并行度

                Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo> handleWithMetaInfo =
                        e.getValue().get(i % previousParallelism); // 不用取模也可以,随便拿一份就行 ;在子任务中获取一份旧的信息

                // 把旧的信息重组,再分配给新的
                OperatorStateHandle operatorStateHandle = mergeMap.get(handleWithMetaInfo.f0);
                if (operatorStateHandle == null) {
                    operatorStateHandle =
                            new OperatorStreamStateHandle(
                                    new HashMap<>(broadcastState.size()), handleWithMetaInfo.f0);

                    mergeMap.put(handleWithMetaInfo.f0, operatorStateHandle);
                }
                operatorStateHandle
                        .getStateNameToPartitionOffsets()
                        .put(e.getKey(), handleWithMetaInfo.f1);
            }
        }
    }

    private static final class GroupByStateNameResults {
        private final EnumMap<
                        OperatorStateHandle.Mode,
                        Map<
                                String,
                                List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>>
                byMode;

        GroupByStateNameResults(
                EnumMap<
                                OperatorStateHandle.Mode,
                                Map<
                                        String,
                                        List<
                                                Tuple2<
                                                        StreamStateHandle,
                                                        OperatorStateHandle.StateMetaInfo>>>>
                        byMode) {
            this.byMode = Preconditions.checkNotNull(byMode);
        }

        public Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                getByMode(OperatorStateHandle.Mode mode) {
            return byMode.get(mode);
        }
    }

    private static final class StateEntry {
        final List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>> entries;
        final BitSet reportedSubtaskIndices;

        public StateEntry(int estimatedEntrySize, int parallelism) {
            this.entries = new ArrayList<>(estimatedEntrySize);
            this.reportedSubtaskIndices = new BitSet(parallelism);
        }

        void addEntry(
                int subtaskIndex,
                Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo> entry) {
            this.entries.add(entry);
            // set() 方法将指定索引处的位设置为 true
            reportedSubtaskIndices.set(subtaskIndex);
        }

        boolean isPartiallyReported() {
            // cardinality() 方法返回此 BitSet 中设置为 true 的位数
            return reportedSubtaskIndices.cardinality() > 0
                    && reportedSubtaskIndices.cardinality() < reportedSubtaskIndices.size();
        }
    }
}
