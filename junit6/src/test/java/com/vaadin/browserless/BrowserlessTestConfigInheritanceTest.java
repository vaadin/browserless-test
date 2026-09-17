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

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that a {@link BrowserlessTestConfig} declared by a base test class
 * is merged with, rather than replaced by, the one declared by a subclass.
 */
@ViewPackages(classes = WelcomeView.class)
@BrowserlessTestConfig(applicationProperties = "shared.property=fromSubclass")
class BrowserlessTestConfigInheritanceTest extends AbstractInheritedConfigTest {

    @Test
    void baseClassConfiguration_isInherited() {
        Assertions.assertEquals("fromBase", property("base.property"),
                "Properties declared by the base class must not be lost when "
                        + "a subclass declares its own configuration");
        Assertions.assertTrue(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND),
                "Feature flags declared by the base class must be inherited");
    }

    @Test
    void subclassConfiguration_winsOverTheBaseClassOne() {
        Assertions.assertEquals("fromSubclass", property("shared.property"));
    }

    @Test
    @BrowserlessTestConfig(applicationProperties = "shared.property=fromMethod")
    void methodConfiguration_winsOverTheWholeHierarchy() {
        Assertions.assertEquals("fromMethod", property("shared.property"));
        Assertions.assertEquals("fromBase", property("base.property"),
                "Base class properties should still be applied");
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }
}
