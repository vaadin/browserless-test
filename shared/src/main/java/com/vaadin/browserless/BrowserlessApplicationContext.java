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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

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
 * multiple UI instances (browser windows).
 * <p>
 * Instances of this class are <strong>thread-affine</strong>: they must be
 * created, used, and closed on the same thread. The active context is held in a
 * {@link ThreadLocal} and is not visible to other threads. This class is not
 * safe for concurrent access from multiple threads; driving the same context
 * from parallel test threads is unsupported.
 * <p>
 * This base class is unsecured: it has no {@link SecurityContextHandler}. For
 * multi-user security isolation, configure a handler via
 * {@link Builder#withSecurityContextHandler(SecurityContextHandler)} — the
 * builder transitions to {@link SecuredBrowserlessApplicationContext.Builder}
 * and {@link SecuredBrowserlessApplicationContext.Builder#build() build()}
 * returns the typed {@link SecuredBrowserlessApplicationContext}, which exposes
 * credential-aware {@code newUser(...)} overloads. Framework-specific modules
 * (Spring, Quarkus) provide convenience factory methods for both paths.
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
 * @see SecuredBrowserlessApplicationContext
 */
public class BrowserlessApplicationContext implements AutoCloseable {

    private final VaadinServletService service;
    private final UIFactory uiFactory;
    private final List<Runnable> closeHooks;
    private final List<BrowserlessUserContext> users = new ArrayList<>();
    private TestSignalEnvironment signalsTestEnvironment;
    private boolean closed;

    BrowserlessApplicationContext(VaadinServletService service,
            UIFactory uiFactory, List<Runnable> closeHooks) {
        this.service = service;
        this.uiFactory = uiFactory;
        this.closeHooks = closeHooks;
        // Always-on Signals support, mirroring BaseBrowserlessTest. Registered
        // here so it covers session-init listeners fired by
        // BrowserlessUserContext.
        this.signalsTestEnvironment = TestSignalEnvironment.register();
    }

    /**
     * Creates a plain Java application context with default settings.
     * <p>
     * The returned context has no {@link SecurityContextHandler} configured;
     * use {@link #builder(Routes)} and
     * {@link Builder#withSecurityContextHandler(SecurityContextHandler)} to
     * enable framework-specific security integration.
     *
     * @param routes
     *            the discovered routes
     * @return a new application context
     */
    public static BrowserlessApplicationContext create(Routes routes) {
        return builder(routes).build();
    }

    /**
     * Creates a builder for customizing the application context.
     *
     * @param routes
     *            the discovered routes
     * @return a new builder
     */
    public static Builder builder(Routes routes) {
        return new Builder(routes);
    }

    /**
     * Creates a new user context representing an anonymous user session.
     * <p>
     * When a {@link SecurityContextHandler} is configured (on a
     * {@link SecuredBrowserlessApplicationContext}), the handler is asked to
     * install its anonymous-equivalent state (e.g. Spring sets an
     * {@code AnonymousAuthenticationToken}). On this unsecured base class no
     * security setup is performed.
     *
     * @return the new user context
     * @throws IllegalStateException
     *             if this context has been closed
     */
    public BrowserlessUserContext newUser() {
        return newUserInternal(null);
    }

    BrowserlessUserContext newUserInternal(Object credentials) {
        checkNotClosed();
        var user = new BrowserlessUserContext(this, credentials);
        users.add(user);
        return user;
    }

    /**
     * Closes this application context and all its user contexts.
     * <p>
     * Closes every {@link BrowserlessUserContext} created by this application
     * (which in turn closes their windows), fires service destroy listeners,
     * and resets the {@link VaadinService} thread-local. This method is
     * idempotent: subsequent invocations have no effect.
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
        if (signalsTestEnvironment != null) {
            signalsTestEnvironment.unregister();
            signalsTestEnvironment = null;
        }
        MockVaadin.fireServiceDestroy(service);
        VaadinService.setCurrent(null);
        List<RuntimeException> hookFailures = new ArrayList<>();
        for (Runnable hook : closeHooks) {
            try {
                hook.run();
            } catch (RuntimeException e) {
                hookFailures.add(e);
            }
        }
        if (!hookFailures.isEmpty()) {
            RuntimeException aggregate = new RuntimeException(
                    "One or more close hooks failed");
            hookFailures.forEach(aggregate::addSuppressed);
            throw aggregate;
        }
    }

    VaadinServletService getService() {
        return service;
    }

    UIFactory getUIFactory() {
        return uiFactory;
    }

    TestSignalEnvironment getSignalsTestEnvironment() {
        return signalsTestEnvironment;
    }

    /**
     * Returns the configured security context handler, or {@code null} when
     * unsecured. Overridden by {@link SecuredBrowserlessApplicationContext} to
     * return its typed, non-null handler.
     */
    SecurityContextHandler<?> getSecurityContextHandler() {
        return null;
    }

    void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException(
                    "BrowserlessApplicationContext is already closed");
        }
    }

    /**
     * Builder for creating a customized {@link BrowserlessApplicationContext}.
     * <p>
     * Calling {@link #withSecurityContextHandler(SecurityContextHandler)}
     * transitions to a {@link SecuredBrowserlessApplicationContext.Builder}
     * whose {@link SecuredBrowserlessApplicationContext.Builder#build()
     * build()} returns the credential-typed
     * {@link SecuredBrowserlessApplicationContext}.
     */
    public static class Builder {

        private final Routes routes;
        private BiFunction<Routes, UIFactory, VaadinServlet> servletFactory;
        private UIFactory uiFactory = () -> new MockedUI();
        private Set<Class<?>> lookupServices = Collections.emptySet();
        private final List<Runnable> closeHooks = new ArrayList<>();

        Builder(Routes routes) {
            this.routes = Objects.requireNonNull(routes);
        }

        /**
         * Sets the security context handler for multi-user auth isolation and
         * transitions to a credential-typed
         * {@link SecuredBrowserlessApplicationContext.Builder}. The base
         * builder's accumulated state (servlet factory, UI factory, lookup
         * services, close hooks) is carried over.
         *
         * @param <C>
         *            the credentials type accepted by the handler
         * @param handler
         *            the handler; must not be {@code null}
         * @return a secured builder configured with the given handler
         * @throws NullPointerException
         *             if {@code handler} is {@code null}
         */
        public <C> SecuredBrowserlessApplicationContext.Builder<C> withSecurityContextHandler(
                SecurityContextHandler<C> handler) {
            Objects.requireNonNull(handler, "handler must not be null");
            return new SecuredBrowserlessApplicationContext.Builder<>(this,
                    handler);
        }

        /**
         * Sets a custom servlet factory. The factory receives the routes and
         * the {@link UIFactory} configured via
         * {@link #withUIFactory(UIFactory)} (or its default), and must return a
         * fully configured {@link VaadinServlet}. Wiring the {@code UIFactory}
         * through the builder ensures the servlet uses the same factory as
         * {@link BrowserlessUIContext} window creation, so paths like the
         * legacy {@code afterSessionClose} session-recreation hook don't end up
         * producing UIs of a different type. When unset, a default servlet that
         * uses the configured {@code UIFactory} is created by {@link #build()}.
         *
         * @param factory
         *            the servlet factory
         * @return this builder
         * @throws NullPointerException
         *             if {@code factory} is {@code null}
         */
        public Builder withServletFactory(
                BiFunction<Routes, UIFactory, VaadinServlet> factory) {
            this.servletFactory = Objects.requireNonNull(factory);
            return this;
        }

        /**
         * Sets the UI factory used when creating UI instances for this
         * application's windows. Defaults to a factory producing
         * {@link MockedUI} instances.
         *
         * @param uiFactory
         *            the UI factory
         * @return this builder
         * @throws NullPointerException
         *             if {@code uiFactory} is {@code null}
         */
        public Builder withUIFactory(UIFactory uiFactory) {
            this.uiFactory = Objects.requireNonNull(uiFactory);
            return this;
        }

        /**
         * Adds the given Vaadin Lookup service classes to the set configured
         * for this builder. Successive calls accumulate; the builder starts
         * with an empty set. Calling with no arguments is a no-op.
         *
         * @param services
         *            the service implementation classes to add
         * @return this builder
         * @throws NullPointerException
         *             if {@code services} or any of its elements is
         *             {@code null}
         */
        public Builder withLookupServices(Class<?>... services) {
            Objects.requireNonNull(services);
            if (services.length == 0) {
                return this;
            }
            Set<Class<?>> updated = new LinkedHashSet<>(this.lookupServices);
            for (Class<?> service : services) {
                updated.add(Objects.requireNonNull(service));
            }
            this.lookupServices = updated;
            return this;
        }

        Set<Class<?>> getLookupServices() {
            return lookupServices;
        }

        /**
         * Registers a hook to be invoked when the built application context is
         * closed.
         * <p>
         * Hooks run in registration order, after users and the Vaadin service
         * have been torn down, exactly once (subsequent {@code close()} calls
         * are no-ops). They are intended for releasing framework-specific state
         * attached during context creation — for example, Spring's
         * lookup-initializer ThreadLocal. A throwing hook does not prevent the
         * remaining hooks from running; any thrown {@link RuntimeException}s
         * are collected and surfaced from {@code close()} as a single
         * {@link RuntimeException} with the originals attached as suppressed
         * exceptions.
         *
         * @param hook
         *            the hook to register; must not be {@code null}
         * @return this builder
         * @throws NullPointerException
         *             if {@code hook} is {@code null}
         */
        public Builder withCloseHook(Runnable hook) {
            this.closeHooks.add(Objects.requireNonNull(hook));
            return this;
        }

        /**
         * Builds the application context.
         *
         * @return a new application context
         */
        public BrowserlessApplicationContext build() {
            return new BrowserlessApplicationContext(buildService(), uiFactory,
                    buildCloseHooks());
        }

        VaadinServletService buildService() {
            VaadinServlet servlet = servletFactory != null
                    ? servletFactory.apply(routes, uiFactory)
                    : new MockVaadinServlet(routes, uiFactory);
            return MockVaadin.setupServlet(servlet, lookupServices);
        }

        UIFactory uiFactory() {
            return uiFactory;
        }

        List<Runnable> buildCloseHooks() {
            return List.copyOf(closeHooks);
        }
    }
}
