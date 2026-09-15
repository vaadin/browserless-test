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

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
