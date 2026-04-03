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

import com.vaadin.flow.component.UI;

/**
 * Verifies that {@link BrowserlessClassExtension} creates a single Vaadin
 * environment shared across all test methods (same {@link UI} instance).
 */
@ViewPackages(classes = WelcomeView.class)
class BrowserlessExtensionPerClassTest {

    @RegisterExtension
    static BrowserlessClassExtension ext = new BrowserlessClassExtension();

    private static UI sharedUI;

    @BeforeAll
    static void captureUI() {
        sharedUI = UI.getCurrent();
        Assertions.assertNotNull(sharedUI,
                "Expecting current UI to be available after per-class init");
        ext.navigate(WelcomeView.class);
    }

    @Test
    void firstTest_sameUIInstance() {
        Assertions.assertSame(sharedUI, UI.getCurrent(),
                "Per-class lifecycle must reuse the same UI across tests");
        Assertions.assertInstanceOf(WelcomeView.class, ext.getCurrentView());
    }

    @Test
    void secondTest_sameUIInstance() {
        Assertions.assertSame(sharedUI, UI.getCurrent(),
                "Per-class lifecycle must reuse the same UI across tests");
    }
}
