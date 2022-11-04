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
 * limitations under the License
 */

package org.apache.flink.runtime.executiongraph;

import org.apache.flink.runtime.jobgraph.DistributionPattern;
import org.apache.flink.runtime.jobgraph.IntermediateResultPartitionID;
import org.apache.flink.runtime.scheduler.strategy.ConsumedPartitionGroup;
import org.apache.flink.runtime.scheduler.strategy.ConsumerVertexGroup;
import org.apache.flink.runtime.scheduler.strategy.ExecutionVertexID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Utilities for building {@link EdgeManager}. */
public class EdgeManagerBuildUtil {

    /**
     * Calculate the connections between {@link ExecutionJobVertex} and {@link IntermediateResult} *
     * based on the {@link DistributionPattern}.
     *
     * @param vertex the downstream consumer {@link ExecutionJobVertex}
     * @param intermediateResult the upstream consumed {@link IntermediateResult}
     * @param distributionPattern the {@link DistributionPattern} of the edge that connects the
     *     upstream {@link IntermediateResult} and the downstream {@link IntermediateResult}
     */
    static void connectVertexToResult(
            ExecutionJobVertex vertex,
            IntermediateResult intermediateResult,
            DistributionPattern distributionPattern) {

        switch (distributionPattern) {
            // 下游 JobVertex 的输入 partition 算法，如果是 forward 或 rescale 的话为 POINTWISE
            case POINTWISE:
                connectPointwise(vertex.getTaskVertices(), intermediateResult);
                break;
            // 每一个并行的ExecutionVertex节点都会链接到源节点产生的  所有中间结果IntermediateResultPartition
            case ALL_TO_ALL:
                connectAllToAll(vertex.getTaskVertices(), intermediateResult);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized distribution pattern.");
        }
    }

    /**
     * Given parallelisms of two job vertices, compute the max number of edges connected to a target
     * execution vertex from the source execution vertices. Note that edge is considered undirected
     * here. It can be an edge connected from an upstream job vertex to a downstream job vertex, or
     * in a reversed way.
     *
     * @param targetParallelism parallelism of the target job vertex.
     * @param sourceParallelism parallelism of the source job vertex.
     * @param distributionPattern the {@link DistributionPattern} of the connecting edge.
     */
    public static int computeMaxEdgesToTargetExecutionVertex(
            int targetParallelism, int sourceParallelism, DistributionPattern distributionPattern) {
        switch (distributionPattern) {
            case POINTWISE:
                return (sourceParallelism + targetParallelism - 1) / targetParallelism;
            case ALL_TO_ALL:
                return sourceParallelism;
            default:
                throw new IllegalArgumentException("Unrecognized distribution pattern.");
        }
    }

    private static void connectAllToAll(
            ExecutionVertex[] taskVertices, IntermediateResult intermediateResult) { //前置的IntermediateResult

        List<IntermediateResultPartitionID> consumedPartitions =
                Arrays.stream(intermediateResult.getPartitions())
                        .map(IntermediateResultPartition::getPartitionId)
                        .collect(Collectors.toList());

        ConsumedPartitionGroup consumedPartitionGroup =
                createAndRegisterConsumedPartitionGroupToEdgeManager(
                        consumedPartitions, intermediateResult);

        for (ExecutionVertex ev : taskVertices) {
            //所有的上游 只构成一个  被消费分区组
            ev.addConsumedPartitionGroup(consumedPartitionGroup);
        }

        List<ExecutionVertexID> consumerVertices =
                Arrays.stream(taskVertices)
                        .map(ExecutionVertex::getID)
                        .collect(Collectors.toList());

        //调用工厂方法,创建ConsumerVertexGroup 对象
        ConsumerVertexGroup consumerVertexGroup =
                ConsumerVertexGroup.fromMultipleVertices(consumerVertices);

        for (IntermediateResultPartition partition : intermediateResult.getPartitions()) {
            //所有的下游 也只构成一个 消费顶点组
            partition.addConsumers(consumerVertexGroup);
        }
    }

    private static void connectPointwise(
            ExecutionVertex[] taskVertices, IntermediateResult intermediateResult) {

        final int sourceCount = intermediateResult.getPartitions().length;
        final int targetCount = taskVertices.length;

        if (sourceCount == targetCount) {
            // caseA 一对一进行连接
            for (int i = 0; i < sourceCount; i++) {
                ExecutionVertex executionVertex = taskVertices[i];//下游
                IntermediateResultPartition partition = intermediateResult.getPartitions()[i];//上游

                ConsumerVertexGroup consumerVertexGroup =
                        ConsumerVertexGroup.fromSingleVertex(executionVertex.getID());
                partition.addConsumers(consumerVertexGroup);

                //到EdgeManager中注册
                ConsumedPartitionGroup consumedPartitionGroup =
                        createAndRegisterConsumedPartitionGroupToEdgeManager(
                                partition.getPartitionId(), intermediateResult);

                executionVertex.addConsumedPartitionGroup(consumedPartitionGroup);
            }
        } else if (sourceCount > targetCount) {
            // caseB 源的并行度 大于 目标并行度
            for (int index = 0; index < targetCount; index++) {

                ExecutionVertex executionVertex = taskVertices[index];
                //下游的vertex 不够用 , 一个vertex 就组成一个 消费顶点组
                ConsumerVertexGroup consumerVertexGroup =
                        ConsumerVertexGroup.fromSingleVertex(executionVertex.getID());
                /**
                 *  假设 sourceCount = 8   targetCount = 5
                 *
                 *  当 index = 0 时, start = 0 ,end = 1 consumedPartitions 的容量为 1;
                 *  当 index = 1 时, start = 1 ,end = 3 consumedPartitions 的容量为 2;
                 *  当 index = 2 时, start = 3 ,end = 4 consumedPartitions 的容量为 1;
                 *  当 index = 3 时, start = 4 ,end = 6 consumedPartitions 的容量为 2;
                 *  当 index = 4 时, start = 6 ,end = 8 consumedPartitions 的容量为 2;
                 *
                 *  其实这也算一种散列算法
                 */
                int start = index * sourceCount / targetCount;
                int end = (index + 1) * sourceCount / targetCount;

                List<IntermediateResultPartitionID> consumedPartitions =
                        new ArrayList<>(end - start);

                for (int i = start; i < end; i++) {
                    IntermediateResultPartition partition = intermediateResult.getPartitions()[i];

                    //让上游的partition 维护下游的信息
                    partition.addConsumers(consumerVertexGroup);

                    consumedPartitions.add(partition.getPartitionId());
                }

                //上游的 partition多了,多个partition组成一个 被消费分区组
                ConsumedPartitionGroup consumedPartitionGroup =
                        createAndRegisterConsumedPartitionGroupToEdgeManager(
                                consumedPartitions, intermediateResult);
                //让下游的vertex 维护 上游的信息
                executionVertex.addConsumedPartitionGroup(consumedPartitionGroup);
            }
        } else {
            // caseC 源的并行度 小于 目标并行度
            for (int partitionNum = 0; partitionNum < sourceCount; partitionNum++) {

                IntermediateResultPartition partition =
                        intermediateResult.getPartitions()[partitionNum];

                //上游的partition不够, 一个partition就组成一个 被消费的分区组
                ConsumedPartitionGroup consumedPartitionGroup =
                        createAndRegisterConsumedPartitionGroupToEdgeManager(
                                partition.getPartitionId(), intermediateResult);
                /**
                 *  假设 sourceCount = 5   targetCount = 8
                 *
                 *  当 partitionNum = 0 时, start = 0 ,end = 2 consumers 的容量为 2;
                 *  当 partitionNum = 1 时, start = 2 ,end = 4 consumers 的容量为 2;
                 *  当 partitionNum = 2 时, start = 4 ,end = 5 consumers 的容量为 1;
                 *  当 partitionNum = 3 时, start = 5 ,end = 7 consumers 的容量为 2;
                 *  当 partitionNum = 4 时, start = 7 ,end = 8 consumers 的容量为 1;
                 *
                 *  其实这也可以认为是一种散列算法
                 */
                int start = (partitionNum * targetCount + sourceCount - 1) / sourceCount;
                int end = ((partitionNum + 1) * targetCount + sourceCount - 1) / sourceCount;

                List<ExecutionVertexID> consumers = new ArrayList<>(end - start);

                for (int i = start; i < end; i++) {
                    ExecutionVertex executionVertex = taskVertices[i];
                    //让下游的vertex 维护 上游的信息
                    //内部调用getEdgeManager().connectVertexWithConsumedPartitionGroup(executionVertexId, consumedPartitions);
                    //更新 EdgeManager 的 vertexConsumedPartitions 成员变量
                    executionVertex.addConsumedPartitionGroup(consumedPartitionGroup);

                    consumers.add(executionVertex.getID());
                }
                //下游的target多了 , 多个target 组成一个消费顶点组 ,共享上游的一个partition
                ConsumerVertexGroup consumerVertexGroup =
                        ConsumerVertexGroup.fromMultipleVertices(consumers);
                //让上游的partition 维护下游的信息
                //内部调用getEdgeManager().connectPartitionWithConsumerVertexGroup(partitionId, consumers);
                //更新 EdgeManager 的 partitionConsumers 成员变量
                partition.addConsumers(consumerVertexGroup);
            }
        }
    }

    private static ConsumedPartitionGroup createAndRegisterConsumedPartitionGroupToEdgeManager(
            IntermediateResultPartitionID consumedPartitionId,
            IntermediateResult intermediateResult) {
        //注意和下面的重载方法的不同
        //唯一的不同是 要将第一个入参 包装成为一个集合 ; 这样就能和下面的方法适配了
        ConsumedPartitionGroup consumedPartitionGroup =
                ConsumedPartitionGroup.fromSinglePartition(consumedPartitionId);
        registerConsumedPartitionGroupToEdgeManager(consumedPartitionGroup, intermediateResult);
        return consumedPartitionGroup;
    }

    private static ConsumedPartitionGroup createAndRegisterConsumedPartitionGroupToEdgeManager(
            List<IntermediateResultPartitionID> consumedPartitions,
            IntermediateResult intermediateResult) {
        //注意和上面的重载方法的不同
        ConsumedPartitionGroup consumedPartitionGroup =
                ConsumedPartitionGroup.fromMultiplePartitions(consumedPartitions);
        registerConsumedPartitionGroupToEdgeManager(consumedPartitionGroup, intermediateResult);
        return consumedPartitionGroup;
    }

    private static void registerConsumedPartitionGroupToEdgeManager(
            ConsumedPartitionGroup consumedPartitionGroup, IntermediateResult intermediateResult) {
        intermediateResult
                .getProducer()
                .getGraph()
                .getEdgeManager()
                .registerConsumedPartitionGroup(consumedPartitionGroup);
    }
}
