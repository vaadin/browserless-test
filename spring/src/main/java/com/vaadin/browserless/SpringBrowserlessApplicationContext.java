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

import java.util.Objects;
import java.util.function.UnaryOperator;

import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;

import com.vaadin.browserless.mocks.MockSpringServlet;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.mocks.SpringSecurityRequestCustomizer;

/**
 * Factory for creating a Spring-integrated
 * {@link BrowserlessApplicationContext}.
 * <p>
 * Wires a Spring-aware servlet and the
 * {@link BrowserlessTestSpringLookupInitializer} so the test context can reach
 * Spring beans and lifecycle. Three entry points are provided:
 * <ul>
 * <li>{@link #create(ApplicationContext, UnaryOperator)} (and the view-package
 * shortcuts) — returns the unsecured
 * {@link BrowserlessApplicationContext}.</li>
 * <li>{@link #createSecured(ApplicationContext, UnaryOperator)} (and the
 * view-package shortcuts) — returns the credential-typed
 * {@link SecuredBrowserlessApplicationContext}; requires Spring Security on the
 * classpath.</li>
 * <li>{@link #builder(ApplicationContext)} — returns a pre-wired
 * {@link BrowserlessApplicationContext.Builder} for full customization (e.g. a
 * custom {@code UIFactory} or a different security handler).</li>
 * </ul>
 *
 * <pre>
 * var app = SpringBrowserlessApplicationContext.createSecured(springCtx,
 *         ProtectedView.class);
 * var admin = app.newUser("admin", "ADMIN");
 * var window = admin.newWindow();
 * window.navigate(ProtectedView.class);
 * </pre>
 *
 * @see BrowserlessApplicationContext
 * @see SecuredBrowserlessApplicationContext
 * @see SpringSecurityContextHandler
 * @since 1.1
 */
public final class SpringBrowserlessApplicationContext {

    private SpringBrowserlessApplicationContext() {
    }

    /**
     * Creates a Spring-pre-wired builder. The builder has the Spring servlet,
     * the lookup initializer and the lookup-initializer close hook configured;
     * callers can chain additional customizations (including a custom
     * {@code UIFactory}) before calling
     * {@link BrowserlessApplicationContext.Builder#build()}.
     *
     * @param applicationContext
     *            the Spring application context
     * @return a pre-wired builder
     */
    public static BrowserlessApplicationContext.Builder builder(
            ApplicationContext applicationContext) {
        BrowserlessTestSpringLookupInitializer
                .setApplicationContext(applicationContext);
        BrowserlessApplicationContext.Builder builder = new BrowserlessApplicationContext.Builder()
                .withServletFactory((r, uif) -> new MockSpringServlet(r,
                        applicationContext, uif))
                .withUIFactory(() -> new MockedUI())
                .withLookupServices(
                        BrowserlessTestSpringLookupInitializer.class)
                .withCloseHook(
                        BrowserlessTestSpringLookupInitializer::clearApplicationContext);
        if (SpringSecuritySupport.isPresent()) {
            // Register the security-aware request customizer when Spring
            // Security is on the classpath. It is a no-op when no
            // authentication is set, so it is safe (and consistent with the
            // pre-split factory) to wire it on the unsecured path too.
            builder.withLookupServices(SpringSecurityRequestCustomizer.class);
        }
        return builder;
    }

    /**
     * Creates an unsecured Spring-integrated application context that scans the
     * given packages for {@code @Route}-annotated views.
     *
     * @param applicationContext
     *            the Spring application context
     * @param viewPackages
     *            package names to scan for views; an empty array falls back to
     *            a full classpath scan
     * @return a new unsecured application context configured for Spring
     */
    public static BrowserlessApplicationContext create(
            ApplicationContext applicationContext, String... viewPackages) {
        return create(applicationContext,
                b -> b.withViewPackages(viewPackages));
    }

    /**
     * Creates an unsecured Spring-integrated application context that scans the
     * packages of the given classes for {@code @Route}-annotated views.
     *
     * @param applicationContext
     *            the Spring application context
     * @param viewPackageClasses
     *            classes whose packages should be scanned for views
     * @return a new unsecured application context configured for Spring
     */
    public static BrowserlessApplicationContext create(
            ApplicationContext applicationContext,
            Class<?>... viewPackageClasses) {
        return create(applicationContext,
                b -> b.withViewPackages(viewPackageClasses));
    }

    /**
     * Creates an unsecured Spring-integrated application context, applying the
     * given configurer to the pre-wired builder before building it.
     *
     * @param applicationContext
     *            the Spring application context
     * @param configurer
     *            builder configurer; e.g. {@code b -> b.withViewPackages(...)}.
     *            Pass {@link UnaryOperator#identity()} to keep defaults.
     * @return a new unsecured application context configured for Spring
     */
    public static BrowserlessApplicationContext create(
            ApplicationContext applicationContext,
            UnaryOperator<BrowserlessApplicationContext.Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        return configurer.apply(builder(applicationContext)).build();
    }

    /**
     * Creates a Spring-integrated application context with Spring Security
     * wiring that scans the given packages for {@code @Route}-annotated views.
     * Requires Spring Security on the classpath; throws otherwise.
     *
     * @param applicationContext
     *            the Spring application context
     * @param viewPackages
     *            package names to scan for views
     * @return a new secured application context configured for Spring Security
     * @throws IllegalStateException
     *             if Spring Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<Authentication> createSecured(
            ApplicationContext applicationContext, String... viewPackages) {
        return createSecured(applicationContext,
                b -> b.withViewPackages(viewPackages));
    }

    /**
     * Creates a Spring-integrated application context with Spring Security
     * wiring that scans the packages of the given classes for
     * {@code @Route}-annotated views. Requires Spring Security on the
     * classpath; throws otherwise.
     *
     * @param applicationContext
     *            the Spring application context
     * @param viewPackageClasses
     *            classes whose packages should be scanned for views
     * @return a new secured application context configured for Spring Security
     * @throws IllegalStateException
     *             if Spring Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<Authentication> createSecured(
            ApplicationContext applicationContext,
            Class<?>... viewPackageClasses) {
        return createSecured(applicationContext,
                b -> b.withViewPackages(viewPackageClasses));
    }

    /**
     * Creates a Spring-integrated application context with Spring Security
     * wiring, applying the given configurer to the pre-wired builder. Requires
     * Spring Security on the classpath; throws otherwise.
     *
     * @param applicationContext
     *            the Spring application context
     * @param configurer
     *            builder configurer
     * @return a new secured application context configured for Spring Security
     * @throws IllegalStateException
     *             if Spring Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<Authentication> createSecured(
            ApplicationContext applicationContext,
            UnaryOperator<BrowserlessApplicationContext.Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        if (!SpringSecuritySupport.isPresent()) {
            throw new IllegalStateException(
                    "SpringBrowserlessApplicationContext.createSecured(...)"
                            + " requires Spring Security on the classpath."
                            + " Use create(...) for unsecured contexts.");
        }
        return configurer.apply(builder(applicationContext))
                .withSecurityContextHandler(new SpringSecurityContextHandler())
                .build();
    }
}
