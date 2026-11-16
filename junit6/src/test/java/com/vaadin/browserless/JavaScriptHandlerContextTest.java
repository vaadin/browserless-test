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

import com.example.javascript.ScheduleView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link JavaScriptHandler}s configured for an application context with several
 * users and windows.
 */
class JavaScriptHandlerContextTest {

    private BrowserlessApplicationContext app;

    /** Answers with the number of calls this instance has seen so far. */
    public static class CountingBrowser implements JavaScriptHandler {
        private int answered;

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            if (execution.isWaitingForResult() && execution.getExpression()
                    .contains(ScheduleView.TIME_ZONE_EXPRESSION)) {
                execution.resolve("Zone" + ++answered);
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
                        .withJavaScriptHandlers(CountingBrowser.class).build())
                .build();
        var user = app.newUser();
        var first = user.newWindow();
        var second = user.newWindow();

        Assertions.assertEquals("Times shown in Zone1",
                first.navigate(ScheduleView.class).getTimeZone().getText());
        Assertions.assertEquals("Times shown in Zone1",
                second.navigate(ScheduleView.class).getTimeZone().getText(),
                "A new window should get a handler instance of its own");
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
