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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

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
        String log = captureStdErr(() -> TesterRegistry.registerPackages(
                "com.vaadin.browserless.dontscan.classnotfound"));
        Assertions.assertEquals("", log);
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOrLogOnNoClassDefFoundError()
            throws IOException {
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
        String log = captureStdErr(() -> registerPackageWith(fixture,
                "com.vaadin.browserless.dontscan.noclassdeffound"));
        Assertions.assertEquals("", log);
    }

    @Test
    public void scanForTesters_testerForClassNotInClasspath_doNotThrowOrLogOnTypeNotPresentException()
            throws IOException {
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
        String log = captureStdErr(() -> registerPackageWith(fixture,
                "com.vaadin.browserless.dontscan.typenotpresent"));
        Assertions.assertEquals("", log);
    }

    @Test
    public void scanForTesters_testerFailsToInitialize_logWarningWithCause() {
        // InitFailureSubclassTester extends InitFailureTester, so whichever
        // is loaded second finds a class whose initialization already failed
        String log = captureStdErr(() -> TesterRegistry.registerPackages(
                "com.vaadin.browserless.dontscan.initfailure"));
        Assertions.assertTrue(log.contains("WARN"), log);
        Assertions.assertTrue(log.contains(
                "Tester 'com.vaadin.browserless.dontscan.initfailure.InitFailureTester' cannot be loaded because it or a class it references failed to initialize"),
                log);
        Assertions.assertTrue(log.contains(
                "Tester 'com.vaadin.browserless.dontscan.initfailure.InitFailureSubclassTester' cannot be loaded because it or a class it references failed to initialize"),
                log);
        Assertions.assertTrue(log.contains("Tester initialization failed"),
                log);
        // Each warning carries the initialization failure. A class whose
        // initialization already failed in an earlier scan does not repeat
        // the original message, so only the error type is checked.
        String[] warnings = log.split("cannot be loaded because", -1);
        Assertions.assertEquals(3, warnings.length, log);
        Assertions.assertTrue(
                warnings[1].contains("ExceptionInInitializerError"), log);
        Assertions.assertTrue(
                warnings[2].contains("ExceptionInInitializerError"), log);
    }

    private String captureStdErr(Executable scan) {
        // Load the registry first so that only the given scan is captured
        TesterRegistry.registerPackages();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            Assertions.assertDoesNotThrow(scan);
        } finally {
            System.setErr(originalErr);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    /**
     * Compiles the given tester together with a {@code MissingComponent} into
     * the package {@code com.vaadin.browserless.dontscan.<subPackage>}, then
     * deletes the component class. This is the situation of a tester whose
     * component comes from a dependency the application does not have.
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
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-d", out.toString(), "-cp",
                System.getProperty("java.class.path"), component.toString(),
                testerSource.toString());
        Assertions.assertEquals(0, result, "Compiling the fixture failed");
        Files.delete(out.resolve(pkg.replace('.', '/'))
                .resolve("MissingComponent.class"));

        return new URLClassLoader(new URL[] { out.toUri().toURL() },
                getClass().getClassLoader());
    }

    // The scan finds the fixture through the context class loader, like a
    // tester that only an application class loader can see
    private static void registerPackageWith(URLClassLoader loader, String pkg)
            throws IOException {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try (loader) {
            TesterRegistry.registerPackages(pkg);
        } finally {
            thread.setContextClassLoader(original);
        }
    }
}
