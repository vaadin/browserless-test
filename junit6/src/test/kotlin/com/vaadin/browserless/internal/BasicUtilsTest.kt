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
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.karibudsl.v10.textField
import com.vaadin.browserless.expectThrows
import com.vaadin.browserless.internal.BasicUtils._blur
import com.vaadin.browserless.internal.BasicUtils._fireDomEvent
import com.vaadin.browserless.internal.BasicUtils._focus
import com.vaadin.browserless.internal.BasicUtils.checkEditableByUser
import com.vaadin.browserless.internal.BasicUtils.expectNotEditableByUser
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.internal.JacksonUtils
import kotlin.test.expect

@DynaTestDsl
internal fun DynaNodeGroup.basicUtilsTestbatch() {

    group("checkEditableByUser") {
        test("disabled textfield fails") {
            expectThrows(java.lang.IllegalStateException::class, "The AttachedTextField\\[DISABLED,.*] is not enabled".toRegex()) {
                checkEditableByUser(AttachedTextField().apply { isEnabled = false })
            }
        }
        test("invisible textfield fails") {
            expectThrows(
                java.lang.IllegalStateException::class,
                "The AttachedTextField\\[INVIS,.*] is not effectively visible".toRegex()
            ) {
                checkEditableByUser(AttachedTextField().apply { isVisible = false })
            }
        }
        test("non attached textfield fails") {
            expectThrows(
                java.lang.IllegalStateException::class,
                "The TextField\\[.*] is not attached".toRegex()
            ) {
                checkEditableByUser(TextField())
            }
        }
        test("textfield in invisible layout fails") {
            expectThrows(java.lang.IllegalStateException::class, "The TextField\\[.*] is not effectively visible".toRegex()) {
                VerticalLayout().apply {
                    isVisible = false
                    textField().also { checkEditableByUser(it) }
                }
            }
        }
        test("textfield succeeds") {
            checkEditableByUser(AttachedTextField())
        }
    }

    group("expectNotEditableByUser") {
        test("disabled textfield fails") {
            expectNotEditableByUser(AttachedTextField().apply { isEnabled = false })
        }
        test("invisible textfield fails") {
            expectNotEditableByUser(AttachedTextField().apply { isVisible = false })
        }
        test("textfield in invisible layout fails") {
            VerticalLayout().apply {
                isVisible = false
                textField().also { expectNotEditableByUser(it) }
            }
        }
        test("textfield succeeds") {
            expectThrows(AssertionError::class, "The AttachedTextField\\[.*] is editable".toRegex()) {
                expectNotEditableByUser(AttachedTextField())
            }
        }
    }

    group("fireDomEvent()") {
        test("smoke") {
            _fireDomEvent(Div(), "click")
        }
        test("listeners are called") {
            val div = Div()
            lateinit var event: DomEvent
            div.element.addEventListener("click") { e -> event = e }
            _fireDomEvent(div, "click")
            expect("click") { event.type }
        }
        test("higher-level listeners are called") {
            val div = Div()
            lateinit var event: ClickEvent<Div>
            div.addClickListener { e -> event = e }
            _fireDomEvent(div, "click", JacksonUtils.createObjectNode().apply { put("event.screenX", 20.0) })
            expect(20) { event.screenX }
            expect(true) { event.isFromClient }
        }
    }

    test("_focus") {
        val f = AttachedTextField()
        var called = false
        f.addFocusListener { called = true }
        _focus(f)
        expect(true) { called }
    }

    test("_blur") {
        val f = AttachedTextField()
        var called = false
        f.addBlurListener { called = true }
        _blur(f)
        expect(true) { called }
    }
}
