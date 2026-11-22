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

import java.util.List;

import com.example.javascript.ScheduleView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;

/**
 * The capture each UI gets when it is created, which holds the JavaScript sent
 * before the test could start a capture of its own.
 */
class UIJavaScriptCaptureTest {

    @Nested
    @ViewPackages(classes = ScheduleView.class)
    class OnBaseClass extends BrowserlessTest {

        @Test
        void reload_newUIHasItsOwnCapture_withWhatItsViewSentWhileBuilt() {
            ScheduleView view = navigate(ScheduleView.class);
            ExecutedJavaScript beforeReload = getExecutedJavaScript();

            ScheduleView reloaded = (ScheduleView) reload();
            ExecutedJavaScript afterReload = getExecutedJavaScript();

            Assertions.assertEquals(List.of(view.getElement()),
                    findTimeZoneRequests(beforeReload));
            Assertions.assertEquals(List.of(reloaded.getElement()),
                    findTimeZoneRequests(afterReload));
        }

        @Test
        void stop_isRefused_andTheCaptureKeepsEverything() {
            ScheduleView view = navigate(ScheduleView.class);
            ExecutedJavaScript executed = getExecutedJavaScript();

            Assertions.assertThrows(IllegalStateException.class,
                    executed::stop);
            view.getElement().executeJs("this.print()");

            Assertions.assertEquals(List.of(view.getElement()),
                    findTimeZoneRequests(executed));
            Assertions.assertTrue(executed.getAll().getLast().getExpression()
                    .contains("this.print()"));
        }
    }

    @Nested
    @ViewPackages(classes = ScheduleView.class)
    class OnExtension {

        @RegisterExtension
        BrowserlessExtension ext = new BrowserlessExtension();

        @Test
        void navigate_viewsJavaScriptIsInTheUIsCapture() {
            ScheduleView view = ext.navigate(ScheduleView.class);

            Assertions.assertEquals(List.of(view.getElement()),
                    findTimeZoneRequests(ext.getExecutedJavaScript()));
        }
    }

    @Test
    void windows_eachHaveTheirOwnCapture() {
        try (BrowserlessApplicationContext app = BrowserlessApplicationContext
                .create(ScheduleView.class)) {
            BrowserlessUserContext user = app.newUser();
            BrowserlessUIContext window1 = user.newWindow();
            BrowserlessUIContext window2 = user.newWindow();

            ScheduleView view1 = window1.navigate(ScheduleView.class);
            ScheduleView view2 = window2.navigate(ScheduleView.class);

            Assertions.assertEquals(List.of(view1.getElement()),
                    findTimeZoneRequests(window1.getExecutedJavaScript()));
            Assertions.assertEquals(List.of(view2.getElement()),
                    findTimeZoneRequests(window2.getExecutedJavaScript()));
        }
    }

    @Test
    void uiNotCreatedByBrowserlessTest_hasNoCapture() {
        IllegalStateException e = Assertions.assertThrows(
                IllegalStateException.class,
                () -> BrowserlessDSL.getExecutedJavaScript(new UI()));
        Assertions.assertTrue(
                e.getMessage().contains("ExecutedJavaScript.capture(ui)"));
    }

    private static List<Element> findTimeZoneRequests(
            ExecutedJavaScript executed) {
        return executed.getAll().stream()
                .filter(execution -> execution.getExpression()
                        .contains(ScheduleView.TIME_ZONE_EXPRESSION))
                .map(ExecutedJavaScript.Execution::getElement).toList();
    }
}
