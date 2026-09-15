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
package com.vaadin.flow.component.datepicker;

import java.time.LocalDate;
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
class DatePickerTesterTest extends BrowserlessTest
        implements CommitsEmptyValueContract, ClearButtonContract {

    DatePickerView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(DatePickerView.class);
        view = navigate(DatePickerView.class);
    }

    @Test
    void dateWithinMax_isValid_dateOverMax_isCommittedAndInvalid() {
        view.picker.setMax(LocalDate.of(1995, 1, 1));

        test(view.picker).setValue(LocalDate.of(1994, 12, 31));

        Assertions.assertTrue(test(view.picker).isValid(),
                "a date within max should leave the field valid");

        final LocalDate newValue = LocalDate.of(1995, 1, 5);
        test(view.picker).setValue(newValue);

        Assertions.assertEquals(newValue, view.picker.getValue(),
                "the date the user can type should have been committed");
        Assertions.assertFalse(test(view.picker).isValid(),
                "a date over max should leave the field invalid");
    }

    @Test
    void valueUnderMinDate_isCommitted_fieldIsInvalid() {
        view.picker.setMin(LocalDate.of(1995, 1, 5));
        final LocalDate newValue = LocalDate.of(1995, 1, 1);

        test(view.picker).setValue(newValue);

        Assertions.assertEquals(newValue, view.picker.getValue(),
                "the date the user can type should have been committed");
        Assertions.assertFalse(test(view.picker).isValid(),
                "a date under min should leave the field invalid");
    }

    @Test
    void isValid_reportsAStaleValidStateAndAnExternalInvalidState() {
        test(view.picker).setValue(LocalDate.of(1995, 1, 5));
        view.picker.setMax(LocalDate.of(1995, 1, 1));

        Assertions.assertFalse(test(view.picker).isValid(),
                "a date violating a constraint set after it was committed "
                        + "should not be valid, although the component has not "
                        + "re-run its own validation");

        view.picker.setMax(LocalDate.of(1995, 1, 5));
        view.picker.setInvalid(true);

        Assertions.assertFalse(test(view.picker).isValid(),
                "a field marked invalid from the outside should not be valid");
    }

    @Test
    void readOnlyPicker_isNotUsable() {
        view.picker.setReadOnly(true);

        Assertions.assertFalse(test(view.picker).isUsable(),
                "Read only DatePicker shouldn't be usable");
    }

    @Test
    void readOnlyPicker_setValue_throws() {
        view.picker.setReadOnly(true);

        assertThrows(IllegalStateException.class,
                () -> test(view.picker).setValue(LocalDate.of(1995, 1, 5)));
    }

    @Test
    void setValue_eventIsFired_valueIsSet() {

        AtomicReference<LocalDate> value = new AtomicReference<>(null);

        view.picker.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<DatePicker, LocalDate>>) event -> {
                    if (event.isFromClient()) {
                        value.compareAndSet(null, event.getValue());
                    }
                });

        final LocalDate newValue = LocalDate.of(1995, 1, 5);
        test(view.picker).setValue(newValue);

        Assertions.assertEquals(newValue, value.get());
    }

    @Override
    public HasValue<?, ?> fieldUnderTest() {
        view.picker.setValue(LocalDate.of(1995, 1, 5));
        return view.picker;
    }

    @Override
    public void clear() {
        test(view.picker).clear();
    }

    @Override
    public void clickClearButton() {
        test(view.picker).clickClearButton();
    }

    @Override
    public void setEmptyValue() {
        test(view.picker).setValue(view.picker.getEmptyValue());
    }

    @Override
    public boolean isValid() {
        return test(view.picker).isValid();
    }
}
