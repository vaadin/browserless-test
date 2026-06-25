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
 * Package-private extension used exclusively by {@code @ExtendWith} on
 * {@link BrowserlessTest}.
 *
 * <p>
 * It only manages the {@code @TestInstance(PER_CLASS)} lifecycle, where the
 * Vaadin environment is shared across the class and must be set up in
 * {@code @BeforeAll}/torn down in {@code @AfterAll}. The default per-method
 * lifecycle is intentionally <em>not</em> handled here: {@link BrowserlessTest}
 * sets up the environment from instance {@code @BeforeEach}/{@code @AfterEach}
 * methods, which JUnit 5 runs after all extension {@code beforeEach} callbacks.
 * Driving per-method setup from a {@code BeforeEachCallback} on this superclass
 * extension would otherwise run before extensions registered on the concrete
 * subclass (e.g. weld-junit5's {@code @EnableAutoWeld}), breaking tests that
 * rely on those extensions for {@code MockVaadin}'s dependencies.
 */
class BrowserlessTestExtension extends AbstractBrowserlessExtension
        implements BeforeAllCallback, AfterAllCallback {

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
