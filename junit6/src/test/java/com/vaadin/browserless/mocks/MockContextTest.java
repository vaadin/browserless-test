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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.TestSerialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockContextTest {

    private MockContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new MockContext();
    }

    @Test
    void attributes_setToNullOrRemoved_dropTheAttribute() {
        assertNull(ctx.getAttribute("foo"));
        assertEquals(List.of(), names());
        ctx.setAttribute("foo", "bar");
        assertEquals("bar", ctx.getAttribute("foo"));
        assertEquals(List.of("foo"), names());
        ctx.setAttribute("foo", null);
        assertNull(ctx.getAttribute("foo"));
        assertEquals(List.of(), names());
        ctx.setAttribute("foo", "bar");
        assertEquals(List.of("foo"), names());
        assertEquals("bar", ctx.getAttribute("foo"));
        ctx.removeAttribute("foo");
        assertEquals(List.of(), names());
        assertNull(ctx.getAttribute("foo"));
    }

    @Test
    void setInitParameter_alreadySet_keepsTheFirstValue() {
        assertNull(ctx.getInitParameter("foo"));
        assertEquals(List.of(), initParameterNames());
        assertTrue(ctx.setInitParameter("foo", "bar"));
        assertEquals(List.of("foo"), initParameterNames());
        assertEquals("bar", ctx.getInitParameter("foo"));
        assertFalse(ctx.setInitParameter("foo", "baz"));
        assertEquals("bar", ctx.getInitParameter("foo"));
        assertEquals(List.of("foo"), initParameterNames());
    }

    @Test
    void getRealPath_pathOutsideTheRoots_returnsNull() {
        ctx.setRealPathRoots(List.of("src/main/webapp/frontend",
                "src/main/webapp", "src/test/webapp"));
        assertNull(ctx.getRealPath("/index.html"));
        assertTrue(ctx.getRealPath("/VAADIN/themes/default/img/1.txt")
                .replace('\\', '/')
                .endsWith("/VAADIN/themes/default/img/1.txt"));
        assertTrue(ctx.getRealPath("/VAADIN/themes/valo/../default/img/1.txt")
                .replace('\\', '/')
                .endsWith("/VAADIN/themes/default/img/1.txt"));
        // stepping out of root is not allowed and returns null. Avoids browsing
        // through the filesystem
        assertNull(ctx.getRealPath("/../../../build.gradle.kts"));
    }

    @Test
    void serialization_contextWithAttributes_roundTrips() {
        ctx.setAttribute("foo", "bar");
        ctx.setInitParameter("foo", "bar");
        TestSerialization.cloneBySerialization(ctx);
    }

    private List<String> names() {
        return Collections.list(ctx.getAttributeNames());
    }

    private List<String> initParameterNames() {
        return Collections.list(ctx.getInitParameterNames());
    }
}
