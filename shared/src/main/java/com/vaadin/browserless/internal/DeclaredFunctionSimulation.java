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
package com.vaadin.browserless.internal;

import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;

/**
 * A Java simulation of a JavaScript function that a {@code @JsDefinition}
 * interface declares as a filter or as event data of a DOM event listener,
 * which the browser runs when the event occurs.
 * <p>
 * A simulation implements the definition interface and runs the declared
 * function against one {@link ClientDomEvent}. The methods of the interface
 * return the {@link DomEventFilter} or {@link DomEventData} that the server
 * registers, not the value the function returns in the browser, so a method
 * hands that value to {@link #answer(Object)} instead and returns what it
 * returns:
 *
 * <pre>
 * public DomEventFilter isButton(int button) {
 *     return answer(Double.valueOf(button).equals(event().property("button")));
 * }
 * </pre>
 * <p>
 * Simulations are registered in {@link ClientDomEvent}, by the definition
 * interface they implement.
 * <p>
 * For internal use only.
 */
abstract class DeclaredFunctionSimulation {

    private final ClientDomEvent event;
    private Object value;

    /**
     * Creates a simulation that runs against the given event.
     *
     * @param event
     *            the event the declared function runs for, not {@code null}
     */
    protected DeclaredFunctionSimulation(ClientDomEvent event) {
        this.event = event;
    }

    /**
     * Gets the event the declared function runs for.
     *
     * @return the event, not {@code null}
     */
    protected final ClientDomEvent event() {
        return event;
    }

    /**
     * Records the value that the declared function returns in the browser.
     *
     * @param value
     *            the value, a {@link Boolean}, {@link Double} or
     *            {@link String}, or {@code null} for {@code undefined}
     * @param <T>
     *            the return type of the simulated method
     * @return {@code null}, to be returned by the simulated method
     */
    protected final <T> T answer(Object value) {
        this.value = value;
        return null;
    }

    /**
     * Gets the value recorded with {@link #answer(Object)}.
     *
     * @return the value, or {@code null} for {@code undefined}
     */
    final Object value() {
        return value;
    }
}
