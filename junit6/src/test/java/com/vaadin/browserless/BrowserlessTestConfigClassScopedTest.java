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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that the configuration installed by a
 * {@link BrowserlessClassExtension} survives the per method resolution
 * performed by {@link BrowserlessTestConfigExtension}, which a
 * {@link BrowserlessTest} subclass registers as well.
 *
 * The class extension resolves the programmatic configuration once, before
 * creating the Vaadin environment shared by the whole class; the per method
 * resolution knows nothing about it, so it must not replace nor clear it while
 * that environment is alive.
 */
@ViewPackages(classes = WelcomeView.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BrowserlessTestConfig(applicationProperties = "class.property=fromClass")
class BrowserlessTestConfigClassScopedTest extends BrowserlessTest {

    @RegisterExtension
    static BrowserlessClassExtension extension = new BrowserlessClassExtension()
            .withApplicationProperty("extension.property", "fromExtension");

    // No @BeforeEach/@AfterEach here: the class extension drives both from its
    // beforeAll/afterAll callbacks, so the environment is created only once.
    @Override
    protected void initVaadinEnvironment() {
        super.initVaadinEnvironment();
    }

    @Override
    protected void cleanVaadinEnvironment() {
        super.cleanVaadinEnvironment();
    }

    @Test
    void programmaticConfiguration_isStillVisibleDuringTheTest() {
        Assertions.assertEquals("fromExtension",
                testConfiguration().getApplicationProperties()
                        .get("extension.property"),
                "The configuration of the class extension must not be dropped "
                        + "by the per method resolution");
        Assertions.assertEquals("fromClass", testConfiguration()
                .getApplicationProperties().get("class.property"));
    }

    @Test
    void programmaticConfiguration_reachedTheVaadinEnvironment() {
        Assertions.assertEquals("fromExtension",
                property("extension.property"));
        Assertions.assertEquals("fromClass", property("class.property"));
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }
}
