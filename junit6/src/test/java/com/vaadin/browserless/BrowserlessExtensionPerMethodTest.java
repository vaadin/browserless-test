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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.example.base.WelcomeView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.flow.component.UI;

/**
 * Verifies that {@link BrowserlessExtension} creates a fresh Vaadin environment
 * for each test method (different {@link UI} instances).
 */
@ViewPackages(classes = WelcomeView.class)
class BrowserlessExtensionPerMethodTest {

    @RegisterExtension
    BrowserlessExtension ext = new BrowserlessExtension();

    private static final Set<UI> seenUIs = Collections
            .synchronizedSet(new HashSet<>());

    @Test
    void firstTest_recordsUI() {
        UI ui = UI.getCurrent();
        Assertions.assertNotNull(ui, "Expecting current UI to be available");
        ext.navigate(WelcomeView.class);
        Assertions.assertInstanceOf(WelcomeView.class, ext.getCurrentView());
        seenUIs.add(ui);
    }

    @Test
    void secondTest_recordsUI() {
        UI ui = UI.getCurrent();
        Assertions.assertNotNull(ui, "Expecting current UI to be available");
        seenUIs.add(ui);
    }

    @AfterAll
    static void assertDistinctUIInstances() {
        Assertions.assertEquals(2, seenUIs.size(),
                "Per-method lifecycle must create a distinct UI for each test");
    }
}
