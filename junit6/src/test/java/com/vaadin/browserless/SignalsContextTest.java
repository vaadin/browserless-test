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
import java.util.concurrent.TimeUnit;

import com.example.base.signals.SignalsView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.vaadin.flow.signals.SignalEnvironment;

/**
 * Mirrors {@link SignalsTest} but drives the scenarios through the
 * {@link BrowserlessApplicationContext} / {@link BrowserlessUserContext} /
 * {@link BrowserlessUIContext} API, verifying that the context surface offers
 * the same Signals-testing capabilities as {@link BrowserlessTest}.
 */
@Timeout(10)
class SignalsContextTest {

    private BrowserlessApplicationContext app;
    private BrowserlessUIContext window;

    @BeforeEach
    void setUp() {
        app = BrowserlessApplicationContext.create(SignalsView.class);
        window = app.newUser().newWindow();
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void attachedComponent_triggerSignal_effectEvaluatedSynchronously() {
        var view = window.navigate(SignalsView.class);
        var counterTester = window.test(view.counter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());

        window.test(view.incrementButton).click();
        Assertions.assertEquals("Counter: 1", counterTester.getText());
    }

    @Test
    void detachedComponent_triggerSignal_effectEvaluatedOnAttach() {
        var view = window.navigate(SignalsView.class);
        var counterTester = window.test(view.counter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());
        view.counter.removeFromParent();
        Assertions.assertFalse(counterTester.isUsable());

        window.test(view.incrementButton).click();
        Assertions.assertEquals("Counter: 0", view.counter.getText());

        view.add(view.counter);
        Assertions.assertEquals("Counter: 1", view.counter.getText());
    }

    @Test
    void attachedComponent_triggerSignalFromNonUIThread_effectEvaluatedAsynchronously() {
        var view = window.navigate(SignalsView.class);
        var counterTester = window.test(view.asyncCounter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());
        CompletableFuture.runAsync(() -> {
            view.asyncNumberSignal.incrementBy(10.0);
        });
        window.runPendingSignalsTasks();
        Assertions.assertEquals("Counter: 10", counterTester.getText());
    }

    @Test
    void attachedComponent_triggerSignalFromNonUIThreadThroughComponentEffect_effectEvaluatedAsynchronously() {
        var view = window.navigate(SignalsView.class);
        var counterTester = window.test(view.asyncCounter);
        Assertions.assertEquals("Counter: 0", counterTester.getText());
        window.test(view.quickBackgroundTaskButton).click();
        window.runPendingSignalsTasks(300, TimeUnit.MILLISECONDS);
        Assertions.assertEquals("Counter: 10", counterTester.getText());
    }

    @Test
    void effectDispatcher_routesToTestQueue_notServiceThreadPool()
            throws InterruptedException {
        // On the test thread, both VaadinServiceEnvironment (from MockVaadin)
        // and TestSignalEnvironment are active. The default effect dispatcher
        // must resolve to TestSignalEnvironment's queue (via registerFirst)
        // so that runPendingSignalsTasks() can drive effect execution
        // deterministically. Without registerFirst,
        // VaadinServiceEnvironment's thread pool would be used instead,
        // making effects run asynchronously outside the test's control.
        window.navigate(SignalsView.class);

        var latch = new CountDownLatch(1);
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(latch::countDown);

        Assertions.assertFalse(latch.await(50, TimeUnit.MILLISECONDS),
                "Task should be queued, not executed immediately on a "
                        + "thread pool");
        window.runPendingSignalsTasks();
        Assertions.assertTrue(latch.await(0, TimeUnit.MILLISECONDS),
                "Task should have executed after draining the test queue");
    }

    @Test
    void attachedComponent_slowEffect_effectEvaluatedAsynchronously() {
        var view = window.navigate(SignalsView.class);
        var counterTester = window.test(view.asyncWithDelayCounter);
        Assertions.assertEquals("Counter: 0 (delayed)",
                counterTester.getText());
        window.test(view.slowBackgroundTaskButton).click();
        Assertions.assertTrue(
                window.runPendingSignalsTasks(300, TimeUnit.MILLISECONDS),
                "Expected pending signals tasks to be run");
        Assertions.assertEquals("Counter: 10 (delayed)",
                counterTester.getText());
    }

    @Test
    void applicationContextClose_unregistersSignalEnvironment()
            throws InterruptedException {
        // Pre-condition: the test environment is registered.
        Assertions.assertNotNull(app.getSignalsTestEnvironment(),
                "Application context must have a signals test environment "
                        + "registered while open");

        // Closing the app must unregister the test environment, restoring
        // the default dispatcher to whatever was active before.
        app.close();

        // The application context must clear its reference to the test
        // environment on close.
        Assertions.assertNull(app.getSignalsTestEnvironment(),
                "Application context must clear its signals test "
                        + "environment reference after close");

        // Directly probe the global registry: with the test environment
        // unregistered, the default effect dispatcher must no longer queue
        // tasks on the (now orphaned) test queue. It should fall through to
        // the immediate executor (or any remaining environment), so the
        // task runs without anyone calling runPendingSignalsTasks().
        var latch = new CountDownLatch(1);
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(latch::countDown);
        Assertions.assertTrue(latch.await(50, TimeUnit.MILLISECONDS),
                "After close(), tasks submitted to the default effect "
                        + "dispatcher must not be intercepted by the "
                        + "closed test environment's queue");

        // Re-opening a fresh context must re-install a working test
        // environment — proving the previous unregister fully released the
        // global registry.
        app = BrowserlessApplicationContext.create(SignalsView.class);
        window = app.newUser().newWindow();
        var view = window.navigate(SignalsView.class);
        CompletableFuture
                .runAsync(() -> view.asyncNumberSignal.incrementBy(7.0));
        window.runPendingSignalsTasks();
        Assertions.assertEquals("Counter: 7",
                window.test(view.asyncCounter).getText());
    }

}
