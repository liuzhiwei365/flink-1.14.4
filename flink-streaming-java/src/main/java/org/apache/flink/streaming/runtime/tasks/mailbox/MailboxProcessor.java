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

package org.apache.flink.streaming.runtime.tasks.mailbox;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.flink.streaming.runtime.tasks.StreamTaskActionExecutor;
import org.apache.flink.streaming.runtime.tasks.mailbox.TaskMailbox.MailboxClosedException;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.WrappingRuntimeException;
import org.apache.flink.util.function.RunnableWithException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

import static org.apache.flink.streaming.runtime.tasks.mailbox.TaskMailbox.MIN_PRIORITY;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * This class encapsulates the logic of the mailbox-based execution model. At the core of this model
 * {@link #runMailboxLoop()} that continuously executes the provided {@link MailboxDefaultAction} in
 * a loop. On each iteration, the method also checks if there are pending actions in the mailbox and
 * executes such actions. This model ensures single-threaded execution between the default action
 * (e.g. record processing) and mailbox actions (e.g. checkpoint trigger, timer firing, ...).
 *
 * <p>The {@link MailboxDefaultAction} interacts with this class through the {@link
 * MailboxController} to communicate control flow changes to the mailbox loop, e.g. that invocations
 * of the default action are temporarily or permanently exhausted.
 *
 * <p>The design of {@link #runMailboxLoop()} is centered around the idea of keeping the expected
 * hot path (default action, no mail) as fast as possible. This means that all checking of mail and
 * other control flags (mailboxLoopRunning, suspendedDefaultAction) are always connected to #hasMail
 * indicating true. This means that control flag changes in the mailbox thread can be done directly,
 * but we must ensure that there is at least one action in the mailbox so that the change is picked
 * up. For control flag changes by all other threads, that must happen through mailbox actions, this
 * is automatically the case.
 *
 * <p>This class has a open-prepareClose-close lifecycle that is connected with and maps to the
 * lifecycle of the encapsulated {@link TaskMailbox} (which is open-quiesce-close).
 */
@Internal
public class MailboxProcessor implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(MailboxProcessor.class);

    /**
     * The mailbox data-structure that manages request for special actions, like timers,
     * checkpoints, ...
     */
    protected final TaskMailbox mailbox;

    /**
     * Action that is repeatedly executed if no action request is in the mailbox. Typically record
     * processing.
     */
    //如果邮箱中没有 特殊的操作请求，则重复执行的操作;     通常是 处理用户数据
    protected final MailboxDefaultAction mailboxDefaultAction;

    /**
     * Control flag to terminate the mailbox processor. Once it was terminated could not be
     * restarted again. Must only be accessed from mailbox thread.
     */
    // 控制 信箱处理 的标志, 一旦终止,将不能恢复
    // 初始值为true
    private boolean mailboxLoopRunning;

    /**
     * Control flag to temporary suspend the mailbox loop/processor. After suspending the mailbox
     * processor can be still later resumed. Must only be accessed from mailbox thread.
     */
    // 控制 信箱挂起 的标志,挂起后,还可以重新恢复
    // 初始值为false
    private boolean suspended;

    /**
     * Remembers a currently active suspension of the default action. Serves as flag to indicate a
     * suspended default action (suspended if not-null) and to reuse the object as return value in
     * consecutive suspend attempts. Must only be accessed from mailbox thread.
     */
    //用作指示 用户数据处理的挂起 （如果不是null则挂起）的标志    并在连续的挂起尝试中 将重用对象 作为返回值。  只能从邮箱线程访问
    private DefaultActionSuspension suspendedDefaultAction;

    private final StreamTaskActionExecutor actionExecutor;

    @VisibleForTesting
    public MailboxProcessor() {
        this(MailboxDefaultAction.Controller::suspendDefaultAction);
    }

    public MailboxProcessor(MailboxDefaultAction mailboxDefaultAction) {
        this(mailboxDefaultAction, StreamTaskActionExecutor.IMMEDIATE);
    }

    public MailboxProcessor(
            MailboxDefaultAction mailboxDefaultAction, StreamTaskActionExecutor actionExecutor) {
        this(mailboxDefaultAction, new TaskMailboxImpl(Thread.currentThread()), actionExecutor);
    }

    public MailboxProcessor(
            MailboxDefaultAction mailboxDefaultAction,
            TaskMailbox mailbox,
            StreamTaskActionExecutor actionExecutor) {
        this.mailboxDefaultAction = Preconditions.checkNotNull(mailboxDefaultAction);
        this.actionExecutor = Preconditions.checkNotNull(actionExecutor);
        this.mailbox = Preconditions.checkNotNull(mailbox);
        this.mailboxLoopRunning = true;
        this.suspendedDefaultAction = null;
    }

    // 一个不传
    public MailboxExecutor getMainMailboxExecutor() {
        return new MailboxExecutorImpl(mailbox, MIN_PRIORITY, actionExecutor);
    }
    // 传入信箱处理器
    public MailboxExecutor getMailboxExecutor(int priority) {
        return new MailboxExecutorImpl(mailbox, priority, actionExecutor, this);
    }

    /** Lifecycle method to close the mailbox for action submission. */
    public void prepareClose() {
        mailbox.quiesce();
    }

    /**
     * Lifecycle method to close the mailbox for action submission/retrieval. This will cancel all
     * instances of {@link java.util.concurrent.RunnableFuture} that are still contained in the
     * mailbox.
     */
    @Override
    public void close() {
        List<Mail> droppedMails = mailbox.close();
        if (!droppedMails.isEmpty()) {
            LOG.debug("Closing the mailbox dropped mails {}.", droppedMails);
            Optional<RuntimeException> maybeErr = Optional.empty();
            for (Mail droppedMail : droppedMails) {
                try {
                    droppedMail.tryCancel(false);
                } catch (RuntimeException x) {
                    maybeErr =
                            Optional.of(ExceptionUtils.firstOrSuppressed(x, maybeErr.orElse(null)));
                }
            }
            maybeErr.ifPresent(
                    e -> {
                        throw e;
                    });
        }
    }

    /**
     * Finishes running all mails in the mailbox. If no concurrent write operations occurred, the
     * mailbox must be empty after this method.
     */
    public void drain() throws Exception {
        for (final Mail mail : mailbox.drain()) {
            mail.run();
        }
    }

    /**
     * Runs the mailbox processing loop. This is where the main work is done. This loop can be
     * suspended at any time by calling {@link #suspend()}. For resuming the loop this method should
     * be called again.
     */
    public void runMailboxLoop() throws Exception {
        suspended = !mailboxLoopRunning;

        final TaskMailbox localMailbox = mailbox;

        checkState(
                localMailbox.isMailboxThread(),
                "Method must be executed by declared mailbox thread!");

        assert localMailbox.getState() == TaskMailbox.State.OPEN : "Mailbox must be opened!";

        final MailboxController defaultActionContext = new MailboxController(this);

        while (isNextLoopPossible()) {
            // 此方法处理邮箱中的所有特殊操作 , 而不是处理用户数据
            // 在默认操作（也就是处理数据数据）可用之前，会一直处理 mailbox,如果mailbox中的元素处理完
            // 则, 内部会一直用task方法阻塞 “processMail” , 调用不会返回
            processMail(localMailbox, false);

            if (isNextLoopPossible()) {
                // MailboxProcessor在构造的时候 , mailboxDefaultAction赋值的就是 StreamTask::processInput方法
                // MailboxDefaultAction 可以认为是一个  Consume 类型的 函数式接口
                // 调用 runDefaultAction 方法 相当于调用了   StreamTask::processInput方法 (也可能是 子类的 processInput 方法)
                mailboxDefaultAction.runDefaultAction(
                        defaultActionContext); // lock is acquired inside default action as needed
            }
        }
    }

    /** Suspend the running of the loop which was started by {@link #runMailboxLoop()}}. */
    public void suspend() {
        sendPoisonMail(() -> suspended = true);
    }

    /**
     * Execute a single (as small as possible) step of the mailbox.
     *
     * @return true if something was processed.
     */
    // 功能还在测试阶段
    @VisibleForTesting
    public boolean runMailboxStep() throws Exception {
        suspended = !mailboxLoopRunning;

        if (processMail(mailbox, true)) {
            return true;
        }
        if (isDefaultActionAvailable() && isNextLoopPossible()) {
            mailboxDefaultAction.runDefaultAction(new MailboxController(this));
            return true;
        }
        return false;
    }

    /**
     * Check if the current thread is the mailbox thread.
     *
     * @return only true if called from the mailbox thread.
     */
    public boolean isMailboxThread() {
        return mailbox.isMailboxThread();
    }

    /**
     * Reports a throwable for rethrowing from the mailbox thread. This will clear and cancel all
     * other pending mails.
     *
     * @param throwable to report by rethrowing from the mailbox loop.
     */
    public void reportThrowable(Throwable throwable) {
        sendControlMail(
                () -> {
                    if (throwable instanceof Exception) {
                        throw (Exception) throwable;
                    } else if (throwable instanceof Error) {
                        throw (Error) throwable;
                    } else {
                        throw WrappingRuntimeException.wrapIfNecessary(throwable);
                    }
                },
                "Report throwable %s",
                throwable);
    }

    /**
     * This method must be called to end the stream task when all actions for the tasks have been
     * performed.
     */
    public void allActionsCompleted() {
        sendPoisonMail(
                () -> {
                    mailboxLoopRunning = false;
                    suspended = true;
                });
    }

    /** Send mail in first priority for internal needs. */
    private void sendPoisonMail(RunnableWithException mail) {
        mailbox.runExclusively(
                () -> {
                    // keep state check and poison mail enqueuing atomic, such that no intermediate
                    // #close may cause a
                    // MailboxStateException in #sendPriorityMail.
                    if (mailbox.getState() == TaskMailbox.State.OPEN) {
                        sendControlMail(mail, "poison mail");
                    }
                });
    }

    /**
     * Sends the given <code>mail</code> using {@link TaskMailbox#putFirst(Mail)} . Intended use is
     * to control this <code>MailboxProcessor</code>; no interaction with tasks should be performed;
     */
    private void sendControlMail(
            RunnableWithException mail, String descriptionFormat, Object... descriptionArgs) {
        mailbox.putFirst(
                new Mail(
                        mail,
                        Integer.MAX_VALUE /*not used with putFirst*/,
                        descriptionFormat,
                        descriptionArgs));
    }

    /**
     * This helper method handles all special actions from the mailbox. In the current design, this
     * method also evaluates all control flag changes. This keeps the hot path in {@link
     * #runMailboxLoop()} free from any other flag checking, at the cost that all flag changes must
     * make sure that the mailbox signals mailbox#hasMail.
     *
     * @return true if a mail has been processed.
     */
    //  此方法处理邮箱中的所有特殊操作 , 而不是处理用户数据
    //  在当前设计中，此方法还评估 所有控制标志 的更改
    private boolean processMail(TaskMailbox mailbox, boolean singleStep) throws Exception {

        // batch队列中是否有数据 （会把将 queue中的数据全部 移动到 batch中）
        boolean isBatchAvailable = mailbox.createBatch();

        // 只会非阻塞地处理 batch中的元素,不会处理 queue中的元素
        boolean processed = isBatchAvailable && processMailsNonBlocking(singleStep);
        if (singleStep) {
            return processed;
        }

        // 非阻塞地处理 batch;  阻塞地处理 queue
        // 如果默认操作（用户数据处理逻辑） 当前不可用,被挂起, 我们可以运行阻塞邮箱执行, 直到默认操作再次可用
        processed |= processMailsWhenDefaultActionUnavailable();

        return processed;
    }

    private boolean processMailsWhenDefaultActionUnavailable() throws Exception {
        boolean processedSomething = false;
        Optional<Mail> maybeMail;
        // 当 ! suspendedDefaultAction == null 且 信箱处理器没有被挂起
        // 也就是 信箱处理器没有被挂起, 但是 处理用户数据的逻辑 被挂起了  （可见, 处理 checkpoint 和 处理用户数据 有一定的互斥性；用户数据量过大可能会影响checkpoint的逻辑）
        while (!isDefaultActionAvailable() && isNextLoopPossible()) {
            // 非阻塞地拿
            maybeMail = mailbox.tryTake(MIN_PRIORITY);
            if (!maybeMail.isPresent()) {
                // 前面没拿到, 继续阻塞地拿 （前面的非阻塞拿,效率高些,并非多此一举）
                maybeMail = Optional.of(mailbox.take(MIN_PRIORITY));
            }
            maybePauseIdleTimer();
            // 运行 mail 封装的任务
            maybeMail.get().run();
            maybeRestartIdleTimer();
            processedSomething = true;
        }
        return processedSomething;
    }

    // singleStep 为true 则表示只处理一个 Mail 对象
    // 如果处理过 Mail 则 返回true , 没有真实处理过Mail 则返回false
    private boolean processMailsNonBlocking(boolean singleStep) throws Exception {
        long processedMails = 0;
        Optional<Mail> maybeMail;

        // 不断从 batch 队列头 拉取Mail, 并进行处理
        while (isNextLoopPossible() && (maybeMail = mailbox.tryTakeFromBatch()).isPresent()) {
            if (processedMails++ == 0) {
                maybePauseIdleTimer();
            }
            maybeMail.get().run();
            if (singleStep) {
                break;
            }
        }
        if (processedMails > 0) {
            maybeRestartIdleTimer();
            return true;
        } else {
            return false;
        }
    }

    private void maybePauseIdleTimer() {
        if (suspendedDefaultAction != null && suspendedDefaultAction.suspensionTimer != null) {
            suspendedDefaultAction.suspensionTimer.markEnd();
        }
    }

    private void maybeRestartIdleTimer() {
        if (suspendedDefaultAction != null && suspendedDefaultAction.suspensionTimer != null) {
            suspendedDefaultAction.suspensionTimer.markStart();
        }
    }

    /**
     * Calling this method signals that the mailbox-thread should (temporarily) stop invoking the
     * default action, e.g. because there is currently no input available.
     */
    // 通过给 suspendedDefaultAction 赋值的方式 , 来挂起 用户数据处理的逻辑
    private MailboxDefaultAction.Suspension suspendDefaultAction(
            @Nullable PeriodTimer suspensionTimer) {

        checkState(
                mailbox.isMailboxThread(),
                "Suspending must only be called from the mailbox thread!");

        checkState(suspendedDefaultAction == null, "Default action has already been suspended");
        if (suspendedDefaultAction == null) {
            suspendedDefaultAction = new DefaultActionSuspension(suspensionTimer);
        }

        return suspendedDefaultAction;
    }

    @VisibleForTesting
    public boolean isDefaultActionAvailable() {
        return suspendedDefaultAction == null;
    }

    private boolean isNextLoopPossible() {
        // 'Suspended' can be false only when 'mailboxLoopRunning' is true.
        return !suspended;
    }

    @VisibleForTesting
    public boolean isMailboxLoopRunning() {
        return mailboxLoopRunning;
    }

    @VisibleForTesting
    public boolean hasMail() {
        return mailbox.hasMail();
    }

    /**
     * Implementation of {@link MailboxDefaultAction.Controller} that is connected to a {@link
     * MailboxProcessor} instance.
     */
    protected static final class MailboxController implements MailboxDefaultAction.Controller {

        private final MailboxProcessor mailboxProcessor;

        protected MailboxController(MailboxProcessor mailboxProcessor) {
            this.mailboxProcessor = mailboxProcessor;
        }

        @Override
        public void allActionsCompleted() {
            mailboxProcessor.allActionsCompleted();
        }

        @Override
        public MailboxDefaultAction.Suspension suspendDefaultAction(
                PeriodTimer suspensionPeriodTimer) {
            return mailboxProcessor.suspendDefaultAction(suspensionPeriodTimer);
        }

        @Override
        public MailboxDefaultAction.Suspension suspendDefaultAction() {
            return mailboxProcessor.suspendDefaultAction(null);
        }
    }

    /**
     * Represents the suspended state of the default action and offers an idempotent method to
     * resume execution.
     */
    private final class DefaultActionSuspension implements MailboxDefaultAction.Suspension {
        @Nullable private final PeriodTimer suspensionTimer;

        public DefaultActionSuspension(@Nullable PeriodTimer suspensionTimer) {
            this.suspensionTimer = suspensionTimer;
        }

        @Override
        public void resume() {
            if (mailbox.isMailboxThread()) {
                resumeInternal();
            } else {
                try {
                    sendControlMail(this::resumeInternal, "resume default action");
                } catch (MailboxClosedException ex) {
                    // Ignored
                }
            }
        }

        private void resumeInternal() {
            if (suspendedDefaultAction == this) {
                suspendedDefaultAction = null;
            }
        }
    }
}
