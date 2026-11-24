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

import java.util.ArrayList;
import java.util.List;

import com.example.javascript.ScheduleView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * A view reads the user's time zone from the browser while it is built. A
 * {@link JavaScriptHandler} configured for the test environment answers like
 * the browser does, so the view renders as in a browser, without the test doing
 * anything, also after a reload. A handler can also call a return channel the
 * browser calls as soon as the JavaScript runs.
 */
@ViewPackages(classes = ScheduleView.class)
@BrowserlessTestConfig(javaScriptHandlers = {
        JavaScriptHandlerTest.BrowserInHelsinki.class,
        JavaScriptHandlerTest.ClipboardAllowed.class })
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

    public static class ClipboardAllowed implements JavaScriptHandler {
        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            if (execution.getExpression()
                    .contains("navigator.clipboard.writeText")) {
                execution.invokeReturnChannel(true);
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

        ScheduleView reloaded = (ScheduleView) reload();

        Assertions.assertEquals("Times shown in Europe/Helsinki",
                reloaded.getTimeZone().getText());
    }

    @Test
    void handler_invokesAReturnChannelLive_andWhatItSchedulesIsSentAfter() {
        Div copy = new Div();
        UI.getCurrent().add(copy);
        List<String> received = new ArrayList<>();
        ReturnChannelRegistration channel = copy.getElement().getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> {
                    received.add(arguments.toString());
                    copy.getElement().callJsFunction("showCopied");
                });
        ExecutedJavaScript executed = ExecutedJavaScript.capture();

        copy.getElement().executeJs(
                "navigator.clipboard.writeText($0).then(() => $1(true))",
                "text", channel);
        roundTrip();

        Assertions.assertEquals(List.of("[true]"), received);
        List<ExecutedJavaScript.Execution> sent = executed.getAll();
        Assertions.assertEquals(2, sent.size(), sent.toString());
        Assertions.assertTrue(sent.get(0).getExpression().contains("clipboard"),
                sent.toString());
        Assertions.assertEquals(List.of(List.of()),
                executed.getFunctionCalls(copy, "showCopied"));
        Assertions.assertTrue(
                sent.get(1).getParameters().contains("showCopied"),
                "JavaScript the channel handler schedules should be sent "
                        + "after the JavaScript that triggered it: " + sent);
    }
}
