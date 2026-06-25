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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.browserless.locator.Locators;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonTester;

/**
 * Demonstrates the recommended way to get a Vaadin environment shared across a
 * whole test class without extending {@link BrowserlessTest} (which only
 * supports the per-method lifecycle): register a
 * {@link BrowserlessClassExtension} and implement {@link TesterWrappers} (and
 * optionally {@link Locators}) so the testing DSL is available directly on the
 * test class.
 */
@ViewPackages(classes = WelcomeView.class)
class BrowserlessClassExtensionWrappersTest
        implements TesterWrappers, Locators {

    @RegisterExtension
    static BrowserlessClassExtension ext = new BrowserlessClassExtension();

    private static UI sharedUI;

    @BeforeAll
    static void captureUI() {
        sharedUI = UI.getCurrent();
        Assertions.assertNotNull(sharedUI,
                "Expecting current UI to be available after per-class init");
    }

    @Test
    void firstTest_sharedUiAndWrappersAvailable() {
        Assertions.assertSame(sharedUI, UI.getCurrent(),
                "Per-class lifecycle must reuse the same UI across tests");

        // The DSL inherited from TesterWrappers works directly on the test
        // class, without extending BrowserlessTest.
        Button button = new Button("ok");
        UI.getCurrent().add(button);
        ButtonTester<Button> tester = test(button);
        Assertions.assertNotNull(tester);
    }

    @Test
    void secondTest_sameUiInstance() {
        Assertions.assertSame(sharedUI, UI.getCurrent(),
                "Per-class lifecycle must reuse the same UI across tests");
    }
}
