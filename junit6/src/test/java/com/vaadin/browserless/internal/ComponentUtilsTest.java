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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import static com.vaadin.browserless.TestAssertions.expectThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentUtilsTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Nested
    class RemoveFromParent {

        @Test
        void componentWithNoParent_doesNothing() {
            Text t = new Text("foo");
            ComponentUtils.removeFromParent(t);
            assertNull(t.getParent().orElse(null));
        }

        @Test
        void nestedComponent_isDetachedFromItsParent() {
            FlexLayout fl = new FlexLayout();
            fl.add(new Span("foo"));
            Component label = fl.getComponentAt(0);
            assertSame(fl, label.getParent().get());
            ComponentUtils.removeFromParent(label);
            assertNull(label.getParent().orElse(null));
            assertEquals(0, fl.getComponentCount());
        }

        @Test
        void removedComponent_canBeReattached() {
            FlexLayout fl = new FlexLayout();
            fl.add(new Span("foo"));
            Component label = fl.getComponentAt(0);
            ComponentUtils.removeFromParent(label);
            fl.add(label);
            assertSame(fl, label.getParent().orElse(null));
            assertEquals(1, fl.getComponentCount());
        }
    }

    @Test
    void serverClick_firesTheClickListener() {
        Button b = new Button();
        AtomicInteger clicked = new AtomicInteger();
        b.addClickListener(e -> clicked.incrementAndGet());
        ComponentUtils.serverClick(b);
        assertEquals(1, clicked.get());
    }

    @Test
    void tooltip_setAndCleared_readBack() {
        Button b = new Button();
        assertNull(b.getTooltip().getText());
        b.setTooltipText("");
        assertEquals("", b.getTooltip().getText());
        b.setTooltipText("foo");
        assertEquals("foo", b.getTooltip().getText());
        b.setTooltipText(null);
        assertNull(b.getTooltip().getText());
    }

    @Test
    void addContextMenuListener_doesNotThrow() {
        ComponentUtils.addContextMenuListener(new Button(), e -> {
        });
    }

    @Nested
    class FindAncestor {

        @Test
        void noParent_returnsNull() {
            assertNull(ComponentUtils.findAncestor(new Button(), c -> false));
        }

        @Test
        void nothingAccepted_returnsNull() {
            Button button = new Button();
            UI.getCurrent().add(button);
            assertNull(ComponentUtils.findAncestor(button, c -> false));
        }

        @Test
        void acceptsTheUi_findsIt() {
            Button button = new Button();
            UI.getCurrent().add(button);
            assertSame(UI.getCurrent(),
                    ComponentUtils.findAncestor(button, c -> c instanceof UI));
        }

        @Test
        void acceptsEverything_skipsSelfAndFindsTheParent() {
            Button button = new Button();
            UI.getCurrent().add(button);
            assertSame(UI.getCurrent(),
                    ComponentUtils.findAncestor(button, c -> true));
        }
    }

    @Nested
    class FindAncestorOrSelf {

        @Test
        void noParent_returnsNull() {
            assertNull(ComponentUtils.findAncestorOrSelf(new Button(),
                    c -> false));
        }

        @Test
        void nothingAccepted_returnsNull() {
            Button button = new Button();
            UI.getCurrent().add(button);
            assertNull(ComponentUtils.findAncestorOrSelf(button, c -> false));
        }

        @Test
        void acceptsEverything_findsSelf() {
            Button button = new Button();
            UI.getCurrent().add(button);
            assertSame(button,
                    ComponentUtils.findAncestorOrSelf(button, c -> true));
        }
    }

    @Test
    void isNestedIn_onlyTrueOnceAdded() {
        assertFalse(ComponentUtils.isNestedIn(new Button(), UI.getCurrent()));
        Button button = new Button();
        UI.getCurrent().add(button);
        assertTrue(ComponentUtils.isNestedIn(button, UI.getCurrent()));
    }

    @Test
    void isAttached_trueOnceAddedAndStillTrueForAClosedUi() {
        assertTrue(ComponentUtils.isAttached(UI.getCurrent()));
        assertFalse(ComponentUtils.isAttached(new Button("foo")));
        Button button = new Button();
        UI.getCurrent().add(button);
        assertTrue(ComponentUtils.isAttached(button));
        UI.getCurrent().close();
        assertTrue(ComponentUtils.isAttached(UI.getCurrent()));
    }

    @Test
    void insertBefore_existingChild_insertsAheadOfIt() {
        HorizontalLayout l = new HorizontalLayout();
        Span first = new Span("first");
        l.addComponentAsFirst(first);
        ComponentUtils.insertBefore(l, new Span("second"), first);
        assertEquals("second, first", texts(l));
        ComponentUtils.insertBefore(l, new Span("third"), first);
        assertEquals("second, third, first", texts(l));
    }

    @Test
    void hasChildren_reflectsWhetherAnythingIsAdded() {
        HorizontalLayout l = new HorizontalLayout();
        assertFalse(ComponentUtils.hasChildren(l));
        l.addComponentAsFirst(new Span("first"));
        assertTrue(ComponentUtils.hasChildren(l));
        l.removeAll();
        assertFalse(ComponentUtils.hasChildren(l));
    }

    @Nested
    class ClassNames2 {

        @Test
        void addClassNames2_whitespaceSeparated_addsEachName() {
            Div div = new Div();
            ComponentUtils.addClassNames2(div, "foo  bar    baz");
            assertTrue(div.getClassNames()
                    .containsAll(List.of("foo", "bar", "baz")));
        }

        @Test
        void addClassNames2_severalStrings_addsEachName() {
            Div div = withClassNames();
            assertTrue(div.getClassNames()
                    .containsAll(List.of("foo", "bar", "baz", "one", "two")));
        }

        @Test
        void setClassNames2_whitespaceSeparated_replacesTheNames() {
            Div div = withClassNames();
            ComponentUtils.setClassNames2(div, "  three four  ");
            assertTrue(
                    div.getClassNames().containsAll(List.of("three", "four")));
        }

        @Test
        void setClassNames2_severalStrings_replacesTheNames() {
            Div div = withClassNames();
            ComponentUtils.setClassNames2(div, "  three ", "four  ");
            assertTrue(
                    div.getClassNames().containsAll(List.of("three", "four")));
        }

        @Test
        void removeClassNames2_whitespaceSeparated_removesEachName() {
            Div div = withClassNames();
            ComponentUtils.removeClassNames2(div, "  bar baz  ");
            assertTrue(div.getClassNames()
                    .containsAll(List.of("foo", "one", "two")));
        }

        @Test
        void removeClassNames2_severalStrings_removesEachName() {
            Div div = withClassNames();
            ComponentUtils.removeClassNames2(div, "  bar ", "baz  ");
            assertTrue(div.getClassNames()
                    .containsAll(List.of("foo", "one", "two")));
        }

        private Div withClassNames() {
            Div div = new Div();
            ComponentUtils.addClassNames2(div, "foo  bar    baz", "  one  two");
            return div;
        }
    }

    @Test
    void placeholder_resolvedForFieldsThatHaveOneAndRefusedForOthers() {
        TextField tf = new TextField();
        tf.setPlaceholder("foo");
        assertEquals("foo", ComponentUtils.placeholder(tf));
        ComponentUtils.placeholder(tf, "");
        assertEquals("", ComponentUtils.placeholder(tf));

        TextArea ta = new TextArea();
        ta.setPlaceholder("foo");
        assertEquals("foo", ComponentUtils.placeholder(ta));
        ComponentUtils.placeholder(ta, "");
        assertEquals("", ComponentUtils.placeholder(ta));

        Button b = new Button(); // doesn't support placeholder
        assertNull(ComponentUtils.placeholder(b));
        expectThrows(IllegalStateException.class,
                "Button doesn't support setting placeholder",
                () -> ComponentUtils.placeholder(b, "foo"));
    }

    @Nested
    class Label {

        @Test
        void textField_setAndCleared_readBack() {
            TextField c = new TextField();
            assertEquals("", ComponentUtils.label(c));
            ComponentUtils.label(c, "foo");
            assertEquals("foo", ComponentUtils.label(c));
            ComponentUtils.label(c, "");
            assertEquals("", ComponentUtils.label(c));
        }

        @Test
        void checkbox_setAndCleared_readBack() {
            Checkbox c = new Checkbox();
            assertEquals("", ComponentUtils.label(c));
            ComponentUtils.label(c, "foo");
            assertEquals("foo", ComponentUtils.label(c));
            ComponentUtils.label(c, "");
            assertEquals("", ComponentUtils.label(c));
        }
    }

    @Test
    void caption_resolvesToTheButtonTextTheFieldLabelAndTheFormItemLabel() {
        Button button = new Button("foo");
        assertEquals("foo", ComponentUtils.caption(button));
        ComponentUtils.caption(button, "");
        assertEquals("", ComponentUtils.caption(button));

        Checkbox checkbox = new Checkbox();
        ComponentUtils.caption(checkbox, "foo");
        assertEquals("foo", ComponentUtils.caption(checkbox));
        ComponentUtils.caption(checkbox, "");
        assertEquals("", ComponentUtils.caption(checkbox));

        assertEquals("", ComponentUtils.label(new FormLayout.FormItem()));
        FormLayout fl = new FormLayout();
        Component formItem = fl.addFormItem(new Button(), "foo");
        assertEquals("foo", ComponentUtils.caption(formItem));
    }

    @Test
    void caption_button_isItsText() {
        Button c = new Button("foo");
        assertEquals("foo", ComponentUtils.caption(c));
        ComponentUtils.caption(c, "");
        assertEquals("", ComponentUtils.caption(c));
    }

    private static String texts(Component parent) {
        return parent.getChildren().map(BasicUtils::_text)
                .collect(Collectors.joining(", "));
    }
}
