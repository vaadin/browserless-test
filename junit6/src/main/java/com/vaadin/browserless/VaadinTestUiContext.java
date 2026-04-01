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

import java.util.Map;

import com.vaadin.browserless.internal.MockInternalSeverError;
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
 * Represents a single browser window (UI) in the browserless test environment.
 * Each UI context has its own UI instance and automatically activates its
 * Vaadin thread-local state when any method is called.
 *
 * <p>
 * This is the main interaction point for simulating user actions:
 *
 * <pre>
 * {@code
 * VaadinTestUiContext window = user.newWindow();
 * SimpleView view = window.navigate(SimpleView.class);
 * window.test(view.getButton()).click();
 * }
 * </pre>
 */
public class VaadinTestUiContext implements AutoCloseable, TesterWrappers {

    private static final ThreadLocal<VaadinTestUiContext> activeContext = new ThreadLocal<>();

    private final VaadinTestUserContext user;
    private UI ui;

    VaadinTestUiContext(VaadinTestUserContext user) {
        this.user = user;
        // Activate this context and create the UI
        activate();
        MockVaadin.createUI(user.getApp().getUiFactory(),
                user.getSession());
        this.ui = UI.getCurrent();
    }

    /**
     * Activates this UI context, setting the appropriate VaadinService,
     * VaadinSession, UI, request, response, and Spring Security context
     * as current on the calling thread. Automatically saves the previous
     * user's security context before switching.
     */
    public void activate() {
        VaadinTestUiContext previous = activeContext.get();
        boolean switchingUser = previous != null && previous != this
                && previous.user != this.user;
        if (switchingUser) {
            previous.user.saveSecurityContext();
        }
        activeContext.set(this);

        VaadinService.setCurrent(user.getApp().getService());
        VaadinSession.setCurrent(user.getSession());
        UI.setCurrent(ui);
        CurrentInstance.set(VaadinRequest.class, user.getRequest());
        CurrentInstance.set(VaadinResponse.class, user.getResponse());
        // Only restore SecurityContext when switching to a different user;
        // re-activating the same user must not overwrite changes made since
        // the last save (e.g. a login click that set authentication).
        if (switchingUser || previous == null) {
            user.restoreSecurityContext();
        }
    }

    /**
     * Navigates to the given view class.
     *
     * @param target
     *            view class to navigate to
     * @param <T>
     *            view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(Class<T> target) {
        activate();
        ui.navigate(target);
        return validateNavigationTarget(target);
    }

    /**
     * Navigates to the given view class with a URL parameter.
     *
     * @param target
     *            view class to navigate to
     * @param parameter
     *            URL parameter
     * @param <T>
     *            view type
     * @param <C>
     *            parameter type
     * @return the instantiated view
     */
    public <C, T extends Component & HasUrlParameter<C>> T navigate(
            Class<T> target, C parameter) {
        activate();
        ui.navigate(target, parameter);
        return validateNavigationTarget(target);
    }

    /**
     * Navigates to the given view class with route parameters.
     *
     * @param target
     *            view class to navigate to
     * @param parameters
     *            route parameters
     * @param <T>
     *            view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(Class<T> target,
            Map<String, String> parameters) {
        activate();
        ui.navigate(target, new RouteParameters(parameters));
        return validateNavigationTarget(target);
    }

    /**
     * Navigates to the given location string and verifies the expected target.
     *
     * @param location
     *            navigation location string
     * @param expectedTarget
     *            expected view class
     * @param <T>
     *            view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(String location,
            Class<T> expectedTarget) {
        activate();
        ui.navigate(location);
        return validateNavigationTarget(expectedTarget);
    }

    /**
     * Gets a query object for finding components of the given type in the UI.
     *
     * @param type
     *            component type to search for
     * @param <T>
     *            component type
     * @return a component query
     */
    public <T extends Component> ComponentQuery<T> $(Class<T> type) {
        activate();
        return BaseBrowserlessTest.internalQuery(type);
    }

    /**
     * Gets a query object for finding components of the given type in the UI.
     *
     * @param type
     *            component type to search for
     * @param <T>
     *            component type
     * @return a component query
     */
    public <T extends Component> ComponentQuery<T> get(Class<T> type) {
        return $(type);
    }

    /**
     * Gets a query object for finding components nested inside a given
     * component.
     *
     * @param type
     *            component type to search for
     * @param fromThis
     *            starting component for search
     * @param <T>
     *            component type
     * @return a component query scoped to the given component
     */
    public <T extends Component> ComponentQuery<T> $(Class<T> type,
            Component fromThis) {
        activate();
        return new ComponentQuery<>(type).from(fromThis);
    }

    /**
     * Gets a query object for finding components nested inside a given
     * component.
     *
     * @param type
     *            component type to search for
     * @param fromThis
     *            starting component for search
     * @param <T>
     *            component type
     * @return a component query scoped to the given component
     */
    public <T extends Component> ComponentQuery<T> get(Class<T> type,
            Component fromThis) {
        return $(type, fromThis);
    }

    /**
     * Gets a query object for finding components inside the current view.
     *
     * @param type
     *            component type to search for
     * @param <T>
     *            component type
     * @return a component query scoped to the current view
     */
    public <T extends Component> ComponentQuery<T> $view(Class<T> type) {
        activate();
        Component viewComponent = getCurrentView().getElement().getComponent()
                .orElseThrow(() -> new AssertionError(
                        "Cannot get Component instance for current view"));
        return new ComponentQuery<>(type).from(viewComponent);
    }

    /**
     * Gets a query object for finding components inside the current view.
     *
     * @param type
     *            component type to search for
     * @param <T>
     *            component type
     * @return a component query scoped to the current view
     */
    public <T extends Component> ComponentQuery<T> getView(Class<T> type) {
        return $view(type);
    }

    /**
     * Gets the current view instance shown in the UI.
     *
     * @return the current view
     */
    public HasElement getCurrentView() {
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
     * Gets the UI instance for this window.
     *
     * @return the UI
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Returns the URL of the last external navigation triggered by
     * {@code Page.setLocation()} or {@code Page.open()}, or {@code null}
     * if no external navigation has been triggered.
     *
     * <p>
     * This is useful for testing flows where the application redirects
     * to an external service (e.g. login, payment).
     *
     * @return the external navigation URL, or {@code null}
     */
    public String getExternalNavigationURL() {
        activate();
        String lastUrl = null;
        var invocations = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        for (var invocation : invocations) {
            String expr = invocation.getInvocation().getExpression();
            if (expr.contains("window.open(")) {
                var params = invocation.getInvocation().getParameters();
                if (!params.isEmpty()
                        && params.get(0) instanceof String url) {
                    lastUrl = url;
                }
            }
        }
        return lastUrl;
    }

    /**
     * Closes this UI context.
     */
    @Override
    public void close() {
        if (ui != null) {
            activate();
            MockVaadin.clearCurrentUI();
            ui = null;
        }
    }

    private <T extends Component> T validateNavigationTarget(Class<T> target) {
        HasElement currentView = getCurrentView();
        if (!target.isAssignableFrom(currentView.getClass())) {
            if (currentView instanceof MockInternalSeverError) {
                System.err.println(
                        currentView.getElement().getProperty("stackTrace"));
            }
            throw new IllegalArgumentException(
                    "Navigation resulted in unexpected class "
                            + currentView.getClass().getName() + " instead of "
                            + target.getName());
        }
        return target.cast(currentView);
    }
}
