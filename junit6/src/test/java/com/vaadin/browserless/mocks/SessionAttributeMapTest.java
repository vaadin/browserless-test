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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionAttributeMapTest {

    private MockHttpSession session;

    private Map<String, Object> attrs;

    @BeforeEach
    void setUp() {
        session = MockHttpSession.create(new MockContext());
        attrs = new SessionAttributeMap(session);
    }

    @Nested
    class Size {

        @Test
        void newMap_isZero() {
            assertEquals(0, attrs.size());
        }

        @Test
        void put_growsByOne() {
            attrs.put("foo", "bar");
            assertEquals(1, attrs.size());
        }

        @Test
        void setAttributeOnSession_growsByOne() {
            session.setAttribute("foo", "bar");
            assertEquals(1, attrs.size());
        }

        @Test
        void clear_dropsToZero() {
            attrs.put("foo", "bar");
            attrs.clear();
            assertEquals(0, attrs.size());
        }
    }

    @Nested
    class IsEmpty {

        @Test
        void newMap_isEmpty() {
            assertTrue(attrs.isEmpty());
        }

        @Test
        void put_isNoLongerEmpty() {
            attrs.put("foo", "bar");
            assertFalse(attrs.isEmpty());
        }

        @Test
        void setAttributeOnSession_isNoLongerEmpty() {
            session.setAttribute("foo", "bar");
            assertFalse(attrs.isEmpty());
        }

        @Test
        void clear_isEmptyAgain() {
            attrs.put("foo", "bar");
            attrs.clear();
            assertTrue(attrs.isEmpty());
        }
    }

    @Nested
    class Get {

        @Test
        void emptyMap_returnsNull() {
            assertNull(attrs.get("foo"));
        }

        @Test
        void unknownKey_returnsNull() {
            session.setAttribute("foo", "bar");
            assertNull(attrs.get("bar"));
        }

        @Test
        void keySetOnSession_returnsItsValue() {
            session.setAttribute("foo", "bar");
            assertEquals("bar", attrs.get("foo"));
        }

        @Test
        void removedKey_returnsNull() {
            session.setAttribute("foo", "bar");
            attrs.remove("foo");
            assertNull(attrs.get("foo"));
        }
    }

    @Nested
    class Remove {

        @Test
        void emptyMap_returnsNull() {
            assertNull(attrs.remove("foo"));
            assertTrue(attrs.isEmpty());
        }

        @Test
        void unknownKey_leavesOtherEntriesAlone() {
            attrs.put("bar", "foo");
            assertNull(attrs.remove("foo"));
            assertEquals("foo", attrs.get("bar"));
            assertEquals("foo", session.getAttribute("bar"));
        }

        @Test
        void existingKey_returnsOldValueAndClearsTheSessionAttribute() {
            attrs.put("bar", "foo");
            assertEquals("foo", attrs.remove("bar"));
            assertTrue(attrs.isEmpty());
            assertNull(session.getAttribute("bar"));
        }
    }

    @Nested
    class Clear {

        @Test
        void emptyMap_staysEmpty() {
            attrs.clear();
            assertTrue(attrs.isEmpty());
        }

        @Test
        void singleEntry_isRemoved() {
            attrs.put("foo", "bar");
            attrs.clear();
            assertTrue(attrs.isEmpty());
        }

        @Test
        void manyEntries_allClearedFromTheSession() {
            for (int i = 0; i <= 1000; i++) {
                session.setAttribute(Integer.toString(i), i);
            }
            assertFalse(attrs.isEmpty());
            attrs.clear();
            assertTrue(attrs.isEmpty());
            for (int i = 0; i <= 1000; i++) {
                assertNull(session.getAttribute(Integer.toString(i)));
            }
        }
    }
}
