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
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.quarkus.mocks.MockQuarkusServlet;

/**
 * Factory for creating a Quarkus-integrated
 * {@link BrowserlessApplicationContext}.
 * <p>
 * Configures the context with a Quarkus-aware servlet, security context
 * handling, and lookup services.
 *
 * <pre>
 * var app = QuarkusBrowserlessApplicationContext.create(routes);
 * var admin = app.newUser(securityIdentity);
 * var window = admin.newWindow();
 * window.navigate(ProtectedView.class);
 * </pre>
 *
 * @see BrowserlessApplicationContext
 * @see QuarkusSecurityContextHandler
 */
public final class QuarkusBrowserlessApplicationContext {

    private QuarkusBrowserlessApplicationContext() {
    }

    /**
     * Creates a Quarkus-integrated application context.
     *
     * @param routes
     *            the discovered routes
     * @return a new application context configured for Quarkus
     */
    public static BrowserlessApplicationContext<SecurityIdentity> create(
            Routes routes) {
        return create(routes, () -> new MockedUI());
    }

    /**
     * Creates a Quarkus-integrated application context with a custom UI
     * factory.
     *
     * @param routes
     *            the discovered routes
     * @param uiFactory
     *            the UI factory
     * @return a new application context configured for Quarkus
     */
    public static BrowserlessApplicationContext<SecurityIdentity> create(
            Routes routes, UIFactory uiFactory) {
        BrowserlessApplicationContext.Builder<SecurityIdentity> builder = BrowserlessApplicationContext
                .<SecurityIdentity> builder(routes)
                .withServletFactory((r, uif) -> new MockQuarkusServlet(r,
                        CDI.current().getBeanManager(), uif))
                .withUIFactory(uiFactory)
                .withLookupServices(QuarkusTestLookupInitializer.class);
        if (QuarkusSecuritySupport.isPresent()) {
            builder.withSecurityContextHandler(
                    new QuarkusSecurityContextHandler());
        }
        return builder.build();
    }
}
