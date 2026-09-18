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

import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;

import static com.vaadin.browserless.TestAssertions.expectThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@link Locator} assertions that don't need a
 * {@link TestingLifecycleHook} of their own, so they run against the default
 * hook a user would get.
 */
class LocatorWithoutLifecycleHookTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void expectNoDialogs_aDialogIsOpen_failsWithTheComponentTree() {
        Locator._expectNoDialogs(); // succeeds when there are no dialogs

        Dialog dialog = new Dialog();
        dialog.open();
        AssertionError exception = assertThrows(AssertionError.class,
                Locator::_expectNoDialogs);
        String expected = ".*Too many visible Dialogs \\(1\\) in MockedUI\\[] "
                + "matching Dialog and count=0\\.\\.0: \\[Dialog\\[.*]]\\. Component tree:\n"
                + "└── MockedUI\\[]\n" + "\\s+└── Dialog\\[.*].*\n";
        assertTrue(
                Pattern.compile(expected, Pattern.MULTILINE)
                        .matcher(exception.getMessage()).matches(),
                "Unexpected failure message: " + exception.getMessage());

        dialog.close();
        Locator._expectNoDialogs();
    }

    @Nested
    class InternalServerErrorHandling {

        @Test
        void expectOne_navigationFailed_failsFastOnTheInternalServerError() {
            // Vaadin shows InternalServerError when an exception occurs during
            // the navigation phase. The _expect*() functions should detect this
            // and fail fast.
            rerouteToError();

            expectThrows(AssertionError.class, Pattern.quote(
                    "An internal server error occurred; please check log for the actual stack-trace. Error text: There was an exception while trying to navigate to"),
                    () -> Locator._expectOne(UI.class));
        }

        @Test
        void expectInternalServerError_noErrorHappened_fails() {
            expectThrows(AssertionError.class, Pattern.quote(
                    "Expected an internal server error but none happened. Component tree:\n"
                            + "└── MockedUI[]"),
                    Locator::_expectInternalServerError);
        }

        @Test
        void expectInternalServerError_navigationFailed_passes() {
            rerouteToError();

            Locator._expectInternalServerError();
        }

        @Test
        void expectInternalServerError_expectedMessageMatches_passes() {
            rerouteToError();

            Locator._expectInternalServerError("Simulated");
        }

        private void rerouteToError() {
            Utils.currentUI()
                    .addBeforeEnterListener(event -> event.rerouteToError(
                            new RuntimeException("Simulated"), "Simulated"));
            UI.getCurrent().navigate("");
        }
    }
}
