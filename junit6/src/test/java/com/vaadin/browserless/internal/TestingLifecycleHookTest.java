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
package com.vaadin.browserless.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;

class TestingLifecycleHookTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void default_runsRoundtripBeforeLookup() {
        AtomicBoolean roundtrip = expectRoundtrip();

        TestingLifecycleHook.DEFAULT.awaitBeforeLookup();

        Assertions.assertTrue(roundtrip.get());
    }

    private static AtomicBoolean expectRoundtrip() {
        AtomicBoolean roundtrip = new AtomicBoolean();
        UI ui = UI.getCurrent();
        ui.beforeClientResponse(ui, context -> roundtrip.set(true));
        return roundtrip;
    }
}
