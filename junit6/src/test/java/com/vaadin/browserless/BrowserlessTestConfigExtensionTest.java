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

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies how {@link BrowserlessExtension} combines the configuration declared
 * with {@link BrowserlessTestConfig} and the one defined programmatically on
 * the extension instance.
 */
@ViewPackages(classes = WelcomeView.class)
@BrowserlessTestConfig(applicationProperties = { "class.property=fromClass",
        "shared.property=fromClass",
        "overridden.property=fromClass" }, featureFlags = "collaborationEngineBackend")
class BrowserlessTestConfigExtensionTest {

    private static final Feature FEATURE = FeatureFlags.COLLABORATION_ENGINE_BACKEND;

    @RegisterExtension
    BrowserlessExtension extension = new BrowserlessExtension()
            .withApplicationProperty("extension.property", "fromExtension")
            .withApplicationProperty("overridden.property", "fromExtension")
            .withApplicationProperty("shared.property", "fromExtension");

    @Test
    void programmaticConfiguration_winsOverClassLevelAnnotation() {
        Assertions.assertEquals("fromExtension",
                property("overridden.property"));
        Assertions.assertEquals("fromExtension",
                property("extension.property"));
        Assertions.assertEquals("fromClass", property("class.property"),
                "Class level properties should still be applied");
        Assertions.assertTrue(featureFlags().isEnabled(FEATURE),
                "Feature flags declared on the test class should be applied");
    }

    @Test
    @BrowserlessTestConfig(applicationProperties = "shared.property=fromMethod")
    void methodLevelAnnotation_winsOverProgrammaticConfiguration() {
        Assertions.assertEquals("fromMethod", property("shared.property"));
        Assertions.assertEquals("fromExtension",
                property("overridden.property"));
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }

    private static FeatureFlags featureFlags() {
        return FeatureFlags.get(VaadinService.getCurrent().getContext());
    }
}
