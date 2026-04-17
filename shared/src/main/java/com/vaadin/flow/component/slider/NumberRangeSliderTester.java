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

/**
 * Base tester for {@link NumberRangeSlider} components.
 *
 * @param <T>
 *            component type
 * @param <TValue>
 *            value type
 * @param <TNumber>
 *            numeric type for start, end, min, max, step
 */
abstract class NumberRangeSliderTester<T extends NumberRangeSlider<?, TValue, TNumber>, TValue extends Range<TNumber>, TNumber extends Number>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    NumberRangeSliderTester(T component) {
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
    public void setValue(TValue value) {
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
    public void setStart(TNumber start) {
        ensureComponentIsUsable();
        TNumber end = getComponent().getValue().end();
        validateRange(start, end);
        setValueAsUser(createRange(start, end));
    }

    /**
     * Simulates the user dragging only the end (max) thumb.
     *
     * @param end
     *            new end value
     * @throws IllegalArgumentException
     *             if end is outside min/max or is below current start
     */
    public void setEnd(TNumber end) {
        ensureComponentIsUsable();
        TNumber start = getComponent().getValue().start();
        validateRange(start, end);
        setValueAsUser(createRange(start, end));
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
        TValue current = getComponent().getValue();
        double newStart = Math.min(
                current.start().doubleValue()
                        + steps * getComponent().getStep().doubleValue(),
                current.end().doubleValue());
        setValueAsUser(createRange(fromDouble(newStart), current.end()));
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
        TValue current = getComponent().getValue();
        double newStart = Math.max(
                current.start().doubleValue()
                        - steps * getComponent().getStep().doubleValue(),
                getComponent().getMin().doubleValue());
        setValueAsUser(createRange(fromDouble(newStart), current.end()));
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
        TValue current = getComponent().getValue();
        double newEnd = Math.min(
                current.end().doubleValue()
                        + steps * getComponent().getStep().doubleValue(),
                getComponent().getMax().doubleValue());
        setValueAsUser(createRange(current.start(), fromDouble(newEnd)));
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
        TValue current = getComponent().getValue();
        double newEnd = Math.max(
                current.end().doubleValue()
                        - steps * getComponent().getStep().doubleValue(),
                current.start().doubleValue());
        setValueAsUser(createRange(current.start(), fromDouble(newEnd)));
    }

    /**
     * Converts a double value to the slider's numeric type.
     *
     * @param value
     *            the double value to convert
     * @return the converted value
     */
    protected abstract TNumber fromDouble(double value);

    /**
     * Creates a range value from start and end values.
     *
     * @param start
     *            the start value
     * @param end
     *            the end value
     * @return the range value
     */
    protected abstract TValue createRange(TNumber start, TNumber end);

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

    private void validateRange(TNumber start, TNumber end) {
        double min = getComponent().getMin().doubleValue();
        double max = getComponent().getMax().doubleValue();
        if (start.doubleValue() < min) {
            throw new IllegalArgumentException(
                    "Start " + start + " is below minimum " + min);
        }
        if (end.doubleValue() > max) {
            throw new IllegalArgumentException(
                    "End " + end + " is above maximum " + max);
        }
        if (start.doubleValue() > end.doubleValue()) {
            throw new IllegalArgumentException(
                    "Start " + start + " exceeds end " + end);
        }
    }
}
