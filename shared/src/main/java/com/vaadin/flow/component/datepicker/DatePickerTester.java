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

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for the DatePicker components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(DatePicker.class)
public class DatePickerTester<T extends DatePicker> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public DatePickerTester(T component) {
        super(component);
    }

    /**
     * Set the given date as value to the component, as the user would enter it.
     * <p/>
     * A date that violates the component's constraints — outside
     * {@literal min - max}, or the empty value on a required field — is
     * committed all the same, because the browser commits it too and simply
     * leaves the field invalid. Assert that outcome with {@link #isInvalid()}
     * instead of expecting this method to throw.
     *
     * @param date
     *            date to set to the component
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void setValue(LocalDate date) {
        ensureComponentIsUsable();

        setValueAsUser(date);
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

    /**
     * Empties the field by clicking its clear button, as the user would.
     * <p/>
     * Unlike {@link #clear()}, which models selecting the contents and deleting
     * them and is therefore always available, this requires the clear button to
     * be visible — a hidden clear button is not something the user can click.
     *
     * @throws IllegalStateException
     *             if the component is not usable, or its clear button is not
     *             visible
     */
    public void clickClearButton() {
        clickClearButtonAsUser();
    }
}
