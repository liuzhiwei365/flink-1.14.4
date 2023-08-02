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

package org.apache.flink.runtime.state;

import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;


//算子状态句柄, 它不是实际的状态,但是可以读回状态, 操作状态
public interface OperatorStateHandle extends StreamStateHandle {

    /** Returns a map of meta data for all contained states by their name. */

    // 每个状态名称 到 状态元数据 的映射 , StateMetaInfo 包含偏移量数组, 一个偏移量与一个不可分割的最小基本状态粒度 对应
    Map<String, StateMetaInfo> getStateNameToPartitionOffsets();

    //返回  用于读取  算子状态信息的 输入流
    @Override
    FSDataInputStream openInputStream() throws IOException;

    // 返回 指向状态数据的 底层的 状态句柄
    StreamStateHandle getDelegateStateHandle();

    /**
     * The modes that determine how an {@link OperatorStreamStateHandle} is assigned to tasks during
     * restore.
     */
    enum Mode {
        SPLIT_DISTRIBUTE, // The operator state partitions in the state handle are split and
        // distributed to one task each.
        UNION, // The operator state partitions are UNION-ed upon restoring and sent to all tasks.
        BROADCAST // The operator states are identical, as the state is produced from a broadcast
        // stream.
    }

    /** Meta information about the operator state handle. */

    // 专门用来描述 算子状态 的 状态元数据的
    class StateMetaInfo implements Serializable {

        private static final long serialVersionUID = 3593817615858941166L;

        // 一个offsets数组的 元素 就代表 最小的状态粒度（不可再分割）
        private final long[] offsets;
        private final Mode distributionMode;

        public StateMetaInfo(long[] offsets, Mode distributionMode) {
            this.offsets = Preconditions.checkNotNull(offsets);
            this.distributionMode = Preconditions.checkNotNull(distributionMode);
        }

        public long[] getOffsets() {
            return offsets;
        }

        public Mode getDistributionMode() {
            return distributionMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StateMetaInfo that = (StateMetaInfo) o;

            return Arrays.equals(getOffsets(), that.getOffsets())
                    && getDistributionMode() == that.getDistributionMode();
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(getOffsets());
            result = 31 * result + getDistributionMode().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "StateMetaInfo{"
                    + "offsets="
                    + Arrays.toString(offsets)
                    + ", distributionMode="
                    + distributionMode
                    + '}';
        }
    }
}
