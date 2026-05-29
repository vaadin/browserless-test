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

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.html.Span
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl

@DynaTestDsl
internal fun DynaNodeGroup.compositeTests() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("Composite<*> causes virtual children to be fetched twice") {
        class MyComposite : Composite<VirtualChildComponent>()

        val comp = MyComposite()
        comp._expectOne<Span> { text = "virtual child" }
    }
}

@Tag("my-test")
class VirtualChildComponent : Component() {
    init {
        val child = Span("virtual child")
        element.appendVirtualChild(child.element)
    }
}
