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

import jakarta.enterprise.inject.spi.CDI;

import java.util.Set;

import com.testapp.security.ProtectedView;
import io.quarkus.security.identity.CurrentIdentityAssociation;
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
 * Tests that the Quarkus security identity snapshot is a defensive copy, not a
 * mutable reference. Mutating the live {@link CurrentIdentityAssociation} on
 * the thread after switching users must not corrupt the saved snapshot.
 */
@QuarkusTest
@TestProfile(SecurityTestConfig.NavigationAccessControlConfig.class)
class QuarkusSecuritySnapshotTest {

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
    void mutatingLiveContext_doesNotCorruptSavedSnapshot() {
        var adminIdentity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("john"))
                .addRoles(Set.of("USER")).setAnonymous(false).build();

        // Create admin user and verify access
        var admin = app.newUser(adminIdentity);
        var adminWindow = admin.newWindow();
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView());

        // Switch to anonymous user (saves admin's snapshot)
        var anon = app.newUser();
        var anonWindow = anon.newWindow();

        // Mutate the live identity on the thread
        CDI.current().select(CurrentIdentityAssociation.class).get()
                .setIdentity(QuarkusSecurityIdentity.builder()
                        .setAnonymous(true).build());

        // Switch back to admin — snapshot should NOT be corrupted
        adminWindow.navigate(ProtectedView.class);
        Assertions.assertInstanceOf(ProtectedView.class,
                adminWindow.getCurrentView(),
                "Admin snapshot should survive mutation of the live"
                        + " CurrentIdentityAssociation");

        // Assert identity contents directly, so the test doesn't pass merely
        // because of an unrelated re-authentication path
        SecurityIdentity restored = CurrentIdentityAssociation.current();
        Assertions.assertEquals("john", restored.getPrincipal().getName(),
                "Restored identity should have admin's principal");
        Assertions.assertTrue(restored.getRoles().contains("USER"),
                "Restored identity should have admin's USER role");
        Assertions.assertFalse(restored.isAnonymous(),
                "Restored identity should not be anonymous");
    }
}
