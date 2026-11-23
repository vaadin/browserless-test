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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import com.example.failedinit.FailedInitComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.dontscan.classnotfound.ClassNotFoundComponent;
import com.vaadin.browserless.dontscan.classnotfound.ClassNotFoundComponentTester;

// Ensure that scanning testers does not fail when a tester references
// a component class that is not present on the classpath.
public class TesterScanTest {

    @Test
    public void scanForTesters_testerForClassNotInClasspath_skipsClassWithoutWarning() {
        // Loads a dummy tester annotated with @Tests for an existing component
        // and, using an FQN, for a non-existing one. Skipping the missing one
        // is expected, e.g. for the Pro component testers when only
        // vaadin-core is on the classpath, so it is not a warning.
        String log = captureStandardError(() -> Assertions
                .assertDoesNotThrow(() -> TesterRegistry.registerPackages(
                        "com.vaadin.browserless.dontscan.classnotfound")));

        Assertions.assertEquals("", log);
        Assertions.assertEquals(ClassNotFoundComponentTester.class,
                TesterRegistry.resolve(ClassNotFoundComponent.class));
    }

    @Test
    public void scanForTesters_testerNeedsClassWithFailedInitialization_warns() {
        // A class whose static initializer has already failed is on the
        // classpath but broken, so a tester that needs it is a real problem.
        // The fixture lives outside com.vaadin.browserless so that no other
        // test scans it and runs the failing initializer from a scan.
        Assertions.assertThrows(LinkageError.class,
                () -> Class.forName("com.example.failedinit.BrokenInit"));

        String log = captureStandardError(
                () -> Assertions.assertDoesNotThrow(() -> TesterRegistry
                        .registerPackages("com.example.failedinit")));

        Assertions.assertTrue(log
                .contains("WARN com.vaadin.browserless.TesterRegistry - Tester "
                        + "'com.example.failedinit.FailedInitComponentTester'"
                        + " cannot be loaded" + System.lineSeparator()
                        + "java.lang.NoClassDefFoundError: Could not initialize class "
                        + "com.example.failedinit.BrokenInit"),
                log);
        Assertions.assertEquals(ComponentTester.class,
                TesterRegistry.resolve(FailedInitComponent.class));
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowNoClassDefFound() {
        // Loads a dummy tester annotated with @Tests referencing a class in
        // another module with provided scope so the test itself is not able to
        // load the class.
        Assertions.assertDoesNotThrow(() -> TesterRegistry.registerPackages(
                "com.vaadin.browserless.dontscan.noclassdeffound"));
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowTypeNotPresentException() {
        // Loads a dummy tester annotated with @Tests referencing a class in
        // another module with provided scope so the test itself is not able to
        // load the class.
        Assertions.assertDoesNotThrow(() -> TesterRegistry.registerPackages(
                "com.vaadin.browserless.dontscan.typenotpresent"));
    }

    private static String captureStandardError(Runnable action) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setErr(originalErr);
        }
        return err.toString(StandardCharsets.UTF_8);
    }
}
