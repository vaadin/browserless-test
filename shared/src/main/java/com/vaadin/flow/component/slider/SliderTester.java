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

import java.util.function.Consumer;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for Slider components.
 *
 * @param <T>
 *            component type
 */
@Tests(Slider.class)
public class SliderTester<T extends Slider> extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public SliderTester(T component) {
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
    public void setValue(Double value) {
        ensureComponentIsUsable();
        if (value != null) {
            if (value < getComponent().getMin()) {
                throw new IllegalArgumentException("Value " + value
                        + " is below minimum " + getComponent().getMin());
            }
            if (value > getComponent().getMax()) {
                throw new IllegalArgumentException("Value " + value
                        + " is above maximum " + getComponent().getMax());
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
        double newValue = Math.min(
                getComponent().getValue() + steps * getComponent().getStep(),
                getComponent().getMax());
        setValueAsUser(newValue);
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
        double newValue = Math.max(
                getComponent().getValue() - steps * getComponent().getStep(),
                getComponent().getMin());
        setValueAsUser(newValue);
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && !getComponent().isReadOnly();
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        if (getComponent().isReadOnly()) {
            collector.accept("read only");
        }
    }
}
