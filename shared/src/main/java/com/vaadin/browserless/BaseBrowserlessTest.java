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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.VaadinSession;

/**
 * Base class for browserless tests.
 *
 * Provides methods to set up and clean a mocked Vaadin environment.
 *
 * The class allows scan classpath for routes and error views. Subclasses should
 * typically restrict classpath scanning to a specific packages for faster
 * bootstrap, by using {@link ViewPackages} annotation. If the annotation is not
 * present a full classpath scan is performed
 *
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see ViewPackages
 */
public abstract class BaseBrowserlessTest {

    private TestSignalEnvironment signalsTestEnvironment;

    protected synchronized Routes discoverRoutes() {
        return discoverRoutes(scanPackages());
    }

    /**
     * Discover and return Routes for mocked Vaadin core system.
     *
     * @see #initVaadinEnvironment()
     * @return Routes
     */
    // Protected for access by adapter subclass in legacy module
    protected static Routes discoverRoutes(Set<String> packageNames) {
        return RouteDiscovery.discover(packageNames);
    }

    /**
     * Create mocked Vaadin core obects, such as session, servlet populated with
     * Routes, UI etc. for testing and find testers for the components.
     */
    protected void initVaadinEnvironment() {
        scanTesters();
        MockVaadin.setup(discoverRoutes(), MockedUI::new, lookupServices());
        initSignalsSupport();
    }

    protected void initSignalsSupport() {
        signalsTestEnvironment = TestSignalEnvironment.register();
    }

    /**
     * Scan testers and populate testers map with them. The test method can find
     * appropriate test based on testers map.
     *
     * @see #test(Component)
     */
    protected void scanTesters() {
        if (getClass().isAnnotationPresent(ComponentTesterPackages.class)) {
            TesterRegistry.registerPackages(getClass()
                    .getAnnotation(ComponentTesterPackages.class).value());
        }
    }

    protected Set<String> scanPackages() {
        Set<String> packagesToScan = new HashSet<>();

        if (getClass().isAnnotationPresent(ViewPackages.class)) {
            ViewPackages packages = getClass()
                    .getAnnotation(ViewPackages.class);
            Stream.of(packages.classes()).map(Class::getPackageName)
                    .collect(Collectors.toCollection(() -> packagesToScan));
            packagesToScan.addAll(Set.of(packages.packages()));
            // Assume current class package scan if annotation exist but does
            // not provide any restriction
            if (packagesToScan.isEmpty()) {
                packagesToScan.add(getClass().getPackageName());
            }
        }
        packagesToScan.removeIf(Objects::isNull);
        return packagesToScan;
    }

    /**
     * Tears down mocked Vaadin.
     */
    protected void cleanVaadinEnvironment() {
        if (signalsTestEnvironment != null) {
            signalsTestEnvironment.unregister();
            signalsTestEnvironment = null;
        }
        MockVaadin.tearDown();
    }

    /**
     * Gets the services implementations to be used to initialized Vaadin
     * {@link com.vaadin.flow.di.Lookup}.
     *
     * Default implementation returns an empty Set. Override this method to
     * provide custom Vaadin services, such as
     * {@link com.vaadin.flow.di.InstantiatorFactory},
     * {@link com.vaadin.flow.di.ResourceProvider}, etc.
     *
     * @return set of services implementation classes, never {@literal null}.
     */
    protected Set<Class<?>> lookupServices() {
        return Collections.emptySet();
    }

    /**
     * Navigate to the given view class if it is registered.
     *
     * @param navigationTarget
     *            view class to navigate to
     * @param <T>
     *            view type
     * @return instantiated view
     */
    public <T extends Component> T navigate(Class<T> navigationTarget) {
        return BrowserlessDSL.navigate(verifyAndGetUI(), navigationTarget);
    }

    /**
     * Navigate to view with url parameter.
     *
     * @param navigationTarget
     *            view class to navigate to
     * @param parameter
     *            parameter to send to view
     * @param <T>
     *            view type
     * @param <C>
     *            parameter type
     * @return instantiated view
     */
    public <C, T extends Component & HasUrlParameter<C>> T navigate(
            Class<T> navigationTarget, C parameter) {
        return BrowserlessDSL.navigate(verifyAndGetUI(), navigationTarget,
                parameter);
    }

    /**
     * Navigate to view corresponding to the given navigation target with the
     * specified parameters.
     *
     * @param navigationTarget
     *            view class to navigate to
     * @param parameters
     *            parameters to pass to view.
     * @param <T>
     *            view type
     * @return instantiated view
     */
    public <T extends Component> T navigate(Class<T> navigationTarget,
            Map<String, String> parameters) {
        return BrowserlessDSL.navigate(verifyAndGetUI(), navigationTarget,
                parameters);
    }

    /**
     * Navigate to given location string. Check that location navigated to is
     * the expected view or throw exception.
     *
     * @param location
     *            location string for navigating
     * @param expectedTarget
     *            class that is expected for navigation
     * @param <T>
     *            view type
     * @return instantiated view
     */
    public <T extends Component> T navigate(String location,
            Class<T> expectedTarget) {
        return BrowserlessDSL.navigate(verifyAndGetUI(), location,
                expectedTarget);
    }

    /**
     * Simulates a keyboard shortcut performed on the browser.
     *
     * @param key
     *            Primary key of the shortcut. This must not be a
     *            {@link KeyModifier}.
     * @param modifiers
     *            Key modifiers. Can be empty.
     */
    public void fireShortcut(Key key, KeyModifier... modifiers) {
        BrowserlessDSL.fireShortcut(verifyAndGetUI(), key, modifiers);
    }

    /**
     * Get the current view instance that is shown on the ui.
     *
     * @return current view
     */
    public HasElement getCurrentView() {
        return BrowserlessDSL.getCurrentView(verifyAndGetUI());
    }

    // Protected for access by adapter subclass in legacy module
    protected static <T extends ComponentTester<Y>, Y extends Component> T internalWrap(
            Y component) {
        return TesterRegistry.wrap(component);
    }

    protected static <T extends ComponentTester<Y>, Y extends Component> T internalWrap(
            Class<T> wrap, Y component) {
        return TesterRegistry.instantiate(wrap, component);
    }

    /**
     * Wrap component with ComponentTester best matching component type.
     *
     * @param component
     *            component to get test wrapper for
     * @param <T>
     *            tester type
     * @param <Y>
     *            component type
     * @return component in wrapper with test helpers
     */
    public <T extends ComponentTester<Y>, Y extends Component> T test(
            Y component) {
        verifyAndGetUI();
        return internalWrap(component);
    }

    /**
     * Wrap component in given ComponentTester.
     *
     * @param tester
     *            test wrapper to use
     * @param component
     *            component to wrap
     * @param <T>
     *            tester type
     * @param <Y>
     *            component type
     * @return initialized test wrapper for component
     */
    public <T extends ComponentTester<Y>, Y extends Component> T test(
            Class<T> tester, Y component) {
        verifyAndGetUI();
        return TesterRegistry.instantiate(tester, component);
    }

    /**
     * Gets a query object for finding a component inside the UI
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     * @since 1.1
     */
    public <T extends Component> ComponentQuery<T> find(
            Class<T> componentType) {
        return BrowserlessDSL.find(verifyAndGetUI(), componentType);
    }

    /**
     * Gets a query object for finding a component nested inside the given
     * component.
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param fromThis
     *            component used as starting element for search.
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     * @since 1.1
     */
    public <T extends Component> ComponentQuery<T> find(Class<T> componentType,
            Component fromThis) {
        return BrowserlessDSL.find(verifyAndGetUI(), componentType, fromThis);
    }

    /**
     * Gets a query object for finding a component inside the current view
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     * @since 1.1
     */
    public <T extends Component> ComponentQuery<T> findInView(
            Class<T> componentType) {
        return BrowserlessDSL.findView(verifyAndGetUI(), componentType);
    }

    /**
     * Gets a query object for finding a component inside the UI.
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     * @deprecated since 1.1, for removal in 2.0; use {@link #find(Class)}
     *             instead.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public <T extends Component> ComponentQuery<T> $(Class<T> componentType) {
        return find(componentType);
    }

    /**
     * Gets a query object for finding a component nested inside the given
     * component.
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param fromThis
     *            component used as starting element for search.
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     * @deprecated since 1.1, for removal in 2.0; use
     *             {@link #find(Class, Component)} instead.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public <T extends Component> ComponentQuery<T> $(Class<T> componentType,
            Component fromThis) {
        return find(componentType, fromThis);
    }

    /**
     * Gets a query object for finding a component inside the current view.
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     * @deprecated since 1.1, for removal in 2.0; use {@link #findInView(Class)}
     *             instead.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public <T extends Component> ComponentQuery<T> $view(
            Class<T> componentType) {
        return findInView(componentType);
    }

    /**
     * Simulates a server round-trip, flushing pending component changes.
     */
    protected static void roundTrip() {
        BrowserlessDSL.roundTrip(UI.getCurrent());
    }

    /**
     * Processes all pending Signals tasks with a default max wait time of 100
     * milliseconds. This is a convenience method for tests that need to wait
     * for asynchronous Signal effects to complete.
     *
     * <p>
     * When Signals are triggered from background threads or non-UI contexts,
     * their effects are enqueued to simulate asynchronous processing. This
     * method allows tests to flush and execute all such pending tasks
     * synchronously, ensuring deterministic behavior in unit tests.
     *
     * <p>
     * If any {@link VaadinSession} lock is held by the current thread, it is
     * temporarily released during the wait to allow background threads to
     * acquire the lock and enqueue tasks.
     *
     * @return {@code true} if any pending Signals tasks were processed.
     * @see #runPendingSignalsTasks(long, TimeUnit)
     * @see TestSignalEnvironment#runPendingTasks(long, TimeUnit)
     */
    protected final boolean runPendingSignalsTasks() {
        return BrowserlessDSL.runPendingSignalsTasks(signalsTestEnvironment);
    }

    /**
     * Processes all pending Signals tasks, waiting up to the specified timeout
     * for tasks to arrive. This method is essential for testing asynchronous
     * Signal effects triggered from background threads or non-UI contexts.
     *
     * <p>
     * When Signals are triggered from background threads or non-UI contexts,
     * their effects are enqueued to simulate asynchronous processing. This
     * method allows tests to flush and execute all such pending tasks
     * synchronously, ensuring deterministic behavior in unit tests.
     *
     * <p>
     * The timeout applies only to waiting for the first task to arrive. Once
     * the first task is found, all remaining tasks in the queue are processed
     * immediately without additional waiting. If any {@link VaadinSession} lock
     * is held by the current thread, it is temporarily released during the wait
     * to allow background threads to acquire the lock and enqueue tasks.
     *
     * @param maxWaitTime
     *            the maximum time to wait for the first task to arrive in the
     *            given time unit. If &lt;= 0, returns immediately if no tasks
     *            are available.
     * @param unit
     *            the time unit of the timeout value
     * @return {@code true} if any pending Signals tasks were processed.
     * @see TestSignalEnvironment#runPendingTasks(long, TimeUnit)
     */
    protected final boolean runPendingSignalsTasks(long maxWaitTime,
            TimeUnit unit) {
        return BrowserlessDSL.runPendingSignalsTasks(signalsTestEnvironment,
                maxWaitTime, unit);
    }

    /*
     * Checks that the mock UI is available, otherwise fails fast with an
     * exception giving advices on possible causes of the problem.
     *
     * Principal cause for having a null UI is that the test extends the wrong
     * base class for the current configuration.
     *
     */
    private UI verifyAndGetUI() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            String message = "Test Vaadin environment is not initialized correctly. "
                    + "This may happen when the test is extending the wrong base class for the testing engine in use. "
                    + "Current test class is expected to run with "
                    + testingEngine() + ".";
            throw new BrowserlessTestSetupException(message);
        }
        return ui;
    }

    /**
     * Gets the name of the Test Engine that is able to run the base class
     * implementation.
     *
     * The Test Engine name is reported in the exception thrown when the Vaadin
     * environment is not set up correctly.
     *
     * @return name of the Test Engine.
     */
    protected abstract String testingEngine();

}
