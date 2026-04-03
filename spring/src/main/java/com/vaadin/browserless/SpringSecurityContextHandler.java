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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security implementation of {@link SecurityContextHandler}.
 * <p>
 * Manages the thread-local {@link SecurityContext} via
 * {@link SecurityContextHolder} for multi-user test isolation.
 * <p>
 * The {@link #setupAuthentication(Object)} method expects an
 * {@link Authentication} instance as the credentials parameter.
 *
 * @see SecurityContextHandler
 * @see SpringBrowserlessApplicationContext
 */
public class SpringSecurityContextHandler
        implements SecurityContextHandler<Authentication> {

    @Override
    public void setupAuthentication(Authentication credentials) {
        if (credentials != null) {
            SecurityContext ctx = SecurityContextHolder.getContext();
            if (ctx == null) {
                ctx = SecurityContextHolder.createEmptyContext();
                SecurityContextHolder.setContext(ctx);
            }
            ctx.setAuthentication(credentials);
        }
    }

    @Override
    public Object saveContext() {
        SecurityContext current = SecurityContextHolder.getContext();
        SecurityContext copy = SecurityContextHolder.createEmptyContext();
        copy.setAuthentication(current.getAuthentication());
        return copy;
    }

    @Override
    public void restoreContext(Object snapshot) {
        if (snapshot instanceof SecurityContext ctx) {
            SecurityContextHolder.setContext(ctx);
        } else {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
