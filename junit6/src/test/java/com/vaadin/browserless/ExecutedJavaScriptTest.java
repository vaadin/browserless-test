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

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

class ExecutedJavaScriptTest extends BrowserlessTest {

    @JsDefinition
    public interface OtherFocusJs extends Serializable {
        @JsExpression("this.focus()")
        void focus();
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
                executed.getAll().stream().filter(execution -> execution
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
    void resolve_runsTheApplicationsSuccessHandler() {
        AtomicReference<Integer> result = new AtomicReference<>();
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("return this.childElementCount")
                .then(Integer.class, result::set);

        ExecutedJavaScript.Execution execution = executed.getAll().get(0);
        Assertions.assertTrue(execution.isWaitingForResult());
        execution.resolve(3);

        Assertions.assertEquals(3, result.get());
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

        List<ExecutedJavaScript.Execution> executions = executed.getAll();
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
    void expressionAndParameters_ofElementAndPageJavaScript() {
        ExecutedJavaScript executed = ExecutedJavaScript.capture();
        calendar.getElement().executeJs("this.render($0)", 5);
        UI.getCurrent().getPage().executeJs("window.render($0)", "week");

        ExecutedJavaScript.Execution onElement = executed.getAll().stream()
                .filter(execution -> execution.getElement()
                        .equals(calendar.getElement()))
                .findFirst().orElseThrow();
        Assertions.assertTrue(
                onElement.getExpression().contains("this.render($0)"),
                onElement.getExpression());
        Assertions.assertEquals(List.of(5, calendar.getElement()),
                onElement.getParameters());

        ExecutedJavaScript.Execution onPage = executed.getAll().stream()
                .filter(execution -> execution.getElement()
                        .equals(UI.getCurrent().getElement()))
                .findFirst().orElseThrow();
        Assertions.assertEquals("window.render($0)", onPage.getExpression());
        Assertions.assertEquals(List.of("week"), onPage.getParameters());
    }

    private static List<Object> functionNames(ExecutedJavaScript executed) {
        return executed.getAll().stream().map(execution -> execution.getJsCall()
                .orElseThrow().arguments().get(0)).toList();
    }
}
