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
import kotlin.test.expect

/**
 * @author mavi
 */
class MockRequestTest : DynaTest({
    lateinit var request: MockRequest
    beforeEach { request = MockRequest(MockHttpSession.create(MockContext())) }

    test("attributes") {
        expect(null) { request.getAttribute("foo") }
        request.setAttribute("foo", "bar")
        expect("bar") { request.getAttribute("foo") }
        request.setAttribute("foo", null)
        expect(null) { request.getAttribute("foo") }
        request.setAttribute("foo", "bar")
        expect("bar") { request.getAttribute("foo") }
        request.removeAttribute("foo")
        expect(null) { request.getAttribute("foo") }
    }

    test("parameters") {
        expect(null) { request.getParameter("foo") }
        expectList() { request.parameterNames.toList() }
        expect(null) { request.getParameterValues("foo") }
        request.setParameter("foo", "bar")
        expect("bar") { request.getParameter("foo") }
        expectList("foo") { request.parameterNames.toList() }
        expectList("bar") { request.getParameterValues("foo")!!.toList() }
        request.parameters["foo"] = arrayOf("bar", "baz")
        expect("bar") { request.getParameter("foo") }
        expectList("foo") { request.parameterNames.toList() }
        expectList("bar", "baz") { request.getParameterValues("foo")!!.toList() }
    }

    test("getSession(false) returns null once the session is invalidated") {
        // Mirrors the servlet container contract: after invalidation there is
        // no current session, so getSession(false) must return null rather than
        // the stale, invalidated session. See issue #115.
        val session = request.session as MockHttpSession
        expect(true) { session.isValid }
        session.setAttribute("foo", "bar")
        session.invalidate()
        expect(null) { request.getSession(false) }
        expect(false) { session.isValid }
    }

    test("getSession(true) creates a new session when invalidated") {
        var session = request.session as MockHttpSession
        val requestedId = request.requestedSessionId
        expect(true) { session.isValid }
        session.setAttribute("foo", "bar")
        session.invalidate()
        expect(false) { session.isValid }
        expect(true) { session != request.getSession(true) }
        session = request.getSession(true) as MockHttpSession
        expect(true) { session.isValid }
        expect(null) { session.getAttribute("foo") }
        // the replacement session has its own ID, but the ID the client asked
        // for stays the same for the lifetime of the request
        expect(true) { session.id != requestedId }
        expect(requestedId) { request.requestedSessionId }
    }

    test("getSession() creates a new session when invalidated") {
        var session = request.session as MockHttpSession
        expect(true) { session.isValid }
        session.setAttribute("foo", "bar")
        session.invalidate()
        expect(false) { session.isValid }
        expect(true) { session != request.session }
        session = request.session as MockHttpSession
        expect(true) { session.isValid }
        expect(null) { session.getAttribute("foo") }
    }

    test("changeSessionId() rotates the ID but keeps the session and its attributes") {
        // Apps which do not use Spring Security do session-fixation protection
        // themselves by calling changeSessionId() after a successful login; the
        // session (and thus the VaadinSession stored in it) must survive that.
        val session = request.session as MockHttpSession
        val oldId = session.id
        session.setAttribute("foo", "bar")

        val newId = request.changeSessionId()

        expect(true) { newId != oldId }
        expect(newId) { session.id }
        expect(session) { request.session }
        expect("bar") { session.getAttribute("foo") }
        // the client keeps sending the ID it was given until it picks up the
        // new one, so the requested ID does not change
        expect(oldId) { request.requestedSessionId }
    }

    test("changeSessionId() fails once the session is invalidated") {
        (request.session as MockHttpSession).invalidate()
        expectThrows(IllegalStateException::class) {
            request.changeSessionId()
        }
    }

    test("principal") {
        expect(null) { request.userPrincipal }
        request.userPrincipalInt = MockPrincipal("foo")
        expect(MockPrincipal("foo")) { request.userPrincipal }
    }

    test("isUserInRole") {
        expect(false) { request.isUserInRole("foo") }
        request.userPrincipalInt = MockPrincipal("foo")
        expect(false) { request.isUserInRole("foo") }
        request.userPrincipalInt = MockPrincipal("foo", listOf("foo"))
        expect(false) { request.isUserInRole("foo") }
        request.isUserInRole = { p, r -> (p as MockPrincipal).isUserInRole(r) }
        expect(true) { request.isUserInRole("foo") }
    }
})
