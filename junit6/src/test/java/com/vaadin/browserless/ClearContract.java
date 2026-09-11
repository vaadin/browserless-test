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
package com.vaadin.browserless;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.HasValue;

/**
 * The {@code clear()} contract, shared by every value tester that offers it.
 * <p>
 * {@code clear()} models deleting the field contents from the keyboard. That is
 * always available to the user, so it needs no clear button and it bypasses the
 * set-time validity check — a field may legitimately end up invalid once
 * emptied. It does require a usable component.
 * <p>
 * A tester's test class implements this to have the contract asserted against
 * it, so the expectations live in one place instead of drifting apart per
 * component. Testers whose {@code setValue} actually refuses the empty value
 * implement {@link RefusesEmptyValueContract} instead.
 */
public interface ClearContract {

    /**
     * The field under test: attached, usable, and holding a non-empty value.
     * Called once per test.
     *
     * @return the field the contract is asserted against
     */
    HasValue<?, ?> fieldUnderTest();

    /**
     * Invokes {@code clear()} on the tester under test.
     */
    void clear();

    @Test
    default void clear_noClearButtonNeeded_fieldIsEmptied() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());
        ClearContracts.setClearButtonVisible(field, false);

        clear();

        Assertions.assertTrue(field.isEmpty(),
                "clear() should have emptied the field");
    }

    @Test
    default void clear_notUsable_throws() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());
        ClearContracts.setClearButtonVisible(field, true);
        field.setReadOnly(true);

        Assertions.assertThrows(IllegalStateException.class, this::clear,
                "clear() should require a usable component");
        Assertions.assertFalse(field.isEmpty(),
                "value should not have changed");
    }
}
