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

import java.security.Principal;
import java.util.List;

import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.server.VaadinRequest;

/**
 * Tests the {@code newUser(username, roles...)} helper in
 * {@link SpringBrowserlessApplicationContext}. The helper builds an
 * {@link Authentication} via {@link SpringSecurityContextHandler}'s
 * {@code createCredentials} override, mirroring the conventions of
 * {@code @WithMockUser}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityTestConfig.NavigationAccessControlConfig.class)
class SpringNewUserHelperTest {

    @Autowired
    private ApplicationContext applicationContext;

    private BrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void setUp() {
        Routes routes = new Routes().autoDiscoverViews("com.testapp.security");
        app = SpringBrowserlessApplicationContext.create(routes,
                applicationContext);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void newUser_withUsernameAndRoles_authenticatesUserOnRequest() {
        var window = app.newUser("john", "DEV", "PO").newWindow();

        Principal principal = VaadinRequest.getCurrent().getUserPrincipal();
        Assertions.assertNotNull(principal,
                "Principal should be exposed on the request");
        Assertions.assertEquals("john", principal.getName());

        Assertions.assertTrue(VaadinRequest.getCurrent().isUserInRole("DEV"));
        Assertions.assertTrue(VaadinRequest.getCurrent().isUserInRole("PO"));
        Assertions.assertFalse(VaadinRequest.getCurrent().isUserInRole("CEO"));

        // Helper still drives Vaadin's @PermitAll access control
        window.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                window.getCurrentView());
    }

    @Test
    void newUser_rolesAreNormalisedWithRolePrefix() {
        app.newUser("john", "DEV").newWindow();

        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        Assertions.assertNotNull(auth, "Authentication should be set");
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        Assertions.assertEquals(List.of("ROLE_DEV"), authorities,
                "Roles passed without the ROLE_ prefix should be prefixed");
    }

    @Test
    void newUser_rolesAlreadyPrefixedAreNotDoublePrefixed() {
        app.newUser("john", "ROLE_DEV").newWindow();

        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        Assertions.assertEquals(List.of("ROLE_DEV"), authorities,
                "Roles already prefixed must not be double-prefixed");
    }

    @Test
    void anonymousUser_setsAnonymousAuthenticationToken() {
        app.newUser().newWindow();

        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        Assertions.assertInstanceOf(AnonymousAuthenticationToken.class, auth,
                "Null credentials must install an AnonymousAuthenticationToken,"
                        + " mirroring @WithAnonymousUser");
    }

    @Test
    void anonymousUser_principalIsNotExposedOnRequest() {
        var window = app.newUser().newWindow();

        Principal principal = VaadinRequest.getCurrent().getUserPrincipal();
        Assertions.assertNull(principal,
                "Anonymous tokens must not be exposed as a request principal,"
                        + " matching production Spring Security behaviour");

        // And anonymous users still hit the login redirect
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> window.navigate(ProtectedView.class));
        Assertions.assertInstanceOf(LoginView.class, window.getCurrentView());
    }
}
