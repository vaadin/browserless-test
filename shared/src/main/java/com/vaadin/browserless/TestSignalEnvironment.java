/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.browserless;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.SignalEnvironment;

/**
 * Test-only {@link SignalEnvironment} that records submitted tasks instead of
 * executing them asynchronously. This allows unit tests to deterministically
 * drive and observe Signal processing by explicitly flushing the task queue.
 *
 * <p>
 * How it works:
 * <ul>
 * <li>{@link #getEffectDispatcher()} returns an executor that enqueues tasks
 * into an internal queue. {@link #getResultNotifier()} returns {@code null} so
 * that result notifications fall through to the next environment or run
 * immediately.</li>
 * <li>Tests call {@link #runPendingTasks(long, TimeUnit)} to dequeue and run
 * all pending tasks on the calling thread.</li>
 * <li>If the current thread holds a {@link VaadinSession} lock, the lock is
 * temporarily released so that background threads have a chance of locking the
 * session.</li>
 * </ul>
 *
 * <p>
 * Shared signals ({@code SharedValueSignal}, {@code SharedListSignal} and
 * friends) back onto a
 * {@code com.vaadin.flow.signals.shared.impl.LocalAsynchronousSignalTree},
 * which confirms submitted commands through the effect dispatcher. Their writes
 * are therefore visible immediately, while the {@code SignalOperation} returned
 * by the write is only completed once the queued confirmation task is executed
 * by {@link #runPendingTasks(long, TimeUnit)}.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * {@code
 * TestSignalEnvironment env = TestSignalEnvironment.register();
 * try {
 *     // trigger signals here
 *     env.waitForTasksCompletion(100, TimeUnit.MILLISECONDS);
 * } finally {
 *     env.unregister();
 * }
 * }
 * </pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public class TestSignalEnvironment extends SignalEnvironment {

    /**
     * The maximum number of pending tasks that {@link #unregister()} runs, so
     * that a task that keeps submitting new ones cannot hang teardown.
     */
    static final int MAX_TEARDOWN_TASKS = 10_000;

    private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private Runnable cleanup;

    protected TestSignalEnvironment() {
    }

    /**
     * Registers this test environment as active {@link SignalEnvironment} and
     * returns the created instance.
     *
     * <p>
     * Use together with {@link #unregister()} in a try/finally block to ensure
     * proper cleanup.
     *
     * @return a new registered {@link TestSignalEnvironment} instance
     */
    public static TestSignalEnvironment register() {
        TestSignalEnvironment environment = new TestSignalEnvironment();
        environment.cleanup = SignalEnvironment.registerFirst(environment);
        return environment;
    }

    /**
     * Runs the tasks that are still pending on the calling thread, and then
     * unregisters this test environment if it was registered.
     *
     * <p>
     * A shared signal often outlives the test that wrote to it, for example
     * when it is held by an application-scoped bean or a static field. If the
     * confirmation of such a write were discarded with the queue, the signal
     * would keep the write unconfirmed for good, and every later conditional
     * update of it, such as {@code SharedValueSignal.update(...)}, would be
     * rejected and retried forever. Running the pending tasks confirms those
     * writes. Pending effects of UIs that are still open run as well, which is
     * why teardown also runs the pending tasks before it closes the UIs. The
     * environment stays registered while the tasks run, so tasks they submit
     * are queued and run here too, on the calling thread, up to a limit of
     * {@value #MAX_TEARDOWN_TASKS} tasks; tasks beyond it are discarded with a
     * warning. A task that fails is logged and skipped, so that the remaining
     * tasks still run and teardown completes. Only a
     * {@link VirtualMachineError}, such as running out of memory, is passed on,
     * after the environment has been unregistered.
     * <p>
     * The tasks run one after another on the calling thread, so a pending task
     * must not block waiting for another one. For example, an effect that calls
     * {@code signal.update(...).result().join()} waits for a confirmation
     * queued behind it and hangs teardown, which test timeouts do not cover.
     *
     * <p>
     * This method is idempotent and safe to call multiple times. If a cleanup
     * action was provided by
     * {@link SignalEnvironment#register(SignalEnvironment)}, it will be
     * invoked.
     */
    public void unregister() {
        if (cleanup != null) {
            try {
                runRemainingTasks();
            } finally {
                cleanup.run();
                cleanup = null;
            }
        }
    }

    /**
     * Runs the tasks that are still pending on the calling thread, like
     * {@link #unregister()} does, but keeps this environment registered.
     * <p>
     * Teardown calls this before it closes the windows, so that pending effects
     * still see open UIs, and calls {@link #unregister()} only after the
     * windows are closed. Tasks queued while closing, such as the confirmation
     * of a shared signal write made by a detach or session destroy listener,
     * are then still queued here and run on the calling thread, instead of
     * going to the next registered {@link SignalEnvironment}, which may run
     * them asynchronously after the next test has started.
     */
    void runRemainingTasks() {
        VaadinSession session = VaadinSession.getCurrent();
        boolean hasLock = session != null && session.hasLock();
        int run = 0;
        Runnable task;
        while ((task = tasks.poll()) != null) {
            if (run++ == MAX_TEARDOWN_TASKS) {
                tasks.clear();
                LoggerFactory.getLogger(TestSignalEnvironment.class).warn(
                        "Discarded pending Signals tasks after running {} of them at teardown; a task probably keeps submitting new ones",
                        MAX_TEARDOWN_TASKS);
                return;
            }
            try {
                task.run();
            } catch (VirtualMachineError e) {
                // The JVM may be unusable, so do not carry on silently
                throw e;
            } catch (Throwable e) {
                // Any other failure, not only exceptions, must leave the rest
                // of the queue and the caller's teardown running
                LoggerFactory.getLogger(TestSignalEnvironment.class).warn(
                        "Skipped a pending Signals task that failed at teardown",
                        e);
            } finally {
                if (hasLock) {
                    // Unlocking runs the UI.access() tasks that the task
                    // queued, as it does between tasks in runPendingTasks()
                    MockVaadin.unlockRunningUIQueue(session);
                    session.lock();
                }
            }
        }
    }

    @Override
    protected boolean isActive() {
        // Test environment is always active to catch all signals task submitted
        // when a Vaadin session or service is not available, for example when
        // effects are triggered by background threads.
        return true;
    }

    @Override
    protected Executor getResultNotifier() {
        // Return null so result notifications fall through to the next
        // environment (e.g. VaadinServiceEnvironment) or to the immediate
        // executor. This keeps result processing synchronous on the calling
        // thread, which is important for deterministic test behavior when
        // signal operations are triggered on the test thread.
        return null;
    }

    @Override
    protected Executor getEffectDispatcher() {
        return tasks::offer;
    }

    /**
     * Executes pending tasks from the queue, continuously polling for new tasks
     * until the timeout expires with no new task arriving.
     *
     * <p>
     * If a {@link VaadinSession} lock is held by the current thread, it is
     * temporarily released while polling for tasks, allowing background threads
     * to acquire the lock and enqueue tasks. The lock is reacquired before
     * running each task and released again before the next poll.
     *
     * <p>
     * If the current thread is interrupted while waiting for tasks, the method
     * restores the interrupt status and fails with an {@link AssertionError}.
     *
     * @param maxWaitTime
     *            the maximum time to wait for the next task to arrive in the
     *            given time unit. If &lt;= 0, returns immediately if no tasks
     *            are available.
     * @param unit
     *            the time unit of the timeout value
     * @return {@code true} if any pending Signals tasks were processed.
     */
    public boolean runPendingTasks(long maxWaitTime, TimeUnit unit) {
        long deadlineMillis = System.currentTimeMillis()
                + unit.toMillis(maxWaitTime);
        VaadinSession session = VaadinSession.getCurrent();
        boolean hadLock = false;
        if (session != null && session.hasLock()) {
            hadLock = true;
            MockVaadin.unlockRunningUIQueue(session);
        }
        try {
            boolean processedAny = false;
            while (true) {
                long remainingMillis = deadlineMillis
                        - System.currentTimeMillis();
                Runnable task;
                try {
                    task = remainingMillis > 0
                            ? tasks.poll(remainingMillis, TimeUnit.MILLISECONDS)
                            : tasks.poll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(
                            "Thread interrupted while waiting for pending Signals tasks");
                }
                if (task == null) {
                    if (!processedAny) {
                        LoggerFactory.getLogger(TestSignalEnvironment.class)
                                .debug("No pending Signals tasks found after waiting for {} {}",
                                        maxWaitTime, unit);
                    }
                    break;
                }
                // Re-acquire the session lock before running the task so
                // that DOM operations (which assert the lock is held) work
                // correctly when the effect runs directly on the test
                // thread instead of going through ui.access().
                if (hadLock) {
                    session.lock();
                }
                try {
                    task.run();
                } finally {
                    if (hadLock) {
                        MockVaadin.unlockRunningUIQueue(session);
                    }
                }
                processedAny = true;
            }
            return processedAny;
        } finally {
            if (hadLock) {
                session.lock();
            }
        }
    }

}
