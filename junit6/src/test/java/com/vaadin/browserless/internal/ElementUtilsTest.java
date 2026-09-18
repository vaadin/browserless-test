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
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ElementUtilsTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void setOrRemoveAttribute_nullValue_removesTheAttribute() {
        Element t = new Div().getElement();
        assertNull(t.getAttribute("foo"));
        ElementUtils.setOrRemoveAttribute(t, "foo", "bar");
        assertEquals("bar", t.getAttribute("foo"));
        ElementUtils.setOrRemoveAttribute(t, "foo", null);
        assertNull(t.getAttribute("foo"));
    }

    @Nested
    class ToggleClassName {

        @Test
        void absentClassName_isAdded() {
            Div t = new Div();
            ElementUtils.toggle(t.getClassNames(), "test");
            assertEquals(Set.of("test"), t.getClassNames());
        }

        @Test
        void presentClassName_isRemoved() {
            Div t = new Div();
            t.getClassNames().add("test");
            ElementUtils.toggle(t.getClassNames(), "test");
            assertEquals(Set.of(), t.getClassNames());
        }
    }

    @Test
    void insertBefore_existingChild_insertsAheadOfIt() {
        Element l = new Div().getElement();
        Element first = new Span("first").getElement();
        l.appendChild(first);
        Element second = new Span("second").getElement();
        ElementUtils.insertBefore(l, second, first);
        assertEquals("second, first", childTexts(l));
        ElementUtils.insertBefore(l, new Span("third").getElement(), first);
        assertEquals("second, third, first", childTexts(l));
    }

    @Test
    void textRecursively2_nestedElementsAndInnerHtml_concatenatesAllText() {
        assertEquals("foo",
                ElementUtils.textRecursively2(new Span("foo").getElement()));

        Div div = new Div();
        div.add(new Span("foo"), new Text("bar"), new Paragraph("baz"));
        assertEquals("foobarbaz",
                ElementUtils.textRecursively2(div.getElement()));

        Element raw = new Element("div");
        raw.setProperty("innerHTML", "foo");
        assertEquals("foo", ElementUtils.textRecursively2(raw));
    }

    @Nested
    class GetVirtualChildren {

        @Test
        void noVirtualChildren_returnsEmptyList() {
            assertEquals(List.of(),
                    ElementUtils.getVirtualChildren(new Div().getElement()));
            assertEquals(List.of(),
                    ElementUtils.getVirtualChildren(new Span().getElement()));
            Button b = new Button();
            UI.getCurrent().add(b);
            assertEquals(List.of(),
                    ElementUtils.getVirtualChildren(b.getElement()));
        }

        @Test
        void appendedVirtualChild_isReturned() {
            Element span = new Span().getElement();
            Div parent = new Div();
            parent.getElement().appendVirtualChild(span);
            assertEquals(List.of(span),
                    ElementUtils.getVirtualChildren(parent.getElement()));
        }
    }

    @Test
    void getChildrenInSlot_slottedComponent_returnsIt() {
        assertEquals(List.of(), ElementUtils
                .getChildrenInSlot(new TextField().getElement(), "prefix"));

        Div div = new Div();
        TextField tf = new TextField();
        tf.setPrefixComponent(div);
        assertEquals(List.of(div.getElement()),
                ElementUtils.getChildrenInSlot(tf.getElement(), "prefix"));
    }

    @Test
    void clearSlot_slottedComponent_removesIt() {
        TextField tf = new TextField();
        tf.setPrefixComponent(new Div());
        ElementUtils.clearSlot(tf.getElement(), "prefix");
        assertEquals(List.of(),
                ElementUtils.getChildrenInSlot(tf.getElement(), "prefix"));
        assertNull(tf.getPrefixComponent());
    }

    @Test
    void fireDomEvent_plainElement_doesNotThrow() {
        Element element = new Div().getElement();
        ElementUtils._fireDomEvent(element, new DomEvent(element, "click",
                JacksonUtils.createObjectNode()));
    }

    private static String childTexts(Element parent) {
        return parent.getChildren().map(Element::getText)
                .collect(Collectors.joining(", "));
    }
}
