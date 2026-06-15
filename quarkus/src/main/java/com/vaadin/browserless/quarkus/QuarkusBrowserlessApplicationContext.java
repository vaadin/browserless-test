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
package com.vaadin.browserless.quarkus;

import jakarta.enterprise.inject.spi.CDI;

import java.util.Objects;
import java.util.function.UnaryOperator;

import io.quarkus.security.identity.SecurityIdentity;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.quarkus.mocks.MockQuarkusServlet;

/**
 * Factory for creating a Quarkus-integrated
 * {@link BrowserlessApplicationContext}.
 * <p>
 * Wires a Quarkus-aware servlet and the {@link QuarkusTestLookupInitializer}.
 * Three entry points are provided:
 * <ul>
 * <li>{@link #create(UnaryOperator)} (and the view-package shortcuts) — returns
 * the unsecured {@link BrowserlessApplicationContext}.</li>
 * <li>{@link #createSecured(UnaryOperator)} (and the view-package shortcuts) —
 * returns the credential-typed {@link SecuredBrowserlessApplicationContext};
 * requires Quarkus Security on the classpath.</li>
 * <li>{@link #builder()} — returns a pre-wired
 * {@link BrowserlessApplicationContext.Builder} for full customization (e.g. a
 * custom {@code UIFactory} or a different security handler).</li>
 * </ul>
 *
 * <pre>
 * var app = QuarkusBrowserlessApplicationContext
 *         .createSecured(ProtectedView.class);
 * var admin = app.newUser(securityIdentity);
 * var window = admin.newWindow();
 * window.navigate(ProtectedView.class);
 * </pre>
 *
 * @see BrowserlessApplicationContext
 * @see SecuredBrowserlessApplicationContext
 * @see QuarkusSecurityContextHandler
 * @since 1.1
 */
public final class QuarkusBrowserlessApplicationContext {

    private QuarkusBrowserlessApplicationContext() {
    }

    /**
     * Creates a Quarkus-pre-wired builder. The builder has the Quarkus servlet
     * and the lookup initializer configured; callers can chain additional
     * customizations (including a custom {@code UIFactory}) before calling
     * {@link BrowserlessApplicationContext.Builder#build()}.
     *
     * @return a pre-wired builder
     */
    public static BrowserlessApplicationContext.Builder builder() {
        return new BrowserlessApplicationContext.Builder()
                .withServletFactory((r, uif) -> new MockQuarkusServlet(r,
                        CDI.current().getBeanManager(), uif))
                .withUIFactory(() -> new MockedUI())
                .withLookupServices(QuarkusTestLookupInitializer.class);
    }

    /**
     * Creates an unsecured Quarkus-integrated application context that scans
     * the given packages for {@code @Route}-annotated views.
     *
     * @param viewPackages
     *            package names to scan for views; an empty array falls back to
     *            a full classpath scan
     * @return a new unsecured application context configured for Quarkus
     */
    public static BrowserlessApplicationContext create(String... viewPackages) {
        return create(b -> b.withViewPackages(viewPackages));
    }

    /**
     * Creates an unsecured Quarkus-integrated application context that scans
     * the packages of the given classes for {@code @Route}-annotated views.
     *
     * @param viewPackageClasses
     *            classes whose packages should be scanned for views
     * @return a new unsecured application context configured for Quarkus
     */
    public static BrowserlessApplicationContext create(
            Class<?>... viewPackageClasses) {
        return create(b -> b.withViewPackages(viewPackageClasses));
    }

    /**
     * Creates an unsecured Quarkus-integrated application context, applying the
     * given configurer to the pre-wired builder before building it.
     *
     * @param configurer
     *            builder configurer; e.g. {@code b -> b.withViewPackages(...)}.
     *            Pass {@link UnaryOperator#identity()} to keep defaults.
     * @return a new unsecured application context configured for Quarkus
     */
    public static BrowserlessApplicationContext create(
            UnaryOperator<BrowserlessApplicationContext.Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        return configurer.apply(builder()).build();
    }

    /**
     * Creates a Quarkus-integrated application context with Quarkus Security
     * wiring that scans the given packages for {@code @Route}-annotated views.
     * Requires Quarkus Security on the classpath; throws otherwise.
     *
     * @param viewPackages
     *            package names to scan for views
     * @return a new secured application context configured for Quarkus Security
     * @throws IllegalStateException
     *             if Quarkus Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<SecurityIdentity> createSecured(
            String... viewPackages) {
        return createSecured(b -> b.withViewPackages(viewPackages));
    }

    /**
     * Creates a Quarkus-integrated application context with Quarkus Security
     * wiring that scans the packages of the given classes for
     * {@code @Route}-annotated views. Requires Quarkus Security on the
     * classpath; throws otherwise.
     *
     * @param viewPackageClasses
     *            classes whose packages should be scanned for views
     * @return a new secured application context configured for Quarkus Security
     * @throws IllegalStateException
     *             if Quarkus Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<SecurityIdentity> createSecured(
            Class<?>... viewPackageClasses) {
        return createSecured(b -> b.withViewPackages(viewPackageClasses));
    }

    /**
     * Creates a Quarkus-integrated application context with Quarkus Security
     * wiring, applying the given configurer to the pre-wired builder. Requires
     * Quarkus Security on the classpath; throws otherwise.
     *
     * @param configurer
     *            builder configurer
     * @return a new secured application context configured for Quarkus Security
     * @throws IllegalStateException
     *             if Quarkus Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<SecurityIdentity> createSecured(
            UnaryOperator<BrowserlessApplicationContext.Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        if (!QuarkusSecuritySupport.isPresent()) {
            throw new IllegalStateException(
                    "QuarkusBrowserlessApplicationContext.createSecured(...)"
                            + " requires Quarkus Security on the classpath."
                            + " Use create(...) for unsecured contexts.");
        }
        return configurer.apply(builder())
                .withSecurityContextHandler(new QuarkusSecurityContextHandler())
                .build();
    }
}
