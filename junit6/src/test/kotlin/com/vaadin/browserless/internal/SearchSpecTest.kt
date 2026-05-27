/**
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal

import java.util.function.Predicate
import kotlin.test.expect
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.checkbox.CheckboxGroup
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.html.Input
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.listbox.ListBox
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.timepicker.TimePicker
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.karibudsl.v10.DateRangePopup

@DynaTestDsl
internal fun DynaNodeGroup.searchSpecTest() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("clazz") {
        val spec = SearchSpec(Button::class.java)
        expect(true) { spec.toPredicate().test(Button()) }
        expect(false) { spec.toPredicate().test(Span()) }
    }

    test("id") {
        val spec = SearchSpec(Component::class.java).apply { id = "25" }
        expect(true) { spec.toPredicate().test(Button().apply { setId("25") }) }
        expect(false) { spec.toPredicate().test(Button().apply { setId("42") }) }
        expect(false) { spec.toPredicate().test(Button()) }
    }

    test("caption") {
        val spec = SearchSpec(Component::class.java).apply { caption = "foo" }
        expect(true) { spec.toPredicate().test(Button("foo")) }
        expect(false) { spec.toPredicate().test(Button("bar")) }
        expect(false) { spec.toPredicate().test(Button()) }
        expect(true) { spec.toPredicate().test(Checkbox("foo")) }
        expect(false) { spec.toPredicate().test(Checkbox("bar")) }
        expect(false) { spec.toPredicate().test(Checkbox()) }
        expect(true) {
            spec.toPredicate().test(CheckboxGroup<Int>().apply {
                label = "foo"
            })
        }
        expect(false) {
            spec.toPredicate().test(CheckboxGroup<Int>().apply {
                label = "bar"
            })
        }
        expect(false) { spec.toPredicate().test(CheckboxGroup<Int>()) }
        expect(true) {
            spec.toPredicate().test(Select<Int>().apply {
                label = "foo"
            })
        }
        expect(false) {
            spec.toPredicate().test(Select<Int>().apply {
                label = "bar"
            })
        }
        expect(false) { spec.toPredicate().test(Select<Int>()) }
        expect(true) {
            spec.toPredicate().test(ListBox<Int>().also {
                ComponentUtils.label(it, "foo")
            })
        }
        expect(false) {
            spec.toPredicate().test(ListBox<Int>().also {
                ComponentUtils.label(it, "bar")
            })
        }
        expect(false) { spec.toPredicate().test(ListBox<Int>()) }
        expect(true) {
            spec.toPredicate().test(RadioButtonGroup<Int>().apply {
                label = "foo"
            })
        }
        expect(false) {
            spec.toPredicate().test(RadioButtonGroup<Int>().apply {
                label = "bar"
            })
        }
        expect(false) { spec.toPredicate().test(RadioButtonGroup<Int>()) }
        // tests CustomField
        expect(true) {
            spec.toPredicate().test(DateRangePopup().apply {
                label = "foo"
            })
        }
        expect(false) {
            spec.toPredicate().test(DateRangePopup().apply {
                label = "bar"
            })
        }
        expect(false) { spec.toPredicate().test(DateRangePopup()) }

        expect(true) { spec.toPredicate().test(Input().also { ComponentUtils.label(it, "foo") }) }
        expect(false) { spec.toPredicate().test(Input().also { ComponentUtils.label(it, "bar") }) }
        expect(false) { spec.toPredicate().test(Input()) }
        expect(true) { spec.toPredicate().test(TextField("foo")) }
        expect(false) { spec.toPredicate().test(TextField("bar")) }
        expect(false) { spec.toPredicate().test(TextField()) }
        expect(true) { spec.toPredicate().test(TextArea("foo")) }
        expect(false) { spec.toPredicate().test(TextArea("bar")) }
        expect(false) { spec.toPredicate().test(TextArea()) }
        expect(true) { spec.toPredicate().test(TimePicker("foo")) }
        expect(false) { spec.toPredicate().test(TimePicker("bar")) }
        expect(false) { spec.toPredicate().test(TimePicker()) }
        expect(true) { spec.toPredicate().test(DatePicker("foo")) }
        expect(false) { spec.toPredicate().test(DatePicker("bar")) }
        expect(false) { spec.toPredicate().test(DatePicker()) }
        expect(true) { spec.toPredicate().test(ComboBox<Int>("foo")) }
        expect(false) { spec.toPredicate().test(ComboBox<Int>("bar")) }
        expect(false) { spec.toPredicate().test(ComboBox<Int>()) }
    }

    test("text") {
        val spec = SearchSpec(Component::class.java).apply { text = "foo" }
        expect(true) { spec.toPredicate().test(Button("foo")) }
        expect(false) { spec.toPredicate().test(Button("bar")) }
        expect(false) { spec.toPredicate().test(Button()) }
        expect(true) { spec.toPredicate().test(Text("foo")) }
        expect(false) { spec.toPredicate().test(Text("bar")) }
        expect(false) { spec.toPredicate().test(Text("")) }
    }

    test("predicates") {
        var spec = SearchSpec(Component::class.java).apply {
            predicates.add(Predicate { it is Button })
        }
        expect(true) { spec.toPredicate().test(Button()) }
        expect(false) { spec.toPredicate().test(Span()) }
    }
}
