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
import org.apache.flink.util.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.util.UUID;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Default implementation for leader election service. Composed with different {@link
 * LeaderElectionDriver}, we could perform a leader election for the contender, and then persist the
 * leader information to various storage.
 */
public class DefaultLeaderElectionService
        implements LeaderElectionService, LeaderElectionEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLeaderElectionService.class);

    private final Object lock = new Object();

    private final LeaderElectionDriverFactory leaderElectionDriverFactory;

    /** The leader contender which applies for leadership. */
    private volatile LeaderContender leaderContender;

    @GuardedBy("lock")
    private volatile UUID issuedLeaderSessionID;

    @GuardedBy("lock")
    private volatile UUID confirmedLeaderSessionID;

    @GuardedBy("lock")
    private volatile String confirmedLeaderAddress;

    @GuardedBy("lock")
    private volatile boolean running;

    private LeaderElectionDriver leaderElectionDriver;

    public DefaultLeaderElectionService(LeaderElectionDriverFactory leaderElectionDriverFactory) {
        this.leaderElectionDriverFactory = checkNotNull(leaderElectionDriverFactory);

        leaderContender = null;

        issuedLeaderSessionID = null;
        confirmedLeaderSessionID = null;
        confirmedLeaderAddress = null;

        this.leaderElectionDriver = null;

        running = false;
    }

    @Override
    public final void start(LeaderContender contender) throws Exception {
        checkNotNull(contender, "Contender must not be null.");
        Preconditions.checkState(leaderContender == null, "Contender was already set.");

        synchronized (lock) {
            leaderContender = contender;
            leaderElectionDriver =
                    leaderElectionDriverFactory.createLeaderElectionDriver(
                            this,
                            new LeaderElectionFatalErrorHandler(),
                            leaderContender.getDescription());
            LOG.info("Starting DefaultLeaderElectionService with {}.", leaderElectionDriver);

            running = true;
        }
    }

    @Override
    public final void stop() throws Exception {
        LOG.info("Stopping DefaultLeaderElectionService.");

        synchronized (lock) {
            if (!running) {
                return;
            }
            running = false;
            clearConfirmedLeaderInformation();
        }

        leaderElectionDriver.close();
    }

    @Override
    public void confirmLeadership(UUID leaderSessionID, String leaderAddress) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Confirm leader session ID {} for leader {}.", leaderSessionID, leaderAddress);
        }

        checkNotNull(leaderSessionID);

        synchronized (lock) {
            if (hasLeadership(leaderSessionID)) {
                if (running) {
                    confirmLeaderInformation(leaderSessionID, leaderAddress);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(
                                "Ignoring the leader session Id {} confirmation, since the "
                                        + "LeaderElectionService has already been stopped.",
                                leaderSessionID);
                    }
                }
            } else {
                // Received an old confirmation call
                if (!leaderSessionID.equals(this.issuedLeaderSessionID)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(
                                "Receive an old confirmation call of leader session ID {}, "
                                        + "current issued session ID is {}",
                                leaderSessionID,
                                issuedLeaderSessionID);
                    }
                } else {
                    LOG.warn(
                            "The leader session ID {} was confirmed even though the "
                                    + "corresponding JobManager was not elected as the leader.",
                            leaderSessionID);
                }
            }
        }
    }

    @Override
    public boolean hasLeadership(@Nonnull UUID leaderSessionId) {
        synchronized (lock) {
            if (running) {
                return leaderElectionDriver.hasLeadership()
                        && leaderSessionId.equals(issuedLeaderSessionID);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "hasLeadership is called after the service is stopped, returning false.");
                }
                return false;
            }
        }
    }

    /**
     * Returns the current leader session ID or null, if the contender is not the leader.
     *
     * @return The last leader session ID or null, if the contender is not the leader
     */
    @VisibleForTesting
    @Nullable
    public UUID getLeaderSessionID() {
        return confirmedLeaderSessionID;
    }

    @GuardedBy("lock")
    private void confirmLeaderInformation(UUID leaderSessionID, String leaderAddress) {
        confirmedLeaderSessionID = leaderSessionID;
        confirmedLeaderAddress = leaderAddress;
        leaderElectionDriver.writeLeaderInformation(
                LeaderInformation.known(confirmedLeaderSessionID, confirmedLeaderAddress));
    }

    @GuardedBy("lock")
    private void clearConfirmedLeaderInformation() {
        confirmedLeaderSessionID = null;
        confirmedLeaderAddress = null;
    }

    @Override
    @GuardedBy("lock")
    public void onGrantLeadership() {
        synchronized (lock) {
            if (running) {
                issuedLeaderSessionID = UUID.randomUUID();
                clearConfirmedLeaderInformation();

                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Grant leadership to contender {} with session ID {}.",
                            leaderContender.getDescription(),
                            issuedLeaderSessionID);
                }

                leaderContender.grantLeadership(issuedLeaderSessionID);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Ignoring the grant leadership notification since the {} has "
                                    + "already been closed.",
                            leaderElectionDriver);
                }
            }
        }
    }

    @Override
    @GuardedBy("lock")
    public void onRevokeLeadership() {
        synchronized (lock) {
            if (running) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Revoke leadership of {} ({}@{}).",
                            leaderContender.getDescription(),
                            confirmedLeaderSessionID,
                            confirmedLeaderAddress);
                }

                issuedLeaderSessionID = null;
                clearConfirmedLeaderInformation();

                leaderContender.revokeLeadership();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Clearing the leader information on {}.", leaderElectionDriver);
                }
                // Clear the old leader information on the external storage
                leaderElectionDriver.writeLeaderInformation(LeaderInformation.empty());
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Ignoring the revoke leadership notification since the {} "
                                    + "has already been closed.",
                            leaderElectionDriver);
                }
            }
        }
    }

    /*
           目前我所知道的是，@Immutable、@ThreadSafe、@NotThreadSafe、@GuardedBy 都不是 JDK 的标准注释。
       如果我们需要使用它们，需要导入额外的依赖项：jcip-annotations.jar。

           @ThreadSafe 是表示这个类是线程安全的。具体是否真安全，那要看实现者怎么实现的了，反正打上这个标签只是表示一下。不线程安全的类打上这个注解也没事儿。
           @Immutable 表示，类是不可变的，包含了　@ThreadSafe　的意思。
           @NotThreadSafe 表示这个类不是线程安全的。如果是线程安全的非要打上这个注解，那也不会报错。
           这三个注解，对用户和维护者是有益的，用户可以立即看出来这个类是否是线程安全的，维护者则是可以根据这个注解，重点检查线程安全方面。另外，代码分析工具可能会利用这个注解。

       域注解和方法注解都是用@GuardedBy( lock )来标识。里面的Lock是告诉维护者：这个状态变量，这个方法被哪个锁保护着。这样可以强烈的提示类的维护者注意这里。
           @GuardedBy( "this" ) 受对象内部锁保护
           @GuardedBy( "fieldName" ) 受 与fieldName引用相关联的锁 保护。
           @GuardedBy( "ClassName.fieldName" ) 受 一个类的静态field的锁 保存。
           @GuardedBy( "methodName()" ) 锁对象是 methodName() 方法的返值，受这个锁保护。
           @GuardedBy( "ClassName.class" ) 受 ClassName类的直接锁对象保护。而不是这个类的某个实例的锁对象。
    */

    // 这里的lock  指的是lock 成员变量
    @Override
    @GuardedBy("lock")
    public void onLeaderInformationChange(LeaderInformation leaderInformation) {
        synchronized (lock) {
            if (running) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(
                            "Leader node changed while {} is the leader with session ID {}. New leader information {}.",
                            leaderContender.getDescription(),
                            confirmedLeaderSessionID,
                            leaderInformation);
                }
                if (confirmedLeaderSessionID != null) {
                    final LeaderInformation confirmedLeaderInfo =
                            LeaderInformation.known(
                                    confirmedLeaderSessionID, confirmedLeaderAddress);
                    if (leaderInformation.isEmpty()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(
                                    "Writing leader information by {} since the external storage is empty.",
                                    leaderContender.getDescription());
                        }
                        leaderElectionDriver.writeLeaderInformation(confirmedLeaderInfo);
                    } else if (!leaderInformation.equals(confirmedLeaderInfo)) {
                        // the data field does not correspond to the expected leader information
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(
                                    "Correcting leader information by {}.",
                                    leaderContender.getDescription());
                        }
                        leaderElectionDriver.writeLeaderInformation(confirmedLeaderInfo);
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Ignoring change notification since the {} has "
                                    + "already been closed.",
                            leaderElectionDriver);
                }
            }
        }
    }

    private class LeaderElectionFatalErrorHandler implements FatalErrorHandler {

        @Override
        public void onFatalError(Throwable throwable) {
            synchronized (lock) {
                if (!running) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(
                                "Ignoring error notification since the service has been stopped.");
                    }
                    return;
                }

                if (throwable instanceof LeaderElectionException) {
                    leaderContender.handleError((LeaderElectionException) throwable);
                } else {
                    leaderContender.handleError(new LeaderElectionException(throwable));
                }
            }
        }
    }
}
