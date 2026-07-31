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

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that {@link BrowserlessTestConfig} declared on a
 * {@link BrowserlessTest} subclass and on its test methods is applied to the
 * mock Vaadin environment.
 */
@ViewPackages(classes = WelcomeView.class)
@BrowserlessTestConfig(applicationProperties = { "class.property=fromClass",
        "shared.property=fromClass" }, featureFlags = "collaborationEngineBackend")
class BrowserlessTestConfigBaseClassTest extends BrowserlessTest {

    private static final Feature FEATURE = FeatureFlags.COLLABORATION_ENGINE_BACKEND;

    @Test
    void classLevelConfiguration_isApplied() {
        Assertions.assertEquals("fromClass", property("class.property"));
        Assertions.assertEquals("fromClass", property("shared.property"));
        Assertions.assertTrue(featureFlags().isEnabled(FEATURE),
                "Feature flag enabled on the test class should be enabled");
    }

    @Test
    @BrowserlessTestConfig(applicationProperties = "shared.property=fromMethod", featureFlags = "collaborationEngineBackend=false")
    void methodLevelConfiguration_winsOverClassLevel() {
        Assertions.assertEquals("fromMethod", property("shared.property"),
                "Method level property should win over the class level one");
        Assertions.assertEquals("fromClass", property("class.property"),
                "Class level properties should still be applied");
        Assertions.assertFalse(featureFlags().isEnabled(FEATURE),
                "Method level configuration should be able to disable a feature");
    }

    @Test
    void otherTestMethods_areNotAffected() {
        Assertions.assertEquals("fromClass", property("shared.property"),
                "Configuration of another test method must not leak");
        Assertions.assertTrue(featureFlags().isEnabled(FEATURE));
    }

    @Test
    void browserlessMode_isStillEnforced() {
        Assertions.assertTrue(
                VaadinService.getCurrent().getDeploymentConfiguration()
                        .getBooleanProperty("browserless", false));
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }

    private static FeatureFlags featureFlags() {
        return FeatureFlags.get(VaadinService.getCurrent().getContext());
    }
}
