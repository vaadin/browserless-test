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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration.ShortcutFilterJs;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientDomEventTest {

    private static final Map<String, Object> EVENT = Map.of("button", 0,
            "ctrlKey", false, "key", "Enter", "pressure", 0.5);

    private static final ShortcutFilterJs SHORTCUT_FILTERS = JsSnippets
            .of(ShortcutFilterJs.class);

    private final Element element = new Element("div");
    private final List<DomEvent> fired = new ArrayList<>();

    @ParameterizedTest
    @CsvSource(delimiter = ';', quoteCharacter = '`', value = {
            "event.button;0", "event.ctrlKey;false", "event.pressure;0.5",
            "event.key;\"Enter\"", "event.type;\"click\"",
            "event.button === 0;true", "event.button !== 0;false",
            "event.button == 0;true", "event.button != 0;false",
            "event.button < 1;true", "event.button <= 0;true",
            "event.button > 0;false", "event.button >= 1;false",
            "event.key === 'Enter';true", "event.key === \"Escape\";false",
            "!event.ctrlKey;true", "!!event.button;false",
            "event.ctrlKey || event.key;\"Enter\"",
            "event.button && event.key;0",
            "(event.button === 0 || event.ctrlKey) && event.key === 'Enter';true",
            "event.missing === undefined;true", "event.missing === null;true",
            "event.defaultPrevented;false" })
    void dispatch_evaluatesExpressionAgainstEvent(String expression,
            String json) {
        element.addEventListener("click", fired::add).addEventData(expression);

        ClientDomEvent.dispatch(element, "click", EVENT).fire();

        assertEquals(json, eventData().get(expression).toString());
    }

    @Test
    void dispatch_undefinedOrUnsupportedEventData_leftOut() {
        element.addEventListener("click", fired::add)
                .addEventData("event.missing").addEventData("element.value")
                .addEventData("event.target.id").synchronizeProperty("value")
                .mapEventTargetElement();

        ClientDomEvent.dispatch(element, "click", EVENT).fire();

        assertEquals(1, fired.size());
        assertFalse(eventData().has("event.missing"));
        assertFalse(eventData().has("element.value"));
        assertFalse(eventData().has("event.target.id"));
    }

    @Test
    void dispatch_filter_onlyMatchingListenerRuns() {
        List<String> runs = new ArrayList<>();
        element.addEventListener("click", event -> runs.add("left"))
                .setFilter("event.button === 0");
        element.addEventListener("click", event -> runs.add("right"))
                .setFilter("event.button === 2");

        ClientDomEvent.dispatch(element, "click", EVENT).fire();

        assertEquals(List.of("left"), runs);
    }

    @Test
    void dispatch_preventDefault_detectedBeforeFiring() {
        element.addEventListener("click", fired::add).preventDefault();

        ClientDomEvent event = ClientDomEvent.dispatch(element, "click", EVENT);

        assertTrue(event.isDefaultPrevented());
        assertTrue(fired.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', quoteCharacter = '`', value = {
            "event.ctrlKey;false", "event.button === 0;true" })
    void dispatch_filteredPreventDefault_onlyWhenFilterMatches(String filter,
            boolean prevented) {
        element.addEventListener("click", fired::add).setFilter(filter)
                .preventDefault();

        assertEquals(prevented, ClientDomEvent.dispatch(element, "click", EVENT)
                .isDefaultPrevented());
    }

    @Test
    void dispatch_shortCircuitedCall_hasNoEffect() {
        element.addEventListener("click", fired::add)
                .addEventData("event.button === 0 || event.preventDefault()");

        assertFalse(ClientDomEvent.dispatch(element, "click", EVENT)
                .isDefaultPrevented());
    }

    @Test
    void dispatch_stopPropagation_doesNotPreventDefault() {
        element.addEventListener("click", fired::add).stopPropagation();

        ClientDomEvent event = ClientDomEvent.dispatch(element, "click", EVENT);
        event.fire();

        assertFalse(event.isDefaultPrevented());
        assertEquals(1, fired.size());
    }

    @Test
    void dispatch_unsupportedPreventDefaultExpression_throws() {
        element.addEventListener("click", fired::add)
                .setFilter("event.target.id === 'x'").preventDefault();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ClientDomEvent.dispatch(element, "click", EVENT));
        assertTrue(e.getMessage().contains("without a browser"),
                e.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "KEY_S;CONTROL;true",
            "KEY_S;CONTROL SHIFT;false", "KEY_S;'';false",
            "KEY_A;CONTROL;false" })
    void dispatchKeyDown_shortcutFilter_matchesKeyAndModifiers(String key,
            String modifiers, boolean matches) {
        element.addEventListener("keydown", fired::add)
                .setFilterExpression(SHORTCUT_FILTERS.matchKeydown("Control",
                        "Shift Alt AltGraph Meta", true, null, true, true,
                        false, "KeyS"));
        List<KeyModifier> pressed = Arrays.stream(modifiers.split(" "))
                .filter(name -> !name.isEmpty()).map(KeyModifier::valueOf)
                .toList();
        Key pressedKey = "KEY_S".equals(key) ? Key.KEY_S : Key.KEY_A;

        ClientDomEvent event = ClientDomEvent.dispatchKeyDown(element,
                pressedKey, pressed);
        event.fire();

        assertEquals(matches, fired.size() == 1);
        assertEquals(matches, event.isDefaultPrevented());
    }

    @Test
    void dispatchKeyDown_eventDataOfKeyAndModifiers() {
        element.addEventListener("keydown", fired::add)
                .addEventData("event.key").addEventData("event.ctrlKey")
                .addEventData("event.shiftKey");

        ClientDomEvent
                .dispatchKeyDown(element, Key.ENTER, List.of(KeyModifier.SHIFT))
                .fire();

        assertEquals("\"Enter\"", eventData().get("event.key").toString());
        assertFalse(eventData().get("event.ctrlKey").booleanValue());
        assertTrue(eventData().get("event.shiftKey").booleanValue());
    }

    @Test
    void dispatch_unknownDeclaredFilter_listenerNotNotified() {
        element.addEventListener("click", fired::add).setFilterExpression(
                JsSnippets.of(CustomFilterJs.class).custom());

        ClientDomEvent.dispatch(element, "click", EVENT).fire();

        assertTrue(fired.isEmpty());
    }

    @Test
    void dispatch_unknownDeclaredFilterWithPreventDefault_throws() {
        element.addEventListener("click", fired::add)
                .setFilterExpression(
                        JsSnippets.of(CustomFilterJs.class).custom())
                .preventDefault();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ClientDomEvent.dispatch(element, "click", EVENT));
        assertTrue(e.getMessage().contains("without a browser"),
                e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({ "1,single", "2,double", "3,''" })
    void dispatch_clickCountFilters_onlyMatchingListenerRuns(int detail,
            String expected) {
        ClickableDiv div = new ClickableDiv();
        List<String> runs = new ArrayList<>();
        div.addSingleClickListener(event -> runs.add("single"));
        div.addDoubleClickListener(event -> runs.add("double"));

        ClientDomEvent.dispatch(div.getElement(), "click",
                Map.of("button", 0, "detail", detail)).fire();

        assertEquals(expected.isEmpty() ? List.of() : List.of(expected), runs);
    }

    @Test
    void dispatch_clickCountFilter_undefinedDetail_noMatch() {
        ClickableDiv div = new ClickableDiv();
        List<String> runs = new ArrayList<>();
        div.addSingleClickListener(event -> runs.add("single"));

        ClientDomEvent.dispatch(div.getElement(), "click", Map.of()).fire();

        assertTrue(runs.isEmpty());
    }

    @Tag("div")
    private static class ClickableDiv extends Component
            implements ClickNotifier<ClickableDiv> {
    }

    @JsDefinition
    public interface CustomFilterJs extends Serializable {
        @JsExpression("return event.target.id === 'x';")
        DomEventFilter custom();
    }

    private JsonNode eventData() {
        assertEquals(1, fired.size());
        return fired.get(0).getEventData();
    }
}
