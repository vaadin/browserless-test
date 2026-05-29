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
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.internal.JacksonUtils
import kotlin.test.expect
import com.vaadin.browserless.expectThrows

@DynaTestDsl
internal fun DynaNodeGroup.basicUtilsTestbatch() {

    group("checkEditableByUser") {
        test("disabled textfield fails") {
            expectThrows(java.lang.IllegalStateException::class, "The AttachedTextField\\[DISABLED,.*] is not enabled".toRegex()) {
                AttachedTextField().apply { isEnabled = false }.checkEditableByUser()
            }
        }
        test("invisible textfield fails") {
            expectThrows(
                java.lang.IllegalStateException::class,
                "The AttachedTextField\\[INVIS,.*] is not effectively visible".toRegex()
            ) {
                AttachedTextField().apply { isVisible = false }.checkEditableByUser()
            }
        }
        test("non attached textfield fails") {
            expectThrows(
                java.lang.IllegalStateException::class,
                "The TextField\\[.*] is not attached".toRegex()
            ) {
                TextField().checkEditableByUser()
            }
        }
        test("textfield in invisible layout fails") {
            expectThrows(java.lang.IllegalStateException::class, "The TextField\\[.*] is not effectively visible".toRegex()) {
                VerticalLayout().apply {
                    isVisible = false
                    textField().also { it.checkEditableByUser() }
                }
            }
        }
        test("textfield succeeds") {
            AttachedTextField().checkEditableByUser()
        }
    }

    group("expectNotEditableByUser") {
        test("disabled textfield fails") {
            AttachedTextField().apply { isEnabled = false }.expectNotEditableByUser()
        }
        test("invisible textfield fails") {
            AttachedTextField().apply { isVisible = false }.expectNotEditableByUser()
        }
        test("textfield in invisible layout fails") {
            VerticalLayout().apply {
                isVisible = false
                textField().also { it.expectNotEditableByUser() }
            }
        }
        test("textfield succeeds") {
            expectThrows(AssertionError::class, "The AttachedTextField\\[.*] is editable".toRegex()) {
                AttachedTextField().expectNotEditableByUser()
            }
        }
    }

    group("fireDomEvent()") {
        test("smoke") {
            Div()._fireDomEvent("click")
        }
        test("listeners are called") {
            val div = Div()
            lateinit var event: DomEvent
            div.element.addEventListener("click") { e -> event = e }
            div._fireDomEvent("click")
            expect("click") { event.type }
        }
        test("higher-level listeners are called") {
            val div = Div()
            lateinit var event: ClickEvent<Div>
            div.addClickListener { e -> event = e }
            div._fireDomEvent("click", JacksonUtils.createObjectNode().apply { put("event.screenX", 20.0) })
            expect(20) { event.screenX }
            expect(true) { event.isFromClient }
        }
    }

    test("_focus") {
        val f = AttachedTextField()
        var called = false
        f.addFocusListener { called = true }
        f._focus()
        expect(true) { called }
    }

    test("_blur") {
        val f = AttachedTextField()
        var called = false
        f.addBlurListener { called = true }
        f._blur()
        expect(true) { called }
    }
}
