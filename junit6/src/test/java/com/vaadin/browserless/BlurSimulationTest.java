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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.BlurNotifier.BlurEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.FocusNotifier.FocusEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldTester;

/**
 * Covers https://vaadin.com/forum/t/missing-blur-event-simulation-api-in-browserless-test/179736
 *
 * Business logic (validation, persistence, dialogs) is often attached to blur
 * listeners of Focusable components. ComponentTester exposes explicit focus()
 * and blur() methods, and beyond that focus and blur happen implicitly, the
 * way they do with a real user: FocusTracker keeps track of which component is
 * focused, entering a value through a tester first focuses the field, and
 * interacting with any other component (another setValue, a button click, ...)
 * moves focus there, firing blur on the previously focused component. Test
 * script authors never need to call blur themselves.
 */
@ViewPackages(packages = "com.example")
public class BlurSimulationTest extends BrowserlessTest {

    private Div container;
    private TextField textField;
    private AtomicReference<BlurEvent<TextField>> receivedBlur;

    @BeforeEach
    public void init() {
        container = new Div();
        getCurrentView().getElement().appendChild(container.getElement());
        textField = new TextField("Amount");
        container.add(textField);
        receivedBlur = new AtomicReference<>();
        // Stands in for real-world logic that only runs on blur
        textField.addBlurListener(receivedBlur::set);
    }

    @Test
    public void blur_throughPublicApi_firesServerSideBlurEvent() {
        TextFieldTester<TextField, String> tester = test(textField);
        tester.setValue("100");

        // Note that Flow's Focusable.blur() cannot be used here: it only
        // executes client-side JS, so the server-side event would arrive
        // earliest on the next round-trip even with a real browser
        tester.blur();

        Assertions.assertNotNull(receivedBlur.get(),
                "Blur listener should be reachable through the public tester API");
        Assertions.assertTrue(receivedBlur.get().isFromClient(),
                "Simulated blur should look like it came from the client");
    }

    @Test
    public void setValue_implicitlyFocusesField() {
        AtomicReference<FocusEvent<TextField>> receivedFocus = new AtomicReference<>();
        textField.addFocusListener(receivedFocus::set);

        test(textField).setValue("100");

        Assertions.assertNotNull(receivedFocus.get(),
                "Entering a value through the tester should first focus the field, like a real user would");
        Assertions.assertTrue(receivedFocus.get().isFromClient(),
                "Implicit focus should look like it came from the client");
    }

    @Test
    public void setValue_onAnotherField_implicitlyBlursPreviouslyFocusedField() {
        TextField other = new TextField("Other");
        container.add(other);

        test(textField).setValue("100");
        test(other).setValue("200");

        Assertions.assertNotNull(receivedBlur.get(),
                "Entering a value in another field should blur the previously focused field");
        Assertions.assertTrue(receivedBlur.get().isFromClient(),
                "Implicit blur should look like it came from the client");
    }

    @Test
    public void buttonClick_implicitlyBlursFocusedField_blurListenerRunsFirst() {
        // The classic case: value change + save button, business logic on blur
        List<String> events = new ArrayList<>();
        textField.addBlurListener(e -> events.add("blur"));
        Button save = new Button("Save", e -> events.add("click"));
        container.add(save);

        test(textField).setValue("100");
        test(save).click();

        // In a browser blur always fires before the click on the other
        // element is processed
        Assertions.assertEquals(List.of("blur", "click"), events,
                "Clicking a button should first blur the focused field, then handle the click");
    }

    @Test
    public void setValue_sameFieldConsecutively_keepsFocus() {
        AtomicInteger focusCount = new AtomicInteger();
        AtomicInteger blurCount = new AtomicInteger();
        textField.addFocusListener(e -> focusCount.incrementAndGet());
        textField.addBlurListener(e -> blurCount.incrementAndGet());

        TextFieldTester<TextField, String> tester = test(textField);
        tester.setValue("100");
        tester.setValue("200");

        Assertions.assertEquals(0, blurCount.get(),
                "Consecutive edits of the same field should not blur it in between");
        Assertions.assertTrue(focusCount.get() <= 1,
                "Editing the same field again should not re-fire focus");
    }

    @Test
    public void blur_workaroundBypassingTester_firesServerSideBlurEvent() {
        test(textField).setValue("100");

        // The workaround from the forum thread: works, but bypasses the
        // tester abstraction and forces the test author to remember
        // fromClient=true
        ComponentUtil.fireEvent(textField, new BlurEvent<>(textField, true));

        Assertions.assertNotNull(receivedBlur.get(),
                "Blur listener should have been notified");
        Assertions.assertTrue(receivedBlur.get().isFromClient());
    }
}
