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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.example.base.WelcomeView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(packages = "com.example")
public class ComponentTesterTest extends BrowserlessTest {

    private WelcomeView home;

    @BeforeEach
    public void initHome() {
        home = getHome();
    }

    @Test
    public void canGetWrapperForView_viewIsUsable() {
        final ComponentTester<WelcomeView> home_ = test(home);
        assertTrue(home_.isUsable(), "Home should be visible and interactable");
    }

    @Test
    public void componentIsDisabled_isUsableReturnsFalse() {
        home.getElement().setEnabled(false);

        final ComponentTester<WelcomeView> home_ = test(home);
        assertFalse(home_.isUsable(),
                "Home should be visible but not interactable");
    }

    @Test
    public void componentIsHidden_isUsableReturnsFalse() {
        home.setVisible(false);

        final ComponentTester<WelcomeView> home_ = test(home);
        assertFalse(home_.isUsable(),
                "Home should not be interactable when component is not visible");
    }

    @Test
    public void componentModality_componentIsUsableReturnsCorrectly() {
        final ComponentTester<WelcomeView> home_ = test(home);

        final Span span = new Span();
        home.add(span);
        final ComponentTester<Span> span_ = test(span);

        assertTrue(span_.isUsable(), "Span should be attached to the ui");

        span_.setModal(true);

        assertTrue(span_.isUsable(),
                "Span should interactable when it is modal");
        assertFalse(home_.isUsable(),
                "Home should not be interactable when Span is modal");

        span_.setModal(false);

        assertTrue(home_.isUsable(),
                "Home should be interactable when Span is not modal");
    }

    @Test
    public void componentModality_modalityDropsOnComponentRemoval() {
        final ComponentTester<WelcomeView> home_ = test(home);

        final Span span = new Span();
        home.add(span);
        final ComponentTester<Span> span_ = test(span);

        assertTrue(span_.isUsable(), "Span should be attached to the ui");

        span_.setModal(true);

        assertTrue(span_.isUsable(),
                "Span should be interactable when it is modal");
        assertFalse(home_.isUsable(),
                "Home should not be interactable when Span is modal");

        home.remove(span);

        assertTrue(home_.isUsable(),
                "Home should be interactable when Span is removed");
    }

    @Test
    public void parentNotVisible_childIsNotInteractable() {
        final Span span = new Span();
        home.add(span);
        final ComponentTester<Span> span_ = test(span);

        assertTrue(span_.isUsable(), "Span should be attached to the ui");

        home.setVisible(false);

        assertFalse(span_.isUsable(),
                "Span should not be interactable when parent is hidden");
    }

    @Test
    public void nonAttachedComponent_isNotInteractable() {
        Span span = new Span();

        ComponentTester<Span> span_ = test(span);

        assertFalse(span_.isUsable(),
                "Span is not attached so it is not usable.");
    }

    @Test
    void findByQuery_matchingComponent_getsComponent() {
        Span one = new Span("One");
        Span two = new Span("Two");
        Div container = new Div(new Div(new Div(one)), new Div(two), new Div());

        ComponentTester<Div> wrapper_ = test(container);

        Optional<Span> result = wrapper_.findByQuery(Span.class,
                query -> query.withText("One"));
        assertTrue(result.isPresent());
        assertSame(one, result.get());

        result = wrapper_.findByQuery(Span.class,
                query -> query.withText("Two"));
        assertTrue(result.isPresent());
        assertSame(two, result.get());
    }

    @Test
    void findByQuery_notMatchingComponent_empty() {
        Span one = new Span("One");
        Span two = new Span("Two");
        Div container = new Div(new Div(new Div(one)), new Div(two), new Div());

        ComponentTester<Div> wrapper_ = test(container);

        Optional<Span> result = wrapper_.findByQuery(Span.class,
                query -> query.withText("Three"));
        assertTrue(result.isEmpty());
    }

    @Test
    void findByQuery_multipleMatchingComponents_throws() {
        Span one = new Span("Span One");
        Span two = new Span("Span Two");
        Div container = new Div(new Div(new Div(one)), new Div(two), new Div());

        ComponentTester<Div> wrapper_ = test(container);

        assertThrows(IllegalArgumentException.class,
                () -> wrapper_.findByQuery(Span.class,
                        query -> query.withTextContaining("Span")));
    }

    @Test
    void findAllByQuery_matchingComponent_getsComponents() {
        Span one = new Span("Span One");
        Span two = new Span("Span Two");
        Span three = new Span("Span Two bis");
        Div container = new Div(new Div(new Div(one)), new Div(two),
                new Div(three));

        ComponentTester<Div> wrapper_ = test(container);

        List<Span> result = wrapper_.findAllByQuery(Span.class,
                query -> query.withTextContaining("One"));
        assertIterableEquals(List.of(one), result);

        result = wrapper_.findAllByQuery(Span.class,
                query -> query.withTextContaining("Two"));
        assertIterableEquals(List.of(two, three), result);

        result = wrapper_.findAllByQuery(Span.class,
                query -> query.withTextContaining("Span"));
        assertIterableEquals(List.of(one, two, three), result);
    }

    @Test
    void findAllByQuery_notMatchingComponent_empty() {
        Span one = new Span("Span One");
        Span two = new Span("Span Two");
        Span three = new Span("Span Two bis");
        Div container = new Div(new Div(new Div(one)), new Div(two),
                new Div(three));

        ComponentTester<Div> wrapper_ = test(container);

        List<Span> result = wrapper_.findAllByQuery(Span.class,
                query -> query.withTextContaining("Three"));
        assertTrue(result.isEmpty());
    }

    private WelcomeView getHome() {
        final HasElement view = getCurrentView();
        assertTrue(view instanceof WelcomeView,
                "Home should be navigated to by default");
        return (WelcomeView) view;
    }

    @Tag("span")
    public static class Span extends Component implements HasText {
        public Span() {
        }

        public Span(String text) {
            setText(text);
        }
    }

    @Tag("div")
    public static class Div extends Component implements HasComponents {
        public Div(Component... components) {
            add(components);
        }
    }

    @Tag("div")
    public static class ClickableDiv extends Component
            implements HasComponents, ClickNotifier<ClickableDiv> {
    }

    static class ExposedTester<T extends Component> extends ComponentTester<T> {
        public ExposedTester(T component) {
            super(component);
        }

        @Override
        public void fireDomEvent(String eventType) {
            super.fireDomEvent(eventType);
        }

        @Override
        public void fireDomEvent(String eventType, ObjectNode eventData) {
            super.fireDomEvent(eventType, eventData);
        }
    }

    @Test
    void fireDomEvent_clickEvent_isFromClientTrue() {
        ClickableDiv div = new ClickableDiv();
        home.add(div);

        AtomicReference<ClickEvent<ClickableDiv>> captured = new AtomicReference<>();
        div.addClickListener(captured::set);

        new ExposedTester<>(div).fireDomEvent("click");

        assertNotNull(captured.get(),
                "ClickEvent listener should have been called");
        assertTrue(captured.get().isFromClient(),
                "ClickEvent should be from client");
    }

    @Test
    void fireDomEvent_withEventData_isFromClientTrue() {
        ClickableDiv div = new ClickableDiv();
        home.add(div);

        AtomicReference<ClickEvent<ClickableDiv>> captured = new AtomicReference<>();
        div.addClickListener(captured::set);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.screenX", 42.0);

        new ExposedTester<>(div).fireDomEvent("click", eventData);

        assertNotNull(captured.get(),
                "ClickEvent listener should have been called");
        assertTrue(captured.get().isFromClient(),
                "ClickEvent should be from client");
        assertEquals(42, captured.get().getScreenX(),
                "screenX should match event data");
    }

    @Test
    void fireDomEvent_domListener_isCalled() {
        ClickableDiv div = new ClickableDiv();
        home.add(div);

        AtomicBoolean called = new AtomicBoolean(false);
        div.getElement().addEventListener("custom-event",
                e -> called.set(true));

        new ExposedTester<>(div).fireDomEvent("custom-event");

        assertTrue(called.get(), "DOM event listener should have been called");
    }

}
