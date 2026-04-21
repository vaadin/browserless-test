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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Application-level context for multi-user browserless testing.
 * <p>
 * Manages a shared {@link VaadinServletService} and servlet that are shared
 * across all users and their windows. This models the application level of the
 * Vaadin hierarchy: one application contains multiple user sessions, each with
 * multiple UI instances (browser tabs).
 * <p>
 * Create instances via {@link #create(Routes)} for plain Java or use the
 * {@link #builder(Routes)} for full customization. Framework-specific modules
 * (Spring, Quarkus) provide their own convenience factory methods.
 *
 * <pre>
 * var app = BrowserlessApplicationContext.create(routes);
 * var user1 = app.newUser();
 * var window1 = user1.newWindow();
 * window1.navigate(MyView.class);
 * // ...
 * app.close();
 * </pre>
 *
 * @see BrowserlessUserContext
 * @see BrowserlessUIContext
 */
public class BrowserlessApplicationContext<C> implements AutoCloseable {

    private final VaadinServletService service;
    private final VaadinServlet servlet;
    private final UIFactory uiFactory;
    private final SecurityContextHandler<C> securityContextHandler;
    private final List<BrowserlessUserContext> users = new ArrayList<>();
    private boolean closed;

    private BrowserlessApplicationContext(VaadinServletService service,
            VaadinServlet servlet, UIFactory uiFactory,
            SecurityContextHandler<C> securityContextHandler) {
        this.service = service;
        this.servlet = servlet;
        this.uiFactory = uiFactory;
        this.securityContextHandler = securityContextHandler;
    }

    /**
     * Creates a plain Java application context with default settings.
     *
     * @param routes
     *            the discovered routes
     * @return a new application context
     */
    public static BrowserlessApplicationContext<Void> create(Routes routes) {
        return BrowserlessApplicationContext.<Void> builder(routes).build();
    }

    /**
     * Creates a builder for customizing the application context.
     *
     * @param <C>
     *            the credentials type for the security context handler
     * @param routes
     *            the discovered routes
     * @return a new builder
     */
    public static <C> Builder<C> builder(Routes routes) {
        return new Builder<>(routes);
    }

    /**
     * Creates a new user context representing an independent user session.
     * <p>
     * The returned context has its own
     * {@link com.vaadin.flow.server.VaadinSession}, HTTP request, and response.
     * If credentials are provided and a {@link SecurityContextHandler} is
     * configured, authentication is set up automatically.
     *
     * @return the new user context
     */
    public BrowserlessUserContext newUser() {
        return newUser(null);
    }

    /**
     * Creates a new user context with the given credentials.
     * <p>
     * The credentials are passed to
     * {@link SecurityContextHandler#setupAuthentication(Object)
     * SecurityContextHandler.setupAuthentication()} if a handler is configured.
     * The security context is then automatically captured as the user's initial
     * snapshot.
     *
     * @param credentials
     *            framework-specific credentials, or {@code null} for an
     *            anonymous user
     * @return the new user context
     */
    public BrowserlessUserContext newUser(C credentials) {
        checkNotClosed();
        var user = new BrowserlessUserContext(this, credentials);
        users.add(user);
        return user;
    }

    /**
     * Closes this application context and all its user contexts.
     * <p>
     * Fires service destroy listeners and clears thread-local state.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (var user : users) {
            user.close();
        }
        users.clear();
        MockVaadin.fireServiceDestroy(service);
        VaadinService.setCurrent(null);
    }

    VaadinServletService getService() {
        return service;
    }

    VaadinServlet getServlet() {
        return servlet;
    }

    UIFactory getUIFactory() {
        return uiFactory;
    }

    SecurityContextHandler<C> getSecurityContextHandler() {
        return securityContextHandler;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException(
                    "BrowserlessApplicationContext is already closed");
        }
    }

    /**
     * Builder for creating a customized {@link BrowserlessApplicationContext}.
     */
    public static class Builder<C> {

        private final Routes routes;
        private SecurityContextHandler<C> securityContextHandler;
        private Function<Routes, VaadinServlet> servletFactory;
        private UIFactory uiFactory = () -> new MockedUI();
        private Set<Class<?>> lookupServices = Collections.emptySet();

        Builder(Routes routes) {
            this.routes = Objects.requireNonNull(routes);
        }

        /**
         * Sets the security context handler for multi-user auth isolation.
         *
         * @param handler
         *            the handler, or {@code null} for no security management
         * @return this builder
         */
        public Builder<C> withSecurityContextHandler(
                SecurityContextHandler<C> handler) {
            this.securityContextHandler = handler;
            return this;
        }

        /**
         * Sets a custom servlet factory. The factory receives the routes and
         * must return a fully configured {@link VaadinServlet}.
         *
         * @param factory
         *            the servlet factory
         * @return this builder
         */
        public Builder<C> withServletFactory(
                Function<Routes, VaadinServlet> factory) {
            this.servletFactory = Objects.requireNonNull(factory);
            return this;
        }

        /**
         * Sets the UI factory for creating new UI instances.
         *
         * @param uiFactory
         *            the UI factory
         * @return this builder
         */
        public Builder<C> withUIFactory(UIFactory uiFactory) {
            this.uiFactory = Objects.requireNonNull(uiFactory);
            return this;
        }

        /**
         * Sets the Vaadin Lookup service classes.
         *
         * @param services
         *            the service implementation classes
         * @return this builder
         */
        public Builder<C> withLookupServices(Set<Class<?>> services) {
            this.lookupServices = Objects.requireNonNull(services);
            return this;
        }

        /**
         * Builds the application context.
         *
         * @return a new application context
         */
        public BrowserlessApplicationContext<C> build() {
            VaadinServlet servlet = servletFactory != null
                    ? servletFactory.apply(routes)
                    : new MockVaadinServlet(routes, uiFactory);
            VaadinServletService service = MockVaadin.setupServlet(servlet,
                    lookupServices);
            return new BrowserlessApplicationContext<>(service, servlet,
                    uiFactory, securityContextHandler);
        }
    }
}
