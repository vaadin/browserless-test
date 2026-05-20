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

import java.util.concurrent.atomic.AtomicInteger;

import com.example.base.signals.SignalsView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.html.NativeButtonTester;

/**
 * Covers the {@link BrowserlessUIContext} DSL methods that mirror
 * {@link BaseBrowserlessTest}: {@link BrowserlessUIContext#fireShortcut} and
 * the explicit-tester {@link BrowserlessUIContext#test(Class, Y)} overload.
 */
class BrowserlessUIContextDSLTest {

    private BrowserlessApplicationContext app;
    private BrowserlessUIContext window;

    @BeforeEach
    void setUp() {
        app = BrowserlessApplicationContext.create(SignalsView.class);
        window = app.newUser().newWindow();
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void fireShortcut_uiListener_invokedForExactMatch() {
        AtomicInteger eventsCounter = new AtomicInteger();
        window.getUI().addShortcutListener(eventsCounter::incrementAndGet,
                Key.KEY_W, KeyModifier.ALT, KeyModifier.SHIFT);

        window.fireShortcut(Key.KEY_W);
        Assertions.assertEquals(0, eventsCounter.get());

        window.fireShortcut(Key.KEY_W, KeyModifier.ALT);
        Assertions.assertEquals(0, eventsCounter.get());

        window.fireShortcut(Key.KEY_W, KeyModifier.ALT, KeyModifier.SHIFT);
        Assertions.assertEquals(1, eventsCounter.get());
    }

    @Test
    void testWithExplicitTester_wrapsComponentWithGivenTester() {
        var view = window.navigate(SignalsView.class);
        NativeButtonTester tester = window.test(NativeButtonTester.class,
                view.incrementButton);
        Assertions.assertNotNull(tester);
        tester.click();
        // Effect runs synchronously for on-attach signals.
        Assertions.assertEquals("Counter: 1", view.counter.getText());
    }

}
