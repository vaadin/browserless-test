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
import com.vaadin.browserless.expectList
import com.vaadin.browserless.internal.ElementUtils._fireDomEvent
import com.vaadin.browserless.internal.ElementUtils.clearSlot
import com.vaadin.browserless.internal.ElementUtils.getChildrenInSlot
import com.vaadin.browserless.internal.ElementUtils.getVirtualChildren
import com.vaadin.browserless.internal.ElementUtils.insertBefore
import com.vaadin.browserless.internal.ElementUtils.setOrRemoveAttribute
import com.vaadin.browserless.internal.ElementUtils.textRecursively2
import com.vaadin.browserless.internal.ElementUtils.toggle
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.dom.Element
import com.vaadin.flow.internal.JacksonUtils
import kotlin.test.expect

fun DynaNodeGroup.elementUtilsTests() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("setOrRemoveAttribute") {
        val t = Div().element
        expect(null) { t.getAttribute("foo") }
        setOrRemoveAttribute(t, "foo", "bar")
        expect("bar") { t.getAttribute("foo") }
        setOrRemoveAttribute(t, "foo", null)
        expect(null) { t.getAttribute("foo") }
    }

    group("toggle class name") {
        test("add") {
            val t = Div()
            toggle(t.classNames, "test")
            expect(setOf("test")) { t.classNames }
        }
        test("remove") {
            val t = Div()
            t.classNames.add("test")
            toggle(t.classNames, "test")
            expect(setOf<String>()) { t.classNames }
        }
    }

    test("insertBefore") {
        val l = Div().element
        val first: Element = Span("first").element
        l.appendChild(first)
        val second: Element = Span("second").element
        insertBefore(l, second, first)
        expect("second, first") { l.children.toList().joinToString { it.text } }
        insertBefore(l, Span("third").element, first)
        expect("second, third, first") { l.children.toList().joinToString { it.text } }
    }

    test("textRecursively2") {
        expect("foo") { textRecursively2(Span("foo").element) }
        expect("foobarbaz") {
            val div = Div()
            div.add(Span("foo"), Text("bar"), Paragraph("baz"))
            textRecursively2(div.element)
        }
        expect("foo") { textRecursively2(Element("div").apply { setProperty("innerHTML", "foo") }) }
    }

    group("getVirtualChildren()") {
        test("initially empty") {
            expectList<Element>() { getVirtualChildren(Div().element) }
            expectList<Element>() { getVirtualChildren(Span().element) }
            expectList<Element>() {
                val b = Button()
                UI.getCurrent().add(b)
                getVirtualChildren(b.element)
            }
        }
        test("add virtual child") {
            val span = Span().element
            val parent = Div()
            parent.element.appendVirtualChild(span)
            expectList(span) { getVirtualChildren(parent.element) }
        }
    }

    test("getChildrenInSlot") {
        expectList<Element>() { getChildrenInSlot(TextField().element, "prefix") }
        val div = Div()
        expectList(div.element) { getChildrenInSlot(TextField().apply { prefixComponent = div }.element, "prefix") }
    }

    test("clearSlot") {
        val tf = TextField()
        tf.prefixComponent = Div()
        clearSlot(tf.element, "prefix")
        expectList<Element>() { getChildrenInSlot(tf.element, "prefix") }
        expect(null) { tf.prefixComponent }
    }

    test("fireDomEvent() smoke") {
        val element = Div().element
        _fireDomEvent(element, DomEvent(element, "click", JacksonUtils.createObjectNode()))
    }

}
