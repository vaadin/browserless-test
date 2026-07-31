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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a custom Vaadin configuration to a single test class or test method.
 * <p>
 * Both Vaadin application properties (also known as init parameters) and
 * feature flags are scoped to the mock Vaadin environment created for the
 * annotated test, so they neither require a {@code @BeforeEach}/
 * {@code @AfterEach} pair to set and reset system properties, nor leak into
 * other tests.
 *
 * <pre>
 * &#64;BrowserlessTestConfig(applicationProperties = "devmode.sessionSerialization.enabled=true", featureFlags = "myExperimentalFeature")
 * class MyViewTest extends BrowserlessTest {
 * }
 * </pre>
 *
 * The annotation can be placed both on the test class and on a test method. In
 * that case the two configurations are merged, and values declared on the
 * method win over the ones declared on the class.
 * <p>
 * Method level configuration requires an environment that is created for each
 * test method. It is therefore not supported when the Vaadin environment is
 * shared by all the tests in the class, for example with
 * {@link BrowserlessTestConfig} on a class using
 * {@code @TestInstance(Lifecycle.PER_CLASS)} or
 * {@code BrowserlessClassExtension}; in those cases the annotation must be
 * placed on the test class.
 *
 * @see BrowserlessConfiguration
 * @since 1.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Inherited
public @interface BrowserlessTestConfig {

    /**
     * Vaadin feature flags to enable or disable for the annotated test,
     * overriding the values potentially defined in the
     * {@literal vaadin-featureflags.properties} file or in system properties.
     * <p>
     * Each entry is either the identifier of the feature, to enable it, or an
     * {@code id=true|false} pair, for example {@code featureFlags = {
     * "featureToEnable", "featureToDisable=false" }}.
     *
     * @return the feature flags to override, never {@literal null}
     */
    String[] featureFlags() default {};

    /**
     * Vaadin application properties (init parameters) to apply to the mock
     * Vaadin environment created for the annotated test.
     * <p>
     * Each entry is a {@code name=value} pair, for example
     * {@code applicationProperties = "devmode.sessionSerialization.enabled=true"}.
     * Values are split on the first {@literal =} character, so a value may
     * itself contain {@literal =}.
     *
     * @return the application properties to apply, never {@literal null}
     */
    String[] applicationProperties() default {};

    /**
     * Service implementation classes to be used to initialize the Vaadin
     * {@link com.vaadin.flow.di.Lookup} for the annotated test, such as
     * {@link com.vaadin.flow.di.InstantiatorFactory} or
     * {@link com.vaadin.flow.di.ResourceProvider} implementations.
     * <p>
     * Unlike application properties and feature flags, lookup services declared
     * on the test class and on the test method are <strong>accumulated</strong>
     * rather than replaced: a test method can add a service, but cannot remove
     * one declared by its test class.
     *
     * @return the lookup service implementation classes, never {@literal null}
     */
    Class<?>[] lookupServices() default {};
}
