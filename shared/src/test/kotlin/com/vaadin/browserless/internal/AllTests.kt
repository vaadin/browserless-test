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
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.littemplate.LitTemplate
import com.vaadin.flow.component.polymertemplate.PolymerTemplate
import com.vaadin.flow.component.polymertemplate.TemplateParser
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.templatemodel.TemplateModel
import com.github.mvysny.dynatest.DynaTest
import org.jsoup.nodes.Element

class AllTests : DynaTest({

    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }


    test("Component.isTemplate") {
        expect(false) { Button("foo").isTemplate }
        expect(true) { MyLitTemplate().isTemplate }
        expect(true) { MyPolymerTemplate().isTemplate }
    }

})

internal interface MyModel : TemplateModel
internal class MyTemplateParser : TemplateParser {
    override fun getTemplateContent(
        clazz: Class<out PolymerTemplate<*>>?,
        tag: String?,
        service: VaadinService?
    ): TemplateParser.TemplateData {
        return TemplateParser.TemplateData("", Element(tag!!))
    }

}
@Tag("my-polymer")
internal class MyPolymerTemplate : PolymerTemplate<MyModel>(MyTemplateParser()) {

}
@Tag("my-lit")
internal class MyLitTemplate : LitTemplate()