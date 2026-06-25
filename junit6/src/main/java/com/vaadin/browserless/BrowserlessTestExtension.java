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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that sets up and tears down a browserless Vaadin
 * environment, registered with {@code @ExtendWith}.
 *
 * <p>
 * The lifecycle is detected from the test class: with the default per-method
 * lifecycle a fresh environment is created before each test and torn down
 * after; with {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)} a single
 * environment is shared across the class (set up in {@code @BeforeAll}, torn
 * down in {@code @AfterAll}).
 *
 * <pre>
 * {@code
 * &#64;TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * &#64;ExtendWith(BrowserlessTestExtension.class)
 * &#64;ViewPackages(classes = MyView.class)
 * class MyStatefulTest {
 * }
 * }
 * </pre>
 *
 * <p>
 * This is the primary way to obtain a shared per-class environment, since
 * {@link BrowserlessTest} itself only supports the per-method lifecycle.
 * Because the extension is registered explicitly on the concrete test class, it
 * can be ordered relative to other required extensions (such as weld-junit5's
 * {@code @EnableAutoWeld}) as the test needs. It is meant to be used on its
 * own, not in combination with {@link BrowserlessTest} (which already manages
 * the environment via instance lifecycle methods).
 *
 * @see BrowserlessTest
 * @see BrowserlessExtension
 * @see BrowserlessClassExtension
 */
public class BrowserlessTestExtension extends AbstractBrowserlessExtension
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        AfterEachCallback {

    /**
     * Creates a new extension.
     */
    public BrowserlessTestExtension() {
    }

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (isPerClass(ctx)) {
            doInit(ctx.getTestInstance().orElse(null), ctx);
        }
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        if (isPerClass(ctx)) {
            doCleanup();
        }
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        if (!isPerClass(ctx)) {
            doInit(ctx.getTestInstance().orElse(null), ctx);
        }
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        if (!isPerClass(ctx)) {
            doCleanup();
        }
    }

    private boolean isPerClass(ExtensionContext ctx) {
        return ctx.getTestInstanceLifecycle()
                .filter(l -> l == TestInstance.Lifecycle.PER_CLASS).isPresent();
    }
}
