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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.TriggerInputJs;
import com.vaadin.flow.component.trigger.internal.TriggerJs;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction.WriteToClipboardJs;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.dom.impl.ElementJsInitializerRegistration.InitializerJs;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.shared.Registration;

/**
 * Runs, on the server, the client-side part of the triggers an application
 * installs on elements, such as copying to the clipboard when a button is
 * clicked.
 * <p>
 * A trigger reaches the browser as an element initializer, a
 * {@link InitializerJs} call carrying a function declared by Flow's trigger
 * snippets. This class keeps the initializers that the
 * {@link JavaScriptDispatcher} sends to the client, and when a tester fires a
 * DOM event that a browser would fire, it runs the event listeners installed
 * for it.
 * <p>
 * Only the snippets listed in {@link #evaluate} are simulated. A trigger that
 * listens to an event no tester fires, or an action that uses a snippet that is
 * not simulated, is logged and otherwise ignored, so the rest of the test runs
 * as before.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ClientInitializers implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ClientInitializers.class);

    /**
     * The DOM events testers fire, and so the only ones whose listeners run.
     */
    private static final Set<String> SIMULATED_EVENTS = Set.of("click");

    // Insertion ordered, so listeners run in the order they were installed,
    // as in a browser
    private final Map<StateNode, Map<Integer, Installed>> byNode = new HashMap<>();

    private ClientInitializers() {
    }

    private record Installed(@Nullable String eventName,
            @Nullable JsFunction listener) implements Serializable {
    }

    private static ClientInitializers getOrCreate(UI ui) {
        ClientInitializers initializers = ComponentUtil.getData(ui,
                ClientInitializers.class);
        if (initializers == null) {
            initializers = new ClientInitializers();
            ComponentUtil.setData(ui, ClientInitializers.class, initializers);
        }
        return initializers;
    }

    /**
     * Creates the handler that keeps track of the element initializers sent to
     * the client of the given UI.
     *
     * @param ui
     *            the UI, not null
     * @return the handler, not null
     */
    static JavaScriptDispatcher.Handler handlerFor(UI ui) {
        return invocation -> {
            JsCall call = invocation.getInvocation().getJsCall();
            if (call == null || call.definitionType() != InitializerJs.class) {
                return;
            }
            ClientInitializers initializers = getOrCreate(ui);
            StateNode node = invocation.getOwner();
            int id = (Integer) call.arguments().get(0);
            if ("init".equals(call.methodName())) {
                initializers.init(node, id,
                        (JsFunction) call.arguments().get(1));
            } else if ("dispose".equals(call.methodName())) {
                Map<Integer, Installed> installed = initializers.byNode
                        .get(node);
                if (installed != null) {
                    installed.remove(id);
                }
            }
        };
    }

    private void init(StateNode node, int id, JsFunction initializer) {
        Installed installed = install(node, initializer);
        Map<Integer, Installed> installedOnNode = byNode.get(node);
        if (installedOnNode == null) {
            installedOnNode = new LinkedHashMap<>();
            byNode.put(node, installedOnNode);
            // The client drops the initializers of an element that is
            // detached; the server installs them again when it is attached
            Registration[] detach = new Registration[1];
            detach[0] = node.addDetachListener(() -> {
                byNode.remove(node);
                detach[0].remove();
            });
        }
        installedOnNode.put(id, installed);
    }

    private static Installed install(StateNode node, JsFunction initializer) {
        JsCall call = initializer.getDeclaredCall();
        if (call == null) {
            LOGGER.debug("A JavaScript initializer of {} is not simulated in"
                    + " browserless tests", Element.get(node));
            return new Installed(null, null);
        }
        if (call.definitionType() == TriggerJs.class
                && "listenToEvent".equals(call.methodName())) {
            String eventName = (String) call.arguments().get(1);
            if (SIMULATED_EVENTS.contains(eventName)) {
                return new Installed(eventName,
                        (JsFunction) call.arguments().get(0));
            }
            LOGGER.warn("A trigger listening to '{}' events on {} is not"
                    + " simulated in browserless tests, as no tester fires"
                    + " that event", eventName, Element.get(node));
        } else {
            LOGGER.warn("A {} trigger on {} is not simulated in browserless"
                    + " tests", describe(call), Element.get(node));
        }
        return new Installed(null, null);
    }

    /**
     * Runs the client-side listeners for a DOM event that the user fires on the
     * given component, as a browser does before it sends the event to the
     * server.
     * <p>
     * Promises the listeners start are settled by the returned task, as a
     * browser settles them only after the event has been handled. Run it once
     * the server-side event has been fired.
     *
     * @param ui
     *            the UI the user interacts with, not null
     * @param component
     *            the component the event is fired on, not null
     * @param eventName
     *            the name of the DOM event, {@code click}
     * @return the task that settles the promises the listeners started, not
     *         null
     */
    public static Runnable fireDomEvent(UI ui, Component component,
            String eventName) {
        if (!SIMULATED_EVENTS.contains(eventName)) {
            throw new IllegalArgumentException("Listeners of '" + eventName
                    + "' events are not simulated");
        }
        // Initializers still pending are installed before the user acts. The
        // component may only get attached then, as a menu item does when its
        // menu is rendered.
        JavaScriptDispatcher.dispatch(ui);
        Map<Integer, Installed> installed = getOrCreate(ui).byNode
                .get(component.getElement().getNode());
        if (installed == null) {
            return () -> {
            };
        }
        SimulatedClipboard clipboard = SimulatedClipboard.of(ui.getSession());
        List<Runnable> settlements = new ArrayList<>();
        for (Installed listener : List.copyOf(installed.values())) {
            if (eventName.equals(listener.eventName())
                    && listener.listener() != null) {
                runListener(component.getElement(), listener.listener(),
                        clipboard, settlements);
            }
        }
        return () -> settlements.forEach(Runnable::run);
    }

    private static void runListener(Element element, JsFunction listener,
            SimulatedClipboard clipboard, List<Runnable> settlements) {
        try {
            evaluate(listener, clipboard, settlements);
        } catch (NotSimulated e) {
            LOGGER.warn("A trigger on {} is not simulated in browserless tests,"
                    + " as it uses {}", element, e.getMessage());
        }
    }

    /**
     * Does on the server what the browser does when it calls the given
     * function.
     *
     * @param function
     *            the function to call
     * @param clipboard
     *            the clipboard of the user who fired the event
     * @param settlements
     *            where to add the settlement of a promise the function starts
     * @return the value the function returns
     * @throws NotSimulated
     *             if the function, or one it calls, is not simulated
     */
    private static @Nullable Object evaluate(JsFunction function,
            SimulatedClipboard clipboard, List<Runnable> settlements) {
        JsCall call = function.getDeclaredCall();
        if (call == null) {
            throw new NotSimulated("JavaScript compiled from a string");
        }
        List<Object> args = call.arguments();
        Class<?> type = call.definitionType();
        String method = call.methodName();
        if (type == TriggerInputJs.class && "returnValue".equals(method)) {
            return args.get(0);
        }
        if (type == TriggerInputJs.class && "returnNull".equals(method)) {
            return null;
        }
        if (type == WriteToClipboardJs.class
                && "writeToClipboard".equals(method)) {
            String text = (String) evaluate((JsFunction) args.get(0), clipboard,
                    settlements);
            String html = (String) evaluate((JsFunction) args.get(1), clipboard,
                    settlements);
            if (evaluate((JsFunction) args.get(2), clipboard,
                    settlements) != null) {
                throw new NotSimulated("an image copy");
            }
            clipboard.write(text, html);
            return text != null ? text : html;
        }
        if (type == TriggerJs.class && "reportOutcome".equals(method)) {
            Object value = evaluate((JsFunction) args.get(0), clipboard,
                    settlements);
            ReturnChannelRegistration channel = (ReturnChannelRegistration) args
                    .get(1);
            settlements.add(() -> {
                ObjectNode outcome = JacksonUtils.createObjectNode();
                outcome.put("ok", true);
                outcome.set("value", value == null ? outcome.nullNode()
                        : JacksonUtils.getMapper().valueToTree(value));
                ArrayNode arguments = JacksonUtils.createArrayNode();
                arguments.add(outcome);
                channel.invoke(arguments);
            });
            return null;
        }
        throw new NotSimulated(describe(call));
    }

    private static String describe(JsCall call) {
        return call.definitionType().getSimpleName() + "." + call.methodName();
    }

    /**
     * Thrown when a function is not simulated, with the message naming what is
     * not simulated.
     */
    private static final class NotSimulated extends RuntimeException {
        private NotSimulated(String what) {
            super(what, null, false, false);
        }
    }
}
