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

import com.vaadin.flow.server.VaadinRequest;

/**
 * Abstracts per-user request-context management for multi-user testing.
 * <p>
 * Some frameworks resolve session-scoped beans against a thread-local request
 * rather than against the {@code VaadinSession}. Spring's {@code @SessionScope}
 * beans, for example, are resolved via {@code RequestContextHolder}, which is
 * bound to a single request for the whole test thread. Without per-user
 * binding, every browserless user resolves those beans against the same
 * request, so the second user reuses the first user's session-scoped instances
 * — see
 * <a href="https://github.com/vaadin/browserless-test/issues/110">#110</a>.
 * <p>
 * Implementations bridge such a framework's request-context thread-local with
 * the browserless multi-user context hierarchy: {@link #bind(VaadinRequest)} is
 * invoked whenever a user's session thread-locals are applied (user creation,
 * window activation, close), so the active user's own request — and therefore
 * its own session — backs the framework's scope resolution.
 * <p>
 * This handler is wired automatically by the framework integration (e.g.
 * {@code SpringBrowserlessApplicationContext}); it is not a user-facing
 * configuration option. Implementations must be thread-safe with respect to the
 * thread-local state they manage.
 *
 * @see SecurityContextHandler
 * @since 1.1
 */
interface RequestContextHandler {

    /**
     * Captures the current thread's request-context binding as an opaque
     * snapshot, so it can be restored after a temporary binding (e.g. while a
     * user context is being constructed on a thread that already carries
     * another user's binding).
     *
     * @return an opaque snapshot, or {@code null} if nothing is bound
     */
    Object saveContext();

    /**
     * Restores a previously captured snapshot onto the current thread.
     *
     * @param snapshot
     *            a snapshot previously returned by {@link #saveContext()}, or
     *            {@code null} to clear the binding
     */
    void restoreContext(Object snapshot);

    /**
     * Binds the given user's request as the active request context on the
     * current thread, so framework scopes resolve against this user's session.
     *
     * @param request
     *            the user's request; never {@code null}
     */
    void bind(VaadinRequest request);

    /**
     * Clears the request-context binding from the current thread.
     */
    void clearContext();
}
