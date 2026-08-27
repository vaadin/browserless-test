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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * Keeps track of the component that has keyboard focus during a browserless
 * test, mirroring how focus behaves with a real user in a browser.
 * <p>
 * Testers report simulated user interactions here: interacting with a
 * component moves focus to it, which fires a {@code blur} DOM event on the
 * previously focused component and a {@code focus} DOM event on the newly
 * focused one, both as if they came from the client. Test authors normally
 * never need to call this class directly; focus and blur listeners fire
 * implicitly, just like in production. For explicit control there are
 * {@link ComponentTester#focus()} and {@link ComponentTester#blur()}.
 *
 * @since 1.3
 */
public final class FocusTracker {

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
    public static Optional<Component> getFocusedComponent(UI ui) {
        FocusTracker tracker = ComponentUtil.getData(ui, FocusTracker.class);
        return tracker == null ? Optional.empty()
                : Optional.ofNullable(tracker.focused);
    }

    /**
     * Moves focus to the given component as part of a simulated user
     * interaction.
     * <p>
     * Fires a {@code blur} DOM event on the previously focused component and,
     * if the given component is {@link Focusable}, a {@code focus} DOM event
     * on it. Interacting again with the already focused component is a no-op,
     * as in a browser.
     *
     * @param component
     *            the component the user interacts with, not null
     */
    static void moveFocusTo(Component component) {
        UI ui = component.getUI().orElseGet(UI::getCurrent);
        if (ui == null) {
            return;
        }
        FocusTracker tracker = getOrCreate(ui);
        if (tracker.focused == component) {
            return;
        }
        Component previous = tracker.focused;
        // Browsers only give keyboard focus to focusable elements; for
        // anything else focus falls back to the document body
        tracker.focused = component instanceof Focusable ? component : null;
        if (previous != null && previous.isAttached()) {
            fireDomEvent(previous, "blur");
        }
        if (tracker.focused != null) {
            fireDomEvent(component, "focus");
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
        UI ui = component.getUI().orElseGet(UI::getCurrent);
        if (ui != null) {
            FocusTracker tracker = getOrCreate(ui);
            if (tracker.focused == component) {
                tracker.focused = null;
            }
        }
        if (component.isAttached()) {
            fireDomEvent(component, "blur");
        }
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
        component.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(component.getElement(), eventType,
                        JacksonUtils.createObjectNode()));
    }
}
