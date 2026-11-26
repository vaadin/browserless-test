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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;

import static com.vaadin.browserless.TestAssertions.expectThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicUtilsTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Nested
    class CheckEditableByUser {

        @Test
        void disabledField_throws() {
            AttachedTextField tf = new AttachedTextField();
            tf.setEnabled(false);
            expectThrows(IllegalStateException.class,
                    "The AttachedTextField\\[DISABLED,.*] is not enabled",
                    () -> BasicUtils.checkEditableByUser(tf));
        }

        @Test
        void invisibleField_throws() {
            AttachedTextField tf = new AttachedTextField();
            tf.setVisible(false);
            expectThrows(IllegalStateException.class,
                    "The AttachedTextField\\[INVIS,.*] is not effectively visible",
                    () -> BasicUtils.checkEditableByUser(tf));
        }

        @Test
        void detachedField_throws() {
            expectThrows(IllegalStateException.class,
                    "The TextField\\[.*] is not attached",
                    () -> BasicUtils.checkEditableByUser(new TextField()));
        }

        @Test
        void fieldInInvisibleLayout_throws() {
            VerticalLayout layout = new VerticalLayout();
            layout.setVisible(false);
            TextField tf = new TextField();
            layout.add(tf);
            expectThrows(IllegalStateException.class,
                    "The TextField\\[.*] is not effectively visible",
                    () -> BasicUtils.checkEditableByUser(tf));
        }

        @Test
        void usableField_passes() {
            BasicUtils.checkEditableByUser(new AttachedTextField());
        }
    }

    @Nested
    class ExpectNotEditableByUser {

        @Test
        void disabledField_passes() {
            AttachedTextField tf = new AttachedTextField();
            tf.setEnabled(false);
            BasicUtils.expectNotEditableByUser(tf);
        }

        @Test
        void invisibleField_passes() {
            AttachedTextField tf = new AttachedTextField();
            tf.setVisible(false);
            BasicUtils.expectNotEditableByUser(tf);
        }

        @Test
        void fieldInInvisibleLayout_passes() {
            VerticalLayout layout = new VerticalLayout();
            layout.setVisible(false);
            TextField tf = new TextField();
            layout.add(tf);
            BasicUtils.expectNotEditableByUser(tf);
        }

        @Test
        void usableField_throws() {
            expectThrows(AssertionError.class,
                    "The AttachedTextField\\[.*] is editable", () -> BasicUtils
                            .expectNotEditableByUser(new AttachedTextField()));
        }
    }

    @Nested
    class FireDomEvent {

        @Test
        void plainDiv_doesNotThrow() {
            BasicUtils._fireDomEvent(new Div(), "click");
        }

        @Test
        void domListener_isCalled() {
            Div div = new Div();
            AtomicReference<DomEvent> event = new AtomicReference<>();
            div.getElement().addEventListener("click", event::set);
            BasicUtils._fireDomEvent(div, "click");
            assertEquals("click", event.get().getType());
        }

        @Test
        void clickListener_receivesTheEventDataFromTheClient() {
            Div div = new Div();
            AtomicReference<ClickEvent<Div>> event = new AtomicReference<>();
            div.addClickListener(event::set);
            ObjectNode data = JacksonUtils.createObjectNode();
            data.put("event.screenX", 20.0);
            BasicUtils._fireDomEvent(div, "click", data);
            assertEquals(20, event.get().getScreenX());
            assertTrue(event.get().isFromClient());
        }
    }

    @Test
    void focus_attachedField_firesFocusEvent() {
        AttachedTextField f = new AttachedTextField();
        AtomicBoolean called = new AtomicBoolean();
        f.addFocusListener(e -> called.set(true));
        BasicUtils._focus(f);
        assertTrue(called.get());
    }

    @Test
    void blur_attachedField_firesBlurEvent() {
        AttachedTextField f = new AttachedTextField();
        AtomicBoolean called = new AtomicBoolean();
        f.addBlurListener(e -> called.set(true));
        BasicUtils._blur(f);
        assertTrue(called.get());
    }
}
