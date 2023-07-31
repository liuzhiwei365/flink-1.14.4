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

import java.io.IOException;


// 迭代 指定状态名下 -> 指定键组编号范围下 -> 的所有 kv 状态
// 将所有的状态划分进入连续的 键组 是有必要的;  结果迭代序列的排序规则是   先按 kg (键组), 再按 kv (键值)
public interface KeyValueStateIterator extends AutoCloseable {

    /**
     * Advances the iterator. Should only be called if {@link #isValid()} returned true. Valid flag
     * can only change after calling {@link #next()}.
     */
    void next() throws IOException;

    /** Returns the key-group for the current key. */
    int keyGroup();

    byte[] key();

    byte[] value();

    /** Returns the Id of the K/V state to which the current key belongs. */
    int kvStateId();

    /**
     * Indicates if current key starts a new k/v-state, i.e. belong to a different k/v-state than
     * it's predecessor.
     *
     * @return true iff the current key belong to a different k/v-state than it's predecessor.
     */
    boolean isNewKeyValueState();

    /**
     * Indicates if current key starts a new key-group, i.e. belong to a different key-group than
     * it's predecessor.
     *
     * @return true iff the current key belong to a different key-group than it's predecessor.
     */
    boolean isNewKeyGroup();

    /**
     * Check if the iterator is still valid. Getters like {@link #key()}, {@link #value()}, etc. as
     * well as {@link #next()} should only be called if valid returned true. Should be checked after
     * each call to {@link #next()} before accessing iterator state.
     *
     * @return True iff this iterator is valid.
     */
    boolean isValid();

    @Override
    void close();
}
