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

package org.apache.flink.runtime.checkpoint;

import javax.annotation.concurrent.NotThreadSafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A repartitioner that assigns the same channel state to multiple subtasks according to some
 * mapping.
 *
 * <p>The replicated data will then be filtered before processing the record.
 *
 * <p>Note that channel mappings are cached for the same parallelism changes.
 */
@NotThreadSafe
public class MappingBasedRepartitioner<T> implements OperatorStateRepartitioner<T> {
    private final RescaleMappings newToOldSubtasksMapping;

    public MappingBasedRepartitioner(RescaleMappings newToOldSubtasksMapping) {
        this.newToOldSubtasksMapping = newToOldSubtasksMapping;
    }

    // 抽取 oldIndexes 老并行度编号 列表数组 抽取 其对应的所有的 状态句柄
    private static <T> List<T> extractOldState(
            List<List<T>> previousParallelSubtaskStates, int[] oldIndexes) {
        switch (oldIndexes.length) {
            case 0:
                return Collections.emptyList();
            case 1:
                return previousParallelSubtaskStates.get(oldIndexes[0]);
            default:
                return Arrays.stream(oldIndexes)
                        .boxed()
                        .flatMap(oldIndex -> previousParallelSubtaskStates.get(oldIndex).stream())
                        .collect(Collectors.toList());
        }
    }

    // 拿到每个新 并行度编号 应该拿到的 所有状态句柄
    @Override
    public List<List<T>> repartitionState(
            List<List<T>> previousParallelSubtaskStates, int oldParallelism, int newParallelism) {
        // 用来存储结果
        List<List<T>> repartitioned = new ArrayList<>();


        for (int newIndex = 0; newIndex < newParallelism; newIndex++) {
            // 拿到指定  并行度编号newIndex,  应该拿到的 所有状态句柄
            repartitioned.add(
                    extractOldState(
                            previousParallelSubtaskStates,
                            // 新并行度编号 所对应的 老并行度编号 列表数组
                            newToOldSubtasksMapping.getMappedIndexes(newIndex)));
        }
        return repartitioned;
    }
}
