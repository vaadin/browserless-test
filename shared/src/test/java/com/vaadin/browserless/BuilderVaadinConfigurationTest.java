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

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the custom Vaadin configuration declared on the application
 * context builder reaches the mocked Vaadin environment.
 */
class BuilderVaadinConfigurationTest {

    private static final Feature FEATURE = FeatureFlags.COLLABORATION_ENGINE_BACKEND;

    @Test
    void applicationProperties_areVisibleInDeploymentConfiguration() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes()
                        .withApplicationProperty("custom.property", "value")
                        .withApplicationProperties(
                                Map.of("another.property", "another")))) {
            DeploymentConfiguration configuration = app.getService()
                    .getDeploymentConfiguration();

            assertEquals("value",
                    configuration.getStringProperty("custom.property", null));
            assertEquals("another",
                    configuration.getStringProperty("another.property", null));
        }
    }

    @Test
    void featureFlags_areAppliedToTheApplication() {
        try (var app = BrowserlessApplicationContext.create(
                b -> b.withoutRoutes().withFeatureFlags(FEATURE.getId()))) {
            assertTrue(featureFlags(app).isEnabled(FEATURE),
                    "Feature flag enabled on the builder should be enabled");
        }
    }

    @Test
    void featureFlags_areVisibleWhileTheServiceIsInitialized() {
        AtomicBoolean enabledDuringServiceInit = new AtomicBoolean();
        try (var app = BrowserlessApplicationContext.create(b -> b
                .withoutRoutes().withFeatureFlags(FEATURE)
                .withServletFactory((routes,
                        uiFactory) -> new MockVaadinServlet(routes, uiFactory) {
                            @Override
                            protected VaadinServletService createServletService(
                                    DeploymentConfiguration deploymentConfiguration) {
                                enabledDuringServiceInit.set(FeatureFlags
                                        .get(new VaadinServletContext(
                                                getServletContext()))
                                        .isEnabled(FEATURE));
                                return super.createServletService(
                                        deploymentConfiguration);
                            }
                        }))) {
            assertTrue(enabledDuringServiceInit.get(),
                    "Feature flag should already be enabled when the Vaadin service is created");
        }
    }

    @Test
    void featureFlags_doNotLeakToOtherApplications() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes().withFeatureFlags(FEATURE))) {
            assertTrue(featureFlags(app).isEnabled(FEATURE));
        }
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes())) {
            assertFalse(featureFlags(app).isEnabled(FEATURE),
                    "Feature flag should not leak into an application that does not enable it");
        }
    }

    @Test
    void featureFlags_canBeToggledWithoutStoringThemOnDisk() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes())) {
            File propertiesFile = new File(
                    ApplicationConfiguration.get(app.getService().getContext())
                            .getJavaResourceFolder(),
                    FeatureFlags.PROPERTIES_FILENAME);
            boolean existedBefore = propertiesFile.exists();

            FeatureFlags featureFlags = featureFlags(app);
            featureFlags.setEnabled(FEATURE.getId(), true);

            assertTrue(featureFlags.isEnabled(FEATURE));
            assertEquals(existedBefore, propertiesFile.exists(),
                    "Toggling a feature flag in a test should not write "
                            + propertiesFile + " into the project");
        }
    }

    @Test
    void featureFlags_toggledInATest_doNotLeakToOtherApplications() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes())) {
            featureFlags(app).setEnabled(FEATURE.getId(), true);
        }
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes())) {
            assertFalse(featureFlags(app).isEnabled(FEATURE));
        }
    }

    @Test
    void featureFlags_survivePropertiesReload() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes().withFeatureFlags(FEATURE))) {
            FeatureFlags featureFlags = featureFlags(app);
            featureFlags.loadProperties();

            assertTrue(featureFlags.isEnabled(FEATURE),
                    "Overrides should be re-applied when feature flags are reloaded");
        }
    }

    @Test
    void featureFlags_toggledAtRuntime_survivePropertiesReload() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withoutRoutes())) {
            FeatureFlags featureFlags = featureFlags(app);
            featureFlags.setEnabled(FEATURE.getId(), true);
            featureFlags.loadProperties();

            assertTrue(featureFlags.isEnabled(FEATURE),
                    "A feature flag toggled by the test should be re-applied "
                            + "when feature flags are reloaded");
        }
    }

    @Test
    void alreadyInitializedServlet_rejectsConfigurationItCannotApply() {
        VaadinServlet[] shared = new VaadinServlet[1];
        BiFunction<Routes, UIFactory, VaadinServlet> sharedServletFactory = (
                routes, uiFactory) -> {
            if (shared[0] == null) {
                shared[0] = new MockVaadinServlet(routes, uiFactory);
            }
            return shared[0];
        };
        try (var app = BrowserlessApplicationContext.create(b -> b
                .withoutRoutes().withServletFactory(sharedServletFactory))) {
            assertNotNull(app.getService());
        }

        // The servlet is already initialized now, so the configuration below
        // can no longer be applied and must not be silently dropped.
        BrowserlessTestSetupException exception = assertThrows(
                BrowserlessTestSetupException.class,
                () -> BrowserlessApplicationContext.create(b -> b
                        .withoutRoutes()
                        .withServletFactory(sharedServletFactory)
                        .withApplicationProperty("late.property", "value")));

        assertTrue(exception.getMessage().contains("late.property"),
                "The error should report the discarded configuration, but was: "
                        + exception.getMessage());
    }

    @Test
    void alreadyInitializedServlet_withoutConfiguration_isReused() {
        VaadinServlet[] shared = new VaadinServlet[1];
        BiFunction<Routes, UIFactory, VaadinServlet> sharedServletFactory = (
                routes, uiFactory) -> {
            if (shared[0] == null) {
                shared[0] = new MockVaadinServlet(routes, uiFactory);
            }
            return shared[0];
        };
        try (var app = BrowserlessApplicationContext.create(b -> b
                .withoutRoutes().withServletFactory(sharedServletFactory))) {
            assertNotNull(app.getService());
        }
        try (var app = BrowserlessApplicationContext.create(b -> b
                .withoutRoutes().withServletFactory(sharedServletFactory))) {
            assertNotNull(app.getService());
        }
    }

    @Test
    void unknownFeatureFlag_failsWithAvailableFlags() {
        BrowserlessTestSetupException exception = assertThrows(
                BrowserlessTestSetupException.class,
                () -> BrowserlessApplicationContext.create(b -> b
                        .withoutRoutes().withFeatureFlags("notAFeatureFlag")));

        assertTrue(exception.getMessage().contains("notAFeatureFlag"));
        assertTrue(exception.getMessage().contains(FEATURE.getId()),
                "The error should list the available feature flags");
    }

    private static FeatureFlags featureFlags(
            BrowserlessApplicationContext app) {
        return FeatureFlags.get(app.getService().getContext());
    }
}
