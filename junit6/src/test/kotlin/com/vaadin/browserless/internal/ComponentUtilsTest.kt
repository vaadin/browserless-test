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
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.shared.SlotUtils
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import kotlin.test.expect
import com.vaadin.browserless.internal.ComponentUtils.getCaption
import com.vaadin.browserless.internal.ComponentUtils.getLabel
import com.vaadin.browserless.internal.ComponentUtils.getPlaceholder

fun DynaNodeGroup.componentUtilsTests() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("getPlaceholder") {
        expect("foo") { getPlaceholder(TextField().apply { placeholder = "foo" }) }
        expect("") { getPlaceholder(TextField().apply { placeholder = "" }) }
        expect("foo") { getPlaceholder(TextArea().apply { placeholder = "foo" }) }
        // doesn't support placeholder
        expect(null) { getPlaceholder(Button()) }
    }

    group("getLabel") {
        test("TextField") {
            val c = TextField()
            expect("") { getLabel(c) }
            c.label = "foo"
            expect("foo") { getLabel(c) }
        }
        test("Checkbox") {
            val c = Checkbox()
            expect("") { getLabel(c) }
            c.label = "foo"
            expect("foo") { getLabel(c) }
        }
        test("FormItem") {
            expect("") { getLabel(FormLayout.FormItem()) }
            val item = FormLayout().addFormItem(Button(), Div(Span("Last")))
            SlotUtils.addToSlot(item, "label", Span("  name "))
            expect("Last name") { getLabel(item) }
        }
    }

    test("getCaption") {
        expect("foo") { getCaption(Button("foo")) }
        expect("") { getCaption(Button()) }
        expect("foo") { getCaption(Checkbox("foo")) }
        expect("") { getCaption(Checkbox()) }
        expect("foo") { getCaption(FormLayout().addFormItem(Button(), "foo")) }
    }
}
