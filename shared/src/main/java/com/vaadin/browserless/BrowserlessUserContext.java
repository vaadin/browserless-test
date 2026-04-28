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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.SessionObjects;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * User-level context for multi-user browserless testing.
 * <p>
 * Represents a single logical user with their own {@link VaadinSession}, HTTP
 * request, and response. Each user context can have multiple windows (UI
 * instances) via {@link #newWindow()}.
 * <p>
 * Security context (if a {@link SecurityContextHandler} is configured on the
 * parent {@link BrowserlessApplicationContext}) is initialised when this user
 * is created and refreshed on user-switch (when activating a different user's
 * window), capturing the outgoing user's live thread-local state at that
 * moment. The new user's authentication is installed on the thread before
 * {@code SessionInit} listeners fire, so listeners observe this user's identity
 * — matching the Vaadin+Spring flow where the security filter chain runs before
 * the servlet. The user's initial snapshot is captured after init fires, so any
 * security mutation a listener performs persists into the snapshot. Mutations
 * to the security context while one of this user's windows is active persist on
 * the thread and are captured into the snapshot at the next user-switch —
 * same-user window switches don't touch the snapshot.
 *
 * @see BrowserlessApplicationContext#newUser()
 * @see BrowserlessUIContext
 */
public class BrowserlessUserContext implements AutoCloseable {

    private final BrowserlessApplicationContext<?> app;
    private final VaadinSession session;
    private final VaadinRequest request;
    private final VaadinResponse response;
    private final List<BrowserlessUIContext> windows = new ArrayList<>();
    private Object securitySnapshot;
    private boolean closed;

    BrowserlessUserContext(BrowserlessApplicationContext<?> app,
            Object credentials) {
        this.app = app;

        // Save current thread-local state so we can restore it after setup
        VaadinService previousService = VaadinService.getCurrent();
        VaadinSession previousSession = VaadinSession.getCurrent();
        VaadinRequest previousRequest = VaadinRequest.getCurrent();
        VaadinResponse previousResponse = VaadinResponse.getCurrent();
        UI previousUI = UI.getCurrent();
        // Raw type so we can pass the Object credentials to setupAuthentication
        // without forcing every caller (and the surrounding wildcard
        // BrowserlessApplicationContext<?>) to know the concrete C.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        SecurityContextHandler handler = app.getSecurityContextHandler();
        Object previousSecuritySnapshot = handler != null
                ? handler.saveContext()
                : null;

        try {
            // Set service as current (needed for session creation)
            VaadinService.setCurrent(app.getService());

            // Create session objects without setting thread-locals
            SessionObjects objs = MockVaadin
                    .createSessionObjects(app.getService());
            this.session = objs.getSession();
            this.request = objs.getRequest();
            this.response = objs.getResponse();

            // Install thread-locals temporarily for session init listeners
            VaadinSession.setCurrent(session);
            CurrentInstance.set(VaadinRequest.class, request);
            CurrentInstance.set(VaadinResponse.class, response);

            // Set up authentication BEFORE firing session-init listeners so
            // they observe this user's identity, mirroring the Vaadin+Spring
            // flow where the security filter chain runs before the servlet.
            if (handler != null) {
                // Start with a clean security context for this user
                handler.clearContext();
                // Always delegate to the handler so it can interpret null
                // credentials (e.g. Spring sets an
                // AnonymousAuthenticationToken)
                handler.setupAuthentication(credentials);
            }

            // Fire session init listeners
            MockVaadin.fireSessionInit(app.getService(), session, request);

            // Capture as this user's initial security snapshot, after init
            // so any security mutation a listener performs persists into
            // this user's snapshot rather than being silently discarded.
            if (handler != null) {
                securitySnapshot = handler.saveContext();
            }
        } finally {
            // Restore previous thread-local state
            VaadinService.setCurrent(previousService);
            VaadinSession.setCurrent(previousSession);
            CurrentInstance.set(VaadinRequest.class, previousRequest);
            CurrentInstance.set(VaadinResponse.class, previousResponse);
            UI.setCurrent(previousUI);
            // Restore previous security context — handler contract specifies
            // null → clearContext, so the snapshot is forwarded as-is.
            if (handler != null) {
                handler.restoreContext(previousSecuritySnapshot);
            }
        }
    }

    /**
     * Creates a new window (UI instance) for this user.
     * <p>
     * The window is automatically activated (thread-locals set) and a new UI is
     * created. If a route target for {@code ""} is registered, the UI will
     * navigate to it.
     *
     * @return the new UI context
     */
    public BrowserlessUIContext newWindow() {
        checkNotClosed();
        var window = new BrowserlessUIContext(this);
        windows.add(window);
        return window;
    }

    /**
     * Closes this user context and all its windows.
     * <p>
     * Destroys the Vaadin session and clears associated state. If a window
     * belonging to a different user is the active context on the calling thread
     * when this method runs, that window is re-activated at the end so
     * subsequent operations on it see a coherent thread-local state.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (var window : windows) {
            window.close();
        }
        windows.clear();

        // After window.close() iterations the thread state may belong to a
        // window of another user (see BrowserlessUIContext.close()'s
        // re-activation step for cross-user non-active closes). Capture the
        // active context now so we can restore it after the destroy phase.
        BrowserlessUIContext active = BrowserlessUIContext.getActive();
        boolean activeIsAnotherUser = active != null
                && active.getUser() != this;

        // Set thread-locals so destroy listeners (and the security snapshot
        // observed by them) see this user's identity, not whatever the
        // thread happens to carry from another active user. VaadinRequest
        // and VaadinResponse are set for parity even though Vaadin's
        // session-destroy listeners run under session.access semantics that
        // null them out — keeping the prep symmetric protects against
        // future Vaadin changes.
        VaadinService.setCurrent(app.getService());
        VaadinSession.setCurrent(session);
        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinResponse.class, response);
        restoreSecurityContext();

        // Destroy session: fire destroy listeners and drain the queue,
        // mirroring MockVaadin.closeCurrentSession (which gates the
        // session-recreation hook via a thread-local flag).
        MockVaadin.fireSessionDestroyAndDrain(session);

        VaadinService.setCurrent(null);
        VaadinSession.setCurrent(null);
        CurrentInstance.set(VaadinRequest.class, null);
        CurrentInstance.set(VaadinResponse.class, null);
        // Drop this user's security snapshot from the thread so it does not
        // leak into subsequent activations or tests sharing the thread.
        SecurityContextHandler<?> handler = app.getSecurityContextHandler();
        if (handler != null) {
            handler.clearContext();
        }

        // If a different user's window is still active, re-establish its
        // thread-locals (UI/Session/Request/Response/security) so any
        // subsequent operation on it observes a coherent state. Clear
        // activeContext first so activate() takes the full-restore branch.
        if (activeIsAnotherUser && !active.isClosed()) {
            BrowserlessUIContext.clearActiveContext();
            active.activate();
        }
    }

    /**
     * Returns the Vaadin session associated with this user.
     *
     * @return the Vaadin session
     */
    public VaadinSession getSession() {
        return session;
    }

    BrowserlessApplicationContext<?> getApp() {
        return app;
    }

    VaadinRequest getRequest() {
        return request;
    }

    VaadinResponse getResponse() {
        return response;
    }

    /**
     * Saves the current thread's security context into this user's snapshot.
     * Called automatically by {@link BrowserlessUIContext#activate()} when
     * switching away from this user.
     */
    void saveSecurityContext() {
        SecurityContextHandler<?> handler = app.getSecurityContextHandler();
        if (handler != null) {
            securitySnapshot = handler.saveContext();
        }
    }

    /**
     * Restores this user's security context onto the current thread. Called
     * automatically by {@link BrowserlessUIContext#activate()} when switching
     * to this user.
     */
    void restoreSecurityContext() {
        SecurityContextHandler<?> handler = app.getSecurityContextHandler();
        if (handler != null) {
            // Contract: handler.restoreContext(null) clears the context.
            handler.restoreContext(securitySnapshot);
        }
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException(
                    "BrowserlessUserContext is already closed");
        }
    }
}
