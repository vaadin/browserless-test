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

import java.math.BigDecimal;
import java.util.Objects;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for NumberField components.
 *
 * @param <T>
 *            component type
 * @param <V>
 *            value type
 * @since 1.0
 */
@Tests(fqn = { "com.vaadin.flow.component.textfield.IntegerField",
        "com.vaadin.flow.component.textfield.NumberField" })
public class NumberFieldTester<T extends AbstractNumberField<T, V>, V extends Number>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public NumberFieldTester(T component) {
        super(component);
    }

    /**
     * Set the given value for the component.
     * <p/>
     * Throws if component is not usable or the value is invalid.
     *
     * @param value
     *            value to set
     * @throws IllegalArgumentException
     *             if given value is not valid
     */
    public void setValue(V value) {
        ensureComponentIsUsable();
        if (!isValid(value)) {
            throw new IllegalArgumentException(
                    "Given value '" + value + "' is not valid");
        }
        setValueAsUser(value);
    }

    private boolean isValid(V value) {
        final boolean isRequiredButEmpty = getComponent().isRequired()
                && Objects.equals(getComponent().getEmptyValue(), value);
        final boolean isGreaterThanMax = value != null
                && value.doubleValue() > getComponent().getMaxDouble();
        final boolean isSmallerThanMin = value != null
                && value.doubleValue() < getComponent().getMinDouble();

        return !(isRequiredButEmpty || isGreaterThanMax || isSmallerThanMin);
        // TODO: Can we access the Generic isValidByStep
        // || !isValidByStep(value);
    }

    /**
     * Simulates the user clicking the step up button once.
     * <p>
     * The step buttons are only rendered when
     * {@link AbstractNumberField#setStepButtonsVisible(boolean)} is enabled, so
     * stepping a field without them throws an {@link IllegalStateException}.
     *
     * @throws IllegalStateException
     *             if the component is not usable, the step buttons are not
     *             visible, or the new value would be outside the
     *             {@literal min - max} range
     */
    public void stepUp() {
        stepUp(1);
    }

    /**
     * Simulates the user clicking the step up button the given number of times.
     * <p>
     * The value is set once, after all the steps have been applied, so a single
     * value change event is fired.
     *
     * @param times
     *            how many times the step up button is clicked, must be a
     *            non-negative integer
     * @throws IllegalArgumentException
     *             if {@code times} is negative
     * @throws IllegalStateException
     *             if the component is not usable, the step buttons are not
     *             visible, or the new value would be outside the
     *             {@literal min - max} range
     */
    public void stepUp(int times) {
        step(times, true);
    }

    /**
     * Simulates the user clicking the step down button once.
     * <p>
     * The step buttons are only rendered when
     * {@link AbstractNumberField#setStepButtonsVisible(boolean)} is enabled, so
     * stepping a field without them throws an {@link IllegalStateException}.
     *
     * @throws IllegalStateException
     *             if the component is not usable, the step buttons are not
     *             visible, or the new value would be outside the
     *             {@literal min - max} range
     */
    public void stepDown() {
        stepDown(1);
    }

    /**
     * Simulates the user clicking the step down button the given number of
     * times.
     * <p>
     * The value is set once, after all the steps have been applied, so a single
     * value change event is fired.
     *
     * @param times
     *            how many times the step down button is clicked, must be a
     *            non-negative integer
     * @throws IllegalArgumentException
     *             if {@code times} is negative
     * @throws IllegalStateException
     *             if the component is not usable, the step buttons are not
     *             visible, or the new value would be outside the
     *             {@literal min - max} range
     */
    public void stepDown(int times) {
        step(times, false);
    }

    private void step(int times, boolean up) {
        if (times < 0) {
            throw new IllegalArgumentException(
                    "The 'times' parameter must be a non-negative integer.");
        }
        ensureComponentIsUsable();
        if (!getComponent().isStepButtonsVisible()) {
            throw new IllegalStateException(
                    "Cannot step the value of a field without step buttons. "
                            + "Call setStepButtonsVisible(true) on the component, "
                            + "or use setValue to type the value instead.");
        }
        if (times == 0) {
            return;
        }
        final V currentValue = getComponent().getValue();
        double value = currentValue == null ? stepFromEmpty(up)
                : stepOnce(currentValue.doubleValue(), up);
        for (int i = 1; i < times; i++) {
            value = stepOnce(value, up);
        }
        setValueAsUser(toValue(value));
    }

    /**
     * Steps an empty field, which the browser always allows: stepping starts
     * from zero, or from the closest boundary when zero is outside the
     * {@literal min - max} range.
     */
    private double stepFromEmpty(boolean up) {
        final double min = getComponent().getMinDouble();
        final double max = getComponent().getMaxDouble();
        if (min > 0) {
            return min;
        }
        if (max < 0) {
            // Stepping up in a range that is entirely negative lands on the
            // greatest value aligned with the step.
            return up ? alignedFloor(max) : max;
        }
        final double stepped = stepAligned(0, up);
        return stepped < min || stepped > max ? 0 : stepped;
    }

    private double stepOnce(double value, boolean up) {
        final double min = getComponent().getMinDouble();
        final double max = getComponent().getMaxDouble();
        // A value outside the range is snapped to the closest boundary, as the
        // browser does before applying any step.
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        final double stepped = stepAligned(value, up);
        if (stepped < min || stepped > max) {
            throw new IllegalStateException("Cannot step "
                    + (up ? "up" : "down") + " from " + value
                    + " as the new value " + stepped
                    + " would be outside the allowed range (min: " + min
                    + ", max: " + max
                    + "). The browser disables the step button in this case.");
        }
        return stepped;
    }

    /**
     * Applies a single step, aligning the result with the {@literal step}
     * scale, which is measured from {@literal min} when set and from zero
     * otherwise. This is what the step buttons do in the browser: stepping up
     * from a value that is not aligned with the step moves to the next aligned
     * value instead of adding a full step.
     */
    private double stepAligned(double value, boolean up) {
        final BigDecimal step = BigDecimal.valueOf(getStep());
        final BigDecimal current = BigDecimal.valueOf(value);
        final BigDecimal margin = margin(current);
        final BigDecimal stepped = up ? current.subtract(margin).add(step)
                : current.subtract(margin.signum() == 0 ? step : margin);
        return stepped.doubleValue();
    }

    /**
     * Returns how far the given value is from the {@literal step} scale,
     * keeping the sign of the value like the remainder in the web component's
     * {@code _getIncrement} does. A value below the step basis, that is a
     * negative value in a field without {@literal min}, therefore has a
     * negative margin and steps towards the basis, which is what the real step
     * buttons do: in a plain NumberField, stepping down from -2.5 lands on -2.
     */
    private BigDecimal margin(BigDecimal value) {
        return value.subtract(BigDecimal.valueOf(getStepBasis()))
                .remainder(BigDecimal.valueOf(getStep()));
    }

    /**
     * Returns the greatest value aligned with the {@literal step} scale that is
     * not greater than the given value.
     */
    private double alignedFloor(double value) {
        final BigDecimal current = BigDecimal.valueOf(value);
        BigDecimal margin = margin(current);
        if (margin.signum() < 0) {
            margin = margin.add(BigDecimal.valueOf(getStep()));
        }
        return current.subtract(margin).doubleValue();
    }

    private double getStep() {
        final double step = getComponent().getStepDouble();
        // The browser falls back to a step of one for a non-positive step.
        return step > 0 ? step : 1;
    }

    private double getStepBasis() {
        // The step scale is only measured from min when min is actually set on
        // the element, otherwise it is measured from zero. The element property
        // is the only reliable signal for that: getMinDouble() reports
        // Integer.MIN_VALUE for an IntegerField without min, and the
        // component's own minSetByUser flag is private.
        return getComponent().getElement().getProperty("min") == null ? 0
                : getComponent().getMinDouble();
    }

    @SuppressWarnings("unchecked")
    private V toValue(double value) {
        if (getComponent() instanceof IntegerField) {
            return (V) Integer.valueOf((int) Math.round(value));
        }
        return (V) Double.valueOf(value);
    }
}
