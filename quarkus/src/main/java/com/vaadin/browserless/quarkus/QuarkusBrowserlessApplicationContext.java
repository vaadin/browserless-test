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

import io.quarkus.security.identity.SecurityIdentity;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.quarkus.mocks.MockQuarkusServlet;

/**
 * Factory for creating a Quarkus-integrated
 * {@link BrowserlessApplicationContext}.
 * <p>
 * Wires a Quarkus-aware servlet and the {@link QuarkusTestLookupInitializer}.
 * Three entry points are provided:
 * <ul>
 * <li>{@link #create(Routes)} — returns the unsecured
 * {@link BrowserlessApplicationContext}.</li>
 * <li>{@link #createSecured(Routes)} — returns the credential-typed
 * {@link SecuredBrowserlessApplicationContext}; requires Quarkus Security on
 * the classpath.</li>
 * <li>{@link #builder(Routes)} — returns a pre-wired
 * {@link BrowserlessApplicationContext.Builder} for full customization (e.g.
 * plugging in a different security handler).</li>
 * </ul>
 *
 * <pre>
 * var app = QuarkusBrowserlessApplicationContext.createSecured(routes);
 * var admin = app.newUser(securityIdentity);
 * var window = admin.newWindow();
 * window.navigate(ProtectedView.class);
 * </pre>
 *
 * @see BrowserlessApplicationContext
 * @see SecuredBrowserlessApplicationContext
 * @see QuarkusSecurityContextHandler
 */
public final class QuarkusBrowserlessApplicationContext {

    private QuarkusBrowserlessApplicationContext() {
    }

    /**
     * Creates a Quarkus-pre-wired builder. The builder has the Quarkus servlet
     * and the lookup initializer configured; callers can chain additional
     * customizations before calling
     * {@link BrowserlessApplicationContext.Builder#build()}.
     *
     * @param routes
     *            the discovered routes
     * @return a pre-wired builder
     */
    public static BrowserlessApplicationContext.Builder builder(Routes routes) {
        return builder(routes, () -> new MockedUI());
    }

    /**
     * Creates a Quarkus-pre-wired builder with a custom UI factory.
     *
     * @param routes
     *            the discovered routes
     * @param uiFactory
     *            the UI factory
     * @return a pre-wired builder
     */
    public static BrowserlessApplicationContext.Builder builder(Routes routes,
            UIFactory uiFactory) {
        return BrowserlessApplicationContext.builder(routes)
                .withServletFactory((r, uif) -> new MockQuarkusServlet(r,
                        CDI.current().getBeanManager(), uif))
                .withUIFactory(uiFactory)
                .withLookupServices(QuarkusTestLookupInitializer.class);
    }

    /**
     * Creates an unsecured Quarkus-integrated application context.
     *
     * @param routes
     *            the discovered routes
     * @return a new unsecured application context configured for Quarkus
     */
    public static BrowserlessApplicationContext create(Routes routes) {
        return builder(routes).build();
    }

    /**
     * Creates an unsecured Quarkus-integrated application context with a custom
     * UI factory.
     *
     * @param routes
     *            the discovered routes
     * @param uiFactory
     *            the UI factory
     * @return a new unsecured application context configured for Quarkus
     */
    public static BrowserlessApplicationContext create(Routes routes,
            UIFactory uiFactory) {
        return builder(routes, uiFactory).build();
    }

    /**
     * Creates a Quarkus-integrated application context with Quarkus Security
     * wiring. Requires Quarkus Security on the classpath; throws otherwise.
     *
     * @param routes
     *            the discovered routes
     * @return a new secured application context configured for Quarkus Security
     * @throws IllegalStateException
     *             if Quarkus Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<SecurityIdentity> createSecured(
            Routes routes) {
        return createSecured(routes, () -> new MockedUI());
    }

    /**
     * Creates a secured Quarkus-integrated application context with a custom UI
     * factory. Requires Quarkus Security on the classpath; throws otherwise.
     *
     * @param routes
     *            the discovered routes
     * @param uiFactory
     *            the UI factory
     * @return a new secured application context configured for Quarkus Security
     * @throws IllegalStateException
     *             if Quarkus Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<SecurityIdentity> createSecured(
            Routes routes, UIFactory uiFactory) {
        if (!QuarkusSecuritySupport.isPresent()) {
            throw new IllegalStateException(
                    "QuarkusBrowserlessApplicationContext.createSecured(...)"
                            + " requires Quarkus Security on the classpath."
                            + " Use create(...) for unsecured contexts.");
        }
        return builder(routes, uiFactory)
                .withSecurityContextHandler(new QuarkusSecurityContextHandler())
                .build();
    }
}
