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

import java.util.concurrent.atomic.AtomicReference;

import com.example.adhoc.CounterWidget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;

/**
 * Exercises the ad-hoc component testing path
 * ({@link BrowserlessApplicationContext#forComponent(java.util.function.Supplier)}
 * and its
 * {@link BrowserlessUIContext#forComponent(com.vaadin.flow.component.Component)}
 * shorthand). No {@code @Route} view or {@code Routes} discovery is required —
 * the component is attached directly to a fresh UI.
 */
class AdhocComponentTest {

    @Test
    void forComponent_window_attachedAndInteractive() {
        CounterWidget widget = new CounterWidget();
        try (var window = BrowserlessUIContext.forComponent(widget)) {
            Assertions.assertTrue(widget.isAttached(),
                    "forComponent() should attach the component to the UI");

            window.findButton().withText("Increment").click();
            window.findButton().withText("Increment").click();

            Assertions.assertEquals(2, widget.getCount());
        }
    }

    @Test
    void forComponent_longForm_attachesComponent() {
        CounterWidget widget = new CounterWidget();
        try (var app = BrowserlessApplicationContext
                .forComponent(() -> widget)) {
            var window = app.newUser().newWindow();
            Assertions.assertTrue(widget.isAttached(),
                    "forComponent() should attach the component to the window's UI");

            window.findButton().withText("Increment").click();
            window.findButton().withText("Increment").click();

            Assertions.assertEquals(2, widget.getCount());
        }
    }

    @Test
    void forComponent_freshFactory_attachesNewInstancePerWindow() {
        try (var app = BrowserlessApplicationContext
                .forComponent(CounterWidget::new)) {
            var window1 = app.newUser().newWindow();
            var window2 = app.newUser().newWindow();

            CounterWidget first = window1.find(CounterWidget.class).first();
            CounterWidget second = window2.find(CounterWidget.class).first();

            Assertions.assertNotSame(first, second,
                    "Each window should get its own component instance");
            Assertions.assertTrue(first.isAttached());
            Assertions.assertTrue(second.isAttached());
        }
    }

    @Test
    void forComponent_supplier_constructsWithUiThreadLocalPresent() {
        AtomicReference<UI> uiDuringConstruction = new AtomicReference<>();
        try (var window = BrowserlessUIContext.forComponent(() -> {
            uiDuringConstruction.set(UI.getCurrent());
            return new CounterWidget();
        })) {
            Assertions.assertNotNull(uiDuringConstruction.get(),
                    "UI.getCurrent() should be set while the factory builds the component");
            Assertions.assertSame(window.getUI(), uiDuringConstruction.get(),
                    "The component should be built against this window's UI");
        }
    }

    @Test
    void forComponent_close_cascadesToBundledApp() {
        CounterWidget widget = new CounterWidget();
        var window = BrowserlessUIContext.forComponent(widget);
        Assertions.assertTrue(widget.isAttached());

        window.close();

        Assertions.assertFalse(widget.isAttached(),
                "Closing the forComponent window should tear down the bundled app and detach the widget");
        // The thread-local Vaadin state should be cleared too: no UI on this
        // thread once the bundled app is gone.
        Assertions.assertNull(UI.getCurrent(),
                "Bundled app should be closed, clearing thread-locals");
    }
}
