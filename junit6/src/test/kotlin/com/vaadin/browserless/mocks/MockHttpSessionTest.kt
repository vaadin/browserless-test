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
import com.github.mvysny.dynatest.expectThrows
import jakarta.servlet.http.HttpSession
import java.io.Serializable
import kotlin.test.expect

class MockHttpSessionTest : DynaTest({
    lateinit var session: HttpSession
    beforeEach { session = MockHttpSession.create(MockContext()) }

    test("attributes") {
        expect(null) { session.getAttribute("foo") }
        expectList() { session.attributeNames.toList() }
        session.setAttribute("foo", "bar")
        expectList("foo") { session.attributeNames.toList() }
        expect("bar") { session.getAttribute("foo") }
        session.setAttribute("foo", null)
        expect(null) { session.getAttribute("foo") }
        expectList() { session.attributeNames.toList() }
        session.setAttribute("foo", "bar")
        expect("bar") { session.getAttribute("foo") }
        expectList("foo") { session.attributeNames.toList() }
        session.removeAttribute("foo")
        expect(null) { session.getAttribute("foo") }
        expectList() { session.attributeNames.toList() }
    }

    test("serializable") {
        session.setAttribute("foo", "bar")
        (session as Serializable).cloneBySerialization()
    }

    group("invalidate") {
        test("smoke") {
            session.invalidate()
            expect(false) { (session as MockHttpSession).isValid }
        }
        test("calling invalidate() second time throws") {
            session.invalidate()
            expectThrows(IllegalStateException::class) {
                session.invalidate()
            }
        }
        test("getAttribute() fails on invalidated session") {
            session.invalidate()
            expectThrows(IllegalStateException::class) {
                session.getAttribute("foo")
            }
        }
        test("getId() succeeds on invalidated session") {
            session.invalidate()
            session.id
        }
        test("getServletContext() succeeds on invalidated session") {
            session.invalidate()
            session.servletContext
        }
        test("maxActiveInterval succeeds on invalidated session") {
            session.invalidate()
            session.maxInactiveInterval = session.maxInactiveInterval + 1
        }
        test("getCreationTime() fails on invalidated session") {
            session.invalidate()
            expectThrows(IllegalStateException::class) {
                session.creationTime
            }
        }
        test("getLastAccessedTime() fails on invalidated session") {
            session.invalidate()
            expectThrows(IllegalStateException::class) {
                session.lastAccessedTime
            }
        }
        test("getAttributeNames() fails on invalidated session") {
            session.invalidate()
            expectThrows(IllegalStateException::class) {
                session.attributeNames
            }
        }
    }
})
