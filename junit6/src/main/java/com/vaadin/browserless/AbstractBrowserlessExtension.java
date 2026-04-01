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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.browserless.internal.MockInternalSeverError;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.ShortcutsKt;
import com.vaadin.browserless.mocks.MockSpringServlet;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.mocks.SpringSecurityRequestCustomizer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteParameters;

/**
 * Abstract base for browserless JUnit 5 extensions. Holds all shared state and
 * logic; concrete subclasses implement only the lifecycle callbacks they need.
 */
abstract class AbstractBrowserlessExtension implements TesterWrappers {

    // Programmatic config (builder-style)
    private final Set<String> viewPackages = new HashSet<>();
    private final Set<Class<?>> services = new HashSet<>();
    private final Set<String> componentTesterPackages = new HashSet<>();

    // Runtime state
    private TestSignalEnvironment signalsTestEnvironment;
    private Runnable cleanupAction;
    private boolean springContextSet;

    // --- Protected builder helpers ---

    protected void addViewPackages(Class<?>... classes) {
        Stream.of(classes).map(Class::getPackageName)
                .forEach(viewPackages::add);
    }

    protected void addViewPackages(String... packages) {
        viewPackages.addAll(Arrays.asList(packages));
    }

    protected void addServices(Class<?>... serviceClasses) {
        services.addAll(Arrays.asList(serviceClasses));
    }

    protected void addComponentTesterPackages(String... packages) {
        componentTesterPackages.addAll(Arrays.asList(packages));
    }

    // --- Lifecycle callbacks ---

    protected void doInit(Object testInstance, ExtensionContext ctx) {
        if (testInstance instanceof BaseBrowserlessTest base) {
            base.initVaadinEnvironment();
            cleanupAction = base::cleanVaadinEnvironment;
        } else {
            standaloneInit(ctx);
            cleanupAction = this::standaloneCleanup;
        }
    }

    protected void doCleanup() {
        if (cleanupAction != null) {
            cleanupAction.run();
            cleanupAction = null;
        }
    }

    private void standaloneCleanup() {
        if (signalsTestEnvironment != null) {
            signalsTestEnvironment.unregister();
            signalsTestEnvironment = null;
        }
        MockVaadin.tearDown();
        if (springContextSet) {
            BrowserlessTestSpringLookupInitializer
                    .clearCurrentApplicationContext();
            springContextSet = false;
        }
    }

    private void standaloneInit(ExtensionContext ctx) {
        Class<?> testClass = ctx.getRequiredTestClass();

        // Scan for additional component testers
        Set<String> testerPkgs = new HashSet<>(componentTesterPackages);
        ComponentTesterPackages testerAnnotation = testClass
                .getAnnotation(ComponentTesterPackages.class);
        if (testerAnnotation != null) {
            testerPkgs.addAll(Arrays.asList(testerAnnotation.value()));
        }
        for (String pkg : testerPkgs) {
            if (BaseBrowserlessTest.scanned.add(pkg)) {
                BaseBrowserlessTest.testers
                        .putAll(BaseBrowserlessTest.scanForTesters(pkg));
            }
        }

        // Resolve view packages from annotation and programmatic config
        Set<String> packages = new HashSet<>(viewPackages);
        ViewPackages vpAnnotation = testClass.getAnnotation(ViewPackages.class);
        if (vpAnnotation != null) {
            Stream.of(vpAnnotation.classes()).map(Class::getPackageName)
                    .forEach(packages::add);
            packages.addAll(Arrays.asList(vpAnnotation.packages()));
            // If annotation is present but empty, default to test class package
            if (packages.isEmpty()) {
                packages.add(testClass.getPackageName());
            }
        }
        packages.removeIf(Objects::isNull);

        Routes routes = BaseBrowserlessTest.discoverRoutes(packages);

        ApplicationContext appCtx = getSpringApplicationContext(ctx);
        if (appCtx != null) {
            BrowserlessTestSpringLookupInitializer
                    .setCurrentApplicationContext(appCtx);
            springContextSet = true;
            MockSpringServlet servlet = new MockSpringServlet(routes, appCtx,
                    MockedUI::new);
            Set<Class<?>> springServices = new HashSet<>(services);
            springServices.add(BrowserlessTestSpringLookupInitializer.class);
            springServices.add(SpringSecurityRequestCustomizer.class);
            MockVaadin.setup(MockedUI::new, servlet, springServices);
        } else {
            MockVaadin.setup(routes, MockedUI::new, services);
        }
        signalsTestEnvironment = TestSignalEnvironment.register();
    }

    private static ApplicationContext getSpringApplicationContext(
            ExtensionContext ctx) {
        try {
            return SpringExtension.getApplicationContext(ctx);
        } catch (Exception | NoClassDefFoundError e) {
            return null;
        }
    }

    // --- Testing DSL ---

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
        getUI().navigate(target);
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
        getUI().navigate(target, parameter);
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
        getUI().navigate(target, new RouteParameters(parameters));
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
        getUI().navigate(location);
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
        getUI();
        return BaseBrowserlessTest.internalQuery(type);
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
        getUI();
        return new ComponentQuery<>(type).from(fromThis);
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
        Component viewComponent = getCurrentView().getElement().getComponent()
                .orElseThrow(() -> new AssertionError(
                        "Cannot get Component instance for current view"));
        return new ComponentQuery<>(type).from(viewComponent);
    }

    /**
     * Gets the current view instance shown in the UI.
     *
     * @return the current view
     */
    public HasElement getCurrentView() {
        return getUI().getInternals().getActiveRouterTargetsChain().get(0);
    }

    /**
     * Simulates a server round-trip, flushing pending component changes.
     */
    public void roundTrip() {
        BaseBrowserlessTest.roundTrip();
    }

    /**
     * Processes all pending Signals tasks with a default max wait of 100
     * milliseconds.
     *
     * @return {@code true} if any pending Signals tasks were processed
     */
    public boolean runPendingSignalsTasks() {
        return runPendingSignalsTasks(100, TimeUnit.MILLISECONDS);
    }

    /**
     * Processes all pending Signals tasks, waiting up to the specified timeout
     * for tasks to arrive.
     *
     * @param maxWaitTime
     *            maximum time to wait for the first task
     * @param unit
     *            time unit for the timeout
     * @return {@code true} if any pending Signals tasks were processed
     */
    public boolean runPendingSignalsTasks(long maxWaitTime, TimeUnit unit) {
        if (signalsTestEnvironment != null) {
            return signalsTestEnvironment.runPendingTasks(maxWaitTime, unit);
        }
        return false;
    }

    /**
     * Simulates a keyboard shortcut performed on the browser.
     *
     * @param key
     *            primary key of the shortcut
     * @param modifiers
     *            key modifiers
     */
    public void fireShortcut(Key key, KeyModifier... modifiers) {
        UI ui = getUI();
        if (ui.hasModalComponent()) {
            ShortcutsKt._fireShortcut(
                    ui.getInternals().getActiveModalComponent(), key,
                    modifiers);
        } else {
            ShortcutsKt.fireShortcut(key, modifiers);
        }
    }

    private UI getUI() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new BrowserlessTestSetupException(
                    "Test Vaadin environment is not initialized. "
                            + "Make sure BrowserlessExtension is registered and active "
                            + "before calling test DSL methods.");
        }
        return ui;
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
