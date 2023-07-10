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

package org.apache.flink.runtime.checkpoint;

/** The type of checkpoint to perform. */
public enum CheckpointType {

    /** A checkpoint, full or incremental. */
    CHECKPOINT(false, PostCheckpointAction.NONE, "Checkpoint"),

    /** A regular savepoint. */
    SAVEPOINT(true, PostCheckpointAction.NONE, "Savepoint"),

    /** A savepoint taken while suspending the job. */
    SAVEPOINT_SUSPEND(true, PostCheckpointAction.SUSPEND, "Suspend Savepoint"),

    /** A savepoint taken while terminating the job. */
    SAVEPOINT_TERMINATE(true, PostCheckpointAction.TERMINATE, "Terminate Savepoint");

    private final boolean isSavepoint;

    private final PostCheckpointAction postCheckpointAction;

    private final String name;

    CheckpointType(
            final boolean isSavepoint,
            final PostCheckpointAction postCheckpointAction,
            final String name) {

        this.isSavepoint = isSavepoint;
        this.postCheckpointAction = postCheckpointAction;
        this.name = name;
    }

    public boolean isSavepoint() {
        return isSavepoint;
    }

    public boolean isSynchronous() {
        //  PostCheckpointAction枚举 只有如下三个元素
        //        NONE,
        //        SUSPEND,    将job 挂起
        //        TERMINATE,  将job 终止

        //  很容易理解, 无论是挂起还是终止, checkpoint 都必须同步来做
        //  (因为异步做的话, 可能导致checkpoint还没做完,但是作业已经终止; 这导致了逻辑错误,不安全)
        return postCheckpointAction != PostCheckpointAction.NONE;
    }

    public PostCheckpointAction getPostCheckpointAction() {
        return postCheckpointAction;
    }

    public boolean shouldAdvanceToEndOfTime() {
        return shouldDrain();
    }

    public boolean shouldDrain() {
        return getPostCheckpointAction() == PostCheckpointAction.TERMINATE;
    }

    public boolean shouldIgnoreEndOfInput() {
        return getPostCheckpointAction() == PostCheckpointAction.SUSPEND;
    }

    public String getName() {
        return name;
    }

    /** What's the intended action after the checkpoint (relevant for stopping with savepoint). */
    // 检查点之后打算采取什么行动
    public enum PostCheckpointAction {
        NONE,
        SUSPEND,  // 将job 挂起
        TERMINATE  //将job 终止
    }
}
