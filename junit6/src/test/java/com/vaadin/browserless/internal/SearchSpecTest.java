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

import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchSpecTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void clazz_matchesOnlyTheGivenType() {
        Predicate<Component> p = new SearchSpec<>(Button.class).toPredicate();
        assertTrue(p.test(new Button()));
        assertFalse(p.test(new Span()));
    }

    @Test
    void id_matchesTheComponentId() {
        SearchSpec<Component> spec = new SearchSpec<>(Component.class);
        spec.setId("25");
        Predicate<Component> p = spec.toPredicate();

        Button matching = new Button();
        matching.setId("25");
        assertTrue(p.test(matching));

        Button other = new Button();
        other.setId("42");
        assertFalse(p.test(other));

        assertFalse(p.test(new Button()));
    }

    @Test
    void caption_resolvedAcrossEveryComponentKindThatHasOne() {
        SearchSpec<Component> spec = new SearchSpec<>(Component.class);
        spec.setCaption("foo");
        Predicate<Component> p = spec.toPredicate();

        assertTrue(p.test(new Button("foo")));
        assertFalse(p.test(new Button("bar")));
        assertFalse(p.test(new Button()));

        assertTrue(p.test(new Checkbox("foo")));
        assertFalse(p.test(new Checkbox("bar")));
        assertFalse(p.test(new Checkbox()));

        assertTrue(p.test(labelled(new CheckboxGroup<Integer>(), "foo")));
        assertFalse(p.test(labelled(new CheckboxGroup<Integer>(), "bar")));
        assertFalse(p.test(new CheckboxGroup<Integer>()));

        assertTrue(p.test(labelled(new Select<Integer>(), "foo")));
        assertFalse(p.test(labelled(new Select<Integer>(), "bar")));
        assertFalse(p.test(new Select<Integer>()));

        assertTrue(p.test(labelled(new ListBox<Integer>(), "foo")));
        assertFalse(p.test(labelled(new ListBox<Integer>(), "bar")));
        assertFalse(p.test(new ListBox<Integer>()));

        assertTrue(p.test(labelled(new RadioButtonGroup<Integer>(), "foo")));
        assertFalse(p.test(labelled(new RadioButtonGroup<Integer>(), "bar")));
        assertFalse(p.test(new RadioButtonGroup<Integer>()));

        // tests CustomField
        assertTrue(p.test(labelled(new StringCustomField(), "foo")));
        assertFalse(p.test(labelled(new StringCustomField(), "bar")));
        assertFalse(p.test(new StringCustomField()));

        assertTrue(p.test(labelled(new Input(), "foo")));
        assertFalse(p.test(labelled(new Input(), "bar")));
        assertFalse(p.test(new Input()));

        assertTrue(p.test(new TextField("foo")));
        assertFalse(p.test(new TextField("bar")));
        assertFalse(p.test(new TextField()));

        assertTrue(p.test(new TextArea("foo")));
        assertFalse(p.test(new TextArea("bar")));
        assertFalse(p.test(new TextArea()));

        assertTrue(p.test(new TimePicker("foo")));
        assertFalse(p.test(new TimePicker("bar")));
        assertFalse(p.test(new TimePicker()));

        assertTrue(p.test(new DatePicker("foo")));
        assertFalse(p.test(new DatePicker("bar")));
        assertFalse(p.test(new DatePicker()));

        assertTrue(p.test(new ComboBox<Integer>("foo")));
        assertFalse(p.test(new ComboBox<Integer>("bar")));
        assertFalse(p.test(new ComboBox<Integer>()));
    }

    @Test
    void text_matchesTheElementText() {
        SearchSpec<Component> spec = new SearchSpec<>(Component.class);
        spec.setText("foo");
        Predicate<Component> p = spec.toPredicate();

        assertTrue(p.test(new Button("foo")));
        assertFalse(p.test(new Button("bar")));
        assertFalse(p.test(new Button()));

        assertTrue(p.test(new Text("foo")));
        assertFalse(p.test(new Text("bar")));
        assertFalse(p.test(new Text("")));
    }

    @Test
    void predicates_customPredicateIsApplied() {
        SearchSpec<Component> spec = new SearchSpec<>(Component.class);
        spec.getPredicates().add(c -> c instanceof Button);
        Predicate<Component> p = spec.toPredicate();

        assertTrue(p.test(new Button()));
        assertFalse(p.test(new Span()));
    }

    private static <T extends Component> T labelled(T component, String label) {
        ComponentUtils.label(component, label);
        return component;
    }

    /**
     * Stands in for any {@link CustomField}, to check that a caption is
     * resolved for one.
     */
    private static class StringCustomField extends CustomField<String> {
        @Override
        protected String generateModelValue() {
            return "";
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
        }
    }
}
