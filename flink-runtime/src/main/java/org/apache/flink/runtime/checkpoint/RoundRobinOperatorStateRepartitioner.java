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
        Preconditions.checkArgument(previousParallelSubtaskStates.size() == oldParallelism,
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

            // 取出之前老的 状态元数据分布
            GroupByStateNameResults nameToStateByMode =
                    groupByStateMode(previousParallelSubtaskStates);

            // 集合gc 回收
            if (OPTIMIZE_MEMORY_USE) {
                previousParallelSubtaskStates.clear();
            }

            // 之前老的 状态元数据分布 GroupByStateNameResults对象,来做真正的状态重新分区逻辑
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

    //  每个 value 的 长度为   旧的并行度 * 算子状态句柄数
    private Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
            collectUnionStates(List<List<OperatorStateHandle>> parallelSubtaskStates) {
        // 获取 UNION 模式下所有  状态名 到  所有状态元数据 的映射
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

        // 遍历 老的并行度 次
        for (int i = 0; i < parallelSubtaskStates.size(); ++i) {
            final int subtaskIndex = i;

            List<OperatorStateHandle> subTaskState = parallelSubtaskStates.get(i);

            // 遍历老的 subtask 下 所有的 算子状态句柄
            for (OperatorStateHandle operatorStateHandle : subTaskState) {

                if (operatorStateHandle == null) {continue;}

                // 拿到 所有状态名 的 元数据信息
                final Set<Map.Entry<String, OperatorStateHandle.StateMetaInfo>>
                        partitionOffsetEntries = operatorStateHandle.getStateNameToPartitionOffsets().entrySet();

                partitionOffsetEntries.stream()
                        .filter(// 根据元数据里面的 分布模式信息, 过滤出想要的模式的状态
                                entry -> entry.getValue().getDistributionMode().equals(mode))
                        .forEach(
                                entry -> {
                                    // 1 第一次构建 StateEntry, 并放入 states 中,存在了以后就不会重复构建了
                                    // 2 因为 这里 两层for 循环 再加 一个 foreach 遍历
                                    //   这里最终出现的 StateMetaInfo 的总数量应该是
                                    //            旧的并行度 * 算子状态句柄数 * 某类分布模式的状态名个数
                                    StateEntry stateEntry =
                                            states.computeIfAbsent(
                                                    entry.getKey(), // 状态名
                                                    k -> new StateEntry(
                                                    parallelSubtaskStates.size() // 旧的并行度
                                                                   * partitionOffsetEntries.size(), //状态名的个数
                                                                    parallelSubtaskStates.size())); // 旧的并行度
                                    // 1 填充 states 下的  stateEntry
                                    // 2 这里 只不过把 所有的 StateMetaInfo, 换了种 存储结构和 组织方式
                                    stateEntry.addEntry(
                                            subtaskIndex,
                                            Tuple2.of( operatorStateHandle.getDelegateStateHandle(),
                                            entry.getValue())); // value的类型为OperatorStateHandle.StateMetaInfo
                                });
            }
        }

        return states;
    }

    /** Group by the different named states. */
    @SuppressWarnings("unchecked, rawtype")
    private GroupByStateNameResults groupByStateMode(
            List<List<OperatorStateHandle>> previousParallelSubtaskStates) {

        // 存储方法结果 , EnumMap 也是java原生api 的一种 map
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
                                                     previousParallelSubtaskStates) { // 遍历 算子旧并行度 次

            for (OperatorStateHandle operatorStateHandle :
                                       previousParallelSubtaskState) { // 遍历 算子状态句柄 次

                if (operatorStateHandle == null) {
                    continue;
                }

                final Set<Map.Entry<String, OperatorStateHandle.StateMetaInfo>>
                        partitionOffsetEntries =
                                operatorStateHandle.getStateNameToPartitionOffsets().entrySet();

                // 泛型中的String代表单个状态的名字 ,StateMetaInfo主要维护偏移量;因为多个状态存储的在一起,必须通过偏移量切分
                for (Map.Entry<String, OperatorStateHandle.StateMetaInfo> e :
                                                                  partitionOffsetEntries) {

                    // 3 层 循环 遍历 单个状态

                    OperatorStateHandle.StateMetaInfo metaInfo = e.getValue();

                    // nameToState的 状态元数据总数目为  ( 某种模式下的状态名个数 * 旧并行度 * 单个并行度下元数据数目 )
                    Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                            nameToState = nameToStateByMode.get(metaInfo.getDistributionMode());

                    List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>
                            stateLocations =
                                    nameToState.computeIfAbsent( // get  方法的加强版, 第一次会创建 ArrayList对象,以后不会
                                            e.getKey(), // 某种模式下的 单个状态的名字
                                            k ->
                                                new ArrayList<>(
                                    previousParallelSubtaskStates.size() // 旧并行度
                                                * partitionOffsetEntries.size())); // 单个并行度下元数据数目
                    // 填充 取出的 List
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

        // 用来存储方法返回结果, list长度为新的并行度
        List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList = new ArrayList<>(newParallelism);

        // 初始化结构对象,放入一些空的 map
        for (int i = 0; i < newParallelism; ++i) {
                mergeMapList.add(new HashMap<>());
        }

        // 处理 SPLIT_DISTRIBUTE 类型
        Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                nameToDistributeState = nameToStateByMode.getByMode(OperatorStateHandle.Mode.SPLIT_DISTRIBUTE);

        repartitionSplitState(nameToDistributeState, newParallelism, mergeMapList);

        // 处理 UNION 类型
        Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                nameToUnionState = nameToStateByMode.getByMode(OperatorStateHandle.Mode.UNION);

        repartitionUnionState(nameToUnionState, mergeMapList);

        // 处理 BROADCAST 类型
        Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                nameToBroadcastState = nameToStateByMode.getByMode(OperatorStateHandle.Mode.BROADCAST);

        repartitionBroadcastState(nameToBroadcastState, mergeMapList);

        return mergeMapList;
    }

    /** Repartition SPLIT_DISTRIBUTE state. */
    private void repartitionSplitState(
            Map<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                    nameToDistributeState,
            int newParallelism,
            List<Map<StreamStateHandle, OperatorStateHandle>> mergeMapList) {

        //最开始的 sub task 编号
        int startParallelOp = 0;

        //  遍历 SPLIT_DISTRIBUTE 模式下  状态名 数目 次
        for (Map.Entry<String, List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>>>
                                         e : nameToDistributeState.entrySet()) {

            List<Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo>> current = // 旧并行度
                    e.getValue();

            // 拿到某个状态名称的  最小粒度的总数目
            int totalPartitions = 0;
            for (Tuple2<StreamStateHandle, OperatorStateHandle.StateMetaInfo> offsets : current) {
                totalPartitions += offsets.f1.getOffsets().length;
            }

            // 在所有并行算子实例间重分区
            int lstIdx = 0;
            // 维护offsets 数组的索引位置
            int offsetIdx = 0;
            int baseFraction = totalPartitions / newParallelism;
            int remainder = totalPartitions % newParallelism;

            //  newStartParallelOp 为下次进去下面的for 循环时的 首个遍历的 subtask 编号
            int newStartParallelOp = startParallelOp;

            for (int i = 0; i < newParallelism; ++i) {

                //  本次遍历的 sub task 编号
                int parallelOpIdx = (i + startParallelOp) % newParallelism;

                // 某个记录新的sub task 应该分得的 粒度个数(需求总量)
                int numberOfPartitionsToAssign = baseFraction;

                if (remainder > 0) {
                    ++numberOfPartitionsToAssign;
                    --remainder;
                } else if (remainder == 0) {
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
                    // numberOfPartitionsToAssign表示需要分配的状态粒度 数目
                    // 如果 remaining 能够满足其全部的分配数目,则直接全部分配
                    // 如果不能满足其全部需要的数目,则尽可能满足,把 ramaining 消耗完
                    if (remaining > numberOfPartitionsToAssign) {
                        // java 原生 api Arrays.copyOfRange的使用方法:    将数组拷贝至另外一个数组
                        //  第一个参数为要拷贝的数组对象, 第二个参数为拷贝的开始位置（包含）,第三个参数为拷贝的结束位置（不包含）
                        offs = Arrays.copyOfRange(
                                        offsets, offsetIdx, offsetIdx + numberOfPartitionsToAssign);
                        // 这个offsets数组还没有被消耗完, 这次消耗了 numberOfPartitionsToAssign个位置,移动到还未被消耗的位置
                        offsetIdx += numberOfPartitionsToAssign;
                    } else {
                        if (OPTIMIZE_MEMORY_USE) {handleWithOffsets.f1 = null; }//gc
                        offs = Arrays.copyOfRange(offsets, offsetIdx, offsets.length);
                        //  这个offsets数组已经本分配消耗完了, 下一个offsets数据来了后,又得从0位置开始消耗,所以将其设置为0
                        offsetIdx = 0;
                        // 只有总需求没有全部得到满足, while循环便会一直进行
                        // 我们将 lstIdx 移动到下一个位置,以便获取下一个 offsets 数据的供给 来继续分配
                        ++lstIdx;
                    }
                    // 需求总量已经被分配了 remaining个, 更新需求总量
                    numberOfPartitionsToAssign -= remaining;

                    ///  我们将在 单个OperatorStateHandle中 使用相同StreamStateHandle的 分区合并

                    // 获取新的并行度下的 某个 subtask 编号的 mergeMap
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
                }//第三次while 循环结束


            } // 第二次for循环结束

            startParallelOp = newStartParallelOp;
            e.setValue(null);
        }// 第一层for 循环结束







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

        // 维护每个  subTask 是否已经添加过 元素到 成员 entries 中
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
