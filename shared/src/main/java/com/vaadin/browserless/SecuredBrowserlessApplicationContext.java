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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.experimental.Feature;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Credential-aware application context.
 * <p>
 * Extends {@link BrowserlessApplicationContext} with a configured, non-null
 * {@link SecurityContextHandler} and credential-typed {@code newUser(...)}
 * overloads for installing per-user security state. Build instances via a
 * {@link BrowserlessApplicationContext.Builder} configured with
 * {@link BrowserlessApplicationContext.Builder#withSecurityContextHandler(SecurityContextHandler)},
 * which transitions to {@link Builder} and produces this typed context.
 * <p>
 * All thread-affinity and lifecycle guarantees of the base
 * {@link BrowserlessApplicationContext} apply unchanged.
 *
 * @param <C>
 *            the credentials type accepted by {@link #newUser(Object)}, as
 *            defined by the configured {@link SecurityContextHandler}
 * @see BrowserlessApplicationContext
 * @see SecurityContextHandler
 * @since 1.1
 */
public class SecuredBrowserlessApplicationContext<C>
        extends BrowserlessApplicationContext {

    private final SecurityContextHandler<C> handler;

    SecuredBrowserlessApplicationContext(VaadinServletService service,
            UIFactory uiFactory, SecurityContextHandler<C> handler,
            List<Runnable> closeHooks) {
        super(service, uiFactory, closeHooks);
        this.handler = handler;
    }

    /**
     * Creates a new user context with the given credentials.
     * <p>
     * The security context for this user is first cleared, then the credentials
     * are passed to {@link SecurityContextHandler#setupAuthentication(Object)
     * SecurityContextHandler.setupAuthentication()} — including when
     * {@code credentials} is {@code null}, so that the handler can install its
     * anonymous-equivalent state. The resulting security state is captured as
     * this user's initial snapshot and is automatically restored whenever one
     * of this user's windows is activated.
     *
     * @param credentials
     *            framework-specific credentials, or {@code null} for an
     *            anonymous user
     * @return the new user context
     * @throws IllegalStateException
     *             if this context has been closed
     */
    public BrowserlessUserContext newUser(C credentials) {
        return newUserInternal(credentials);
    }

    /**
     * Creates a new user context for the given username and roles.
     * <p>
     * Delegates to
     * {@link SecurityContextHandler#createCredentials(String, String...)} on
     * the configured handler, then to {@link #newUser(Object)}. Spring and
     * Quarkus handlers ship with overrides that mirror the conventions of
     * {@code @WithMockUser} / Quarkus security identity construction; custom
     * handlers must override {@code createCredentials} to opt into this helper.
     *
     * @param username
     *            the username
     * @param roles
     *            the roles for the user; may be empty
     * @return the new user context
     * @throws IllegalStateException
     *             if this context has been closed
     * @throws UnsupportedOperationException
     *             if the configured handler doesn't override
     *             {@link SecurityContextHandler#createCredentials(String, String...)}
     */
    public BrowserlessUserContext newUser(String username, String... roles) {
        checkNotClosed();
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        return newUser(handler.createCredentials(username, roles));
    }

    @Override
    SecurityContextHandler<C> getSecurityContextHandler() {
        return handler;
    }

    /**
     * Builder for {@link SecuredBrowserlessApplicationContext}. Obtained from
     * {@link BrowserlessApplicationContext.Builder#withSecurityContextHandler(SecurityContextHandler)};
     * delegates to the underlying base builder for shared configuration.
     *
     * @param <C>
     *            the credentials type
     */
    public static final class Builder<C> {

        private final BrowserlessApplicationContext.Builder base;
        private final SecurityContextHandler<C> handler;

        Builder(BrowserlessApplicationContext.Builder base,
                SecurityContextHandler<C> handler) {
            this.base = base;
            this.handler = handler;
        }

        /**
         * Replaces the security context handler, possibly switching its
         * credentials type.
         *
         * @param <D>
         *            the new credentials type
         * @param handler
         *            the handler; must not be {@code null}
         * @return a builder configured with the given handler
         * @throws NullPointerException
         *             if {@code handler} is {@code null}
         */
        public <D> Builder<D> withSecurityContextHandler(
                SecurityContextHandler<D> handler) {
            Objects.requireNonNull(handler, "handler must not be null");
            return new Builder<>(base, handler);
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withServletFactory
         */
        public Builder<C> withServletFactory(
                BiFunction<Routes, UIFactory, VaadinServlet> factory) {
            base.withServletFactory(factory);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withUIFactory
         */
        public Builder<C> withUIFactory(UIFactory uiFactory) {
            base.withUIFactory(uiFactory);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withLookupServices
         */
        public Builder<C> withLookupServices(Class<?>... services) {
            base.withLookupServices(services);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withViewPackages(String...)
         */
        public Builder<C> withViewPackages(String... packages) {
            base.withViewPackages(packages);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withViewPackages(Class[])
         */
        public Builder<C> withViewPackages(Class<?>... classes) {
            base.withViewPackages(classes);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withComponentTesterPackages(String...)
         */
        public Builder<C> withComponentTesterPackages(String... packages) {
            base.withComponentTesterPackages(packages);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withComponentTesterPackages(Class[])
         */
        public Builder<C> withComponentTesterPackages(Class<?>... classes) {
            base.withComponentTesterPackages(classes);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withCloseHook
         */
        public Builder<C> withCloseHook(Runnable hook) {
            base.withCloseHook(hook);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withApplicationProperty
         */
        public Builder<C> withApplicationProperty(String name, String value) {
            base.withApplicationProperty(name, value);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withApplicationProperties
         */
        public Builder<C> withApplicationProperties(
                Map<String, String> properties) {
            base.withApplicationProperties(properties);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withFeatureFlags(String...)
         */
        public Builder<C> withFeatureFlags(String... featureIds) {
            base.withFeatureFlags(featureIds);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withFeatureFlags(Feature...)
         */
        public Builder<C> withFeatureFlags(Feature... features) {
            base.withFeatureFlags(features);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withFeatureFlag(String,
         *      boolean)
         */
        public Builder<C> withFeatureFlag(String featureId, boolean enabled) {
            base.withFeatureFlag(featureId, enabled);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withFeatureFlag(Feature,
         *      boolean)
         */
        public Builder<C> withFeatureFlag(Feature feature, boolean enabled) {
            base.withFeatureFlag(feature, enabled);
            return this;
        }

        /**
         * @see BrowserlessApplicationContext.Builder#withConfiguration
         */
        public Builder<C> withConfiguration(
                BrowserlessConfiguration configuration) {
            base.withConfiguration(configuration);
            return this;
        }

        /**
         * Builds the secured application context.
         *
         * @return a new secured application context
         */
        public SecuredBrowserlessApplicationContext<C> build() {
            SecuredBrowserlessApplicationContext<C> context = new SecuredBrowserlessApplicationContext<>(
                    base.buildService(), base.uiFactory(), handler,
                    base.buildCloseHooks());
            context.setRequestContextHandler(base.requestContextHandler());
            return context;
        }
    }
}
