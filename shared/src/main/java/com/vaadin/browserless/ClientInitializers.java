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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.TriggerSimulator;
import com.vaadin.flow.component.trigger.internal.TriggerSimulator.EventListener;
import com.vaadin.flow.component.trigger.internal.TriggerSimulator.NotSimulatedException;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.dom.impl.ElementJsInitializerRegistration.InitializerJs;
import com.vaadin.flow.internal.StateNode;
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
 * for it with Flow's {@link TriggerSimulator}, which provides what the browser
 * does with each snippet.
 * <p>
 * A trigger that listens to an event no tester fires, or that uses JavaScript
 * the simulator does not simulate, is logged and otherwise ignored, so the rest
 * of the test runs as before.
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

    // The listener is null for an initializer that is not simulated
    private record Installed(
            @Nullable EventListener listener) implements Serializable {
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
        if (initializer.getDeclaredCall() == null) {
            // JavaScript the application writes itself, not a trigger
            LOGGER.debug("A JavaScript initializer of {} is not simulated in"
                    + " browserless tests", Element.get(node));
            return new Installed(null);
        }
        try {
            EventListener listener = TriggerSimulator
                    .readEventListener(initializer);
            if (SIMULATED_EVENTS.contains(listener.eventName())) {
                return new Installed(listener);
            }
            LOGGER.warn("A trigger listening to '{}' events on {} is not"
                    + " simulated in browserless tests, as no tester fires"
                    + " that event", listener.eventName(), Element.get(node));
        } catch (NotSimulatedException e) {
            LOGGER.warn("A trigger on {} is not simulated in browserless"
                    + " tests: {}", Element.get(node), e.getMessage());
        }
        return new Installed(null);
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
        for (Installed installedListener : List.copyOf(installed.values())) {
            EventListener listener = installedListener.listener();
            if (listener == null || !eventName.equals(listener.eventName())) {
                continue;
            }
            try {
                settlements.add(listener.handleEvent(clipboard::write));
            } catch (NotSimulatedException e) {
                LOGGER.warn(
                        "A trigger on {} is not simulated in browserless"
                                + " tests: {}",
                        component.getElement(), e.getMessage());
            }
        }
        return () -> settlements.forEach(Runnable::run);
    }
}
