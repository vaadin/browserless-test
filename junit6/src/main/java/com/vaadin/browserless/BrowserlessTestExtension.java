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
 * This extension only manages the <em>shared</em> (PER_CLASS) environment, set
 * up in {@code @BeforeAll} and torn down in {@code @AfterAll}. The default
 * per-method lifecycle is intentionally handled by instance
 * {@code @BeforeEach}/{@code @AfterEach} methods on {@link BrowserlessTest},
 * not by a {@code BeforeEachCallback}: a {@code BeforeEachCallback} declared on
 * a superclass is always booted before any extension a subclass adds (e.g.
 * weld-junit5's {@code @EnableAutoWeld}), which would force {@code MockVaadin}
 * setup to run before those extensions' state is ready. Instance lifecycle
 * methods, on the other hand, can be re-declared by the subclass and thus
 * composed correctly with subclass-registered extensions.
 */
class BrowserlessTestExtension extends AbstractBrowserlessExtension
        implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (isPerClass(ctx)) {
            Object testInstance = ctx.getTestInstance().orElse(null);
            // Run the shared setup while the per-method guard is still open,
            // so initVaadinEnvironment() (or a subclass override of it) runs.
            doInit(testInstance, ctx);
            // From now on the per-method instance hooks must stand down: the
            // environment is shared across all tests in the class.
            setExtensionManagedLifecycle(testInstance, true);
        }
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        if (isPerClass(ctx)) {
            // Re-open the guard so the shared teardown actually runs.
            setExtensionManagedLifecycle(ctx.getTestInstance().orElse(null),
                    false);
            doCleanup();
        }
    }

    private void setExtensionManagedLifecycle(Object testInstance,
            boolean managed) {
        if (testInstance instanceof BrowserlessTest browserlessTest) {
            browserlessTest.setExtensionManagedLifecycle(managed);
        }
    }

    private boolean isPerClass(ExtensionContext ctx) {
        return ctx.getTestInstanceLifecycle()
                .filter(l -> l == TestInstance.Lifecycle.PER_CLASS).isPresent();
    }
}
