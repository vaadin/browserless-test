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

    /**
     * Empties the field, as when the user deletes its contents (or clicks the
     * clear button, where one is shown).
     * <p/>
     * Emptying is something the user can always do, so the empty value is set
     * without running the set-time validity check: a field may legitimately end
     * up invalid — a required field, for instance — once emptied.
     *
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void clear() {
        clearAsUser();
    }

    /**
     * Empties the field by clicking its clear button, as the user would.
     * <p/>
     * Unlike {@link #clear()}, which models selecting the contents and deleting
     * them and is therefore always available, this requires the clear button to
     * be visible — a hidden clear button is not something the user can click.
     *
     * @throws IllegalStateException
     *             if the component is not usable, or its clear button is not
     *             visible
     */
    public void clickClearButton() {
        clickClearButtonAsUser();
    }

    /**
     * Checks whether the current value of the field is valid, applying the same
     * constraints as the component itself: required, {@literal min},
     * {@literal max} and, when it is explicitly set, the {@literal step} scale.
     * <p>
     * A field can hold a value that does not satisfy its constraints, for
     * example when the value is set on the server or stepped from an unaligned
     * value, so a test asserting on validation state should check this instead
     * of assuming that a value that could be set is valid.
     *
     * @return {@code true} if the current value satisfies the constraints of
     *         the field
     */
    public boolean isValid() {
        final V value = getComponent().getValue();
        return isValid(value) && isAlignedWithStep(value);
    }

    private boolean isValid(V value) {
        final boolean isRequiredButEmpty = getComponent().isRequired()
                && Objects.equals(getComponent().getEmptyValue(), value);
        final boolean isGreaterThanMax = value != null
                && value.doubleValue() > getComponent().getMaxDouble();
        final boolean isSmallerThanMin = value != null
                && value.doubleValue() < getComponent().getMinDouble();

        return !(isRequiredButEmpty || isGreaterThanMax || isSmallerThanMin);
        // The step scale is deliberately not checked here: a value that is
        // not aligned with the step can be committed from the browser too, so
        // setValue refusing it would be wrong. Use isValid() to assert on it.
    }

    /**
     * Mirrors the component's own step validation: the {@literal step} scale is
     * only taken into account when the step is explicitly set.
     */
    private boolean isAlignedWithStep(V value) {
        if (value == null
                || getComponent().getElement().getProperty("step") == null
                || getComponent().getStepDouble() == 0) {
            return true;
        }
        return margin(BigDecimal.valueOf(value.doubleValue())).signum() == 0;
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
     * Each click sets the value on its own, so one value change event is fired
     * per click, as in the browser. If a click cannot be performed, the value
     * keeps the steps that were applied before it.
     *
     * @param times
     *            how many times the step up button is clicked, must be a
     *            positive integer
     * @throws IllegalArgumentException
     *             if {@code times} is not positive
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
     * Each click sets the value on its own, so one value change event is fired
     * per click, as in the browser. If a click cannot be performed, the value
     * keeps the steps that were applied before it.
     *
     * @param times
     *            how many times the step down button is clicked, must be a
     *            positive integer
     * @throws IllegalArgumentException
     *             if {@code times} is not positive
     * @throws IllegalStateException
     *             if the component is not usable, the step buttons are not
     *             visible, or the new value would be outside the
     *             {@literal min - max} range
     */
    public void stepDown(int times) {
        step(times, false);
    }

    private void step(int times, boolean up) {
        if (times <= 0) {
            throw new IllegalArgumentException(
                    "The 'times' parameter must be a positive integer.");
        }
        ensureComponentIsUsable();
        if (!getComponent().isStepButtonsVisible()) {
            throw new IllegalStateException(
                    "Cannot step the value of a field without step buttons. "
                            + "Call setStepButtonsVisible(true) on the component, "
                            + "or use setValue to type the value instead.");
        }
        for (int i = 0; i < times; i++) {
            final V currentValue = getComponent().getValue();
            final double stepped = currentValue == null ? stepFromEmpty(up)
                    : stepOnce(currentValue.doubleValue(), up);
            setValueAsUser(toValue(stepped));
        }
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
