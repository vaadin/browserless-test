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

import com.example.adhoc.CounterWidget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Exercises the ad-hoc component testing path
 * ({@link BrowserlessApplicationContext#create()} +
 * {@link BrowserlessUIContext#show(com.vaadin.flow.component.Component)}). No
 * {@code @Route} view or {@code Routes} discovery is required — components are
 * attached directly to a fresh UI.
 */
class AdhocComponentTest {

    @Test
    void adhoc_widget_attachedAndInteractive() {
        CounterWidget widget = new CounterWidget();
        try (var window = BrowserlessUIContext.adhoc(widget)) {
            Assertions.assertTrue(widget.isAttached(),
                    "adhoc() should attach the component to the UI");

            window.findButton().withCaption("Increment").click();
            window.findButton().withCaption("Increment").click();

            Assertions.assertEquals(2, widget.getCount());
        }
    }

    @Test
    void adhoc_close_cascadesToOwnedApp() {
        CounterWidget widget = new CounterWidget();
        var window = BrowserlessUIContext.adhoc(widget);
        Assertions.assertTrue(widget.isAttached());

        window.close();

        Assertions.assertFalse(widget.isAttached(),
                "Closing the adhoc window should tear down the owned app and detach the widget");
        // The thread-local Vaadin state should be cleared too: no UI on this
        // thread once the bundled app is gone.
        Assertions.assertNull(com.vaadin.flow.component.UI.getCurrent(),
                "Owned app should be closed, clearing thread-locals");
    }

    @Test
    void show_widget_attachedAndInteractive_longForm() {
        try (var app = BrowserlessApplicationContext.create()) {
            var window = app.newUser().newWindow();

            CounterWidget widget = window.show(new CounterWidget());

            Assertions.assertTrue(widget.isAttached());

            window.findButton().withCaption("Increment").click();
            Assertions.assertEquals(1, widget.getCount());
        }
    }

    @Test
    void show_returnsSameInstance() {
        try (var app = BrowserlessApplicationContext.create()) {
            var window = app.newUser().newWindow();

            CounterWidget widget = new CounterWidget();
            CounterWidget returned = window.show(widget);

            Assertions.assertSame(widget, returned);
        }
    }

    @Test
    void show_secondCall_replacesPriorContent() {
        try (var app = BrowserlessApplicationContext.create()) {
            var window = app.newUser().newWindow();

            CounterWidget first = window.show(new CounterWidget());
            CounterWidget second = window.show(new CounterWidget());

            Assertions.assertFalse(first.isAttached(),
                    "Prior content should be detached on a fresh show()");
            Assertions.assertTrue(second.isAttached());

            // Only the second counter is visible — the increment in this test
            // affects only the still-attached widget.
            window.findButton().withCaption("Increment").click();
            Assertions.assertEquals(0, first.getCount());
            Assertions.assertEquals(1, second.getCount());
        }
    }

    @Test
    void show_wrappedInLayout_componentsInsideAreFindable() {
        try (var app = BrowserlessApplicationContext.create()) {
            var window = app.newUser().newWindow();

            TextField field = new TextField("Name");
            field.setId("name");
            HorizontalLayout row = new HorizontalLayout(field);
            window.show(new VerticalLayout(row));

            window.findTextField().withId("name").setValue("Ada");
            Assertions.assertEquals("Ada", field.getValue());
        }
    }

    @Test
    void show_reattachesComponentFromAnotherParent() {
        try (var app = BrowserlessApplicationContext.create()) {
            var window = app.newUser().newWindow();

            TextField field = new TextField("Name");
            new VerticalLayout(field); // detached parent; field has parent now
            Assertions.assertTrue(field.getParent().isPresent());

            window.show(field);

            Assertions.assertTrue(field.isAttached());
            // Direct parent is the UI now, not the prior VerticalLayout.
            Assertions.assertTrue(
                    field.getParent().filter(p -> p == window.getUI())
                            .isPresent(),
                    "field's parent should be the UI after show()");
        }
    }
}
