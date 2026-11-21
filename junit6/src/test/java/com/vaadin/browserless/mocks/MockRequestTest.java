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
package com.vaadin.browserless.mocks;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockRequestTest {

    private MockRequest request;

    @BeforeEach
    void setUp() {
        request = new MockRequest(MockHttpSession.create(new MockContext()));
    }

    @Test
    void attributes_setToNullOrRemoved_dropTheAttribute() {
        assertNull(request.getAttribute("foo"));
        request.setAttribute("foo", "bar");
        assertEquals("bar", request.getAttribute("foo"));
        request.setAttribute("foo", null);
        assertNull(request.getAttribute("foo"));
        request.setAttribute("foo", "bar");
        assertEquals("bar", request.getAttribute("foo"));
        request.removeAttribute("foo");
        assertNull(request.getAttribute("foo"));
    }

    @Test
    void parameters_setSinglyOrInBulk_reportedBackToTheCaller() {
        assertNull(request.getParameter("foo"));
        assertEquals(List.of(), parameterNames());
        assertNull(request.getParameterValues("foo"));

        request.setParameter("foo", "bar");
        assertEquals("bar", request.getParameter("foo"));
        assertEquals(List.of("foo"), parameterNames());
        assertEquals(List.of("bar"),
                List.of(request.getParameterValues("foo")));

        request.getParameters().put("foo", new String[] { "bar", "baz" });
        assertEquals("bar", request.getParameter("foo"));
        assertEquals(List.of("foo"), parameterNames());
        assertEquals(List.of("bar", "baz"),
                List.of(request.getParameterValues("foo")));
    }

    @Test
    void getSession_invalidatedSessionAndNoCreate_returnsNull() {
        // Mirrors the servlet container contract: after invalidation there is
        // no current session, so getSession(false) must return null rather than
        // the stale, invalidated session. See issue #115.
        MockHttpSession session = (MockHttpSession) request.getSession();
        assertTrue(session.isValid());
        session.setAttribute("foo", "bar");
        session.invalidate();
        assertNull(request.getSession(false));
        assertFalse(session.isValid());
    }

    @Test
    void getSession_invalidatedSessionAndCreate_returnsAFreshSessionKeepingTheRequestedId() {
        MockHttpSession session = (MockHttpSession) request.getSession();
        String requestedId = request.getRequestedSessionId();
        assertTrue(session.isValid());
        session.setAttribute("foo", "bar");
        session.invalidate();
        assertFalse(session.isValid());
        assertNotSame(session, request.getSession(true));
        session = (MockHttpSession) request.getSession(true);
        assertTrue(session.isValid());
        assertNull(session.getAttribute("foo"));
        // the replacement session has its own ID, but the ID the client asked
        // for stays the same for the lifetime of the request
        assertNotEquals(requestedId, session.getId());
        assertEquals(requestedId, request.getRequestedSessionId());
    }

    @Test
    void getSession_invalidatedSession_returnsAFreshEmptySession() {
        MockHttpSession session = (MockHttpSession) request.getSession();
        assertTrue(session.isValid());
        session.setAttribute("foo", "bar");
        session.invalidate();
        assertFalse(session.isValid());
        assertNotSame(session, request.getSession());
        session = (MockHttpSession) request.getSession();
        assertTrue(session.isValid());
        assertNull(session.getAttribute("foo"));
    }

    @Test
    void changeSessionId_afterLogin_rotatesTheIdButKeepsTheSessionAndItsAttributes() {
        // Apps which do not use Spring Security do session-fixation protection
        // themselves by calling changeSessionId() after a successful login; the
        // session (and thus the VaadinSession stored in it) must survive that.
        MockHttpSession session = (MockHttpSession) request.getSession();
        String oldId = session.getId();
        session.setAttribute("foo", "bar");

        String newId = request.changeSessionId();

        assertNotEquals(oldId, newId);
        assertEquals(newId, session.getId());
        assertSame(session, request.getSession());
        assertEquals("bar", session.getAttribute("foo"));
        // the client keeps sending the ID it was given until it picks up the
        // new one, so the requested ID does not change
        assertEquals(oldId, request.getRequestedSessionId());
    }

    @Test
    void changeSessionId_invalidatedSession_throws() {
        ((MockHttpSession) request.getSession()).invalidate();
        assertThrows(IllegalStateException.class,
                () -> request.changeSessionId());
    }

    @Test
    void getUserPrincipal_principalSet_reportsIt() {
        assertNull(request.getUserPrincipal());
        request.setUserPrincipalInt(new MockPrincipal("foo"));
        assertEquals(new MockPrincipal("foo"), request.getUserPrincipal());
    }

    @Test
    void isUserInRole_withoutRoleChecker_grantsNothing() {
        assertFalse(request.isUserInRole("foo"));
        request.setUserPrincipalInt(new MockPrincipal("foo"));
        assertFalse(request.isUserInRole("foo"));
        request.setUserPrincipalInt(new MockPrincipal("foo", List.of("foo")));
        assertFalse(request.isUserInRole("foo"));
        request.setUserInRole((BiPredicate<java.security.Principal, String>) (p,
                r) -> ((MockPrincipal) p).isUserInRole(r));
        assertTrue(request.isUserInRole("foo"));
    }

    private List<String> parameterNames() {
        return Collections.list(request.getParameterNames());
    }
}
