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
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import jakarta.servlet.http.Cookie
import kotlin.test.expect

class MockResponseTest : DynaTest({
    lateinit var request: MockResponse
    beforeEach { request = MockResponse() }

    test("headers") {
        expect(null) { request.getHeader("foo") }
        expectList() { request.headerNames.toList() }
        expectList() { request.getHeaders("foo").toList() }
        request.setHeader("foo", "bar")
        expect("bar") { request.getHeader("foo") }
        expectList("foo") { request.headerNames.toList() }
        expectList("bar") { request.getHeaders("foo").toList() }
        request.headers["foo"] = arrayOf("bar", "baz")
        expect("bar") { request.getHeader("foo") }
        expectList("foo") { request.headerNames.toList() }
        expectList("bar", "baz") { request.getHeaders("foo").toList() }
    }

    test("cookies") {
        request.cookies += Cookie("foo", "bar")
        expect("bar") { request.getCookie("foo").value }
        expect(null) { request.findCookie("qqq") }
        expectThrows(IllegalStateException::class, "no such cookie with name baz. Available cookies: foo=bar") {
            request.getCookie("baz")
        }
    }
})
