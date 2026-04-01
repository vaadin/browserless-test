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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Represents a single user's session in the browserless test environment.
 * Holds a VaadinSession and its associated HTTP session.
 *
 * <p>
 * Create UI contexts (browser windows) via {@link #newWindow()} to simulate multiple
 * windows for this user.
 *
 * <pre>
 * {@code
 * VaadinTestUserContext user = app.newUser();
 * VaadinTestUiContext window1 = user.newWindow();
 * VaadinTestUiContext window2 = user.newWindow();
 * }
 * </pre>
 */
public class VaadinTestUserContext implements AutoCloseable {

    private final VaadinTestApplicationContext app;
    private final VaadinSession session;
    private final VaadinRequest request;
    private final VaadinResponse response;
    private final List<VaadinTestUiContext> windows = new ArrayList<>();
    private SecurityContext securityContext;

    VaadinTestUserContext(VaadinTestApplicationContext app) {
        this.app = app;

        // Save previous context
        VaadinSession prevSession = VaadinSession.getCurrent();
        UI prevUI = UI.getCurrent();
        VaadinRequest prevRequest = VaadinRequest.getCurrent();
        VaadinResponse prevResponse = VaadinResponse.getCurrent();
        SecurityContext prevSecurityContext = null;
        try {
            prevSecurityContext = SecurityContextHolder.getContext();
        } catch (NoClassDefFoundError ignored) {
        }

        try {
            // Use MockVaadin to create a full session + UI, which sets
            // everything as current. We capture the state and then close
            // the auto-created UI (windows are created explicitly via newWindow()).
            VaadinService.setCurrent(app.getService());
            // Restore this user's security context if already set (e.g.
            // via setAuthentication before opening windows)
            restoreSecurityContext();
            MockVaadin.createSession(app.getServletContext(),
                    app.getUiFactory());

            this.session = VaadinSession.getCurrent();
            this.request = VaadinRequest.getCurrent();
            this.response = VaadinResponse.getCurrent();

            // Close the auto-created UI; windows will be created explicitly
            MockVaadin.clearCurrentUI();
        } finally {
            // Restore previous context
            VaadinSession.setCurrent(prevSession);
            UI.setCurrent(prevUI);
            CurrentInstance.set(VaadinRequest.class, prevRequest);
            CurrentInstance.set(VaadinResponse.class, prevResponse);
            try {
                if (prevSecurityContext != null) {
                    SecurityContextHolder.setContext(prevSecurityContext);
                }
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }

    /**
     * Creates a new browser window (UI) for this user.
     *
     * @return a new UI context
     */
    public VaadinTestUiContext newWindow() {
        VaadinTestUiContext window = new VaadinTestUiContext(this);
        windows.add(window);
        return window;
    }

    /**
     * Sets the Spring Security authentication for this user. Call this before
     * creating windows to ensure the security context is in place when the
     * UI initializes and route access control is evaluated.
     *
     * @param authentication
     *            the authentication token
     */
    public void setAuthentication(Authentication authentication) {
        try {
            if (securityContext == null) {
                securityContext = SecurityContextHolder
                        .createEmptyContext();
            }
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException(
                    "Spring Security is not on the classpath", e);
        }
    }

    /**
     * Gets the VaadinSession for this user.
     *
     * @return the session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Closes this user context and all its windows.
     */
    @Override
    public void close() {
        for (VaadinTestUiContext window : windows) {
            window.close();
        }
        windows.clear();
    }

    /**
     * Saves the current Spring Security context for this user so it can be
     * restored on the next {@code activate()} call. Called automatically
     * when switching away from this user's window.
     */
    void saveSecurityContext() {
        try {
            securityContext = SecurityContextHolder.getContext();
        } catch (NoClassDefFoundError e) {
            // Spring Security not on classpath
        }
    }

    /**
     * Restores this user's Spring Security context. Called automatically
     * by {@code VaadinTestUiContext.activate()}.
     */
    void restoreSecurityContext() {
        try {
            if (securityContext != null) {
                SecurityContextHolder.setContext(securityContext);
            } else {
                SecurityContextHolder.clearContext();
            }
        } catch (NoClassDefFoundError e) {
            // Spring Security not on classpath
        }
    }

    VaadinTestApplicationContext getApp() {
        return app;
    }

    VaadinRequest getRequest() {
        return request;
    }

    VaadinResponse getResponse() {
        return response;
    }
}
