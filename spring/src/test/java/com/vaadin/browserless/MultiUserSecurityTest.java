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

import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.browserless.internal.Routes;

/**
 * Tests multi-user security context isolation with Spring Security.
 * Verifies that switching between users' windows correctly saves and
 * restores Spring SecurityContext.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityTestConfig.NavigationAccessControlConfig.class)
class MultiUserSecurityTest {

    @Autowired
    private ApplicationContext applicationContext;

    private BrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void setUp() {
        Routes routes = new Routes().autoDiscoverViews(
                "com.testapp.security");
        app = SpringBrowserlessApplicationContext.create(routes,
                applicationContext);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void authenticatedUser_seesProtectedView() {
        var adminAuth = new UsernamePasswordAuthenticationToken("john",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        var admin = app.newUser(adminAuth);
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
    void multipleUsers_securityContextIsolated() {
        var adminAuth = new UsernamePasswordAuthenticationToken("john",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        var admin = app.newUser(adminAuth);
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
