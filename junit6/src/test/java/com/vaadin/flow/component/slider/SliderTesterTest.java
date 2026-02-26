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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class SliderTesterTest extends BrowserlessTest {

    SliderView view;

    @BeforeAll
    static void enableSliderFeatureFlag() {
        System.setProperty("vaadin.experimental.sliderComponent", "true");
    }

    @AfterAll
    static void clearSliderFeatureFlag() {
        System.clearProperty("vaadin.experimental.sliderComponent");
    }

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(SliderView.class);
        view = navigate(SliderView.class);
    }

    @Test
    void setValue_usable_valueChanges() {
        test(view.slider).setValue(50.0);
        Assertions.assertEquals(50.0, view.slider.getValue(),
                "Slider value should be 50");
    }

    @Test
    void setValue_usable_eventFired() {
        AtomicReference<Double> received = new AtomicReference<>();
        view.slider.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                received.set(event.getValue());
            }
        });

        test(view.slider).setValue(70.0);
        Assertions.assertEquals(70.0, received.get(),
                "Value change event should have fired");
    }

    @Test
    void setValue_disabled_throws() {
        view.slider.setEnabled(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.slider).setValue(50.0));
    }

    @Test
    void setValue_readOnly_throws() {
        view.slider.setReadOnly(true);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.slider).setValue(50.0));
    }

    @Test
    void setValue_invisible_throws() {
        view.slider.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.slider).setValue(50.0));
    }

    @Test
    void setValue_belowMin_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.slider).setValue(-10.0));
    }

    @Test
    void setValue_aboveMax_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.slider).setValue(110.0));
    }

    @Test
    void increment_usable_valueIncreasesByStep() {
        test(view.slider).setValue(50.0);
        test(view.slider).increment();
        Assertions.assertEquals(60.0, view.slider.getValue(),
                "Value should increase by step");
    }

    @Test
    void decrement_usable_valueDecreasesByStep() {
        test(view.slider).setValue(50.0);
        test(view.slider).decrement();
        Assertions.assertEquals(40.0, view.slider.getValue(),
                "Value should decrease by step");
    }

    @Test
    void increment_atMax_staysAtMax() {
        test(view.slider).setValue(100.0);
        test(view.slider).increment();
        Assertions.assertEquals(100.0, view.slider.getValue(),
                "Value should stay at max");
    }

    @Test
    void decrement_atMin_staysAtMin() {
        test(view.slider).setValue(0.0);
        test(view.slider).decrement();
        Assertions.assertEquals(0.0, view.slider.getValue(),
                "Value should stay at min");
    }

    @Test
    void incrementBy_usable_valueIncreasesByMultipleSteps() {
        test(view.slider).setValue(0.0);
        test(view.slider).incrementBy(3);
        Assertions.assertEquals(30.0, view.slider.getValue(),
                "Value should increase by 3 steps");
    }

    @Test
    void decrementBy_usable_valueDecreasesByMultipleSteps() {
        test(view.slider).setValue(50.0);
        test(view.slider).decrementBy(2);
        Assertions.assertEquals(30.0, view.slider.getValue(),
                "Value should decrease by 2 steps");
    }

    @Test
    void increment_readOnly_throws() {
        view.slider.setReadOnly(true);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.slider).increment());
    }

    @Test
    void isUsable_readOnly_false() {
        view.slider.setReadOnly(true);
        Assertions.assertFalse(test(view.slider).isUsable(),
                "Read-only slider should not be usable");
    }
}
