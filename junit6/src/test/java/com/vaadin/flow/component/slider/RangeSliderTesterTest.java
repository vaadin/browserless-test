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
class RangeSliderTesterTest extends BrowserlessTest {

    RangeSliderView view;

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
                .setAnnotatedRoute(RangeSliderView.class);
        view = navigate(RangeSliderView.class);
    }

    @Test
    void setValue_usable_valueChanges() {
        RangeSliderValue range = new RangeSliderValue(20.0, 80.0);
        test(view.rangeSlider).setValue(range);
        Assertions.assertEquals(range, view.rangeSlider.getValue(),
                "Range value should be set");
    }

    @Test
    void setValue_usable_eventFired() {
        AtomicReference<RangeSliderValue> received = new AtomicReference<>();
        view.rangeSlider.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                received.set(event.getValue());
            }
        });

        RangeSliderValue range = new RangeSliderValue(30.0, 70.0);
        test(view.rangeSlider).setValue(range);
        Assertions.assertEquals(range, received.get(),
                "Value change event should have fired");
    }

    @Test
    void setValue_disabled_throws() {
        view.rangeSlider.setEnabled(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.rangeSlider)
                        .setValue(new RangeSliderValue(20.0, 80.0)));
    }

    @Test
    void setValue_readOnly_throws() {
        view.rangeSlider.setReadOnly(true);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.rangeSlider)
                        .setValue(new RangeSliderValue(20.0, 80.0)));
    }

    @Test
    void setValue_startBelowMin_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.rangeSlider)
                        .setValue(new RangeSliderValue(-10.0, 80.0)));
    }

    @Test
    void setValue_endAboveMax_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.rangeSlider)
                        .setValue(new RangeSliderValue(20.0, 110.0)));
    }

    @Test
    void setValue_startExceedsEnd_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.rangeSlider)
                        .setValue(new RangeSliderValue(80.0, 20.0)));
    }

    @Test
    void setStart_usable_updatesStart() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).setStart(40);
        Assertions.assertEquals(new RangeSliderValue(40.0, 80.0),
                view.rangeSlider.getValue(), "Start should be updated");
    }

    @Test
    void setEnd_usable_updatesEnd() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).setEnd(60);
        Assertions.assertEquals(new RangeSliderValue(20.0, 60.0),
                view.rangeSlider.getValue(), "End should be updated");
    }

    @Test
    void setStart_exceedsEnd_throws() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 50.0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.rangeSlider).setStart(60));
    }

    @Test
    void setEnd_belowStart_throws() {
        test(view.rangeSlider).setValue(new RangeSliderValue(50.0, 80.0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.rangeSlider).setEnd(40));
    }

    @Test
    void incrementStart_usable_increasesByStep() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).incrementStart();
        Assertions.assertEquals(30.0, view.rangeSlider.getValue().start(),
                "Start should increase by step");
    }

    @Test
    void decrementStart_usable_decreasesByStep() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).decrementStart();
        Assertions.assertEquals(10.0, view.rangeSlider.getValue().start(),
                "Start should decrease by step");
    }

    @Test
    void incrementStart_atEnd_clampsToEnd() {
        test(view.rangeSlider).setValue(new RangeSliderValue(50.0, 50.0));
        test(view.rangeSlider).incrementStart();
        Assertions.assertEquals(50.0, view.rangeSlider.getValue().start(),
                "Start should be clamped to end");
    }

    @Test
    void decrementStart_atMin_staysAtMin() {
        test(view.rangeSlider).setValue(new RangeSliderValue(0.0, 80.0));
        test(view.rangeSlider).decrementStart();
        Assertions.assertEquals(0.0, view.rangeSlider.getValue().start(),
                "Start should stay at min");
    }

    @Test
    void incrementEnd_usable_increasesByStep() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).incrementEnd();
        Assertions.assertEquals(90.0, view.rangeSlider.getValue().end(),
                "End should increase by step");
    }

    @Test
    void decrementEnd_usable_decreasesByStep() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).decrementEnd();
        Assertions.assertEquals(70.0, view.rangeSlider.getValue().end(),
                "End should decrease by step");
    }

    @Test
    void incrementEnd_atMax_staysAtMax() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 100.0));
        test(view.rangeSlider).incrementEnd();
        Assertions.assertEquals(100.0, view.rangeSlider.getValue().end(),
                "End should stay at max");
    }

    @Test
    void decrementEnd_atStart_clampsToStart() {
        test(view.rangeSlider).setValue(new RangeSliderValue(50.0, 50.0));
        test(view.rangeSlider).decrementEnd();
        Assertions.assertEquals(50.0, view.rangeSlider.getValue().end(),
                "End should be clamped to start");
    }

    @Test
    void incrementStartBy_usable_increasesByMultipleSteps() {
        test(view.rangeSlider).setValue(new RangeSliderValue(0.0, 80.0));
        test(view.rangeSlider).incrementStartBy(3);
        Assertions.assertEquals(30.0, view.rangeSlider.getValue().start(),
                "Start should increase by 3 steps");
    }

    @Test
    void decrementEndBy_usable_decreasesByMultipleSteps() {
        test(view.rangeSlider).setValue(new RangeSliderValue(20.0, 80.0));
        test(view.rangeSlider).decrementEndBy(2);
        Assertions.assertEquals(60.0, view.rangeSlider.getValue().end(),
                "End should decrease by 2 steps");
    }

    @Test
    void increment_readOnly_throws() {
        view.rangeSlider.setReadOnly(true);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.rangeSlider).incrementStart());
    }

    @Test
    void isUsable_readOnly_false() {
        view.rangeSlider.setReadOnly(true);
        Assertions.assertFalse(test(view.rangeSlider).isUsable(),
                "Read-only range slider should not be usable");
    }
}
