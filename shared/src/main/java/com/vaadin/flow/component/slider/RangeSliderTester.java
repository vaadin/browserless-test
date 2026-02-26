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
 * Tester for RangeSlider components.
 *
 * @param <T>
 *            component type
 */
@Tests(RangeSlider.class)
public class RangeSliderTester<T extends RangeSlider>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public RangeSliderTester(T component) {
        super(component);
    }

    /**
     * Simulates the user dragging both thumbs to set the range value.
     * <p>
     * Throws if the component is not usable, if start or end are outside the
     * min/max range, or if start exceeds end.
     *
     * @param value
     *            range value to set
     * @throws IllegalArgumentException
     *             if the value is invalid
     */
    public void setValue(RangeSliderValue value) {
        ensureComponentIsUsable();
        if (value != null) {
            validateRange(value.start(), value.end());
        }
        setValueAsUser(value);
    }

    /**
     * Simulates the user dragging only the start (min) thumb.
     *
     * @param start
     *            new start value
     * @throws IllegalArgumentException
     *             if start is outside min/max or exceeds current end
     */
    public void setStart(double start) {
        ensureComponentIsUsable();
        double end = getComponent().getValue().end();
        validateRange(start, end);
        setValueAsUser(new RangeSliderValue(start, end));
    }

    /**
     * Simulates the user dragging only the end (max) thumb.
     *
     * @param end
     *            new end value
     * @throws IllegalArgumentException
     *             if end is outside min/max or is below current start
     */
    public void setEnd(double end) {
        ensureComponentIsUsable();
        double start = getComponent().getValue().start();
        validateRange(start, end);
        setValueAsUser(new RangeSliderValue(start, end));
    }

    /**
     * Simulates pressing the arrow key on the start thumb to increase start by
     * one step. Clamped to end value.
     */
    public void incrementStart() {
        incrementStartBy(1);
    }

    /**
     * Simulates pressing the arrow key on the start thumb to decrease start by
     * one step. Clamped to min.
     */
    public void decrementStart() {
        decrementStartBy(1);
    }

    /**
     * Simulates the user dragging the start thumb to increase start by the
     * given number of steps. Clamped to end value.
     *
     * @param steps
     *            number of steps to increment
     */
    public void incrementStartBy(int steps) {
        ensureComponentIsUsable();
        RangeSliderValue current = getComponent().getValue();
        double newStart = Math.min(
                current.start() + steps * getComponent().getStep(),
                current.end());
        setValueAsUser(new RangeSliderValue(newStart, current.end()));
    }

    /**
     * Simulates the user dragging the start thumb to decrease start by the
     * given number of steps. Clamped to min.
     *
     * @param steps
     *            number of steps to decrement
     */
    public void decrementStartBy(int steps) {
        ensureComponentIsUsable();
        RangeSliderValue current = getComponent().getValue();
        double newStart = Math.max(
                current.start() - steps * getComponent().getStep(),
                getComponent().getMin());
        setValueAsUser(new RangeSliderValue(newStart, current.end()));
    }

    /**
     * Simulates pressing the arrow key on the end thumb to increase end by one
     * step. Clamped to max.
     */
    public void incrementEnd() {
        incrementEndBy(1);
    }

    /**
     * Simulates pressing the arrow key on the end thumb to decrease end by one
     * step. Clamped to start value.
     */
    public void decrementEnd() {
        decrementEndBy(1);
    }

    /**
     * Simulates the user dragging the end thumb to increase end by the given
     * number of steps. Clamped to max.
     *
     * @param steps
     *            number of steps to increment
     */
    public void incrementEndBy(int steps) {
        ensureComponentIsUsable();
        RangeSliderValue current = getComponent().getValue();
        double newEnd = Math.min(
                current.end() + steps * getComponent().getStep(),
                getComponent().getMax());
        setValueAsUser(new RangeSliderValue(current.start(), newEnd));
    }

    /**
     * Simulates the user dragging the end thumb to decrease end by the given
     * number of steps. Clamped to start value.
     *
     * @param steps
     *            number of steps to decrement
     */
    public void decrementEndBy(int steps) {
        ensureComponentIsUsable();
        RangeSliderValue current = getComponent().getValue();
        double newEnd = Math.max(
                current.end() - steps * getComponent().getStep(),
                current.start());
        setValueAsUser(new RangeSliderValue(current.start(), newEnd));
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

    private void validateRange(double start, double end) {
        double min = getComponent().getMin();
        double max = getComponent().getMax();
        if (start < min) {
            throw new IllegalArgumentException(
                    "Start " + start + " is below minimum " + min);
        }
        if (end > max) {
            throw new IllegalArgumentException(
                    "End " + end + " is above maximum " + max);
        }
        if (start > end) {
            throw new IllegalArgumentException(
                    "Start " + start + " exceeds end " + end);
        }
    }
}
