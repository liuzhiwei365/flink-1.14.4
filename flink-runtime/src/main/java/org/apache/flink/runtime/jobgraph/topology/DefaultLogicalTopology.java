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

package org.apache.flink.runtime.jobgraph.topology;

import org.apache.flink.runtime.executiongraph.failover.flip1.LogicalPipelinedRegionComputeUtil;
import org.apache.flink.runtime.jobgraph.IntermediateDataSet;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.apache.flink.util.Preconditions.checkNotNull;

/** Default implementation of {@link LogicalTopology}. It is an adapter of {@link JobGraph}. */
public class DefaultLogicalTopology implements LogicalTopology {

    private final List<DefaultLogicalVertex> verticesSorted;

    // 逻辑拓扑阶段 与 JobGraph阶段 对应

    private final Map<JobVertexID, DefaultLogicalVertex> idToVertexMap;

    private final Map<IntermediateDataSetID, DefaultLogicalResult> idToResultMap;

    private DefaultLogicalTopology(final List<JobVertex> jobVertices) {
        checkNotNull(jobVertices);

        this.verticesSorted = new ArrayList<>(jobVertices.size());
        this.idToVertexMap = new HashMap<>();
        this.idToResultMap = new HashMap<>();

        buildVerticesAndResults(jobVertices);
    }

    public static DefaultLogicalTopology fromJobGraph(final JobGraph jobGraph) {
        checkNotNull(jobGraph);

        return fromTopologicallySortedJobVertices(
                jobGraph.getVerticesSortedTopologicallyFromSources());
    }

    public static DefaultLogicalTopology fromTopologicallySortedJobVertices(
            final List<JobVertex> jobVertices) {
        return new DefaultLogicalTopology(jobVertices);
    }

    private void buildVerticesAndResults(final Iterable<JobVertex> topologicallySortedJobVertices) {
        final Function<JobVertexID, DefaultLogicalVertex> vertexRetriever = this::getVertex;
        final Function<IntermediateDataSetID, DefaultLogicalResult> resultRetriever =
                this::getResult;

        for (JobVertex jobVertex : topologicallySortedJobVertices) {
            final DefaultLogicalVertex logicalVertex =
                    new DefaultLogicalVertex(jobVertex, resultRetriever);
            this.verticesSorted.add(logicalVertex);
            this.idToVertexMap.put(logicalVertex.getId(), logicalVertex);

            // 如果是分流的情况, 则IntermediateDataSet 会有多个
            for (IntermediateDataSet intermediateDataSet : jobVertex.getProducedDataSets()) {
                final DefaultLogicalResult logicalResult =
                        new DefaultLogicalResult(intermediateDataSet, vertexRetriever);
                idToResultMap.put(logicalResult.getId(), logicalResult);
            }
        }
    }

    @Override
    public Iterable<DefaultLogicalVertex> getVertices() {
        return verticesSorted;
    }

    private DefaultLogicalVertex getVertex(final JobVertexID vertexId) {
        return Optional.ofNullable(idToVertexMap.get(vertexId))
                .orElseThrow(
                        () -> new IllegalArgumentException("can not find vertex: " + vertexId));
    }

    private DefaultLogicalResult getResult(final IntermediateDataSetID resultId) {
        return Optional.ofNullable(idToResultMap.get(resultId))
                .orElseThrow(
                        () -> new IllegalArgumentException("can not find result: " + resultId));
    }

    @Override
    public Iterable<DefaultLogicalPipelinedRegion> getAllPipelinedRegions() {
        // 计算 PipelinedRegions
        final Set<Set<LogicalVertex>> regionsRaw =
                LogicalPipelinedRegionComputeUtil.computePipelinedRegions(verticesSorted);

        final Set<DefaultLogicalPipelinedRegion> regions = new HashSet<>();
        for (Set<LogicalVertex> regionVertices : regionsRaw) {
            regions.add(new DefaultLogicalPipelinedRegion(regionVertices));
        }
        return regions;
    }
}
