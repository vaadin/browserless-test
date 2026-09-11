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
package com.vaadin.flow.component.textfield;

import java.util.Objects;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for NumberField components.
 *
 * @param <T>
 *            component type
 * @param <V>
 *            value type
 * @since 1.0
 */
@Tests(fqn = { "com.vaadin.flow.component.textfield.IntegerField",
        "com.vaadin.flow.component.textfield.NumberField" })
public class NumberFieldTester<T extends AbstractNumberField<T, V>, V extends Number>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public NumberFieldTester(T component) {
        super(component);
    }

    /**
     * Set the given value for the component.
     * <p/>
     * Throws if component is not usable or the value is invalid.
     *
     * @param value
     *            value to set
     * @throws IllegalArgumentException
     *             if given value is not valid
     */
    public void setValue(V value) {
        ensureComponentIsUsable();
        if (!isValid(value)) {
            throw new IllegalArgumentException(
                    "Given value '" + value + "' is not valid");
        }
        setValueAsUser(value);
    }

    /**
     * Empties the field, as when the user deletes its contents (or clicks the
     * clear button, where one is shown).
     * <p/>
     * Emptying is something the user can always do, so the empty value is set
     * without running the set-time validity check: a field may legitimately end
     * up invalid — a required field, for instance — once emptied.
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

    private boolean isValid(V value) {
        final boolean isRequiredButEmpty = getComponent().isRequired()
                && Objects.equals(getComponent().getEmptyValue(), value);
        final boolean isGreaterThanMax = value != null
                && value.doubleValue() > getComponent().getMaxDouble();
        final boolean isSmallerThanMin = value != null
                && value.doubleValue() < getComponent().getMinDouble();

        return !(isRequiredButEmpty || isGreaterThanMax || isSmallerThanMin);
        // TODO: Can we access the Generic isValidByStep
        // || !isValidByStep(value);
    }

    // TODO: support stepUp/stepDown if controls are visible.
}
