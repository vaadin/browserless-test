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

/**
 * Abstracts per-user security context management for multi-user testing.
 * <p>
 * Framework modules (Spring, Quarkus) provide implementations that bridge
 * their security infrastructure (e.g. Spring's {@code SecurityContextHolder},
 * Quarkus's {@code CurrentIdentityAssociation}) with the browserless
 * multi-user context hierarchy.
 * <p>
 * Implementations must be thread-safe with respect to the thread-local
 * security state they manage.
 *
 * @see BrowserlessApplicationContext.Builder#withSecurityContextHandler(SecurityContextHandler)
 */
public interface SecurityContextHandler<C> {

    /**
     * Sets up authentication for a new user from the given credentials.
     * <p>
     * The type parameter {@code C} is determined by the framework
     * implementation. For example, Spring uses
     * {@code org.springframework.security.core.Authentication} and Quarkus
     * uses {@code io.quarkus.security.identity.SecurityIdentity}.
     *
     * @param credentials framework-specific credentials object
     */
    void setupAuthentication(C credentials);

    /**
     * Captures the current thread's security context as an opaque snapshot.
     * <p>
     * Called automatically when switching away from a user context to
     * preserve its security state.
     *
     * @return an opaque snapshot of the current security context, or
     *         {@code null} if no security context is active
     */
    Object saveContext();

    /**
     * Restores a previously saved security context snapshot onto the
     * current thread.
     *
     * @param snapshot a snapshot previously returned by {@link #saveContext()},
     *                 or {@code null} to clear the context
     */
    void restoreContext(Object snapshot);

    /**
     * Clears the security context from the current thread.
     */
    void clearContext();
}
