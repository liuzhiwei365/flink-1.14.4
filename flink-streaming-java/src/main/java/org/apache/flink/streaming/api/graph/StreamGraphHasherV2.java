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

package org.apache.flink.streaming.api.graph;

import org.apache.flink.api.dag.Transformation;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.streaming.api.operators.UdfStreamOperatorFactory;

import org.apache.flink.shaded.guava30.com.google.common.hash.HashFunction;
import org.apache.flink.shaded.guava30.com.google.common.hash.Hasher;
import org.apache.flink.shaded.guava30.com.google.common.hash.Hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.apache.flink.util.StringUtils.byteToHexString;

/**
 * StreamGraphHasher from Flink 1.2. This contains duplicated code to ensure that the algorithm does
 * not change with future Flink versions.
 *
 * <p>DO NOT MODIFY THIS CLASS
 */
public class StreamGraphHasherV2 implements StreamGraphHasher {

    private static final Logger LOG = LoggerFactory.getLogger(StreamGraphHasherV2.class);

    /**
     * Returns a map with a hash for each {@link StreamNode} of the {@link StreamGraph}. The hash is
     * used as the {@link JobVertexID} in order to identify nodes across job submissions if they
     * didn't change.
     *
     * <p>The complete {@link StreamGraph} is traversed. The hash is either computed from the
     * transformation's user-specified id (see {@link Transformation#getUid()}) or generated in a
     * deterministic way.
     *
     * <p>The generated hash is deterministic with respect to:
     *
     * <ul>
     *   <li>node-local properties (node ID),
     *   <li>chained output nodes, and
     *   <li>input nodes hashes
     * </ul>
     *
     * @return A map from {@link StreamNode#id} to hash as 16-byte array.
     */
    @Override
    public Map<Integer, byte[]> traverseStreamGraphAndGenerateHashes(StreamGraph streamGraph) {
        // 返回  Murmur3_128HashFunction 方法
        final HashFunction hashFunction = Hashing.murmur3_128(0);

        // 存储每个顶点 id 对应 的hash 值
        final Map<Integer, byte[]> hashes = new HashMap<>();

        Set<Integer> visited = new HashSet<>();
        // 双向队列
        // 该算法中 用队列来维护全局顺序
        Queue<StreamNode> remaining = new ArrayDeque<>();

        // 我们需要使source 顺序具有确定性。source id 的返回顺序不同, 这意味着两次提交同一个程序
        // 可能会导致不同的遍历, 从而破坏确定性哈希分配 (用排序来保证 源的顺序确定 ; 用队列保证全局顺序确定)
        List<Integer> sources = new ArrayList<>();
        for (Integer sourceNodeId : streamGraph.getSourceIDs()) {
            sources.add(sourceNodeId);
        }
        Collections.sort(sources);

        //以广度优先的方式遍历图形。请记住该图不是一个树,并且可以存在多条路径指向一个节点
        // Start with source nodes
        for (Integer sourceNodeId : sources) {
            remaining.add(streamGraph.getStreamNode(sourceNodeId));
            visited.add(sourceNodeId);
        }

        StreamNode currentNode;
        while ((currentNode = remaining.poll()) != null) {

            // Generate the hash code. Because multiple path exist to each
            // node, we might not have all required inputs available to
            // generate the hash code.

            if (generateNodeHash(
                    currentNode,
                    hashFunction,
                    hashes,
                    streamGraph.isChainingEnabled(),
                    streamGraph)) {
                // Add the child nodes
                for (StreamEdge outEdge : currentNode.getOutEdges()) {
                    StreamNode child = streamGraph.getTargetVertex(outEdge);

                    if (!visited.contains(child.getId())) {
                        remaining.add(child);
                        visited.add(child.getId());
                    }
                }
            } else {
                // We will revisit this later.
                visited.remove(currentNode.getId());
            }
        }

        return hashes;
    }

    /**
     * Generates a hash for the node and returns whether the operation was successful.
     *
     * @param node The node to generate the hash for
     * @param hashFunction The hash function to use
     * @param hashes The current state of generated hashes
     * @return <code>true</code> if the node hash has been generated. <code>false</code>, otherwise.
     *     If the operation is not successful, the hash needs be generated at a later point when all
     *     input is available.
     * @throws IllegalStateException If node has user-specified hash and is intermediate node of a
     *     chain
     */
    private boolean generateNodeHash(
            StreamNode node,
            HashFunction hashFunction,
            Map<Integer, byte[]> hashes,
            boolean isChainingEnabled,
            StreamGraph streamGraph) {

        // 检查用户是否指定了 transformation uid
        String userSpecifiedHash = node.getTransformationUID();

        if (userSpecifiedHash == null) {
            // 如果用户没有指定 transformation uid

            // 在给每个node 分配 hash值时,先要检查其 所有的依赖的 node 是否已经分配,如果存在有一个没有分配就返回 false
            for (StreamEdge inEdge : node.getInEdges()) {
                // If the input node has not been visited yet, the current
                // node will be visited again at a later point when all input
                // nodes have been visited and their hashes set.
                if (!hashes.containsKey(inEdge.getSourceId())) {
                    return false;
                }
            }

            Hasher hasher = hashFunction.newHasher();
            // 考虑众多因素 以生成唯一的 hash码
            byte[] hash =
                    generateDeterministicHash(node, hasher, hashes, isChainingEnabled, streamGraph);

            if (hashes.put(node.getId(), hash) != null) {
                // Sanity check
                throw new IllegalStateException(
                        "Unexpected state. Tried to add node hash "
                                + "twice. This is probably a bug in the JobGraph generator.");
            }

            return true;
        } else {
            Hasher hasher = hashFunction.newHasher();
            byte[] hash = generateUserSpecifiedHash(node, hasher);

            for (byte[] previousHash : hashes.values()) {
                if (Arrays.equals(previousHash, hash)) {
                    throw new IllegalArgumentException(
                            "Hash collision on user-specified ID "
                                    + "\""
                                    + userSpecifiedHash
                                    + "\". "
                                    + "Most likely cause is a non-unique ID. Please check that all IDs "
                                    + "specified via `uid(String)` are unique.");
                }
            }

            if (hashes.put(node.getId(), hash) != null) {
                // Sanity check
                throw new IllegalStateException(
                        "Unexpected state. Tried to add node hash "
                                + "twice. This is probably a bug in the JobGraph generator.");
            }

            return true;
        }
    }

    /** Generates a hash from a user-specified ID. */
    private byte[] generateUserSpecifiedHash(StreamNode node, Hasher hasher) {
        hasher.putString(node.getTransformationUID(), Charset.forName("UTF-8"));

        return hasher.hash().asBytes();
    }

    /** Generates a deterministic hash from node-local properties and input and output edges. */
    private byte[] generateDeterministicHash(
            StreamNode node,
            Hasher hasher,
            Map<Integer, byte[]> hashes,
            boolean isChainingEnabled,
            StreamGraph streamGraph) {

        // Include stream node to hash. We use the current size of the computed
        // hashes as the ID. We cannot use the node's ID, because it is
        // assigned from a static counter. This will result in two identical
        // programs having different hashes.
        generateNodeLocalHash(hasher, hashes.size());

        // Include chained nodes to hash
        for (StreamEdge outEdge : node.getOutEdges()) {
            if (isChainable(outEdge, isChainingEnabled, streamGraph)) {

                // Use the hash size again, because the nodes are chained to
                // this node. This does not add a hash for the chained nodes.
                generateNodeLocalHash(hasher, hashes.size());
            }
        }

        // 将hash 码 转成字节数组
        byte[] hash = hasher.hash().asBytes();

        // Make sure that all input nodes have their hash set before entering
        // this loop (calling this method).
        for (StreamEdge inEdge : node.getInEdges()) {
            byte[] otherHash = hashes.get(inEdge.getSourceId());

            // Sanity check
            // 要确保 依赖的 input node 的hash已经生成
            if (otherHash == null) {
                throw new IllegalStateException(
                        "Missing hash for input node "
                                + streamGraph.getSourceVertex(inEdge)
                                + ". Cannot generate hash for "
                                + node
                                + ".");
            }

            // 要注意的是，该节点的哈希值还与该节点和下游节点能够 chain 在一起的个数有关
            // 最后还需要跟其上游所有节点的哈希值进行异或操作 保证 hash 唯一
            for (int j = 0; j < hash.length; j++) {
                hash[j] = (byte) (hash[j] * 37 ^ otherHash[j]);
            }
        }

        // debug 打印
        if (LOG.isDebugEnabled()) {
            String udfClassName = "";
            if (node.getOperatorFactory() instanceof UdfStreamOperatorFactory) {
                udfClassName =
                        ((UdfStreamOperatorFactory) node.getOperatorFactory())
                                .getUserFunctionClassName();
            }

            LOG.debug(
                    "Generated hash '"
                            + byteToHexString(hash)
                            + "' for node "
                            + "'"
                            + node.toString()
                            + "' {id: "
                            + node.getId()
                            + ", "
                            + "parallelism: "
                            + node.getParallelism()
                            + ", "
                            + "user function: "
                            + udfClassName
                            + "}");
        }

        return hash;
    }

    /**
     * Applies the {@link Hasher} to the {@link StreamNode} . The hasher encapsulates the current
     * state of the hash.
     *
     * <p>The specified ID is local to this node. We cannot use the {@link StreamNode#id}, because
     * it is incremented in a static counter. Therefore, the IDs for identical jobs will otherwise
     * be different.
     */
    private void generateNodeLocalHash(Hasher hasher, int id) {
        // This resolves conflicts for otherwise identical source nodes. BUT
        // the generated hash codes depend on the ordering of the nodes in the
        // stream graph.
        hasher.putInt(id);
    }

    private boolean isChainable(
            StreamEdge edge, boolean isChainingEnabled, StreamGraph streamGraph) {
        return isChainingEnabled && StreamingJobGraphGenerator.isChainable(edge, streamGraph);
    }
}
