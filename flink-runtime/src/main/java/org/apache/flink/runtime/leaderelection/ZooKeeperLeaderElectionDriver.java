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

package org.apache.flink.runtime.leaderelection;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.rpc.FatalErrorHandler;
import org.apache.flink.runtime.util.ZooKeeperUtils;
import org.apache.flink.util.ExceptionUtils;

import org.apache.flink.shaded.curator4.org.apache.curator.framework.CuratorFramework;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.state.ConnectionState;
import org.apache.flink.shaded.curator4.org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.flink.shaded.zookeeper3.org.apache.zookeeper.CreateMode;
import org.apache.flink.shaded.zookeeper3.org.apache.zookeeper.KeeperException;
import org.apache.flink.shaded.zookeeper3.org.apache.zookeeper.data.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * {@link LeaderElectionDriver} implementation for Zookeeper. The leading JobManager is elected
 * using ZooKeeper. The current leader's address as well as its leader session ID is published via
 * ZooKeeper.
 */
public class ZooKeeperLeaderElectionDriver implements LeaderElectionDriver, LeaderLatchListener {

    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperLeaderElectionDriver.class);

    /** Client to the ZooKeeper quorum. */
    private final CuratorFramework client;

    /** Curator recipe for leader election. */
    private final LeaderLatch leaderLatch;

    /** Curator recipe to watch a given ZooKeeper node for changes. */
    private final TreeCache cache;

    /** ZooKeeper path of the node which stores the current leader information. */
    private final String connectionInformationPath;

    private final String leaderLatchPath;

    private final ConnectionStateListener listener =
            (client, newState) -> handleStateChange(newState);

    private final LeaderElectionEventHandler leaderElectionEventHandler;

    private final FatalErrorHandler fatalErrorHandler;

    private final String leaderContenderDescription;

    private volatile boolean running;

    /**
     * Creates a ZooKeeperLeaderElectionDriver object.
     *
     * @param client Client which is connected to the ZooKeeper quorum
     * @param path ZooKeeper node path for the leader election
     * @param leaderElectionEventHandler Event handler for processing leader change events
     * @param fatalErrorHandler Fatal error handler
     * @param leaderContenderDescription Leader contender description
     */
    // 构造方法中的主要逻辑：
    // 1 创建TreeCache,利用TreeCache将本地的Leader信息写入到 zk
    // 2 创建LeaderLatch, 启动LeaderLatch 参与选举,当选举成功后,调用LeaderLatch中 listeners监听列表的所有监听器的isLeader方法
    //   失败,则调用所有监听器的 notLeader方法 (核心)
    //      本类ZooKeeperLeaderElectionDriver 会作为监听器注册给 LeaderLatch的 listers成员,所以选举成功后会调用本类的isLeader方法
    // 3 CuratorFramework 客户端会注册本类的 listener成员(作为监听器),当与zk的连接状态发生变化,打印日志
    public ZooKeeperLeaderElectionDriver(
            CuratorFramework client,
            String path,
            LeaderElectionEventHandler leaderElectionEventHandler,
            FatalErrorHandler fatalErrorHandler,
            String leaderContenderDescription)
            throws Exception {
        checkNotNull(path);
        this.client = checkNotNull(client);
        this.connectionInformationPath = ZooKeeperUtils.generateConnectionInformationPath(path);
        this.leaderElectionEventHandler = checkNotNull(leaderElectionEventHandler);
        this.fatalErrorHandler = checkNotNull(fatalErrorHandler);
        this.leaderContenderDescription = checkNotNull(leaderContenderDescription);

        leaderLatchPath = ZooKeeperUtils.generateLeaderLatchPath(path);
        /* Curator有两种leader选举的模式,分别是 LeaderSelector和 LeaderLatch
           前者是所有存活的客户端不间断的轮流做Leader,大同社会。
           后者是一旦选举 出Leader,除非有客户端挂掉重新触发选举，否则不会交出领导权。某党?
        */
        leaderLatch = new LeaderLatch(client, leaderLatchPath);
        this.cache =
                ZooKeeperUtils.createTreeCache(
                        client,
                        connectionInformationPath,
                        // 会用远程zk 的 "sessionId 和 地址" 与 DefaultLeaderElectionService 的本地成员 进行对比
                        // 如果不一致,则将本地的重新写入zk
                        this::retrieveLeaderInformationFromZooKeeper);

        running = true;

        // ZooKeeperLeaderElectionDriver是一个LeaderLatchListener，选举成功会调用isLeader方法，
        // 由leader变为非leader调用notLeader方法；并且还同时是NodeCacheListener，要通过NodeCache
        // 方式添加了监控当前节点变化，当监听的节点发生变化时，则调用nodeChanged方法
        leaderLatch.addListener(this);
        leaderLatch.start();

        cache.start();

        // 这里的ConnectionStateListener 可以认为是一个函数式接口, 会回调stateChanged  方法
        client.getConnectionStateListenable().addListener(listener);
    }

    @Override
    public void close() throws Exception {
        if (!running) {
            return;
        }
        running = false;

        LOG.info("Closing {}", this);

        client.getConnectionStateListenable().removeListener(listener);

        Exception exception = null;

        try {
            cache.close();
        } catch (Exception e) {
            exception = e;
        }

        try {
            leaderLatch.close();
        } catch (Exception e) {
            exception = ExceptionUtils.firstOrSuppressed(e, exception);
        }

        if (exception != null) {
            throw new Exception(
                    "Could not properly stop the ZooKeeperLeaderElectionDriver.", exception);
        }
    }

    @Override
    public boolean hasLeadership() {
        assert (running);
        return leaderLatch.hasLeadership();
    }

    @Override
    public void isLeader() {
        leaderElectionEventHandler.onGrantLeadership();
    }

    @Override
    public void notLeader() {
        leaderElectionEventHandler.onRevokeLeadership();
    }

    // 从zk 读取本Leader 的 sessionId 和 地址
    private void retrieveLeaderInformationFromZooKeeper() throws Exception {
        if (leaderLatch.hasLeadership()) {
            ChildData childData = cache.getCurrentData(connectionInformationPath);
            if (childData != null) {
                final byte[] data = childData.getData();
                if (data != null && data.length > 0) {
                    final ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    final ObjectInputStream ois = new ObjectInputStream(bais);

                    final String leaderAddress = ois.readUTF();
                    final UUID leaderSessionID = (UUID) ois.readObject();

                    // 会用远程zk 的 "sessionId 和 地址" 与 DefaultLeaderElectionService 的本地成员 进行对比
                    // 如果不一致,则将本地的重新写入zk
                    leaderElectionEventHandler.onLeaderInformationChange(
                            LeaderInformation.known(leaderSessionID, leaderAddress));
                    return;
                }
            }
            leaderElectionEventHandler.onLeaderInformationChange(LeaderInformation.empty());
        }
    }

    /** Writes the current leader's address as well the given leader session ID to ZooKeeper. */
    @Override
    public void writeLeaderInformation(LeaderInformation leaderInformation) {
        assert (running);
        // this method does not have to be synchronized because the curator framework client
        // is thread-safe. We do not write the empty data to ZooKeeper here. Because
        // check-leadership-and-update
        // is not a transactional operation. We may wrongly clear the data written by new leader.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Write leader information: {}.", leaderInformation);
        }
        if (leaderInformation.isEmpty()) {
            return;
        }

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeUTF(leaderInformation.getLeaderAddress());
            oos.writeObject(leaderInformation.getLeaderSessionID());

            oos.close();

            boolean dataWritten = false;

            while (!dataWritten && leaderLatch.hasLeadership()) {
                Stat stat = client.checkExists().forPath(connectionInformationPath);

                if (stat != null) {
                    long owner = stat.getEphemeralOwner();
                    long sessionID = client.getZookeeperClient().getZooKeeper().getSessionId();

                    if (owner == sessionID) {
                        try {
                            // 最终写到zk上面的是字节数组
                            client.setData().forPath(connectionInformationPath, baos.toByteArray());

                            dataWritten = true;
                        } catch (KeeperException.NoNodeException noNode) {
                            // node was deleted in the meantime
                        }
                    } else {
                        try {
                            client.delete().forPath(connectionInformationPath);
                        } catch (KeeperException.NoNodeException noNode) {
                            // node was deleted in the meantime --> try again
                        }
                    }
                } else {
                    try {
                        client.create()
                                .creatingParentsIfNeeded()
                                .withMode(CreateMode.EPHEMERAL)
                                .forPath(connectionInformationPath, baos.toByteArray());

                        dataWritten = true;
                    } catch (KeeperException.NodeExistsException nodeExists) {
                        // node has been created in the meantime --> try again
                    }
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully wrote leader information: {}.", leaderInformation);
            }
        } catch (Exception e) {
            fatalErrorHandler.onFatalError(
                    new LeaderElectionException(
                            "Could not write leader address and leader session ID to "
                                    + "ZooKeeper.",
                            e));
        }
    }

    private void handleStateChange(ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
                LOG.debug("Connected to ZooKeeper quorum. Leader election can start.");
                break;
            case SUSPENDED:
                LOG.warn("Connection to ZooKeeper suspended, waiting for reconnection.");
                break;
            case RECONNECTED:
                LOG.info(
                        "Connection to ZooKeeper was reconnected. Leader election can be restarted.");
                break;
            case LOST:
                // Maybe we have to throw an exception here to terminate the JobManager
                LOG.warn(
                        "Connection to ZooKeeper lost. The contender "
                                + leaderContenderDescription
                                + " no longer participates in the leader election.");
                break;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%s{leaderLatchPath='%s', connectionInformationPath='%s'}",
                getClass().getSimpleName(), leaderLatchPath, connectionInformationPath);
    }

    @VisibleForTesting
    String getConnectionInformationPath() {
        return connectionInformationPath;
    }
}
