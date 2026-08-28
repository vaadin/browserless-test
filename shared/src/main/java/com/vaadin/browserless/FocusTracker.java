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

import java.util.Optional;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * Keeps track of the component that has keyboard focus during a browserless
 * test, mirroring how focus behaves with a real user in a browser.
 * <p>
 * Testers report simulated user interactions here: interacting with a component
 * moves focus to it, which fires a {@code blur} DOM event on the previously
 * focused component and a {@code focus} DOM event on the newly focused one,
 * both as if they came from the client. Test authors normally never need to
 * call this class directly; focus and blur listeners fire implicitly, just like
 * in production. For explicit control there are {@link ComponentTester#focus()}
 * and {@link ComponentTester#blur()}.
 * <p>
 * Server-initiated focus changes are simulated as well: when application code
 * calls {@link Focusable#focus()} or {@link Focusable#blur()}, which only
 * schedule a client-side JavaScript call, the tracker picks the call up from
 * the pending JavaScript queue and reacts like a browser would, firing the
 * corresponding focus/blur DOM events back. This happens at the end of each
 * simulated user interaction and on server round-trips.
 *
 * @since 1.3
 */
public final class FocusTracker {

    private Component focused;

    private boolean flushing;

    private FocusTracker() {
    }

    /**
     * Gets the component currently considered focused in the given UI, if any.
     *
     * @param ui
     *            the UI to check, not null
     * @return the focused component, or empty if nothing is focused
     */
    public static Optional<Component> getFocusedComponent(UI ui) {
        FocusTracker tracker = getOrCreate(ui);
        tracker.flushServerInitiatedFocus(ui);
        return Optional.ofNullable(tracker.focused);
    }

    /**
     * Moves focus to the given component as part of a simulated user
     * interaction.
     * <p>
     * Fires a {@code blur} DOM event on the previously focused component and,
     * if the given component is {@link Focusable}, a {@code focus} DOM event on
     * it. Interacting again with the already focused component is a no-op, as
     * in a browser.
     *
     * @param component
     *            the component the user interacts with, not null
     */
    static void moveFocusTo(Component component) {
        UI ui = uiOf(component);
        if (ui == null) {
            return;
        }
        FocusTracker tracker = getOrCreate(ui);
        tracker.flushServerInitiatedFocus(ui);
        tracker.doMoveFocusTo(component, false);
    }

    private void doMoveFocusTo(Component component, boolean serverInitiated) {
        if (focused == component) {
            return;
        }
        Component previous = focused;
        // Browsers only give keyboard focus to focusable elements; for
        // anything else focus falls back to the document body
        focused = component instanceof Focusable ? component : null;
        if (previous != null && previous.isAttached()) {
            // The blur on the previously focused element is a plain browser
            // reaction even when the focus change was server-initiated
            fireDomEvent(previous, "blur", JacksonUtils.createObjectNode());
        }
        if (focused != null) {
            ObjectNode eventData = JacksonUtils.createObjectNode();
            if (serverInitiated) {
                // Focusable.focus() sets this marker on the element so that
                // the FocusEvent reports isFromClient() == false; mirror it
                eventData.put("event.target._nextFocusIsFromClient", false);
            }
            fireDomEvent(component, "focus", eventData);
        }
    }

    /**
     * Simulates the given component losing keyboard focus, firing a
     * {@code blur} DOM event as if it came from the client.
     *
     * @param component
     *            the component to blur, not null
     */
    static void blur(Component component) {
        UI ui = uiOf(component);
        if (ui != null) {
            FocusTracker tracker = getOrCreate(ui);
            tracker.flushServerInitiatedFocus(ui);
            if (tracker.focused == component) {
                tracker.focused = null;
            }
        }
        if (component.isAttached()) {
            fireDomEvent(component, "blur");
        }
    }

    /**
     * Simulates the client executing any focus/blur JavaScript calls scheduled
     * by the server, such as {@link Focusable#focus()} inside a click listener.
     * Called by testers at the end of each simulated interaction and on server
     * round-trips.
     *
     * @param ui
     *            the UI whose pending JavaScript queue to process, may be null
     */
    static void flush(UI ui) {
        if (ui != null) {
            getOrCreate(ui).flushServerInitiatedFocus(ui);
        }
    }

    /**
     * Same as {@link #flush(UI)}, resolving the UI from the given component.
     *
     * @param component
     *            the component that was interacted with, not null
     */
    static void flush(Component component) {
        flush(uiOf(component));
    }

    private void flushServerInitiatedFocus(UI ui) {
        if (flushing) {
            return;
        }
        flushing = true;
        try {
            // executeJs calls are queued via beforeClientResponse, so
            // materialize them first, then consume the queue like a browser
            // receiving the invocations would
            ui.getInternals().getStateTree()
                    .runExecutionsBeforeClientResponse();
            for (PendingJavaScriptInvocation invocation : ui.getInternals()
                    .dumpPendingJavaScriptInvocations()) {
                if (invocation.isCanceled()) {
                    continue;
                }
                // These patterns must match the JavaScript generated by
                // com.vaadin.flow.component.Focusable#focus(FocusOption...)
                // and #blur(); if those implementations change, update this
                // and BlurSimulationTest's serverSideFocus tests catch it
                String expression = invocation.getInvocation().getExpression();
                boolean focusCall = expression.contains("this.focus(");
                boolean blurCall = expression.contains("this.blur()");
                if (!focusCall && !blurCall) {
                    continue;
                }
                Component target = Element.get(invocation.getOwner())
                        .getComponent().orElse(null);
                if (target == null) {
                    continue;
                }
                if (focusCall) {
                    doMoveFocusTo(target, true);
                } else if (focused == target) {
                    // blurring an element that does not have focus is a no-op
                    // in a browser
                    focused = null;
                    if (target.isAttached()) {
                        // Focusable.blur() sets this marker on the element so
                        // that the BlurEvent reports isFromClient() == false
                        ObjectNode eventData = JacksonUtils.createObjectNode();
                        eventData.put("event.target._nextBlurIsFromClient",
                                false);
                        fireDomEvent(target, "blur", eventData);
                    }
                }
            }
        } finally {
            flushing = false;
        }
    }

    private static UI uiOf(Component component) {
        return component.getUI().orElseGet(UI::getCurrent);
    }

    private static FocusTracker getOrCreate(UI ui) {
        FocusTracker tracker = ComponentUtil.getData(ui, FocusTracker.class);
        if (tracker == null) {
            tracker = new FocusTracker();
            ComponentUtil.setData(ui, FocusTracker.class, tracker);
        }
        return tracker;
    }

    private static void fireDomEvent(Component component, String eventType) {
        fireDomEvent(component, eventType, JacksonUtils.createObjectNode());
    }

    private static void fireDomEvent(Component component, String eventType,
            ObjectNode eventData) {
        component.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(component.getElement(), eventType,
                        eventData));
    }
}
