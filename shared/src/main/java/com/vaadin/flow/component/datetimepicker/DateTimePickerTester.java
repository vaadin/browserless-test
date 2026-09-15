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
package com.vaadin.flow.component.datetimepicker;

import java.time.LocalDateTime;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for DateTimePicker components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(DateTimePicker.class)
public class DateTimePickerTester<T extends DateTimePicker>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public DateTimePickerTester(T component) {
        super(component);
    }

    /**
     * Set the date to the component, as the user would enter it.
     * <p/>
     * A value that violates the component's constraints — outside
     * {@literal min - max}, or the empty value on a required field — is
     * committed all the same, because the browser commits it too and simply
     * leaves the field invalid. Assert that outcome with {@link #isInvalid()}
     * instead of expecting this method to throw.
     *
     * @param dateTime
     *            date time to set to component
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void setValue(LocalDateTime dateTime) {
        ensureComponentIsUsable();

        setValueAsUser(dateTime);
    }

    /**
     * Checks whether the field is currently showing as invalid, as the browser
     * would show it.
     *
     * @return {@literal true} if the field is invalid
     */
    public boolean isInvalid() {
        return getComponent().isInvalid();
    }

    /**
     * Empties the field, as when the user deletes its contents (or clicks the
     * clear button, where one is shown).
     * <p/>
     * Emptying is something the user can always do, so it needs no clear
     * button: a field may legitimately end up invalid — a required field, for
     * instance — once emptied.
     *
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void clear() {
        clearAsUser();
    }

}
