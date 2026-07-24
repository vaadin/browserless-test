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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registry and virtual clipboard are stored in the UI attribute map, so
 * they must not break a session-serialization test.
 */
class TriggerSimulationSerializationTest {

    @Test
    void registry_serializes_droppingArmedGraph() throws Exception {
        Div host = new Div();
        ClickTrigger trigger = new ClickTrigger(host);
        Action action = new SetPropertyAction<>(host, "value", "x");

        TriggerSimulation.Registry registry = new TriggerSimulation.Registry();
        registry.armed(trigger, List.of(action));
        assertFalse(registry.armedOn(host.getElement()).isEmpty());

        TriggerSimulation.Registry restored = roundTrip(registry);

        // The transient armed-trigger graph is not serialized; a deserialized
        // registry starts empty rather than failing on the graph.
        assertTrue(restored.armedOn(host.getElement()).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
