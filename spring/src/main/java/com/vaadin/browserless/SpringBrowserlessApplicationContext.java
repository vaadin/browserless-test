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

import java.util.Set;

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
 * Configures the context with Spring-aware servlet, security context
 * handling, and lookup services.
 *
 * <pre>
 * var app = SpringBrowserlessApplicationContext.create(routes, springCtx);
 * var admin = app.newUser(adminAuth);
 * var window = admin.newWindow();
 * window.navigate(ProtectedView.class);
 * </pre>
 *
 * @see BrowserlessApplicationContext
 * @see SpringSecurityContextHandler
 */
public final class SpringBrowserlessApplicationContext {

    private SpringBrowserlessApplicationContext() {
    }

    /**
     * Creates a Spring-integrated application context.
     *
     * @param routes the discovered routes
     * @param applicationContext the Spring application context
     * @return a new application context configured for Spring
     */
    public static BrowserlessApplicationContext<Authentication> create(
            Routes routes, ApplicationContext applicationContext) {
        return create(routes, applicationContext, () -> new MockedUI());
    }

    /**
     * Creates a Spring-integrated application context with a custom UI
     * factory.
     *
     * @param routes the discovered routes
     * @param applicationContext the Spring application context
     * @param uiFactory the UI factory
     * @return a new application context configured for Spring
     */
    public static BrowserlessApplicationContext<Authentication> create(
            Routes routes, ApplicationContext applicationContext,
            UIFactory uiFactory) {
        // Set the Spring application context for the lookup initializer
        BrowserlessTestSpringLookupInitializer
                .setApplicationContext(applicationContext);

        return BrowserlessApplicationContext
                .<Authentication>builder(routes)
                .withServletFactory(r -> new MockSpringServlet(r,
                        applicationContext, uiFactory))
                .withUIFactory(uiFactory)
                .withLookupServices(Set.of(
                        BrowserlessTestSpringLookupInitializer.class,
                        SpringSecurityRequestCustomizer.class))
                .withSecurityContextHandler(
                        new SpringSecurityContextHandler())
                .build();
    }
}
