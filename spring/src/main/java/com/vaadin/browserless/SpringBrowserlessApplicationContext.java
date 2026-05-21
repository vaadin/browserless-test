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

import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
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
 * <li>{@link #create(Routes, ApplicationContext)} — returns the unsecured
 * {@link BrowserlessApplicationContext}.</li>
 * <li>{@link #createSecured(Routes, ApplicationContext)} — returns the
 * credential-typed {@link SecuredBrowserlessApplicationContext}; requires
 * Spring Security on the classpath.</li>
 * <li>{@link #builder(Routes, ApplicationContext)} — returns a pre-wired
 * {@link BrowserlessApplicationContext.Builder} for full customization (e.g.
 * plugging in a different security handler).</li>
 * </ul>
 *
 * <pre>
 * var app = SpringBrowserlessApplicationContext.createSecured(routes,
 *         springCtx);
 * var admin = app.newUser("admin", "ADMIN");
 * var window = admin.newWindow();
 * window.navigate(ProtectedView.class);
 * </pre>
 *
 * @see BrowserlessApplicationContext
 * @see SecuredBrowserlessApplicationContext
 * @see SpringSecurityContextHandler
 */
public final class SpringBrowserlessApplicationContext {

    private SpringBrowserlessApplicationContext() {
    }

    /**
     * Creates a Spring-pre-wired builder. The builder has the Spring servlet,
     * the lookup initializer and the lookup-initializer close hook configured;
     * callers can chain additional customizations before calling
     * {@link BrowserlessApplicationContext.Builder#build()}.
     *
     * @param routes
     *            the discovered routes
     * @param applicationContext
     *            the Spring application context
     * @return a pre-wired builder
     */
    public static BrowserlessApplicationContext.Builder builder(Routes routes,
            ApplicationContext applicationContext) {
        return builder(routes, applicationContext, () -> new MockedUI());
    }

    /**
     * Creates a Spring-pre-wired builder with a custom UI factory.
     *
     * @param routes
     *            the discovered routes
     * @param applicationContext
     *            the Spring application context
     * @param uiFactory
     *            the UI factory
     * @return a pre-wired builder
     */
    public static BrowserlessApplicationContext.Builder builder(Routes routes,
            ApplicationContext applicationContext, UIFactory uiFactory) {
        BrowserlessTestSpringLookupInitializer
                .setApplicationContext(applicationContext);
        BrowserlessApplicationContext.Builder builder = BrowserlessApplicationContext
                .builder(routes)
                .withServletFactory((r, uif) -> new MockSpringServlet(r,
                        applicationContext, uif))
                .withUIFactory(uiFactory)
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
     * Creates an unsecured Spring-integrated application context.
     *
     * @param routes
     *            the discovered routes
     * @param applicationContext
     *            the Spring application context
     * @return a new unsecured application context configured for Spring
     */
    public static BrowserlessApplicationContext create(Routes routes,
            ApplicationContext applicationContext) {
        return builder(routes, applicationContext).build();
    }

    /**
     * Creates an unsecured Spring-integrated application context with a custom
     * UI factory.
     *
     * @param routes
     *            the discovered routes
     * @param applicationContext
     *            the Spring application context
     * @param uiFactory
     *            the UI factory
     * @return a new unsecured application context configured for Spring
     */
    public static BrowserlessApplicationContext create(Routes routes,
            ApplicationContext applicationContext, UIFactory uiFactory) {
        return builder(routes, applicationContext, uiFactory).build();
    }

    /**
     * Creates a Spring-integrated application context with Spring Security
     * wiring. Requires Spring Security on the classpath; throws otherwise.
     *
     * @param routes
     *            the discovered routes
     * @param applicationContext
     *            the Spring application context
     * @return a new secured application context configured for Spring Security
     * @throws IllegalStateException
     *             if Spring Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<Authentication> createSecured(
            Routes routes, ApplicationContext applicationContext) {
        return createSecured(routes, applicationContext, () -> new MockedUI());
    }

    /**
     * Creates a secured Spring-integrated application context with a custom UI
     * factory. Requires Spring Security on the classpath; throws otherwise.
     *
     * @param routes
     *            the discovered routes
     * @param applicationContext
     *            the Spring application context
     * @param uiFactory
     *            the UI factory
     * @return a new secured application context configured for Spring Security
     * @throws IllegalStateException
     *             if Spring Security is not on the classpath
     */
    public static SecuredBrowserlessApplicationContext<Authentication> createSecured(
            Routes routes, ApplicationContext applicationContext,
            UIFactory uiFactory) {
        if (!SpringSecuritySupport.isPresent()) {
            throw new IllegalStateException(
                    "SpringBrowserlessApplicationContext.createSecured(...)"
                            + " requires Spring Security on the classpath."
                            + " Use create(...) for unsecured contexts.");
        }
        return builder(routes, applicationContext, uiFactory)
                .withSecurityContextHandler(new SpringSecurityContextHandler())
                .build();
    }
}
