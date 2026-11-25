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
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.dom.Element
import com.vaadin.flow.internal.JacksonUtils
import kotlin.test.expect
import com.vaadin.browserless.internal.ElementUtils._fireDomEvent
import com.vaadin.browserless.internal.ElementUtils.getTextIncludingInnerHtml

fun DynaNodeGroup.elementUtilsTests() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("getTextIncludingInnerHtml") {
        expect("foo") { getTextIncludingInnerHtml(Span("foo").element) }
        expect("foobarbaz") {
            val div = Div()
            div.add(Span("foo"), Text("bar"), Paragraph("baz"))
            getTextIncludingInnerHtml(div.element)
        }
        expect("foo") { getTextIncludingInnerHtml(Element("div").apply { setProperty("innerHTML", "foo") }) }
    }

    test("fireDomEvent() smoke") {
        val element = Div().element
        _fireDomEvent(element, DomEvent(element, "click", JacksonUtils.createObjectNode()))
    }
}
