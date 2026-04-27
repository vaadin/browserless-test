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
package com.vaadin.browserless.quarkus;

import java.util.Set;

import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.internal.Routes;

/**
 * Tests multi-user security context isolation with Quarkus Security. Verifies
 * that switching between users' windows correctly saves and restores the
 * Quarkus {@link SecurityIdentity}.
 */
@QuarkusTest
@TestProfile(SecurityTestConfig.NavigationAccessControlConfig.class)
class MultiUserSecurityTest {

    private BrowserlessApplicationContext<SecurityIdentity> app;

    @BeforeEach
    void setUp() {
        Routes routes = new Routes().autoDiscoverViews("com.testapp.security");
        app = QuarkusBrowserlessApplicationContext.create(routes);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void authenticatedUser_seesProtectedView() {
        var adminIdentity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("john"))
                .addRoles(Set.of("USER")).setAnonymous(false).build();

        var admin = app.newUser(adminIdentity);
        var adminWindow = admin.newWindow();

        // Authenticated user sees the protected view
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView());
    }

    @Test
    void anonymousUser_redirectedToLogin() {
        var anon = app.newUser();
        var anonWindow = anon.newWindow();

        // Anonymous user is redirected to login
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> anonWindow.navigate(ProtectedView.class));
        Assertions.assertInstanceOf(LoginView.class,
                anonWindow.getCurrentView());
    }

    @Test
    void newUser_byUsernameAndRoles_authenticatesUser() {
        var window = app.newUser("john", "USER").newWindow();

        // Helper-built identity grants access to a @PermitAll view
        window.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                window.getCurrentView());
    }

    @Test
    void multipleUsers_securityContextIsolated() {
        var adminIdentity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("john"))
                .addRoles(Set.of("USER")).setAnonymous(false).build();

        var admin = app.newUser(adminIdentity);
        var adminWindow = admin.newWindow();

        var anon = app.newUser();
        var anonWindow = anon.newWindow();

        // Admin sees protected view
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView());

        // Anonymous user is redirected to login
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> anonWindow.navigate(ProtectedView.class));
        Assertions.assertInstanceOf(LoginView.class,
                anonWindow.getCurrentView());

        // Switch back to admin — security context should be restored
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView(),
                "Admin's security context should be restored after switching back");
    }
}
