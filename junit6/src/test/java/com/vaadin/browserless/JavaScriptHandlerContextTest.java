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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.example.javascript.ScheduleView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.flow.server.VaadinServlet;

/**
 * {@link JavaScriptHandler}s configured for an application context with several
 * users and windows.
 */
class JavaScriptHandlerContextTest {

    private BrowserlessApplicationContext app;

    /** Answers with the number of the instance, counted from 1. */
    public static class NumberedBrowser implements JavaScriptHandler {
        private static final AtomicInteger instances = new AtomicInteger();

        private final int number = instances.incrementAndGet();

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            if (execution.isWaitingForResult() && execution.getExpression()
                    .contains(ScheduleView.TIME_ZONE_EXPRESSION)) {
                execution.resolve("Browser" + number);
            }
        }
    }

    public static class NoDefaultConstructor implements JavaScriptHandler {
        public NoDefaultConstructor(String zone) {
        }

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
        }
    }

    @BeforeEach
    void resetInstanceNumbers() {
        NumberedBrowser.instances.set(0);
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.close();
        }
    }

    @Test
    void everyWindow_getsItsOwnHandler() {
        app = new BrowserlessApplicationContext.Builder()
                .withViewPackages(ScheduleView.class)
                .withConfiguration(BrowserlessConfiguration.builder()
                        .withJavaScriptHandlers(NumberedBrowser.class).build())
                .build();
        var user = app.newUser();
        var first = user.newWindow();
        var second = user.newWindow();

        // Each window's view is answered by the instance created for that
        // window; one instance shared by both would answer Browser1 twice
        Assertions.assertEquals("Times shown in Browser1",
                first.navigate(ScheduleView.class).getTimeZone().getText());
        Assertions.assertEquals("Times shown in Browser2",
                second.navigate(ScheduleView.class).getTimeZone().getText(),
                "A new window should get a handler instance of its own");
        Assertions.assertEquals(2, NumberedBrowser.instances.get(),
                "One handler instance should be created per window");
    }

    @Test
    void reusedServlet_getsTheHandlersOfTheNewConfiguration() {
        VaadinServlet[] shared = new VaadinServlet[1];
        BiFunction<Routes, UIFactory, VaadinServlet> sharedServletFactory = (
                routes, uiFactory) -> {
            if (shared[0] == null) {
                shared[0] = new MockVaadinServlet(routes, uiFactory);
            }
            return shared[0];
        };
        new BrowserlessApplicationContext.Builder()
                .withViewPackages(ScheduleView.class)
                .withServletFactory(sharedServletFactory).build().close();

        // The servlet is already initialized, but handlers are read when each
        // UI is created, so this configuration still applies
        app = new BrowserlessApplicationContext.Builder()
                .withViewPackages(ScheduleView.class)
                .withServletFactory(sharedServletFactory)
                .withConfiguration(BrowserlessConfiguration.builder()
                        .withJavaScriptHandlers(NumberedBrowser.class).build())
                .build();

        Assertions.assertEquals("Times shown in Browser1",
                app.newUser().newWindow().navigate(ScheduleView.class)
                        .getTimeZone().getText());
    }

    @Test
    void handlerWithoutNoArgumentConstructor_failsWithItsName() {
        app = new BrowserlessApplicationContext.Builder()
                .withViewPackages(ScheduleView.class)
                .withConfiguration(BrowserlessConfiguration.builder()
                        .withJavaScriptHandlers(NoDefaultConstructor.class)
                        .build())
                .build();
        var user = app.newUser();

        BrowserlessTestSetupException error = Assertions.assertThrows(
                BrowserlessTestSetupException.class, user::newWindow);
        Assertions.assertTrue(
                error.getMessage()
                        .contains(NoDefaultConstructor.class.getName()),
                error.getMessage());
    }
}
