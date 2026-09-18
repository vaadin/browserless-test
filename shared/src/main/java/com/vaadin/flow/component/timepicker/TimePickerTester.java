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

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for TimePicker components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(TimePicker.class)
public class TimePickerTester<T extends TimePicker> extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public TimePickerTester(T component) {
        super(component);
    }

    /**
     * Set the time to the component, as the user would enter it.
     * <p>
     * A time that violates the component's constraints — outside
     * {@literal min - max}, or the empty value on a required field — is
     * committed all the same, because the browser commits it too and simply
     * leaves the field invalid. Assert that outcome with {@link #isValid()}
     * instead of expecting this method to throw.
     *
     * @param time
     *            time to set to component
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void setValue(LocalTime time) {
        ensureComponentIsUsable();

        setValueAsUser(time);
    }

    /**
     * Checks whether the field is currently valid, applying the same
     * constraints as the component itself — required, {@literal min} and
     * {@literal max} — and honouring an invalid state set from the outside, as
     * a {@link com.vaadin.flow.data.binder.Binder} or a custom validator does.
     * <p>
     * A field can hold a value that does not satisfy its constraints — the user
     * can type one, {@link #setValue(LocalTime)} commits it as the browser
     * does, and the value can also be set on the server — so a test asserting
     * on validation state checks this instead of expecting a value to be
     * refused.
     *
     * @return {@code true} if the field is not marked invalid and its current
     *         value satisfies the constraints of the field
     */
    public boolean isValid() {
        final LocalTime time = getComponent().getValue();
        return !getComponent().isInvalid() && !getComponent()
                .getDefaultValidator().apply(time, null).isError();
    }

    /**
     * Empties the field, as when the user deletes its contents (or clicks the
     * clear button, where one is shown).
     * <p>
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
     * <p>
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
