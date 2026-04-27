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
 * Tests the {@code newUser(username, roles...)} helper in
 * {@link QuarkusBrowserlessApplicationContext} and the
 * {@link SecurityContextHandler}-level contract that {@code null} credentials
 * must produce an anonymous-equivalent identity.
 */
@QuarkusTest
@TestProfile(SecurityTestConfig.NavigationAccessControlConfig.class)
class QuarkusNewUserHelperTest {

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
    void newUser_withUsernameAndRoles_installsIdentityWithPrincipalAndRoles() {
        app.newUser("john", "USER", "ADMIN").newWindow();

        SecurityIdentity identity = CurrentIdentityAssociation.current();
        Assertions.assertEquals("john", identity.getPrincipal().getName());
        Assertions.assertTrue(identity.getRoles().contains("USER"));
        Assertions.assertTrue(identity.getRoles().contains("ADMIN"));
        Assertions.assertFalse(identity.isAnonymous());
    }

    @Test
    void anonymousUser_setsAnonymousIdentity() {
        app.newUser().newWindow();

        SecurityIdentity identity = CurrentIdentityAssociation.current();
        Assertions.assertTrue(identity.isAnonymous(),
                "Null credentials must install an anonymous-equivalent"
                        + " SecurityIdentity per SecurityContextHandler"
                        + " contract");
    }

    @Test
    void setupAuthenticationNull_installsAnonymousIdentity_withoutPriorClear() {
        var handler = new QuarkusSecurityContextHandler();

        // Pre-load a non-anonymous identity so we can detect a no-op.
        var admin = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("john"))
                .addRoles(Set.of("USER")).setAnonymous(false).build();
        CDI.current().select(CurrentIdentityAssociation.class).get()
                .setIdentity(admin);

        // The contract requires setupAuthentication(null) to produce an
        // anonymous-equivalent state on its own — without relying on a prior
        // clearContext() to have happened.
        handler.setupAuthentication(null);

        SecurityIdentity current = CurrentIdentityAssociation.current();
        Assertions.assertTrue(current.isAnonymous(),
                "setupAuthentication(null) must install an"
                        + " anonymous-equivalent identity even when called"
                        + " without a preceding clearContext()");
    }
}
