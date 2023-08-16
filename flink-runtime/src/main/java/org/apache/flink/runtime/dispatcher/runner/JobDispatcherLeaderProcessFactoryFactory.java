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

package org.apache.flink.runtime.dispatcher.runner;

import org.apache.flink.runtime.dispatcher.JobDispatcherFactory;
import org.apache.flink.runtime.dispatcher.PartialDispatcherServices;
import org.apache.flink.runtime.entrypoint.component.JobGraphRetriever;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobmanager.JobGraphStoreFactory;
import org.apache.flink.runtime.rpc.FatalErrorHandler;
import org.apache.flink.runtime.rpc.RpcService;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.FlinkRuntimeException;

import java.util.concurrent.Executor;

/** Factory for the {@link JobDispatcherLeaderProcessFactory}. */
public class JobDispatcherLeaderProcessFactoryFactory
        implements DispatcherLeaderProcessFactoryFactory {

    private final JobGraphRetriever jobGraphRetriever;

    private JobDispatcherLeaderProcessFactoryFactory(JobGraphRetriever jobGraphRetriever) {
        this.jobGraphRetriever = jobGraphRetriever;
    }

    // 只有per-job 模式才会走到这里
    @Override
    public DispatcherLeaderProcessFactory createFactory(
            JobGraphStoreFactory jobGraphStoreFactory,
            Executor ioExecutor,
            RpcService rpcService,
            PartialDispatcherServices partialDispatcherServices,
            FatalErrorHandler fatalErrorHandler) {

        final JobGraph jobGraph;

        try {
            //  1 利用 JobGraphRetriever 把job graph 从文件系统中读回来,
            //  这和session模式 有区别, session 模式是通过 JobGraphStore.recoverJobGraph 方法 取回多个JobGraph
            //
            //  2 后续, Dispatcher.runRecoveredJob 在运行作业的时候 会用到这里的JobGraph
            jobGraph =
                    jobGraphRetriever.retrieveJobGraph(
                            partialDispatcherServices.getConfiguration());
        } catch (FlinkException e) {
            throw new FlinkRuntimeException("Could not retrieve the JobGraph.", e);
        }

        final DefaultDispatcherGatewayServiceFactory defaultDispatcherServiceFactory =
                new DefaultDispatcherGatewayServiceFactory(
                        JobDispatcherFactory.INSTANCE, rpcService, partialDispatcherServices);

        return new JobDispatcherLeaderProcessFactory(
                defaultDispatcherServiceFactory, jobGraph, fatalErrorHandler);
    }

    public static JobDispatcherLeaderProcessFactoryFactory create(
            JobGraphRetriever jobGraphRetriever) {
        return new JobDispatcherLeaderProcessFactoryFactory(jobGraphRetriever);
    }
}
