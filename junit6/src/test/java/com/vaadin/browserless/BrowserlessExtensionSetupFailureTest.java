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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.example.base.WelcomeView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Verifies that a Vaadin environment whose setup fails halfway through is still
 * torn down, so that its thread locals do not bleed into the next test.
 */
class BrowserlessExtensionSetupFailureTest {

    @Test
    void failingSetup_stillTearsDownTheEnvironment() {
        FailingTest test = new FailingTest();
        TestableExtension extension = new TestableExtension();

        Assertions.assertThrows(IllegalStateException.class,
                () -> extension.init(test, new StubContext(FailingTest.class)));
        Assertions.assertNotNull(VaadinService.getCurrent(),
                "The environment should be half built, otherwise this test "
                        + "would not exercise the clean up path");

        extension.cleanup();

        Assertions.assertNull(VaadinService.getCurrent(),
                "A setup failing after MockVaadin.setup() must not leave the "
                        + "Vaadin service behind for the next test");
        Assertions.assertNull(VaadinSession.getCurrent(),
                "A failed setup must not leave a Vaadin session behind");
        Assertions.assertNull(UI.getCurrent(),
                "A failed setup must not leave a UI behind");
    }

    @ViewPackages(classes = WelcomeView.class)
    static class FailingTest extends BaseBrowserlessTest {

        @Override
        protected void initVaadinEnvironment() {
            super.initVaadinEnvironment();
            // Stands for anything that can fail once MockVaadin.setup() has
            // already installed the Vaadin thread locals.
            throw new IllegalStateException("setup failed");
        }

        @Override
        protected String testingEngine() {
            return "JUnit 6";
        }
    }

    static class TestableExtension extends AbstractBrowserlessExtension {
        void init(Object testInstance, ExtensionContext ctx) {
            doInit(testInstance, ctx);
        }

        void cleanup() {
            doCleanup();
        }
    }

    /**
     * Minimal {@link ExtensionContext} exposing just the test class, which is
     * all the configuration resolution needs; everything else is out of scope
     * for this test and fails loudly if it is ever reached.
     */
    private record StubContext(Class<?> testClass) implements ExtensionContext {

        @Override
        public Optional<Class<?>> getTestClass() {
            return Optional.of(testClass);
        }

        @Override
        public List<Class<?>> getEnclosingTestClasses() {
            return List.of();
        }

        @Override
        public Optional<Method> getTestMethod() {
            return Optional.empty();
        }

        @Override
        public Optional<ExtensionContext> getParent() {
            return Optional.empty();
        }

        @Override
        public ExtensionContext getRoot() {
            return this;
        }

        @Override
        public String getUniqueId() {
            return testClass.getName();
        }

        @Override
        public String getDisplayName() {
            return testClass.getSimpleName();
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public Optional<AnnotatedElement> getElement() {
            return Optional.of(testClass);
        }

        @Override
        public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
            return Optional.empty();
        }

        @Override
        public Optional<Object> getTestInstance() {
            return Optional.empty();
        }

        @Override
        public Optional<TestInstances> getTestInstances() {
            return Optional.empty();
        }

        @Override
        public Optional<Throwable> getExecutionException() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getConfigurationParameter(String key) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> getConfigurationParameter(String key,
                Function<? super String, ? extends T> transformer) {
            return Optional.empty();
        }

        @Override
        public void publishReportEntry(Map<String, String> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publishFile(String name, MediaType mediaType,
                ThrowingConsumer<Path> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publishDirectory(String name,
                ThrowingConsumer<Path> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Store getStore(Namespace namespace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Store getStore(StoreScope scope, Namespace namespace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ExecutionMode getExecutionMode() {
            return ExecutionMode.SAME_THREAD;
        }

        @Override
        public ExecutableInvoker getExecutableInvoker() {
            throw new UnsupportedOperationException();
        }
    }
}
