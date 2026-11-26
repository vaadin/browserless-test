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
package com.vaadin.browserless.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static com.vaadin.browserless.TestAssertions.expectThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AsyncTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    /**
     * The commands scheduled by {@code UI.access()} only run when something
     * flushes the queue, which in a browserless test is the explicit round
     * trip. Asserting that they have <em>not</em> run before it is the point of
     * most of these.
     */
    private static void assertMockedInstancesAvailable() {
        assertNotNull(VaadinSession.getCurrent());
        assertNotNull(VaadinService.getCurrent());
        assertNotNull(UI.getCurrent());
        // Request and response are never available in access() commands
        assertNull(VaadinRequest.getCurrent());
        assertNull(VaadinResponse.getCurrent());
    }

    @Nested
    class FromUiThread {

        @Test
        void access_blockIsNotCalledImmediately() {
            AtomicBoolean checkAccess = new AtomicBoolean(true);
            UI.getCurrent().access(() -> {
                if (checkAccess.get()) {
                    fail("Shouldn't be called now");
                }
            });
            checkAccess.set(false);
        }

        @Test
        void accessSynchronously_blockIsCalledImmediatelyBecauseTheTestHoldsTheUiLock() {
            AtomicBoolean called = new AtomicBoolean();
            UI.getCurrent().accessSynchronously(() -> called.set(true));
            assertTrue(called.get());
        }

        @Test
        void clientRoundtrip_processesEveryQueuedAccessCall() {
            AtomicInteger calledCount = new AtomicInteger();
            UI.getCurrent().access(new Command() {
                @Override
                public void execute() {
                    if (calledCount.get() < 4) {
                        calledCount.incrementAndGet();
                        UI.getCurrent().access(this);
                    }
                }
            });
            assertEquals(0, calledCount.get());
            MockVaadin.clientRoundtrip();
            assertEquals(4, calledCount.get());
        }

        @Test
        void clientRoundtrip_accessCommandThrows_propagatesTheFailure() {
            UI.getCurrent().access(() -> {
                throw new RuntimeException("simulated");
            });
            expectThrows(ExecutionException.class, "simulated",
                    () -> MockVaadin.clientRoundtrip());
        }

        @Test
        void access_hasProperlyMockedInstances() {
            UI.getCurrent().access(AsyncTest::assertMockedInstancesAvailable);
            MockVaadin.clientRoundtrip();
        }

        // https://github.com/mvysny/karibu-testing/issues/80
        @Test
        void push_doesNothingButCanBeCalled() {
            UI.getCurrent().push();
            UI.getCurrent().access(() -> UI.getCurrent().push());
            MockVaadin.clientRoundtrip();
            UI.getCurrent().accessSynchronously(() -> UI.getCurrent().push());
        }
    }

    @Nested
    class FromBackgroundThread {

        private ExecutorService executor;

        @BeforeEach
        void startExecutor() {
            executor = Executors.newCachedThreadPool();
        }

        @AfterEach
        void stopExecutor() throws InterruptedException {
            executor.shutdown();
            executor.awaitTermination(4, TimeUnit.SECONDS);
        }

        private void asyncAwait(Consumer<UI> block) {
            UI ui = UI.getCurrent();
            try {
                executor.submit(() -> block.accept(ui)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new AssertionError(e);
            }
        }

        @Test
        void access_blockIsNotCalledImmediatelyBecauseTheTestHoldsTheUiLock() {
            asyncAwait(ui -> {
                AtomicBoolean checkAccess = new AtomicBoolean(true);
                ui.access(() -> {
                    if (checkAccess.get()) {
                        fail("Shouldn't be called now");
                    }
                });
                checkAccess.set(false);
            });
        }

        @Test
        void clientRoundtrip_processesEveryQueuedAccessCall() {
            AtomicInteger calledCount = new AtomicInteger();
            asyncAwait(ui -> ui.access(new Command() {
                @Override
                public void execute() {
                    if (calledCount.get() < 4) {
                        calledCount.incrementAndGet();
                        UI.getCurrent().access(this);
                    }
                }
            }));
            assertEquals(0, calledCount.get());
            MockVaadin.clientRoundtrip();
            assertEquals(4, calledCount.get());
        }

        @Test
        void access_hasProperlyMockedInstances() {
            asyncAwait(
                    ui -> ui.access(AsyncTest::assertMockedInstancesAvailable));
            MockVaadin.clientRoundtrip();
        }

        // https://github.com/mvysny/karibu-testing/issues/80
        @Test
        void push_doesNothingButCanBeCalled() {
            asyncAwait(ui -> ui.access(() -> UI.getCurrent().push()));
            MockVaadin.clientRoundtrip();
        }
    }
}
