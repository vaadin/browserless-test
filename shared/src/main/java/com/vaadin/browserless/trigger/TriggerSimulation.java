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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.component.trigger.internal.Triggers;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

/**
 * Browserless simulation of Flow's client-side trigger/action API.
 * <p>
 * Triggers install client-side JavaScript, so a server-side click never reaches
 * them. This engine bridges the gap without a browser: it observes trigger
 * arming through {@link Triggers#addArmingListener} (recording, per {@link UI},
 * which actions are wired to which host and DOM event), and {@link #fire} then
 * reproduces the server-observable effect of each matching action through a
 * registered {@link ActionSimulator}.
 * <p>
 * The arming observer is installed per mocked environment via {@link #install}
 * and removed when that environment is torn down — mirroring how
 * {@code TestSignalEnvironment} is registered/unregistered — so no test-only
 * listener is left behind on the process-global {@link Triggers} registry
 * between tests. Built-in action simulators, being stateless, are registered
 * once on class initialisation.
 * <p>
 * Test tooling drives this indirectly — e.g. clicking a button through a
 * component tester fires {@code "click"} here in addition to the server-side
 * click event.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.1
 */
public final class TriggerSimulation {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TriggerSimulation.class);

    private static final Map<Class<? extends Action>, ActionSimulator<?>> SIMULATORS = new ConcurrentHashMap<>();

    static {
        // Stateless universal simulators; register once for the process.
        ClipboardActionSimulators.registerInto(TriggerSimulation::register);
    }

    private TriggerSimulation() {
    }

    /**
     * Installs a trigger-arming observer for the given mocked environment's
     * {@link VaadinService} and returns a {@link Registration} that removes it.
     * Call at environment setup, before the application arms any triggers (i.e.
     * before navigation), and remove it at teardown.
     * <p>
     * {@link Triggers} scopes the observer to {@code owner}'s service, so it is
     * notified only of armings from this environment — concurrent environments
     * (parallel tests, multiple apps) stay isolated without any filtering here.
     *
     * @param owner
     *            the service of the environment this observer belongs to, not
     *            {@code null}
     * @return a registration that removes the observer on teardown, never
     *         {@code null}
     */
    public static Registration install(VaadinService owner) {
        return Triggers.addArmingListener(owner, new Triggers.ArmingListener() {
            @Override
            public void onArmed(Trigger trigger, List<Action> actions) {
                UI ui = uiOf(trigger);
                if (ui != null) {
                    registryFor(ui).armed(trigger, actions);
                }
            }

            @Override
            public void onDisarmed(Trigger trigger) {
                UI ui = uiOf(trigger);
                if (ui != null) {
                    registryFor(ui).disarmed(trigger);
                }
            }
        });
    }

    /**
     * Registers a simulator for an action type, replacing any previous one.
     *
     * @param actionType
     *            the action class to handle, not {@code null}
     * @param simulator
     *            the simulator, not {@code null}
     * @param <A>
     *            the action type
     */
    public static <A extends Action> void register(Class<A> actionType,
            ActionSimulator<A> simulator) {
        SIMULATORS.put(actionType, simulator);
    }

    /**
     * Fires the DOM-event triggers armed on {@code host} that match
     * {@code eventType}, running each of their actions through its simulator.
     * Unknown action types (e.g. pure client-side ones with no server effect)
     * are ignored.
     *
     * @param host
     *            the component the gesture targets, not {@code null}
     * @param eventType
     *            the DOM event name (e.g. {@code "click"}), not {@code null}
     * @param eventData
     *            the simulated event payload, not {@code null}
     */
    public static void fire(Component host, String eventType,
            ObjectNode eventData) {
        UI ui = host.getUI().orElse(UI.getCurrent());
        if (ui == null) {
            return;
        }
        Registry registry = ComponentUtil.getData(ui, Registry.class);
        if (registry == null) {
            return;
        }
        SimulationContext context = new SimulationContext(ui, eventData);
        for (Registry.Armed armed : registry.armedOn(host.getElement())) {
            if (armed.trigger() instanceof DomEventTrigger dom
                    && eventType.equals(dom.getEventName())) {
                armed.actions().forEach(
                        action -> dispatch(action, armed.trigger(), context));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void dispatch(Action action, Trigger trigger,
            SimulationContext context) {
        ActionSimulator simulator = SIMULATORS.get(action.getClass());
        if (simulator == null) {
            LOGGER.debug("No ActionSimulator registered for {}; ignoring",
                    action.getClass().getName());
            return;
        }
        simulator.simulate(action, trigger, context);
    }

    private static UI uiOf(Trigger trigger) {
        UI current = UI.getCurrent();
        if (current != null) {
            return current;
        }
        return trigger.getHost().getComponent().flatMap(Component::getUI)
                .orElse(null);
    }

    private static Registry registryFor(UI ui) {
        Registry registry = ComponentUtil.getData(ui, Registry.class);
        if (registry == null) {
            registry = new Registry();
            ComponentUtil.setData(ui, Registry.class, registry);
        }
        return registry;
    }

    /**
     * Per-UI record of armed triggers, keyed by host element. Stored as UI data
     * so it is discarded with the UI.
     * <p>
     * Implements {@link Serializable} because it lives in the UI's attribute
     * map and must not break a session-serialization test. The armed-trigger
     * graph (triggers, actions, elements) is held {@code transient}: it is
     * test-only state that should not ride the serialized session, so a
     * deserialized registry simply starts empty.
     */
    static final class Registry implements Serializable {

        record Armed(Trigger trigger, List<Action> actions) {
        }

        // host element -> (trigger -> accumulated actions, insertion-ordered)
        private transient Map<Element, LinkedHashMap<Trigger, List<Action>>> byHost = new LinkedHashMap<>();

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            byHost = new LinkedHashMap<>();
        }

        void armed(Trigger trigger, List<Action> actions) {
            byHost.computeIfAbsent(trigger.getHost(),
                    h -> new LinkedHashMap<>())
                    .computeIfAbsent(trigger, t -> new ArrayList<>())
                    .addAll(actions);
        }

        void disarmed(Trigger trigger) {
            LinkedHashMap<Trigger, List<Action>> triggers = byHost
                    .get(trigger.getHost());
            if (triggers != null) {
                triggers.remove(trigger);
                if (triggers.isEmpty()) {
                    byHost.remove(trigger.getHost());
                }
            }
        }

        List<Armed> armedOn(Element host) {
            LinkedHashMap<Trigger, List<Action>> triggers = byHost.get(host);
            if (triggers == null) {
                return List.of();
            }
            List<Armed> result = new ArrayList<>();
            triggers.forEach((trigger, actions) -> result
                    .add(new Armed(trigger, List.copyOf(actions))));
            return result;
        }
    }
}
