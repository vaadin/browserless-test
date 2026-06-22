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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import com.example.locator.LocatorDemoView;
import com.example.locator.LocatorDemoView.Person;
import com.example.locator.PersonFormLocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.browserless.locator.Locator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

/**
 * Mirror of {@link LocatorApiTest} exercising the same {@code find*} locator
 * scenarios through the {@link BrowserlessExtension} entry point — the primary
 * 1.1 audience. Multi-user / multi-window scenarios and pure input-validation
 * paths that don't depend on the entry point are not mirrored here; see
 * {@link LocatorApiTest} for those.
 */
class LocatorApiOnExtensionTest {

    @Nested
    @ViewPackages(classes = LocatorDemoView.class)
    class OnBrowserlessExtension {

        @RegisterExtension
        BrowserlessExtension ext = new BrowserlessExtension();

        @Test
        void buttonByCaption_click_firesListener() {
            ext.navigate(LocatorDemoView.class);

            ext.findTextField().withId("name").setValue("World");
            ext.findButton().withText("Save").click();

            // getText() is inherited via SpanTester -> HtmlClickContainer ->
            // HtmlContainerTester. The processor walks the supertype chain, so
            // inherited tester methods become locator delegates too.
            Assertions.assertEquals("Saved: World",
                    ext.findSpan().withId("echo").getText());
        }

        @Test
        void buttonByCaption_multipleButtons_filterPicksRightOne() {
            ext.navigate(LocatorDemoView.class);

            ext.findTextField().withId("name").setValue("X");
            ext.findButton().withText("Clear").click();

            Assertions.assertEquals("",
                    ext.findTextField().withId("name").component().getValue());
        }

        @Test
        void textField_setValue_thenRead_roundTrips() {
            ext.navigate(LocatorDemoView.class);

            ext.findTextField().withId("name").setValue("hello");
            Assertions.assertEquals("hello",
                    ext.findTextField().withId("name").component().getValue());
        }

        @Test
        void withValue_typedAgainstComponentValueType() {
            // HasValueFilter threads V from the component's HasValue<?, V>, so
            // withValue is bound to the component's exact value type — String
            // for TextField, BigDecimal for BigDecimalField, LocalDate for
            // DatePicker, Boolean for Checkbox. Wrong types
            // (e.g. findTextField().withValue(42),
            // findDatePicker().withValue("2026-05-28"))
            // would not compile here.
            ext.navigate(LocatorDemoView.class);

            // Reuse a fresh locator across the exists() assertions so each
            // typed-value block reads cleanly. Actions (setValue, click) use
            // a separate chain — calling them on the stored locator would
            // narrow its query's count via component()→single() and make a
            // follow-up withValue(...) that matches nothing throw instead of
            // returning false.

            ext.findTextField().withId("name").setValue("typed");
            var name = ext.findTextField().withId("name");
            Assertions.assertTrue(name.withValue("typed").exists());
            Assertions.assertFalse(name.withValue("other").exists());

            BigDecimal priceValue = new BigDecimal("19.95");
            ext.findBigDecimalField().withId("price").setValue(priceValue);
            var price = ext.findBigDecimalField().withId("price");
            Assertions.assertTrue(price.withValue(priceValue).exists());
            Assertions.assertFalse(
                    price.withValue(new BigDecimal("0.00")).exists());

            LocalDate dateValue = LocalDate.of(2026, 5, 28);
            ext.findDatePicker().withId("date").setValue(dateValue);
            var date = ext.findDatePicker().withId("date");
            Assertions.assertTrue(date.withValue(dateValue).exists());
            Assertions.assertFalse(
                    date.withValue(LocalDate.of(1999, 12, 31)).exists());

            var accept = ext.findCheckbox().withId("accept");
            Assertions.assertTrue(accept.withValue(false).exists());
            Assertions.assertFalse(accept.withValue(true).exists());

            ext.findCheckbox().withId("accept").click();
            Assertions.assertTrue(accept.withValue(true).exists());
            Assertions.assertFalse(accept.withValue(false).exists());
        }

        @Test
        void grid_typedRowAccessor() {
            ext.navigate(LocatorDemoView.class);

            Person first = ext.findGrid(Person.class).getRow(0);

            Assertions.assertEquals("Alice", first.name());
            Assertions.assertEquals(3, ext.findGrid(Person.class).size());
        }

        @Test
        void singleLocator_reusedAfterUiChange_reresolves() {
            ext.navigate(LocatorDemoView.class);

            var save = ext.findButton().withText("Save");
            ext.findTextField().withId("name").setValue("first");
            save.click();

            // Resolution caches across calls in one chain. After a UI change
            // that could replace the component, invalidate() rewinds the
            // cache.
            ext.findTextField().withId("name").setValue("second");
            save.invalidate().click();

            Assertions.assertEquals("Saved: second",
                    ext.findSpan().withId("echo").getComponent().getText());
        }

        @Test
        void customLocator_viaGetSupplier_composesBuiltins() {
            ext.navigate(LocatorDemoView.class);

            ext.find(PersonFormLocator::new).fillIn("Ada", "ada@example.com");
            ext.find(PersonFormLocator::new).submit();

            Assertions.assertEquals("Submitted: Ada <ada@example.com>",
                    ext.findSpan().withId("echo").getComponent().getText());
        }

        @Test
        void filterChain_withLabel_selectsField() {
            ext.navigate(LocatorDemoView.class);

            // Outer TextField has label "Name"; inner PersonForm has
            // "Full name" / "Email address" — labels are unique at the top
            // level so withLabel resolves to exactly one match.
            ext.findTextField().withLabel("Name").setValue("via label");
            Assertions.assertEquals("via label",
                    ext.findTextField().withId("name").component().getValue());

            // Substring match against the inner form's label.
            ext.findTextField().withLabelContaining("Full").setValue("inner");
            Assertions.assertEquals("inner", ext.findTextField()
                    .withId("pf-name").component().getValue());
        }

        @Test
        void filterChain_withAriaLabel_selectsButton() {
            ext.navigate(LocatorDemoView.class);

            ext.findTextField().withId("name").setValue("X");

            // The Clear button identifies itself to screen readers via
            // aria-label="Reset form".
            ext.findButton().withAriaLabel("Reset form").click();
            Assertions.assertEquals("",
                    ext.findTextField().withId("name").component().getValue());

            // Substring match against the same attribute.
            ext.findTextField().withId("name").setValue("X");
            ext.findButton().withAriaLabelContaining("Reset").click();
            Assertions.assertEquals("",
                    ext.findTextField().withId("name").component().getValue());
        }

        @Test
        void filterChain_withTestId_selectsButton() {
            ext.navigate(LocatorDemoView.class);

            ext.findTextField().withId("name").setValue("Ada");

            // The Save button is tagged with data-testid="save-button" via
            // Component#setTestId.
            ext.findButton().withTestId("save-button").click();
            Assertions.assertEquals("Saved: Ada",
                    ext.findSpan().withId("echo").component().getText());
        }

        @Test
        void filterChain_expandedSurface() {
            ext.navigate(LocatorDemoView.class);

            // withAttribute — every component with an id has the "id"
            // attribute set on its element.
            Assertions.assertTrue(
                    ext.findTextField().withAttribute("id", "name").exists());

            // withCondition — typed predicate against the matched type.
            ext.findButton().withCondition(b -> "Save".equals(b.getText()))
                    .click();
            Assertions.assertEquals("Saved: ",
                    ext.findSpan().withId("echo").component().getText());
        }

        @Test
        void filterChain_escapeHatch_unaryOperator() {
            ext.navigate(LocatorDemoView.class);

            // Use the escape hatch to compose a ComponentQuery-only filter
            // (withPropertyValue is not exposed on Locator directly).
            ext.findButton()
                    .with(q -> q.withPropertyValue(Button::getText, "Clear"))
                    .click();

            // Save button was untouched; Clear emptied the name field.
            Assertions.assertEquals("",
                    ext.findTextField().withId("name").component().getValue());
        }

        @Test
        void filterChain_escapeHatch_returnsDifferentQuery() {
            ext.navigate(LocatorDemoView.class);

            ext.findTextField().withId("name").setValue("Ada");

            // UnaryOperator returns a fresh query — its contract is `T
            // apply(T)`,
            // so the locator must adopt the returned instance rather than
            // discarding it. The fresh query has no caption filter; only the
            // Save button matches the original Span query state we ignore by
            // returning a new query targeting Save.
            ext.findButton().with(
                    q -> new ComponentQuery<>(Button.class).withCaption("Save"))
                    .click();

            Assertions.assertEquals("Saved: Ada",
                    ext.find(Span.class).withId("echo").single().getText());
        }

        @Test
        void exists_truePathAndFalsePath() {
            ext.navigate(LocatorDemoView.class);

            Assertions.assertTrue(ext.findTextField().withId("name").exists(),
                    "filter chain matching a real component returns true");
            Assertions.assertFalse(
                    ext.findTextField().withId("does-not-exist").exists(),
                    "filter chain matching nothing returns false");
        }

        @Test
        void components_returnsAllMatchesAndKeepsLocatorReusable() {
            ext.navigate(LocatorDemoView.class);

            var buttons = ext.findButton();
            // Save, Clear, plus PersonForm's Submit.
            Assertions.assertEquals(3, buttons.components().size());

            // components() bypasses the single-match cache, so the same
            // instance can still resolve a specific pick afterwards.
            Assertions.assertEquals("Save",
                    buttons.atIndex(1).component().getText());
        }

        @Test
        void inside_componentOverload_scopesToDescendants() {
            ext.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = ext
                    .find(LocatorDemoView.PersonForm.class).single();

            // Globally there are 3 buttons in the view; scoping to the
            // form's descendants narrows the match down to the single
            // Submit button inside the composite.
            Assertions.assertEquals(3, ext.findButton().components().size());
            Assertions.assertEquals(1,
                    ext.findButton().inside(form).components().size());
        }

        @Test
        void inside_locatorOverload_evaluatesParentLazily() {
            ext.navigate(LocatorDemoView.class);

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
            var childLoc = ext.findButton().inside(formLoc);
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

        @Test
        void inside_locatorOverload_rejectsSelfReference() {
            ext.navigate(LocatorDemoView.class);

            var loc = ext.findButton();
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> loc.inside(loc));
        }

        @Test
        void use_seedsLocatorWithComponent_actionWorks() {
            ext.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = ext
                    .find(LocatorDemoView.PersonForm.class).single();

            // Caller holds direct references to the composite's children —
            // use(...) skips the filter chain and seeds the locator with
            // each instance.
            ext.use(form.nameField).setValue("Ada");
            ext.use(form.emailField).setValue("ada@example.com");
            ext.use(form.submit).click();

            Assertions.assertEquals("Submitted: Ada <ada@example.com>",
                    ext.findSpan().withId("echo").getText());
        }

        @Test
        void use_componentAndExistsReturnSeededInstance() {
            ext.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = ext
                    .find(LocatorDemoView.PersonForm.class).single();

            var loc = ext.use(form.submit);
            Assertions.assertSame(form.submit, loc.component());
            Assertions.assertEquals(List.of(form.submit), loc.components());
            Assertions.assertTrue(loc.exists());
            // invalidate() rewinds the cache; re-resolution still matches
            // the same instance because the identity predicate is sticky.
            Assertions.assertSame(form.submit, loc.invalidate().component());
        }

        @Test
        void use_additionalFilterCanExcludeSeededComponent() {
            ext.navigate(LocatorDemoView.class);

            LocatorDemoView.PersonForm form = ext
                    .find(LocatorDemoView.PersonForm.class).single();

            // form.submit's id is "pf-submit"; an extra withId("nope")
            // filter composes on top of the identity predicate and excludes
            // the only matching component.
            var loc = ext.use(form.submit).withId("nope");
            Assertions.assertFalse(loc.exists(),
                    "incompatible filter must zero out the seeded match");
            Assertions.assertThrows(NoSuchElementException.class,
                    loc::component);
        }

        @Test
        void use_genericTarget_carriesTypeArg() {
            ext.navigate(LocatorDemoView.class);

            // The demo view has a single Grid<Person>. Get a typed reference
            // via the typed find entry, then exercise use(Grid<T>) to bind
            // the locator to that instance.
            Grid<Person> grid = ext.findGrid(Person.class).getComponent();
            Person first = ext.use(grid).getRow(0);
            Assertions.assertEquals("Alice", first.name());
        }

        @Test
        void filterChain_escapeHatch_nullReturnThrows() {
            ext.navigate(LocatorDemoView.class);

            // Returning null from the operator is a contract violation —
            // fail loudly rather than silently dropping the operator's
            // intent.
            IllegalStateException ex = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> ext.findButton().with(q -> null));
            Assertions.assertTrue(ex.getMessage().contains("non-null"),
                    "message should explain the contract: " + ex.getMessage());
        }

        @Test
        void atIndex_picksNthMatch() {
            ext.navigate(LocatorDemoView.class);

            // The demo view has multiple buttons (Save, Clear, plus Submit
            // inside the PersonForm composite). atIndex is 1-based, so
            // atIndex(2) targets Clear.
            ext.findTextField().withId("name").setValue("X");
            ext.findButton().atIndex(2).click();
            Assertions.assertEquals("",
                    ext.findTextField().withId("name").component().getValue());
        }

        @Test
        void atIndex_stickyAcrossFilterSteps_butClearedByInvalidate() {
            ext.navigate(LocatorDemoView.class);

            // The demo view has three buttons total: Save (1), Clear (2),
            // PersonForm's Submit (3). atIndex(2) picks Clear.
            var btn = ext.findButton().atIndex(2);
            Assertions.assertEquals("Clear", btn.component().getText());

            // atIndex is part of the filter chain — narrowing further
            // does NOT drop the pick. Once the chain is narrowed to a
            // single match, atIndex(2) on a single-match query throws.
            btn.withText("Clear");
            Assertions.assertThrows(IndexOutOfBoundsException.class,
                    btn::component,
                    "pickIndex stays sticky across filter steps");

            // invalidate() is the explicit rewind hatch: clears the
            // cached resolution AND pickIndex. After it, resolution
            // falls back to single-match, which succeeds.
            Assertions.assertEquals("Clear",
                    btn.invalidate().component().getText());
        }

        @Test
        void invalidate_clearsAtIndexPick_soNextComponentCallExpectsSingleMatch() {
            ext.navigate(LocatorDemoView.class);

            var btn = ext.findButton().atIndex(1);
            Assertions.assertTrue(btn.exists());
            Assertions.assertNotNull(btn.component());

            btn.invalidate();

            // The filter chain itself is intact, so exists() still
            // sees the three buttons. But component() now falls back
            // to single() because invalidate() rewound the atIndex
            // pick — that is the documented "single match expected"
            // contract until the caller re-applies atIndex(int).
            Assertions.assertTrue(btn.exists());
            Assertions.assertThrows(NoSuchElementException.class,
                    btn::component);
        }

        @Test
        void invalidate_preservesFilterSetResultsSize() {
            ext.navigate(LocatorDemoView.class);

            var btn = ext.findButton().with(q -> q.withResultsSize(1));
            Assertions.assertThrows(AssertionError.class, btn::exists,
                    "count=(1,1) on 3 matches must fail");

            btn.invalidate();

            // invalidate() rewinds resolution state but not the
            // filter chain. A user-set results-size constraint is
            // part of the chain and must survive.
            Assertions.assertThrows(AssertionError.class, btn::exists,
                    "invalidate() must preserve user-set results-size");
        }

        @Test
        void singleResolutionDoesNotLeakCountConstraint() {
            ext.navigate(LocatorDemoView.class);

            // withId narrows to (0, 1). component() routes through
            // single() → find(); the (1, 1) it forces internally
            // must not leak into the persistent query spec.
            var loc = ext.findTextField().withId("name");
            Assertions.assertNotNull(loc.component());

            // Narrow the chain to zero matches. With (0, 1)
            // preserved, exists() simply returns false. With a
            // leaked (1, 1), the count check throws.
            loc.withLabel("definitely-no-such-caption-12345");
            Assertions.assertFalse(loc.exists(),
                    "find() must not leak (1, 1) into the filter chain");
        }

        @Test
        void findSupplier_nullReturnThrows() {
            ext.navigate(LocatorDemoView.class);

            // Same contract as Locator.with: a null Locator from the factory
            // is a contract violation that should surface immediately.
            IllegalStateException ex = Assertions.assertThrows(
                    IllegalStateException.class, () -> ext.find(() -> null));
            Assertions.assertTrue(ex.getMessage().contains("non-null"),
                    "message should explain the contract: " + ex.getMessage());
        }
    }

    @Nested
    class FriendlyFailureOnMissingEnvironment {

        BrowserlessExtension ext = new BrowserlessExtension();

        @Test
        void findButton_withoutActiveExtension_throwsSetupException() {
            // Extension is constructed but NOT registered with
            // @RegisterExtension, so doInit() was never called.
            // activateLocatorContext() must trigger the friendly
            // "environment is not initialized" check before the locator tries
            // to resolve a UI.
            Assertions.assertThrows(BrowserlessTestSetupException.class,
                    () -> ext.findButton().withText("Save").click());
        }
    }
}
