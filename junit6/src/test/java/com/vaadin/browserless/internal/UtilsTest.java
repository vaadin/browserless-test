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
package com.vaadin.browserless.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Utils#isBlank(CharSequence)} and {@link Utils#trim(String)} follow
 * Kotlin's whitespace rules, which, unlike {@link String#isBlank()} and
 * {@link String#strip()}, also treat a non-breaking space as whitespace.
 */
class UtilsTest {

    private static final String NBSP = " ";

    @Test
    void isBlank_nonBreakingSpace_isBlank() {
        assertTrue(Utils.isBlank(NBSP));
        assertTrue(Utils.isBlank(" " + NBSP + "\t"));
        assertTrue(Utils.isBlank(null));
        assertFalse(Utils.isBlank(NBSP + "a"));
    }

    @Test
    void trim_nonBreakingSpace_isRemoved() {
        assertEquals("a b", Utils.trim(NBSP + " a b\t" + NBSP));
        assertEquals("", Utils.trim(NBSP));
    }
}
