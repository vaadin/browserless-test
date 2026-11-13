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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;

class ExecutedJavaScriptTest extends BrowserlessTest {

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
        // leave out what the navigation to the view sent
        ExecutedJavaScript.get();
    }

    @Test
    void functionCalls_recordedWithArguments_alongWithFocusCall() {
        button.addClickListener(event -> {
            calendar.getElement().callJsFunction("setOptions", "week", 7);
            field.getElement().callJsFunction("setOptions", "ignored");
            calendar.getElement().callJsFunction("render");
            field.focus();
            button.blur();
        });

        test(button).click();

        ExecutedJavaScript executed = ExecutedJavaScript.get();
        Assertions.assertEquals(List.of(List.of("week", 7)),
                executed.getFunctionCalls(calendar, "setOptions"));
        Assertions.assertEquals(1,
                executed.getCalls(Focusable.FocusJs.class, "focus").size());
        Assertions.assertEquals(1,
                executed.getCalls(Focusable.FocusJs.class, "blur").size());
        Assertions.assertTrue(test(field).isFocused(),
                "The focus call should still be simulated");
    }

    @Test
    void lookup_sendsPendingJavaScript() {
        AtomicReference<Boolean> focused = new AtomicReference<>(false);
        field.addFocusListener(event -> focused.set(true));
        field.focus();

        find(Button.class).single();

        Assertions.assertTrue(focused.get(),
                "The lookup should send the focus call to the client");
    }

    @Test
    void functionCall_scheduledByTest_recordedWithoutRoundTrip() {
        calendar.getElement().callJsFunction("render");

        Assertions.assertEquals(List.of(List.of()),
                ExecutedJavaScript.get().getFunctionCalls(calendar, "render"));
    }

    @Test
    void resolve_runsTheApplicationsSuccessHandler() {
        AtomicReference<Integer> result = new AtomicReference<>();
        calendar.getElement().executeJs("return this.childElementCount")
                .then(Integer.class, result::set);

        ExecutedJavaScript.Execution execution = ExecutedJavaScript.get()
                .getAll().get(0);
        Assertions.assertTrue(execution.isWaitingForResult());
        execution.resolve(3);

        Assertions.assertEquals(3, result.get());
        Assertions.assertThrows(IllegalStateException.class,
                () -> execution.resolve(4), "A browser answers only once");
    }

    @Test
    void reject_runsTheApplicationsErrorHandler() {
        AtomicReference<String> error = new AtomicReference<>();
        calendar.getElement().executeJs("return this.missing()").then(
                value -> Assertions.fail("Should not succeed"), error::set);
        calendar.getElement().executeJs("this.render()");

        List<ExecutedJavaScript.Execution> executions = ExecutedJavaScript.get()
                .getAll();
        executions.get(0).reject("missing is not a function");

        Assertions.assertEquals("missing is not a function", error.get());
        Assertions.assertThrows(IllegalStateException.class,
                () -> executions.get(1).reject("failed"),
                "Nobody waits for the result, so there is nothing to answer");
    }

    @Test
    void get_returnsWhatWasSentSinceThePreviousGet() {
        calendar.getElement().callJsFunction("render");
        ExecutedJavaScript first = ExecutedJavaScript.get();
        calendar.getElement().callJsFunction("refresh");

        Assertions.assertEquals(List.of("render"), functionNames(first));
        Assertions.assertEquals(List.of("refresh"),
                functionNames(ExecutedJavaScript.get()));
    }

    private static List<Object> functionNames(ExecutedJavaScript executed) {
        return executed.getAll().stream().map(execution -> execution.getJsCall()
                .orElseThrow().arguments().get(0)).toList();
    }
}
