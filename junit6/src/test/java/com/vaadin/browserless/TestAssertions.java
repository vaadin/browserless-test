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
package com.vaadin.browserless;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Assertion helpers replacing the ones the Kotlin tests took from DynaTest.
 */
public final class TestAssertions {

    private TestAssertions() {
    }

    /**
     * Asserts that the given block throws an exception of the given type whose
     * message matches the given regular expression.
     * <p>
     * JUnit's own {@code assertThrows} says nothing about the message, and
     * several of these tests assert on the message precisely because it is the
     * thing a user reads when a lookup or a usability check fails.
     *
     * @param clazz
     *            the expected exception type, or a supertype of it
     * @param expectedMessageRegex
     *            a regular expression the message must contain a match for
     * @param block
     *            the code expected to throw
     * @param <T>
     *            the exception type
     * @return the exception that was thrown, so a caller can assert further
     */
    public static <T extends Throwable> T expectThrows(Class<T> clazz,
            String expectedMessageRegex,
            org.junit.jupiter.api.function.Executable block) {
        T ex = assertThrows(clazz, block);
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (!Pattern.compile(expectedMessageRegex).matcher(message).find()) {
            throw new AssertionError(clazz.getName() + " message: Expected '"
                    + expectedMessageRegex + "' but was '" + message + "'", ex);
        }
        return ex;
    }
}
