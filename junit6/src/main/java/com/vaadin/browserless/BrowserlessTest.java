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
 * &#64;ViewPackages(classes = {CartView.class, CheckoutView.class})
 * class CartViewTest extends BrowserlessTest {
 * }
 *
 * &#64;ViewPackages(packages = {"com.example.shop.cart", "com.example.security"})
 * class CartViewTest extends BrowserlessTest {
 * }
 *
 * &#64;ViewPackages(
 *    classes = {CartView.class, CheckoutView.class},
 *    packages = {"com.example.security"}
 * )
 * class CartViewTest extends BrowserlessTest {
 * }
 * </pre>
 *
 * The Vaadin environment lifecycle is managed by {@link
 * BrowserlessTestExtension}, which calls {@link #initVaadinEnvironment()}
 * before each test and {@link #cleanVaadinEnvironment()} after each test via
 * virtual dispatch. When the test class is annotated with
 * {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)}, the environment is
 * shared across all tests in the class (initialized once in {@code @BeforeAll},
 * torn down in {@code @AfterAll}).
 *
 * <p>
 * To provide custom Flow service implementations via the {@link
 * com.vaadin.flow.di.Lookup} SPI, override {@link #lookupServices()}:
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
 * <strong>Note:</strong> Subclasses that override {@code initVaadinEnvironment}
 * must NOT add {@code @BeforeEach} — the extension handles invocation via
 * virtual dispatch.
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

    @Override
    protected final String testingEngine() {
        return "JUnit 6";
    }
}
