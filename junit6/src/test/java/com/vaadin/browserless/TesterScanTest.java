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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// Ensure that scanning testers does not fail when a tester references
// a component class that is not present on the classpath.
public class TesterScanTest {

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOrLogOnClassNotFoundException() {
        // Loads a dummy tester annotated with @Tests using an FQN to a
        // non-existing component class, as the commercial component testers
        // do in an application without those components. Skipping it is
        // expected, so nothing is logged at the default level. The registry
        // is loaded first so that only the scan of that package is captured.
        TesterRegistry.registerPackages();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            Assertions.assertDoesNotThrow(() -> TesterRegistry.registerPackages(
                    "com.vaadin.browserless.dontscan.classnotfound"));
        } finally {
            System.setErr(originalErr);
        }
        Assertions.assertEquals("", captured.toString(StandardCharsets.UTF_8));
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

}
