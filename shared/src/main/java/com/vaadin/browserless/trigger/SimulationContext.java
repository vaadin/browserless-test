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
package com.vaadin.browserless.trigger;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.Action;

/**
 * The context handed to an {@link ActionSimulator} when a trigger is fired: the
 * simulated event payload, the owning {@link UI}, and a helper to evaluate an
 * action's {@link Action.Input inputs} server-side.
 *
 * @since 1.1
 */
public final class SimulationContext {

    private final UI ui;
    private final ObjectNode eventData;

    SimulationContext(UI ui, ObjectNode eventData) {
        this.ui = ui;
        this.eventData = eventData;
    }

    /**
     * The UI the fired trigger belongs to.
     *
     * @return the UI, never {@code null}
     */
    public UI getUI() {
        return ui;
    }

    /**
     * The simulated event payload supplied to
     * {@link TriggerSimulation#fire(com.vaadin.flow.component.Component, String, ObjectNode)}.
     *
     * @return the event data, never {@code null}
     */
    public ObjectNode getEventData() {
        return eventData;
    }

    /**
     * Evaluates the given input server-side, reproducing the value it would
     * produce on the client at fire time (see {@link Action.Input#evaluate}).
     *
     * @param input
     *            the input to evaluate, not {@code null}
     * @return the input's value as a {@link JsonNode}
     */
    public JsonNode evaluate(Action.@Nullable Input<?> input) {
        return input == null ? null : input.evaluate(eventData);
    }
}
