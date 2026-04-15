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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages
class IntegerSliderTesterTest extends BrowserlessTest {

    IntegerSliderView view;

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
                .setAnnotatedRoute(IntegerSliderView.class);
        view = navigate(IntegerSliderView.class);
    }

    @Test
    void setValue_usable_valueChanges() {
        test(view.slider).setValue(50);
        assertEquals(50, view.slider.getValue(),
                "Slider value should be 50");
    }

    @Test
    void setValue_usable_eventFired() {
        AtomicReference<Integer> received = new AtomicReference<>();
        view.slider.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                received.set(event.getValue());
            }
        });

        test(view.slider).setValue(70);
        assertEquals(70, received.get(),
                "Value change event should have fired");
    }

    @Test
    void setValue_disabled_throws() {
        view.slider.setEnabled(false);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> test(view.slider).setValue(50));
        assertTrue(exception.getMessage().contains("not enabled"));
    }

    @Test
    void setValue_readOnly_throws() {
        view.slider.setReadOnly(true);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> test(view.slider).setValue(50));
        assertTrue(exception.getMessage().contains("read only"));
    }

    @Test
    void setValue_invisible_throws() {
        view.slider.setVisible(false);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> test(view.slider).setValue(50));
        assertTrue(exception.getMessage().contains("not visible"));
    }

    @Test
    void setValue_belowMin_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> test(view.slider).setValue(-10));
        assertTrue(exception.getMessage().contains("below minimum"));
    }

    @Test
    void setValue_aboveMax_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> test(view.slider).setValue(110));
        assertTrue(exception.getMessage().contains("above maximum"));
    }

    @Test
    void setValue_notAlignedWithStep_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> test(view.slider).setValue(55));
        assertTrue(exception.getMessage().contains("not aligned with step"));
    }

    @Test
    void increment_usable_valueIncreasesByStep() {
        test(view.slider).setValue(50);
        test(view.slider).increment();
        assertEquals(60, view.slider.getValue(),
                "Value should increase by step");
    }

    @Test
    void decrement_usable_valueDecreasesByStep() {
        test(view.slider).setValue(50);
        test(view.slider).decrement();
        assertEquals(40, view.slider.getValue(),
                "Value should decrease by step");
    }

    @Test
    void increment_atMax_staysAtMax() {
        test(view.slider).setValue(100);
        test(view.slider).increment();
        assertEquals(100, view.slider.getValue(),
                "Value should stay at max");
    }

    @Test
    void decrement_atMin_staysAtMin() {
        test(view.slider).setValue(0);
        test(view.slider).decrement();
        assertEquals(0, view.slider.getValue(), "Value should stay at min");
    }

    @Test
    void incrementBy_usable_valueIncreasesByMultipleSteps() {
        test(view.slider).setValue(0);
        test(view.slider).incrementBy(3);
        assertEquals(30, view.slider.getValue(),
                "Value should increase by 3 steps");
    }

    @Test
    void decrementBy_usable_valueDecreasesByMultipleSteps() {
        test(view.slider).setValue(50);
        test(view.slider).decrementBy(2);
        assertEquals(30, view.slider.getValue(),
                "Value should decrease by 2 steps");
    }

    @Test
    void increment_readOnly_throws() {
        view.slider.setReadOnly(true);
        assertThrows(IllegalStateException.class,
                () -> test(view.slider).increment());
    }

    @Test
    void isUsable_readOnly_false() {
        view.slider.setReadOnly(true);
        assertFalse(test(view.slider).isUsable(),
                "Read-only slider should not be usable");
    }
}
