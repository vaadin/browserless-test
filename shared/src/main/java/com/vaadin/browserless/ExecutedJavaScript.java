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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.shared.Registration;

/**
 * The JavaScript the server has sent to the browser of a UI during a
 * browserless test.
 * <p>
 * There is no browser, so JavaScript scheduled with {@code executeJs},
 * {@code callJsFunction} or a declared JavaScript definition does not run.
 * Browserless Test plays the client instead: at every client boundary - a round
 * trip, a component lookup, the end of a simulated user interaction - it takes
 * the pending JavaScript of the UI, the way a server response would, and
 * simulates what it can (server-side {@code Focusable.focus()} and
 * {@code blur()}, for example).
 * <p>
 * A test that wants to check what was sent starts a capture with
 * {@link #capture()}, runs the steps to check, however many round trips they
 * take, and then queries the capture:
 *
 * <pre>
 * ExecutedJavaScript executed = ExecutedJavaScript.capture();
 * test(save).click();
 *
 * assertEquals(List.of(List.of("Saved")),
 *         executed.getFunctionCalls(dialog, "open"));
 * </pre>
 * <p>
 * A capture holds everything sent to the browser of its UI after it was
 * started; JavaScript still pending when it starts is sent first and is not
 * part of it. A capture runs until {@link #stop()} or for as long as the UI
 * lives. Querying it does not consume anything, and several captures of the
 * same UI each see everything sent while they are active. A query sends
 * whatever is still pending first, so JavaScript scheduled just before is
 * included.
 * <p>
 * A call whose result the application waits for, with {@code then(...)} or
 * {@code toCompletableFuture(...)}, can be answered with
 * {@link Execution#resolve(Object)} or {@link Execution#reject(String)}, as the
 * browser would.
 * <p>
 * JavaScript scheduled for an element that is not visible is not sent, as in a
 * browser, and is captured only once the element becomes visible.
 */
public final class ExecutedJavaScript implements Serializable {

    private final UI ui;

    private final List<Execution> executions = new ArrayList<>();

    private @Nullable Registration registration;

    private ExecutedJavaScript(UI ui) {
        this.ui = ui;
    }

    /**
     * Starts capturing the JavaScript sent to the browser of
     * {@link UI#getCurrent()}.
     *
     * @return the capture, not null
     * @throws IllegalStateException
     *             if there is no current UI
     * @see #capture(UI)
     */
    public static ExecutedJavaScript capture() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException("There is no current UI");
        }
        return capture(ui);
    }

    /**
     * Starts capturing the JavaScript sent to the browser of the given UI.
     * JavaScript that is still pending is sent first and is not captured.
     *
     * @param ui
     *            the UI, not null
     * @return the capture, not null
     */
    public static ExecutedJavaScript capture(UI ui) {
        Objects.requireNonNull(ui, "ui");
        JavaScriptDispatcher dispatcher = JavaScriptDispatcher.forUI(ui);
        JavaScriptDispatcher.dispatch(ui);
        ExecutedJavaScript capture = new ExecutedJavaScript(ui);
        capture.registration = dispatcher
                .addHandler(invocation -> capture.executions
                        .add(new Execution(dispatcher, invocation)));
        return capture;
    }

    /**
     * Stops capturing, after first sending whatever is still pending. What was
     * captured so far can still be queried. Stopping a capture that is already
     * stopped does nothing.
     */
    public void stop() {
        if (registration != null) {
            JavaScriptDispatcher.dispatch(ui);
            registration.remove();
            registration = null;
        }
    }

    /**
     * Gets everything sent to the browser since the capture started, and until
     * it was stopped, in the order the browser received it.
     *
     * @return the sent JavaScript, not null
     */
    public List<Execution> getAll() {
        if (registration != null) {
            JavaScriptDispatcher.dispatch(ui);
        }
        return List.copyOf(executions);
    }

    /**
     * Gets the calls of one method of a JavaScript definition, the interface
     * that {@code Element.executeJs(Class)} is called with, that were sent to
     * the browser, in the order the browser received them.
     *
     * @param definitionType
     *            the JavaScript definition, not null
     * @param methodName
     *            the name of the called method, not null
     * @return the calls, not null
     */
    public List<JsCall> getCalls(Class<?> definitionType, String methodName) {
        Objects.requireNonNull(definitionType, "definitionType");
        Objects.requireNonNull(methodName, "methodName");
        return getAll().stream()
                .flatMap(execution -> execution.getJsCall()
                        .filter(call -> call.definitionType() == definitionType
                                && call.methodName().equals(methodName))
                        .stream())
                .toList();
    }

    /**
     * Gets the arguments of each call of the named client-side function on the
     * given component's element, made with
     * {@link Element#callJsFunction(String, Object...)}, in the order the
     * browser received them.
     *
     * @param component
     *            the component whose element the function was called on, not
     *            null
     * @param functionName
     *            the name of the function as it was passed to
     *            {@code callJsFunction}, not null
     * @return the arguments of each call, not null
     */
    public List<List<Object>> getFunctionCalls(Component component,
            String functionName) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(functionName, "functionName");
        Element element = component.getElement();
        return getAll().stream()
                .filter(execution -> element.equals(execution.getElement()))
                .flatMap(execution -> execution.getJsCall().stream())
                .filter(call -> call
                        .definitionType() == Element.CallFunctionJs.class
                        && functionName.equals(call.arguments().get(0)))
                .map(call -> {
                    List<Object> arguments = call.flattenArguments();
                    return arguments.subList(1, arguments.size());
                }).toList();
    }

    /**
     * One piece of JavaScript sent to the browser.
     */
    public static final class Execution implements Serializable {

        private final JavaScriptDispatcher dispatcher;

        private final PendingJavaScriptInvocation invocation;

        private Execution(JavaScriptDispatcher dispatcher,
                PendingJavaScriptInvocation invocation) {
            this.dispatcher = dispatcher;
            this.invocation = invocation;
        }

        /**
         * Gets the element the JavaScript was run on, which is the element of
         * the UI for JavaScript run on the page.
         *
         * @return the element, not null
         */
        public Element getElement() {
            return Element.get(invocation.getOwner());
        }

        /**
         * Gets the call of a JavaScript definition that this is, which says
         * which definition, which method of it and with which arguments, as the
         * application wrote it.
         *
         * @return the call, or empty for JavaScript run from a string with
         *         {@code executeJs(String, Object...)}
         */
        public Optional<JsCall> getJsCall() {
            return Optional.ofNullable(invocation.getInvocation().getJsCall());
        }

        /**
         * Gets the JavaScript expression of this execution, in the form Flow
         * sends it.
         * <ul>
         * <li>For JavaScript run on the page, it is the expression as the
         * application wrote it.</li>
         * <li>For JavaScript run on an element with
         * {@link Element#executeJs(String, Object...)}, the expression the
         * application wrote is wrapped so that it runs with the element as
         * {@code this}; check it with {@code contains(...)}.</li>
         * <li>For a call of a JavaScript definition, it is the declared
         * expression of the called method, which the browser itself does not
         * need; {@link #getJsCall()} tells what was called.</li>
         * </ul>
         *
         * @return the expression, not null
         */
        public String getExpression() {
            return invocation.getInvocation().getExpression();
        }

        /**
         * Gets the parameters that the expression of this execution runs with:
         * the ones the application passed and, for JavaScript run on an
         * element, the element as the last one, which the expression uses as
         * {@code this}.
         *
         * @return the parameters, not null
         * @see #getExpression()
         */
        public List<Object> getParameters() {
            return invocation.getInvocation().getParameters();
        }

        /**
         * Checks whether the application waits for the result of this
         * JavaScript, with {@link PendingJavaScriptResult#then} or
         * {@link PendingJavaScriptResult#toCompletableFuture}.
         *
         * @return {@literal true} if the application waits for the result
         */
        public boolean isWaitingForResult() {
            return invocation.isSubscribed();
        }

        /**
         * Answers the JavaScript with the given return value, as the browser
         * does when the JavaScript has run, which runs the success handler the
         * application registered.
         *
         * @param value
         *            the value the JavaScript returned, converted to JSON the
         *            way Flow converts a value; a {@link JsonNode} is used as
         *            is
         * @throws IllegalStateException
         *             if the application does not wait for the result, or if
         *             the JavaScript was already answered
         */
        public void resolve(@Nullable Object value) {
            ensureCanAnswer();
            invocation.complete(value instanceof JsonNode node ? node
                    : JacksonUtils.getMapper().valueToTree(value));
        }

        /**
         * Answers the JavaScript with an error, as the browser does when the
         * JavaScript throws, which runs the error handler the application
         * registered.
         *
         * @param message
         *            the error message, not null
         * @throws IllegalStateException
         *             if the application does not wait for the result, or if
         *             the JavaScript was already answered
         */
        public void reject(String message) {
            Objects.requireNonNull(message, "message");
            ensureCanAnswer();
            invocation.completeExceptionally(
                    JacksonUtils.getMapper().valueToTree(message));
        }

        private void ensureCanAnswer() {
            if (!isWaitingForResult()) {
                throw new IllegalStateException(
                        "The application does not wait for the result of "
                                + this);
            }
            // A browser answers once; Flow forgets the return channel after
            // the first answer
            if (!dispatcher.markAnswered(invocation)) {
                throw new IllegalStateException(
                        "The browser has already answered " + this);
            }
        }

        @Override
        public String toString() {
            return getJsCall().map(JsCall::toString)
                    .orElseGet(this::getExpression);
        }
    }
}
