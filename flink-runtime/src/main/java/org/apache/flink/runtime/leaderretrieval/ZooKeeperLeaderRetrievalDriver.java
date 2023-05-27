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

package org.apache.flink.runtime.leaderretrieval;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.leaderelection.LeaderInformation;
import org.apache.flink.runtime.leaderelection.ZooKeeperLeaderElectionDriver;
import org.apache.flink.runtime.rpc.FatalErrorHandler;
import org.apache.flink.runtime.util.ZooKeeperUtils;
import org.apache.flink.util.ExceptionUtils;

import org.apache.flink.shaded.curator4.org.apache.curator.framework.CuratorFramework;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.state.ConnectionState;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.state.ConnectionStateListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.UUID;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * The counterpart to the {@link ZooKeeperLeaderElectionDriver}. {@link LeaderRetrievalService}
 * implementation for Zookeeper. It retrieves the current leader which has been elected by the
 * {@link ZooKeeperLeaderElectionDriver}. The leader address as well as the current leader session
 * ID is retrieved from ZooKeeper.
 */
public class ZooKeeperLeaderRetrievalDriver implements LeaderRetrievalDriver {
    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperLeaderRetrievalDriver.class);

    /** Connection to the used ZooKeeper quorum. */
    private final CuratorFramework client;

    /** Curator recipe to watch changes of a specific ZooKeeper node. */
    private final TreeCache cache;

    private final String connectionInformationPath;

    private final ConnectionStateListener connectionStateListener =
            (client, newState) -> handleStateChange(newState);

    private final LeaderRetrievalEventHandler leaderRetrievalEventHandler;

    private final LeaderInformationClearancePolicy leaderInformationClearancePolicy;

    private final FatalErrorHandler fatalErrorHandler;

    private volatile boolean running;

    /**
     * Creates a leader retrieval service which uses ZooKeeper to retrieve the leader information.
     *
     * @param client Client which constitutes the connection to the ZooKeeper quorum
     * @param path Path of the ZooKeeper node which contains the leader information
     * @param leaderRetrievalEventHandler Handler to notify the leader changes.
     * @param leaderInformationClearancePolicy leaderInformationClearancePolicy controls when the
     *     leader information is being cleared
     * @param fatalErrorHandler Fatal error handler
     */
    public ZooKeeperLeaderRetrievalDriver(
            CuratorFramework client, // zk 的客户端
            String path,
            LeaderRetrievalEventHandler leaderRetrievalEventHandler,
            LeaderInformationClearancePolicy leaderInformationClearancePolicy,
            FatalErrorHandler fatalErrorHandler)
            throws Exception {
        this.client = checkNotNull(client, "CuratorFramework client");
        this.connectionInformationPath = ZooKeeperUtils.generateConnectionInformationPath(path);

        // 调用zk curator 的api 来创建 TreeCache 对象; 而该对象 正好是专门监听zk服务器上 指定路径
        this.cache =
                ZooKeeperUtils.createTreeCache(
                        client,
                        connectionInformationPath,
                        this::retrieveLeaderInformationFromZooKeeper);

        this.leaderRetrievalEventHandler = checkNotNull(leaderRetrievalEventHandler);
        this.leaderInformationClearancePolicy = leaderInformationClearancePolicy;
        this.fatalErrorHandler = checkNotNull(fatalErrorHandler);

        cache.start();

        // 当zk服务器上 指定路径变化后 , 会触发监听器 调用 stateChanged方法
        // ConnectionStateListener 类只有一个抽象方法; 所以它是一个函数式接口
        // 所以 (client, newState) -> handleStateChange(newState) 是 stateChanged方法的匿名写法

        // 所以  当zk服务器上 指定路径变化后 , 监听器 会调用本类（驱动类）的 handleStateChange 方法
        client.getConnectionStateListenable().addListener(connectionStateListener);

        running = true;
    }

    @Override
    public void close() throws Exception {
        if (!running) {
            return;
        }

        running = false;

        LOG.info("Closing {}.", this);

        client.getConnectionStateListenable().removeListener(connectionStateListener);

        cache.close();
    }

    // zk的客户端在 创建 TreeCache 的时候,会把本方法当做回调逻辑传入 ,于是zk节点信息发生变化的时候,会调用本方法寻回 leader的信息
    // 从zk 服务器 寻回新的主节点 信息
    private void retrieveLeaderInformationFromZooKeeper() {
        try {
            LOG.debug("Leader node has changed.");

            final ChildData childData = cache.getCurrentData(connectionInformationPath);

            if (childData != null) {
                final byte[] data = childData.getData();
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    final String leaderAddress = ois.readUTF();
                    final UUID leaderSessionID = (UUID) ois.readObject();

                    // DefaultLeaderRetrievalService.notifyLeaderAddress

                    // 如果 场景是 TaskExecutor 寻回 ResourceManager 则调用栈如下
                    // TaskExecutor.ResourceManagerLeaderListener.notifyLeaderAddress
                    // TaskExecutor.notifyOfNewResourceManagerLeader
                    // org.apache.flink.runtime.taskexecutor.TaskExecutor.reconnectToResourceManager

                    // 最终会 利用zk服务器寻回新的ResourceManager  ,重新连接 新的ResourceManager
                    leaderRetrievalEventHandler.notifyLeaderAddress(
                            LeaderInformation.known(leaderSessionID, leaderAddress));
                    return;
                }
            }
            notifyNoLeader();
        } catch (Exception e) {
            fatalErrorHandler.onFatalError(
                    new LeaderRetrievalException("Could not handle node changed event.", e));
            ExceptionUtils.checkInterrupted(e);
        }
    }

    // 监听器监听路径 发生变化的时候,会被调用
    private void handleStateChange(ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
                LOG.debug("Connected to ZooKeeper quorum. Leader retrieval can start.");
                break;
            case SUSPENDED:
                LOG.warn("Connection to ZooKeeper suspended, waiting for reconnection.");
                if (leaderInformationClearancePolicy
                        == LeaderInformationClearancePolicy.ON_SUSPENDED_CONNECTION) {
                    notifyNoLeader();
                }
                break;
            case RECONNECTED: // 监听的主节点发生变化, 调用重连 逻辑
                LOG.info(
                        "Connection to ZooKeeper was reconnected. Leader retrieval can be restarted.");
                onReconnectedConnectionState();
                break;
            case LOST:
                LOG.warn(
                        "Connection to ZooKeeper lost. Can no longer retrieve the leader from "
                                + "ZooKeeper.");
                notifyNoLeader();
                break;
        }
    }

    private void notifyNoLeader() {
        leaderRetrievalEventHandler.notifyLeaderAddress(LeaderInformation.empty());
    }

    private void onReconnectedConnectionState() {
        // check whether we find some new leader information in ZooKeeper
        retrieveLeaderInformationFromZooKeeper();
    }

    @Override
    public String toString() {
        return "ZookeeperLeaderRetrievalDriver{"
                + "connectionInformationPath='"
                + connectionInformationPath
                + '\''
                + '}';
    }

    @VisibleForTesting
    public String getConnectionInformationPath() {
        return connectionInformationPath;
    }

    /** Policy when to clear the leader information and to notify the listener about it. */
    public enum LeaderInformationClearancePolicy {
        // clear the leader information as soon as the ZK connection is suspended
        ON_SUSPENDED_CONNECTION,

        // clear the leader information only once the ZK connection is lost
        ON_LOST_CONNECTION
    }
}
