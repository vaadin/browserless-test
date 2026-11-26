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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void getTextIncludingInnerHtml_nestedElementsAndInnerHtml_concatenatesAllText() {
        assertEquals("foo", ElementUtils
                .getTextIncludingInnerHtml(new Span("foo").getElement()));

        Div div = new Div();
        div.add(new Span("foo"), new Text("bar"), new Paragraph("baz"));
        assertEquals("foobarbaz",
                ElementUtils.getTextIncludingInnerHtml(div.getElement()));

        Element raw = new Element("div");
        raw.setProperty("innerHTML", "foo");
        assertEquals("foo", ElementUtils.getTextIncludingInnerHtml(raw));
    }

    @Test
    void fireDomEvent_plainElement_doesNotThrow() {
        Element element = new Div().getElement();
        ElementUtils._fireDomEvent(element, new DomEvent(element, "click",
                JacksonUtils.createObjectNode()));
    }
}
