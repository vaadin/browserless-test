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

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;

/**
 * JUnit extension resolving the {@link BrowserlessTestConfig} annotations
 * declared by a test, and making the resulting configuration available to the
 * {@link BaseBrowserlessTest} instance before it initializes the Vaadin
 * environment.
 * <p>
 * Registered by the browserless base test classes; there is no need to add it
 * to a test explicitly.
 *
 * @since 1.2
 */
public class BrowserlessTestConfigExtension
        implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        browserlessTest(context).ifPresent(test -> test
                .setResolvedConfiguration(resolveConfiguration(context)));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        browserlessTest(context)
                .ifPresent(test -> test.setResolvedConfiguration(null));
    }

    private static Optional<BaseBrowserlessTest> browserlessTest(
            ExtensionContext context) {
        return context.getTestInstance()
                .filter(BaseBrowserlessTest.class::isInstance)
                .map(BaseBrowserlessTest.class::cast);
    }

    /**
     * Resolves the effective configuration for the given extension context,
     * merging the {@link BrowserlessTestConfig} annotations declared by the
     * test class and by the current test method, if any.
     * <p>
     * Meta annotations, superclasses and, for nested tests, enclosing classes
     * are taken into account. Values declared on the test method win over the
     * ones declared on the test class, which in turn win over the ones declared
     * on an enclosing test class.
     *
     * @param context
     *            the current extension context, not {@literal null}
     * @return the effective configuration, never {@literal null}
     */
    static BrowserlessConfiguration resolveConfiguration(
            ExtensionContext context) {
        return resolveConfiguration(context, BrowserlessConfiguration.empty());
    }

    /**
     * Same as {@link #resolveConfiguration(ExtensionContext)}, but with a
     * configuration defined programmatically that wins over the one declared on
     * the test class, and loses against the one declared on the test method.
     *
     * @param context
     *            the current extension context, not {@literal null}
     * @param programmaticConfiguration
     *            the configuration defined programmatically, not
     *            {@literal null}
     * @return the effective configuration, never {@literal null}
     */
    static BrowserlessConfiguration resolveConfiguration(
            ExtensionContext context,
            BrowserlessConfiguration programmaticConfiguration) {
        // Enclosing classes of a nested test are merged from the outermost to
        // the innermost one, so that a nested test can refine the configuration
        // declared by its enclosing test class.
        BrowserlessConfiguration configuration = BrowserlessConfiguration
                .empty();
        for (Class<?> enclosingClass : context.getEnclosingTestClasses()) {
            configuration = configuration.merge(forClass(enclosingClass));
        }
        configuration = configuration
                .merge(context.getTestClass()
                        .map(BrowserlessTestConfigExtension::forClass)
                        .orElseGet(BrowserlessConfiguration::empty))
                .merge(programmaticConfiguration);

        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isEmpty()) {
            // The Vaadin environment is created once for the whole test class,
            // so a method level configuration could not be applied.
            context.getTestClass().ifPresent(
                    BrowserlessTestConfigExtension::failOnMethodLevelConfiguration);
            return configuration;
        }
        return configuration.merge(AnnotationSupport
                .findAnnotation(testMethod.get(), BrowserlessTestConfig.class)
                .map(BrowserlessConfiguration::from)
                .orElseGet(BrowserlessConfiguration::empty));
    }

    private static BrowserlessConfiguration forClass(Class<?> testClass) {
        return AnnotationSupport
                .findAnnotation(testClass, BrowserlessTestConfig.class)
                .map(BrowserlessConfiguration::from)
                .orElseGet(BrowserlessConfiguration::empty);
    }

    /**
     * Fails if the given test class declares a {@link BrowserlessTestConfig}
     * annotation on a method, which cannot be honored when the Vaadin
     * environment is created once for the whole class.
     *
     * @param testClass
     *            the test class to check, not {@literal null}
     */
    static void failOnMethodLevelConfiguration(Class<?> testClass) {
        String annotatedMethods = AnnotationSupport
                .findAnnotatedMethods(testClass, BrowserlessTestConfig.class,
                        HierarchyTraversalMode.TOP_DOWN)
                .stream().map(Method::getName).sorted()
                .collect(Collectors.joining(", "));
        if (!annotatedMethods.isEmpty()) {
            throw new BrowserlessTestSetupException("@"
                    + BrowserlessTestConfig.class.getSimpleName()
                    + " is not supported on test methods when the Vaadin environment is "
                    + "shared by all the tests in the class, but it is present on "
                    + annotatedMethods
                    + ". Move the configuration to the test class, or use a Vaadin "
                    + "environment created for each test method.");
        }
    }
}
