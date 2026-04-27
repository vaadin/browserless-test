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
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Spring Security implementation of {@link SecurityContextHandler}.
 * <p>
 * Manages the thread-local {@link SecurityContext} via
 * {@link SecurityContextHolder} for multi-user test isolation.
 * <p>
 * The {@link #setupAuthentication(Object)} method expects an
 * {@link Authentication} instance as the credentials parameter, or {@code null}
 * for an anonymous user — in which case an {@link AnonymousAuthenticationToken}
 * is installed (mirroring the behaviour of {@code @WithAnonymousUser}).
 *
 * @see SecurityContextHandler
 * @see SpringBrowserlessApplicationContext
 */
public class SpringSecurityContextHandler
        implements SecurityContextHandler<Authentication> {

    @Override
    public void setupAuthentication(Authentication credentials) {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx == null) {
            ctx = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(ctx);
        }
        ctx.setAuthentication(
                credentials != null ? credentials : anonymousAuthentication());
    }

    private static AnonymousAuthenticationToken anonymousAuthentication() {
        return new AnonymousAuthenticationToken("browserless-anonymous-key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    @Override
    public SecurityContext saveContext() {
        SecurityContext current = SecurityContextHolder.getContext();
        SecurityContext copy = SecurityContextHolder.createEmptyContext();
        copy.setAuthentication(current.getAuthentication());
        return copy;
    }

    @Override
    public void restoreContext(Object snapshot) {
        if (snapshot == null) {
            SecurityContextHolder.clearContext();
        } else if (snapshot instanceof SecurityContext ctx) {
            SecurityContextHolder.setContext(ctx);
        } else {
            throw new IllegalArgumentException(
                    "Expected a SecurityContext snapshot, got "
                            + snapshot.getClass().getName());
        }
    }

    @Override
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds an {@link Authentication} for the given username and roles, in the
     * same shape produced by Spring Security's {@code @WithMockUser}: a
     * {@link UsernamePasswordAuthenticationToken} carrying a {@link User}
     * principal whose authorities are the given roles, prefixed with
     * {@code ROLE_} when not already prefixed.
     */
    @Override
    public Authentication createCredentials(String username, String... roles) {
        List<GrantedAuthority> authorities = new ArrayList<>(roles.length);
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(
                    role.startsWith("ROLE_") ? role : "ROLE_" + role));
        }
        User principal = new User(username, "", authorities);
        return UsernamePasswordAuthenticationToken.authenticated(principal,
                principal.getPassword(), principal.getAuthorities());
    }
}
