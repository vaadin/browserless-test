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

import com.example.base.WelcomeView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that a class level {@link BrowserlessTestConfig} is applied to the
 * Vaadin environment shared by all the tests of a
 * {@link BrowserlessClassExtension}, and that a method level configuration is
 * rejected, since it could not be honored.
 */
@ViewPackages(classes = WelcomeView.class)
@BrowserlessTestConfig(applicationProperties = "class.property=fromClass", featureFlags = "collaborationEngineBackend")
class BrowserlessTestConfigClassExtensionTest {

    @RegisterExtension
    static BrowserlessClassExtension extension = new BrowserlessClassExtension()
            .withApplicationProperty("extension.property", "fromExtension");

    @Test
    void classLevelConfiguration_isApplied() {
        Assertions.assertEquals("fromClass",
                VaadinService.getCurrent().getDeploymentConfiguration()
                        .getStringProperty("class.property", null));
        Assertions.assertEquals("fromExtension",
                VaadinService.getCurrent().getDeploymentConfiguration()
                        .getStringProperty("extension.property", null));
        Assertions.assertTrue(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND));
    }

    @Test
    void methodLevelConfiguration_isRejected() {
        BrowserlessTestSetupException exception = Assertions.assertThrows(
                BrowserlessTestSetupException.class,
                () -> BrowserlessTestConfigExtension
                        .failOnMethodLevelConfiguration(
                                MethodLevelConfigFixture.class));

        Assertions.assertTrue(
                exception.getMessage().contains("configuredTestMethod"),
                "The error should name the offending test methods, but was: "
                        + exception.getMessage());
    }

    @Test
    void withoutMethodLevelConfiguration_isAccepted() {
        Assertions.assertDoesNotThrow(() -> BrowserlessTestConfigExtension
                .failOnMethodLevelConfiguration(
                        BrowserlessTestConfigClassExtensionTest.class));
    }

    static class MethodLevelConfigFixture {
        @BrowserlessTestConfig(featureFlags = "collaborationEngineBackend")
        void configuredTestMethod() {
        }
    }
}
