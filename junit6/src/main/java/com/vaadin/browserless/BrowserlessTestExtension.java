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
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Opt-in JUnit 5 extension that lets a {@link BrowserlessTest} subclass share a
 * single Vaadin environment across all its test methods using
 * {@code @TestInstance(PER_CLASS)}.
 *
 * <p>
 * {@link BrowserlessTest} manages the default per-method lifecycle on its own
 * (from instance {@code @BeforeEach}/{@code @AfterEach} methods) and does not
 * support {@code PER_CLASS}. Register this extension explicitly to enable it:
 *
 * <pre>
 * {@code
 * &#64;TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * &#64;ExtendWith(BrowserlessTestExtension.class)
 * class MyStatefulTest extends BrowserlessTest {
 * }
 * }
 * </pre>
 *
 * <p>
 * It only acts on the {@code PER_CLASS} lifecycle, setting up the shared
 * environment in {@code @BeforeAll} and tearing it down in {@code @AfterAll};
 * for the per-method lifecycle it is a no-op (handled by
 * {@link BrowserlessTest} instead). Because it is registered explicitly on the
 * concrete test class, users can order it relative to other required extensions
 * (such as weld-junit5's {@code @EnableAutoWeld}) as needed.
 *
 * @see BrowserlessTest
 * @see BrowserlessExtension
 * @see BrowserlessClassExtension
 */
public class BrowserlessTestExtension extends AbstractBrowserlessExtension
        implements BeforeAllCallback, AfterAllCallback {

    /**
     * Creates a new extension.
     */
    public BrowserlessTestExtension() {
    }

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (isPerClass(ctx)) {
            Object testInstance = ctx.getTestInstance().orElse(null);
            if (testInstance instanceof BrowserlessTest test) {
                test.perClassLifecycle = true;
            }
            doInit(testInstance, ctx);
        }
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        if (isPerClass(ctx)) {
            doCleanup();
        }
    }

    private boolean isPerClass(ExtensionContext ctx) {
        return ctx.getTestInstanceLifecycle()
                .filter(l -> l == TestInstance.Lifecycle.PER_CLASS).isPresent();
    }
}
