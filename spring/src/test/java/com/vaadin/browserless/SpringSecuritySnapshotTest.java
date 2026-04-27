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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.browserless.internal.Routes;

/**
 * Tests that the Spring security context snapshot is a defensive copy, not a
 * mutable reference. Mutating the live SecurityContext on the thread after
 * switching users must not corrupt the saved snapshot.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityTestConfig.NavigationAccessControlConfig.class)
class SpringSecuritySnapshotTest {

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
    void mutationOnActiveUser_isCapturedAndRestoredOnSwitchBack() {
        var admin = app.newUser("john", "USER");
        var adminWindow = admin.newWindow();

        // Sanity: admin's auth is on the thread.
        Assertions.assertEquals("john", SecurityContextHolder.getContext()
                .getAuthentication().getName());

        // Mutate the live SecurityContext *while admin is still active* —
        // simulating a test that programmatically changes auth mid-flight
        // (e.g. logging out the current user).
        SecurityContextHolder.getContext().setAuthentication(null);

        // Switching to another user captures the outgoing (admin's) live
        // state — including the mutation — into admin's snapshot.
        var bob = app.newUser("bob", "USER");
        bob.newWindow();

        // Re-activating admin must restore the *mutated* state, not the
        // creation-time auth: mutations on the active user are captured at
        // the user-switch point, per the BrowserlessUserContext contract.
        adminWindow.activate();
        Assertions.assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Mutations applied while admin was active should survive a"
                        + " round-trip through bob — captured into admin's"
                        + " snapshot on the user-switch and restored on"
                        + " re-activation.");
    }

    @Test
    void mutatingLiveContext_doesNotCorruptSavedSnapshot() {
        // Create admin user and verify access
        var admin = app.newUser("john", "USER");
        var adminWindow = admin.newWindow();
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView());

        // Switch to anonymous user (saves admin's snapshot)
        var anon = app.newUser();
        var anonWindow = anon.newWindow();

        // Mutate the live SecurityContext on the thread
        SecurityContextHolder.getContext().setAuthentication(null);

        // Switch back to admin — snapshot should NOT be corrupted
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView(),
                "Admin snapshot should survive mutation of the live"
                        + " SecurityContext");
    }
}
