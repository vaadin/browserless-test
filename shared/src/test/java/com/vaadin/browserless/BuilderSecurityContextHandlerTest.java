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

import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;

/**
 * Contract tests for the security-handler-related entry points on
 * {@link BrowserlessApplicationContext} and its
 * {@link BrowserlessApplicationContext.Builder}: the {@code newUser(username,
 * roles...)} helper's failure modes and the builder's null-reset behaviour.
 */
class BuilderSecurityContextHandlerTest {

    private static Routes emptyRoutes() {
        return new Routes(new HashSet<>(), new HashSet<>(), new HashSet<>(),
                true);
    }

    @Test
    void withSecurityContextHandler_acceptsNullToReset() {
        try (var app = BrowserlessApplicationContext
                .<String> builder(emptyRoutes())
                .withSecurityContextHandler(new MinimalHandler())
                .withSecurityContextHandler(null).build()) {
            Assertions.assertNull(app.getSecurityContextHandler(),
                    "withSecurityContextHandler(null) must reset the"
                            + " previously configured handler");
        }
    }

    @Test
    void newUserByUsernameAndRoles_throwsISE_whenNoHandlerConfigured() {
        try (var app = BrowserlessApplicationContext
                .<Void> builder(emptyRoutes()).build()) {
            var ex = Assertions.assertThrows(IllegalStateException.class,
                    () -> app.newUser("foo", "BAR"));
            Assertions.assertTrue(
                    ex.getMessage().contains("SecurityContextHandler"),
                    "ISE message should mention SecurityContextHandler");
        }
    }

    @Test
    void newUserByUsernameAndRoles_throwsUOE_whenHandlerDoesNotOverrideCreateCredentials() {
        try (var app = BrowserlessApplicationContext
                .<String> builder(emptyRoutes())
                .withSecurityContextHandler(new MinimalHandler()).build()) {
            var ex = Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> app.newUser("foo", "BAR"));
            Assertions.assertTrue(ex.getMessage().contains("createCredentials"),
                    "UOE message should reference createCredentials");
        }
    }

    /**
     * Bare-bones handler that does not override
     * {@link SecurityContextHandler#createCredentials(String, String...)}, so
     * the default UOE-throwing implementation kicks in.
     */
    private static class MinimalHandler
            implements SecurityContextHandler<String> {
        @Override
        public void setupAuthentication(String credentials) {
        }

        @Override
        public Object saveContext() {
            return null;
        }

        @Override
        public void restoreContext(Object snapshot) {
        }

        @Override
        public void clearContext() {
        }
    }
}
