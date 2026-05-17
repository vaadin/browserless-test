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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.vaadin.flow.signals.SignalEnvironment;

/**
 * Verifies that {@link SpringBrowserlessTest} initializes signal support so
 * that the default effect dispatcher resolves to {@link TestSignalEnvironment}
 * (a task queue drained by {@code runPendingSignalsTasks}) rather than the
 * VaadinService thread pool. Without this, mutations on a shared signal
 * dispatch their async {@code confirm()} through a real thread pool, racing
 * with the next mutation's synchronous {@code notifyObservers} and silently
 * dropping notifications (see use-cases MUC03 nickname-update flake).
 */
@ContextConfiguration(classes = SpringSignalsTest.TestConfig.class)
@Timeout(10)
class SpringSignalsTest extends SpringBrowserlessTest {

    @Test
    void signalEnvironment_defaultDispatcher_routesToTestQueue()
            throws InterruptedException {
        var latch = new CountDownLatch(1);
        SignalEnvironment.getDefaultEffectDispatcher()
                .execute(latch::countDown);

        Assertions.assertFalse(latch.await(50, TimeUnit.MILLISECONDS),
                "Task should be queued in the test environment, not "
                        + "executed immediately on a thread pool. "
                        + "TestSignalEnvironment is not registered — "
                        + "SpringBrowserlessTest.initVaadinEnvironment must "
                        + "call initSignalsSupport().");
        Assertions.assertTrue(runPendingSignalsTasks(),
                "Expected runPendingSignalsTasks() to process the queued "
                        + "task, but no TestSignalEnvironment is registered.");
        Assertions.assertTrue(latch.await(0, TimeUnit.MILLISECONDS),
                "Task should have executed after draining the test queue");
    }

    @Configuration
    static class TestConfig {
    }
}
