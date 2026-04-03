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
 * Package-private extension used exclusively by {@code @ExtendWith} on
 * {@link BrowserlessTest}. Auto-detects lifecycle from
 * {@code @TestInstance(PER_CLASS)} on the test class.
 */
class BrowserlessTestExtension extends AbstractBrowserlessExtension
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        AfterEachCallback {

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
