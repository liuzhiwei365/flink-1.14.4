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

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.metrics.groups.OperatorMetricGroup;
import org.apache.flink.runtime.checkpoint.CheckpointMetaData;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.io.network.api.writer.RecordWriter;
import org.apache.flink.runtime.io.network.api.writer.RecordWriterDelegate;
import org.apache.flink.runtime.io.network.partition.consumer.IndexedInputGate;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.metrics.MetricNames;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;
import org.apache.flink.runtime.operators.coordination.OperatorEventDispatcher;
import org.apache.flink.runtime.plugable.SerializationDelegate;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.streaming.api.graph.StreamConfig;
import org.apache.flink.streaming.api.graph.StreamEdge;
import org.apache.flink.streaming.api.operators.BoundedMultiInput;
import org.apache.flink.streaming.api.operators.Input;
import org.apache.flink.streaming.api.operators.MultipleInputStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.OperatorSnapshotFutures;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.operators.SourceOperator;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperatorFactoryUtil;
import org.apache.flink.streaming.api.operators.StreamTaskStateInitializer;
import org.apache.flink.streaming.runtime.io.RecordWriterOutput;
import org.apache.flink.streaming.runtime.io.StreamTaskSourceInput;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.mailbox.MailboxExecutorFactory;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.SerializedValue;

import org.apache.flink.shaded.guava30.com.google.common.io.Closer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * The {@code OperatorChain} contains all operators that are executed as one chain within a single
 * {@link StreamTask}.
 *
 * <p>The main entry point to the chain is it's {@code mainOperator}. {@code mainOperator} is
 * driving the execution of the {@link StreamTask}, by pulling the records from network inputs
 * and/or source inputs and pushing produced records to the remaining chained operators.
 *
 * @param <OUT> The type of elements accepted by the chain, i.e., the input type of the chain's main
 *     operator.
 */
public abstract class OperatorChain<OUT, OP extends StreamOperator<OUT>>
        implements BoundedMultiInput, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(OperatorChain.class);

    protected final RecordWriterOutput<?>[] streamOutputs;

    protected final WatermarkGaugeExposingOutput<StreamRecord<OUT>> mainOperatorOutput;

    /**
     * For iteration, {@link StreamIterationHead} and {@link StreamIterationTail} used for executing
     * feedback edges do not contain any operators, in which case, {@code mainOperatorWrapper} and
     * {@code tailOperatorWrapper} are null.
     *
     * <p>Usually first operator in the chain is the same as {@link #mainOperatorWrapper}, but
     * that's not the case if there are chained source inputs. In this case, one of the source
     * inputs will be the first operator. For example the following operator chain is possible:
     *
     * <pre>
     * first
     *      \
     *      main (multi-input) -> ... -> tail
     *      /
     * second
     * </pre>
     *
     * <p>Where "first" and "second" (there can be more) are chained source operators. When it comes
     * to things like closing, stat initialisation or state snapshotting, the operator chain is
     * traversed: first, second, main, ..., tail or in reversed order: tail, ..., main, second,
     * first
     */
    @Nullable protected final StreamOperatorWrapper<OUT, OP> mainOperatorWrapper; // 如果可能，内部会有定时服务

    @Nullable protected final StreamOperatorWrapper<?, ?> firstOperatorWrapper;
    @Nullable protected final StreamOperatorWrapper<?, ?> tailOperatorWrapper;

    protected final Map<StreamConfig.SourceInputConfig, ChainedSource> chainedSources;

    protected final int numOperators;

    protected final OperatorEventDispatcherImpl operatorEventDispatcher;

    protected final Closer closer = Closer.create();

    protected final @Nullable FinishedOnRestoreInput finishedOnRestoreInput;

    protected boolean isClosed;

    public OperatorChain(
            StreamTask<OUT, OP> containingTask,
            RecordWriterDelegate<SerializationDelegate<StreamRecord<OUT>>> recordWriterDelegate) {

        // step1 创建OperatorEventDispatcherImpl对象,  用于 发送 算子事件给算子协调者  和处理算子协调者发来的事件
        this.operatorEventDispatcher =
                new OperatorEventDispatcherImpl(
                        containingTask.getEnvironment().getUserCodeClassLoader().asClassLoader(),
                        containingTask.getEnvironment().getOperatorCoordinatorEventGateway());

        final ClassLoader userCodeClassloader = containingTask.getUserCodeClassLoader();
        final StreamConfig configuration = containingTask.getConfiguration();

        // step2 获取StreamTask的StreamOperator工厂,用于创建 算子,
        //       在先前构建StreamGraph的时候, StreamOperator 会封装为 StreamOperatorFactory 存储在 StreamGraph结构中
        //       而这里就会从 userCodeClassloader 解析定义的 StreamOperatorFactory,最终创建 算子实例
        StreamOperatorFactory<OUT> operatorFactory =
                configuration.getStreamOperatorFactory(userCodeClassloader);


        // step3 从configuration中 读取  chainedConfigs,即算子之间的链接配置. chainedConfigs 决定了算子之间 Output 接口的具体实现
        Map<Integer, StreamConfig> chainedConfigs =
                configuration.getTransitiveChainedTaskConfigsWithSelf(userCodeClassloader);


        // step4 根据StreamEdge 创建 RecordWriterOutput 组件
        List<StreamEdge> outEdgesInOrder = configuration.getOutEdgesInOrder(userCodeClassloader);
        Map<StreamEdge, RecordWriterOutput<?>> streamOutputMap = new HashMap<>(outEdgesInOrder.size());
        this.streamOutputs = new RecordWriterOutput<?>[outEdgesInOrder.size()];

        this.finishedOnRestoreInput =
                this.isTaskDeployedAsFinished()
                        ? new FinishedOnRestoreInput(
                                streamOutputs, configuration.getInputs(userCodeClassloader).length)
                        : null;

        boolean success = false;
        try {
            //   遍历所有的输出边,为每个输出边创建 RecordWriterOutput 对象
            createChainOutputs(
                    outEdgesInOrder,
                    recordWriterDelegate,
                    chainedConfigs,
                    containingTask,
                    streamOutputMap);

            List<StreamOperatorWrapper<?, ?>> allOpWrappers = new ArrayList<>(chainedConfigs.size());

            /*
               最终OperatorChain 的结构 如下：
               first chained source
                                   \
                                   main operator ->  ... -> tail
                                    /
               second chained source
             */
            // step5  递归地创建 OperatorChain中的内部算子
            //     (注意,将算子链接起来,是在后面调用 linkOperatorWrappers 方法实现的)
            // 这里, 针对的是 main operator 后面的部分
            this.mainOperatorOutput =
                    createOutputCollector(
                            containingTask,
                            configuration,
                            chainedConfigs,
                            userCodeClassloader,
                            streamOutputMap,
                            allOpWrappers,
                            containingTask.getMailboxExecutorFactory());

            if (operatorFactory != null) {
                // step6 创建算子链中的第一个算子 , 如果operatorFactory 实现了 ProcessingTimeServiceAware,会将
                // ProcessingTimeService 创建出来,并设置给 算子
                Tuple2<OP, Optional<ProcessingTimeService>> mainOperatorAndTimeService =
                        StreamOperatorFactoryUtil.createOperator(
                                operatorFactory,
                                containingTask,
                                configuration,
                                mainOperatorOutput,
                                operatorEventDispatcher);

                OP mainOperator = mainOperatorAndTimeService.f0;
                // 设置Watermark监控项
                mainOperator
                        .getMetricGroup()
                        .gauge(MetricNames.IO_CURRENT_OUTPUT_WATERMARK,
                                mainOperatorOutput.getWatermarkGauge());

                // 创建mainOperatorWrapper , 内部有时间服务
                this.mainOperatorWrapper =
                        createOperatorWrapper(
                                mainOperator,
                                containingTask,
                                configuration,
                                mainOperatorAndTimeService.f1,
                                true);

                // 将mainOperatorWrapper添加到chain的最后
                allOpWrappers.add(mainOperatorWrapper);
                // 按照数据流相反的顺序加入到allOpWrappers集合; 所以,尾部的operatorWrapper就是index为0的元素
                this.tailOperatorWrapper = allOpWrappers.get(0);
            } else {
                checkState(allOpWrappers.size() == 0);
                this.mainOperatorWrapper = null;
                this.tailOperatorWrapper = null;
            }

            /*
               最终OperatorChain 的结构 如下：
               first chained source
                                   \
                                   main operator ->  ... -> tail
                                    /
               second chained source
             */
            // step7  创建chain数据源, 这里针对的是 main operator 前面的部分
            this.chainedSources =
                    createChainedSources(
                            containingTask,
                            configuration.getInputs(userCodeClassloader),
                            chainedConfigs,
                            userCodeClassloader,
                            allOpWrappers);

            this.numOperators = allOpWrappers.size();

            // step8 将所有的StreamOperatorWrapper按照从上游到下游的顺序，形成双向链表
            firstOperatorWrapper = linkOperatorWrappers(allOpWrappers);

            success = true;
        } finally {
            // make sure we clean up after ourselves in case of a failure after acquiring
            // the first resources
            if (!success) {
                for (int i = 0; i < streamOutputs.length; i++) {
                    if (streamOutputs[i] != null) {
                        streamOutputs[i].close();
                    }
                    streamOutputs[i] = null;
                }
            }
        }
    }

    @VisibleForTesting
    OperatorChain(
            List<StreamOperatorWrapper<?, ?>> allOperatorWrappers,
            RecordWriterOutput<?>[] streamOutputs,
            WatermarkGaugeExposingOutput<StreamRecord<OUT>> mainOperatorOutput,
            StreamOperatorWrapper<OUT, OP> mainOperatorWrapper) {
        this.streamOutputs = streamOutputs;
        this.finishedOnRestoreInput = null;
        this.mainOperatorOutput = checkNotNull(mainOperatorOutput);
        this.operatorEventDispatcher = null;

        checkState(allOperatorWrappers != null && allOperatorWrappers.size() > 0);
        this.mainOperatorWrapper = checkNotNull(mainOperatorWrapper);
        this.tailOperatorWrapper = allOperatorWrappers.get(0);
        this.numOperators = allOperatorWrappers.size();
        this.chainedSources = Collections.emptyMap();

        firstOperatorWrapper = linkOperatorWrappers(allOperatorWrappers);
    }

    public abstract boolean isTaskDeployedAsFinished();

    public abstract void dispatchOperatorEvent(
            OperatorID operator, SerializedValue<OperatorEvent> event) throws FlinkException;

    public abstract void prepareSnapshotPreBarrier(long checkpointId) throws Exception;

    /**
     * Ends the main operator input specified by {@code inputId}).
     *
     * @param inputId the input ID starts from 1 which indicates the first input.
     */
    public abstract void endInput(int inputId) throws Exception;

    /**
     * Initialize state and open all operators in the chain from <b>tail to heads</b>, contrary to
     * {@link StreamOperator#close()} which happens <b>heads to tail</b> (see {@link
     * #finishOperators(StreamTaskActionExecutor)}).
     */
    public abstract void initializeStateAndOpenOperators(
            StreamTaskStateInitializer streamTaskStateInitializer) throws Exception;

    /**
     * Closes all operators in a chain effect way. Closing happens from <b>heads to tail</b>
     * operator in the chain, contrary to {@link StreamOperator#open()} which happens <b>tail to
     * heads</b> (see {@link #initializeStateAndOpenOperators(StreamTaskStateInitializer)}).
     */
    public abstract void finishOperators(StreamTaskActionExecutor actionExecutor) throws Exception;

    public abstract void notifyCheckpointComplete(long checkpointId) throws Exception;

    public abstract void notifyCheckpointAborted(long checkpointId) throws Exception;

    public abstract void snapshotState(
            Map<OperatorID, OperatorSnapshotFutures> operatorSnapshotsInProgress,
            CheckpointMetaData checkpointMetaData,
            CheckpointOptions checkpointOptions,
            Supplier<Boolean> isRunning,
            ChannelStateWriter.ChannelStateWriteResult channelStateWriteResult,
            CheckpointStreamFactory storage)
            throws Exception;

    public OperatorEventDispatcher getOperatorEventDispatcher() {
        return operatorEventDispatcher;
    }

    public void broadcastEvent(AbstractEvent event) throws IOException {
        broadcastEvent(event, false);
    }

    public void broadcastEvent(AbstractEvent event, boolean isPriorityEvent) throws IOException {
        for (RecordWriterOutput<?> streamOutput : streamOutputs) {
            streamOutput.broadcastEvent(event, isPriorityEvent);
        }
    }

    /**
     * Execute {@link StreamOperator#close()} of each operator in the chain of this {@link
     * StreamTask}. Closing happens from <b>tail to head</b> operator in the chain.
     */
    public void closeAllOperators() throws Exception {
        isClosed = true;
    }

    public RecordWriterOutput<?>[] getStreamOutputs() {
        return streamOutputs;
    }

    /** Returns an {@link Iterable} which traverses all operators in forward topological order. */
    @VisibleForTesting
    public Iterable<StreamOperatorWrapper<?, ?>> getAllOperators() {
        return getAllOperators(false);
    }

    /**
     * Returns an {@link Iterable} which traverses all operators in forward or reverse topological
     * order.
     */
    protected Iterable<StreamOperatorWrapper<?, ?>> getAllOperators(boolean reverse) {
        return reverse
                ? new StreamOperatorWrapper.ReadIterator(tailOperatorWrapper, true)
                : new StreamOperatorWrapper.ReadIterator(mainOperatorWrapper, false);
    }

    public Input getFinishedOnRestoreInputOrDefault(Input defaultInput) {
        return finishedOnRestoreInput == null ? defaultInput : finishedOnRestoreInput;
    }

    public int getNumberOfOperators() {
        return numOperators;
    }

    public WatermarkGaugeExposingOutput<StreamRecord<OUT>> getMainOperatorOutput() {
        return mainOperatorOutput;
    }

    public ChainedSource getChainedSource(StreamConfig.SourceInputConfig sourceInput) {
        checkArgument(
                chainedSources.containsKey(sourceInput),
                "Chained source with sourcedId = [%s] was not found",
                sourceInput);
        return chainedSources.get(sourceInput);
    }

    public List<Output<StreamRecord<?>>> getChainedSourceOutputs() {
        return chainedSources.values().stream()
                .map(ChainedSource::getSourceOutput)
                .collect(Collectors.toList());
    }

    public StreamTaskSourceInput<?> getSourceTaskInput(StreamConfig.SourceInputConfig sourceInput) {
        checkArgument(
                chainedSources.containsKey(sourceInput),
                "Chained source with sourcedId = [%s] was not found",
                sourceInput);
        return chainedSources.get(sourceInput).getSourceTaskInput();
    }

    public List<StreamTaskSourceInput<?>> getSourceTaskInputs() {
        return chainedSources.values().stream()
                .map(ChainedSource::getSourceTaskInput)
                .collect(Collectors.toList());
    }

    /**
     * This method should be called before finishing the record emission, to make sure any data that
     * is still buffered will be sent. It also ensures that all data sending related exceptions are
     * recognized.
     *
     * @throws IOException Thrown, if the buffered data cannot be pushed into the output streams.
     */
    public void flushOutputs() throws IOException {
        for (RecordWriterOutput<?> streamOutput : getStreamOutputs()) {
            streamOutput.flush();
        }
    }

    /**
     * This method releases all resources of the record writer output. It stops the output flushing
     * thread (if there is one) and releases all buffers currently held by the output serializers.
     *
     * <p>This method should never fail.
     */
    public void close() throws IOException {
        closer.close();
    }

    @Nullable
    public OP getMainOperator() {
        return (mainOperatorWrapper == null) ? null : mainOperatorWrapper.getStreamOperator();
    }

    public boolean isClosed() {
        return isClosed;
    }

    /** Wrapper class to access the chained sources and their's outputs. */
    public static class ChainedSource {
        private final WatermarkGaugeExposingOutput<StreamRecord<?>> chainedSourceOutput;
        private final StreamTaskSourceInput<?> sourceTaskInput;

        public ChainedSource(
                WatermarkGaugeExposingOutput<StreamRecord<?>> chainedSourceOutput,
                StreamTaskSourceInput<?> sourceTaskInput) {
            this.chainedSourceOutput = chainedSourceOutput;
            this.sourceTaskInput = sourceTaskInput;
        }

        public WatermarkGaugeExposingOutput<StreamRecord<?>> getSourceOutput() {
            return chainedSourceOutput;
        }

        public StreamTaskSourceInput<?> getSourceTaskInput() {
            return sourceTaskInput;
        }
    }

    // ------------------------------------------------------------------------
    //  initialization utilities
    // ------------------------------------------------------------------------

    // 为每个输出边创建 RecordWriterOutput 对象
    private void createChainOutputs(
            List<StreamEdge> outEdgesInOrder,
            RecordWriterDelegate<SerializationDelegate<StreamRecord<OUT>>> recordWriterDelegate,
            Map<Integer, StreamConfig> chainedConfigs,
            StreamTask<OUT, OP> containingTask,
            Map<StreamEdge, RecordWriterOutput<?>> streamOutputMap) {
        for (int i = 0; i < outEdgesInOrder.size(); i++) {
            StreamEdge outEdge = outEdgesInOrder.get(i);

            RecordWriterOutput<?> streamOutput =
                    createStreamOutput(
                            // RecordWriter 对象最终来源与 StreamTask的 成员变量 recordWriterDelegate
                            recordWriterDelegate.getRecordWriter(i),
                            outEdge,
                            chainedConfigs.get(outEdge.getSourceId()),
                            containingTask.getEnvironment());

            this.streamOutputs[i] = streamOutput;
            streamOutputMap.put(outEdge, streamOutput);
        }
    }

    private RecordWriterOutput<OUT> createStreamOutput(
            RecordWriter<SerializationDelegate<StreamRecord<OUT>>> recordWriter,
            StreamEdge edge,
            StreamConfig upStreamConfig,
            Environment taskEnvironment) {
        OutputTag sideOutputTag = edge.getOutputTag(); // OutputTag, return null if not sideOutput

        TypeSerializer outSerializer;

        // 如果该 边 是旁路输出 ,拿到旁路输出的序列化器
        if (edge.getOutputTag() != null) {
            // side output
            outSerializer =
                    upStreamConfig.getTypeSerializerSideOut(
                            edge.getOutputTag(),
                            taskEnvironment.getUserCodeClassLoader().asClassLoader());
        } else {
            // 不是旁路输出，也拿到相关的序列化器
            // main output
            outSerializer =
                    upStreamConfig.getTypeSerializerOut(
                            taskEnvironment.getUserCodeClassLoader().asClassLoader());
        }
        // 注册给 closer对象
        // closer 是一个栈 stack
        return closer.register(
                // RecordWriterOutput 内部成员 recordWriter ，负责 将序列化的二进制数据写入网络
                new RecordWriterOutput<OUT>(
                        recordWriter,
                        outSerializer,
                        sideOutputTag,
                        edge.supportsUnalignedCheckpoints()));
    }

    @SuppressWarnings("rawtypes")
    private Map<StreamConfig.SourceInputConfig, ChainedSource> createChainedSources(
            StreamTask<OUT, OP> containingTask,
            StreamConfig.InputConfig[] configuredInputs,
            Map<Integer, StreamConfig> chainedConfigs,
            ClassLoader userCodeClassloader,
            List<StreamOperatorWrapper<?, ?>> allOpWrappers) {
        if (Arrays.stream(configuredInputs)
                .noneMatch(input -> input instanceof StreamConfig.SourceInputConfig)) {
            return Collections.emptyMap();
        }
        checkState(
                mainOperatorWrapper.getStreamOperator() instanceof MultipleInputStreamOperator,
                "Creating chained input is only supported with MultipleInputStreamOperator and MultipleInputStreamTask");
        Map<StreamConfig.SourceInputConfig, ChainedSource> chainedSourceInputs = new HashMap<>();
        MultipleInputStreamOperator<?> multipleInputOperator =
                (MultipleInputStreamOperator<?>) mainOperatorWrapper.getStreamOperator();
        List<Input> operatorInputs = multipleInputOperator.getInputs();

        int sourceInputGateIndex =
                Arrays.stream(containingTask.getEnvironment().getAllInputGates())
                                .mapToInt(IndexedInputGate::getInputGateIndex)
                                .max()
                                .orElse(-1)
                        + 1;

        for (int inputId = 0; inputId < configuredInputs.length; inputId++) {
            if (!(configuredInputs[inputId] instanceof StreamConfig.SourceInputConfig)) {
                continue;
            }
            StreamConfig.SourceInputConfig sourceInput =
                    (StreamConfig.SourceInputConfig) configuredInputs[inputId];
            int sourceEdgeId = sourceInput.getInputEdge().getSourceId();
            StreamConfig sourceInputConfig = chainedConfigs.get(sourceEdgeId);
            OutputTag outputTag = sourceInput.getInputEdge().getOutputTag();

            WatermarkGaugeExposingOutput chainedSourceOutput =
                    createChainedSourceOutput(
                            containingTask,
                            sourceInputConfig,
                            userCodeClassloader,
                            getFinishedOnRestoreInputOrDefault(operatorInputs.get(inputId)),
                            multipleInputOperator.getMetricGroup(),
                            outputTag);

            SourceOperator<?, ?> sourceOperator =
                    (SourceOperator<?, ?>)
                            createOperator(
                                    containingTask,
                                    sourceInputConfig,
                                    userCodeClassloader,
                                    (WatermarkGaugeExposingOutput<StreamRecord<OUT>>)
                                            chainedSourceOutput,
                                    allOpWrappers,
                                    true);
            chainedSourceInputs.put(
                    sourceInput,
                    new ChainedSource(
                            chainedSourceOutput,
                            this.isTaskDeployedAsFinished()
                                    ? new StreamTaskFinishedOnRestoreSourceInput<>(
                                            sourceOperator, sourceInputGateIndex++, inputId)
                                    : new StreamTaskSourceInput<>(
                                            sourceOperator, sourceInputGateIndex++, inputId)));
        }
        return chainedSourceInputs;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private WatermarkGaugeExposingOutput<StreamRecord> createChainedSourceOutput(
            StreamTask<?, OP> containingTask,
            StreamConfig sourceInputConfig,
            ClassLoader userCodeClassloader,
            Input input,
            OperatorMetricGroup metricGroup,
            OutputTag outputTag) {

        WatermarkGaugeExposingOutput<StreamRecord> chainedSourceOutput;
        if (containingTask.getExecutionConfig().isObjectReuseEnabled()) {
            chainedSourceOutput = new ChainingOutput(input, metricGroup, outputTag);
        } else {
            TypeSerializer<?> inSerializer =
                    sourceInputConfig.getTypeSerializerOut(userCodeClassloader);
            chainedSourceOutput =
                    new CopyingChainingOutput(input, inSerializer, metricGroup, outputTag);
        }
        /**
         * Chained sources are closed when {@link
         * org.apache.flink.streaming.runtime.io.StreamTaskSourceInput} are being closed.
         */
        return closer.register(chainedSourceOutput);
    }


    // 本方法是一个递归方法
    // 配合 createOperatorChain 方法使用,  createOperatorChain 中会递归调用 createOutputCollector
    private <T> WatermarkGaugeExposingOutput<StreamRecord<T>> createOutputCollector(
            StreamTask<?, ?> containingTask,
            StreamConfig operatorConfig,
            Map<Integer, StreamConfig> chainedConfigs,
            ClassLoader userCodeClassloader,
            Map<StreamEdge, RecordWriterOutput<?>> streamOutputs,
            List<StreamOperatorWrapper<?, ?>> allOperatorWrappers,
            MailboxExecutorFactory mailboxExecutorFactory) {

        List<Tuple2<WatermarkGaugeExposingOutput<StreamRecord<T>>, StreamEdge>> allOutputs =
                new ArrayList<>(4);

        // create collectors for the network outputs
        // 遍历非链式StreamEdge，非链式的StreamEdge输出需要走网络连接
        // 因此生成的Output类型为RecordWriterOutput
        for (StreamEdge outputEdge : operatorConfig.getNonChainedOutputs(userCodeClassloader)) {
            @SuppressWarnings("unchecked")
            RecordWriterOutput<T> output = (RecordWriterOutput<T>) streamOutputs.get(outputEdge);

            allOutputs.add(new Tuple2<>(output, outputEdge));
        }


        // 遍历 该算子  所有的下游算子
        for (StreamEdge outputEdge : operatorConfig.getChainedOutputs(userCodeClassloader)) {
            // 下游算子的编号
            int outputId = outputEdge.getTargetId();
            // 下游算子的 配置
            StreamConfig chainedOpConfig = chainedConfigs.get(outputId);

            // 1   内部会递归调用 createOutputCollector
            // 2   会创建单个算子
            WatermarkGaugeExposingOutput<StreamRecord<T>> output =
                    createOperatorChain(
                            containingTask,
                            chainedOpConfig,
                            chainedConfigs,
                            userCodeClassloader,
                            streamOutputs,
                            allOperatorWrappers,
                            outputEdge.getOutputTag(),
                            mailboxExecutorFactory);
            allOutputs.add(new Tuple2<>(output, outputEdge));
        }

        if (allOutputs.size() == 1) {
            return allOutputs.get(0).f0;
        } else {
            // send to N outputs. Note that this includes the special case
            // of sending to zero outputs
            @SuppressWarnings({"unchecked"})
            Output<StreamRecord<T>>[] asArray = new Output[allOutputs.size()];
            for (int i = 0; i < allOutputs.size(); i++) {
                asArray[i] = allOutputs.get(i).f0;
            }

            // This is the inverse of creating the normal ChainingOutput.
            // If the chaining output does not copy we need to copy in the broadcast output,
            // otherwise multi-chaining would not work correctly.
            if (containingTask.getExecutionConfig().isObjectReuseEnabled()) {
                return closer.register(new CopyingBroadcastingOutputCollector<>(asArray));
            } else {
                return closer.register(new BroadcastingOutputCollector<>(asArray));
            }
        }
    }

    /**
     * Recursively create chain of operators that starts from the given {@param operatorConfig}.
     * Operators are created tail to head and wrapped into an {@link WatermarkGaugeExposingOutput}.
     */
    private <IN, OUT> WatermarkGaugeExposingOutput<StreamRecord<IN>> createOperatorChain(
            StreamTask<OUT, ?> containingTask,
            StreamConfig operatorConfig,
            Map<Integer, StreamConfig> chainedConfigs,
            ClassLoader userCodeClassloader,
            Map<StreamEdge, RecordWriterOutput<?>> streamOutputs,
            List<StreamOperatorWrapper<?, ?>> allOperatorWrappers,
            OutputTag<IN> outputTag,
            MailboxExecutorFactory mailboxExecutorFactory) {
        // create the output that the operator writes to first. this may recursively create more
        // operators
        WatermarkGaugeExposingOutput<StreamRecord<OUT>> chainedOperatorOutput =
                createOutputCollector(
                        containingTask,
                        operatorConfig,
                        chainedConfigs,
                        userCodeClassloader,
                        streamOutputs,
                        allOperatorWrappers,
                        mailboxExecutorFactory);

        OneInputStreamOperator<IN, OUT> chainedOperator =
                createOperator(
                        containingTask,
                        operatorConfig,
                        userCodeClassloader,
                        chainedOperatorOutput,
                        allOperatorWrappers,
                        false);

        // 返回 ChainingOutput 或  CopyingChainingOutput 对象 ; 并把 相关算子 封装进入 该对象中
        return wrapOperatorIntoOutput(
                chainedOperator, containingTask, operatorConfig, userCodeClassloader, outputTag);
    }

    /**
     * Create and return a single operator from the given {@param operatorConfig} that will be
     * producing records to the {@param output}.
     */
    private <OUT, OP extends StreamOperator<OUT>> OP createOperator(
            StreamTask<OUT, ?> containingTask,
            StreamConfig operatorConfig,
            ClassLoader userCodeClassloader,
            WatermarkGaugeExposingOutput<StreamRecord<OUT>> output,
            List<StreamOperatorWrapper<?, ?>> allOperatorWrappers,
            boolean isHead) {

        // now create the operator and give it the output collector to write its output to
        Tuple2<OP, Optional<ProcessingTimeService>> chainedOperatorAndTimeService =
                StreamOperatorFactoryUtil.createOperator(
                        operatorConfig.getStreamOperatorFactory(userCodeClassloader),
                        containingTask,
                        operatorConfig,
                        output,
                        operatorEventDispatcher);

        OP chainedOperator = chainedOperatorAndTimeService.f0;
        allOperatorWrappers.add(
                createOperatorWrapper(
                        chainedOperator,
                        containingTask,
                        operatorConfig,
                        chainedOperatorAndTimeService.f1,
                        isHead));

        chainedOperator
                .getMetricGroup()
                .gauge(
                        MetricNames.IO_CURRENT_OUTPUT_WATERMARK,
                        output.getWatermarkGauge()::getValue);
        return chainedOperator;
    }

    private <IN, OUT> WatermarkGaugeExposingOutput<StreamRecord<IN>> wrapOperatorIntoOutput(
            OneInputStreamOperator<IN, OUT> operator,
            StreamTask<OUT, ?> containingTask,
            StreamConfig operatorConfig,
            ClassLoader userCodeClassloader,
            OutputTag<IN> outputTag) {

        WatermarkGaugeExposingOutput<StreamRecord<IN>> currentOperatorOutput;
        if (containingTask.getExecutionConfig().isObjectReuseEnabled()) {
            currentOperatorOutput = new ChainingOutput<>(operator, outputTag);
        } else {
            TypeSerializer<IN> inSerializer =
                    operatorConfig.getTypeSerializerIn1(userCodeClassloader);
            currentOperatorOutput = new CopyingChainingOutput<>(operator, inSerializer, outputTag);
        }

        // wrap watermark gauges since registered metrics must be unique
        operator.getMetricGroup()
                .gauge(
                        MetricNames.IO_CURRENT_INPUT_WATERMARK,
                        currentOperatorOutput.getWatermarkGauge()::getValue);

        return closer.register(currentOperatorOutput);
    }

    /**
     * Links operator wrappers in forward topological order.
     *
     * @param allOperatorWrappers is an operator wrapper list of reverse topological order
     */
    private StreamOperatorWrapper<?, ?> linkOperatorWrappers(
            List<StreamOperatorWrapper<?, ?>> allOperatorWrappers) {
        StreamOperatorWrapper<?, ?> previous = null;
        for (StreamOperatorWrapper<?, ?> current : allOperatorWrappers) {
            if (previous != null) {
                previous.setPrevious(current);
            }
            current.setNext(previous);
            previous = current;
        }
        return previous;
    }

    private <T, P extends StreamOperator<T>> StreamOperatorWrapper<T, P> createOperatorWrapper(
            P operator,
            StreamTask<?, ?> containingTask,
            StreamConfig operatorConfig,
            Optional<ProcessingTimeService> processingTimeService,
            boolean isHead) {
        return new StreamOperatorWrapper<>(
                operator,
                processingTimeService,
                containingTask
                        .getMailboxExecutorFactory()
                        .createExecutor(operatorConfig.getChainIndex()),
                isHead);
    }
}
