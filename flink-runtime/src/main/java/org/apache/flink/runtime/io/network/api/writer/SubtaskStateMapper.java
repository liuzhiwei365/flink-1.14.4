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

package org.apache.flink.runtime.io.network.api.writer;

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.checkpoint.RescaleMappings;
import org.apache.flink.runtime.state.KeyGroupRange;
import org.apache.flink.runtime.state.KeyGroupRangeAssignment;
import org.apache.flink.runtime.util.IntArrayList;

import java.util.stream.IntStream;

/**
 * The {@code SubtaskStateMapper} narrows down the subtasks that need to be read during rescaling to
 * recover from a particular subtask when in-flight data has been stored in the checkpoint.
 *
 * <p>Mappings of old subtasks to new subtasks may be unique or non-unique. A unique assignment
 * means that a particular old subtask is only assigned to exactly one new subtask. Non-unique
 * assignments require filtering downstream. That means that the receiver side has to cross-verify
 * for a deserialized record if it truly belongs to the new subtask or not. Most {@code
 * SubtaskStateMapper} will only produce unique assignments and are thus optimal. Some rescaler,
 * such as {@link #RANGE}, create a mixture of unique and non-unique mappings, where downstream
 * tasks need to filter on some mapped subtasks.
 */
@Internal
public enum SubtaskStateMapper {

    /**
     * Extra state is redistributed to other subtasks without any specific guarantee (only that up-
     * and downstream are matched).
     */
    ARBITRARY {
        @Override
        public int[] getOldSubtasks(
                int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            // The current implementation uses round robin but that may be changed later.
            return ROUND_ROBIN.getOldSubtasks(
                    newSubtaskIndex, oldNumberOfSubtasks, newNumberOfSubtasks);
        }
    },

    /** Restores extra subtasks to the first subtask. */
    FIRST {
        @Override
        public int[] getOldSubtasks(
                int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            return newSubtaskIndex == 0 ? IntStream.range(0, oldNumberOfSubtasks).toArray() : EMPTY;
        }
    },

    /**
     * Replicates the state to all subtasks. This rescaling causes a huge overhead and completely
     * relies on filtering the data downstream.
     *
     * <p>This strategy should only be used as a fallback.
     */
    FULL {
        @Override
        public int[] getOldSubtasks(
                int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            return IntStream.range(0, oldNumberOfSubtasks).toArray();
        }

        @Override
        public boolean isAmbiguous() {
            return true;
        }
    },

    /**
     * Remaps old ranges to new ranges. For minor rescaling that means that new subtasks are mostly
     * assigned 2 old subtasks.
     *
     * <p>Example:<br>
     * old assignment: 0 -> [0;43); 1 -> [43;87); 2 -> [87;128)<br>
     * new assignment: 0 -> [0;64]; 1 -> [64;128)<br>
     * subtask 0 recovers data from old subtask 0 + 1 and subtask 1 recovers data from old subtask 0
     * + 2
     *
     * <p>For all downscale from n to [n-1 .. n/2], each new subtasks get exactly two old subtasks
     * assigned.
     *
     * <p>For all upscale from n to [n+1 .. 2*n-1], most subtasks get two old subtasks assigned,
     * except the two outermost.
     *
     * <p>Larger scale factors ({@code <n/2}, {@code >2*n}), will increase the number of old
     * subtasks accordingly. However, they will also create more unique assignment, where an old
     * subtask is exclusively assigned to a new subtask. Thus, the number of non-unique mappings is
     * upper bound by 2*n.
     */
    RANGE {

        // 返回的  int[]  一定是一个 range 范围
        @Override
        public int[] getOldSubtasks(
                int newSubtaskIndex, // 新的 并行度索引编号   base
                int oldNumberOfSubtasks, // 老并行度数据
                int newNumberOfSubtasks) { // 新并行度数目
            // the actual maxParallelism cancels out
            int maxParallelism = KeyGroupRangeAssignment.UPPER_BOUND_MAX_PARALLELISM;

            // 计算新的  并行度索引编号 所对应的 键组范围
            final KeyGroupRange newRange =
                    KeyGroupRangeAssignment.computeKeyGroupRangeForOperatorIndex(
                            maxParallelism, newNumberOfSubtasks, newSubtaskIndex);

            // 根据键组的 开始值 和 结束值 映射  老的分区范围
            // 并且认为老的 分区范围  与 新的分区编号（ 就是 新的并行度索引编号 newSubtaskIndex ）是对应的
            /*
               1 这里其实利用的是 区间映射投影（数学理论支撑） 的技巧,只不过是 离散的版本
               2 我们 假设 [0 , maxPara) 是最精细的分区模版
               3 [0,oldPara)  和 [0,newPara) 之间的缩放要以 [0 , maxPara) 作为参照

               画图实例： 假设oldPara = 4  newPara = 2  maxPara = 8

               newPara-0            maxPara-0             oldPara-0
                                    maxPara-1
                                    maxPara-2             oldPara-1
                                    maxPara-3
               newPara-1            maxPara-4             oldPara-2
                                    maxPara-5
                                    maxPara-6             oldPara-3
                                    maxPara-7

             */
            final int start =
                    KeyGroupRangeAssignment.computeOperatorIndexForKeyGroup(
                            maxParallelism, oldNumberOfSubtasks, newRange.getStartKeyGroup());

            final int end =
                    KeyGroupRangeAssignment.computeOperatorIndexForKeyGroup(
                            maxParallelism, oldNumberOfSubtasks, newRange.getEndKeyGroup());
            return IntStream.range(start, end + 1).toArray();
        }

        @Override
        public boolean isAmbiguous() {
            return true;
        }
    },

    /**
     * Redistributes subtask state in a round robin fashion. Returns a mapping of {@code newIndex ->
     * oldIndexes}. The mapping is accessed by using {@code Bitset oldIndexes =
     * mapping.get(newIndex)}.
     *
     * <p>For {@code oldParallelism < newParallelism}, that mapping is trivial. For example if
     * oldParallelism = 6 and newParallelism = 10.
     *
     * <table>
     *     <thead><td>New index</td><td>Old indexes</td></thead>
     *     <tr><td>0</td><td>0</td></tr>
     *     <tr><td>1</td><td>1</td></tr>
     *     <tr><td span="2" align="center">...</td></tr>
     *     <tr><td>5</td><td>5</td></tr>
     *     <tr><td>6</td><td></td></tr>
     *     <tr><td span="2" align="center">...</td></tr>
     *     <tr><td>9</td><td></td></tr>
     * </table>
     *
     * <p>For {@code oldParallelism > newParallelism}, new indexes get multiple assignments by
     * wrapping around assignments in a round-robin fashion. For example if oldParallelism = 10 and
     * newParallelism = 4.
     *
     * <table>
     *     <thead><td>New index</td><td>Old indexes</td></thead>
     *     <tr><td>0</td><td>0, 4, 8</td></tr>
     *     <tr><td>1</td><td>1, 5, 9</td></tr>
     *     <tr><td>2</td><td>2, 6</td></tr>
     *     <tr><td>3</td><td>3, 7</td></tr>
     * </table>
     */
    ROUND_ROBIN {
        @Override
        public int[] getOldSubtasks(
                int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            final IntArrayList subtasks =
                    new IntArrayList(oldNumberOfSubtasks / newNumberOfSubtasks + 1);
            for (int subtask = newSubtaskIndex;
                    subtask < oldNumberOfSubtasks;
                    subtask += newNumberOfSubtasks) {
                subtasks.add(subtask);
            }
            return subtasks.toArray();
        }
    },

    UNSUPPORTED {
        @Override
        public int[] getOldSubtasks(
                int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            throw new UnsupportedOperationException(
                    "Cannot rescale the given pointwise partitioner.\n"
                            + "Did you change the partitioner to forward or rescale?\n"
                            + "It may also help to add an explicit shuffle().");
        }
    };

    private static final int[] EMPTY = new int[0];

    /**
     * Returns all old subtask indexes that need to be read to restore all buffers for the given new
     * subtask index on rescale.
     */
    // 得到 新的 并行度编号 对应的 老的并行度编号列表
    public abstract int[] getOldSubtasks(
            int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks);

    /** Returns a mapping new subtask index to all old subtask indexes. */

    public RescaleMappings getNewToOldSubtasksMapping(int oldParallelism, int newParallelism) {

        /*
           1 RescaleMappings.of 只是被已经重分区好的结果, 再做整理,修剪和包装 而已
           2 核心在  getOldSubtasks 方法
                   2.1   本类的多个枚举对象 RANGE, ROUND_ROBIN 对 getOldSubtasks 都有重新实现

                   2.2   具体走哪个实现得看 相关的 StreamPartitioner

                         StreamPartitioner的子类都有如下两个方法:
                             getUpstreamSubtaskStateMapper      定义了 ResultSubpartition 的重分区策略
                             getDownstreamSubtaskStateMapper    定义了 InputChannel 的重分区策略

                   2.3   StreamingJobGraphGenerator.connect 方法中会调用
                         设置 DownstreamSubtaskStateMapper   和  UpstreamSubtaskStateMapper 的方法
                         而 它们最终来自与分区器

                   2.4   分区器以及  input output 重分区策略
                         分区器                          UpstreamSubtaskStateMapper     DownstreamSubtaskStateMapper
                         BroadcastPartitioner                ARBITRARY                       UNSUPPORTED（不支持）
                   *     BinaryHashPartitioner               ARBITRARY                       FULL
                   *     KeyGroupStreamPartitioner           ARBITRARY                       RANGE
                         GlobalPartitioner                   ARBITRARY                       FIRST

                         ShufflePartitioner                  ARBITRARY                        ROUND_ROBIN
                         RebalancePartitioner                ARBITRARY                        ROUND_ROBIN

                         ForwardPartitioner                  UNSUPPORTED（不支持）             UNSUPPORTED（不支持）
                         RescalePartitioner                  UNSUPPORTED（不支持）             UNSUPPORTED（不支持）
         */
        return RescaleMappings.of(
                IntStream.range(0, newParallelism)
                        .mapToObj(
                                channelIndex ->
                                        // 核心;   得到 channelIndex 对应的 老的并行度编号列表
                                        getOldSubtasks(channelIndex, oldParallelism, newParallelism)),
                oldParallelism);
    }

    /**
     * Returns true iff this mapper can potentially lead to ambiguous mappings where the different
     * new subtasks map to the same old subtask. The assumption is that such replicated data needs
     * to be filtered.
     */
    public boolean isAmbiguous() {
        return false;
    }
}
