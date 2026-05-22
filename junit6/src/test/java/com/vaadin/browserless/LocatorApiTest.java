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

import java.util.List;
import java.util.NoSuchElementException;

import com.example.locator.LocatorDemoView;
import com.example.locator.LocatorDemoView.Person;
import com.example.locator.PersonFormLocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.locator.Locator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

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

    private static BrowserlessApplicationContext createApplicationContext() {
        return BrowserlessApplicationContext.create(LocatorDemoView.class);
    }

    @Test
    void buttonByCaption_click_firesListener() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.findTextField().withId("name").setValue("World");
            window.findButton().withCaption("Save").click();

            // getText() is inherited via SpanTester -> HtmlClickContainer ->
            // HtmlContainerTester. The processor walks the supertype chain, so
            // inherited tester methods become locator delegates too.
            Assertions.assertEquals("Saved: World",
                    window.findSpan().withId("echo").getText());
        }
    }

    @Test
    void buttonByCaption_multipleButtons_filterPicksRightOne() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.findTextField().withId("name").setValue("X");
            window.findButton().withCaption("Clear").click();

            Assertions.assertEquals("", window.findTextField().withId("name")
                    .component().getValue());
        }
    }

    @Test
    void textField_setValue_thenRead_roundTrips() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.findTextField().withId("name").setValue("hello");
            Assertions.assertEquals("hello", window.findTextField()
                    .withId("name").component().getValue());
        }
    }

    @Test
    void grid_typedRowAccessor() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            Person first = window.findGrid(Person.class).getRow(0);

            Assertions.assertEquals("Alice", first.name());
            Assertions.assertEquals(3, window.findGrid(Person.class).size());
        }
    }

    @Test
    void singleLocator_reusedAfterUiChange_reresolves() {
        try (var app = createApplicationContext()) {
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
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.find(PersonFormLocator::new).fillIn("Ada",
                    "ada@example.com");
            window.find(PersonFormLocator::new).submit();

            Assertions.assertEquals("Submitted: Ada <ada@example.com>",
                    window.findSpan().withId("echo").getComponent().getText());
        }
    }

    @Test
    void filterChain_expandedSurface() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // withAttribute — every component with an id has the "id"
            // attribute set on its element.
            Assertions.assertTrue(window.findTextField()
                    .withAttribute("id", "name").exists());

            // withCondition — typed predicate against the matched type.
            window.findButton().withCondition(b -> "Save".equals(b.getText()))
                    .click();
            Assertions.assertEquals("Saved: ",
                    window.findSpan().withId("echo").component().getText());
        }
    }

    @Test
    void filterChain_escapeHatch_unaryOperator() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // Use the escape hatch to compose a ComponentQuery-only filter
            // (withPropertyValue is not exposed on Locator directly).
            window.findButton()
                    .with(q -> q.withPropertyValue(Button::getText, "Clear"))
                    .click();

            // Save button was untouched; Clear emptied the name field.
            Assertions.assertEquals("", window.findTextField().withId("name")
                    .component().getValue());
        }
    }

    @Test
    void filterChain_escapeHatch_returnsDifferentQuery() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            window.findTextField().withId("name").setValue("Ada");

            // UnaryOperator returns a fresh query — its contract is `T
            // apply(T)`,
            // so the locator must adopt the returned instance rather than
            // discarding it. The fresh query has no caption filter; only the
            // Save button matches the original Span query state we ignore by
            // returning a new query targeting Save.
            window.findButton().with(
                    q -> new ComponentQuery<>(Button.class).withCaption("Save"))
                    .click();

            Assertions.assertEquals("Saved: Ada",
                    window.find(Span.class).withId("echo").single().getText());
        }
    }

    @Test
    void exists_truePathAndFalsePath() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            Assertions.assertTrue(
                    window.findTextField().withId("name").exists(),
                    "filter chain matching a real component returns true");
            Assertions.assertFalse(
                    window.findTextField().withId("does-not-exist").exists(),
                    "filter chain matching nothing returns false");
        }
    }

    @Test
    void components_returnsAllMatchesAndKeepsLocatorReusable() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            var buttons = window.findButton();
            // Save, Clear, plus PersonForm's Submit.
            Assertions.assertEquals(3, buttons.components().size());

            // components() bypasses the single-match cache, so the same
            // instance can still resolve a specific pick afterwards.
            Assertions.assertEquals("Save",
                    buttons.atIndex(1).component().getText());
        }
    }

    @Test
    void inside_componentOverload_scopesToDescendants() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = window
                    .find(LocatorDemoView.PersonForm.class).single();

            // Globally there are 3 buttons in the view; scoping to the
            // form's descendants narrows the match down to the single
            // Submit button inside the composite.
            Assertions.assertEquals(3, window.findButton().components().size());
            Assertions.assertEquals(1,
                    window.findButton().inside(form).components().size());
        }
    }

    @Test
    void inside_locatorOverload_evaluatesParentLazily() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            class CountingFormLocator extends
                    Locator<LocatorDemoView.PersonForm, CountingFormLocator> {
                int calls = 0;

                CountingFormLocator() {
                    super(LocatorDemoView.PersonForm.class);
                }

                @Override
                public LocatorDemoView.PersonForm component() {
                    calls++;
                    return super.component();
                }
            }
            var formLoc = new CountingFormLocator();

            // inside(Locator) must not resolve the parent yet.
            var childLoc = window.findButton().inside(formLoc);
            Assertions.assertEquals(0, formLoc.calls,
                    "inside(Locator) must defer parent resolution");

            // First child action resolves the parent exactly once.
            Assertions.assertEquals(1, childLoc.components().size());
            Assertions.assertEquals(1, formLoc.calls);

            // invalidate() on the parent propagates: the next child action
            // re-asks the parent for its component.
            formLoc.invalidate();
            Assertions.assertEquals(1, childLoc.components().size());
            Assertions.assertEquals(2, formLoc.calls);
        }
    }

    @Test
    void inside_locatorOverload_rejectsSelfReference() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            var loc = window.findButton();
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> loc.inside(loc));
        }
    }

    @Test
    void use_seedsLocatorWithComponent_actionWorks() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = window
                    .find(LocatorDemoView.PersonForm.class).single();

            // Caller holds direct references to the composite's children —
            // use(...) skips the filter chain and seeds the locator with
            // each instance.
            window.use(form.nameField).setValue("Ada");
            window.use(form.emailField).setValue("ada@example.com");
            window.use(form.submit).click();

            Assertions.assertEquals("Submitted: Ada <ada@example.com>",
                    window.findSpan().withId("echo").getText());
        }
    }

    @Test
    void use_componentAndExistsReturnSeededInstance() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = window
                    .find(LocatorDemoView.PersonForm.class).single();

            var loc = window.use(form.submit);
            Assertions.assertSame(form.submit, loc.component());
            Assertions.assertEquals(List.of(form.submit), loc.components());
            Assertions.assertTrue(loc.exists());
            // invalidate() rewinds the cache; re-resolution still matches
            // the same instance because the identity predicate is sticky.
            Assertions.assertSame(form.submit, loc.invalidate().component());
        }
    }

    @Test
    void use_additionalFilterCanExcludeSeededComponent() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = window
                    .find(LocatorDemoView.PersonForm.class).single();

            // form.submit's id is "pf-submit"; an extra withId("nope")
            // filter composes on top of the identity predicate and excludes
            // the only matching component.
            var loc = window.use(form.submit).withId("nope");
            Assertions.assertFalse(loc.exists(),
                    "incompatible filter must zero out the seeded match");
            Assertions.assertThrows(NoSuchElementException.class,
                    loc::component);
        }
    }

    @Test
    void use_genericTarget_carriesTypeArg() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // The demo view has a single Grid<Person>. Get a typed reference
            // via the typed find entry, then exercise use(Grid<T>) to bind
            // the locator to that instance.
            Grid<Person> grid = window.findGrid(Person.class).getComponent();
            Person first = window.use(grid).getRow(0);
            Assertions.assertEquals("Alice", first.name());
        }
    }

    @Test
    void filterChain_escapeHatch_nullReturnThrows() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // Returning null from the operator is a contract violation —
            // fail loudly rather than silently dropping the operator's
            // intent.
            IllegalStateException ex = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> window.findButton().with(q -> null));
            Assertions.assertTrue(ex.getMessage().contains("non-null"),
                    "message should explain the contract: " + ex.getMessage());
        }
    }

    @Test
    void atIndex_picksNthMatch() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // The demo view has multiple buttons (Save, Clear, plus Submit
            // inside the PersonForm composite). atIndex is 1-based, so
            // atIndex(2) targets Clear.
            window.findTextField().withId("name").setValue("X");
            window.findButton().atIndex(2).click();
            Assertions.assertEquals("", window.findTextField().withId("name")
                    .component().getValue());
        }
    }

    @Test
    void atIndex_stickyAcrossFilterSteps_butClearedByInvalidate() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // The demo view has three buttons total: Save (1), Clear (2),
            // PersonForm's Submit (3). atIndex(2) picks Clear.
            var btn = window.findButton().atIndex(2);
            Assertions.assertEquals("Clear", btn.component().getText());

            // atIndex is part of the filter chain — narrowing further
            // does NOT drop the pick. Once the chain is narrowed to a
            // single match, atIndex(2) on a single-match query throws.
            btn.withCaption("Clear");
            Assertions.assertThrows(IndexOutOfBoundsException.class,
                    btn::component,
                    "pickIndex stays sticky across filter steps");

            // invalidate() is the explicit rewind hatch: clears the
            // cached resolution AND pickIndex. After it, resolution
            // falls back to single-match, which succeeds.
            Assertions.assertEquals("Clear",
                    btn.invalidate().component().getText());
        }
    }

    @Test
    void atIndex_zeroOrNegativeThrows() {
        // No app context needed — the validation runs on the locator itself
        // before any resolution attempt.
        IllegalArgumentException zero = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new com.vaadin.flow.component.button.ButtonLocator()
                        .atIndex(0));
        Assertions.assertTrue(zero.getMessage().contains("greater than zero"),
                "message should explain the contract: " + zero.getMessage());

        IllegalArgumentException negative = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new com.vaadin.flow.component.button.ButtonLocator()
                        .atIndex(-1));
        Assertions.assertTrue(
                negative.getMessage().contains("greater than zero"),
                "message should explain the contract: "
                        + negative.getMessage());
    }

    @Test
    void findSupplier_nullReturnThrows() {
        try (var app = createApplicationContext()) {
            var window = app.newUser().newWindow();
            window.navigate(LocatorDemoView.class);

            // Same contract as Locator.with: a null Locator from the factory
            // is a contract violation that should surface immediately.
            IllegalStateException ex = Assertions.assertThrows(
                    IllegalStateException.class, () -> window.find(() -> null));
            Assertions.assertTrue(ex.getMessage().contains("non-null"),
                    "message should explain the contract: " + ex.getMessage());
        }
    }

    @Test
    void multiUser_locatorsRespectActiveWindow() {
        try (var app = createApplicationContext()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(LocatorDemoView.class);
            bob.navigate(LocatorDemoView.class);

            alice.findTextField().withId("name").setValue("alice-value");
            bob.findTextField().withId("name").setValue("bob-value");

            // Each window's locator targets its own UI; values do not leak.
            Assertions.assertEquals("alice-value", alice.findTextField()
                    .withId("name").component().getValue());
            Assertions.assertEquals("bob-value",
                    bob.findTextField().withId("name").component().getValue());
        }
    }
}
