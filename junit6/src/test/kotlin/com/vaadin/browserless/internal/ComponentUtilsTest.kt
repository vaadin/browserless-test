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
package com.vaadin.browserless.internal

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.expectThrows
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.shared.SlotUtils
import com.vaadin.flow.component.orderedlayout.FlexLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import kotlin.test.expect

fun DynaNodeGroup.componentUtilsTests() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    group("removeFromParent()") {
        test("component with no parent") {
            val t = Text("foo")
            t.removeFromParent()
            expect(null) { t.parent.orElse(null) }
        }
        test("nested component") {
            val fl = FlexLayout().apply { add(Span("foo")) }
            val label = fl.getComponentAt(0)
            expect(fl) { label.parent.get() }
            label.removeFromParent()
            expect(null) { label.parent.orElse(null) }
            expect(0) { fl.componentCount }
        }
        test("reattach") {
            val fl = FlexLayout().apply { add(Span("foo")) }
            val label = fl.getComponentAt(0)
            label.removeFromParent()
            fl.add(label)
            expect(fl) { label.parent.orElse(null) }
            expect(1) { fl.componentCount }
        }
    }

    test("serverClick()") {
        val b = Button()
        var clicked = 0
        b.addClickListener { clicked++ }
        ComponentUtils.serverClick(b, true, 0, 1, false, false, false, false)
        expect(1) { clicked }
    }

    test("tooltip") {
        val b = Button()
        expect(null) { b.tooltip.text }
        b.setTooltipText("")
        expect<String?>("") { b.tooltip.text } // https://youtrack.jetbrains.com/issue/KT-32501
        b.setTooltipText("foo")
        expect<String?>("foo") { b.tooltip.text } // https://youtrack.jetbrains.com/issue/KT-32501
        b.setTooltipText(null)
        expect(null) { b.tooltip.text }
    }

    test("addContextMenuListener smoke") {
        ComponentUtils.addContextMenuListener(Button(), {})
    }

    group("findAncestorOrSelf") {
        test("null on no parent") {
            expect(null) { ComponentUtils.findAncestorOrSelf(Button()) { false } }
        }
        test("null on no acceptance") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(null) { ComponentUtils.findAncestorOrSelf(button) { false } }
        }
        test("finds self") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(button) { ComponentUtils.findAncestorOrSelf(button) { true } }
        }
    }

    test("isNestedIn") {
        expect(false) { ComponentUtils.isNestedIn(Button(), UI.getCurrent()) }
        val button = Button()
        UI.getCurrent().add(button)
        expect(true) { ComponentUtils.isNestedIn(button, UI.getCurrent()) }
    }

    test("isAttached") {
        expect(true) { UI.getCurrent().isAttached() }
        expect(false) { Button("foo").isAttached() }
        expect(true) {
            val button = Button()
            UI.getCurrent().add(button)
            button.isAttached()
        }
        UI.getCurrent().close()
        expect(true) { UI.getCurrent().isAttached() }
    }

    test("insertBefore") {
        val l = HorizontalLayout()
        val first = Span("first")
        l.addComponentAsFirst(first)
        val second = Span("second")
        ComponentUtils.insertBefore(l, second, first)
        expect("second, first") { l.children.toList().map { BasicUtils.get_text(it) } .joinToString() }
        ComponentUtils.insertBefore(l, Span("third"), first)
        expect("second, third, first") { l.children.toList().map { BasicUtils.get_text(it) } .joinToString() }
    }

    test("hasChildren") {
        val l = HorizontalLayout()
        expect(false) { ComponentUtils.getHasChildren(l) }
        l.addComponentAsFirst(Span("first"))
        expect(true) { ComponentUtils.getHasChildren(l) }
        l.removeAll()
        expect(false) { ComponentUtils.getHasChildren(l) }
    }

    /*
    test("isNotEmpty") {
        val l = HorizontalLayout()
        expect(false) { l.isNotEmpty }
        l.addComponentAsFirst(Span("first"))
        expect(true) { l.isNotEmpty }
        l.removeAll()
        expect(false) { l.isNotEmpty }
    }

    test("isEmpty") {
        val l = HorizontalLayout()
        expect(true) { l.isEmpty }
        l.addComponentAsFirst(Span("first"))
        expect(false) { l.isEmpty }
        l.removeAll()
        expect(true) { l.isEmpty }
    }
     */

    group("classnames2") {
        test("addClassNames2") {
            val div = Div().apply { ComponentUtils.addClassNames2(this, "foo  bar    baz") }
            expect(true) {
                div.classNames.containsAll(listOf("foo", "bar", "baz"))
            }
        }
        test("addClassNames2(vararg)") {
            val div = Div().apply { ComponentUtils.addClassNames2(this, "foo  bar    baz", "  one  two") }
            expect(true) {
                div.classNames.containsAll(listOf("foo", "bar", "baz", "one", "two"))
            }
        }
        test("setClassNames2") {
            val div = Div().apply { ComponentUtils.addClassNames2(this, "foo  bar    baz", "  one  two") }
            ComponentUtils.setClassNames2(div, "  three four  ")
            expect(true) {
                div.classNames.containsAll(listOf("three", "four"))
            }
        }
        test("setClassNames2(vararg)") {
            val div = Div().apply { ComponentUtils.addClassNames2(this, "foo  bar    baz", "  one  two") }
            ComponentUtils.setClassNames2(div, "  three ", "four  ")
            expect(true) {
                div.classNames.containsAll(listOf("three", "four"))
            }
        }
        test("removeClassNames2") {
            val div = Div().apply { ComponentUtils.addClassNames2(this, "foo  bar    baz", "  one  two") }
            ComponentUtils.removeClassNames2(div, "  bar baz  ")
            expect(true) {
                div.classNames.containsAll(listOf("foo", "one", "two"))
            }
        }
        test("removeClassNames2(vararg)") {
            val div = Div().apply { ComponentUtils.addClassNames2(this, "foo  bar    baz", "  one  two") }
            ComponentUtils.removeClassNames2(div, "  bar ", "baz  ")
            expect(true) {
                div.classNames.containsAll(listOf("foo", "one", "two"))
            }
        }
    }

    test("placeholder") {
        var c: Component = TextField().apply { placeholder = "foo" }
        expect("foo") { ComponentUtils.getPlaceholder(c) }
        ComponentUtils.setPlaceholder(c, "")
        expect("") { ComponentUtils.getPlaceholder(c) }
        c = TextArea().apply { placeholder = "foo" }
        expect("foo") { ComponentUtils.getPlaceholder(c) }
        ComponentUtils.setPlaceholder(c, "")
        expect("") { ComponentUtils.getPlaceholder(c) }
        c = Button() // doesn't support placeholder
        expect(null) { ComponentUtils.getPlaceholder(c) }
        expectThrows(IllegalStateException::class, "Button doesn't support setting placeholder") {
            ComponentUtils.setPlaceholder(c, "foo")
        }
    }

    group("label") {
        test("TextField") {
            val c: Component = TextField()
            expect("") { ComponentUtils.getLabel(c) }
            ComponentUtils.setLabel(c, "foo")
            expect("foo") { ComponentUtils.getLabel(c) }
            ComponentUtils.setLabel(c, "")
            expect("") { ComponentUtils.getLabel(c) }
        }
        test("Checkbox") {
            val c: Component = Checkbox()
            expect("") { ComponentUtils.getLabel(c) }
            ComponentUtils.setLabel(c, "foo")
            expect("foo") { ComponentUtils.getLabel(c) }
            ComponentUtils.setLabel(c, "")
            expect("") { ComponentUtils.getLabel(c) }
        }
    }

    @Suppress("DEPRECATION")
    test("caption") {
        var c: Component = Button("foo")
        expect("foo") { ComponentUtils.getCaption(c) }
        ComponentUtils.setCaption(c, "")
        expect("") { ComponentUtils.getCaption(c) }
        c = Checkbox().apply { ComponentUtils.setCaption(this, "foo") }
        expect("foo") { ComponentUtils.getCaption(c) }
        ComponentUtils.setCaption(c, "")
        expect("") { ComponentUtils.getCaption(c) }
        expect("") { ComponentUtils.getLabel(FormLayout.FormItem()) }
        val fl = FormLayout()
        c = fl.addFormItem(Button(), "foo")
        expect("foo") { ComponentUtils.getCaption(c) }
        val item = fl.addFormItem(Button(), Div(Span("Last")))
        SlotUtils.addToSlot(item, "label", Span("  name "))
        expect("Last name") { ComponentUtils.getCaption(item) }
    }
}
