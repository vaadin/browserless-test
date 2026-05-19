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

import com.vaadin.browserless.internal.PrettyPrintTree;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

/**
 * Demonstrates the {@code find*} locator API. The {@code Button}, {@code
 * TextField}, {@code Grid} locator classes used here are emitted at build time
 * by the {@code LocatorProcessor} annotation processor from the existing
 * {@code @Tests}-annotated tester classes.
 * <p>
 * Although {@code TextFieldTester} is declared as {@code <T, V>}, the processor
 * pins {@code V} per {@code @Tests} target by walking the target's supertype
 * parameterization, so {@code findTextField()}, {@code findEmailField()}, etc.
 * are witness-free while {@code findBigDecimalField()} carries
 * {@code BigDecimal} automatically. The remaining witnessed entry points
 * ({@code findGrid(Class<V>)}, {@code findComboBox(Class<V>)}) are testers
 * whose value type isn't pinned at the target level.
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

            window.findTextField().withId("name").setValue("World");
            window.findButton().withCaption("Save").click();

            Assertions.assertEquals("Saved: World",
                    window.findSpan().withId("echo").getComponent().getText());
        }
    }

    @Test
    void buttonByCaption_multipleButtons_filterPicksRightOne() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.findTextField().withId("name").setValue("X");
            window.findButton().withCaption("Clear").click();

            String print = PrettyPrintTree.Companion.ofVaadin(UI.getCurrent())
                    .print();
            System.out.println(print);

            Assertions.assertEquals("", window.findTextField().withId("name")
                    .component().getValue());
        }
    }

    @Test
    void textField_setValue_thenRead_roundTrips() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.findTextField().withId("name").setValue("hello");
            Assertions.assertEquals("hello", window.findTextField()
                    .withId("name").component().getValue());
        }
    }

    @Test
    void grid_typedRowAccessor() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            Person first = window.findGrid(Person.class).getRow(0);

            Assertions.assertEquals("Alice", first.name());
            Assertions.assertEquals(3, window.findGrid(Person.class).size());
        }
    }

    @Test
    void singleLocator_reusedAfterUiChange_reresolves() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            var save = window.findButton().withCaption("Save");
            window.findTextField().withId("name").setValue("first");
            save.click();

            // Resolution caches across calls in one chain. After a UI change
            // that could replace the component, invalidate() rewinds the
            // cache.
            window.findTextField().withId("name").setValue("second");
            save.invalidate().click();

            Assertions.assertEquals("Saved: second",
                    window.findSpan().withId("echo").getComponent().getText());
        }
    }

    @Test
    void customLocator_viaGetSupplier_composesBuiltins() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.find(PersonFormLocator::new).fillIn("Ada", "ada@example.com");
            window.find(PersonFormLocator::new).submit();

            Assertions.assertEquals("Submitted: Ada <ada@example.com>",
                    window.findSpan().withId("echo").getComponent().getText());
        }
    }

    @Test
    void filterChain_expandedSurface() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // withAttribute — every component with an id has the "id"
            // attribute set on its element.
            Assertions.assertTrue(
                    window.findTextField().withAttribute("id", "name").exists());

            // withCondition — typed predicate against the matched type.
            window.findButton()
                    .withCondition(b -> "Save".equals(b.getText())).click();
            Assertions.assertEquals("Saved: ",
                    window.findSpan().withId("echo").component().getText());
        }
    }

    @Test
    void filterChain_escapeHatch_unaryOperator() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // Use the escape hatch to compose a ComponentQuery-only filter
            // (withPropertyValue is not exposed on Locator directly).
            window.findButton()
                    .with(q -> q.withPropertyValue(Button::getText, "Clear"))
                    .click();

            // Save button was untouched; Clear emptied the name field.
            Assertions.assertEquals("",
                    window.findTextField().withId("name").component().getValue());
        }
    }

    @Test
    void multiUser_locatorsRespectActiveWindow() {
        try (var app = BrowserlessApplicationContext.create(routes())) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(LocatorDemoView.class);
            bob.navigate(LocatorDemoView.class);

            alice.findTextField().withId("name").setValue("alice-value");
            bob.findTextField().withId("name").setValue("bob-value");

            // Each window's locator targets its own UI; values do not leak.
            Assertions.assertEquals("alice-value",
                    alice.findTextField().withId("name").component().getValue());
            Assertions.assertEquals("bob-value",
                    bob.findTextField().withId("name").component().getValue());
        }
    }
}
