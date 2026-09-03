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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that {@link BrowserlessTestConfig} is applied to the mock Vaadin
 * Quarkus environment.
 */
@QuarkusTest
@ViewPackages(packages = "com.example")
@BrowserlessTestConfig(applicationProperties = "class.property=fromClass", featureFlags = "collaborationEngineBackend")
class QuarkusBrowserlessTestConfigTest extends QuarkusBrowserlessTest {

    @Test
    void classLevelConfiguration_isApplied() {
        Assertions.assertEquals("fromClass", property("class.property"));
        Assertions.assertTrue(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND));
    }

    @Test
    @BrowserlessTestConfig(applicationProperties = "method.property=fromMethod", featureFlags = "collaborationEngineBackend=false")
    void methodLevelConfiguration_isApplied() {
        Assertions.assertEquals("fromMethod", property("method.property"));
        Assertions.assertEquals("fromClass", property("class.property"));
        Assertions.assertFalse(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND));
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }
}
