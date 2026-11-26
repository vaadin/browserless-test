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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.example.base.signals.SignalsView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalEnvironment;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedNumberSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

@ViewPackages(packages = "com.example.base.signals")
@Timeout(10)
public class SignalsTest extends BrowserlessTest {

    @Test
    void attachedComponent_triggerSignal_effectEvaluatedSynchronously() {
        var view = navigate(SignalsView.class);
        var counterTester = test(view.counter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());

        test(view.incrementButton).click();
        Assertions.assertEquals("Counter: 1", counterTester.getText());
    }

    @Test
    void detachedComponent_triggerSignal_effectEvaluatedOnAttach() {
        var view = navigate(SignalsView.class);
        var counterTester = test(view.counter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());
        view.counter.removeFromParent();
        Assertions.assertFalse(counterTester.isUsable());

        test(view.incrementButton).click();
        Assertions.assertEquals("Counter: 0", view.counter.getText());

        view.add(view.counter);
        Assertions.assertEquals("Counter: 1", view.counter.getText());
    }

    @Test
    void attachedComponent_triggerSignalFromNonUIThread_effectEvaluatedAsynchronously() {
        var view = navigate(SignalsView.class);
        var counterTester = test(view.asyncCounter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());
        CompletableFuture.runAsync(() -> {
            view.asyncNumberSignal.incrementBy(10.0);
        });
        runPendingSignalsTasks();
        Assertions.assertEquals("Counter: 10", counterTester.getText());
    }

    @Test
    void roundTrip_runsEffectOfFinishedBackgroundSignalChange() {
        var view = navigate(SignalsView.class);
        CompletableFuture
                .runAsync(() -> view.asyncNumberSignal.incrementBy(10.0))
                .join();

        roundTrip();

        Assertions.assertEquals("Counter: 10", view.asyncCounter.getText());
    }

    @Test
    void lookup_runsEffectOfFinishedBackgroundSignalChange() {
        var view = navigate(SignalsView.class);
        CompletableFuture
                .runAsync(() -> view.asyncNumberSignal.incrementBy(10.0))
                .join();

        Assertions.assertSame(view.asyncCounter,
                find(Span.class).withText("Counter: 10").single());
    }

    @Test
    void attachedComponent_triggerSignalFromNonUIThreadThroughComponentEffect_effectEvaluatedAsynchronously() {
        var view = navigate(SignalsView.class);
        var counterTester = test(view.asyncCounter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());
        test(view.quickBackgroundTaskButton).click();
        runPendingSignalsTasks(300, TimeUnit.MILLISECONDS);
        Assertions.assertEquals("Counter: 10", counterTester.getText());
    }

    @Test
    void effectDispatcher_routesToTestQueue_notServiceThreadPool()
            throws InterruptedException {
        // On the test thread, both VaadinServiceEnvironment (from
        // MockVaadin) and TestSignalEnvironment are active. The default
        // effect dispatcher must resolve to TestSignalEnvironment's queue
        // (via registerFirst) so that runPendingSignalsTasks() can drive
        // effect execution deterministically. Without registerFirst,
        // VaadinServiceEnvironment's thread pool would be used instead,
        // making effects run asynchronously outside the test's control.
        navigate(SignalsView.class);

        var latch = new CountDownLatch(1);
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(latch::countDown);

        Assertions.assertFalse(latch.await(50, TimeUnit.MILLISECONDS),
                "Task should be queued, not executed immediately on a "
                        + "thread pool");
        runPendingSignalsTasks();
        Assertions.assertTrue(latch.await(0, TimeUnit.MILLISECONDS),
                "Task should have executed after draining the test queue");
    }

    @Test
    void attachedComponent_slowEffect_effectEvaluatedAsynchronously() {
        var view = navigate(SignalsView.class);
        var counterTester = test(view.asyncWithDelayCounter);
        Assertions.assertEquals("Counter: 0 (delayed)",
                counterTester.getText());
        test(view.slowBackgroundTaskButton).click();
        Assertions.assertTrue(
                runPendingSignalsTasks(300, TimeUnit.MILLISECONDS),
                "Expected pending signals tasks to be run");
        Assertions.assertEquals("Counter: 10 (delayed)",
                counterTester.getText());
    }

    @Test
    void sharedListSignal_insert_operationConfirmedByRunPendingSignalsTasks() {
        var tickets = new SharedListSignal<>(String.class);

        var operation = tickets.insertLast("a ticket");

        // The change is applied optimistically right away, but the
        // confirmation of a shared signal write is dispatched through the
        // Signals task queue and therefore stays pending until the test
        // drains it.
        Assertions.assertEquals(1, tickets.peek().size());
        Assertions.assertFalse(operation.result().isDone(),
                "Operation should still be unconfirmed before pending "
                        + "Signals tasks are run");

        Assertions.assertTrue(runPendingSignalsTasks(),
                "Expected a pending confirmation task to be run");
        Assertions.assertTrue(operation.result().isDone(),
                "Operation should be confirmed after running pending "
                        + "Signals tasks");
        Assertions.assertTrue(operation.result().join().successful(),
                "Insert operation should have succeeded");
    }

    @Test
    void cleanVaadinEnvironment_runsPendingTasksOnTestThread() {
        // Outlives the test, like a signal in an application-scoped bean
        var signal = new SharedValueSignal<>("initial");
        SignalEnvironment.getDefaultEffectDispatcher().execute(() -> {
            throw new IllegalStateException("failing pending task");
        });
        signal.set("changed");
        Assertions.assertNull(signal.peekConfirmed(),
                "Nothing should be confirmed before the queue is drained");
        var nestedTaskThread = new AtomicReference<Thread>();
        SignalEnvironment.getDefaultEffectDispatcher().execute(
                () -> SignalEnvironment.getDefaultEffectDispatcher().execute(
                        () -> nestedTaskThread.set(Thread.currentThread())));

        cleanVaadinEnvironment();
        initVaadinEnvironment();

        Assertions.assertEquals("changed", signal.peekConfirmed(),
                "A write left pending at teardown must still be confirmed");
        Assertions.assertSame(Thread.currentThread(), nestedTaskThread.get(),
                "A task submitted by a pending task must run on the test "
                        + "thread during teardown");
    }

    @Test
    void cleanVaadinEnvironment_runsPendingEffectWhileUIIsOpen() {
        var view = navigate(SignalsView.class);
        CompletableFuture
                .runAsync(() -> view.asyncNumberSignal.incrementBy(10.0))
                .join();

        cleanVaadinEnvironment();
        initVaadinEnvironment();

        Assertions.assertEquals("Counter: 10", view.asyncCounter.getText());
    }

    @Test
    void cleanVaadinEnvironment_runsTasksQueuedByDetachListenerOnTestThread() {
        var view = navigate(SignalsView.class);
        // Outlives the test, like an application-scoped "users online"
        // counter
        var signal = new SharedNumberSignal();
        view.addDetachListener(event -> signal.incrementBy(-1));
        var effectThread = new AtomicReference<Thread>();
        var effect = Signal.unboundEffect(() -> {
            if (signal.get() == -1) {
                effectThread.set(Thread.currentThread());
            }
        });

        cleanVaadinEnvironment();
        initVaadinEnvironment();
        effect.remove();

        Assertions.assertSame(Thread.currentThread(), effectThread.get(),
                "Tasks queued while the UI closes must run on the test thread");
        Assertions.assertEquals(-1.0, signal.peekConfirmed(),
                "A write made while the UI closes must be confirmed");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void selfResubmittingTask_stopsAtLimit(boolean byRoundTrip) {
        if (byRoundTrip) {
            navigate(SignalsView.class);
            runPendingSignalsTasks(0, TimeUnit.MILLISECONDS);
        }
        var runs = new AtomicInteger();
        SignalEnvironment.getDefaultEffectDispatcher().execute(new Runnable() {
            @Override
            public void run() {
                // Stops on its own one run past the limit, so that a missing
                // limit fails the test instead of hanging it
                if (runs.incrementAndGet() <= TestSignalEnvironment.MAX_TEARDOWN_TASKS) {
                    SignalEnvironment.getDefaultEffectDispatcher()
                            .execute(this);
                }
            }
        });

        if (byRoundTrip) {
            roundTrip();
        } else {
            cleanVaadinEnvironment();
            initVaadinEnvironment();
        }

        Assertions.assertEquals(TestSignalEnvironment.MAX_TEARDOWN_TASKS,
                runs.get());
    }

    @Test
    void roundTrip_throwsFailureOfSignalsTaskAndKeepsLaterTasksQueued() {
        navigate(SignalsView.class);
        var laterTaskRan = new AtomicReference<Boolean>(false);
        SignalEnvironment.getDefaultEffectDispatcher().execute(() -> {
            throw new IllegalStateException("failing task");
        });
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(() -> laterTaskRan.set(true));

        var failure = Assertions.assertThrows(IllegalStateException.class,
                BrowserlessTest::roundTrip);
        Assertions.assertEquals("failing task", failure.getMessage());
        Assertions.assertFalse(laterTaskRan.get());

        roundTrip();
        Assertions.assertTrue(laterTaskRan.get());
    }

    @Test
    void roundTrip_rethrowsFailureOfAccessQueuedBySignalsTask() {
        navigate(SignalsView.class);
        var ui = UI.getCurrent();
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(() -> ui.access(() -> {
                    throw new IllegalStateException("failing access");
                }));

        var failure = Assertions.assertThrows(ExecutionException.class,
                BrowserlessTest::roundTrip);
        Assertions.assertEquals("failing access",
                failure.getCause().getMessage());
    }

}
