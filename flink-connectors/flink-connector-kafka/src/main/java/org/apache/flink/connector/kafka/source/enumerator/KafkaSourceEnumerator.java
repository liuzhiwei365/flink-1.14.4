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

package org.apache.flink.connector.kafka.source.enumerator;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.connector.source.SplitsAssignment;
import org.apache.flink.connector.kafka.source.KafkaSourceOptions;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.enumerator.subscriber.KafkaSubscriber;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.util.FlinkRuntimeException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** The enumerator class for Kafka source. */
@Internal
public class KafkaSourceEnumerator
        implements SplitEnumerator<KafkaPartitionSplit, KafkaSourceEnumState> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSourceEnumerator.class);
    private final KafkaSubscriber subscriber;
    private final OffsetsInitializer startingOffsetInitializer;
    private final OffsetsInitializer stoppingOffsetInitializer;
    private final Properties properties;
    private final long partitionDiscoveryIntervalMs;
    private final SplitEnumeratorContext<KafkaPartitionSplit> context;
    private final Boundedness boundedness;

    /** Partitions that have been assigned to readers. */
    private final Set<TopicPartition> assignedPartitions;

    /**
     * The discovered and initialized partition splits that are waiting for owner reader to be
     * ready.
     */
    // key 代表 子任务编号 ,  value 代表 该子任务编号下应该 处理的数据分片集合
    private final Map<Integer, Set<KafkaPartitionSplit>> pendingPartitionSplitAssignment;

    /** The consumer group id used for this KafkaSource. */
    private final String consumerGroupId;

    // Lazily instantiated or mutable fields.
    private KafkaConsumer<byte[], byte[]> consumer;
    private AdminClient adminClient;

    //如果周期性分区发现被禁用 并且初始化分区发现已完成, 则此标志将被标记为true
    private boolean noMoreNewPartitionSplits = false;

    public KafkaSourceEnumerator(
            KafkaSubscriber subscriber,
            OffsetsInitializer startingOffsetInitializer,
            OffsetsInitializer stoppingOffsetInitializer,
            Properties properties,
            SplitEnumeratorContext<KafkaPartitionSplit> context,
            Boundedness boundedness) {
        this(
                subscriber,
                startingOffsetInitializer,
                stoppingOffsetInitializer,
                properties,
                context,
                boundedness,
                Collections.emptySet());
    }

    public KafkaSourceEnumerator(
            KafkaSubscriber subscriber,
            OffsetsInitializer startingOffsetInitializer,
            OffsetsInitializer stoppingOffsetInitializer,
            Properties properties,
            SplitEnumeratorContext<KafkaPartitionSplit> context,
            Boundedness boundedness,
            Set<TopicPartition> assignedPartitions) {
        this.subscriber = subscriber;
        this.startingOffsetInitializer = startingOffsetInitializer;
        this.stoppingOffsetInitializer = stoppingOffsetInitializer;
        this.properties = properties;
        this.context = context;
        this.boundedness = boundedness;

        this.assignedPartitions = new HashSet<>(assignedPartitions);
        this.pendingPartitionSplitAssignment = new HashMap<>();
        this.partitionDiscoveryIntervalMs =
                KafkaSourceOptions.getOption(
                        properties,
                        KafkaSourceOptions.PARTITION_DISCOVERY_INTERVAL_MS,
                        Long::parseLong);
        this.consumerGroupId = properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG);
    }

    /**
     * Start the enumerator.
     *
     * <p>Depending on {@link #partitionDiscoveryIntervalMs}, the enumerator will trigger a one-time
     * partition discovery, or schedule a callable for discover partitions periodically.
     *
     * <p>The invoking chain of partition discovery would be:
     *
     * <ol>
     *   <li>{@link #getSubscribedTopicPartitions} in worker thread
     *   <li>{@link #checkPartitionChanges} in coordinator thread
     *   <li>{@link #initializePartitionSplits} in worker thread
     *   <li>{@link #handlePartitionSplitChanges} in coordinator thread
     * </ol>
     */
    @Override
    public void start() {
        consumer = getKafkaConsumer();
        adminClient = getKafkaAdminClient();

        // getSubscribedTopicPartitions 是核心
        if (partitionDiscoveryIntervalMs > 0) {
            LOG.info(
                    "Starting the KafkaSourceEnumerator for consumer group {} "
                            + "with partition discovery interval of {} ms.",
                    consumerGroupId,
                    partitionDiscoveryIntervalMs);
            context.callAsync(
                    this::getSubscribedTopicPartitions, // 正常执行逻辑
                    this::checkPartitionChanges, // 如果有结果拉回，或者有异常，该方法处理
                    0, //初始延迟执行 的 时间段
                    partitionDiscoveryIntervalMs); // 周期性时间段
        } else {
            LOG.info(
                    "Starting the KafkaSourceEnumerator for consumer group {} "
                            + "without periodic partition discovery.",
                    consumerGroupId);
            context.callAsync(this::getSubscribedTopicPartitions, this::checkPartitionChanges);
        }
    }

    @Override
    public void handleSplitRequest(int subtaskId, @Nullable String requesterHostname) {
        // the kafka source pushes splits eagerly, rather than act upon split requests
    }

    @Override
    public void addSplitsBack(List<KafkaPartitionSplit> splits, int subtaskId) {
        addPartitionSplitChangeToPendingAssignments(splits);

        // If the failed subtask has already restarted, we need to assign pending splits to it
        if (context.registeredReaders().containsKey(subtaskId)) {
            assignPendingPartitionSplits(Collections.singleton(subtaskId));
        }
    }

    @Override
    public void addReader(int subtaskId) {
        LOG.debug(
                "Adding reader {} to KafkaSourceEnumerator for consumer group {}.",
                subtaskId,
                consumerGroupId);
        assignPendingPartitionSplits(Collections.singleton(subtaskId));
    }

    @Override
    public KafkaSourceEnumState snapshotState(long checkpointId) throws Exception {
        return new KafkaSourceEnumState(assignedPartitions);
    }

    @Override
    public void close() {
        if (consumer != null) {
            consumer.close();
        }
        if (adminClient != null) {
            adminClient.close();
        }
    }

    // ----------------- private methods -------------------

    /**
     * List subscribed topic partitions on Kafka brokers.
     *
     * <p>NOTE: This method should only be invoked in the worker executor thread, because it
     * requires network I/O with Kafka brokers.
     *
     * @return Set of subscribed {@link TopicPartition}s
     */
    private Set<TopicPartition> getSubscribedTopicPartitions() {
        return subscriber.getSubscribedTopicPartitions(adminClient);
    }

    /**
     * Check if there's any partition changes within subscribed topic partitions fetched by worker
     * thread, and invoke {@link KafkaSourceEnumerator#initializePartitionSplits(PartitionChange)}
     * in worker thread to initialize splits for new partitions.
     *
     * <p>NOTE: This method should only be invoked in the coordinator executor thread.
     *
     * @param fetchedPartitions Map from topic name to its description
     * @param t Exception in worker thread
     */
    private void checkPartitionChanges(Set<TopicPartition> fetchedPartitions, Throwable t) {
        if (t != null) {
            throw new FlinkRuntimeException(
                    "Failed to list subscribed topic partitions due to ", t);
        }
        //  PartitionChange 封装了新增 的 即将移除 的  TopicPartition 集合
        final PartitionChange partitionChange = getPartitionChange(fetchedPartitions);
        if (partitionChange.isEmpty()) {
            return;
        }
        context.callAsync(
                () -> initializePartitionSplits(partitionChange), // 把新增的 TopicPartition 集合 的 偏移量信息填充好
                this::handlePartitionSplitChanges);
    }

    /**
     * Initialize splits for newly discovered partitions.
     *
     * <p>Enumerator will be responsible for fetching offsets when initializing splits if:
     *
     * <ul>
     *   <li>using timestamp for initializing offset
     *   <li>or using specified offset, but the offset is not provided for the newly discovered
     *       partitions
     * </ul>
     *
     * <p>Otherwise offsets will be initialized by readers.
     *
     * <p>NOTE: This method should only be invoked in the worker executor thread, because it
     * potentially requires network I/O with Kafka brokers for fetching offsets.
     *
     * @param partitionChange Newly discovered and removed partitions
     * @return {@link KafkaPartitionSplit} of new partitions and {@link TopicPartition} of removed
     *     partitions
     */
    private PartitionSplitChange initializePartitionSplits(PartitionChange partitionChange) {
        Set<TopicPartition> newPartitions =
                Collections.unmodifiableSet(partitionChange.getNewPartitions());

        // 拿到分区偏移量 寻回器
        OffsetsInitializer.PartitionOffsetsRetriever offsetsRetriever = getOffsetsRetriever();

        // 拿到每个新增分区的偏移量, 并最终封装为 KafkaPartitionSplit 对象

        // 请关注 KafkaSourceBuilder 的构造方法
        // startingOffsetInitializer 和 stoppingOffsetInitializer 最开始在 构造方法中创建并传入
        // startingOffsetInitializer  可以是以下三个实现
        //       ReaderHandledOffsetsInitializer  （默认）
        //       SpecifiedOffsetsInitializer
        //       TimestampOffsetsInitializer
        // stoppingOffsetInitializer  的实现
        //       NoStoppingOffsetsInitializer   （默认）
        Map<TopicPartition, Long> startingOffsets =
                startingOffsetInitializer.getPartitionOffsets(newPartitions, offsetsRetriever);
        Map<TopicPartition, Long> stoppingOffsets =
                stoppingOffsetInitializer.getPartitionOffsets(newPartitions, offsetsRetriever);

        Set<KafkaPartitionSplit> partitionSplits = new HashSet<>(newPartitions.size());
        for (TopicPartition tp : newPartitions) {
            Long startingOffset = startingOffsets.get(tp);
            long stoppingOffset =
                    stoppingOffsets.getOrDefault(tp, KafkaPartitionSplit.NO_STOPPING_OFFSET);
            partitionSplits.add(new KafkaPartitionSplit(tp, startingOffset, stoppingOffset));
        }
        return new PartitionSplitChange(partitionSplits, partitionChange.getRemovedPartitions());
    }

    /**
     * Mark partition splits initialized by {@link
     * KafkaSourceEnumerator#initializePartitionSplits(PartitionChange)} as pending and try to
     * assign pending splits to registered readers.
     *
     * <p>NOTE: This method should only be invoked in the coordinator executor thread.
     *
     * @param partitionSplitChange Partition split changes
     * @param t Exception in worker thread
     */
    private void handlePartitionSplitChanges(
            PartitionSplitChange partitionSplitChange, Throwable t) {
        if (t != null) {
            throw new FlinkRuntimeException("Failed to initialize partition splits due to ", t);
        }
        if (partitionDiscoveryIntervalMs < 0) {
            LOG.debug("Partition discovery is disabled.");
            noMoreNewPartitionSplits = true;
        }
        // TODO: Handle removed partitions.
        // 把新增的 分片信息集合 分配给不同的 子任务编号
        addPartitionSplitChangeToPendingAssignments(partitionSplitChange.newPartitionSplits);
        // 入参 就是 子任务集合,
        assignPendingPartitionSplits(context.registeredReaders().keySet());
    }

    // This method should only be invoked in the coordinator executor thread.
    // 把新增的 分片信息集合 分配给不同的 子任务编号
    private void addPartitionSplitChangeToPendingAssignments(
            Collection<KafkaPartitionSplit> newPartitionSplits) {

        int numReaders = context.currentParallelism();

        for (KafkaPartitionSplit split : newPartitionSplits) {

            // 计算该分片属于那个并行度
            int ownerReader = getSplitOwner(split.getTopicPartition(), numReaders);

            // 把本分片信息  放入 pendingPartitionSplitAssignment  集合中
            pendingPartitionSplitAssignment
                    .computeIfAbsent(ownerReader, r -> new HashSet<>())
                    .add(split);
        }
        LOG.debug(
                "Assigned {} to {} readers of consumer group {}.",
                newPartitionSplits,
                numReaders,
                consumerGroupId);
    }

    // This method should only be invoked in the coordinator executor thread.
    private void assignPendingPartitionSplits(Set<Integer> pendingReaders) {
        Map<Integer, List<KafkaPartitionSplit>> incrementalAssignment = new HashMap<>();

        // Check if there's any pending splits for given readers
        for (int pendingReader : pendingReaders) {
            checkReaderRegistered(pendingReader);

            // Remove pending assignment for the reader
            final Set<KafkaPartitionSplit> pendingAssignmentForReader =
                    pendingPartitionSplitAssignment.remove(pendingReader);

            if (pendingAssignmentForReader != null && !pendingAssignmentForReader.isEmpty()) {
                // Put pending assignment into incremental assignment
                incrementalAssignment
                        .computeIfAbsent(pendingReader, (ignored) -> new ArrayList<>())
                        .addAll(pendingAssignmentForReader);

                // Mark pending partitions as already assigned
                pendingAssignmentForReader.forEach(
                        split -> assignedPartitions.add(split.getTopicPartition()));
            }
        }

        // Assign pending splits to readers
        if (!incrementalAssignment.isEmpty()) {
            LOG.info("Assigning splits to readers {}", incrementalAssignment);
            context.assignSplits(new SplitsAssignment<>(incrementalAssignment));
        }

        //如果周期性分区发现被禁用 并且初始化分区发现已完成, 并且是有界流
        if (noMoreNewPartitionSplits && boundedness == Boundedness.BOUNDED) {
            LOG.debug(
                    "No more KafkaPartitionSplits to assign. Sending NoMoreSplitsEvent to reader {}"
                            + " in consumer group {}.",
                    pendingReaders,
                    consumerGroupId);

            // 向每个 subtask 发送 NoMoreSplitsEvent 消息
            pendingReaders.forEach(context::signalNoMoreSplits);
        }
    }

    private void checkReaderRegistered(int readerId) {
        if (!context.registeredReaders().containsKey(readerId)) {
            throw new IllegalStateException(
                    String.format("Reader %d is not registered to source coordinator", readerId));
        }
    }

    @VisibleForTesting
    PartitionChange getPartitionChange(Set<TopicPartition> fetchedPartitions) {
        final Set<TopicPartition> removedPartitions = new HashSet<>();
        Consumer<TopicPartition> dedupOrMarkAsRemoved =
                (tp) -> {
                    if (!fetchedPartitions.remove(tp)) {
                        removedPartitions.add(tp);
                    }
                };


        // assignedPartitions 与 fetchedPartitions 的差集 添加到 removedPartitions
        // fetchedPartitions 与 assignedPartitions 的差集  用来更新成新的 fetchedPartitions
        assignedPartitions.forEach(dedupOrMarkAsRemoved);

        // 和上一行代码逻辑类似
        pendingPartitionSplitAssignment.forEach(
                (reader, splits) ->
                        splits.forEach(
                                split -> dedupOrMarkAsRemoved.accept(split.getTopicPartition())));

        if (!fetchedPartitions.isEmpty()) {
            LOG.info("Discovered new partitions: {}", fetchedPartitions);
        }
        if (!removedPartitions.isEmpty()) {
            LOG.info("Discovered removed partitions: {}", removedPartitions);
        }

        // 到目前为止，现在 fetchedPartitions里面的元素 是发现的新的 分区, removedPartitions里面的元素 是发现的要被删除的 老的 分区
        return new PartitionChange(fetchedPartitions, removedPartitions);
    }

    private KafkaConsumer<byte[], byte[]> getKafkaConsumer() {
        Properties consumerProps = new Properties();
        deepCopyProperties(properties, consumerProps);
        // set client id prefix
        String clientIdPrefix =
                consumerProps.getProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key());
        consumerProps.setProperty(
                ConsumerConfig.CLIENT_ID_CONFIG, clientIdPrefix + "-enumerator-consumer");
        consumerProps.setProperty(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName());
        consumerProps.setProperty(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName());
        // Disable auto topic creation.
        consumerProps.setProperty(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        return new KafkaConsumer<>(consumerProps);
    }

    private AdminClient getKafkaAdminClient() {
        Properties adminClientProps = new Properties();
        deepCopyProperties(properties, adminClientProps);
        // set client id prefix
        String clientIdPrefix =
                adminClientProps.getProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key());
        adminClientProps.setProperty(
                ConsumerConfig.CLIENT_ID_CONFIG, clientIdPrefix + "-enumerator-admin-client");
        return AdminClient.create(adminClientProps);
    }

    private OffsetsInitializer.PartitionOffsetsRetriever getOffsetsRetriever() {
        String groupId = properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG);
        return new PartitionOffsetsRetrieverImpl(consumer, adminClient, groupId);
    }

    /**
     * Returns the index of the target subtask that a specific Kafka partition should be assigned
     * to.
     *
     * <p>The resulting distribution of partitions of a single topic has the following contract:
     *
     * <ul>
     *   <li>1. Uniformly distributed across subtasks
     *   <li>2. Partitions are round-robin distributed (strictly clockwise w.r.t. ascending subtask
     *       indices) by using the partition id as the offset from a starting index (i.e., the index
     *       of the subtask which partition 0 of the topic will be assigned to, determined using the
     *       topic name).
     * </ul>
     *
     * @param tp the Kafka partition to assign.
     * @param numReaders the total number of readers.
     * @return the id of the subtask that owns the split.
     */
    @VisibleForTesting
    static int getSplitOwner(TopicPartition tp, int numReaders) {
        int startIndex = ((tp.topic().hashCode() * 31) & 0x7FFFFFFF) % numReaders;

        // here, the assumption is that the id of Kafka partitions are always ascending
        // starting from 0, and therefore can be used directly as the offset clockwise from the
        // start index
        return (startIndex + tp.partition()) % numReaders;
    }

    @VisibleForTesting
    static void deepCopyProperties(Properties from, Properties to) {
        for (String key : from.stringPropertyNames()) {
            to.setProperty(key, from.getProperty(key));
        }
    }

    // --------------- private class ---------------

    /** A container class to hold the newly added partitions and removed partitions. */
    @VisibleForTesting
    static class PartitionChange {
        // 发现的新的分区
        private final Set<TopicPartition> newPartitions;
        // 要被删除的老的分区
        private final Set<TopicPartition> removedPartitions;

        PartitionChange(Set<TopicPartition> newPartitions, Set<TopicPartition> removedPartitions) {
            this.newPartitions = newPartitions;
            this.removedPartitions = removedPartitions;
        }

        public Set<TopicPartition> getNewPartitions() {
            return newPartitions;
        }

        public Set<TopicPartition> getRemovedPartitions() {
            return removedPartitions;
        }

        public boolean isEmpty() {
            return newPartitions.isEmpty() && removedPartitions.isEmpty();
        }
    }

    private static class PartitionSplitChange {
        // 新 增加的 分片
        private final Set<KafkaPartitionSplit> newPartitionSplits;
        // 新 移除的 分片
        private final Set<TopicPartition> removedPartitions;

        private PartitionSplitChange(
                Set<KafkaPartitionSplit> newPartitionSplits,
                Set<TopicPartition> removedPartitions) {
            this.newPartitionSplits = Collections.unmodifiableSet(newPartitionSplits);
            this.removedPartitions = Collections.unmodifiableSet(removedPartitions);
        }
    }

    /** The implementation for offsets retriever with a consumer and an admin client. */
    @VisibleForTesting
    public static class PartitionOffsetsRetrieverImpl
            implements OffsetsInitializer.PartitionOffsetsRetriever, AutoCloseable {
        private final KafkaConsumer<?, ?> consumer;
        private final AdminClient adminClient;
        private final String groupId;

        public PartitionOffsetsRetrieverImpl(
                KafkaConsumer<?, ?> consumer, AdminClient adminClient, String groupId) {
            this.consumer = consumer;
            this.adminClient = adminClient;
            this.groupId = groupId;
        }

        @Override
        public Map<TopicPartition, Long> committedOffsets(Collection<TopicPartition> partitions) {
            ListConsumerGroupOffsetsOptions options =
                    new ListConsumerGroupOffsetsOptions()
                            .topicPartitions(new ArrayList<>(partitions));
            try {
                return adminClient
                        .listConsumerGroupOffsets(groupId, options)
                        .partitionsToOffsetAndMetadata()
                        .thenApply(
                                result -> {
                                    Map<TopicPartition, Long> offsets = new HashMap<>();
                                    result.forEach(
                                            (tp, oam) -> {
                                                if (oam != null) {
                                                    offsets.put(tp, oam.offset());
                                                }
                                            });
                                    return offsets;
                                })
                        .get();
            } catch (InterruptedException e) {
                throw new FlinkRuntimeException(
                        "Interrupted while listing offsets for consumer group " + groupId, e);
            } catch (ExecutionException e) {
                throw new FlinkRuntimeException(
                        "Failed to fetch committed offsets for consumer group "
                                + groupId
                                + " due to",
                        e);
            }
        }

        @Override
        public Map<TopicPartition, Long> endOffsets(Collection<TopicPartition> partitions) {
            return consumer.endOffsets(partitions);
        }

        @Override
        public Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions) {
            return consumer.beginningOffsets(partitions);
        }

        @Override
        public Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(
                Map<TopicPartition, Long> timestampsToSearch) {
            return consumer.offsetsForTimes(timestampsToSearch);
        }

        @Override
        public void close() throws Exception {
            consumer.close(Duration.ZERO);
            adminClient.close(Duration.ZERO);
        }
    }
}
