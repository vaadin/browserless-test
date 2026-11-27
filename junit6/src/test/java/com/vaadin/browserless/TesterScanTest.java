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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;

import com.vaadin.browserless.logging.LogCapture;
import com.vaadin.browserless.logging.LogCapture.CapturedEvent;
import com.vaadin.flow.component.Component;

// Ensure that scanning testers does not fail when a tester references
// a component class that is not present on the classpath.
public class TesterScanTest {

    @TempDir
    Path classes;

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOrLogOnClassNotFoundException() {
        // Loads a dummy tester annotated with @Tests using an FQN to a
        // non-existing component class, as the commercial component testers
        // do in an application without those components. Skipping it is
        // expected, so nothing is logged at the default level.
        assertSkipLoggedBelowDefaultLevel(
                () -> TesterRegistry.registerPackages(
                        "com.vaadin.browserless.dontscan.classnotfound"),
                "Tester 'com.vaadin.browserless.dontscan.classnotfound.MissingComponentTester' cannot be loaded because of missing component class 'com.vaadin.browserless.dontscan.classnotfound.MissingComponent' on classpath",
                ClassNotFoundException.class);
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOrLogOnNoClassDefFoundError()
            throws Exception {
        // Like GridProTester, the tester's type parameter is bound to the
        // component class, so loading the tester needs the missing class.
        // The tester claims Span, so if it were loaded, replacing the Span
        // tester would log a warning.
        URLClassLoader fixture = compileWithMissingComponent("noclassdeffound",
                """
                        @Tests(fqn = "com.vaadin.flow.component.html.Span")
                        public class MissingComponentTester<T extends MissingComponent>
                                extends ComponentTester<T> {
                            public MissingComponentTester(T component) {
                                super(component);
                            }
                        }
                        """);
        try (fixture) {
            assertSkipLoggedBelowDefaultLevel(
                    () -> registerPackageWith(fixture,
                            "com.vaadin.browserless.dontscan.noclassdeffound"),
                    "Tester 'com.vaadin.browserless.dontscan.noclassdeffound.MissingComponentTester' cannot be loaded because of missing class on classpath: com/vaadin/browserless/dontscan/noclassdeffound/MissingComponent",
                    NoClassDefFoundError.class);
            assertPresentComponentTesterRegistered(fixture,
                    "com.vaadin.browserless.dontscan.noclassdeffound");
        }
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOrLogOnTypeNotPresentException()
            throws Exception {
        URLClassLoader fixture = compileWithMissingComponent("typenotpresent",
                """
                        @Tests(MissingComponent.class)
                        public class MissingComponentTester
                                extends ComponentTester<Component> {
                            public MissingComponentTester(Component component) {
                                super(component);
                            }
                        }
                        """);
        try (fixture) {
            assertSkipLoggedBelowDefaultLevel(
                    () -> registerPackageWith(fixture,
                            "com.vaadin.browserless.dontscan.typenotpresent"),
                    "Tester 'com.vaadin.browserless.dontscan.typenotpresent.MissingComponentTester' cannot be loaded because of missing class 'com.vaadin.browserless.dontscan.typenotpresent.MissingComponent' on classpath",
                    TypeNotPresentException.class);
            assertPresentComponentTesterRegistered(fixture,
                    "com.vaadin.browserless.dontscan.typenotpresent");
        }
    }

    @Test
    public void scanForTesters_testerFailsToInitialize_logWarningWithCause() {
        // InitFailureSubclassTester extends InitFailureTester, so whichever
        // is loaded second finds a class whose initialization already failed
        List<CapturedEvent> warnings = scanCapturing(
                () -> TesterRegistry
                        .registerPackages("com.example.initfailure"),
                Level.WARN);
        Assertions.assertEquals(2, warnings.size(), warnings::toString);
        Assertions.assertEquals(Set.of(
                "Tester 'com.example.initfailure.InitFailureTester' cannot be loaded because it or a class it references failed to initialize",
                "Tester 'com.example.initfailure.InitFailureSubclassTester' cannot be loaded because it or a class it references failed to initialize"),
                warnings.stream().map(CapturedEvent::message)
                        .collect(Collectors.toSet()));
        // Each warning carries the initialization failure. Loading a class
        // whose initialization already failed does not always repeat the
        // original message, so it is only required once.
        for (CapturedEvent warning : warnings) {
            Assertions.assertEquals(Level.WARN, warning.level());
            Assertions.assertTrue(
                    causes(warning.throwable()).anyMatch(
                            ExceptionInInitializerError.class::isInstance),
                    warning::toString);
        }
        Assertions.assertTrue(
                warnings.stream()
                        .flatMap(warning -> causes(warning.throwable()))
                        .map(Throwable::getMessage).filter(Objects::nonNull)
                        .anyMatch(message -> message
                                .contains("Tester initialization failed")),
                warnings::toString);
    }

    // A skipped tester is logged as one DEBUG line naming the tester and the
    // missing class, with the cause at TRACE, so nothing is logged at the
    // default level
    private static void assertSkipLoggedBelowDefaultLevel(Executable scan,
            String debugMessage, Class<? extends Throwable> cause) {
        List<CapturedEvent> events = scanCapturing(scan, Level.TRACE);
        Assertions.assertEquals(2, events.size(), events::toString);
        Assertions.assertEquals(Level.DEBUG, events.get(0).level());
        Assertions.assertEquals(debugMessage, events.get(0).message());
        Assertions.assertNull(events.get(0).throwable());
        Assertions.assertEquals(Level.TRACE, events.get(1).level());
        Assertions.assertInstanceOf(cause, events.get(1).throwable());
    }

    // Captures the registry's events at the given level and above, which
    // does not depend on the level configured for the test output
    private static List<CapturedEvent> scanCapturing(Executable scan,
            Level level) {
        // Load the registry first so that only the given scan is captured
        TesterRegistry.registerPackages();
        try (LogCapture capture = LogCapture.start(TesterRegistry.class,
                level)) {
            Assertions.assertDoesNotThrow(scan);
            return capture.events();
        }
    }

    private static Stream<Throwable> causes(Throwable throwable) {
        return Stream.iterate(throwable, Objects::nonNull, Throwable::getCause);
    }

    // The scan of the fixture package registers the loadable tester next to
    // the skipped one, which shows that the scan reached the fixture
    @SuppressWarnings("unchecked")
    private static void assertPresentComponentTesterRegistered(
            URLClassLoader fixture, String pkg) throws ClassNotFoundException {
        Class<?> component = fixture.loadClass(pkg + ".PresentComponent");
        Assertions.assertEquals(pkg + ".PresentComponentTester", TesterRegistry
                .resolve((Class<? extends Component>) component).getName());
    }

    /**
     * Compiles the given tester together with a {@code MissingComponent} into
     * the package {@code com.vaadin.browserless.dontscan.<subPackage>}, then
     * deletes the component class. This is the situation of a tester whose
     * component comes from a dependency the application does not have.
     * <p>
     * A {@code PresentComponent} with its {@code PresentComponentTester} is
     * compiled into the same package and kept, so a test can check that the
     * scan reached the fixture.
     *
     * @return a class loader that sees the compiled tester
     */
    private URLClassLoader compileWithMissingComponent(String subPackage,
            String tester) throws IOException {
        String pkg = "com.vaadin.browserless.dontscan." + subPackage;
        Path sources = Files.createDirectories(classes.resolve("src"));
        Path out = Files.createDirectories(classes.resolve("out"));
        Path component = sources.resolve("MissingComponent.java");
        Files.writeString(component, """
                package %s;
                @com.vaadin.flow.component.Tag("span")
                public class MissingComponent
                        extends com.vaadin.flow.component.Component {
                }
                """.formatted(pkg));
        Path testerSource = sources.resolve("MissingComponentTester.java");
        Files.writeString(testerSource, """
                package %s;
                import com.vaadin.browserless.ComponentTester;
                import com.vaadin.browserless.Tests;
                import com.vaadin.flow.component.Component;
                %s""".formatted(pkg, tester));
        Path present = sources.resolve("PresentComponentTester.java");
        Files.writeString(present, """
                package %s;
                import com.vaadin.browserless.ComponentTester;
                import com.vaadin.browserless.Tests;
                @Tests(PresentComponent.class)
                public class PresentComponentTester
                        extends ComponentTester<PresentComponent> {
                    public PresentComponentTester(PresentComponent component) {
                        super(component);
                    }
                }
                """.formatted(pkg));
        Path presentComponent = sources.resolve("PresentComponent.java");
        Files.writeString(presentComponent, """
                package %s;
                @com.vaadin.flow.component.Tag("span")
                public class PresentComponent
                        extends com.vaadin.flow.component.Component {
                }
                """.formatted(pkg));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-d", out.toString(), "-cp",
                System.getProperty("java.class.path"), component.toString(),
                testerSource.toString(), present.toString(),
                presentComponent.toString());
        Assertions.assertEquals(0, result, "Compiling the fixture failed");
        Files.delete(out.resolve(pkg.replace('.', '/'))
                .resolve("MissingComponent.class"));

        return new URLClassLoader(new URL[] { out.toUri().toURL() },
                getClass().getClassLoader());
    }

    // The scan finds the fixture through the context class loader, like a
    // tester that only an application class loader can see
    private static void registerPackageWith(URLClassLoader loader, String pkg) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            TesterRegistry.registerPackages(pkg);
        } finally {
            thread.setContextClassLoader(original);
        }
    }
}
