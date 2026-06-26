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
package com.vaadin.flow.component.slider;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.automation.Steppable;

/**
 * Base tester for {@link NumberSlider} components.
 * <p>
 * Simulates the user moving the slider handle in the browser: setting the value
 * directly or incrementing/decrementing it by steps, with validation that the
 * component is usable (visible, enabled and not read-only) and the value stays
 * within the min/max bounds and step alignment.
 *
 * @param <T>
 *            component type
 * @param <TValue>
 *            value type
 */
abstract class NumberSliderTester<T extends NumberSlider<?, TValue>, TValue extends Number>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    NumberSliderTester(T component) {
        super(component);
    }

    /**
     * Simulates the user dragging the slider handle to the given value.
     * <p>
     * Throws if the component is not usable or the value is outside the min/max
     * range.
     *
     * @param value
     *            value to set
     * @throws IllegalArgumentException
     *             if the value is outside the min/max range
     */
    public void setValue(TValue value) {
        ensureComponentIsUsable();
        if (value != null) {
            if (value.doubleValue() < getComponent().getMin().doubleValue()) {
                throw new IllegalArgumentException("Value " + value
                        + " is below minimum " + getComponent().getMin());
            }
            if (value.doubleValue() > getComponent().getMax().doubleValue()) {
                throw new IllegalArgumentException("Value " + value
                        + " is above maximum " + getComponent().getMax());
            }
            if (!getComponent().isValueAlignedWithStep(value)) {
                throw new IllegalArgumentException(
                        "Value " + value + " is not aligned with step "
                                + getComponent().getStep() + " (min: "
                                + getComponent().getMin() + ", max: "
                                + getComponent().getMax() + ")");
            }
        }
        setValueAsUser(value);
    }

    /**
     * Simulates the user pressing the Right/Up arrow key to increase the value
     * by one step. The value is clamped to the maximum.
     */
    public void increment() {
        incrementBy(1);
    }

    /**
     * Simulates the user pressing the Left/Down arrow key to decrease the value
     * by one step. The value is clamped to the minimum.
     */
    public void decrement() {
        decrementBy(1);
    }

    /**
     * Simulates the user dragging the slider handle to increase the value by
     * the given number of steps. The value is clamped to the maximum.
     *
     * @param steps
     *            number of steps to increment
     */
    public void incrementBy(int steps) {
        ensureComponentIsUsable();
        automation().of(getComponent()).as(Steppable.class).increment(steps);
    }

    /**
     * Simulates the user dragging the slider handle to decrease the value by
     * the given number of steps. The value is clamped to the minimum.
     *
     * @param steps
     *            number of steps to decrement
     */
    public void decrementBy(int steps) {
        ensureComponentIsUsable();
        automation().of(getComponent()).as(Steppable.class).decrement(steps);
    }
}
