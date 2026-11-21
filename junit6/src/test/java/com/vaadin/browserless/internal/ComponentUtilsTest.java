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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.SlotUtils;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentUtilsTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void getPlaceholder_resolvedForFieldsThatHaveOneAndNullForOthers() {
        TextField textField = new TextField();
        textField.setPlaceholder("foo");
        assertEquals("foo", ComponentUtils.getPlaceholder(textField));
        textField.setPlaceholder("");
        assertEquals("", ComponentUtils.getPlaceholder(textField));
        TextArea textArea = new TextArea();
        textArea.setPlaceholder("foo");
        assertEquals("foo", ComponentUtils.getPlaceholder(textArea));
        // doesn't support placeholder
        assertNull(ComponentUtils.getPlaceholder(new Button()));
    }

    @Nested
    class GetLabel {

        @Test
        void textField_set_readBack() {
            TextField c = new TextField();
            assertEquals("", ComponentUtils.getLabel(c));
            c.setLabel("foo");
            assertEquals("foo", ComponentUtils.getLabel(c));
        }

        @Test
        void checkbox_set_readBack() {
            Checkbox c = new Checkbox();
            assertEquals("", ComponentUtils.getLabel(c));
            c.setLabel("foo");
            assertEquals("foo", ComponentUtils.getLabel(c));
        }

        @Test
        void formItem_labelSlot_isItsTrimmedTextFollowedByTheRestOfTheLabel() {
            assertEquals("",
                    ComponentUtils.getLabel(new FormLayout.FormItem()));
            FormLayout.FormItem item = new FormLayout()
                    .addFormItem(new Button(), new Div(new Span("Last")));
            SlotUtils.addToSlot(item, "label", new Span("  name "));
            assertEquals("Last name", ComponentUtils.getLabel(item));
        }
    }

    @Test
    void getCaption_resolvesToTheButtonTextTheFieldLabelAndTheFormItemLabel() {
        assertEquals("foo", ComponentUtils.getCaption(new Button("foo")));
        assertEquals("", ComponentUtils.getCaption(new Button()));
        assertEquals("foo", ComponentUtils.getCaption(new Checkbox("foo")));
        assertEquals("", ComponentUtils.getCaption(new Checkbox()));
        assertEquals("foo", ComponentUtils
                .getCaption(new FormLayout().addFormItem(new Button(), "foo")));
    }
}
