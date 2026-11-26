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

import java.util.List;

import com.example.failedinit.FailedInitComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import com.vaadin.browserless.logging.LogCapture;
import com.vaadin.browserless.logging.LogCapture.CapturedEvent;

// Ensure that scanning testers does not fail when a tester references
// a component class that is not present on the classpath. Skipping such a
// tester is expected, e.g. for the Pro component testers when only
// vaadin-core is on the classpath, so it is logged below the default level.
public class TesterScanTest {

    private static final String SKIPPED = "Skipping tester '%s' because a class it needs is not available: %s";

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOnClassNotFoundException() {
        // Loads a dummy tester annotated with @Tests for an existing component
        // and, using an FQN, for a non-existing one.
        CapturedEvent event = scanSingleEvent(
                "com.vaadin.browserless.dontscan.classnotfound", Level.TRACE);

        Assertions.assertEquals(Level.TRACE, event.level());
        Assertions.assertEquals(SKIPPED.formatted(
                "com.vaadin.browserless.dontscan.classnotfound.ClassNotFoundComponentTester",
                "com.vaadin.browserless.dontscan.classnotfound.MissingComponent"),
                event.message());
        Assertions.assertInstanceOf(ClassNotFoundException.class,
                event.throwable());
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_logsOnlyDebugLineWithoutTrace() {
        // Same as above with trace off, which is what users get unless they
        // turn on trace logging: one DEBUG line without the stack trace.
        CapturedEvent event = scanSingleEvent(
                "com.vaadin.browserless.dontscan.classnotfounddebug",
                Level.DEBUG);

        Assertions.assertEquals(Level.DEBUG, event.level());
        Assertions.assertEquals(SKIPPED.formatted(
                "com.vaadin.browserless.dontscan.classnotfounddebug.ClassNotFoundDebugComponentTester",
                "com.vaadin.browserless.dontscan.classnotfounddebug.MissingComponent"),
                event.message());
        Assertions.assertNull(event.throwable());
    }

    @Test
    public void scanForTesters_testerNeedsClassWithFailedInitialization_warns() {
        // A class whose static initializer has already failed is on the
        // classpath but broken, so a tester that needs it is a real problem.
        // The fixture lives outside com.vaadin.browserless so that no other
        // test scans it and runs the failing initializer from a scan.
        Assertions.assertThrows(LinkageError.class,
                () -> Class.forName("com.example.failedinit.BrokenInit"));

        CapturedEvent event = scanSingleEvent("com.example.failedinit",
                Level.TRACE);

        Assertions.assertEquals(Level.WARN, event.level());
        Assertions.assertEquals(
                "Tester 'com.example.failedinit.FailedInitComponentTester' cannot be loaded",
                event.message());
        Assertions.assertInstanceOf(NoClassDefFoundError.class,
                event.throwable());
        Assertions.assertEquals(
                "Could not initialize class com.example.failedinit.BrokenInit",
                event.throwable().getMessage());
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

    // Scans the package, expecting it to log exactly one event at the given
    // level or above. A single DEBUG or TRACE event also shows that nothing
    // is logged at the default level.
    private static CapturedEvent scanSingleEvent(String pkg, Level level) {
        try (LogCapture capture = LogCapture.start(TesterRegistry.class,
                level)) {
            Assertions.assertDoesNotThrow(
                    () -> TesterRegistry.registerPackages(pkg));
            List<CapturedEvent> events = capture.events();
            Assertions.assertEquals(1, events.size(), events::toString);
            return events.get(0);
        }
    }
}
