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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.vaadin.browserless.internal.Routes;

/**
 * Asserts that {@link SpringBrowserlessApplicationContext} releases the lookup
 * initializer's ThreadLocal when the application context is closed.
 * <p>
 * Plain JUnit 5 — no {@code SpringExtension}/{@code TestContext}, so the
 * {@code TestExecutionListener.afterTestMethod} cleanup does not fire and any
 * leak from {@code app.close()} is observable directly.
 */
class SpringLookupInitializerCloseHookTest {

    @BeforeEach
    @AfterEach
    void resetLookupThreadLocal() {
        BrowserlessTestSpringLookupInitializer.clearApplicationContext();
    }

    @Test
    void close_clearsLookupApplicationContextThreadLocal() {
        Assertions.assertNull(
                BrowserlessTestSpringLookupInitializer.getApplicationContext(),
                "ThreadLocal must start empty");

        try (var springCtx = new AnnotationConfigApplicationContext()) {
            springCtx.refresh();

            var app = SpringBrowserlessApplicationContext.create(new Routes(),
                    springCtx);
            Assertions.assertSame(springCtx,
                    BrowserlessTestSpringLookupInitializer
                            .getApplicationContext(),
                    "create() must publish the application context to the"
                            + " lookup initializer ThreadLocal");

            app.close();

            Assertions.assertNull(
                    BrowserlessTestSpringLookupInitializer
                            .getApplicationContext(),
                    "close() must clear the lookup initializer ThreadLocal so"
                            + " standalone usage doesn't leak across test"
                            + " methods or threads");
        }
    }

    @Test
    void close_isIdempotent_withCloseHook() {
        try (var springCtx = new AnnotationConfigApplicationContext()) {
            springCtx.refresh();

            var app = SpringBrowserlessApplicationContext.create(new Routes(),
                    springCtx);
            app.close();
            Assertions.assertDoesNotThrow(app::close,
                    "Second close() must be a no-op even with hooks"
                            + " registered");
            Assertions.assertNull(
                    BrowserlessTestSpringLookupInitializer
                            .getApplicationContext(),
                    "ThreadLocal must remain cleared after redundant close()");
        }
    }
}
