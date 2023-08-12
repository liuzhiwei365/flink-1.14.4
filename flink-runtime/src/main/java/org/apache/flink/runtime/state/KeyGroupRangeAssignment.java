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

package org.apache.flink.runtime.state;

import org.apache.flink.api.dag.Transformation;
import org.apache.flink.util.MathUtils;
import org.apache.flink.util.Preconditions;

public final class KeyGroupRangeAssignment {

    /**
     * The default lower bound for max parallelism if nothing was configured by the user. We have
     * this so allow users some degree of scale-up in case they forgot to configure maximum
     * parallelism explicitly.
     */
    public static final int DEFAULT_LOWER_BOUND_MAX_PARALLELISM = 1 << 7;

    /** The (inclusive) upper bound for max parallelism. */
    public static final int UPPER_BOUND_MAX_PARALLELISM =
            Transformation.UPPER_BOUND_MAX_PARALLELISM;

    private KeyGroupRangeAssignment() {
        throw new AssertionError();
    }

    /**
     * Assigns the given key to a parallel operator index.
     *
     * @param key the key to assign
     * @param maxParallelism the maximum supported parallelism, aka the number of key-groups.
     * @param parallelism the current parallelism of the operator
     * @return the index of the parallel operator to which the given key should be routed.
     */
    public static int assignKeyToParallelOperator(Object key, int maxParallelism, int parallelism) {
        Preconditions.checkNotNull(key, "Assigned key must not be null!");
        // 先给键 分配键组,
        return computeOperatorIndexForKeyGroup(
                maxParallelism, parallelism, assignToKeyGroup(key, maxParallelism));
    }

    /**
     * Assigns the given key to a key-group index.
     *
     * @param key the key to assign
     * @param maxParallelism the maximum supported parallelism, aka the number of key-groups.
     * @return the key-group to which the given key is assigned
     */
    public static int assignToKeyGroup(Object key, int maxParallelism) {
        Preconditions.checkNotNull(key, "Assigned key must not be null!");
        // 把key 先做 java的 hash ,再做 murmurHash , 再除以 最大的并行度 取模 作为键组
        return computeKeyGroupForKeyHash(key.hashCode(), maxParallelism);
    }

    /**
     * Assigns the given key to a key-group index.
     *
     * @param keyHash the hash of the key to assign
     * @param maxParallelism the maximum supported parallelism, aka the number of key-groups.
     * @return the key-group to which the given key is assigned
     */
    public static int computeKeyGroupForKeyHash(int keyHash, int maxParallelism) {
        return MathUtils.murmurHash(keyHash) % maxParallelism;
    }


    // maxParallelism 就是 键组的数量
    //      如果 maxParallelism = 10 ,那么键组的所有编号 就是 0 ,1 ,2   ...  8, 9
    // parallelism  当前并行度大小,
    // 而 operatorIndex 一定是  [0,parallelism] 离散闭区间的一个数

    // 规划指定 subtask（operatorIndex） 的 键组 编号的 range
    public static KeyGroupRange computeKeyGroupRangeForOperatorIndex(
            int maxParallelism, int parallelism, int operatorIndex) {

        checkParallelismPreconditions(parallelism);
        checkParallelismPreconditions(maxParallelism);

        Preconditions.checkArgument(
                maxParallelism >= parallelism,
                "Maximum parallelism must not be smaller than parallelism.");

        // 当前 operator索引位置
        int start = ((operatorIndex * maxParallelism + parallelism - 1) / parallelism);
        // 当前operator下一个索引的位置
        int end = ((operatorIndex + 1) * maxParallelism - 1) / parallelism;

        return new KeyGroupRange(start, end);
    }

    /**
     * Computes the index of the operator to which a key-group belongs under the given parallelism
     * and maximum parallelism.
     *
     * <p>IMPORTANT: maxParallelism must be <= Short.MAX_VALUE to avoid rounding problems in this
     * method. If we ever want to go beyond this boundary, this method must perform arithmetic on
     * long values.
     *
     * @param maxParallelism Maximal parallelism that the job was initially created with. 0 <
     *     parallelism <= maxParallelism <= Short.MAX_VALUE must hold.
     * @param parallelism The current parallelism under which the job runs. Must be <=
     *     maxParallelism.
     * @param keyGroupId Id of a key-group. 0 <= keyGroupID < maxParallelism.
     * @return The index of the operator to which elements from the given key-group should be routed
     *     under the given parallelism and maxParallelism.
     */
    // 因为 keyGroupId <= maxParallelism
    // 所以 keyGroupId * parallelism / maxParallelism <= maxParallelism * parallelism / maxParallelism
    // <= parallelism
    // 所以结果 一定落在 0 到 parallelism 范围内 , 左闭右开
    //  且 键组id 越大 ,越落在高位
    public static int computeOperatorIndexForKeyGroup(
            int maxParallelism, int parallelism, int keyGroupId) {
        return keyGroupId * parallelism / maxParallelism;
    }

    /**
     * Computes a default maximum parallelism from the operator parallelism. This is used in case
     * the user has not explicitly configured a maximum parallelism to still allow a certain degree
     * of scale-up.
     *
     * @param operatorParallelism the operator parallelism as basis for computation.
     * @return the computed default maximum parallelism.
     */
    public static int computeDefaultMaxParallelism(int operatorParallelism) {

        checkParallelismPreconditions(operatorParallelism);

        /*
             DEFAULT_LOWER_BOUND_MAX_PARALLELISM = 128   相当于本算法的最小值约束
             UPPER_BOUND_MAX_PARALLELISM = 32768   相当于本算法的最大值约束

             roundUpToPowerOfTwo 的实际效果是：
              -1                0
               0                0
               1 -> 1           2 的 0次幂
             (1,2] ->2          2 的 1次幂
             (2,4] -> 4         2 的 2次幂
             (4,8] -> 8         2 的 3次幂
             (8,16] -> 16         2 的 4次幂
             (16,32] -> 32        2 的 5次幂

            总结 该方法就是把输入的数字 扩张到输入的最近的 2 次幂

         */
        return Math.min(
                Math.max(
                        MathUtils.roundUpToPowerOfTwo(
                                operatorParallelism + (operatorParallelism / 2)),
                        DEFAULT_LOWER_BOUND_MAX_PARALLELISM),
                UPPER_BOUND_MAX_PARALLELISM);
    }

    public static void checkParallelismPreconditions(int parallelism) {
        Preconditions.checkArgument(
                parallelism > 0 && parallelism <= UPPER_BOUND_MAX_PARALLELISM,
                "Operator parallelism not within bounds: " + parallelism);
    }
}
