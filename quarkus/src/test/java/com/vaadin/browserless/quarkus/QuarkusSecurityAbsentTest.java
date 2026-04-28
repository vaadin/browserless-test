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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.MockRequestCustomizer;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that {@link QuarkusBrowserlessApplicationContext} bootstraps cleanly
 * when {@code quarkus-security} is not on the test app's classpath. The
 * detector is overridden to simulate the absent classpath without actually
 * changing the Maven-resolved dependencies.
 */
@QuarkusTest
class QuarkusSecurityAbsentTest {

    @AfterEach
    void tearDown() {
        QuarkusSecuritySupport.overrideDetector(null);
    }

    @Test
    void newUser_withoutQuarkusSecurity_doesNotThrow() {
        QuarkusSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        try (var app = QuarkusBrowserlessApplicationContext.create(routes)) {
            Assertions.assertDoesNotThrow(() -> {
                var user = app.newUser();
                user.newWindow();
            });
        }
    }

    @Test
    void create_withoutQuarkusSecurity_doesNotInstallRequestCustomizerLookup() {
        QuarkusSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        try (var app = QuarkusBrowserlessApplicationContext.create(routes)) {
            // Trigger Lookup initialization via a window, then probe the
            // current VaadinService for the registered MockRequestCustomizer.
            app.newUser().newWindow();
            Lookup lookup = VaadinService.getCurrent().getContext()
                    .getAttribute(Lookup.class);
            MockRequestCustomizer customizer = lookup
                    .lookup(MockRequestCustomizer.class);
            Assertions.assertFalse(
                    customizer instanceof QuarkusSecurityCustomizer,
                    "QuarkusSecurityCustomizer must not be registered as a"
                            + " lookup service when Quarkus Security is"
                            + " absent");
        }
    }

    @Test
    void create_withQuarkusSecurity_installsRequestCustomizerLookup() {
        // Default detector: actual classpath probe (quarkus-security IS
        // present in the quarkus module's test classpath).
        Routes routes = new Routes();
        try (var app = QuarkusBrowserlessApplicationContext.create(routes)) {
            app.newUser().newWindow();
            Lookup lookup = VaadinService.getCurrent().getContext()
                    .getAttribute(Lookup.class);
            Assertions.assertInstanceOf(QuarkusSecurityCustomizer.class,
                    lookup.lookup(MockRequestCustomizer.class),
                    "QuarkusSecurityCustomizer must be registered as a lookup"
                            + " service when Quarkus Security is present");
        }
    }
}
