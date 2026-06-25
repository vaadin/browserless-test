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
 * For the default per-method lifecycle, the Vaadin environment is set up before
 * each test by an instance {@code @BeforeEach} method (which calls
 * {@link #initVaadinEnvironment()}) and torn down by an instance
 * {@code @AfterEach} method (which calls {@link #cleanVaadinEnvironment()}).
 * Driving setup from instance lifecycle methods (rather than from an extension
 * callback) ensures it runs <em>after</em> all JUnit 5 extension
 * {@code beforeEach} callbacks, so the test can be combined with extensions
 * that {@code MockVaadin} depends on — for example a CDI container started by
 * weld-junit5's {@code @EnableAutoWeld}, whose {@code BeanManagerProvider} must
 * be in place before {@code MockVaadin.setup()} runs.
 *
 * <p>
 * On its own, {@code BrowserlessTest} only supports the default per-method
 * lifecycle; it does <em>not</em> support
 * {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)}. Tests that need a
 * single Vaadin environment shared across all methods in the class can opt in
 * by registering {@link BrowserlessTestExtension} explicitly, ordering it as
 * needed relative to any other required extensions:
 *
 * <pre>
 * {@code
 * &#64;TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * &#64;ExtendWith(BrowserlessTestExtension.class)
 * &#64;ViewPackages(classes = MyView.class)
 * class MyStatefulTest extends BrowserlessTest {
 * }
 * }
 * </pre>
 *
 * When that extension is present and the class uses {@code PER_CLASS}, it
 * initializes the environment once in {@code @BeforeAll} and tears it down in
 * {@code @AfterAll}, and the per-method hooks above become no-ops.
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
 * <p>
 * <strong>Note:</strong> Subclasses may override
 * {@link #initVaadinEnvironment()} to perform a custom
 * {@code MockVaadin.setup()} (for example to register a CDI-aware servlet). The
 * override must NOT be annotated with {@code @BeforeEach}: it is already
 * invoked by the inherited per-method hook, and adding {@code @BeforeEach}
 * would run the setup twice.
 *
 * <p>
 * To get a graphical ASCII representation of the UI tree on failure, add
 * {@code @ExtendWith(TreeOnFailureExtension.class)} to the test class.
 *
 * @see ViewPackages
 *
 * @see BrowserlessExtension
 *
 * @see BrowserlessTestExtension
 */
public abstract class BrowserlessTest extends BaseBrowserlessTest
        implements TesterWrappers {

    /**
     * Set by {@link BrowserlessTestExtension} when the environment is managed
     * by the extension for a {@code PER_CLASS} test (initialized in
     * {@code @BeforeAll}, torn down in {@code @AfterAll}). When {@code true}
     * the per-method hooks below must not run.
     */
    boolean perClassLifecycle = false;

    /**
     * Sets up a fresh Vaadin environment before each test, unless the
     * environment is managed by {@link BrowserlessTestExtension} for a
     * {@code PER_CLASS} test (in which case it has already been set up once in
     * {@code @BeforeAll}).
     *
     * <p>
     * Implemented as an instance {@code @BeforeEach} method so that it runs
     * after all JUnit 5 extension {@code beforeEach} callbacks. It delegates to
     * {@link #initVaadinEnvironment()}, which subclasses may override.
     */
    @BeforeEach
    final void setUpVaadinEnvironment() {
        if (!perClassLifecycle) {
            initVaadinEnvironment();
        }
    }

    /**
     * Tears down the Vaadin environment after each test, unless the class uses
     * the {@code PER_CLASS} lifecycle (handled in {@code @AfterAll}).
     */
    @AfterEach
    final void tearDownVaadinEnvironment() {
        if (!perClassLifecycle) {
            cleanVaadinEnvironment();
        }
    }

    @Override
    protected final String testingEngine() {
        return "JUnit 6";
    }
}
