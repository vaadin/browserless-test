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

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link Locator} with a {@link TestingLifecycleHook} installed, so
 * that every lookup can additionally be checked to have run the hook.
 */
class LocatorTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
        TestingLifecycleHooks.setCurrent(new MyLifecycleHook());
    }

    @AfterEach
    void tearDown() {
        TestingLifecycleHooks.setCurrent(TestingLifecycleHook.DEFAULT);
        MockVaadin.tearDown();
    }

    @Nested
    class Get {

        @Test
        void get_noComponentMatches_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._get(new Button(), TextField.class));
            expectAfterLookupCalled();
        }

        @Test
        void get_multipleComponentsMatch_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._get(nestedVerticalLayouts(UI.getCurrent()),
                            VerticalLayout.class));
            expectAfterLookupCalled();
        }

        @Test
        void get_containerItselfMatches_returnsTheContainer() {
            Button button = new Button();
            assertSame(button, Locator._get(button, Button.class));
            expectAfterLookupCalled();
        }

        @Test
        void get_nestedComponentMatches_returnsIt() {
            Button button = new Button();
            assertSame(button,
                    Locator._get(new VerticalLayout(button), Button.class));
            expectAfterLookupCalled();
        }

        @Test
        void get_componentIsInvisible_fails() {
            Button button = new Button();
            button.setVisible(false);
            assertThrows(AssertionError.class,
                    () -> Locator._get(button, Button.class));
            expectAfterLookupCalled();
        }
    }

    @Nested
    class Find {

        @Test
        void find_idSpec_returnsOnlyTheComponentWithThatId() {
            Button button = new Button();
            BasicUtils.id_(button, "foo");
            VerticalLayout layout = new VerticalLayout(button, new Button());

            assertEquals(List.of(button), Locator._find(layout, Button.class,
                    spec -> spec.setId("foo")));
            expectAfterLookupCalled();
        }

        @Test
        void find_componentIsInvisible_isNotReturned() {
            Button button = new Button();
            button.setVisible(false);

            assertEquals(List.of(), Locator._find(button, Button.class));
            expectAfterLookupCalled();
        }

        @Test
        void find_nestedComponents_returnsContainerAndDescendants() {
            VerticalLayout layout = nestedVerticalLayouts(null);

            assertEquals(2, Locator._find(layout, VerticalLayout.class).size());
            expectAfterLookupCalled();
        }
    }

    @Nested
    class ExpectNone {

        @Test
        void expectNone_noComponentMatches_passes() {
            Locator._expectNone(new Button(), TextField.class);
            expectAfterLookupCalled();
        }

        @Test
        void expectNone_multipleComponentsMatch_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expectNone(
                            nestedVerticalLayouts(UI.getCurrent()),
                            VerticalLayout.class));
            expectAfterLookupCalled();
        }

        @Test
        void expectNone_containerItselfMatches_fails() {
            Button button = new Button();
            assertThrows(AssertionError.class,
                    () -> Locator._expectNone(button, Button.class));
            expectAfterLookupCalled();
        }

        @Test
        void expectNone_nestedComponentMatches_fails() {
            VerticalLayout layout = new VerticalLayout(new Button());
            assertThrows(AssertionError.class,
                    () -> Locator._expectNone(layout, Button.class));
            expectAfterLookupCalled();
        }
    }

    @Test
    void simpleUi_lookupsFindTheComponentsAUserWouldSee() {
        VerticalLayout layout = new VerticalLayout();
        TextField name = new TextField("Type your name here:");
        Button button = new Button("Click Me");
        button.addClickListener(event -> layout
                .add(new Text("Thanks " + name.getValue() + ", it works!")));
        layout.add(name, button);
        UI.getCurrent().add(layout);

        Locator._get(TextField.class,
                spec -> spec.setCaption("Type your name here:"))
                .setValue("Baron Vladimir Harkonnen");
        expectAfterLookupCalled();

        Locator._get(Button.class, spec -> spec.setCaption("Click Me")).click();
        expectAfterLookupCalled();

        assertEquals("Thanks Baron Vladimir Harkonnen, it works!", Locator._get(
                Text.class,
                // Since Vaadin 25.1 a Button contains a Text child
                spec -> spec.getPredicates().add(text -> text.getParent()
                        .filter(parent -> parent instanceof Button).isEmpty()))
                .getText());
        expectAfterLookupCalled();

        // lookup by value
        Locator._get(TextField.class,
                spec -> spec.setValue("Baron Vladimir Harkonnen"));
        expectAfterLookupCalled();

        assertEquals("Thanks Baron Vladimir Harkonnen, it works!",
                ((Text) layout.getChildren().toList().getLast()).getText());
        assertEquals(3, layout.getComponentCount());
    }

    @Nested
    class Matcher {

        @Test
        void id_matchesOnlyTheExactId() {
            assertTrue(matches(new Button(), spec -> {
            }));
            assertFalse(matches(new Button(), spec -> spec.setId("a")));
            assertTrue(matches(withId(new Button(), "a"), spec -> {
            }));
            assertTrue(matches(withId(new Button(), "a"),
                    spec -> spec.setId("a")));
            assertFalse(matches(withId(new Button(), "a b"),
                    spec -> spec.setId("a")));
            assertFalse(matches(withId(new Button(), "a"),
                    spec -> spec.setId("a b")));
        }

        @Test
        void caption_matchesOnlyTheExactCaption() {
            assertTrue(matches(new Button("click me"),
                    spec -> spec.setCaption("click me")));
            assertTrue(matches(new TextField("name:"),
                    spec -> spec.setCaption("name:")));
            assertTrue(matches(new Button("click me"), spec -> {
            }));
            assertTrue(matches(new TextField("name:"), spec -> {
            }));
            assertFalse(matches(new Button("click me"),
                    spec -> spec.setCaption("Click Me")));
            assertFalse(matches(new TextField("name:"),
                    spec -> spec.setCaption("Name")));
        }

        @Test
        void placeholder_matchesOnlyTheExactPlaceholder() {
            assertTrue(matches(new TextField("name", "the name"),
                    spec -> spec.setPlaceholder("the name")));
            assertTrue(matches(
                    new PasswordField("password", "at least 6 characters"),
                    spec -> spec.setPlaceholder("at least 6 characters")));
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setPlaceholder("foo");
            assertTrue(matches(comboBox, spec -> spec.setPlaceholder("foo")));
            assertFalse(matches(new TextField("name", "the name"),
                    spec -> spec.setPlaceholder("name")));
            assertFalse(matches(
                    new PasswordField("password", "at least 6 characters"),
                    spec -> spec.setPlaceholder("password")));
        }

        @Test
        void value_matchesTheComponentValue() {
            assertTrue(matches(new TextField(null, "Mannerheim", "placeholder"),
                    spec -> spec.setValue("Mannerheim")));
            // "Mannerheim" is the caption here, not the value
            assertFalse(matches(new TextField("Mannerheim"),
                    spec -> spec.setValue("Mannerheim")));
        }

        @Test
        void classes_matchWhenAllOfThemArePresent() {
            assertTrue(matches(withClasses(), spec -> {
            }));
            assertTrue(matches(withClasses(), spec -> spec.setClasses("a")));
            assertTrue(matches(withClasses(), spec -> spec.setClasses("b")));
            assertTrue(matches(withClasses(), spec -> spec.setClasses("a b")));
            assertFalse(matches(withClasses(), spec -> spec.setClasses("a c")));
            assertFalse(matches(withClasses(), spec -> spec.setClasses("c")));
        }

        @Test
        void withoutClasses_matchWhenNoneOfThemIsPresent() {
            assertTrue(matches(withClasses(), spec -> {
            }));
            assertFalse(matches(withClasses(),
                    spec -> spec.setWithoutClasses("a")));
            assertFalse(matches(withClasses(),
                    spec -> spec.setWithoutClasses("b")));
            assertFalse(matches(withClasses(),
                    spec -> spec.setWithoutClasses("a b")));
            assertFalse(matches(withClasses(),
                    spec -> spec.setWithoutClasses("a c")));
            assertTrue(matches(withClasses(),
                    spec -> spec.setWithoutClasses("c")));
        }

        @Test
        void predicates_mustAllAccept() {
            assertTrue(matches(new Button(), spec -> {
            }));
            assertFalse(matches(new Button(),
                    spec -> spec.getPredicates().add(component -> false)));
        }

        @Test
        void themes_matchWhenAllOfThemArePresent() {
            assertTrue(matches(withThemes(), spec -> {
            }));
            assertTrue(matches(withThemes(),
                    spec -> spec.setThemes("custom-theme")));
            assertTrue(
                    matches(withThemes(), spec -> spec.setThemes("my-theme")));
            assertTrue(matches(withThemes(),
                    spec -> spec.setThemes("custom-theme my-theme")));
            assertFalse(
                    matches(withThemes(), spec -> spec.setThemes("no-theme")));
        }

        @Test
        void withoutThemes_matchWhenNoneOfThemIsPresent() {
            assertTrue(matches(withThemes(), spec -> {
            }));
            assertFalse(matches(withThemes(),
                    spec -> spec.setWithoutThemes("custom-theme")));
            assertFalse(matches(withThemes(),
                    spec -> spec.setWithoutThemes("my-theme")));
            assertFalse(matches(withThemes(),
                    spec -> spec.setWithoutThemes("custom-theme my-theme")));
            assertTrue(matches(withThemes(),
                    spec -> spec.setWithoutThemes("no-theme")));
        }

        private Button withClasses() {
            Button button = new Button();
            button.addClassNames("a", "b");
            return button;
        }

        private Button withThemes() {
            Button button = new Button();
            button.addThemeNames("custom-theme", "my-theme");
            return button;
        }

        private Button withId(Button button, String id) {
            BasicUtils.id_(button, id);
            return button;
        }

        /**
         * Checks whether the component matches the given spec. All rules are
         * matched except the count rule, and only against the component itself,
         * not against its children.
         */
        private boolean matches(Component component,
                Consumer<SearchSpec<Component>> block) {
            SearchSpec<Component> spec = new SearchSpec<>(Component.class);
            block.accept(spec);
            return spec.toPredicate().test(component);
        }
    }

    @Nested
    class UnmockedEnvironment {

        @BeforeEach
        void unmock() {
            MockVaadin.tearDown();
            TestingLifecycleHooks.setCurrent(TestingLifecycleHook.DEFAULT);
        }

        @Test
        void get_noMockedEnvironment_stillLooksUpComponents() {
            Locator._get(new Button(), Button.class);
            assertThrows(AssertionError.class,
                    () -> Locator._get(new Button(), TextField.class));
        }
    }

    @Nested
    class ExpectOne {

        @Test
        void expectOne_uiHasNoMatchingComponent_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expectOne(Button.class));
            expectAfterLookupCalled();
        }

        @Test
        void expectOne_containerHasNoMatchingComponent_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expectOne(new Button(), Span.class));
            expectAfterLookupCalled();
        }

        @Test
        void expectOne_multipleComponentsMatch_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expectOne(
                            nestedVerticalLayouts(UI.getCurrent()),
                            VerticalLayout.class));
            expectAfterLookupCalled();
        }

        @Test
        void expectOne_containerItselfMatches_passes() {
            Locator._expectOne(new Button(), Button.class);
            expectAfterLookupCalled();
        }

        @Test
        void expectOne_nestedComponentMatches_passes() {
            Locator._expectOne(new VerticalLayout(new Button()), Button.class);
            expectAfterLookupCalled();
        }

        @Test
        void expectOne_specDoesNotMatch_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expectOne(new Button("foo"), Button.class,
                            spec -> spec.setCaption("bar")));
            expectAfterLookupCalled();
        }
    }

    @Nested
    class Expect {

        @Test
        void expect_uiHasNoMatchingComponent_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expect(Span.class));
            expectAfterLookupCalled();
        }

        @Test
        void expect_containerHasNoMatchingComponent_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expect(new Button(), Span.class));
            expectAfterLookupCalled();
        }

        @Test
        void expect_zeroExpectedAndNoneMatch_passes() {
            Locator._expect(Span.class, 0);
            expectAfterLookupCalled();
            Locator._expect(new Button(), Span.class, 0);
        }

        @Test
        void expect_countIsWrong_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expect(
                            nestedVerticalLayouts(UI.getCurrent()),
                            VerticalLayout.class));
            expectAfterLookupCalled();
        }

        @Test
        void expect_countIsRight_passes() {
            Locator._expect(nestedVerticalLayouts(UI.getCurrent()),
                    VerticalLayout.class, 2);
            expectAfterLookupCalled();
        }

        @Test
        void expect_containerItselfMatches_passes() {
            Locator._expect(new Button(), Button.class, 1);
            expectAfterLookupCalled();
        }

        @Test
        void expect_nestedComponentMatches_passes() {
            Locator._expect(new VerticalLayout(new Button()), Button.class, 1);
            expectAfterLookupCalled();
        }

        @Test
        void expect_specDoesNotMatch_fails() {
            assertThrows(AssertionError.class,
                    () -> Locator._expect(new Button("foo"), Button.class, 1,
                            spec -> spec.setCaption("bar")));
            expectAfterLookupCalled();
        }
    }

    /**
     * Builds a {@link VerticalLayout} holding another one, optionally attaching
     * it to the given parent.
     */
    private static VerticalLayout nestedVerticalLayouts(HasComponents parent) {
        VerticalLayout outer = new VerticalLayout();
        outer.add(new VerticalLayout());
        if (parent != null) {
            parent.add(outer);
        }
        return outer;
    }

    /**
     * Asserts that the last lookup ran both halves of the lifecycle hook, then
     * installs a fresh hook for the next lookup.
     */
    private static void expectAfterLookupCalled() {
        MyLifecycleHook hook = (MyLifecycleHook) TestingLifecycleHooks
                .getCurrent();
        assertTrue(hook.beforeLookupCalled,
                "awaitBeforeLookup() has not been called");
        assertTrue(hook.afterLookupCalled,
                "awaitAfterLookup() has not been called");
        TestingLifecycleHooks.setCurrent(new MyLifecycleHook());
    }

    /**
     * A hook that records whether it has been called, and fails loudly when a
     * lookup calls it out of order or twice.
     */
    private static class MyLifecycleHook implements TestingLifecycleHook {

        private boolean beforeLookupCalled;

        private boolean afterLookupCalled;

        @Override
        public void awaitBeforeLookup() {
            if (beforeLookupCalled) {
                throw new IllegalStateException(
                        "awaitBeforeLookup() has been already called");
            }
            if (afterLookupCalled) {
                throw new IllegalStateException(
                        "awaitAfterLookup() has been already called");
            }
            beforeLookupCalled = true;
        }

        @Override
        public void awaitAfterLookup() {
            if (!beforeLookupCalled) {
                throw new IllegalStateException(
                        "awaitBeforeLookup() has not yet been called");
            }
            if (afterLookupCalled) {
                throw new IllegalStateException(
                        "awaitAfterLookup() has been already called");
            }
            afterLookupCalled = true;
        }
    }
}
