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

package org.apache.flink.runtime.executiongraph.failover.flip1;

import org.apache.flink.runtime.topology.Result;
import org.apache.flink.runtime.topology.Vertex;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Common utils for computing pipelined regions. */
public final class PipelinedRegionComputeUtil {

    static <V extends Vertex<?, ?, V, R>, R extends Result<?, ?, V, R>>
            Map<V, Set<V>> buildRawRegions(
                    final Iterable<? extends V> topologicallySortedVertices,
                    final Function<V, Iterable<R>> getNonReconnectableConsumedResults) {

        final Map<V, Set<V>> vertexToRegion = new IdentityHashMap<>();

        // iterate all the vertices which are topologically sorted
        for (V vertex : topologicallySortedVertices) {
            Set<V> currentRegion = new HashSet<>();
            currentRegion.add(vertex);
            vertexToRegion.put(vertex, currentRegion);

            // Similar to the BLOCKING ResultPartitionType, each vertex connected through
            // PIPELINED_APPROXIMATE is also considered as a single region. This attribute is
            // called "reconnectable". Reconnectable will be removed after FLINK-19895, see also
            // {@link ResultPartitionType#isReconnectable}
            /*
              从输入侧拿到 不可重连接的 ConsumedResults
              目前不可重连的类型只有 PIPELINE   PIPELINED_BOUNDED
            */
            for (R consumedResult : getNonReconnectableConsumedResults.apply(vertex)) {
                final V producerVertex = consumedResult.getProducer();
                // 在处理本vertex 的时候,它的前置 vertex 一定已经在vertexToRegion
                // 维护了,因为topologicallySortedVertices是有顺序的
                final Set<V> producerRegion = vertexToRegion.get(producerVertex);

                if (producerRegion == null) {
                    throw new IllegalStateException(
                            "Producer task "
                                    + producerVertex.getId()
                                    + " failover region is null"
                                    + " while calculating failover region for the consumer task "
                                    + vertex.getId()
                                    + ". This should be a failover region building bug.");
                }

                if (currentRegion != producerRegion) {
                    // 合并 region ,一般来说 currentRegion 要比 producerRegion 的容量小
                    // 从前面 往后面合并 .    这和spark 相反

                    // 把当前region 和前面的region 合并起来
                    currentRegion = mergeRegions(currentRegion, producerRegion, vertexToRegion);
                }
            }
        }

        return vertexToRegion;
    }

    static <V extends Vertex<?, ?, V, ?>> Set<V> mergeRegions(
            final Set<V> region1, final Set<V> region2, final Map<V, Set<V>> vertexToRegion) {

        // merge the smaller region into the larger one to reduce the cost
        final Set<V> smallerSet;
        final Set<V> largerSet;
        if (region1.size() < region2.size()) {
            smallerSet = region1;
            largerSet = region2;
        } else {
            smallerSet = region2;
            largerSet = region1;
        }
        for (V v : smallerSet) {
            // 之所以不添加 largerSet ,是因为 largerSet 大概率已经在vertexToRegion 里面
            vertexToRegion.put(v, largerSet);
        }
        largerSet.addAll(smallerSet);
        return largerSet;
    }

    static <V extends Vertex<?, ?, V, ?>> Set<Set<V>> uniqueRegions(
            final Map<V, Set<V>> vertexToRegion) {
        // IdentityHashMap 与普通的HashMap 的区别在于: 前者key 的判断用 == 来判断(比较地址) , 后者用 equals来判断(比较值)
        final Set<Set<V>> distinctRegions = Collections.newSetFromMap(new IdentityHashMap<>());
        distinctRegions.addAll(vertexToRegion.values());
        return distinctRegions;
    }

    private PipelinedRegionComputeUtil() {}
}
