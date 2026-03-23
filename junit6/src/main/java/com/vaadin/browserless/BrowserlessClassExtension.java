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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for browserless Vaadin testing with per-class lifecycle.
 *
 * <p>
 * The Vaadin environment is initialized once before all tests in the class and
 * torn down after all tests. Use as a static field with
 * {@code @RegisterExtension}:
 *
 * <pre>
 * {@code
 * class MyStatefulTest {
 *     &#64;RegisterExtension
 *     static BrowserlessClassExtension ext = new BrowserlessClassExtension()
 *             .withViewPackages(MyView.class);
 *
 *     &#64;BeforeAll
 *     static void setup() {
 *         ext.navigate(MyView.class);
 *     }
 *
 *     &#64;Test
 *     void testA() {
 *         /* same session *&#47; }
 * 
 *     &#64;Test
 *     void testB() {
 *         /* same session *&#47; }
 * }
 * }
 * </pre>
 *
 * <p>
 * For a fresh environment per test method, use {@link BrowserlessExtension}
 * instead.
 *
 * @see BrowserlessExtension
 * @see BrowserlessTest
 * @see ViewPackages
 */
public class BrowserlessClassExtension extends AbstractBrowserlessExtension
        implements BeforeAllCallback, AfterAllCallback {

    /**
     * Creates a new extension with per-class lifecycle.
     */
    public BrowserlessClassExtension() {
    }

    /**
     * Adds packages to scan for {@code @Route}-annotated views, derived from
     * the given classes' packages.
     *
     * @param classes
     *            classes whose packages should be scanned
     * @return this extension instance
     */
    public BrowserlessClassExtension withViewPackages(Class<?>... classes) {
        addViewPackages(classes);
        return this;
    }

    /**
     * Adds packages to scan for {@code @Route}-annotated views.
     *
     * @param packages
     *            package names to scan
     * @return this extension instance
     */
    public BrowserlessClassExtension withViewPackages(String... packages) {
        addViewPackages(packages);
        return this;
    }

    /**
     * Adds Vaadin {@link com.vaadin.flow.di.Lookup} service implementation
     * classes.
     *
     * @param serviceClasses
     *            service implementation classes to register
     * @return this extension instance
     */
    public BrowserlessClassExtension withServices(Class<?>... serviceClasses) {
        addServices(serviceClasses);
        return this;
    }

    /**
     * Adds extra packages to scan for {@link ComponentTester} implementations.
     *
     * @param packages
     *            package names to scan for testers
     * @return this extension instance
     */
    public BrowserlessClassExtension withComponentTesterPackages(
            String... packages) {
        addComponentTesterPackages(packages);
        return this;
    }

    @Override
    public void beforeAll(ExtensionContext ctx) {
        doInit(ctx.getTestInstance().orElse(null), ctx);
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        doCleanup();
    }
}
