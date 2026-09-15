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
import com.vaadin.flow.component.FocusNotifier.FocusEvent;
import com.vaadin.flow.component.FocusOption;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldTester;

/**
 * Covers
 * https://vaadin.com/forum/t/missing-blur-event-simulation-api-in-browserless-test/179736
 *
 * Business logic (validation, persistence, dialogs) is often attached to blur
 * listeners of Focusable components. ComponentTester exposes explicit focus()
 * and blur() methods, and beyond that focus and blur happen implicitly, the way
 * they do with a real user: FocusTracker keeps track of which component is
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
    public void serverSideFocusInValueChangeListener_movesFocusAndBlursPreviouslyFocusedField() {
        TextField other = new TextField("Other");
        AtomicReference<FocusEvent<TextField>> otherFocus = new AtomicReference<>();
        other.addFocusListener(otherFocus::set);
        container.add(other);
        // Application logic jumping to the next field once a value is
        // entered; Focusable.focus() only schedules a client-side JS call,
        // which the framework picks up like a browser would
        textField.addValueChangeListener(e -> other.focus());

        test(textField).setValue("100");

        Assertions.assertTrue(test(other).isFocused(),
                "Server-side focus() should give the field focus");
        Assertions.assertNotNull(otherFocus.get(),
                "Focus listener should fire for server-side focus()");
        Assertions.assertFalse(otherFocus.get().isFromClient(),
                "Focusable.focus() marks the resulting focus event as not from the client");
        Assertions.assertNotNull(receivedBlur.get(),
                "The previously focused field should have been blurred");
        Assertions.assertTrue(receivedBlur.get().isFromClient(),
                "The blur on the previous field is a plain browser reaction, so it is from the client");
    }

    @Test
    public void serverSideFocusWithOptions_generatesDifferentJs_alsoDetected() {
        TextField other = new TextField("Other");
        AtomicReference<FocusEvent<TextField>> otherFocus = new AtomicReference<>();
        other.addFocusListener(otherFocus::set);
        container.add(other);
        // focus(FocusOption...) generates "this.focus($0)" instead of
        // "this.focus()" in the scheduled JavaScript
        textField.addValueChangeListener(
                e -> other.focus(FocusOption.PreventScroll.ENABLED));

        test(textField).setValue("100");

        Assertions.assertTrue(test(other).isFocused(),
                "Server-side focus(FocusOption...) should give the field focus");
        Assertions.assertNotNull(otherFocus.get(),
                "Focus listener should fire for server-side focus(FocusOption...)");
        Assertions.assertFalse(otherFocus.get().isFromClient(),
                "Focusable.focus() marks the resulting focus event as not from the client");
    }

    @Test
    public void serverSideBlurInValueChangeListener_blursFieldNotFromClient() {
        // Focusable.blur() also only schedules a client-side JS call, which
        // the framework picks up like a browser would
        textField.addValueChangeListener(e -> textField.blur());

        test(textField).setValue("100");

        Assertions.assertNotNull(receivedBlur.get(),
                "Server-side blur() should fire the blur listener");
        Assertions.assertFalse(receivedBlur.get().isFromClient(),
                "Focusable.blur() marks the resulting blur event as not from the client");
        Assertions.assertFalse(test(textField).isFocused(),
                "Server-side blur() should leave the field without focus");
    }

    @Test
    public void serverSideFocusOutsideInteraction_appliedOnRoundTrip() {
        AtomicReference<FocusEvent<TextField>> receivedFocus = new AtomicReference<>();
        textField.addFocusListener(receivedFocus::set);

        // Application code focusing a field outside of any user interaction,
        // for example when building a view
        textField.focus();

        Assertions.assertNull(receivedFocus.get(),
                "Focusable.focus() should not fire anything before the scheduled JS is processed");

        roundTrip();

        Assertions.assertNotNull(receivedFocus.get(),
                "A round-trip should apply the scheduled focus, like the browser would");
    }

    @Test
    public void buttonClickOpensDialog_serverSideFocusOnDialogField_focusesImplicitly() {
        // knoobie's case from the PR review: a button click opens a dialog
        // and the field inside is focused server-side for fast text insertion
        TextField dialogField = new TextField("Quick add");
        AtomicReference<FocusEvent<TextField>> dialogFieldFocus = new AtomicReference<>();
        dialogField.addFocusListener(dialogFieldFocus::set);
        Dialog dialog = new Dialog(dialogField);
        Button open = new Button("Open", e -> {
            dialog.open();
            dialogField.focus();
        });
        container.add(open);

        test(textField).setValue("100");
        test(open).click();

        Assertions.assertNotNull(receivedBlur.get(),
                "Clicking the button should blur the previously focused field");
        Assertions.assertNotNull(dialogFieldFocus.get(),
                "Server-side focus() in the click listener should focus the dialog field");
        Assertions.assertFalse(dialogFieldFocus.get().isFromClient(),
                "Focusable.focus() marks the resulting focus event as not from the client");
        Assertions.assertTrue(test(dialogField).isFocused(),
                "The dialog field should be tracked as the focused component");
        Assertions.assertFalse(test(textField).isFocused());
    }

    @Test
    public void blur_componentNotFocused_doesNotFireBlurEvent() {
        // In a browser blur only happens to the element that has focus
        test(textField).blur();

        Assertions.assertNull(receivedBlur.get(),
                "Blurring a component that does not have focus should be a no-op");
    }

    @Test
    public void click_onNotFocusableComponent_blursFieldAndFocusesNothing() {
        // A click on something that cannot take focus moves focus to the
        // document body in a browser
        Div plainDiv = new Div("Not focusable");
        container.add(plainDiv);

        test(textField).setValue("100");
        test(plainDiv).click();

        Assertions.assertNotNull(receivedBlur.get(),
                "Clicking a component that cannot take focus should still blur the focused field");
        Assertions.assertFalse(test(textField).isFocused(),
                "The field should have lost focus");
        Assertions.assertFalse(test(plainDiv).isFocused(),
                "A component that is not focusable should never be reported as focused");
    }

    @Test
    public void focusAndBlur_notFocusableComponent_failFast() {
        Div plainDiv = new Div();
        container.add(plainDiv);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(plainDiv).focus(),
                "Focusing a component that is not Focusable should fail fast");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(plainDiv).blur(),
                "Blurring a component that is not Focusable should fail fast");
    }

    @Test
    public void focus_readOnlyField_isFocusable() {
        textField.setReadOnly(true);

        test(textField).focus();

        Assertions.assertTrue(test(textField).isFocused(),
                "A read-only field cannot be edited but can still be focused");
    }

    @Test
    public void focus_disabledField_fails() {
        textField.setEnabled(false);

        Assertions.assertThrows(IllegalStateException.class,
                () -> test(textField).focus(),
                "A disabled field should not accept focus");
    }

    @Test
    public void focus_detachedField_fails() {
        TextFieldTester<TextField, String> tester = test(textField);
        container.remove(textField);

        Assertions.assertFalse(tester.isFocused(),
                "A detached field cannot be focused");
        Assertions.assertThrows(IllegalStateException.class, tester::focus,
                "A detached field should not accept focus");
    }

}
