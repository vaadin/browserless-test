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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;

import com.vaadin.browserless.internal.Routes;

@SpringBootTest
@ContextConfiguration(classes = SecurityTestConfig.NavigationAccessControlConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiUserSecurityTest {

    @Autowired
    ApplicationContext springCtx;

    VaadinTestApplicationContext app;

    @BeforeAll
    void setup() {
        // Empty routes — security views are registered dynamically
        // by the VaadinServiceInitListener in SecurityTestConfig
        app = VaadinTestApplicationContext.forSpring(new Routes(), springCtx);
    }

    @AfterAll
    void tearDown() {
        app.close();
    }

    @Test
    void authenticatedUser_seesProtectedView_anonymousUser_seesLogin() {
        VaadinTestUserContext loggedInUser = app.newUser();
        VaadinTestUserContext anonymousUser = app.newUser();

        // Simulate login for the first user before opening a window,
        // so the security context is in place when the UI initializes
        loggedInUser.setAuthentication(
                new UsernamePasswordAuthenticationToken("john", "secret",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        VaadinTestUiContext loggedInWindow = loggedInUser.newWindow();
        VaadinTestUiContext anonymousWindow = anonymousUser.newWindow();

        // Logged-in user navigates to the protected root view
        loggedInWindow.navigate("", ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                loggedInWindow.getCurrentView(),
                "Authenticated user should see the protected view");

        // Anonymous user tries the same — should be redirected to login
        anonymousWindow.navigate("", LoginView.class);
        Assertions.assertInstanceOf(LoginView.class,
                anonymousWindow.getCurrentView(),
                "Anonymous user should be redirected to the login view");

        // Switch back to logged-in user — security context must be preserved
        loggedInWindow.navigate("", ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                loggedInWindow.getCurrentView(),
                "Authenticated user should still see the protected view "
                        + "after switching back");
    }
}
