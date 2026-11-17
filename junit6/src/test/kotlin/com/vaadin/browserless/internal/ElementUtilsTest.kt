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
import com.vaadin.browserless.expectList
import kotlin.test.expect

fun DynaNodeGroup.elementUtilsTests() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("setOrRemoveAttribute") {
        val t = Div().element
        expect(null) { t.getAttribute("foo") }
        ElementUtils.setOrRemoveAttribute(t, "foo", "bar")
        expect("bar") { t.getAttribute("foo") }
        ElementUtils.setOrRemoveAttribute(t, "foo", null)
        expect(null) { t.getAttribute("foo") }
    }

    group("toggle class name") {
        test("add") {
            val t = Div()
            ElementUtils.toggle(t.classNames, "test")
            expect(setOf("test")) { t.classNames }
        }
        test("remove") {
            val t = Div()
            t.classNames.add("test")
            ElementUtils.toggle(t.classNames, "test")
            expect(setOf<String>()) { t.classNames }
        }
    }

    test("insertBefore") {
        val l = Div().element
        val first: Element = Span("first").element
        l.appendChild(first)
        val second: Element = Span("second").element
        ElementUtils.insertBefore(l, second, first)
        expect("second, first") { l.children.toList().joinToString { it.text } }
        ElementUtils.insertBefore(l, Span("third").element, first)
        expect("second, third, first") { l.children.toList().joinToString { it.text } }
    }

    test("textRecursively2") {
        expect("foo") { ElementUtils.getTextRecursively2(Span("foo").element) }
        expect("foobarbaz") {
            val div = Div()
            div.add(Span("foo"), Text("bar"), Paragraph("baz"))
            ElementUtils.getTextRecursively2(div.element)
        }
        expect("foo") { ElementUtils.getTextRecursively2(Element("div").apply { setProperty("innerHTML", "foo") }) }
    }

    group("getVirtualChildren()") {
        test("initially empty") {
            expectList() { ElementUtils.getVirtualChildren(Div().element) }
            expectList() { ElementUtils.getVirtualChildren(Span().element) }
            expectList() {
                val b = Button()
                UI.getCurrent().add(b)
                ElementUtils.getVirtualChildren(b.element)
            }
        }
        test("add virtual child") {
            val span = Span().element
            val parent = Div()
            parent.element.appendVirtualChild(span)
            expectList(span) { ElementUtils.getVirtualChildren(parent.element) }
        }
    }

    test("getChildrenInSlot") {
        expectList() { ElementUtils.getChildrenInSlot(TextField().element, "prefix") }
        val div = Div()
        expectList(div.element) { ElementUtils.getChildrenInSlot(TextField().apply { prefixComponent = div } .element, "prefix") }
    }

    test("clearSlot") {
        val tf = TextField()
        tf.prefixComponent = Div()
        ElementUtils.clearSlot(tf.element, "prefix")
        expectList() { ElementUtils.getChildrenInSlot(tf.element, "prefix") }
        expect(null) { tf.prefixComponent }
    }

    test("fireDomEvent() smoke") {
        val element = Div().element
        ElementUtils._fireDomEvent(element, DomEvent(element, "click", JacksonUtils.createObjectNode()))
    }

}
