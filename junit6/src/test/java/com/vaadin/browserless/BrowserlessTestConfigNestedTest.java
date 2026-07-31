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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that a nested test inherits the {@link BrowserlessTestConfig} of its
 * enclosing test class, and can refine it.
 */
@BrowserlessTestConfig(applicationProperties = { "outer.property=fromOuter",
        "shared.property=fromOuter" })
class BrowserlessTestConfigNestedTest {

    @Nested
    @ViewPackages(classes = WelcomeView.class)
    @BrowserlessTestConfig(applicationProperties = {
            "nested.property=fromNested", "shared.property=fromNested" })
    class NestedTest extends BrowserlessTest {

        @Test
        void enclosingConfiguration_isMergedWithTheNestedOne() {
            Assertions.assertEquals("fromOuter", property("outer.property"),
                    "Configuration of the enclosing test class should be applied");
            Assertions.assertEquals("fromNested", property("nested.property"));
            Assertions.assertEquals("fromNested", property("shared.property"),
                    "Nested configuration should win over the enclosing one");
        }
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }
}
