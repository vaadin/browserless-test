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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsSnippets;

class ExecutedJavaScriptTest extends BrowserlessTest {

    @JsDefinition
    public interface OtherFocusJs extends JsInvokers {
        @JsExpression("this.focus()")
        void focus();

        @JsExpression("this.focus({preventScroll: $0})")
        void focus(boolean preventScroll);

        @JsExpression("this.scrollIntoView()")
        void scrollIntoView();
    }

    private Div calendar;
    private TextField field;
    private Button button;

    @BeforeEach
    void init() {
        calendar = new Div();
        field = new TextField("Name");
        button = new Button("Save");
        Div container = new Div(calendar, field, button);
        getCurrentView().getElement().appendChild(container.getElement());
    }

    // Two ways a test meets the JavaScript an application sends. Handlers play
    // the browser: they act on each call the moment it is sent, so the
    // application behaves as with a browser and the test checks the outcome,
    // see JavaScriptHandlerTest. A capture is for JavaScript nothing can
    // simulate, such as a web component's own API: the test checks afterwards
    // which calls were sent, as below.

    @Test
    void weekButton_switchesTheCalendarView_onlyWhenItChanges() {
        PlannerView planner = showPlanner();
        ExecutedJavaScript executed = ExecutedJavaScript.capture();

        test(planner.week).click();
        test(planner.week).click();

        Assertions.assertEquals(List.of(List.of("timeGridWeek")),
                executed.getFunctionCalls(planner.calendar, "changeView"));
    }

    private PlannerView showPlanner() {
        PlannerView planner = new PlannerView();
        getCurrentView().getElement().appendChild(planner.getElement());
        return planner;
    }

    /**
     * Wraps a client-side calendar web component, the way an add-on does: the
     * server side keeps the state and calls the component's own JavaScript API
     * when it changes.
     */
    @Tag("event-calendar")
    static class EventCalendar extends Component {
        private String view = "dayGridMonth";

        void setView(String view) {
            if (!view.equals(this.view)) {
                this.view = view;
                getElement().callJsFunction("changeView", view);
            }
        }
    }

    static class PlannerView extends Div {
        final EventCalendar calendar = new EventCalendar();
        final Button week = new Button("Week",
                event -> calendar.setView("timeGridWeek"));

        PlannerView() {
            add(week, calendar);
        }
    }

    @Test
    void functionCalls_capturedWithArguments_alongWithFocusCall() {
        button.addClickListener(event -> {
            calendar.getElement().callJsFunction("setOptions", "week", 7);
            field.getElement().callJsFunction("setOptions", "ignored");
            calendar.getElement().callJsFunction("render");
            field.focus();
            button.blur();
            calendar.getElement().executeJs(OtherFocusJs.class).focus();
        });

        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        test(button).click();

        Assertions.assertEquals(List.of(List.of("week", 7)),
                executed.getFunctionCalls(calendar, "setOptions"));
        Assertions.assertEquals(
                List.of("focus " + field.getElement(),
                        "blur " + button.getElement()),
                executed.getExecutions().stream().filter(execution -> execution
                        .getJsCall()
                        .filter(call -> call
                                .definitionType() == Focusable.FocusJs.class)
                        .isPresent())
                        .map(execution -> execution.getJsCall().orElseThrow()
                                .methodName() + " " + execution.getElement())
                        .toList());
        Assertions.assertEquals(List.of("setOptions", "setOptions", "render"),
                executed.getCalls(Element.CallFunctionJs.class, "callFunction")
                        .stream().map(call -> call.arguments().get(0))
                        .toList());
        Assertions.assertEquals(List.of(Focusable.FocusJs.class),
                executed.getCalls(Focusable.FocusJs.class, "focus").stream()
                        .map(JsCall::definitionType).toList(),
                "A focus method of another definition should not match");
        Assertions.assertTrue(test(field).isFocused(),
                "The focus call should still be simulated");
    }

    @Test
    void functionCall_scheduledByListenerDuringDispatch_capturedOnNextRoundTrip() {
        field.addFocusListener(
                event -> calendar.getElement().callJsFunction("render"));
        button.addClickListener(event -> field.focus());
        ExecutedJavaScript executed = ExecutedJavaScript.capture();

        test(button).click();
        roundTrip();

        Assertions.assertEquals(List.of(List.of()),
                executed.getFunctionCalls(calendar, "render"));
    }

    @Test
    void functionCall_scheduledByTest_capturedWithoutRoundTrip() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().callJsFunction("render");

        Assertions.assertEquals(List.of(List.of()),
                executed.getFunctionCalls(calendar, "render"));
    }

    @Test
    void pendingJavaScriptTakenByTest_failsNextDispatchOnce() {
        takePendingJavaScriptLikeATest();

        IllegalStateException e = Assertions
                .assertThrows(IllegalStateException.class, () -> roundTrip());
        Assertions.assertTrue(
                e.getMessage().contains("dumpPendingJavaScriptInvocations()")
                        && e.getMessage()
                                .contains("ExecutedJavaScript.capture()"),
                e.getMessage());

        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().callJsFunction("render");
        roundTrip();
        Assertions.assertEquals(List.of(List.of()),
                executed.getFunctionCalls(calendar, "render"));
    }

    @Test
    void stop_whenFinalDispatchFails_stillStopsTheCapture() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        takePendingJavaScriptLikeATest();

        Assertions.assertThrows(IllegalStateException.class, executed::stop);

        calendar.getElement().callJsFunction("refresh");
        roundTrip();
        Assertions.assertEquals(List.of(),
                executed.getFunctionCalls(calendar, "refresh"),
                "A capture whose stop() failed should not keep collecting");
    }

    private void takePendingJavaScriptLikeATest() {
        UIInternals internals = UI.getCurrent().getInternals();
        Instant lastSent = internals.getLastUpdateSentTimestamp();
        // Make sure the test's take gets a timestamp of its own
        while (!Instant.now().isAfter(lastSent)) {
            Thread.onSpinWait();
        }
        calendar.getElement().callJsFunction("render");
        internals.getStateTree().runExecutionsBeforeClientResponse();
        Assertions.assertFalse(
                internals.dumpPendingJavaScriptInvocations().isEmpty(),
                "The test should take JavaScript that was pending");
    }

    @Test
    void resolve_runsTheApplicationsSuccessHandler() {
        AtomicReference<Integer> result = new AtomicReference<>();
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("return this.childElementCount")
                .then(Integer.class, result::set);

        ExecutedJavaScript.Execution execution = executed.getExecutions()
                .get(0);
        Assertions.assertTrue(execution.isWaitingForResult());
        execution.resolve(3);

        Assertions.assertEquals(3, result.get());
        Assertions.assertFalse(execution.isWaitingForResult(),
                "An answered call should no longer wait for a result");
        Assertions.assertThrows(IllegalStateException.class,
                () -> execution.resolve(4), "A browser answers only once");
    }

    @Test
    void reject_runsTheApplicationsErrorHandler() {
        AtomicReference<String> error = new AtomicReference<>();
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("return this.missing()").then(
                value -> Assertions.fail("Should not succeed"), error::set);
        calendar.getElement().executeJs("this.render()");

        List<ExecutedJavaScript.Execution> executions = executed
                .getExecutions();
        executions.get(0).reject("missing is not a function");

        Assertions.assertEquals("missing is not a function", error.get());
        Assertions.assertThrows(IllegalStateException.class,
                () -> executions.get(1).reject("failed"),
                "Nobody waits for the result, so there is nothing to answer");
    }

    @Test
    void capture_keepsEverythingSentAfterItStarted_acrossRoundTripsAndOtherCaptures() {
        calendar.getElement().callJsFunction("render");
        ExecutedJavaScript first = ExecutedJavaScript.capture();
        calendar.getElement().callJsFunction("refresh");
        roundTrip();
        ExecutedJavaScript second = ExecutedJavaScript.capture();
        calendar.getElement().callJsFunction("update");
        roundTrip();

        Assertions.assertEquals(List.of("refresh", "update"),
                functionNames(first));
        Assertions.assertEquals(List.of("update"), functionNames(second));
        Assertions.assertEquals(List.of("refresh", "update"),
                functionNames(first), "Querying should not consume anything");
    }

    @Test
    void roundTripInListenerDuringDispatch_runsBeforeClientResponseTasks() {
        AtomicReference<Boolean> ranInListener = new AtomicReference<>();
        field.addFocusListener(event -> {
            AtomicReference<Boolean> ran = new AtomicReference<>(false);
            UI.getCurrent().beforeClientResponse(field,
                    context -> ran.set(true));
            roundTrip();
            ranInListener.set(ran.get());
        });
        button.addClickListener(event -> field.focus());

        test(button).click();

        Assertions.assertEquals(true, ranInListener.get());
    }

    @Test
    void stop_keepsWhatWasCaptured_includingPendingJavaScript() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().callJsFunction("render");
        executed.stop();
        calendar.getElement().callJsFunction("refresh");
        roundTrip();

        Assertions.assertEquals(List.of("render"), functionNames(executed));
    }

    @Test
    void clear_forgetsWhatWasCaptured_andKeepsCapturingWhatIsAnsweredAfterwards() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("return this.view").then(String.class,
                view -> calendar.getElement().callJsFunction("render", view));
        calendar.getElement().callJsFunction("refresh");

        ExecutedJavaScript.Execution viewRequest = executed.getExecutions()
                .get(0);
        calendar.getElement().callJsFunction("highlight");
        executed.clear();
        viewRequest.resolve("week");

        Assertions.assertEquals(List.of("highlight", "render"),
                functionNames(executed));
        Assertions.assertEquals(List.of(List.of("week")),
                executed.getFunctionCalls(calendar, "render"));
    }

    @Test
    void getExecutionsForElement_keepsOnlyItsJavaScript_withExpressionAndParameters() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("this.render($0)", 5);
        field.getElement().executeJs("this.select()");
        UI.getCurrent().getPage().executeJs("window.render($0)", "week");
        calendar.getElement().executeJs(OtherFocusJs.class).focus();

        List<ExecutedJavaScript.Execution> onCalendar = executed
                .getExecutions(calendar);
        Assertions.assertEquals(2, onCalendar.size(), onCalendar.toString());
        Assertions.assertEquals(OtherFocusJs.class,
                onCalendar.get(1).getJsCall().orElseThrow().definitionType());
        Assertions.assertEquals(onCalendar,
                executed.getExecutions(calendar.getElement()));

        ExecutedJavaScript.Execution onElement = onCalendar.get(0);
        Assertions.assertTrue(
                onElement.getExpression().contains("this.render($0)"),
                onElement.getExpression());
        Assertions.assertEquals(List.of(5, calendar.getElement()),
                onElement.getParameters());

        List<ExecutedJavaScript.Execution> onUI = executed
                .getExecutions(UI.getCurrent());
        Assertions.assertEquals(1, onUI.size(), onUI.toString());
        ExecutedJavaScript.Execution onPage = onUI.get(0);
        Assertions.assertEquals("window.render($0)", onPage.getExpression());
        Assertions.assertEquals(List.of("week"), onPage.getParameters());
    }

    @Test
    void invokeReturnChannel_runsTheChannelHandler_andSendsWhatItSchedules() {
        List<String> received = new ArrayList<>();
        ReturnChannelRegistration channel = registerChannel(calendar,
                arguments -> {
                    received.add(arguments.toString());
                    calendar.getElement().callJsFunction("render");
                });
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("this.listen($0, $1)", "change",
                channel);
        ExecutedJavaScript.Execution execution = executed.getExecutions()
                .get(0);
        // Records what the client gets without sending anything itself, as
        // querying the capture would
        List<List<Object>> sent = new ArrayList<>();
        JavaScriptDispatcher.forUI(UI.getCurrent())
                .addHandler(invocation -> sent
                        .add(invocation.getInvocation().getParameters()));

        execution.invokeReturnChannel("week", 7);
        Assertions.assertEquals(1, sent.size(),
                "JavaScript the handler schedules should be sent right away");
        Assertions.assertTrue(sent.get(0).contains("render"), sent.toString());
        execution.invokeReturnChannel(
                JacksonUtils.createObjectNode().put("a", 1));
        execution.invokeReturnChannel((Object) null);
        execution.invokeReturnChannel((Object[]) null);

        Assertions.assertEquals(
                List.of("[\"week\",7]", "[{\"a\":1}]", "[null]", "[null]"),
                received);
        Assertions.assertEquals(
                List.of(List.of(), List.of(), List.of(), List.of()),
                executed.getFunctionCalls(calendar, "render"));
    }

    @Test
    void invokeReturnChannel_withSeveralChannels_picksOneByParameterIndex() {
        List<String> received = new ArrayList<>();
        ReturnChannelRegistration first = registerChannel(calendar,
                arguments -> received.add("first"));
        ReturnChannelRegistration second = registerChannel(calendar,
                arguments -> received.add("second"));
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("this.listen($0, $1)", first, second);
        ExecutedJavaScript.Execution execution = executed.getExecutions()
                .get(0);

        Assertions.assertThrows(IllegalStateException.class,
                () -> execution.invokeReturnChannel("x"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> execution.invokeReturnChannelAt(2, "x"),
                "Parameter 2 is the element, not a channel");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> execution.invokeReturnChannelAt(3, "x"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> execution.invokeReturnChannelAt(-1, "x"));
        calendar.getElement().executeJs("this.listen($0)", "change");
        Assertions.assertThrows(IllegalStateException.class,
                () -> executed.getExecutions().get(1).invokeReturnChannel("x"),
                "Nothing to invoke without a channel");
        execution.invokeReturnChannelAt(1);

        Assertions.assertEquals(List.of("second"), received);
    }

    @Test
    void invokeReturnChannel_whenFlowWouldIgnoreTheMessage_throws() {
        List<String> received = new ArrayList<>();
        ReturnChannelRegistration channel = registerChannel(button,
                arguments -> received.add(arguments.toString()));
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        button.getElement().executeJs("this.listen($0)", channel);
        ExecutedJavaScript.Execution execution = executed.getExecutions()
                .get(0);

        // As in Flow, a registered channel reaches an element behind a modal
        Div dialog = new Div();
        UI.getCurrent().add(dialog);
        test(dialog).setModal(true);
        Assertions.assertTrue(button.getElement().getNode().isInert());
        execution.invokeReturnChannel("inert");
        UI.getCurrent().remove(dialog);

        button.setEnabled(false);
        assertRefused("is disabled",
                () -> execution.invokeReturnChannel("disabled"));
        channel.setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);
        execution.invokeReturnChannel("always");

        button.setVisible(false);
        assertRefused("is not visible",
                () -> execution.invokeReturnChannel("hidden"));
        button.setVisible(true);

        channel.remove();
        assertRefused("has been removed",
                () -> execution.invokeReturnChannel("removed"));

        button.setEnabled(true);
        ReturnChannelRegistration other = registerChannel(button,
                arguments -> received.add(arguments.toString()));
        button.getElement().executeJs("this.listen($0)", other);
        ExecutedJavaScript.Execution later = executed.getExecutions().get(1);
        button.getElement().removeFromParent();
        assertRefused("is not attached",
                () -> later.invokeReturnChannel("detached"));

        Assertions.assertEquals(List.of("[\"inert\"]", "[\"always\"]"),
                received);
    }

    private static void assertRefused(String reason, Executable invocation) {
        String message = Assertions
                .assertThrows(IllegalStateException.class, invocation)
                .getMessage();
        Assertions.assertTrue(message.endsWith(reason), message);
    }

    private static ReturnChannelRegistration registerChannel(
            Component component, SerializableConsumer<ArrayNode> handler) {
        return component.getElement().getNode()
                .getFeature(ReturnChannelMap.class).registerChannel(handler);
    }

    private static List<Object> functionNames(ExecutedJavaScript executed) {
        return executed.getExecutions().stream().map(execution -> execution
                .getJsCall().orElseThrow().arguments().get(0)).toList();
    }

    @Test
    void getCallsByMethod_matchesTheCalledMethodByName_ignoringArguments() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs(OtherFocusJs.class).focus();
        calendar.getElement().executeJs(OtherFocusJs.class).scrollIntoView();
        calendar.getElement().executeJs(OtherFocusJs.class).focus(true);

        List<JsCall> focusCalls = List.of(
                new JsCall(OtherFocusJs.class, "focus", List.of()),
                new JsCall(OtherFocusJs.class, "focus", List.of(true)));
        Assertions.assertEquals(focusCalls,
                executed.getCalls(OtherFocusJs.class, OtherFocusJs::focus));
        Assertions.assertEquals(focusCalls,
                executed.getCalls(OtherFocusJs.class, js -> js.focus(false)));
        Assertions.assertEquals(
                List.of(new JsCall(OtherFocusJs.class, "scrollIntoView",
                        List.of())),
                executed.getCalls(OtherFocusJs.class,
                        OtherFocusJs::scrollIntoView));
    }

    @Test
    void getCallsByMethod_refusesAnythingButOneCallOfADefinition() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();

        IllegalArgumentException none = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> executed.getCalls(OtherFocusJs.class, js -> {
                }));
        Assertions.assertTrue(none.getMessage().contains("exactly one method")
                && none.getMessage().endsWith("[]"), none.getMessage());
        IllegalArgumentException two = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> executed.getCalls(OtherFocusJs.class, js -> {
                    js.focus();
                    js.scrollIntoView();
                }));
        Assertions.assertTrue(
                two.getMessage().endsWith("[focus, scrollIntoView]"),
                two.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> executed.getCalls(AmbiguousJs.class, AmbiguousJs::focus));
    }

    // Not a valid definition: it is both invokers and snippets
    @JsDefinition
    public interface AmbiguousJs extends JsInvokers, JsSnippets {
        @JsExpression("this.focus()")
        void focus();
    }
}
