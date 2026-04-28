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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.example.multiuser.SimpleView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Tests that closing user/UI contexts properly tears down thread-local state,
 * matches MockVaadin's session-destroy semantics, and preserves the user's
 * security context for detach listeners.
 */
class BrowserlessClosePathCleanupTest {

    private Routes routes;

    @BeforeEach
    void setUp() {
        routes = new Routes()
                .autoDiscoverViews(SimpleView.class.getPackageName());
    }

    @Test
    void userClose_clearsVaadinRequestAndResponseThreadLocals() {
        try (var app = BrowserlessApplicationContext.create(routes)) {
            var user = app.newUser();
            user.newWindow();

            // Sanity: thread-locals are set after a window has been activated
            Assertions.assertNotNull(VaadinRequest.getCurrent(),
                    "Sanity check: VaadinRequest should be set");
            Assertions.assertNotNull(VaadinResponse.getCurrent(),
                    "Sanity check: VaadinResponse should be set");

            user.close();

            Assertions.assertNull(VaadinRequest.getCurrent(),
                    "user.close() must clear VaadinRequest thread-local");
            Assertions.assertNull(VaadinResponse.getCurrent(),
                    "user.close() must clear VaadinResponse thread-local");
        }
    }

    @Test
    void userClose_drainsSessionAccessTasksScheduledByDestroyListeners() {
        try (var app = BrowserlessApplicationContext.create(routes)) {
            var user = app.newUser();
            user.newWindow();

            var ran = new AtomicBoolean();
            app.getService().addSessionDestroyListener(
                    event -> event.getSession().access(() -> ran.set(true)));

            user.close();

            Assertions.assertTrue(ran.get(),
                    "session.access tasks scheduled by session-destroy"
                            + " listeners must be drained before close()"
                            + " returns");
        }
    }

    @Test
    void uiClose_clearsActiveContextWhenClosingTheActiveWindow() {
        try (var app = BrowserlessApplicationContext.create(routes)) {
            var window = app.newUser().newWindow();
            Assertions.assertSame(window, BrowserlessUIContext.getActive(),
                    "Sanity check: newWindow activates the new window");

            window.close();

            Assertions.assertNull(BrowserlessUIContext.getActive(),
                    "Closing the active window must clear the activeContext"
                            + " ThreadLocal");
        }
    }

    @Test
    void uiClose_leavesActiveContextAloneWhenClosingANonActiveWindow() {
        try (var app = BrowserlessApplicationContext.create(routes)) {
            var user = app.newUser();
            var window1 = user.newWindow();
            var window2 = user.newWindow();
            Assertions.assertSame(window2, BrowserlessUIContext.getActive(),
                    "Sanity check: window2 is the active context");

            // Close the *non*-active window. activeContext must not be touched.
            window1.close();
            Assertions.assertSame(window2, BrowserlessUIContext.getActive(),
                    "Closing a non-active window must not affect activeContext");

            // Closing the active window clears the ThreadLocal.
            window2.close();
            Assertions.assertNull(BrowserlessUIContext.getActive(),
                    "Closing the last active window must clear activeContext");
        }
    }

    @Test
    void appClose_doesNotLeakActiveContextAcrossInvocations() {
        try (var app = BrowserlessApplicationContext.create(routes)) {
            app.newUser().newWindow();
            // Sanity: window is active during the try block
            Assertions.assertNotNull(BrowserlessUIContext.getActive());
        }

        // After app.close() (try-with-resources), the activeContext must not
        // retain a reference to a closed window — otherwise reuse of this
        // thread by a subsequent test method observes stale state.
        Assertions.assertNull(BrowserlessUIContext.getActive(),
                "app.close() must leave activeContext clear");
    }

    @Test
    void uiClose_restoresUserSecurityContextForDetachListeners() {
        var handler = new CapturingHandler();
        try (var app = BrowserlessApplicationContext.<String> builder(routes)
                .withSecurityContextHandler(handler).build()) {

            var alice = app.newUser("alice");
            var aliceWindow = alice.newWindow();

            var bob = app.newUser("bob");
            var bobWindow = bob.newWindow(); // activates bob; thread now
                                             // carries "bob"

            // Sanity: bob is the live snapshot
            Assertions.assertEquals("bob", handler.live.get(),
                    "Sanity check: bob is the live snapshot");

            var observed = new AtomicReference<String>();
            aliceWindow.getUI()
                    .addDetachListener(e -> observed.set(handler.live.get()));

            // Close aliceWindow without re-activating it. The detach listener
            // must observe alice's snapshot, not bob's.
            aliceWindow.close();

            Assertions.assertEquals("alice", observed.get(),
                    "Detach listener should see this user's security"
                            + " snapshot, not whatever the thread happened"
                            + " to carry");
            Assertions.assertSame(bobWindow, BrowserlessUIContext.getActive(),
                    "Closing a non-active window owned by a different user"
                            + " must not displace the active context");
        }
    }

    @Test
    void uiClose_detachListenerSeesClosingUserRequest() {
        try (var app = BrowserlessApplicationContext.create(routes)) {
            var alice = app.newUser();
            var aliceWindow = alice.newWindow();
            var aliceRequest = VaadinRequest.getCurrent();

            var bob = app.newUser();
            bob.newWindow(); // activates bob; thread now carries bob's request

            var observed = new AtomicReference<VaadinRequest>();
            aliceWindow.getUI().addDetachListener(
                    e -> observed.set(VaadinRequest.getCurrent()));

            aliceWindow.close();

            Assertions.assertSame(aliceRequest, observed.get(),
                    "Detach listener should see the closing user's"
                            + " VaadinRequest, not whatever the thread"
                            + " happened to carry from the active user");
        }
    }

    @Test
    void uiClose_nonActiveCrossUserCloseLeavesThreadCoherentWithActiveContext() {
        var handler = new CapturingHandler();
        try (var app = BrowserlessApplicationContext.<String> builder(routes)
                .withSecurityContextHandler(handler).build()) {

            var alice = app.newUser("alice");
            var aliceWindow = alice.newWindow();

            var bob = app.newUser("bob");
            var bobWindow = bob.newWindow();

            // Sanity: bob is the live state on the thread
            Assertions.assertSame(bobWindow.getUI(), UI.getCurrent());
            Assertions.assertSame(bob.getSession(), VaadinSession.getCurrent());
            Assertions.assertEquals("bob", handler.live.get());

            aliceWindow.close();

            // After closing alice's non-active window, the thread must still
            // reflect bob (the active context) — the close() path must repair
            // any coherence it temporarily breaks.
            Assertions.assertSame(bobWindow, BrowserlessUIContext.getActive(),
                    "Sanity: bob remains the active context");
            Assertions.assertSame(bobWindow.getUI(), UI.getCurrent(),
                    "UI.getCurrent() must remain coherent with activeContext"
                            + " after a non-active cross-user close");
            Assertions.assertSame(bob.getSession(), VaadinSession.getCurrent(),
                    "VaadinSession.getCurrent() must remain coherent with"
                            + " activeContext after a non-active cross-user"
                            + " close");
            Assertions.assertEquals("bob", handler.live.get(),
                    "Security context must remain bob's after a non-active"
                            + " cross-user close, not the closed user's"
                            + " snapshot");
        }
    }

    @Test
    void userClose_clearsSecurityContext() {
        var handler = new CapturingHandler();
        try (var app = BrowserlessApplicationContext.<String> builder(routes)
                .withSecurityContextHandler(handler).build()) {
            var user = app.newUser("alice");
            user.newWindow();

            // Sanity: alice's snapshot is live on the thread
            Assertions.assertEquals("alice", handler.live.get());

            user.close();

            Assertions.assertNull(handler.live.get(),
                    "user.close() must clear the security context so the"
                            + " closing user's snapshot does not leak onto"
                            + " the thread");
        }
    }

    /**
     * Minimal {@link SecurityContextHandler} that stores a string snapshot in a
     * thread-local, so tests can observe save/restore behaviour without pulling
     * in Spring or Quarkus dependencies.
     */
    private static class CapturingHandler
            implements SecurityContextHandler<String> {
        private final ThreadLocal<String> live = new ThreadLocal<>();

        @Override
        public void setupAuthentication(String credentials) {
            live.set(credentials != null ? credentials : "<anon>");
        }

        @Override
        public Object saveContext() {
            return live.get();
        }

        @Override
        public void restoreContext(Object snapshot) {
            if (snapshot == null) {
                live.remove();
            } else if (snapshot instanceof String s) {
                live.set(s);
            } else {
                throw new IllegalArgumentException(
                        "Expected a String snapshot, got "
                                + snapshot.getClass().getName());
            }
        }

        @Override
        public void clearContext() {
            live.remove();
        }
    }
}
