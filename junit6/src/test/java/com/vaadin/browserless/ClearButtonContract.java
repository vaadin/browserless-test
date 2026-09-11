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
 * The {@code clickClearButton()} contract, shared by every tester whose
 * component implements {@code HasClearButton}.
 * <p>
 * Clicking the clear button empties the field exactly as {@code clear()} does,
 * validity-check bypass included, but unlike {@code clear()} it is only
 * possible when the button is actually on screen.
 * <p>
 * A tester's test class implements this to have the contract asserted against
 * it. {@code LocatorProcessor} already fails the build when a
 * {@code HasClearButton} tester has no {@code clickClearButton()}; this is what
 * pins its behaviour.
 */
public interface ClearButtonContract {

    /**
     * The field under test: attached, usable, and holding a non-empty value.
     * Called once per test.
     *
     * @return the field the contract is asserted against
     */
    HasValue<?, ?> fieldUnderTest();

    /**
     * Invokes {@code clickClearButton()} on the tester under test.
     */
    void clickClearButton();

    @Test
    default void clickClearButton_buttonVisible_fieldIsEmptied() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());
        ClearContracts.setClearButtonVisible(field, true);

        clickClearButton();

        Assertions.assertTrue(field.isEmpty(),
                "clickClearButton() should have emptied the field");
    }

    @Test
    default void clickClearButton_buttonHidden_throws() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());
        ClearContracts.setClearButtonVisible(field, false);

        Assertions.assertThrows(IllegalStateException.class,
                this::clickClearButton,
                "a hidden clear button is not something the user can click");
        Assertions.assertFalse(field.isEmpty(),
                "value should not have changed");
    }

    @Test
    default void clickClearButton_notUsable_throws() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());
        ClearContracts.setClearButtonVisible(field, true);
        field.setReadOnly(true);

        Assertions.assertThrows(IllegalStateException.class,
                this::clickClearButton,
                "clickClearButton() should require a usable component");
        Assertions.assertFalse(field.isEmpty(),
                "value should not have changed");
    }
}
