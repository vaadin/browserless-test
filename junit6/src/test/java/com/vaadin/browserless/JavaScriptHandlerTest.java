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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A view reads the user's time zone from the browser while it is built. A
 * {@link JavaScriptHandler} configured for the test environment answers like
 * the browser does, so the view renders as in a browser, without the test doing
 * anything, also after a reload.
 */
@ViewPackages(classes = ScheduleView.class)
@BrowserlessTestConfig(javaScriptHandlers = JavaScriptHandlerTest.BrowserInHelsinki.class)
class JavaScriptHandlerTest extends BrowserlessTest {

    public static class BrowserInHelsinki implements JavaScriptHandler {
        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            if (execution.isWaitingForResult() && execution.getExpression()
                    .contains(ScheduleView.TIME_ZONE_EXPRESSION)) {
                execution.resolve("Europe/Helsinki");
            }
        }
    }

    @Test
    void scheduleView_showsTimesInTheBrowsersTimeZone() {
        ScheduleView view = navigate(ScheduleView.class);

        Assertions.assertEquals("Times shown in Europe/Helsinki",
                view.getTimeZone().getText());
    }

    @Test
    void reload_newUIAsksTheBrowserAgain_andGetsAnswered() {
        navigate(ScheduleView.class);

        ScheduleView reloaded = reload(ScheduleView.class);

        Assertions.assertEquals("Times shown in Europe/Helsinki",
                reloaded.getTimeZone().getText());
    }
}
