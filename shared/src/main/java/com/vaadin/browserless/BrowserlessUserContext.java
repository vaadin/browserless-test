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
 * moment. Mutations to the security context while one of this user's windows is
 * active persist on the thread and are captured into this user's snapshot at
 * the next user-switch — same-user window switches don't touch the snapshot.
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

            // Fire session init listeners
            MockVaadin.fireSessionInit(app.getService(), session, request);

            // Set up authentication for this user
            if (handler != null) {
                // Start with a clean security context for this user
                handler.clearContext();
                // Always delegate to the handler so it can interpret null
                // credentials (e.g. Spring sets an
                // AnonymousAuthenticationToken)
                handler.setupAuthentication(credentials);
                // Capture as this user's initial security snapshot
                securitySnapshot = handler.saveContext();
            }
        } finally {
            // Restore previous thread-local state
            VaadinService.setCurrent(previousService);
            VaadinSession.setCurrent(previousSession);
            CurrentInstance.set(VaadinRequest.class, previousRequest);
            CurrentInstance.set(VaadinResponse.class, previousResponse);
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
     * Destroys the Vaadin session and clears associated state.
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

        // Destroy session: fire destroy listeners and drain the queue,
        // mirroring MockVaadin.closeCurrentSession (which gates the
        // session-recreation hook via a thread-local flag).
        VaadinService.setCurrent(app.getService());
        VaadinSession.setCurrent(session);
        MockVaadin.fireSessionDestroyAndDrain(session);
        VaadinService.setCurrent(null);
        CurrentInstance.set(VaadinRequest.class, null);
        CurrentInstance.set(VaadinResponse.class, null);
        // Drop this user's security snapshot from the thread so it does not
        // leak into subsequent activations or tests sharing the thread.
        SecurityContextHandler<?> handler = app.getSecurityContextHandler();
        if (handler != null) {
            handler.clearContext();
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
