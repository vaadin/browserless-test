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
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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
 * var app = BrowserlessApplicationContext.create(MyView.class);
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
     * Creates a plain Java application context, scanning the given packages for
     * {@code @Route}-annotated views.
     *
     * @param viewPackages
     *            package names to scan for views; an empty array falls back to
     *            a full classpath scan
     * @return a new application context
     */
    public static BrowserlessApplicationContext create(String... viewPackages) {
        return new Builder().withViewPackages(viewPackages).build();
    }

    /**
     * Creates a plain Java application context, scanning the packages of the
     * given classes for {@code @Route}-annotated views.
     *
     * @param viewPackageClasses
     *            classes whose packages should be scanned for views
     * @return a new application context
     */
    public static BrowserlessApplicationContext create(
            Class<?>... viewPackageClasses) {
        return new Builder().withViewPackages(viewPackageClasses).build();
    }

    /**
     * Creates a plain Java application context, applying the given configurer
     * to a fresh builder before {@link Builder#build() building} it. The
     * configurer cannot be {@code null}; pass {@link UnaryOperator#identity()}
     * to accept all defaults.
     *
     * @param configurer
     *            builder configurer
     * @return a new application context
     */
    public static BrowserlessApplicationContext create(
            UnaryOperator<Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        return configurer.apply(new Builder()).build();
    }

    /**
     * Creates a credential-typed application context. The configurer must call
     * {@link Builder#withSecurityContextHandler(SecurityContextHandler)} on the
     * supplied builder so that it returns a
     * {@link SecuredBrowserlessApplicationContext.Builder} carrying the
     * required handler.
     * <p>
     * Using a separate method name rather than an overload of
     * {@link #create(UnaryOperator)} is intentional: Java overload resolution
     * cannot disambiguate two lambdas whose parameter types share the
     * {@link Function} erasure.
     *
     * @param <C>
     *            the credentials type
     * @param configurer
     *            builder configurer that installs a security handler and
     *            returns the resulting secured builder
     * @return a new credential-aware application context
     */
    public static <C> SecuredBrowserlessApplicationContext<C> createSecured(
            Function<Builder, SecuredBrowserlessApplicationContext.Builder<C>> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        return configurer.apply(new Builder()).build();
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
        private final Set<String> viewPackages = new LinkedHashSet<>();
        private final Set<String> componentTesterPackages = new LinkedHashSet<>();
        private final List<Runnable> closeHooks = new ArrayList<>();

        /**
         * Creates a builder with no pre-seeded routes. Routes are derived from
         * the view packages configured on this builder (or from a full
         * classpath scan when none are configured) at {@link #build()} time.
         */
        public Builder() {
            this(null);
        }

        Builder(Routes routes) {
            this.routes = routes;
        }

        /**
         * Adds packages to scan for {@code @Route}-annotated views. Successive
         * calls accumulate. Ignored when this builder was created with an
         * explicit {@link Routes} instance.
         *
         * @param packages
         *            package names to scan
         * @return this builder
         * @throws NullPointerException
         *             if {@code packages} or any element is {@code null}
         */
        public Builder withViewPackages(String... packages) {
            Objects.requireNonNull(packages, "packages must not be null");
            for (String pkg : packages) {
                viewPackages.add(Objects.requireNonNull(pkg,
                        "package name must not be null"));
            }
            return this;
        }

        /**
         * Adds the packages of the given classes to the set of packages to scan
         * for {@code @Route}-annotated views. Successive calls accumulate.
         * Ignored when this builder was created with an explicit {@link Routes}
         * instance.
         *
         * @param classes
         *            classes whose packages should be scanned
         * @return this builder
         * @throws NullPointerException
         *             if {@code classes} or any element is {@code null}
         */
        public Builder withViewPackages(Class<?>... classes) {
            Objects.requireNonNull(classes, "classes must not be null");
            Stream.of(classes)
                    .map(c -> Objects
                            .requireNonNull(c, "class must not be null")
                            .getPackageName())
                    .forEach(viewPackages::add);
            return this;
        }

        /**
         * Adds packages to scan for {@link ComponentTester} implementations
         * annotated with {@link Tests}. Successive calls accumulate. Each
         * package is scanned at most once per JVM (see {@link TesterRegistry}).
         *
         * @param packages
         *            package names to scan for testers
         * @return this builder
         * @throws NullPointerException
         *             if {@code packages} or any element is {@code null}
         */
        public Builder withComponentTesterPackages(String... packages) {
            Objects.requireNonNull(packages, "packages must not be null");
            for (String pkg : packages) {
                componentTesterPackages.add(Objects.requireNonNull(pkg,
                        "package name must not be null"));
            }
            return this;
        }

        /**
         * Adds the packages of the given classes to the set of packages to scan
         * for {@link ComponentTester} implementations annotated with
         * {@link Tests}. Successive calls accumulate.
         *
         * @param classes
         *            classes whose packages should be scanned for testers
         * @return this builder
         * @throws NullPointerException
         *             if {@code classes} or any element is {@code null}
         */
        public Builder withComponentTesterPackages(Class<?>... classes) {
            Objects.requireNonNull(classes, "classes must not be null");
            Stream.of(classes)
                    .map(c -> Objects
                            .requireNonNull(c, "class must not be null")
                            .getPackageName())
                    .forEach(componentTesterPackages::add);
            return this;
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
            if (!componentTesterPackages.isEmpty()) {
                TesterRegistry.registerPackages(
                        componentTesterPackages.toArray(String[]::new));
            }
            Routes resolvedRoutes = routes != null ? routes
                    : RouteDiscovery.discover(viewPackages);
            VaadinServlet servlet = servletFactory != null
                    ? servletFactory.apply(resolvedRoutes, uiFactory)
                    : new MockVaadinServlet(resolvedRoutes, uiFactory);
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
