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

import jakarta.servlet.http.HttpSession;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.TestSerialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockHttpSessionTest {

    private HttpSession session;

    @BeforeEach
    void setUp() {
        session = MockHttpSession.create(new MockContext());
    }

    @Test
    void attributes_setToNullOrRemoved_dropTheAttribute() {
        assertNull(session.getAttribute("foo"));
        assertEquals(List.of(), names());
        session.setAttribute("foo", "bar");
        assertEquals(List.of("foo"), names());
        assertEquals("bar", session.getAttribute("foo"));
        session.setAttribute("foo", null);
        assertNull(session.getAttribute("foo"));
        assertEquals(List.of(), names());
        session.setAttribute("foo", "bar");
        assertEquals("bar", session.getAttribute("foo"));
        assertEquals(List.of("foo"), names());
        session.removeAttribute("foo");
        assertNull(session.getAttribute("foo"));
        assertEquals(List.of(), names());
    }

    @Test
    void serialization_sessionWithAttributes_roundTrips() {
        session.setAttribute("foo", "bar");
        TestSerialization.cloneBySerialization((MockHttpSession) session);
    }

    @Nested
    class Invalidate {

        @Test
        void invalidate_marksTheSessionInvalid() {
            session.invalidate();
            assertFalse(((MockHttpSession) session).isValid());
        }

        @Test
        void invalidate_calledTwice_throws() {
            session.invalidate();
            assertThrows(IllegalStateException.class,
                    () -> session.invalidate());
        }

        @Test
        void getAttribute_invalidatedSession_throws() {
            session.invalidate();
            assertThrows(IllegalStateException.class,
                    () -> session.getAttribute("foo"));
        }

        @Test
        void getId_invalidatedSession_stillReadable() {
            session.invalidate();
            session.getId();
        }

        @Test
        void getServletContext_invalidatedSession_stillReadable() {
            session.invalidate();
            session.getServletContext();
        }

        @Test
        void maxInactiveInterval_invalidatedSession_stillWritable() {
            session.invalidate();
            session.setMaxInactiveInterval(
                    session.getMaxInactiveInterval() + 1);
        }

        @Test
        void getCreationTime_invalidatedSession_throws() {
            session.invalidate();
            assertThrows(IllegalStateException.class,
                    () -> session.getCreationTime());
        }

        @Test
        void getLastAccessedTime_invalidatedSession_throws() {
            session.invalidate();
            assertThrows(IllegalStateException.class,
                    () -> session.getLastAccessedTime());
        }

        @Test
        void getAttributeNames_invalidatedSession_throws() {
            session.invalidate();
            assertThrows(IllegalStateException.class,
                    () -> session.getAttributeNames());
        }
    }

    private List<String> names() {
        return Collections.list(session.getAttributeNames());
    }
}
