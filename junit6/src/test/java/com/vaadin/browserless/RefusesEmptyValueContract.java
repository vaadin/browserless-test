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
 * The {@link ClearContract} plus the bypass that motivates it, for testers
 * whose {@code setValue} refuses the empty value on a required field.
 * <p>
 * Implemented by the number field and picker testers. The text field testers
 * and the html {@code Input} accept the empty value outright, so they have no
 * set-time check to bypass and implement plain {@link ClearContract}.
 */
public interface RefusesEmptyValueContract extends ClearContract {

    /**
     * Invokes {@code setValue(emptyValue)} on the tester under test.
     */
    void setEmptyValue();

    @Test
    default void clear_bypassesTheSetTimeValidityCheck() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());
        ClearContracts.setClearButtonVisible(field, false);

        Assertions.assertThrows(IllegalArgumentException.class,
                this::setEmptyValue,
                "setValue() should refuse the empty value on a required field");
        Assertions.assertFalse(field.isEmpty(),
                "a refused setValue() should not have changed the value");

        clear();

        Assertions.assertTrue(field.isEmpty(),
                "clear() should empty the field setValue() refuses to");
    }
}
