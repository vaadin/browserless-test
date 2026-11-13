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
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.internal.PrettyPrintTreeKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCall;

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
 * schedule a {@link Focusable.FocusJs} call for the client, the tracker reacts
 * to the call like a browser would when {@link JavaScriptDispatcher} sends it,
 * firing the corresponding focus/blur DOM events back. This happens at the end
 * of each simulated user interaction and on server round-trips.
 */
final class FocusTracker implements Serializable {

    private Component focused;

    private FocusTracker() {
    }

    /**
     * Gets the component currently considered focused in the given UI, if any.
     *
     * @param ui
     *            the UI to check, not null
     * @return the focused component, or empty if nothing is focused
     */
    static Optional<Component> getFocusedComponent(UI ui) {
        JavaScriptDispatcher.dispatch(ui);
        return Optional.ofNullable(getOrCreate(ui).focused);
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
     * @return the UI the component is attached to, to be used for a subsequent
     *         {@link JavaScriptDispatcher#dispatch(UI)} even if the interaction
     *         detaches the component
     * @throws IllegalStateException
     *             if the component is not attached to a UI
     */
    static UI moveFocusTo(Component component) {
        UI ui = uiOf(component);
        JavaScriptDispatcher.dispatch(ui);
        getOrCreate(ui).doMoveFocusTo(component, false);
        return ui;
    }

    private void doMoveFocusTo(Component component, boolean serverInitiated) {
        if (focused == component) {
            return;
        }
        Component previous = focused;
        // Browsers only give keyboard focus to focusable elements; for
        // anything else focus falls back to the document body
        Component next = component instanceof Focusable ? component : null;
        if (previous != null && previous.isAttached()) {
            // The blur on the previously focused element is a plain browser
            // reaction even when the focus change was server-initiated
            fireDomEvent(previous, "blur", JacksonUtils.createObjectNode());
        }
        if (next != null) {
            ObjectNode eventData = JacksonUtils.createObjectNode();
            if (serverInitiated) {
                // Focusable.focus() sets this marker on the element so that
                // the FocusEvent reports isFromClient() == false; mirror it
                eventData.put("event.target._nextFocusIsFromClient", false);
            }
            fireDomEvent(next, "focus", eventData);
        }
        // Recorded only once the events have been delivered, so that a failing
        // listener does not leave a wrong component marked as focused
        updateFocused(previous, next);
    }

    /**
     * Simulates the given component losing keyboard focus, firing a
     * {@code blur} DOM event as if it came from the client. Blurring a
     * component that does not have focus is a no-op, as in a browser.
     *
     * @param component
     *            the component to blur, not null
     * @return the UI the component is attached to, to be used for a subsequent
     *         {@link JavaScriptDispatcher#dispatch(UI)} even if a listener
     *         detaches the component
     * @throws IllegalStateException
     *             if the component is not attached to a UI
     */
    static UI blur(Component component) {
        UI ui = uiOf(component);
        JavaScriptDispatcher.dispatch(ui);
        FocusTracker tracker = getOrCreate(ui);
        if (tracker.focused == component) {
            fireDomEvent(component, "blur");
            tracker.updateFocused(component, null);
        }
        return ui;
    }

    /**
     * Creates the handler that reacts to the focus and blur calls sent to the
     * client of the given UI.
     *
     * @param ui
     *            the UI, not null
     * @return the handler, not null
     */
    static JavaScriptDispatcher.Handler handlerFor(UI ui) {
        return invocation -> {
            JsCall call = invocation.getInvocation().getJsCall();
            if (call == null
                    || call.definitionType() != Focusable.FocusJs.class) {
                return;
            }
            FocusTracker tracker = getOrCreate(ui);
            Element.get(invocation.getOwner()).getComponent()
                    .ifPresent(target -> call
                            .invokeOn(tracker.new FocusSimulation(target)));
        };
    }

    /**
     * Does on the server what the client does for a {@link Focusable.FocusJs}
     * call on the given component.
     */
    private final class FocusSimulation implements Focusable.FocusJs {

        private final Component target;

        private FocusSimulation(Component target) {
            this.target = target;
        }

        @Override
        public void focus(@Nullable ObjectNode options) {
            doMoveFocusTo(target, true);
        }

        @Override
        public void blur() {
            // blurring an element that does not have focus is a no-op in a
            // browser
            if (focused == target && target.isAttached()) {
                // Focusable.blur() sets this marker on the element so that the
                // BlurEvent reports isFromClient() == false
                ObjectNode eventData = JacksonUtils.createObjectNode();
                eventData.put("event.target._nextBlurIsFromClient", false);
                fireDomEvent(target, "blur", eventData);
                updateFocused(target, null);
            }
        }
    }

    private void updateFocused(Component expected, Component next) {
        // A listener may have moved focus elsewhere while the events were
        // being delivered; in that case the newer focus wins
        if (focused == expected) {
            focused = next;
        }
    }

    private static UI uiOf(Component component) {
        return component.getUI()
                .orElseThrow(() -> new IllegalStateException(PrettyPrintTreeKt
                        .toPrettyString(component)
                        + " is not attached to a UI, so it cannot take or lose focus"));
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
