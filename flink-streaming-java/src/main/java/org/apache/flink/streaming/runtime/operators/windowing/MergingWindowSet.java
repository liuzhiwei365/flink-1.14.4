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

package org.apache.flink.streaming.runtime.operators.windowing;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.windowing.assigners.MergingWindowAssigner;
import org.apache.flink.streaming.api.windowing.windows.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for keeping track of merging {@link Window Windows} when using a {@link
 * MergingWindowAssigner} in a {@link WindowOperator}.
 *
 * <p>When merging windows, we keep one of the original windows as the state window, i.e. the window
 * that is used as namespace to store the window elements. Elements from the state windows of merged
 * windows must be merged into this one state window. We keep a mapping from in-flight window to
 * state window that can be queried using {@link #getStateWindow(Window)}.
 *
 * <p>A new window can be added to the set of in-flight windows using {@link #addWindow(Window,
 * MergeFunction)}. This might merge other windows and the caller must react accordingly in the
 * {@link MergeFunction#merge(Object, Collection, Object, Collection)} and adjust the outside view
 * of windows and state.
 *
 * <p>Windows can be removed from the set of windows using {@link #retireWindow(Window)}.
 *
 * @param <W> The type of {@code Window} that this set is keeping track of.
 */
public class MergingWindowSet<W extends Window> {

    private static final Logger LOG = LoggerFactory.getLogger(MergingWindowSet.class);

    //  从 窗口 映射到 保持窗口状态的窗口
    //  当我们从某个窗口开始逐步合并窗口时, 我们将该开始窗口保留为状态窗口, 以防止代价高昂的状态操作
    private final Map<W, W> mapping;

    // 对 mapping 的初始备份
    private final Map<W, W> initialMapping;

    private final ListState<Tuple2<W, W>> state;

    /** Our window assigner. */
    private final MergingWindowAssigner<?, W> windowAssigner;

    /** Restores a {@link MergingWindowSet} from the given state. */
    public MergingWindowSet(
            MergingWindowAssigner<?, W> windowAssigner, ListState<Tuple2<W, W>> state)
            throws Exception {
        this.windowAssigner = windowAssigner;
        mapping = new HashMap<>();

        Iterable<Tuple2<W, W>> windowState = state.get();
        if (windowState != null) {
            for (Tuple2<W, W> window : windowState) {
                mapping.put(window.f0, window.f1);
            }
        }

        this.state = state;

        initialMapping = new HashMap<>();
        initialMapping.putAll(mapping);
    }

    /**
     * Persist the updated mapping to the given state if the mapping changed since initialization.
     */
    // 如果mapping 和 初始拷贝不一样, 则 把 mapping 持久化
    public void persist() throws Exception {
        if (!mapping.equals(initialMapping)) {
            // 如果 mapping 相对于 初始备份 有了变化
            // 则 更新 管理状态 （即由flink 内核 维护的状态 ）
            state.clear();
            for (Map.Entry<W, W> window : mapping.entrySet()) {
                state.add(new Tuple2<>(window.getKey(), window.getValue()));
            }
        }
    }

    /**
     * Returns the state window for the given in-flight {@code Window}. The state window is the
     * {@code Window} in which we keep the actual state of a given in-flight window. Windows might
     * expand but we keep to original state window for keeping the elements of the window to avoid
     * costly state juggling.
     *
     * @param window The window for which to get the state window.
     */
    public W getStateWindow(W window) {
        return mapping.get(window);
    }

    /**
     * Removes the given window from the set of in-flight windows.
     *
     * @param window The {@code Window} to remove.
     */
    public void retireWindow(W window) {
        W removed = this.mapping.remove(window);
        if (removed == null) {
            throw new IllegalStateException(
                    "Window " + window + " is not in in-flight window set.");
        }
    }

    /**
     * Adds a new {@code Window} to the set of in-flight windows. It might happen that this triggers
     * merging of previously in-flight windows. In that case, the provided {@link MergeFunction} is
     * called.
     *
     * <p>This returns the window that is the representative of the added window after adding. This
     * can either be the new window itself, if no merge occurred, or the newly merged window. Adding
     * an element to a window or calling trigger functions should only happen on the returned
     * representative. This way, we never have to deal with a new window that is immediately
     * swallowed up by another window.
     *
     * <p>If the new window is merged, the {@code MergeFunction} callback arguments also don't
     * contain the new window as part of the list of merged windows.
     *
     * @param newWindow The new {@code Window} to add.
     * @param mergeFunction The callback to be invoked in case a merge occurs.
     * @return The {@code Window} that new new {@code Window} ended up in. This can also be the the
     *     new {@code Window} itself in case no merge occurred.
     * @throws Exception
     */
    public W addWindow(W newWindow, MergeFunction<W> mergeFunction) throws Exception {

        List<W> windows = new ArrayList<>();

        windows.addAll(this.mapping.keySet());
        windows.add(newWindow);

        //最终 : key是合并加宽过的 窗口 , value 是所有要被合并的窗口的集合
        final Map<W, Collection<W>> mergeResults = new HashMap<>();

        // windowAssigner 必定是如下4个实现类的一个
        //DynamicProcessingTimeSessionWindows
        //ProcessingTimeSessionWindows
        //DynamicEventTimeSessionWindows
        //EventTimeSessionWindows
        // 这4个实现都调用了 相同的方法:  TimeWindow.mergeWindows(windows, c)
        windowAssigner.mergeWindows(
                windows,
                new MergingWindowAssigner.MergeCallback<W>() {
                    @Override
                    public void merge(Collection<W> toBeMerged, W mergeResult) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Merging {} into {}", toBeMerged, mergeResult);
                        }
                        mergeResults.put(mergeResult, toBeMerged);
                    }
                });

        W resultWindow = newWindow;
        boolean mergedNewWindow = false;

        // 如果有合并窗口的必要
        for (Map.Entry<W, Collection<W>> c : mergeResults.entrySet()) {
            W mergeResult = c.getKey(); //合并加宽过的 窗口
            Collection<W> mergedWindows = c.getValue(); // 所有要被合并的窗口的集合

            // 如果我们的新窗口在合并窗口中 , 则将合并结果作为 要返回的窗口
            if (mergedWindows.remove(newWindow)) {
                mergedNewWindow = true;
                // 核心
                resultWindow = mergeResult;
            }

            // 选择mergedWindows 集合中的第一个窗口作为  该窗口的的状态窗口
            // 因为属于同一个 mergedWindows 集合的 窗口,它们的 value ,也就是 状态窗口 一定相同
            W mergedStateWindow = this.mapping.get(mergedWindows.iterator().next());

            // 拿到所有 mergedWindows 的元素 在mapping 中的 状态窗口 （保存不用再维护的 状态窗口）
            List<W> mergedStateWindows = new ArrayList<>();
            for (W mergedWindow : mergedWindows) { // 特别注意, 此时,mergedWindows中不含有 newWindow

                // 因为已经合并了,所以不用维护了, 后续 只需 维护 合并后的结果 mergeResult
                W res = this.mapping.remove(mergedWindow);
                if (res != null) {
                    mergedStateWindows.add(res);
                }
            }

            // 被合并的窗口 已经在上面被删去,现在只需要维护 合并后的结果
            this.mapping.put(mergeResult, mergedStateWindow);

            // 删除 mergedStateWindow
            mergedStateWindows.remove(mergedStateWindow);

            if (!(mergedWindows.contains(mergeResult) && mergedWindows.size() == 1)) {
                mergeFunction.merge(
                        mergeResult, // newWindow 来之后 ,最新的 合并结果
                        mergedWindows, // 排除了 newWindow ,代表的是该newWindow 来之前, 其所属的组, 也就是 mapping 不用再继续维护的 窗口
                        this.mapping.get(mergeResult), // newWindow 来之后 ,最新的 合并结果的 状态窗口
                        mergedStateWindows); // mapping 不用再维护的 状态窗口
            }
        }

        if (mergeResults.isEmpty() || (resultWindow.equals(newWindow) && !mergedNewWindow)) {
            // 如果没有合并窗口的必要 ,新来的窗口 自己就是自己的合并后的窗口
            this.mapping.put(resultWindow, resultWindow);
        }

        return resultWindow;
    }

    /**
     * Callback for {@link #addWindow(Window, MergeFunction)}.
     *
     * @param <W>
     */
    public interface MergeFunction<W> {

        /**
         * This gets called when a merge occurs.
         *
         * @param mergeResult The newly resulting merged {@code Window}.
         * @param mergedWindows The merged {@code Window Windows}.
         * @param stateWindowResult The state window of the merge result.
         * @param mergedStateWindows The merged state windows.
         * @throws Exception
         */
        void merge(
                W mergeResult,
                Collection<W> mergedWindows,
                W stateWindowResult,
                Collection<W> mergedStateWindows)
                throws Exception;
    }

    @Override
    public String toString() {
        return "MergingWindowSet{" + "windows=" + mapping + '}';
    }
}
