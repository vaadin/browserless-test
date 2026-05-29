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

import kotlin.test.expect
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.expectList
import com.vaadin.flow.component.Text

fun DynaNodeGroup.depthFirstTreeIteratorTests() {
    test("DepthFirstTreeIterator") {
        val i = DepthFirstTreeIterator("0") { if (it.length > 2) listOf() else listOf("${it}0", "${it}1", "${it}2")}
        expectList("0", "00", "000", "001", "002", "01", "010", "011", "012", "02", "020", "021", "022") { i.asSequence().toList() }
    }

    test("walk") {
        val expected = mutableListOf<Component>()
        val root = VerticalLayout().apply {
            expected.add(this)
            add(Button("Foo").apply {
                expected.add(this)
                // In Vaadin 25.1, Button also has a text node
                this.children.filter { it is Text }
                    .findFirst()
                    .ifPresent { expected.add(it) }
            })
            add(HorizontalLayout().apply {
                expected.add(this)
                add(Span().apply { expected.add(this) })
            })
            add(VerticalLayout().apply { expected.add(this) })
        }
        expect(expected) { root.walk().toList() }
        expect(root) { root.walk().toList()[0] }
    }
}
