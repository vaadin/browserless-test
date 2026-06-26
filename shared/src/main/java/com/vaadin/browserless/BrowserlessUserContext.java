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
 * Instances of this class are <strong>thread-affine</strong>: they must be
 * created, used, and closed on the same thread. The active context is held in a
 * {@link ThreadLocal} and is not visible to other threads. This class is not
 * safe for concurrent access from multiple threads.
 * <p>
 * Security context (if a {@link SecurityContextHandler} is configured on the
 * parent {@link BrowserlessApplicationContext}) is initialised when this user
 * is created and refreshed on user-switch (when activating a different user's
 * window), capturing the outgoing user's live thread-local state at that
 * moment. The new user's authentication is installed on the thread before
 * {@code SessionInit} listeners fire, so listeners observe this user's identity
 * — matching the Vaadin+Spring flow where the security filter chain runs before
 * the servlet. The user's initial snapshot is captured after init fires, so any
 * security mutation a listener performs persists into the snapshot.
 * <p>
 * The security snapshot is <strong>per-user, not per-window</strong>: all of a
 * user's windows share one snapshot. Security-context mutations made while one
 * window is active persist on the thread and remain visible to other windows of
 * the same user; the snapshot is re-captured only on user-switch, so same-user
 * window switches don't touch it.
 *
 * @see BrowserlessApplicationContext#newUser()
 * @see BrowserlessUIContext
 * @since 1.1
 */
public class BrowserlessUserContext implements AutoCloseable {

    private final BrowserlessApplicationContext app;
    private final VaadinSession session;
    private final VaadinRequest request;
    private final VaadinResponse response;
    private final List<BrowserlessUIContext> windows = new ArrayList<>();
    private Object securitySnapshot;
    private boolean closed;

    BrowserlessUserContext(BrowserlessApplicationContext app,
            Object credentials) {
        this.app = app;

        // Save current thread-local state so we can restore it after setup
        VaadinService previousService = VaadinService.getCurrent();
        VaadinSession previousSession = VaadinSession.getCurrent();
        VaadinRequest previousRequest = VaadinRequest.getCurrent();
        VaadinResponse previousResponse = VaadinResponse.getCurrent();
        UI previousUI = UI.getCurrent();
        // Raw type so we can pass the Object credentials to setupAuthentication
        // without knowing the concrete C; getSecurityContextHandler() returns
        // null on the unsecured base class and a typed handler on the secured
        // subclass.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        SecurityContextHandler handler = app.getSecurityContextHandler();
        Object previousSecuritySnapshot = handler != null
                ? handler.saveContext()
                : null;
        // Save the thread's current request-context binding so it can be
        // restored after this temporary setup; applySessionThreadLocals() binds
        // this user's request below.
        RequestContextHandler requestContextHandler = app
                .getRequestContextHandler();
        Object previousRequestContext = requestContextHandler != null
                ? requestContextHandler.saveContext()
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

            // Install thread-locals temporarily for session init listeners.
            // applySessionThreadLocals() ends with restoreSecurityContext()
            // forwarding our still-null snapshot to
            // handler.restoreContext(null),
            // which the handler contract specifies as clearing the context.
            // The explicit clearContext()+setupAuthentication below then
            // installs this user's identity before any listener observes it.
            applySessionThreadLocals();

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
            // Restore the thread's previous request-context binding.
            if (requestContextHandler != null) {
                requestContextHandler.restoreContext(previousRequestContext);
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
        applySessionThreadLocals();

        // Destroy session: fire destroy listeners and drain the queue,
        // mirroring MockVaadin.closeCurrentSession (which gates the
        // session-recreation hook via a thread-local flag).
        MockVaadin.fireSessionDestroyAndDrain(session);

        // Drop this user's thread-local state — including the security
        // snapshot — so it does not leak into subsequent activations or
        // tests sharing the thread.
        clearThreadLocals();

        // If a different user's window is still active, re-establish its
        // thread-locals (UI/Session/Request/Response/security) so any
        // subsequent operation on it observes a coherent state.
        if (activeIsAnotherUser && !active.isClosed()) {
            BrowserlessUIContext.reactivateSurviving(active);
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

    BrowserlessApplicationContext getApp() {
        return app;
    }

    VaadinRequest getRequest() {
        return request;
    }

    VaadinResponse getResponse() {
        return response;
    }

    /**
     * Applies this user's session-level Vaadin thread-locals
     * (service/session/request/response/security) on the current thread, and
     * restores this user's security snapshot. Does not touch
     * {@link UI#setCurrent}. Used by the {@link BrowserlessUIContext}
     * constructor (where {@code MockVaadin.createUI} sets {@code UI.setCurrent}
     * itself, so we intentionally leave any prior UI in place until
     * {@code createUI} succeeds) and by this class's own constructor +
     * {@code close()}.
     */
    void applySessionThreadLocals() {
        VaadinService.setCurrent(app.getService());
        VaadinSession.setCurrent(session);
        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinResponse.class, response);
        restoreSecurityContext();
        bindRequestContext();
    }

    /**
     * Applies this user's full Vaadin thread-local state on the current thread:
     * the session-level state from {@link #applySessionThreadLocals()} plus
     * {@code UI.setCurrent(ui)}. Used by
     * {@link BrowserlessUIContext#activate()} and
     * {@link BrowserlessUIContext#close()}.
     */
    void applyUIThreadLocals(UI ui) {
        applySessionThreadLocals();
        UI.setCurrent(ui);
    }

    /**
     * Clears all Vaadin thread-locals and the security context on the current
     * thread. Vaadin thread-locals are cleared before security so a future
     * security handler that reads {@code VaadinSession.getCurrent()} during
     * clear-out sees {@code null}.
     */
    void clearThreadLocals() {
        VaadinService.setCurrent(null);
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
        CurrentInstance.set(VaadinRequest.class, null);
        CurrentInstance.set(VaadinResponse.class, null);
        SecurityContextHandler<?> handler = app.getSecurityContextHandler();
        if (handler != null) {
            handler.clearContext();
        }
        RequestContextHandler requestContextHandler = app
                .getRequestContextHandler();
        if (requestContextHandler != null) {
            requestContextHandler.clearContext();
        }
    }

    /**
     * Binds this user's request as the active framework request context on the
     * current thread, so framework-managed request/session scopes (e.g.
     * Spring's {@code @SessionScope}) resolve against this user's own session.
     * No-op when no {@link RequestContextHandler} is configured.
     */
    private void bindRequestContext() {
        RequestContextHandler requestContextHandler = app
                .getRequestContextHandler();
        if (requestContextHandler != null) {
            requestContextHandler.bind(request);
        }
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
