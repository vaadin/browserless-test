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
package com.vaadin.browserless.mocks

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import com.github.mvysny.dynatest.expectList
import kotlin.test.expect

class MockContextTest : DynaTest({
    lateinit var ctx: MockContext
    beforeEach { ctx = MockContext() }

    test("attributes") {
        expect(null) { ctx.getAttribute("foo") }
        expectList() { ctx.attributeNames.toList() }
        ctx.setAttribute("foo", "bar")
        expect("bar") { ctx.getAttribute("foo") }
        expectList("foo") { ctx.attributeNames.toList() }
        ctx.setAttribute("foo", null)
        expect(null) { ctx.getAttribute("foo") }
        expectList() { ctx.attributeNames.toList() }
        ctx.setAttribute("foo", "bar")
        expectList("foo") { ctx.attributeNames.toList() }
        expect("bar") { ctx.getAttribute("foo") }
        ctx.removeAttribute("foo")
        expectList() { ctx.attributeNames.toList() }
        expect(null) { ctx.getAttribute("foo") }
    }

    test("init parameters") {
        expect(null) { ctx.getInitParameter("foo") }
        expectList() { ctx.initParameterNames.toList() }
        expect(true) { ctx.setInitParameter("foo", "bar") }
        expectList("foo") { ctx.initParameterNames.toList() }
        expect("bar") { ctx.getInitParameter("foo") }
        expect(false) { ctx.setInitParameter("foo", "baz") }
        expect("bar") { ctx.getInitParameter("foo") }
        expectList("foo") { ctx.initParameterNames.toList() }
    }

    test("realPath") {
        ctx.realPathRoots = listOf("src/main/webapp/frontend", "src/main/webapp", "src/test/webapp")
        expect(null) { ctx.getRealPath("/index.html") }
        expect(true) { ctx.getRealPath("/VAADIN/themes/default/img/1.txt")!!.replace('\\', '/').endsWith("/VAADIN/themes/default/img/1.txt") }
        expect(true) { ctx.getRealPath("/VAADIN/themes/valo/../default/img/1.txt")!!.replace('\\', '/').endsWith("/VAADIN/themes/default/img/1.txt") }
        // stepping out of root is not allowed and returns null. Avoids browsing through the filesystem
        expect(null) { ctx.getRealPath("/../../../build.gradle.kts") }
    }

    test("serializable") {
        ctx.setAttribute("foo", "bar")
        ctx.setInitParameter("foo", "bar")
        ctx.cloneBySerialization()
    }
})
