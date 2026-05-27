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

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.expectThrows
import com.vaadin.browserless.internal.BasicUtils._text
import com.vaadin.browserless.internal.ComponentUtils.addClassNames2
import com.vaadin.browserless.internal.ComponentUtils.addContextMenuListener
import com.vaadin.browserless.internal.ComponentUtils.caption
import com.vaadin.browserless.internal.ComponentUtils.findAncestor
import com.vaadin.browserless.internal.ComponentUtils.findAncestorOrSelf
import com.vaadin.browserless.internal.ComponentUtils.hasChildren
import com.vaadin.browserless.internal.ComponentUtils.insertBefore
import com.vaadin.browserless.internal.ComponentUtils.isAttached
import com.vaadin.browserless.internal.ComponentUtils.isNestedIn
import com.vaadin.browserless.internal.ComponentUtils.label
import com.vaadin.browserless.internal.ComponentUtils.placeholder
import com.vaadin.browserless.internal.ComponentUtils.removeClassNames2
import com.vaadin.browserless.internal.ComponentUtils.removeFromParent
import com.vaadin.browserless.internal.ComponentUtils.serverClick
import com.vaadin.browserless.internal.ComponentUtils.setClassNames2
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
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
            removeFromParent(t)
            expect(null) { t.parent.orElse(null) }
        }
        test("nested component") {
            val fl = FlexLayout().apply { add(Span("foo")) }
            val label = fl.getComponentAt(0)
            expect(fl) { label.parent.get() }
            removeFromParent(label)
            expect(null) { label.parent.orElse(null) }
            expect(0) { fl.componentCount }
        }
        test("reattach") {
            val fl = FlexLayout().apply { add(Span("foo")) }
            val label = fl.getComponentAt(0)
            removeFromParent(label)
            fl.add(label)
            expect(fl) { label.parent.orElse(null) }
            expect(1) { fl.componentCount }
        }
    }

    test("serverClick()") {
        val b = Button()
        var clicked = 0
        b.addClickListener { clicked++ }
        serverClick(b)
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
        addContextMenuListener(Button(), {})
    }

    group("findAncestor") {
        test("null on no parent") {
            expect(null) { findAncestor(Button()) { false } }
        }
        test("null on no acceptance") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(null) { findAncestor(button) { false } }
        }
        test("finds UI") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(UI.getCurrent()) { findAncestor(button) { it is UI } }
        }
        test("doesn't find self") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(UI.getCurrent()) { findAncestor(button) { true } }
        }
    }

    group("findAncestorOrSelf") {
        test("null on no parent") {
            expect(null) { findAncestorOrSelf(Button()) { false } }
        }
        test("null on no acceptance") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(null) { findAncestorOrSelf(button) { false } }
        }
        test("finds self") {
            val button = Button()
            UI.getCurrent().add(button)
            expect(button) { findAncestorOrSelf(button) { true } }
        }
    }

    test("isNestedIn") {
        expect(false) { isNestedIn(Button(), UI.getCurrent()) }
        val button = Button()
        UI.getCurrent().add(button)
        expect(true) { isNestedIn(button, UI.getCurrent()) }
    }

    test("isAttached") {
        expect(true) { isAttached(UI.getCurrent()) }
        expect(false) { isAttached(Button("foo")) }
        expect(true) {
            val button = Button()
            UI.getCurrent().add(button)
            isAttached(button)
        }
        UI.getCurrent().close()
        expect(true) { isAttached(UI.getCurrent()) }
    }

    test("insertBefore") {
        val l = HorizontalLayout()
        val first = Span("first")
        l.addComponentAsFirst(first)
        val second = Span("second")
        insertBefore(l, second, first)
        expect("second, first") { l.children.toList().map { _text(it) } .joinToString() }
        insertBefore(l, Span("third"), first)
        expect("second, third, first") { l.children.toList().map { _text(it) } .joinToString() }
    }

    test("hasChildren") {
        val l = HorizontalLayout()
        expect(false) { hasChildren(l) }
        l.addComponentAsFirst(Span("first"))
        expect(true) { hasChildren(l) }
        l.removeAll()
        expect(false) { hasChildren(l) }
    }

    group("classnames2") {
        test("addClassNames2") {
            val div = Div().apply { addClassNames2(this, "foo  bar    baz") }
            expect(true) {
                div.classNames.containsAll(listOf("foo", "bar", "baz"))
            }
        }
        test("addClassNames2(vararg)") {
            val div = Div().apply { addClassNames2(this, "foo  bar    baz", "  one  two") }
            expect(true) {
                div.classNames.containsAll(listOf("foo", "bar", "baz", "one", "two"))
            }
        }
        test("setClassNames2") {
            val div = Div().apply { addClassNames2(this, "foo  bar    baz", "  one  two") }
            setClassNames2(div, "  three four  ")
            expect(true) {
                div.classNames.containsAll(listOf("three", "four"))
            }
        }
        test("setClassNames2(vararg)") {
            val div = Div().apply { addClassNames2(this, "foo  bar    baz", "  one  two") }
            setClassNames2(div, "  three ", "four  ")
            expect(true) {
                div.classNames.containsAll(listOf("three", "four"))
            }
        }
        test("removeClassNames2") {
            val div = Div().apply { addClassNames2(this, "foo  bar    baz", "  one  two") }
            removeClassNames2(div, "  bar baz  ")
            expect(true) {
                div.classNames.containsAll(listOf("foo", "one", "two"))
            }
        }
        test("removeClassNames2(vararg)") {
            val div = Div().apply { addClassNames2(this, "foo  bar    baz", "  one  two") }
            removeClassNames2(div, "  bar ", "baz  ")
            expect(true) {
                div.classNames.containsAll(listOf("foo", "one", "two"))
            }
        }
    }

    test("placeholder") {
        var c: Component = TextField().apply { setPlaceholder("foo") }
        expect("foo") { placeholder(c) }
        placeholder(c, "")
        expect("") { placeholder(c) }
        c = TextArea().apply { setPlaceholder("foo") }
        expect("foo") { placeholder(c) }
        placeholder(c, "")
        expect("") { placeholder(c) }
        c = Button() // doesn't support placeholder
        expect(null) { placeholder(c) }
        expectThrows(IllegalStateException::class, "Button doesn't support setting placeholder") {
            placeholder(c, "foo")
        }
    }

    group("label") {
        test("TextField") {
            val c: Component = TextField()
            expect("") { label(c) }
            label(c, "foo")
            expect("foo") { label(c) }
            label(c, "")
            expect("") { label(c) }
        }
        test("Checkbox") {
            val c: Component = Checkbox()
            expect("") { label(c) }
            label(c, "foo")
            expect("foo") { label(c) }
            label(c, "")
            expect("") { label(c) }
        }
    }

    test("caption") {
        var c: Component = Button("foo")
        expect("foo") { caption(c) }
        caption(c, "")
        expect("") { caption(c) }
        c = Checkbox().also { caption(it, "foo") }
        expect("foo") { caption(c) }
        caption(c, "")
        expect("") { caption(c) }
        expect("") { label(FormLayout.FormItem()) }
        val fl = FormLayout()
        c = fl.addFormItem(Button(), "foo")
        expect("foo") { caption(c) }
    }

    test("Button.caption") {
        val c = Button("foo")
        expect("foo") { caption(c) }
        caption(c, "")
        expect("") { caption(c) }
    }
}
