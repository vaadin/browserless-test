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
package com.vaadin.flow.component.textfield;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages
class NumberFieldTesterTest extends BrowserlessTest {

    private NumberFieldView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(NumberFieldView.class);
        view = navigate(NumberFieldView.class);
    }

    @Test
    public void readOnlyNumberField_isNotUsable() {
        view.numberField.setReadOnly(true);

        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);

        Assertions.assertFalse(nf_.isUsable(),
                "Read only NumberField shouldn't be usable");
    }

    @Test
    public void readOnlyNumberField_automatictester_readOnlyIsCheckedInUsable() {
        view.numberField.setReadOnly(true);

        Assertions.assertFalse(test(view.numberField).isUsable(),
                "Read only NumberField shouldn't be usable");
    }

    @Test
    public void setNumberFieldValue_eventIsFired_valueIsSet() {

        AtomicReference<Double> value = new AtomicReference<>(null);

        view.numberField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<NumberField, Double>>) event -> {
                    if (event.isFromClient()) {
                        value.compareAndSet(null, event.getValue());
                    }
                });

        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        final Double newValue = 15d;
        nf_.setValue(newValue);

        Assertions.assertEquals(newValue, value.get());
    }

    @Test
    public void setIntegerFieldValue_eventIsFired_valueIsSet() {

        AtomicReference<Integer> value = new AtomicReference<>(null);

        view.integerField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<IntegerField, Integer>>) event -> {
                    value.compareAndSet(null, event.getValue());
                });

        final NumberFieldTester<IntegerField, Integer> inf_ = test(
                view.integerField);
        final Integer newValue = 15;
        inf_.setValue(newValue);

        Assertions.assertEquals(newValue, value.get());
    }

    @Test
    public void nonInteractableField_throwsOnSetValue() {

        view.numberField.getElement().setEnabled(false);
        view.integerField.getElement().setEnabled(false);
        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        final NumberFieldTester<IntegerField, Integer> inf_ = test(
                view.integerField);

        Assertions.assertThrows(IllegalStateException.class,
                () -> nf_.setValue(12d),
                "Setting value to a non interactable number field should fail");
        Assertions.assertThrows(IllegalStateException.class,
                () -> inf_.setValue(12),
                "Setting value to a non interactable integer field should fail");
    }

    @Test
    public void maxValue_throwsExceptionForTooSmallValue() {
        view.numberField.setMax(10.0);

        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        final Double newValue = 15d;

        assertThrows(IllegalArgumentException.class,
                () -> nf_.setValue(newValue));
    }

    @Test
    public void minValue_throwsExceptionForTooSmallValue() {
        view.numberField.setMin(20.0);

        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        final Double newValue = 15d;

        assertThrows(IllegalArgumentException.class,
                () -> nf_.setValue(newValue));
    }

    @Test
    public void stepButtonsNotVisible_step_throws() {
        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);

        assertThrows(IllegalStateException.class, nf_::stepUp,
                "Stepping a field without step buttons should fail");
        assertThrows(IllegalStateException.class, nf_::stepDown,
                "Stepping a field without step buttons should fail");
    }

    @Test
    public void emptyNumberField_stepUp_oneStepIsSet_clientSideEventIsFired() {
        view.numberField.setStepButtonsVisible(true);
        AtomicReference<Double> value = new AtomicReference<>(null);
        view.numberField.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                value.compareAndSet(null, event.getValue());
            }
        });

        test(view.numberField).stepUp();

        Assertions.assertEquals(1d, value.get(),
                "Stepping up an empty field should set the first step");
        Assertions.assertEquals(1d, view.numberField.getValue());
    }

    @Test
    public void unalignedValue_step_movesToTheClosestValueAlignedWithStep() {
        view.numberField.setStepButtonsVisible(true);
        view.numberField.setMin(1);
        view.numberField.setStep(5);
        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        nf_.setValue(3d);

        nf_.stepUp();
        Assertions.assertEquals(6d, view.numberField.getValue(),
                "Step up should align the value with the step scale");

        nf_.stepDown();
        Assertions.assertEquals(1d, view.numberField.getValue(),
                "Step down from an aligned value should apply a full step");
    }

    @Test
    public void decimalStep_step_doesNotLosePrecision() {
        view.numberField.setStepButtonsVisible(true);
        view.numberField.setStep(0.1);
        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        nf_.setValue(0.1);

        nf_.stepUp();
        Assertions.assertEquals(0.2, view.numberField.getValue());

        nf_.stepDown();
        Assertions.assertEquals(0.1, view.numberField.getValue());
    }

    @Test
    public void stepWouldExceedBoundaries_throws_valueIsNotChanged() {
        view.numberField.setStepButtonsVisible(true);
        view.numberField.setMin(0);
        view.numberField.setMax(10);
        view.numberField.setStep(3);
        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);
        nf_.setValue(9d);

        assertThrows(IllegalStateException.class, nf_::stepUp,
                "Step up should fail when the step button would be disabled in the browser");
        Assertions.assertEquals(9d, view.numberField.getValue(),
                "A failed step should not change the value");

        nf_.setValue(0d);
        assertThrows(IllegalStateException.class, nf_::stepDown,
                "Step down should fail when the step button would be disabled in the browser");
        Assertions.assertEquals(0d, view.numberField.getValue(),
                "A failed step should not change the value");
    }

    @Test
    public void integerField_stepUpMultipleTimes_singleEventWithAllStepsApplied() {
        view.integerField.setStepButtonsVisible(true);
        view.integerField.setStep(2);
        AtomicInteger events = new AtomicInteger();
        view.integerField
                .addValueChangeListener(event -> events.incrementAndGet());

        test(view.integerField).stepUp(3);

        Assertions.assertEquals(6, view.integerField.getValue(),
                "Each of the three clicks should apply one step");
        Assertions.assertEquals(1, events.get(),
                "The value should be set once, after all the steps");
    }

    @Test
    public void nonUsableField_step_throws() {
        view.numberField.setStepButtonsVisible(true);
        view.numberField.setReadOnly(true);

        assertThrows(IllegalStateException.class,
                () -> test(view.numberField).stepUp(),
                "Stepping a read only field should fail");
    }

    @Test
    public void negativeTimes_step_throws() {
        view.numberField.setStepButtonsVisible(true);
        final NumberFieldTester<NumberField, Double> nf_ = test(
                view.numberField);

        assertThrows(IllegalArgumentException.class, () -> nf_.stepUp(-1));
        assertThrows(IllegalArgumentException.class, () -> nf_.stepDown(-1));
    }

    @Test
    public void emptyField_step_startsFromZeroOrTheClosestBoundary() {
        final NumberField positiveRange = new NumberField();
        positiveRange.setStepButtonsVisible(true);
        positiveRange.setMin(5);
        final NumberField negativeRange = new NumberField();
        negativeRange.setStepButtonsVisible(true);
        negativeRange.setMax(-3);
        negativeRange.setStep(2);
        view.add(positiveRange, negativeRange);

        test(positiveRange).stepUp();
        Assertions.assertEquals(5d, positiveRange.getValue(),
                "An empty field should first land on min when zero is below the range");

        test(negativeRange).stepUp();
        Assertions.assertEquals(-4d, negativeRange.getValue(),
                "An empty field should first land on the greatest aligned value when zero is above the range");
    }

}
