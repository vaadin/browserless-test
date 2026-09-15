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
package com.vaadin.flow.component.timepicker;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ClearButtonContract;
import com.vaadin.browserless.CommitsEmptyValueContract;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages
class TimePickerTesterTest extends BrowserlessTest
        implements CommitsEmptyValueContract, ClearButtonContract {

    TimePickerView view;
    TimePickerTester<TimePicker> pick_;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(TimePickerView.class);
        view = navigate(TimePickerView.class);
        pick_ = test(view.picker);
    }

    @Test
    void timeWithinMax_isValid_timeOverMax_isCommittedAndInvalid() {
        view.picker.setMax(LocalTime.NOON);

        pick_.setValue(LocalTime.of(10, 0));

        Assertions.assertTrue(pick_.isValid(),
                "a time within max should leave the field valid");

        final LocalTime newValue = LocalTime.of(13, 30);
        pick_.setValue(newValue);

        Assertions.assertEquals(newValue, view.picker.getValue(),
                "the time the user can type should have been committed");
        Assertions.assertFalse(pick_.isValid(),
                "a time over max should leave the field invalid");
    }

    @Test
    void valueUnderMinTime_isCommitted_fieldIsInvalid() {
        view.picker.setMin(LocalTime.NOON);
        final LocalTime newValue = LocalTime.of(10, 0);

        pick_.setValue(newValue);

        Assertions.assertEquals(newValue, view.picker.getValue(),
                "the time the user can type should have been committed");
        Assertions.assertFalse(pick_.isValid(),
                "a time under min should leave the field invalid");
    }

    @Test
    void isValid_reportsAStaleValidStateAndAnExternalInvalidState() {
        pick_.setValue(LocalTime.of(13, 30));
        view.picker.setMax(LocalTime.NOON);

        Assertions.assertFalse(pick_.isValid(),
                "a time violating a constraint set after it was committed "
                        + "should not be valid, although the component has not "
                        + "re-run its own validation");

        view.picker.setMax(LocalTime.of(13, 30));
        view.picker.setInvalid(true);

        Assertions.assertFalse(pick_.isValid(),
                "a field marked invalid from the outside should not be valid");
    }

    @Test
    void readOnlyPicker_isNotUsable() {
        view.picker.setReadOnly(true);

        Assertions.assertFalse(pick_.isUsable(),
                "Read only TimePicker shouldn't be usable");
    }

    @Test
    void readOnlyPicker_setValue_throws() {
        view.picker.setReadOnly(true);

        assertThrows(IllegalStateException.class,
                () -> pick_.setValue(LocalTime.NOON));
    }

    @Test
    void setValue_eventIsFired_valueIsSet() {

        AtomicReference<LocalTime> value = new AtomicReference<>(null);

        view.picker.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TimePicker, LocalTime>>) event -> {
                    if (event.isFromClient()) {
                        value.compareAndSet(null, event.getValue());
                    }
                });

        final LocalTime newValue = LocalTime.NOON;
        pick_.setValue(newValue);

        Assertions.assertEquals(newValue, value.get());
    }

    @Override
    public HasValue<?, ?> fieldUnderTest() {
        view.picker.setValue(LocalTime.NOON);
        return view.picker;
    }

    @Override
    public void clear() {
        pick_.clear();
    }

    @Override
    public void clickClearButton() {
        pick_.clickClearButton();
    }

    @Override
    public void setEmptyValue() {
        pick_.setValue(view.picker.getEmptyValue());
    }

    @Override
    public boolean isValid() {
        return pick_.isValid();
    }
}
