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

import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;

/**
 * Reproduces, on the server, the server-observable effect of an {@link Action}
 * when its trigger fires — without a browser. Registered per action type with
 * {@link TriggerSimulation} and invoked by
 * {@link TriggerSimulation#fire(com.vaadin.flow.component.Component, String, tools.jackson.databind.node.ObjectNode)}.
 *
 * @param <A>
 *            the action type this simulator handles
 * @since 1.1
 */
@FunctionalInterface
public interface ActionSimulator<A extends Action> {

    /**
     * Simulates the given action firing on the given trigger.
     *
     * @param action
     *            the action to simulate, not {@code null}
     * @param trigger
     *            the trigger the action was armed on, not {@code null}
     * @param context
     *            the simulation context (event payload, UI, input evaluation),
     *            not {@code null}
     */
    void simulate(A action, Trigger trigger, SimulationContext context);
}
