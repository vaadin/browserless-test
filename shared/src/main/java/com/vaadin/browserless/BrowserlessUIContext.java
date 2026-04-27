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
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * UI-level context for multi-user browserless testing.
 * <p>
 * Represents a single browser window/tab (one {@link UI} instance). All DSL
 * methods ({@link #navigate}, {@link #$}, {@link #$view}, {@link #test})
 * automatically call {@link #activate()} before executing, which transparently
 * switches the thread-local Vaadin state and security context to this window's
 * user.
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

        // Activate this context (sets thread-locals + restores security)
        activate();

        // Create the UI
        MockVaadin.createUI(user.getApp().getUIFactory(), user.getSession());
        this.ui = UI.getCurrent();
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

        // Set Vaadin thread-locals
        VaadinService.setCurrent(user.getApp().getService());
        VaadinSession.setCurrent(user.getSession());
        UI.setCurrent(ui);
        CurrentInstance.set(VaadinRequest.class, user.getRequest());
        CurrentInstance.set(VaadinResponse.class, user.getResponse());

        // Restore security context on user switch or first activation
        boolean switchingUser = previous == null || previous.user != this.user;
        if (switchingUser) {
            user.restoreSecurityContext();
        }

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
        if (activeContext.get() == this) {
            activeContext.remove();
        }
        if (ui != null) {
            // Set thread-locals to properly close the UI
            VaadinService.setCurrent(user.getApp().getService());
            VaadinSession.setCurrent(user.getSession());
            UI.setCurrent(ui);
            // Restore this user's security context so detach listeners see
            // the right identity, not whatever the thread happens to carry
            user.restoreSecurityContext();
            MockVaadin.closeCurrentUI(true);
            ui = null;
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
}
