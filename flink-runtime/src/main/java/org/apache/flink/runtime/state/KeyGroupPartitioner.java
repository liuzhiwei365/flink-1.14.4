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

import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.util.Preconditions;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Class that contains the base algorithm for partitioning data into key-groups. This algorithm
 * currently works with two array (input, output) for optimal algorithmic complexity. Notice that
 * this could also be implemented over a single array, using some cuckoo-hashing-style element
 * replacement. This would have worse algorithmic complexity but better space efficiency. We
 * currently prefer the trade-off in favor of better algorithmic complexity.
 *
 * @param <T> type of the partitioned elements.
 */
public class KeyGroupPartitioner<T> {

    /**
     * The input data for the partitioning. All elements to consider must be densely in the index
     * interval [0, {@link #numberOfElements}[, without null values.
     */
    @Nonnull private final T[] partitioningSource;

    /**
     * The output array for the partitioning. The size must be {@link #numberOfElements} (or
     * bigger).
     */
    @Nonnull private final T[] partitioningDestination;

    /** Total number of input elements. */
    @Nonnegative private final int numberOfElements;

    /** The total number of key-groups in the job. */
    // 全局作业 范围内, 键组的总数
    @Nonnegative private final int totalKeyGroups;

    /**
     * This bookkeeping array is used to count the elements in each key-group. In a second step, it
     * is transformed into a histogram by accumulation.
     */
    @Nonnull private final int[] counterHistogram;

    /**
     * This is a helper array that caches the key-group for each element, so we do not have to
     * compute them twice.
     */
    // 保存 每个编号位置的元素 所分配的  相对键组值
    //  相对键组值 = 绝对键组值  - firstKeyGroup
    @Nonnull private final int[] elementKeyGroups;

    /** Cached value of keyGroupRange#firstKeyGroup. */
    @Nonnegative private final int firstKeyGroup;

    /** Function to extract the key from a given element. */
    @Nonnull private final KeyExtractorFunction<T> keyExtractorFunction;

    /** Function to write an element to a {@link DataOutputView}. */
    @Nonnull private final ElementWriterFunction<T> elementWriterFunction;

    /** Cached result. */
    @Nullable private PartitioningResult<T> computedResult;

    /**
     * Creates a new {@link KeyGroupPartitioner}.
     *
     * @param partitioningSource the input for the partitioning. All elements must be densely packed
     *     in the index interval [0, {@link #numberOfElements}[, without null values.
     * @param numberOfElements the number of elements to consider from the input, starting at input
     *     index 0.
     * @param partitioningDestination the output of the partitioning. Must have capacity of at least
     *     numberOfElements.
     * @param keyGroupRange the key-group range of the data that will be partitioned by this
     *     instance.
     * @param totalKeyGroups the total number of key groups in the job.
     * @param keyExtractorFunction this function extracts the partition key from an element.
     */
    public KeyGroupPartitioner(
            @Nonnull T[] partitioningSource,
            @Nonnegative int numberOfElements,
            @Nonnull T[] partitioningDestination,
            @Nonnull KeyGroupRange keyGroupRange,
            @Nonnegative int totalKeyGroups,
            @Nonnull KeyExtractorFunction<T> keyExtractorFunction,
            @Nonnull ElementWriterFunction<T> elementWriterFunction) {

        Preconditions.checkState(partitioningSource != partitioningDestination);
        Preconditions.checkState(partitioningSource.length >= numberOfElements);
        Preconditions.checkState(partitioningDestination.length >= numberOfElements);

        this.partitioningSource = partitioningSource;
        this.numberOfElements = numberOfElements;
        this.partitioningDestination = partitioningDestination;


        this.totalKeyGroups = totalKeyGroups;
        this.keyExtractorFunction = keyExtractorFunction;
        this.elementWriterFunction = elementWriterFunction;
        this.firstKeyGroup = keyGroupRange.getStartKeyGroup();
        this.elementKeyGroups = new int[numberOfElements];
        this.counterHistogram = new int[keyGroupRange.getNumberOfKeyGroups()];
        this.computedResult = null;
    }

    /**
     * Partitions the data into key-groups and returns the result as a {@link PartitioningResult}.
     */
    // 返回按 键组分区 排列 后 的结果    （注意PartitioningResult内部的 partitioningDestination成员数组）
    public PartitioningResult<T> partitionByKeyGroup() {
        if (computedResult == null) {
            //  计算每个元素的键组 （这些键组一定是在 本 task 的 keyGroupRange 中 ）
            reportAllElementKeyGroups();
            //  规划出每个键组将来在  partitioningDestination 数组中的 头一个索引下标
            int outputNumberOfElements = buildHistogramByAccumulatingCounts();
            //  按照 事先的规则执行分组
            executePartitioning(outputNumberOfElements);
        }
        return computedResult;
    }

    /** This method iterates over the input data and reports the key-group for each element. */
    protected void reportAllElementKeyGroups() {

        Preconditions.checkState(partitioningSource.length >= numberOfElements);

        for (int i = 0; i < numberOfElements; ++i) {
            int keyGroup =
                    KeyGroupRangeAssignment.assignToKeyGroup(
                            keyExtractorFunction.extractKeyFromElement(partitioningSource[i]),
                            totalKeyGroups);
            reportKeyGroupOfElementAtIndex(i, keyGroup);
        }
    }

    /**
     * This method reports in the bookkeeping data that the element at the given index belongs to
     * the given key-group.
     */
    protected void reportKeyGroupOfElementAtIndex(int index, int keyGroup) {
        // 计算相对键组值
        final int keyGroupIndex = keyGroup - firstKeyGroup;

        // 缓存相对键组值
        elementKeyGroups[index] = keyGroupIndex;

        // 统计 分配相对键组为 keyGroupIndex 的元素的个数
        ++counterHistogram[keyGroupIndex];
    }

    /**
     * This method creates a histogram from the counts per key-group in {@link #counterHistogram}.
     */
    // 初始  counterHistogram
    //    0   1   2   3   4
    //    3   7   2   5   6
    //
    //调用本方法累计计算后,变为：
    //    0   1   2    3    4
    //    0   3   10   12   17
    //    sum = 23

    // 为了把 partitioningSource 中 按键组散列的元素,  按键组 分类收集在一起, 而做的事先规划工作
    // 规划出每个键组将来在  partitioningDestination 数组中的 头一个索引下标
    private int buildHistogramByAccumulatingCounts() {
        int sum = 0;
        for (int i = 0; i < counterHistogram.length; ++i) {
            int currentSlotValue = counterHistogram[i];
            counterHistogram[i] = sum;
            sum += currentSlotValue;
        }
        return sum;
    }

    //  outputNumberOfElements 就是  buildHistogramByAccumulatingCounts 方法返回的 sum值;
    //  sum值 == partitioningSource 的长度 == elementKeyGroups 的长度 == 输入元素总个数
    private void executePartitioning(int outputNumberOfElements) {

        // We repartition the entries by their pre-computed key-groups, using the histogram values
        // as write indexes
        for (int inIdx = 0; inIdx < outputNumberOfElements; ++inIdx) {

            int effectiveKgIdx = elementKeyGroups[inIdx];

            //  假如 counterHistogram 是如下的结构：
            //  相对键组编号       0   1   2    3    4
            //  元素累计值         0   3   10   12   17         sum == 23 == outputNumberOfElements
            //
            //  [0,3)  [3,10)  [10,12)  [12,17)  [17,23)  这些区间中的每一个区间 必定是来自同一个键组
            int outIdx = counterHistogram[effectiveKgIdx]++;

            // 相当于把partitioningSource 中属于同一个 key group 的元素紧凑地放在一起,放在partitioningDestination 数组中
            partitioningDestination[outIdx] = partitioningSource[inIdx];
        }

        //  此时 counterHistogram的结构是：
        //  相对键组编号        0   1    2     3    4
        //  元素累计值          3   10   12   17   23
        this.computedResult =
                new PartitioningResultImpl<>(
                        elementWriterFunction,
                        firstKeyGroup,
                        counterHistogram,
                        partitioningDestination);
    }

    /** This represents the result of key-group partitioning. */
    public interface PartitioningResult<T> extends StateSnapshot.StateKeyGroupWriter {
        Iterator<T> iterator(int keyGroupId);
    }

    /** The data in {@link * #partitionedElements} is partitioned w.r.t. key group range. */
    private static class PartitioningResultImpl<T> implements PartitioningResult<T> {

        /** Function to write one element to a {@link DataOutputView}. */
        @Nonnull private final ElementWriterFunction<T> elementWriterFunction;

        /**
         * The exclusive-end-offsets for all key-groups of the covered range for the partitioning.
         * Exclusive-end-offset for key-group n is under keyGroupOffsets[n - firstKeyGroup].
         */
        @Nonnull private final int[] keyGroupOffsets;

        /**
         * Array with elements that are partitioned w.r.t. the covered key-group range. The start
         * offset for each key-group is in {@link #keyGroupOffsets}.
         */
        @Nonnull private final T[] partitionedElements;

        /** The first key-group of the range covered in the partitioning. */
        @Nonnegative private final int firstKeyGroup;

        PartitioningResultImpl(
                @Nonnull ElementWriterFunction<T> elementWriterFunction,
                @Nonnegative int firstKeyGroup,
                @Nonnull int[] keyGroupEndOffsets,
                @Nonnull T[] partitionedElements) {
            this.elementWriterFunction = elementWriterFunction;
            this.firstKeyGroup = firstKeyGroup;
            this.keyGroupOffsets = keyGroupEndOffsets;
            this.partitionedElements = partitionedElements;
        }

        @Nonnegative
        private int getKeyGroupStartOffsetInclusive(int keyGroup) {
            int idx = keyGroup - firstKeyGroup - 1;
            return idx < 0 ? 0 : keyGroupOffsets[idx];
        }

        @Nonnegative
        private int getKeyGroupEndOffsetExclusive(int keyGroup) {
            return keyGroupOffsets[keyGroup - firstKeyGroup];
        }

        @Override
        public void writeStateInKeyGroup(@Nonnull DataOutputView dov, int keyGroupId)
                throws IOException {

            int startOffset = getKeyGroupStartOffsetInclusive(keyGroupId);
            int endOffset = getKeyGroupEndOffsetExclusive(keyGroupId);

            // write number of mappings in key-group
            dov.writeInt(endOffset - startOffset);

            // write mappings
            for (int i = startOffset; i < endOffset; ++i) {
                elementWriterFunction.writeElement(partitionedElements[i], dov);
            }
        }

        @Override
        public Iterator<T> iterator(int keyGroupId) {
            int startOffset = getKeyGroupStartOffsetInclusive(keyGroupId);
            int endOffset = getKeyGroupEndOffsetExclusive(keyGroupId);

            return Arrays.stream(partitionedElements, startOffset, endOffset).iterator();
        }
    }

    public static <T> StateSnapshotKeyGroupReader createKeyGroupPartitionReader(
            @Nonnull ElementReaderFunction<T> readerFunction,
            @Nonnull KeyGroupElementsConsumer<T> elementConsumer) {
        return new PartitioningResultKeyGroupReader<>(readerFunction, elementConsumer);
    }

    /**
     * General algorithm to read key-grouped state that was written from a {@link
     * PartitioningResultImpl}.
     *
     * @param <T> type of the elements to read.
     */
    private static class PartitioningResultKeyGroupReader<T>
            implements StateSnapshotKeyGroupReader {

        @Nonnull private final ElementReaderFunction<T> readerFunction;

        @Nonnull private final KeyGroupElementsConsumer<T> elementConsumer;

        public PartitioningResultKeyGroupReader(
                @Nonnull ElementReaderFunction<T> readerFunction,
                @Nonnull KeyGroupElementsConsumer<T> elementConsumer) {

            this.readerFunction = readerFunction;
            this.elementConsumer = elementConsumer;
        }

        // 读回  后台存储类型 为 PRIORITY_QUEUE 的 某个状态名称下 指定键组的下 所有状态
        @Override
        public void readMappingsInKeyGroup(@Nonnull DataInputView in, @Nonnegative int keyGroupId)
                throws IOException {
            int numElements = in.readInt();
            for (int i = 0; i < numElements; i++) {

                /* 参见 HeapPriorityQueueSnapshotRestoreWrapper.keyGroupReader 方法

                     KeyGroupPartitioner.createKeyGroupPartitionReader(
                                         elementSerializer::deserialize,
                                         (element, keyGroupId) -> priorityQueue.add(element))
                 */

                // readerFunction 是 元素的 序列化器
                T element = readerFunction.readElement(in);
                // 最终会将 状态数据添加到 HeapPriorityQueueSnapshotRestoreWrapper 的 priorityQueue 中
                elementConsumer.consume(element, keyGroupId);
            }
        }
    }

    /**
     * This functional interface defines how one element is written to a {@link DataOutputView}.
     *
     * @param <T> type of the written elements.
     */
    @FunctionalInterface
    public interface ElementWriterFunction<T> {

        /**
         * This method defines how to write a single element to the output.
         *
         * @param element the element to be written.
         * @param dov the output view to write the element.
         * @throws IOException on write-related problems.
         */
        void writeElement(@Nonnull T element, @Nonnull DataOutputView dov) throws IOException;
    }

    /**
     * This functional interface defines how one element is read from a {@link DataInputView}.
     *
     * @param <T> type of the read elements.
     */
    @FunctionalInterface
    public interface ElementReaderFunction<T> {

        @Nonnull
        T readElement(@Nonnull DataInputView div) throws IOException;
    }

    /**
     * Functional interface to consume elements from a key group.
     *
     * @param <T> type of the consumed elements.
     */
    @FunctionalInterface
    public interface KeyGroupElementsConsumer<T> {
        void consume(@Nonnull T element, @Nonnegative int keyGroupId) throws IOException;
    }
}
