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

import com.example.multiuser.SharedCounterView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;

/**
 * Verifies that {@link BrowserlessDsl} provides the browserless DSL to a class
 * that does <em>not</em> extend {@link BaseBrowserlessTest}: the class only
 * declares {@code implements BrowserlessDsl} and supplies a
 * {@link #currentUI()}. It arranges the Vaadin environment itself (the mixin
 * supplies only the DSL, not the environment setup).
 */
class BrowserlessDslMixinTest implements BrowserlessDsl {

    private BrowserlessApplicationContext app;
    private BrowserlessUIContext window;

    @BeforeEach
    void setUp() {
        SharedCounterView.counter.set(0);
        app = BrowserlessApplicationContext.create(SharedCounterView.class);
        window = app.newUser().newWindow();
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Override
    public UI currentUI() {
        window.activate();
        return UI.getCurrent();
    }

    @Test
    void dslMethods_availableViaMixin_onRawClass() {
        // navigate() default method
        SharedCounterView view = navigate(SharedCounterView.class);
        Assertions.assertNotNull(view);

        // getCurrentView() default method
        Assertions.assertSame(view, getCurrentView());

        // find() default method
        Assertions.assertEquals("Count: 0",
                find(Paragraph.class).single().getText());

        // find() + test() default methods drive a click and round-trip
        test(find(Button.class).withText("Increment").single()).click();
        Assertions.assertEquals("Count: 1",
                find(Paragraph.class).single().getText());
    }
}
