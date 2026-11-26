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

import jakarta.servlet.http.Cookie;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockResponseTest {

    private MockResponse response;

    @BeforeEach
    void setUp() {
        response = new MockResponse();
    }

    @Test
    void headers_setAndOverwrite_reportedBackToTheCaller() {
        assertNull(response.getHeader("foo"));
        assertEquals(List.of(), List.copyOf(response.getHeaderNames()));
        assertEquals(List.of(), List.copyOf(response.getHeaders("foo")));

        response.setHeader("foo", "bar");
        assertEquals("bar", response.getHeader("foo"));
        assertEquals(List.of("foo"), List.copyOf(response.getHeaderNames()));
        assertEquals(List.of("bar"), List.copyOf(response.getHeaders("foo")));

        response.getHeaders().put("foo", new String[] { "bar", "baz" });
        assertEquals("bar", response.getHeader("foo"));
        assertEquals(List.of("foo"), List.copyOf(response.getHeaderNames()));
        assertEquals(List.of("bar", "baz"),
                List.copyOf(response.getHeaders("foo")));
    }

    @Test
    void cookies_addedCookie_foundByNameAndMissingOneThrows() {
        Collections.addAll(response.getCookies(), new Cookie("foo", "bar"));
        assertEquals("bar", response.getCookie("foo").getValue());
        assertNull(response.findCookie("qqq"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> response.getCookie("baz"));
        assertEquals("no such cookie with name baz. Available cookies: foo=bar",
                ex.getMessage());
    }
}
