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
 * The Vaadin environment is set up before each test by the
 * {@link #initVaadinEnvironment()} {@code @BeforeEach} method and cleaned up
 * afterwards by the {@link #cleanVaadinEnvironment()} {@code @AfterEach}
 * method. Being instance lifecycle methods (rather than extension callbacks),
 * they run <em>after</em> all JUnit 5 extension {@code beforeEach} callbacks,
 * so the test can be combined with extensions that {@code MockVaadin} depends
 * on — for example a CDI container started by weld-junit5's
 * {@code @EnableAutoWeld}, whose {@code BeanManagerProvider} must be in place
 * before {@code MockVaadin.setup()} runs.
 *
 * <p>
 * Usually it is not necessary to override {@link #initVaadinEnvironment()} or
 * {@link #cleanVaadinEnvironment()}, but if it is done then the override must
 * keep the {@code @BeforeEach}/{@code @AfterEach} annotation so the hook is
 * still handled by the testing framework. A common reason to override
 * {@link #initVaadinEnvironment()} is to perform a custom
 * {@code MockVaadin.setup()} — for instance to register a CDI-aware servlet:
 *
 * <pre>
 * {@code
 * &#64;BeforeEach
 * &#64;Override
 * protected void initVaadinEnvironment() {
 *     scanTesters();
 *     MockVaadin.setup(MockedUI::new, cdiVaadinServlet, lookupServices());
 * }
 * }
 * </pre>
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
 * This class only supports the default per-method lifecycle. Tests that need a
 * single Vaadin environment shared across all methods in the class should not
 * extend it; instead register a {@link BrowserlessClassExtension} and, if the
 * testing DSL is wanted directly on the test class, implement
 * {@link TesterWrappers} (and optionally
 * {@link com.vaadin.browserless.locator.Locators Locators}):
 *
 * <pre>
 * {@code
 * &#64;ViewPackages(classes = MyView.class)
 * class MyStatefulTest implements TesterWrappers {
 *     &#64;RegisterExtension
 *     static BrowserlessClassExtension ext = new BrowserlessClassExtension();
 * }
 * }
 * </pre>
 *
 * <p>
 * To get a graphical ASCII representation of the UI tree on failure, add
 * {@code @ExtendWith(TreeOnFailureExtension.class)} to the test class.
 *
 * @see ViewPackages
 *
 * @see BrowserlessExtension
 *
 * @see BrowserlessClassExtension
 * @see BrowserlessTestConfig
 * @since 1.0
 */
@ExtendWith(BrowserlessTestConfigExtension.class)
public abstract class BrowserlessTest extends BaseBrowserlessTest
        implements TesterWrappers {

    @BeforeEach
    @Override
    protected void initVaadinEnvironment() {
        super.initVaadinEnvironment();
    }

    @AfterEach
    @Override
    protected void cleanVaadinEnvironment() {
        super.cleanVaadinEnvironment();
    }

    @Override
    protected final String testingEngine() {
        return "JUnit 6";
    }
}
