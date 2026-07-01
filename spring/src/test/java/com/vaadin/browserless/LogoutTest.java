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

import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

/**
 * Reproduces issue #115: invoking Spring Security's
 * {@link SecurityContextLogoutHandler} from within a browserless test must not
 * throw, mirroring how it behaves inside a real servlet container.
 * <p>
 * Also covers issue #127: a logout in one window must leave new windows of the
 * same user unauthenticated, rather than restoring a stale authenticated
 * security snapshot.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityTestConfig.NavigationAccessControlConfig.class)
class LogoutTest {

    @Autowired
    private ApplicationContext applicationContext;

    private SecuredBrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void setUp() {
        app = SpringBrowserlessApplicationContext
                .createSecured(applicationContext, "com.testapp.security");
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void securityContextLogoutHandler_doesNotThrow() {
        var user = app.newUser("john", "USER");
        var window = user.newWindow();
        window.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                window.getCurrentView());

        // This is the exact logout sequence from issue #115. The handler
        // invalidates the HttpSession and then asks the
        // HttpSessionSecurityContextRepository to store the (now empty)
        // context.
        // In a real servlet container request.getSession(false) returns null
        // after invalidation, so saving the empty context is a no-op. The mock
        // request must honour the same contract instead of returning the stale,
        // invalidated session (which would throw IllegalStateException).
        Assertions.assertDoesNotThrow(LogoutTest::logout);
    }

    @Test
    void afterLogout_sameWindow_redirectsToLogin() {
        var user = app.newUser("john", "USER");
        var window = user.newWindow();
        window.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                window.getCurrentView());

        logout();

        // Logging out clears the authentication, so the now-anonymous user is
        // redirected to the login view when trying to reach the protected view
        // again in the same window.
        Assertions.assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Authentication should be cleared after logout");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> window.navigate(ProtectedView.class));
        Assertions.assertInstanceOf(LoginView.class, window.getCurrentView());
    }

    @Test
    void afterLogout_newWindow_isAlsoUnauthenticated() {
        var user = app.newUser("john", "USER");
        var window = user.newWindow();
        window.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                window.getCurrentView());

        // Clear the authentication by *replacing* the security context, the
        // way SecurityContextHolder.clearContext() (and a fresh per-request
        // context load in a real container) does. This is deliberately not the
        // SecurityContextLogoutHandler used by the other tests: that handler
        // clears auth with an in-place context.setAuthentication(null), which
        // mutates the very object the per-user snapshot aliases and so happens
        // to clear the snapshot too, masking this bug. A replacing clear leaves
        // the saved snapshot authenticated and exposes #127.
        SecurityContextHolder.clearContext();

        // Opening another window for the *same* user after logout must not
        // restore the stale authenticated snapshot. The new window must
        // observe the logged-out state, mirroring a real application where
        // logging out in one tab logs the user out of all tabs, existing and
        // new (issue #127).
        var newWindow = user.newWindow();
        Assertions.assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Authentication should remain cleared in a window opened after"
                        + " logout");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> newWindow.navigate(ProtectedView.class));
        Assertions.assertInstanceOf(LoginView.class,
                newWindow.getCurrentView());
    }

    @Test
    void afterLogout_switchUserAndBack_newWindowStillUnauthenticated() {
        var user = app.newUser("john", "USER");
        var window = user.newWindow();
        window.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                window.getCurrentView());

        logout();

        // Switch to a different user. The cross-user switch captures the
        // outgoing (logged-out) user's live state into its snapshot.
        var other = app.newUser("jane", "USER");
        var otherWindow = other.newWindow();
        otherWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                otherWindow.getCurrentView());

        // Switch back to the logged-out user via a new window: it must still
        // be unauthenticated. This guards that the same-user no-clobber rule
        // does not suppress a legitimate cross-user restore (issue #127).
        var newWindow = user.newWindow();
        Assertions.assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Original user must remain logged out after a round-trip"
                        + " through another user");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> newWindow.navigate(ProtectedView.class));
        Assertions.assertInstanceOf(LoginView.class,
                newWindow.getCurrentView());
    }

    @Test
    void afterLogout_newUserCanLogInAgain() {
        var user = app.newUser("john", "USER");
        var window = user.newWindow();
        window.navigate(ProtectedView.class);
        logout();

        // A fresh authenticated user (a new login) gets its own session and
        // reaches the protected view, proving the invalidated-session teardown
        // does not leak into subsequent logins.
        var newUser = app.newUser("jane", "USER");
        var newWindow = newUser.newWindow();
        newWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                newWindow.getCurrentView());
    }

    /**
     * Performs the issue #115 logout sequence against the currently active
     * window's request/response.
     */
    private static void logout() {
        new SecurityContextLogoutHandler().logout(
                VaadinServletRequest.getCurrent().getHttpServletRequest(),
                VaadinServletResponse.getCurrent().getHttpServletResponse(),
                null);
    }
}
