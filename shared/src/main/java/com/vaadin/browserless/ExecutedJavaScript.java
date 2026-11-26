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
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsInvokers;
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
 * Each UI created by Browserless Test also has a capture that was started with
 * the UI, before its first view was navigated to, so it holds everything the UI
 * was sent. The test classes and {@link BrowserlessUIContext} return it from
 * {@code getExecutedJavaScript()}. Everyone who gets it shares the same
 * instance, so it is read-only: no one can stop it, clear it or take anything
 * out of it, and the lists its queries return, like those of any capture,
 * cannot be modified. Answering a call with {@link Execution#resolve(Object)}
 * or {@link Execution#reject(String)} is visible to everyone who looks at that
 * call, as it is the browser's answer to the application.
 * <p>
 * A capture holds everything sent to the browser of its UI after it was started
 * or last cleared; JavaScript still pending when it starts is sent first and is
 * not part of it. A capture runs until {@link #stop()} or for as long as the UI
 * lives. Querying it does not consume anything, and several captures of the
 * same UI each see everything sent while they are active. A query sends
 * whatever is still pending first, so JavaScript scheduled just before is
 * included.
 * <p>
 * A test that plays the browser step by step can call {@link #clear()} after
 * reading a step, so that the next query only sees what is sent afterwards:
 *
 * <pre>
 * List&lt;List&lt;Object&gt;&gt; pages = executed.getFunctionCalls(viewer, "scrollToPage");
 * executed.clear();
 * </pre>
 * <p>
 * A call whose result the application waits for, with {@code then(...)} or
 * {@code toCompletableFuture(...)}, can be answered with
 * {@link Execution#resolve(Object)} or {@link Execution#reject(String)}, as the
 * browser would. A {@link ReturnChannelRegistration} passed to the JavaScript
 * can be called with {@link Execution#invokeReturnChannel(Object...)}, as the
 * JavaScript would call it in the browser.
 * <p>
 * JavaScript scheduled for an element that is not visible is not sent, as in a
 * browser, and is captured only once the element becomes visible.
 */
public final class ExecutedJavaScript implements Serializable {

    /**
     * The UI whose JavaScript is captured.
     */
    private final UI ui;

    /**
     * The JavaScript captured so far, in the order it was sent.
     */
    private final List<Execution> executions = new ArrayList<>();

    /**
     * Whether this is the capture a UI got when it was created, which every
     * caller of {@code getExecutedJavaScript()} shares and relies on to hold
     * everything, so no caller may stop it or take anything out of it.
     */
    private final boolean readOnly;

    /**
     * Removes the capture from the dispatcher; null once it was removed.
     */
    private @Nullable Registration registration;

    private ExecutedJavaScript(UI ui, boolean readOnly) {
        this.ui = ui;
        this.readOnly = readOnly;
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
        return startCapture(dispatcher, false);
    }

    /**
     * Starts capturing what the given dispatcher sends from now on, without
     * sending what is pending first. The capture a UI owns is shared by all
     * callers, which rely on it to hold everything, so it is read-only.
     */
    static ExecutedJavaScript startCapture(JavaScriptDispatcher dispatcher,
            boolean readOnly) {
        ExecutedJavaScript capture = new ExecutedJavaScript(dispatcher.getUI(),
                readOnly);
        capture.registration = dispatcher
                .addHandler(invocation -> capture.executions
                        .add(new Execution(dispatcher, invocation)));
        return capture;
    }

    /**
     * Stops capturing, after first sending whatever is still pending. What was
     * captured so far can still be queried. Stopping a capture that is already
     * stopped does nothing.
     *
     * @throws IllegalStateException
     *             if this is the read-only capture that a UI got when it was
     *             created, which runs for as long as the UI lives
     */
    public void stop() {
        ensureNotReadOnly("stopped");
        if (registration != null) {
            try {
                JavaScriptDispatcher.dispatch(ui);
            } finally {
                registration.remove();
                registration = null;
            }
        }
    }

    /**
     * Forgets everything captured so far, so that the queries only see what is
     * sent to the browser afterwards. JavaScript that is still pending is not
     * sent first, so JavaScript scheduled just before, for example by a handler
     * that {@link Execution#resolve(Object)} ran, is seen by the next query.
     * Executions that were already returned can still be answered. Clearing
     * does not stop the capture.
     *
     * @throws IllegalStateException
     *             if this is the capture that a UI got when it was created,
     *             which is shared and cannot be cleared
     */
    public void clear() {
        ensureNotReadOnly("cleared");
        executions.clear();
    }

    /**
     * Refuses an operation that would leave the shared capture of a UI without
     * part of what the UI was sent.
     */
    private void ensureNotReadOnly(String refused) {
        if (readOnly) {
            throw new IllegalStateException("The capture that the UI got "
                    + "when it was created is read-only and cannot be "
                    + refused + ", as it holds everything sent to the UI. "
                    + "Use ExecutedJavaScript.capture() for a capture that "
                    + "can be " + refused + ".");
        }
    }

    /**
     * Gets everything sent to the browser since the capture started or was last
     * cleared, and until it was stopped, in the order the browser received it,
     * whichever element it was run on; {@link #getExecutions(Component)} and
     * {@link #getExecutions(Element)} keep only what was run on one element,
     * and {@link #getExecutions(Class, Consumer)} only the calls of one method
     * of a JavaScript definition.
     *
     * @return the sent JavaScript, not null
     */
    public List<Execution> getExecutions() {
        if (registration != null) {
            JavaScriptDispatcher.dispatch(ui);
        }
        return List.copyOf(executions);
    }

    /**
     * Gets what was sent to the browser for the given component's element since
     * the capture started or was last cleared, and until it was stopped, in the
     * order the browser received it. Unlike {@link #getExecutions()},
     * JavaScript run on other elements is left out. JavaScript run on the page
     * belongs to the UI, so passing the UI gets it.
     *
     * @param component
     *            the component whose element the JavaScript was run on, not
     *            null
     * @return the JavaScript sent for the component, not null
     * @see Execution#getElement()
     */
    public List<Execution> getExecutions(Component component) {
        Objects.requireNonNull(component, "component");
        return getExecutions(component.getElement());
    }

    /**
     * Gets what was sent to the browser for the given element since the capture
     * started or was last cleared, and until it was stopped, in the order the
     * browser received it. Unlike {@link #getExecutions()}, JavaScript run on
     * other elements is left out. JavaScript run on the page belongs to the
     * element of the UI.
     *
     * @param element
     *            the element the JavaScript was run on, not null
     * @return the JavaScript sent for the element, not null
     * @see Execution#getElement()
     */
    public List<Execution> getExecutions(Element element) {
        Objects.requireNonNull(element, "element");
        return getExecutions().stream()
                .filter(execution -> element.equals(execution.getElement()))
                .toList();
    }

    private List<Execution> getExecutionsOfMethod(Class<?> definitionType,
            String methodName) {
        Objects.requireNonNull(definitionType, "definitionType");
        Objects.requireNonNull(methodName, "methodName");
        return getExecutions().stream()
                .filter(execution -> execution.getJsCall()
                        .filter(call -> call.definitionType() == definitionType
                                && call.methodName().equals(methodName))
                        .isPresent())
                .toList();
    }

    /**
     * Gets what was sent to the browser for the calls of one method of a
     * JavaScript definition, the interface that
     * {@code Element.executeJs(Class)} is called with, in the order the browser
     * received it. Each execution has the call itself
     * ({@link Execution#getJsCall()}) and can be answered the way the browser
     * would, with {@link Execution#resolve(Object)} or
     * {@link Execution#reject(String)}, for a call whose result the application
     * waits for. Unlike {@link #getExecutions()}, everything else is left out.
     * The method is named by calling it, the way the application calls it, so
     * that renaming it updates the test and a method that the definition does
     * not have fails to compile:
     *
     * <pre>
     * executed.getExecutions(WakeLockJs.class, WakeLockJs::request).get(0)
     *         .resolve(null);
     * </pre>
     * <p>
     * The call only names the method: it is not sent, and its arguments are
     * ignored, so any value of the right type will do. Calls are matched by the
     * name of the method, so overloads of one name are not told apart.
     *
     * @param <T>
     *            the JavaScript definition type
     * @param definitionType
     *            the JavaScript definition, not null
     * @param method
     *            calls the method of the definition to get the executions of,
     *            once, on the implementation it is given, not null
     * @return the executions of the calls, not null
     * @throws IllegalArgumentException
     *             if the type is not a JavaScript definition, or if
     *             {@code method} does not call exactly one method of it
     * @see #getCalls(Class, Consumer)
     */
    public <T extends JsInvokers> List<Execution> getExecutions(
            Class<T> definitionType, Consumer<T> method) {
        return getExecutionsOfMethod(definitionType,
                getCalledMethodName(definitionType, method));
    }

    /**
     * Gets the calls of one method of a JavaScript definition, the interface
     * that {@code Element.executeJs(Class)} is called with, that were sent to
     * the browser, in the order the browser received them. To answer a call,
     * use {@link #getExecutions(Class, Consumer)} instead.
     * <p>
     * The method is named as a string, so a misspelled or renamed name matches
     * nothing rather than failing; {@link #getCalls(Class, Consumer)} names it
     * by calling it, which the compiler checks.
     *
     * @param definitionType
     *            the JavaScript definition, not null
     * @param methodName
     *            the name of the called method, not null
     * @return the calls, not null
     * @see #getCalls(Class, Consumer)
     */
    public List<JsCall> getCalls(Class<?> definitionType, String methodName) {
        return getExecutionsOfMethod(definitionType, methodName).stream()
                .map(execution -> execution.getJsCall().orElseThrow()).toList();
    }

    /**
     * Gets the calls of one method of a JavaScript definition, the interface
     * that {@code Element.executeJs(Class)} is called with, that were sent to
     * the browser, in the order the browser received them. The method is named
     * by calling it, the way the application calls it, so that renaming it
     * updates the test and a method that the definition does not have fails to
     * compile:
     *
     * <pre>
     * executed.getCalls(WakeLockJs.class, WakeLockJs::request);
     * executed.getCalls(ScrollJs.class, js -&gt; js.scrollTo(0, 0));
     * </pre>
     * <p>
     * The call only names the method: it is not sent, and its arguments are
     * ignored, so any value of the right type will do. Calls are matched by the
     * name of the method, so overloads of one name are not told apart.
     *
     * @param <T>
     *            the JavaScript definition type
     * @param definitionType
     *            the JavaScript definition, not null
     * @param method
     *            calls the method of the definition to get the calls of, once,
     *            on the implementation it is given, not null
     * @return the calls, not null
     * @throws IllegalArgumentException
     *             if the type is not a JavaScript definition, or if
     *             {@code method} does not call exactly one method of it
     * @see #getCalls(Class, String)
     * @see #getExecutions(Class, Consumer)
     */
    public <T extends JsInvokers> List<JsCall> getCalls(Class<T> definitionType,
            Consumer<T> method) {
        return getCalls(definitionType,
                getCalledMethodName(definitionType, method));
    }

    private static <T extends JsInvokers> String getCalledMethodName(
            Class<T> definitionType, Consumer<T> method) {
        Objects.requireNonNull(definitionType, "definitionType");
        Objects.requireNonNull(method, "method");
        List<String> called = new ArrayList<>();
        method.accept(JsDefinitionProxy.create(definitionType, call -> {
            called.add(call.methodName());
            return null;
        }));
        if (called.size() != 1) {
            throw new IllegalArgumentException(
                    "The method should call exactly one method of "
                            + definitionType.getName() + " but called "
                            + called);
        }
        return called.get(0);
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
        return getExecutions(component).stream()
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

        /**
         * The dispatcher that sent the JavaScript to the client.
         */
        private final JavaScriptDispatcher dispatcher;

        /**
         * The invocation the client received.
         */
        private final PendingJavaScriptInvocation invocation;

        Execution(JavaScriptDispatcher dispatcher,
                PendingJavaScriptInvocation invocation) {
            this.dispatcher = dispatcher;
            this.invocation = invocation;
        }

        /**
         * Gets the UI whose browser the JavaScript was sent to. A handler can
         * use it to tell windows apart or to look at the state of the
         * application in that window.
         *
         * @return the UI, not null
         */
        public UI getUI() {
            return dispatcher.getUI();
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
         * <li>For a string run on the page with
         * {@code Page.executeJs(String, Object...)}, it is the expression as
         * the application wrote it.</li>
         * <li>For a string run on an element with
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
         * Gets the parameters that the expression of this execution runs with,
         * in the form Flow sends them.
         * <ul>
         * <li>For a string run on the page, they are the parameters the
         * application passed.</li>
         * <li>For a string run on an element, they are the parameters the
         * application passed, followed by the element, which the expression
         * uses as {@code this}.</li>
         * <li>For a call of a JavaScript definition, they are the arguments of
         * the call, flattened, followed by the element, or {@code null} for a
         * call on the page; {@link #getJsCall()} gives the arguments as the
         * application passed them.</li>
         * </ul>
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
         * {@link PendingJavaScriptResult#toCompletableFuture}, and it has not
         * been answered yet, so that {@link #resolve(Object)} and
         * {@link #reject(String)} can be called.
         *
         * @return {@literal true} if the application waits for a result that
         *         has not been given yet
         */
        public boolean isWaitingForResult() {
            return invocation.isSubscribed()
                    && !dispatcher.isAnswered(invocation);
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
            invocation.complete(toJson(value));
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

        /**
         * Calls the return channel passed to this JavaScript with the given
         * arguments, as the JavaScript would call it in the browser. A round
         * trip runs before the call, as the browser acts on the latest
         * response, and after it, so that JavaScript the channel's handler
         * schedules is sent.
         * <p>
         * A {@link JavaScriptHandler} can call this method too, for a channel
         * the browser calls as soon as the JavaScript runs. JavaScript the
         * channel's handler schedules is then sent at the next client boundary.
         * <p>
         * The UI the JavaScript was sent to must be the current UI, as the
         * channel's handler runs in it: with several windows, activate its
         * window first.
         * <p>
         * The JavaScript must have received exactly one
         * {@link ReturnChannelRegistration} among its
         * {@linkplain #getParameters() parameters}; use
         * {@link #invokeReturnChannelAt(int, Object...)} to pick one of
         * several.
         * <p>
         * The same checks apply as when Flow receives a return channel message
         * from the browser, but where Flow ignores the message, this method
         * throws: the channel must still be registered, and its element must be
         * attached and visible, and enabled unless the channel's
         * {@link DisabledUpdateMode} is {@link DisabledUpdateMode#ALWAYS
         * ALWAYS}.
         *
         * @param arguments
         *            the arguments the JavaScript passes to the channel, each
         *            converted to JSON the way Flow converts a value; a
         *            {@link JsonNode} is used as is
         * @throws IllegalStateException
         *             if the JavaScript received no return channel or more than
         *             one, if the UI the JavaScript was sent to is not the
         *             current UI, or if Flow would ignore the message
         * @see #invokeReturnChannelAt(int, Object...)
         */
        public void invokeReturnChannel(@Nullable Object... arguments) {
            List<ReturnChannelRegistration> channels = getParameters().stream()
                    .filter(ReturnChannelRegistration.class::isInstance)
                    .map(ReturnChannelRegistration.class::cast).toList();
            if (channels.size() != 1) {
                throw new IllegalStateException("Expected exactly one return "
                        + "channel among the parameters of " + this + ", but "
                        + "found " + channels.size()
                        + ". Use invokeReturnChannelAt(int, Object...) to pick "
                        + "one by its parameter index.");
            }
            invoke(channels.get(0), arguments);
        }

        /**
         * Calls the return channel passed to this JavaScript as the parameter
         * with the given index, as the JavaScript would call it in the browser,
         * with a round trip before and after the call like
         * {@link #invokeReturnChannel(Object...)}.
         * <p>
         * Unlike {@link #invokeReturnChannel(Object...)}, this method works
         * when the JavaScript received several return channels. The same checks
         * apply as when Flow receives a return channel message from the
         * browser, but where Flow ignores the message, this method throws.
         *
         * @param parameterIndex
         *            the index of the channel in {@link #getParameters()}
         * @param arguments
         *            the arguments the JavaScript passes to the channel, each
         *            converted to JSON the way Flow converts a value; a
         *            {@link JsonNode} is used as is
         * @throws IllegalArgumentException
         *             if there is no parameter with the given index, or it is
         *             not a {@link ReturnChannelRegistration}
         * @throws IllegalStateException
         *             if the UI the JavaScript was sent to is not the current
         *             UI, or if Flow would ignore the message
         * @see #invokeReturnChannel(Object...)
         */
        public void invokeReturnChannelAt(int parameterIndex,
                @Nullable Object... arguments) {
            List<Object> parameters = getParameters();
            if (parameterIndex < 0 || parameterIndex >= parameters.size()
                    || !(parameters.get(
                            parameterIndex) instanceof ReturnChannelRegistration channel)) {
                throw new IllegalArgumentException("Parameter " + parameterIndex
                        + " of " + this + " is not a return channel");
            }
            invoke(channel, arguments);
        }

        private void invoke(ReturnChannelRegistration channel,
                @Nullable Object[] arguments) {
            // The round trips and the channel's handler work on the Vaadin
            // thread-locals, so another window's would take this one's place
            if (getUI() != UI.getCurrent()) {
                throw new IllegalStateException("The UI that " + this
                        + " was sent to is not the current UI. Activate its "
                        + "window first, for example with "
                        + "BrowserlessUIContext.activate().");
            }
            // The browser sends the message in reply to the latest response,
            // and Flow updates whether an element is visible only when it
            // sends a response
            BrowserlessDSL.roundTrip(getUI());
            ensureCanReceive(channel);
            ArrayNode json = JacksonUtils.createArrayNode();
            for (Object argument : arguments == null ? new Object[] { null }
                    : arguments) {
                json.add(toJson(argument));
            }
            channel.invoke(json);
            BrowserlessDSL.roundTrip(getUI());
        }

        // The checks of Flow's AbstractRpcInvocationHandler and
        // ReturnChannelHandler, which ignore the message where this throws.
        // A registered channel lets the message through to an inert element.
        private void ensureCanReceive(ReturnChannelRegistration channel) {
            StateNode node = getUI().getInternals().getStateTree()
                    .getNodeById(channel.getStateNodeId());
            if (node == null || !node.isAttached()) {
                throw new IllegalStateException("The element of the return "
                        + "channel passed to " + this + " is not attached");
            }
            if (node.isInactive()) {
                throw new IllegalStateException("The element of the return "
                        + "channel passed to " + this + " is not visible");
            }
            ReturnChannelMap channels = node
                    .getFeatureIfInitialized(ReturnChannelMap.class)
                    .orElse(null);
            if (channels == null
                    || channels.get(channel.getChannelId()) != channel) {
                throw new IllegalStateException("The return channel passed "
                        + "to " + this + " has been removed");
            }
            if (!node.isEnabled() && channel
                    .getDisabledUpdateMode() != DisabledUpdateMode.ALWAYS) {
                throw new IllegalStateException("The element of the return "
                        + "channel passed to " + this + " is disabled");
            }
        }

        private static JsonNode toJson(@Nullable Object value) {
            return value instanceof JsonNode node ? node
                    : JacksonUtils.getMapper().valueToTree(value);
        }

        private void ensureCanAnswer() {
            if (!invocation.isSubscribed()) {
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
