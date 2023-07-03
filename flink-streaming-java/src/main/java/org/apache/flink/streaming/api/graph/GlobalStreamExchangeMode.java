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

package org.apache.flink.streaming.api.graph;

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.streaming.api.transformations.StreamExchangeMode;
import org.apache.flink.streaming.runtime.partitioner.ForwardPartitioner;
import org.apache.flink.streaming.runtime.partitioner.RescalePartitioner;

/**
 * This mode decides the default {@link ResultPartitionType} of job edges. Note that this only
 * affects job edges which are {@link StreamExchangeMode#UNDEFINED}.
 */
@Internal
public enum GlobalStreamExchangeMode {
    // 所有的边都是 BLOCKING类型
    ALL_EDGES_BLOCKING,

    // 如果使用 ForwardPartitioner,则边是 PIPELINED_BOUNDED类型, 否则是 BLOCKING类型
    FORWARD_EDGES_PIPELINED,

    // 如果使用 ForwardPartitioner 或 RescalePartitioner,则边是 PIPELINED_BOUNDED类型, 否则是 BLOCKING类型
    POINTWISE_EDGES_PIPELINED,

    // 所有的边都是 PIPELINED_BOUNDED  类型
    ALL_EDGES_PIPELINED,

    // 所有的边都是 PIPELINED_APPROXIMATE 类型
    ALL_EDGES_PIPELINED_APPROXIMATE
}
