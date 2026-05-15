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

import com.example.locator.LocatorDemoView;
import com.example.locator.LocatorDemoView.Person;
import com.example.locator.PersonFormLocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.html.Span;

/**
 * Demonstrates the {@code get*} locator API. The {@code Button}, {@code
 * TextField}, {@code Grid} locator classes used here are emitted at build time
 * by the {@code LocatorProcessor} annotation processor from the existing
 * {@code @Tests}-annotated tester classes.
 * <p>
 * Note that {@code TextFieldTester} is declared as {@code <T, V>} so the
 * generated {@code getTextField(Class<V>)} entry point takes a value-type
 * witness. This mirrors the existing {@code test(TextField, Class<V>)}
 * overload pattern and gives the user the right typed surface.
 */
class LocatorApiTest {

    private static Routes routes() {
        return new Routes()
                .autoDiscoverViews(LocatorDemoView.class.getPackageName());
    }

    @Test
    void buttonByCaption_click_firesListener() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.getTextField(String.class).withId("name").setValue("World");
            window.getButton().withCaption("Save").click();

            Assertions.assertEquals("Saved: World", window
                    .find(Span.class).withId("echo").single().getText());
        }
    }

    @Test
    void buttonByCaption_multipleButtons_filterPicksRightOne() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.getTextField(String.class).withId("name").setValue("X");
            window.getButton().withCaption("Clear").click();

            Assertions.assertEquals("",
                    window.getTextField(String.class).withId("name")
                            .component().getValue());
        }
    }

    @Test
    void textField_setValue_thenRead_roundTrips() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.getTextField(String.class).withId("name").setValue("hello");
            Assertions.assertEquals("hello",
                    window.getTextField(String.class).withId("name")
                            .component().getValue());
        }
    }

    @Test
    void grid_typedRowAccessor() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            Person first = window.getGrid(Person.class).getRow(0);

            Assertions.assertEquals("Alice", first.name());
            Assertions.assertEquals(3, window.getGrid(Person.class).size());
        }
    }

    @Test
    void singleLocator_reusedAfterUiChange_reresolves() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            var save = window.getButton().withCaption("Save");
            window.getTextField(String.class).withId("name").setValue("first");
            save.click();

            // Resolution caches across calls in one chain. After a UI change
            // that could replace the component, invalidate() rewinds the
            // cache.
            window.getTextField(String.class).withId("name").setValue("second");
            save.invalidate().click();

            Assertions.assertEquals("Saved: second", window
                    .find(Span.class).withId("echo").single().getText());
        }
    }

    @Test
    void customLocator_viaGetSupplier_composesBuiltins() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.get(PersonFormLocator::new)
                    .fillIn("Ada", "ada@example.com");
            window.get(PersonFormLocator::new).submit();

            Assertions.assertEquals("Submitted: Ada <ada@example.com>",
                    window.find(Span.class).withId("echo").single().getText());
        }
    }

    @Test
    void multiUser_locatorsRespectActiveWindow() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(LocatorDemoView.class);
            bob.navigate(LocatorDemoView.class);

            alice.getTextField(String.class).withId("name")
                    .setValue("alice-value");
            bob.getTextField(String.class).withId("name").setValue("bob-value");

            // Each window's locator targets its own UI; values do not leak.
            Assertions.assertEquals("alice-value",
                    alice.getTextField(String.class).withId("name")
                            .component().getValue());
            Assertions.assertEquals("bob-value",
                    bob.getTextField(String.class).withId("name").component()
                            .getValue());
        }
    }
}
