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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.vaadin.experimental.Feature;

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
 * @since 1.1
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

    /**
     * Adds the packages of the given classes to the set of packages to scan for
     * {@link ComponentTester} implementations.
     *
     * @param classes
     *            classes whose packages should be scanned for testers
     * @return this extension instance
     */
    public BrowserlessExtension withComponentTesterPackages(
            Class<?>... classes) {
        addComponentTesterPackages(classes);
        return this;
    }

    /**
     * Sets a Vaadin application property (init parameter) for the tests using
     * this extension.
     *
     * @param name
     *            the property name
     * @param value
     *            the property value
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withApplicationProperty(String name,
            String value) {
        addApplicationProperty(name, value);
        return this;
    }

    /**
     * Sets Vaadin application properties (init parameters) for the tests using
     * this extension.
     *
     * @param properties
     *            the properties to set
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withApplicationProperties(
            Map<String, String> properties) {
        addApplicationProperties(properties);
        return this;
    }

    /**
     * Enables the given Vaadin feature flags for the tests using this
     * extension.
     *
     * @param featureIds
     *            the identifiers of the features to enable
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withFeatureFlags(String... featureIds) {
        addFeatureFlags(featureIds);
        return this;
    }

    /**
     * Enables the given Vaadin feature flags for the tests using this
     * extension.
     *
     * @param features
     *            the features to enable
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withFeatureFlags(Feature... features) {
        addFeatureFlags(features);
        return this;
    }

    /**
     * Enables or disables the given Vaadin feature flag for the tests using
     * this extension.
     *
     * @param featureId
     *            the identifier of the feature
     * @param enabled
     *            {@code true} to enable the feature, {@code false} to disable
     *            it
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withFeatureFlag(String featureId,
            boolean enabled) {
        addFeatureFlag(featureId, enabled);
        return this;
    }

    /**
     * Enables or disables the given Vaadin feature flag for the tests using
     * this extension.
     *
     * @param feature
     *            the feature
     * @param enabled
     *            {@code true} to enable the feature, {@code false} to disable
     *            it
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withFeatureFlag(Feature feature,
            boolean enabled) {
        addFeatureFlag(feature, enabled);
        return this;
    }

    /**
     * Applies the given custom Vaadin configuration to the tests using this
     * extension.
     * <p>
     * The configuration wins over the one declared by a
     * {@link BrowserlessTestConfig} annotation on the test class, but loses
     * against the one declared on the test method.
     *
     * @param configuration
     *            the configuration to apply
     * @return this extension instance
     * @since 1.2
     */
    public BrowserlessExtension withConfiguration(
            BrowserlessConfiguration configuration) {
        addConfiguration(configuration);
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
