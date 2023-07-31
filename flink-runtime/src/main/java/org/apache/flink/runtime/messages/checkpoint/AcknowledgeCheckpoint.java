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

package org.apache.flink.runtime.messages.checkpoint;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.checkpoint.CheckpointMetrics;
import org.apache.flink.runtime.checkpoint.TaskStateSnapshot;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;

/**
 * This message is sent from the {@link org.apache.flink.runtime.taskexecutor.TaskExecutor} to the
 * {@link org.apache.flink.runtime.jobmaster.JobMaster} to signal that the checkpoint of an
 * individual task is completed.
 *
 * <p>This message may carry the handle to the task's chained operator state and the key group
 * state.
 */
//  1 该消息是 TaskExecutor 端发给 JobMaster 端的 CheckpointCoordinator 的
//  2 你可以理解为  ack  checkpoint
//  3 该消息携带了  子任务下 每个算子的  算子子任务状态   和 键组状态

//  4 为什么一个task 中会存在多个算子, 因为多个算子会链化在一起
public class AcknowledgeCheckpoint extends AbstractCheckpointMessage {

    private static final long serialVersionUID = -7606214777192401493L;

    // TaskStateSnapshot 对象 内部有   子任务下 每个算子的  算子子任务状态   和 键组状态
    private final TaskStateSnapshot subtaskState;

    private final CheckpointMetrics checkpointMetrics;

    // ------------------------------------------------------------------------

    public AcknowledgeCheckpoint(
            JobID job,
            ExecutionAttemptID taskExecutionId,
            long checkpointId,
            CheckpointMetrics checkpointMetrics,
            TaskStateSnapshot subtaskState) {

        super(job, taskExecutionId, checkpointId);

        this.subtaskState = subtaskState;
        this.checkpointMetrics = checkpointMetrics;
    }

    @VisibleForTesting
    public AcknowledgeCheckpoint(
            JobID jobId, ExecutionAttemptID taskExecutionId, long checkpointId) {
        this(jobId, taskExecutionId, checkpointId, new CheckpointMetrics(), null);
    }

    // ------------------------------------------------------------------------
    //  properties
    // ------------------------------------------------------------------------

    public TaskStateSnapshot getSubtaskState() {
        return subtaskState;
    }

    public CheckpointMetrics getCheckpointMetrics() {
        return checkpointMetrics;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcknowledgeCheckpoint)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        AcknowledgeCheckpoint that = (AcknowledgeCheckpoint) o;
        return subtaskState != null
                ? subtaskState.equals(that.subtaskState)
                : that.subtaskState == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (subtaskState != null ? subtaskState.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "Confirm Task Checkpoint %d for (%s/%s) - state=%s",
                getCheckpointId(), getJob(), getTaskExecutionId(), subtaskState);
    }
}
