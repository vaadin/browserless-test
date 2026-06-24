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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Verifies that {@link SpringBrowserlessTest} combined with
 * {@code @TestInstance(PER_CLASS)} creates the Vaadin environment once and
 * reuses the same {@link VaadinService}, {@link VaadinSession} and {@link UI}
 * across all test methods in the class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = SpringBrowserlessPerClassLifecycleTest.TestConfig.class)
class SpringBrowserlessPerClassLifecycleTest extends SpringBrowserlessTest {

    private VaadinService sharedService;
    private VaadinSession sharedSession;
    private UI sharedUI;

    @BeforeAll
    void captureVaadinEnvironment() {
        sharedService = VaadinService.getCurrent();
        sharedSession = VaadinSession.getCurrent();
        sharedUI = UI.getCurrent();
        Assertions.assertNotNull(sharedService,
                "VaadinService should be available after PER_CLASS init");
        Assertions.assertNotNull(sharedSession,
                "VaadinSession should be available after PER_CLASS init");
        Assertions.assertNotNull(sharedUI,
                "UI should be available after PER_CLASS init");
    }

    @Test
    void firstTest_sameVaadinEnvironment() {
        assertSameEnvironment();
    }

    @Test
    void secondTest_sameVaadinEnvironment() {
        assertSameEnvironment();
    }

    private void assertSameEnvironment() {
        Assertions.assertSame(sharedService, VaadinService.getCurrent(),
                "PER_CLASS lifecycle must reuse the same VaadinService across tests");
        Assertions.assertSame(sharedSession, VaadinSession.getCurrent(),
                "PER_CLASS lifecycle must reuse the same VaadinSession across tests");
        Assertions.assertSame(sharedUI, UI.getCurrent(),
                "PER_CLASS lifecycle must reuse the same UI across tests");
    }

    @Configuration
    static class TestConfig {
    }
}
