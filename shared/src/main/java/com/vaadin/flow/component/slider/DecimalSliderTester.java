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
 * Tester for DecimalSlider components.
 * <p>
 * Simulates the user moving the slider handle in the browser: setting the value
 * directly or incrementing/decrementing it by steps, with validation that the
 * component is usable and the value stays within the min/max bounds and step
 * alignment.
 * <p>
 * Before Vaadin 25.2 this class was named {@code SliderTester}. It was renamed
 * without a deprecation cycle because the Slider component is experimental and
 * behind a feature flag.
 *
 * @param <T>
 *            component type
 *
 * @since 1.1
 */
@Tests(DecimalSlider.class)
public class DecimalSliderTester<T extends DecimalSlider>
        extends NumberSliderTester<T, Double> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public DecimalSliderTester(T component) {
        super(component);
    }

    @Override
    protected Double fromDouble(double value) {
        return value;
    }
}
