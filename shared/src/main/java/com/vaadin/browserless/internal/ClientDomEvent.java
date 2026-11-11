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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.ClickNotifier.ClickFiltersJs;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration.ShortcutFilterJs;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JavaScriptSemantics;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap.SnippetEntry;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.shared.JsonConstants;

/**
 * A DOM event as the browser dispatches it to the Flow client.
 * <p>
 * When the browser dispatches an event, the Flow client evaluates every
 * JavaScript expression the server registered for that event type — event data
 * expressions, listener filters and the {@code event.preventDefault()} calls
 * added by {@code DomListenerRegistration.preventDefault()} — against the
 * native event object, and sends the results to the server as the event data.
 * This class does the same for a simulated event, so that filtered listeners
 * see the same data as in the browser, and whether a listener prevented the
 * default action is known when the event is dispatched, as in the browser.
 * <p>
 * Only the subset of JavaScript that such expressions use is supported:
 * property reads and the {@code preventDefault()} and {@code stopPropagation()}
 * calls on {@code event}, literals, {@code !}, {@code &&}, {@code ||},
 * comparisons and parentheses. An expression outside that subset is left out of
 * the event data, as if it were undefined, unless it calls
 * {@code preventDefault()}: then the outcome of the event cannot be known and
 * dispatching fails.
 * <p>
 * Filters and event data declared as JavaScript functions of the bundle, rather
 * than as expressions, are run by a {@link DeclaredFunctionSimulation} of the
 * declared function, registered by its {@code @JsDefinition} interface. A
 * declared function without a simulation is handled like an unsupported
 * expression.
 * <p>
 * For internal use only.
 */
public final class ClientDomEvent {

    /**
     * The simulations of declared functions, by the {@code @JsDefinition}
     * interface that declares them.
     */
    private static final Map<Class<?>, Function<ClientDomEvent, DeclaredFunctionSimulation>> SIMULATIONS = Map
            .of(ShortcutFilterJs.class, KeydownFilterSimulation::new,
                    ClickFiltersJs.class, ClickFilterSimulation::new);

    private final Element element;
    private final String type;
    private final Map<String, Object> properties;
    private final Set<String> modifierState;
    private final ObjectNode eventData = JacksonUtils.createObjectNode();
    private boolean defaultPrevented;

    private ClientDomEvent(Element element, String type,
            Map<String, Object> properties, Set<String> modifierState) {
        this.element = element;
        this.type = type;
        this.properties = properties;
        this.modifierState = modifierState;
    }

    /**
     * Dispatches an event of the given type on the given element, the way the
     * browser does: the registered expressions are evaluated against an event
     * object with the given properties, giving the event data and whether the
     * default action was prevented.
     * <p>
     * The event is not yet sent to the server, see {@link #fire()}.
     *
     * @param element
     *            the element the event is dispatched on, not {@code null}
     * @param type
     *            the event type, e.g. {@code click}, not {@code null}
     * @param properties
     *            the properties of the native event object, e.g.
     *            {@code button}; values are {@link Boolean}, {@link Number} or
     *            {@link String}
     * @return the dispatched event
     * @throws IllegalStateException
     *             if a registered expression that calls
     *             {@code preventDefault()} uses JavaScript that cannot be
     *             evaluated without a browser
     */
    public static ClientDomEvent dispatch(Element element, String type,
            Map<String, ?> properties) {
        return dispatch(element, type, properties, Set.of());
    }

    /**
     * Dispatches a {@code keydown} event of the given key on the given element,
     * with the given modifiers pressed, the way the browser does when the user
     * presses the keys. See {@link #dispatch(Element, String, Map)}.
     *
     * @param element
     *            the element the event is dispatched on, not {@code null}
     * @param key
     *            the pressed key, not {@code null}
     * @param modifiers
     *            the modifiers held down while the key is pressed, not
     *            {@code null}
     * @return the dispatched event
     */
    public static ClientDomEvent dispatchKeyDown(Element element, Key key,
            Collection<? extends Key> modifiers) {
        Set<String> modifierState = modifiers.stream()
                .map(modifier -> modifier.getKeys().getFirst())
                .collect(Collectors.toUnmodifiableSet());
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("key", key.getKeys().getFirst());
        properties.put("shiftKey",
                modifierState.contains(KeyModifier.SHIFT.getKeys().getFirst()));
        properties.put("ctrlKey", modifierState
                .contains(KeyModifier.CONTROL.getKeys().getFirst()));
        properties.put("altKey",
                modifierState.contains(KeyModifier.ALT.getKeys().getFirst()));
        properties.put("metaKey",
                modifierState.contains(KeyModifier.META.getKeys().getFirst()));
        return dispatch(element, "keydown", properties, modifierState);
    }

    private static ClientDomEvent dispatch(Element element, String type,
            Map<String, ?> properties, Set<String> modifierState) {
        Map<String, Object> eventProperties = new LinkedHashMap<>(properties);
        eventProperties.putIfAbsent("type", type);
        ClientDomEvent event = new ClientDomEvent(element, type,
                eventProperties, modifierState);
        ElementListenerMap listeners = element.getNode()
                .getFeature(ElementListenerMap.class);
        for (String expression : listeners.getExpressions(type)) {
            // Property synchronization and event target mapping are resolved
            // by the client itself, they are not simulated
            if (!expression.startsWith(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN)
                    && !expression.startsWith(
                            JsonConstants.MAP_STATE_NODE_EVENT_DATA)) {
                event.evaluate(expression);
            }
        }
        for (SnippetEntry snippet : listeners.getSnippetEntries(type)) {
            event.run(snippet);
        }
        return event;
    }

    /**
     * Gets whether an expression called {@code event.preventDefault()} while
     * the event was dispatched.
     *
     * @return {@code true} if the default action of the event was prevented
     */
    public boolean isDefaultPrevented() {
        return defaultPrevented;
    }

    /**
     * Sends the event to the server, running the listeners whose filter
     * matched.
     */
    public void fire() {
        element.getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(element, type, eventData));
    }

    private void evaluate(String expression) {
        Object value;
        try {
            value = new Parser(this, expression).parse();
        } catch (UnsupportedExpressionException e) {
            if (expression.contains("preventDefault")) {
                // Whether the default action is prevented cannot be known
                throw new IllegalStateException("Cannot evaluate the"
                        + " expression '" + expression + "' of a DOM event"
                        + " listener without a browser. Only property reads"
                        + " and preventDefault() or stopPropagation() calls"
                        + " on 'event', literals, '!', '&&', '||',"
                        + " comparisons and parentheses are supported.");
            }
            // Event data that needs a browser to compute is left out, as an
            // undefined value would be
            return;
        }
        putEventData(expression, value);
    }

    private void run(SnippetEntry snippet) {
        JsCall call = snippet.call();
        Function<ClientDomEvent, DeclaredFunctionSimulation> simulationFactory = SIMULATIONS
                .get(call.definitionType());
        Object value;
        if (simulationFactory != null) {
            DeclaredFunctionSimulation simulation = simulationFactory
                    .apply(this);
            call.invokeOn(simulation);
            value = simulation.value();
        } else if (snippet.sideEffect() != null
                || call.getExpression().contains("preventDefault")) {
            // Whether the default action is prevented cannot be known
            throw new IllegalStateException("Cannot run the JavaScript"
                    + " function declared by " + call.definitionType().getName()
                    + "." + call.methodName() + "() for a DOM event listener"
                    + " without a browser.");
        } else {
            // Event data that needs a browser to compute is left out, as an
            // undefined value would be
            return;
        }
        putEventData(snippet.key(), value);
        if (snippet.sideEffect() != null
                && JavaScriptSemantics.isTrueish(value)) {
            invoke(snippet.sideEffect(), snippet.key());
        }
    }

    /**
     * Gets a property of the native event object, as the declared function
     * reads it: numbers are {@link Double}.
     *
     * @param name
     *            the name of the property, e.g. {@code key}
     * @return the value, or {@code null} if it is undefined
     */
    Object property(String name) {
        return read(name);
    }

    /**
     * Gets whether a modifier is pressed, as
     * {@code KeyboardEvent.getModifierState()} tells it.
     *
     * @param name
     *            the name of the modifier, e.g. {@code Control}
     * @return {@code true} if the modifier is pressed
     */
    boolean isModifierPressed(String name) {
        return modifierState.contains(name);
    }

    /**
     * Prevents the default action of the event, as
     * {@code event.preventDefault()} does.
     */
    void preventDefault() {
        defaultPrevented = true;
    }

    private void putEventData(String expression, Object value) {
        if (value instanceof Boolean bool) {
            eventData.put(expression, bool);
        } else if (value instanceof Double number) {
            if (number == Math.rint(number) && !Double.isInfinite(number)) {
                eventData.put(expression, number.longValue());
            } else {
                eventData.put(expression, number);
            }
        } else if (value instanceof String string) {
            eventData.put(expression, string);
        }
        // undefined is left out, as JSON.stringify does
    }

    private Object invoke(String method, String expression) {
        switch (method) {
        case "preventDefault" -> defaultPrevented = true;
        case "stopPropagation" -> {
            // Only the target element is simulated, there is nothing to stop
        }
        default -> throw unsupported(expression);
        }
        return null;
    }

    private Object read(String property) {
        if ("defaultPrevented".equals(property)) {
            return defaultPrevented;
        }
        Object value = properties.get(property);
        return value instanceof Number number ? number.doubleValue() : value;
    }

    private static UnsupportedExpressionException unsupported(
            String expression) {
        return new UnsupportedExpressionException(expression);
    }

    private static final class UnsupportedExpressionException
            extends RuntimeException {
        private UnsupportedExpressionException(String expression) {
            super(expression, null, false, false);
        }
    }

    /**
     * Recursive descent parser that evaluates the expression while parsing.
     * Like JavaScript, {@code &&} and {@code ||} short-circuit, so a call on
     * the skipped side has no effect.
     */
    private static final class Parser {

        private final ClientDomEvent event;
        private final String expression;
        private final List<String> tokens;
        private int position;

        private Parser(ClientDomEvent event, String expression) {
            this.event = event;
            this.expression = expression;
            this.tokens = tokenize(expression);
        }

        private Object parse() {
            Object value = or(true);
            if (position != tokens.size()) {
                throw unsupported(expression);
            }
            return value;
        }

        private Object or(boolean evaluate) {
            Object value = and(evaluate);
            while (accept("||")) {
                boolean done = !evaluate
                        || JavaScriptSemantics.isTrueish(value);
                Object right = and(!done);
                if (!done) {
                    value = right;
                }
            }
            return value;
        }

        private Object and(boolean evaluate) {
            Object value = comparison(evaluate);
            while (accept("&&")) {
                boolean done = !evaluate
                        || !JavaScriptSemantics.isTrueish(value);
                Object right = comparison(!done);
                if (!done) {
                    value = right;
                }
            }
            return value;
        }

        private Object comparison(boolean evaluate) {
            Object left = unary(evaluate);
            String operator = peek();
            if (operator != null
                    && List.of("===", "!==", "==", "!=", "<", ">", "<=", ">=")
                            .contains(operator)) {
                position++;
                Object right = unary(evaluate);
                return evaluate ? compare(operator, left, right) : null;
            }
            return left;
        }

        private Object unary(boolean evaluate) {
            if (accept("!")) {
                return !JavaScriptSemantics.isTrueish(unary(evaluate));
            }
            return primary(evaluate);
        }

        private Object primary(boolean evaluate) {
            String token = next();
            if ("(".equals(token)) {
                Object value = or(evaluate);
                expect(")");
                return value;
            }
            if (token.startsWith("'") || token.startsWith("\"")) {
                return token.substring(1, token.length() - 1);
            }
            if (Character.isDigit(token.charAt(0))) {
                return Double.parseDouble(token);
            }
            switch (token) {
            case "true":
                return true;
            case "false":
                return false;
            case "null", "undefined":
                return null;
            case "event":
                return member(evaluate);
            default:
                throw unsupported(expression);
            }
        }

        private Object member(boolean evaluate) {
            expect(".");
            String name = identifier();
            if (accept("(")) {
                expect(")");
                return evaluate ? event.invoke(name, expression) : null;
            }
            if (".".equals(peek())) {
                // Nested objects such as event.target are not simulated
                throw unsupported(expression);
            }
            return evaluate ? event.read(name) : null;
        }

        private static Object compare(String operator, Object left,
                Object right) {
            return switch (operator) {
            case "===", "==" -> Objects.equals(left, right);
            case "!==", "!=" -> !Objects.equals(left, right);
            default -> left instanceof Double l && right instanceof Double r
                    && switch (operator) {
                    case "<" -> l < r;
                    case ">" -> l > r;
                    case "<=" -> l <= r;
                    default -> l >= r;
                    };
            };
        }

        private String identifier() {
            String token = next();
            if (!Character.isJavaIdentifierStart(token.charAt(0))) {
                throw unsupported(expression);
            }
            return token;
        }

        private boolean accept(String token) {
            if (token.equals(peek())) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(String token) {
            if (!accept(token)) {
                throw unsupported(expression);
            }
        }

        private String peek() {
            return position < tokens.size() ? tokens.get(position) : null;
        }

        private String next() {
            if (position >= tokens.size()) {
                throw unsupported(expression);
            }
            return tokens.get(position++);
        }

        private List<String> tokenize(String source) {
            List<String> result = new ArrayList<>();
            int i = 0;
            while (i < source.length()) {
                char c = source.charAt(i);
                int start = i;
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                } else if (Character.isJavaIdentifierStart(c)) {
                    while (i < source.length() && Character
                            .isJavaIdentifierPart(source.charAt(i))) {
                        i++;
                    }
                } else if (Character.isDigit(c)) {
                    while (i < source.length()
                            && (Character.isDigit(source.charAt(i))
                                    || source.charAt(i) == '.')) {
                        i++;
                    }
                } else if (c == '\'' || c == '"') {
                    i = source.indexOf(c, i + 1);
                    if (i < 0) {
                        throw unsupported(source);
                    }
                    i++;
                } else if (source.startsWith("===", i)
                        || source.startsWith("!==", i)) {
                    i += 3;
                } else if (source.startsWith("==", i)
                        || source.startsWith("!=", i)
                        || source.startsWith("<=", i)
                        || source.startsWith(">=", i)
                        || source.startsWith("&&", i)
                        || source.startsWith("||", i)) {
                    i += 2;
                } else if ("!<>().".indexOf(c) >= 0) {
                    i++;
                } else {
                    throw unsupported(source);
                }
                result.add(source.substring(start, i));
            }
            return result;
        }
    }
}
