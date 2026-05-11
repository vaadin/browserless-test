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

import java.util.List;
import java.util.Map;

import com.vaadin.browserless.internal.MockInternalSeverError;
import com.vaadin.browserless.internal.MockPage;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * UI-level context for multi-user browserless testing.
 * <p>
 * Represents a single browser window (one {@link UI} instance). All DSL methods
 * ({@link #navigate}, {@link #$}, {@link #$view}, {@link #test}) automatically
 * call {@link #activate()} before executing, which transparently switches the
 * thread-local Vaadin state and security context to this window's user.
 * <p>
 * This means you can freely interleave calls on different windows without
 * explicit context switching:
 *
 * <pre>
 * window1.navigate(ViewA.class);
 * window2.navigate(ViewB.class); // auto-switches to window2's user
 * window1.$(Button.class).first(); // auto-switches back to window1's user
 * </pre>
 *
 * @see BrowserlessUserContext#newWindow()
 * @see BrowserlessApplicationContext
 */
public class BrowserlessUIContext implements TesterWrappers, AutoCloseable {

    private static final ThreadLocal<BrowserlessUIContext> activeContext = new ThreadLocal<>();

    private final BrowserlessUserContext user;
    private UI ui;
    private boolean closed;

    BrowserlessUIContext(BrowserlessUserContext user) {
        this.user = user;

        BrowserlessUIContext previous = activeContext.get();

        // Save outgoing user's security context on user switch — symmetric
        // with activate().
        if (previous != null && previous.user != this.user) {
            previous.user.saveSecurityContext();
        }

        // Install this user's Vaadin thread-locals (service/session/request/
        // response/security) so MockVaadin.createUI observes this user's
        // identity. UI is intentionally not touched here — createUI sets
        // UI.setCurrent itself after instantiating the UI, so leaving it
        // alone avoids clobbering a prior UI if createUI throws before
        // reaching its own UI.setCurrent. On same-user re-entry the security
        // restore is a no-op (snapshot matches live thread state).
        user.applySessionThreadLocals();

        try {
            MockVaadin.createUI(user.getApp().getUIFactory(),
                    user.getSession());
            this.ui = UI.getCurrent();
        } catch (RuntimeException ex) {
            // Roll back thread-local state: do not leak this half-built
            // context as the active context.
            if (previous != null && !previous.closed) {
                reactivateSurviving(previous);
            } else {
                user.clearThreadLocals();
            }
            throw ex;
        }

        // Publish as active only after the UI was fully constructed.
        activeContext.set(this);
    }

    /**
     * Activates this UI context on the current thread.
     * <p>
     * Sets all Vaadin thread-locals ({@link VaadinService},
     * {@link VaadinSession}, {@link UI}, {@link VaadinRequest},
     * {@link VaadinResponse}) to this window's values. If a
     * {@link SecurityContextHandler} is configured and the previous active
     * context belonged to a different user, the outgoing user's security
     * context is automatically saved and this user's context is restored.
     * <p>
     * This method is called automatically by all DSL methods. You only need to
     * call it explicitly if you are accessing Vaadin APIs directly (e.g.
     * {@code UI.getCurrent()}).
     */
    public void activate() {
        if (closed) {
            throw new IllegalStateException(
                    "BrowserlessUIContext is already closed");
        }
        BrowserlessUIContext previous = activeContext.get();

        // Save outgoing user's security context on user switch
        if (previous != null && previous != this
                && previous.user != this.user) {
            previous.user.saveSecurityContext();
        }

        // Install this user's Vaadin thread-locals and UI, restoring the
        // user's security snapshot. On same-user re-entry the security
        // restore is a no-op (snapshot matches live thread state).
        user.applyUIThreadLocals(ui);

        activeContext.set(this);
    }

    /**
     * Navigates this window to the given view class.
     *
     * @param navigationTarget
     *            the view class to navigate to
     * @param <T>
     *            the view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(Class<T> navigationTarget) {
        activate();
        ui.navigate(navigationTarget);
        return validateNavigationTarget(navigationTarget);
    }

    /**
     * Navigates this window to the given view class with a URL parameter.
     *
     * @param navigationTarget
     *            the view class to navigate to
     * @param parameter
     *            the URL parameter
     * @param <T>
     *            the view type
     * @param <C>
     *            the parameter type
     * @return the instantiated view
     */
    public <C, T extends Component & HasUrlParameter<C>> T navigate(
            Class<T> navigationTarget, C parameter) {
        activate();
        ui.navigate(navigationTarget, parameter);
        return validateNavigationTarget(navigationTarget);
    }

    /**
     * Navigates this window to the given view class with route parameters.
     *
     * @param navigationTarget
     *            the view class to navigate to
     * @param parameters
     *            the route parameters
     * @param <T>
     *            the view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(Class<T> navigationTarget,
            Map<String, String> parameters) {
        activate();
        ui.navigate(navigationTarget, new RouteParameters(parameters));
        return validateNavigationTarget(navigationTarget);
    }

    /**
     * Navigates this window to the given location and validates the resulting
     * view.
     *
     * @param location
     *            the navigation location string
     * @param expectedTarget
     *            the expected view class
     * @param <T>
     *            the view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(String location,
            Class<T> expectedTarget) {
        activate();
        ui.navigate(location);
        return validateNavigationTarget(expectedTarget);
    }

    /**
     * Gets a query object for finding components of the given type in this
     * window's UI.
     *
     * @param componentType
     *            the type of component to search for
     * @param <T>
     *            the component type
     * @return a query object
     */
    public <T extends Component> ComponentQuery<T> $(Class<T> componentType) {
        activate();
        return new ComponentQuery<>(componentType);
    }

    /**
     * Gets a query object for finding components of the given type nested
     * inside the specified component.
     *
     * @param componentType
     *            the type of component to search for
     * @param fromThis
     *            the component to search within
     * @param <T>
     *            the component type
     * @return a query object
     */
    public <T extends Component> ComponentQuery<T> $(Class<T> componentType,
            Component fromThis) {
        activate();
        return new ComponentQuery<>(componentType).from(fromThis);
    }

    /**
     * Gets a query object for finding components of the given type inside the
     * current view.
     *
     * @param componentType
     *            the type of component to search for
     * @param <T>
     *            the component type
     * @return a query object
     */
    public <T extends Component> ComponentQuery<T> $view(
            Class<T> componentType) {
        activate();
        Component viewComponent = getCurrentView().getElement().getComponent()
                .orElseThrow(() -> new AssertionError(
                        "Cannot get Component instance for current view"));
        return new ComponentQuery<>(componentType).from(viewComponent);
    }

    /**
     * Alias for {@link #$(Class)} — Java-idiomatic name.
     */
    public <T extends Component> ComponentQuery<T> get(Class<T> componentType) {
        return $(componentType);
    }

    /**
     * Alias for {@link #$(Class, Component)} — Java-idiomatic name.
     */
    public <T extends Component> ComponentQuery<T> get(Class<T> componentType,
            Component fromThis) {
        return $(componentType, fromThis);
    }

    /**
     * Alias for {@link #$view(Class)} — Java-idiomatic name.
     */
    public <T extends Component> ComponentQuery<T> getView(
            Class<T> componentType) {
        return $view(componentType);
    }

    /**
     * Wraps a component with the best matching {@link ComponentTester}. This
     * generic fallback is used for component types not covered by the specific
     * {@link TesterWrappers} defaults.
     *
     * @param component
     *            the component to wrap
     * @param <T>
     *            the tester type
     * @param <Y>
     *            the component type
     * @return the component wrapped in a tester
     */
    public <T extends ComponentTester<Y>, Y extends Component> T test(
            Y component) {
        activate();
        return BaseBrowserlessTest.internalWrap(component);
    }

    /**
     * Gets the current view displayed in this window.
     *
     * @return the current view
     */
    public HasElement getCurrentView() {
        activate();
        return ui.getInternals().getActiveRouterTargetsChain().get(0);
    }

    /**
     * Simulates a server round-trip, flushing pending component changes.
     */
    public void roundTrip() {
        activate();
        BaseBrowserlessTest.roundTrip();
    }

    /**
     * Returns the URL from the last external navigation triggered by
     * {@link com.vaadin.flow.component.page.Page#setLocation(String)} or
     * {@link com.vaadin.flow.component.page.Page#open(String)}.
     * <p>
     * This captures URLs where the window name is {@code _self},
     * {@code _parent}, {@code _top}, empty, or {@code null} (all of which
     * navigate the current window). To query URLs opened in other windows, use
     * {@link #getExternalNavigationURL(String)} or {@link #getOpenedWindows()}.
     *
     * @return the external navigation URL, or {@code null} if no external
     *         navigation has occurred
     */
    public String getExternalNavigationURL() {
        activate();
        if (ui.getPage() instanceof MockPage mockPage) {
            return mockPage.getLastExternalNavigationURL();
        }
        return null;
    }

    /**
     * Returns the last URL opened with the given window name.
     * <p>
     * For {@code _blank}, returns the URL from the most recent call. For named
     * windows, returns the last URL that targeted that name.
     *
     * @param windowName
     *            the window name to look up
     * @return the last URL for the given window name, or {@code null} if none
     */
    public String getExternalNavigationURL(String windowName) {
        activate();
        if (ui.getPage() instanceof MockPage mockPage) {
            return mockPage.getExternalNavigationURL(windowName);
        }
        return null;
    }

    /**
     * Returns a map of all windows opened via
     * {@link com.vaadin.flow.component.page.Page#open(String)} or
     * {@link com.vaadin.flow.component.page.Page#open(String, String)},
     * excluding navigations that target the current window ({@code _self},
     * {@code _parent}, {@code _top}, empty, or {@code null}).
     * <p>
     * The map keys are window names, and values are lists of URLs opened under
     * that name. For {@code _blank}, the list contains all URLs (each call
     * opens a new window). For named windows, the list typically contains a
     * single entry (the last URL).
     *
     * @return an unmodifiable map of window names to URL lists
     */
    public Map<String, List<String>> getOpenedWindows() {
        activate();
        if (ui.getPage() instanceof MockPage mockPage) {
            return mockPage.getOpenedWindows();
        }
        return Map.of();
    }

    /**
     * Returns the UI managed by this context.
     *
     * @return the UI instance
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Returns the user context that owns this window.
     *
     * @return the parent user context
     */
    public BrowserlessUserContext getUser() {
        return user;
    }

    /**
     * Closes this UI context and detaches the UI.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        BrowserlessUIContext stillActive = activeContext.get();
        boolean wasActive = stillActive == this;
        if (wasActive) {
            activeContext.remove();
        } else if (stillActive != null && stillActive.user != this.user) {
            // Cross-user non-active close: capture the active user's live
            // security state before the detach run displaces it, so the
            // re-activation below restores their up-to-date snapshot.
            stillActive.user.saveSecurityContext();
        }
        if (ui != null) {
            // Set thread-locals so detach listeners see this user's identity
            // (service/session/UI/request/response/security), not whatever
            // the thread happens to carry from another user's window.
            user.applyUIThreadLocals(ui);
            MockVaadin.closeCurrentUI(true);
            ui = null;
        }
        // Re-establish thread-local coherence with activeContext. After a
        // non-active close with a surviving active sibling, re-activate it.
        // Otherwise (active close, or no surviving active context) clear the
        // closing user's thread-locals so user code reading e.g.
        // SecurityContextHolder / UI.getCurrent() between close() and the
        // next activate() does not observe stale state from the closed
        // window.
        if (!wasActive && stillActive != null && !stillActive.closed) {
            reactivateSurviving(stillActive);
        } else {
            user.clearThreadLocals();
        }
    }

    private <T extends Component> T validateNavigationTarget(
            Class<T> navigationTarget) {
        HasElement currentView = getCurrentView();
        if (!navigationTarget.isAssignableFrom(currentView.getClass())) {
            if (currentView instanceof MockInternalSeverError) {
                System.err.println(
                        currentView.getElement().getProperty("stackTrace"));
            }
            throw new IllegalArgumentException(
                    "Navigation resulted in unexpected class "
                            + currentView.getClass().getName() + " instead of "
                            + navigationTarget.getName());
        }
        return navigationTarget.cast(currentView);
    }

    /**
     * Returns the currently active UI context for the calling thread, or
     * {@code null} if none is active.
     *
     * @return the active context, or {@code null}
     */
    static BrowserlessUIContext getActive() {
        return activeContext.get();
    }

    /**
     * Clears the active-context ThreadLocal without otherwise touching the
     * thread-local Vaadin state. Reserved for defensive test cleanup; lifecycle
     * code paths should prefer
     * {@link #reactivateSurviving(BrowserlessUIContext)} which both clears and
     * re-installs the surviving window's state.
     */
    static void clearActiveContext() {
        activeContext.remove();
    }

    /**
     * Re-establishes thread-local coherence with {@link #activeContext} by
     * re-activating the given surviving window. Clears {@code activeContext}
     * first so {@link #activate()} takes the full-restore branch
     * ({@code previous == null}) and re-installs the active user's security
     * snapshot. Used by lifecycle paths (constructor rollback, non-active
     * window close, cross-user session destroy) that temporarily displaced the
     * active window's thread-local state.
     */
    static void reactivateSurviving(BrowserlessUIContext stillActive) {
        activeContext.remove();
        stillActive.activate();
    }

    boolean isClosed() {
        return closed;
    }
}
