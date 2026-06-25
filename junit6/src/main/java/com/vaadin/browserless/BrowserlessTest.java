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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base JUnit 6 class for browserless tests.
 *
 * The class automatically scans the classpath for routes and error views.
 * Subclasses should typically restrict classpath scanning to specific packages
 * for faster bootstrap by using the {@link ViewPackages} annotation. If the
 * annotation is not present, a full classpath scan is performed.
 *
 * <pre>
 * {@code
 * &#64;ViewPackages(classes = { CartView.class, CheckoutView.class })
 * class CartViewTest extends BrowserlessTest {
 * }
 *
 * &#64;ViewPackages(packages = { "com.example.shop.cart", "com.example.security" })
 * class CartViewTest extends BrowserlessTest {
 * }
 *
 * &#64;ViewPackages(classes = { CartView.class, CheckoutView.class }, packages = {
 *         "com.example.security" })
 * class CartViewTest extends BrowserlessTest {
 * }
 * }
 * </pre>
 *
 * <h2>Lifecycle</h2>
 *
 * With the default per-method test lifecycle, the Vaadin environment is set up
 * before each test by the instance {@code @BeforeEach} method
 * {@link #initVaadinEnvironment()} and torn down after each test by the
 * instance {@code @AfterEach} method {@link #cleanVaadinEnvironment()}. Setup
 * runs before, and teardown after, any {@code @BeforeEach}/{@code @AfterEach}
 * methods declared in subclasses.
 *
 * <p>
 * When the test class is annotated with
 * {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)}, the environment is
 * instead shared across all tests in the class: it is initialized once in
 * {@code @BeforeAll} and torn down in {@code @AfterAll} by
 * {@link BrowserlessTestExtension}, and the per-method instance hooks stand
 * down.
 *
 * <p>
 * To provide custom Flow service implementations via the
 * {@link com.vaadin.flow.di.Lookup} SPI, override {@link #lookupServices()}:
 *
 * <pre>
 * {@code
 * &#64;Override
 * protected Set<Class<?>> lookupServices() {
 *     return Set.of(CustomInstantiatorFactory.class);
 * }
 * }
 * </pre>
 *
 * <h2>Overriding setup / composing with other extensions</h2>
 *
 * A subclass may override {@link #initVaadinEnvironment()} and
 * {@link #cleanVaadinEnvironment()}, for example to set up
 * {@code MockVaadin.setup} with a custom servlet. When doing so it is
 * <strong>mandatory</strong> to re-add the {@code @BeforeEach} (respectively
 * {@code @AfterEach}) annotation on the override:
 *
 * <pre>
 * {@code
 * &#64;BeforeEach
 * &#64;Override
 * protected void initVaadinEnvironment() {
 *     scanTesters();
 *     MockVaadin.setup(MockedUI::new, myCustomServlet, lookupServices());
 * }
 * }
 * </pre>
 *
 * <p>
 * Re-adding the annotation registers the hook at the concrete test class level.
 * This matters when the test composes with another JUnit extension that the
 * setup depends on — for example a CDI container started by weld-junit5's
 * {@code @EnableAutoWeld}. JUnit boots a subclass-registered extension after
 * the inherited superclass setup but before the subclass's own
 * {@code @BeforeEach} methods, so declaring the override at the subclass level
 * guarantees the container (and its {@code BeanManagerProvider}) is ready by
 * the time {@code MockVaadin.setup()} runs.
 *
 * <p>
 * To get a graphical ASCII representation of the UI tree on failure, add
 * {@code @ExtendWith(TreeOnFailureExtension.class)} to the test class.
 *
 * @see ViewPackages
 *
 * @see BrowserlessExtension
 */
@ExtendWith(BrowserlessTestExtension.class)
public abstract class BrowserlessTest extends BaseBrowserlessTest
        implements TesterWrappers {

    /**
     * Set by {@link BrowserlessTestExtension} when it owns the environment
     * lifecycle (i.e. {@code @TestInstance(PER_CLASS)}, handled in
     * {@code @BeforeAll}/{@code @AfterAll}). In that case the per-method
     * instance hooks below must stand down so the shared environment is not
     * re-created or torn down between tests.
     */
    private boolean extensionManagedLifecycle;

    void setExtensionManagedLifecycle(boolean extensionManagedLifecycle) {
        this.extensionManagedLifecycle = extensionManagedLifecycle;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Invoked as an instance {@code @BeforeEach} method (per-method lifecycle).
     * Overriding subclasses must re-add the {@code @BeforeEach} annotation; see
     * the class-level documentation.
     */
    @BeforeEach
    @Override
    protected void initVaadinEnvironment() {
        if (!extensionManagedLifecycle) {
            super.initVaadinEnvironment();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Invoked as an instance {@code @AfterEach} method (per-method lifecycle).
     * Overriding subclasses must re-add the {@code @AfterEach} annotation.
     */
    @AfterEach
    @Override
    protected void cleanVaadinEnvironment() {
        if (!extensionManagedLifecycle) {
            super.cleanVaadinEnvironment();
        }
    }

    @Override
    protected final String testingEngine() {
        return "JUnit 6";
    }
}
