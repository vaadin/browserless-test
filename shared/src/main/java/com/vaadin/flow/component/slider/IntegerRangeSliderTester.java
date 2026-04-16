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

import com.vaadin.browserless.Tests;

/**
 * Tester for IntegerRangeSlider components.
 *
 * @param <T>
 *            component type
 */
@Tests(IntegerRangeSlider.class)
public class IntegerRangeSliderTester<T extends IntegerRangeSlider>
        extends NumberRangeSliderTester<T, IntegerRangeSliderValue, Integer> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public IntegerRangeSliderTester(T component) {
        super(component);
    }

    @Override
    protected Integer toNumber(double value) {
        return (int) value;
    }

    @Override
    protected IntegerRangeSliderValue createValue(Integer start, Integer end) {
        return new IntegerRangeSliderValue(start, end);
    }
}
