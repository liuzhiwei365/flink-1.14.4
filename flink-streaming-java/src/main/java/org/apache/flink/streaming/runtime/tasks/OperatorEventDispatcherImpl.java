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

package org.apache.flink.streaming.runtime.tasks;

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.jobgraph.tasks.TaskOperatorEventGateway;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;
import org.apache.flink.runtime.operators.coordination.OperatorEventDispatcher;
import org.apache.flink.runtime.operators.coordination.OperatorEventGateway;
import org.apache.flink.runtime.operators.coordination.OperatorEventHandler;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.SerializedValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * An implementation of the {@link OperatorEventDispatcher}.
 *
 * <p>This class is intended for single threaded use from the stream task mailbox.
 */
@Internal
public final class OperatorEventDispatcherImpl implements OperatorEventDispatcher {

    //处理算子协调者发来的事件
    //SourceOperatorFactory 和 CollectSinkOperatorFactory 都会调用 本类的registerEventHandler方法来想handlers 注册处理器
    private final Map<OperatorID, OperatorEventHandler> handlers;

    private final ClassLoader classLoader;

    //发送算子事件给算子协调者
    private final TaskOperatorEventGateway toCoordinator;

    public OperatorEventDispatcherImpl(
            ClassLoader classLoader, TaskOperatorEventGateway toCoordinator) {
        this.classLoader = checkNotNull(classLoader);
        this.toCoordinator = checkNotNull(toCoordinator);
        this.handlers = new HashMap<>();
    }

    //分派和路由 其他算子协调者 发来的消息 给不同的  算子事件处理器 来处理
    void dispatchEventToHandlers(
            OperatorID operatorID, SerializedValue<OperatorEvent> serializedEvent)
            throws FlinkException {
        final OperatorEvent evt;
        try {
            evt = serializedEvent.deserializeValue(classLoader);
        } catch (IOException | ClassNotFoundException e) {
            throw new FlinkException("Could not deserialize operator event", e);
        }

        // 拿到指定的处理器
        final OperatorEventHandler handler = handlers.get(operatorID);
        if (handler != null) {
            // 用指定的算子事件处理器  处理 算子事件
            handler.handleOperatorEvent(evt);
        } else {
            throw new FlinkException("Operator not registered for operator events");
        }
    }

    @Override
    public void registerEventHandler(OperatorID operator, OperatorEventHandler handler) {
        final OperatorEventHandler prior = handlers.putIfAbsent(operator, handler);
        if (prior != null) {
            throw new IllegalStateException("already a handler registered for this operatorId");
        }
    }

    @Override
    public OperatorEventGateway getOperatorEventGateway(OperatorID operatorId) {
        return new OperatorEventGatewayImpl(toCoordinator, operatorId);
    }

    // ------------------------------------------------------------------------

    private static final class OperatorEventGatewayImpl implements OperatorEventGateway {

        private final TaskOperatorEventGateway toCoordinator;

        private final OperatorID operatorId;

        private OperatorEventGatewayImpl(
                TaskOperatorEventGateway toCoordinator, OperatorID operatorId) {
            this.toCoordinator = toCoordinator;
            this.operatorId = operatorId;
        }

        @Override
        public void sendEventToCoordinator(OperatorEvent event) {
            final SerializedValue<OperatorEvent> serializedEvent;
            try {
                serializedEvent = new SerializedValue<>(event);
            } catch (IOException e) {
                // this is not a recoverable situation, so we wrap this in an
                // unchecked exception and let it bubble up
                throw new FlinkRuntimeException("Cannot serialize operator event", e);
            }

            toCoordinator.sendOperatorEventToCoordinator(operatorId, serializedEvent);
        }
    }
}
