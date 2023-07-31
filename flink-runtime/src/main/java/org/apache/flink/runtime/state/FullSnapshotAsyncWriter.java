/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state;

import org.apache.flink.api.common.typeutils.base.array.BytePrimitiveArraySerializer;
import org.apache.flink.core.fs.CloseableRegistry;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.core.memory.DataOutputViewStreamWrapper;
import org.apache.flink.runtime.checkpoint.CheckpointType;
import org.apache.flink.util.IOUtils;
import org.apache.flink.util.function.SupplierWithException;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import static org.apache.flink.runtime.state.FullSnapshotUtil.END_OF_KEY_GROUP_MARK;
import static org.apache.flink.runtime.state.FullSnapshotUtil.hasMetaDataFollowsFlag;
import static org.apache.flink.runtime.state.FullSnapshotUtil.setMetaDataFollowsFlagInKey;

/**
 * An asynchronous writer that can write a full snapshot/savepoint from a {@link
 * FullSnapshotResources}.
 *
 * @param <K> type of the backend keys.
 */
public class FullSnapshotAsyncWriter<K>
        implements SnapshotStrategy.SnapshotResultSupplier<KeyedStateHandle> {

    /** Supplier for the stream into which we write the snapshot. */
    @Nonnull
    private final SupplierWithException<CheckpointStreamWithResultProvider, Exception>
            checkpointStreamSupplier;

    @Nonnull private final FullSnapshotResources<K> snapshotResources;
    @Nonnull private final CheckpointType checkpointType;

    public FullSnapshotAsyncWriter(
            @Nonnull CheckpointType checkpointType,
            @Nonnull
                    SupplierWithException<CheckpointStreamWithResultProvider, Exception>
                            checkpointStreamSupplier,
            @Nonnull FullSnapshotResources<K> snapshotResources) {

        this.checkpointStreamSupplier = checkpointStreamSupplier;
        this.snapshotResources = snapshotResources;
        this.checkpointType = checkpointType;
    }

    // 核心 方法, 本类的其他方法都是围绕这个方法

    // 用输入流 来持久化  kv 类型的状态, 并且返回 kv类型 的状态句柄
    @Override
    public SnapshotResult<KeyedStateHandle> get(CloseableRegistry snapshotCloseableRegistry)
            throws Exception {

        final KeyGroupRangeOffsets keyGroupRangeOffsets =
                new KeyGroupRangeOffsets(snapshotResources.getKeyGroupRange());

        //  CheckpointStreamWithResultProvider 是用来创建 输出流的
        final CheckpointStreamWithResultProvider checkpointStreamWithResultProvider =
                checkpointStreamSupplier.get();

        snapshotCloseableRegistry.registerCloseable(checkpointStreamWithResultProvider);

        // 用输入流 来持久化  kv 类型的状态
        writeSnapshotToOutputStream(checkpointStreamWithResultProvider, keyGroupRangeOffsets);

        if (snapshotCloseableRegistry.unregisterCloseable(checkpointStreamWithResultProvider)) {

            // 其实 KeyGroupsSavepointStateHandle 是 KeyGroupsStateHandle 的子类
            // 它们除了 equals 方法 和 toString 方法逻辑不一样, 其他方法逻辑都一样
            final CheckpointStreamWithResultProvider.KeyedStateHandleFactory stateHandleFactory;
            if (checkpointType.isSavepoint()) {
                stateHandleFactory = KeyGroupsSavepointStateHandle::new;
            } else {
                stateHandleFactory = KeyGroupsStateHandle::new;
            }

            // closeAndFinalizeCheckpointStreamResult() 方法 返回的 SnapshotResult 对象中包含 StreamStateHandle 成员
            // 把 StreamStateHandle 类型 转化为 KeyedStateHandle 类型
            return CheckpointStreamWithResultProvider.toKeyedStateHandleSnapshotResult(
                    checkpointStreamWithResultProvider.closeAndFinalizeCheckpointStreamResult(),
                    keyGroupRangeOffsets,
                    stateHandleFactory);
        } else {
            throw new IOException("Stream is already unregistered/closed.");
        }
    }

    // CheckpointStreamWithResultProvider  用于获取输出流对象
    private void writeSnapshotToOutputStream(
            @Nonnull CheckpointStreamWithResultProvider checkpointStreamWithResultProvider,
            @Nonnull KeyGroupRangeOffsets keyGroupRangeOffsets)
            throws IOException, InterruptedException {

        final DataOutputView outputView =
                new DataOutputViewStreamWrapper(
                        checkpointStreamWithResultProvider.getCheckpointOutputStream());

        // 写 kv 状态的元数据
        writeKVStateMetaData(outputView);

        try (KeyValueStateIterator kvStateIterator = snapshotResources.createKVStateIterator()) {
            // 写 kv 状态的实际数据
            writeKVStateData(
                    kvStateIterator, checkpointStreamWithResultProvider, keyGroupRangeOffsets);
        }
    }

    private void writeKVStateMetaData(final DataOutputView outputView) throws IOException {

        KeyedBackendSerializationProxy<K> serializationProxy =
                new KeyedBackendSerializationProxy<>(
                        // TODO: this code assumes that writing a serializer is threadsafe, we
                        // should support to
                        // get a serialized form already at state registration time in the
                        // future
                        snapshotResources.getKeySerializer(),
                        snapshotResources.getMetaInfoSnapshots(),
                        !Objects.equals(
                                UncompressedStreamCompressionDecorator.INSTANCE,
                                snapshotResources.getStreamCompressionDecorator()));

        serializationProxy.write(outputView);
    }

    private void writeKVStateData(
            final KeyValueStateIterator mergeIterator,
            final CheckpointStreamWithResultProvider checkpointStreamWithResultProvider,
            final KeyGroupRangeOffsets keyGroupRangeOffsets)
            throws IOException, InterruptedException {

        byte[] previousKey = null;
        byte[] previousValue = null;
        DataOutputView kgOutView = null;
        OutputStream kgOutStream = null;
        CheckpointStreamFactory.CheckpointStateOutputStream checkpointOutputStream =
                checkpointStreamWithResultProvider.getCheckpointOutputStream();

        try {

            // 设置第一个 键组 作为 我们的 lookahead
            if (mergeIterator.isValid()) {
                // 设置 当前键组 在流中的偏移量  （这里的键组就是 键组range中 的 第一个键组）
                keyGroupRangeOffsets.setKeyGroupOffset(mergeIterator.keyGroup(), checkpointOutputStream.getPos());

                // 获取键组 输出流
                kgOutStream = snapshotResources.getStreamCompressionDecorator()
                                .decorateWithCompression(checkpointOutputStream);
                // 将键组 输出流包装成  视图
                kgOutView = new DataOutputViewStreamWrapper(kgOutStream);

                // 把 kv 状态的 id  作为元数据的一种 写入
                kgOutView.writeShort(mergeIterator.kvStateId());

                // request next k/v pair
                previousKey = mergeIterator.key();
                previousValue = mergeIterator.value();
                mergeIterator.next();
            }

            // 主循环逻辑: 按照  (key-group, kv-state) 的顺序写入 kv 对, 此外, 我们还得追踪 key group 在流中的偏移量
            while (mergeIterator.isValid()) {

                assert (!hasMetaDataFollowsFlag(previousKey));

                // set signal in first key byte that meta data will follow in the stream
                // after this k/v pair

                // 在该k/v对之后的流中元数据将跟随的第一个关键字节中设置信号
                if (mergeIterator.isNewKeyGroup() || mergeIterator.isNewKeyValueState()) {
                    checkInterrupted();
                    setMetaDataFollowsFlagInKey(previousKey);
                }

                // 写入 kv 对 到 kgOutView 中
                writeKeyValuePair(previousKey, previousValue, kgOutView);

                // 写入元数据, 如果需要的话
                if (mergeIterator.isNewKeyGroup()) {
                    // 写入上一个 kg 的 结束标记
                    kgOutView.writeShort(END_OF_KEY_GROUP_MARK);

                    kgOutStream.close();
                    // 追踪新的 key-group
                    keyGroupRangeOffsets.setKeyGroupOffset(
                            mergeIterator.keyGroup(), checkpointOutputStream.getPos());
                    // 获取键组 输出流
                    kgOutStream =snapshotResources.getStreamCompressionDecorator()
                                    .decorateWithCompression(checkpointOutputStream);
                    // 将键组 输出流包装成  视图
                    kgOutView = new DataOutputViewStreamWrapper(kgOutStream);

                    // 把 kv 状态的 id  作为元数据的一种 写入
                    kgOutView.writeShort(mergeIterator.kvStateId());

                } else if (mergeIterator.isNewKeyValueState()) {
                    // 把 kv 状态的 id  作为元数据的一种 写入
                    kgOutView.writeShort(mergeIterator.kvStateId());
                }

                // request next k/v pair
                previousKey = mergeIterator.key();
                previousValue = mergeIterator.value();
                mergeIterator.next();
            }


            //  结尾
            if (previousKey != null) {
                assert (!hasMetaDataFollowsFlag(previousKey));
                setMetaDataFollowsFlagInKey(previousKey);

                // 写最后一个 键组下 的最后一个 kv 对
                writeKeyValuePair(previousKey, previousValue, kgOutView);

                kgOutView.writeShort(END_OF_KEY_GROUP_MARK);

                kgOutStream.close();
                kgOutStream = null;
            }

        } finally {
            // this will just close the outer stream
            IOUtils.closeQuietly(kgOutStream);
        }
    }




    // 上面 writeKVStateData 方法中被调用
    private void writeKeyValuePair(byte[] key, byte[] value, DataOutputView out)
            throws IOException {
        BytePrimitiveArraySerializer.INSTANCE.serialize(key, out);
        BytePrimitiveArraySerializer.INSTANCE.serialize(value, out);
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("RocksDB snapshot interrupted.");
        }
    }
}
