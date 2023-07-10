/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.io;

import org.apache.flink.annotation.Internal;

import java.util.concurrent.CompletableFuture;

/**
 * Interface defining couple of essential methods for listening on data availability using {@link
 * CompletableFuture}. For usage check out for example {@link PullingAsyncDataInput}.
 */

//  利用 CompletableFuture,来监听数据可用性
@Internal
public interface AvailabilityProvider {
    /**
     * Constant that allows to avoid volatile checks {@link CompletableFuture#isDone()}. Check
     * {@link #isAvailable()} and {@link #isApproximatelyAvailable()} for more explanation.
     */
    //  AVAILABLE 的 result 是 NIL ,是一个 new AltResult（null）对象
    CompletableFuture<?> AVAILABLE = CompletableFuture.completedFuture(null);

    /** @return a future that is completed if the respective provider is available. */
    // 如果相关提供者可用, 那么 得到的 future 对象是完成的
    CompletableFuture<?> getAvailableFuture();

    /**
     * In order to best-effort avoid volatile access in {@link CompletableFuture#isDone()}, we check
     * the condition of <code>future == AVAILABLE</code> firstly for getting probable performance
     * benefits while hot looping.
     *
     * <p>It is always safe to use this method in performance nonsensitive scenarios to get the
     * precise state.
     *
     * @return true if this instance is available for further processing.
     */
    // 以future 对象是否可用 来判断组件是否可用
    default boolean isAvailable() {
        CompletableFuture<?> future = getAvailableFuture();
        return future == AVAILABLE || future.isDone();
    }

    /**
     * Checks whether this instance is available only via constant {@link #AVAILABLE} to avoid
     * performance concern caused by volatile access in {@link CompletableFuture#isDone()}. So it is
     * mainly used in the performance sensitive scenarios which do not always need the precise
     * state.
     *
     * <p>This method is still safe to get the precise state if {@link #getAvailableFuture()} was
     * touched via (.get(), .wait(), .isDone(), ...) before, which also has a "happen-before"
     * relationship with this call.
     *
     * @return true if this instance is available for further processing.
     */
    default boolean isApproximatelyAvailable() {
        return getAvailableFuture() == AVAILABLE;
    }

    static CompletableFuture<?> and(CompletableFuture<?> first, CompletableFuture<?> second) {
        if (first == AVAILABLE && second == AVAILABLE) {
            return AVAILABLE;
        } else if (first == AVAILABLE) {
            return second;
        } else if (second == AVAILABLE) {
            return first;
        } else {
            return CompletableFuture.allOf(first, second);
        }
    }

    static CompletableFuture<?> or(CompletableFuture<?> first, CompletableFuture<?> second) {
        if (first == AVAILABLE || second == AVAILABLE) {
            return AVAILABLE;
        }
        return CompletableFuture.anyOf(first, second);
    }

    /**
     * A availability implementation for providing the helpful functions of resetting the
     * available/unavailable states.
     */
    final class AvailabilityHelper implements AvailabilityProvider {

        //  CompletableFuture 无参构造,内部的 result成员一定是null
        //  AvailabilityHelper 通过 result 是否是null 来判断 组件是否可用
        //  当 result 是null ,组件不可用 ; 反之,组件可用

        // 用 CompletableFuture 对象作为 flag ,这种设计可以借鉴
        private CompletableFuture<?> availableFuture = new CompletableFuture<>();

        public CompletableFuture<?> and(CompletableFuture<?> other) {
            return AvailabilityProvider.and(availableFuture, other);
        }

        public CompletableFuture<?> and(AvailabilityProvider other) {
            return and(other.getAvailableFuture());
        }

        public CompletableFuture<?> or(CompletableFuture<?> other) {
            return AvailabilityProvider.or(availableFuture, other);
        }

        public CompletableFuture<?> or(AvailabilityProvider other) {
            return or(other.getAvailableFuture());
        }

        /** Judges to reset the current available state as unavailable. */
        public void resetUnavailable() {
            if (isAvailable()) {
                availableFuture = new CompletableFuture<>();
            }
        }

        /** Resets the constant completed {@link #AVAILABLE} as the current state. */
        // CompletableFuture<?> AVAILABLE = CompletableFuture.completedFuture(null)
        // AVAILABLE 它的 result 是Nil , 对 null 进行了包装的对象,但不是null
        public void resetAvailable() {
            availableFuture = AVAILABLE;
        }

        /**
         * Returns the previously not completed future and resets the constant completed {@link
         * #AVAILABLE} as the current state.
         */
        //返回先前的availableFuture状态,并且重置availableFuture 为 可用  （方法名取的有问题；不过初始状态一定是不可用的,这样一想，似乎方法名也说的通过）
        public CompletableFuture<?> getUnavailableToResetAvailable() {
            CompletableFuture<?> toNotify = availableFuture;
            availableFuture = AVAILABLE;
            return toNotify;
        }

        /**
         * Creates a new uncompleted future as the current state and returns the previous
         * uncompleted one.
         */
        // 返回先前的状态,并且重置状态为 不可用 （方法名取的有问题；不过初始状态一定是不可用的,这样一想，似乎方法名也说的通过）
        public CompletableFuture<?> getUnavailableToResetUnavailable() {
            CompletableFuture<?> toNotify = availableFuture;
            availableFuture = new CompletableFuture<>();
            return toNotify;
        }

        /** @return a future that is completed if the respective provider is available. */
        @Override
        public CompletableFuture<?> getAvailableFuture() {
            return availableFuture;
        }

        @Override
        public String toString() {
            if (availableFuture == AVAILABLE) {
                return "AVAILABLE";
            }
            return availableFuture.toString();
        }
    }
}
