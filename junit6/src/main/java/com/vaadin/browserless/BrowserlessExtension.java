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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for browserless Vaadin testing with per-method lifecycle.
 *
 * <p>
 * A fresh Vaadin environment is initialized before each test method and torn
 * down after. Use as an instance field with {@code @RegisterExtension}:
 *
 * <pre>
 * {@code
 * &#64;ViewPackages(classes = MyView.class)
 * class MyTest {
 *     &#64;RegisterExtension
 *     BrowserlessExtension ext = new BrowserlessExtension()
 *             .withServices(MyService.class);
 *
 *     &#64;Test
 *     void test() {
 *         MyView view = ext.navigate(MyView.class);
 *         ext.test(view.getButton()).click();
 *     }
 * }
 * }
 * </pre>
 *
 * <p>
 * For a shared Vaadin environment across all tests in a class, use
 * {@link BrowserlessClassExtension} instead.
 *
 * @see BrowserlessClassExtension
 * @see BrowserlessTest
 * @see ViewPackages
 */
public class BrowserlessExtension extends AbstractBrowserlessExtension
        implements BeforeEachCallback, AfterEachCallback {

    /**
     * Creates a new extension with per-method lifecycle.
     */
    public BrowserlessExtension() {
    }

    /**
     * Adds packages to scan for {@code @Route}-annotated views, derived from
     * the given classes' packages.
     *
     * @param classes
     *            classes whose packages should be scanned
     * @return this extension instance
     */
    public BrowserlessExtension withViewPackages(Class<?>... classes) {
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
    public BrowserlessExtension withViewPackages(String... packages) {
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
    public BrowserlessExtension withServices(Class<?>... serviceClasses) {
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
    public BrowserlessExtension withComponentTesterPackages(
            String... packages) {
        addComponentTesterPackages(packages);
        return this;
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        doInit(ctx.getTestInstance().orElse(null), ctx);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        doCleanup();
    }
}
