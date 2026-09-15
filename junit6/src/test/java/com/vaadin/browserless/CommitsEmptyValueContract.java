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
 * The {@link ClearContract} plus the set-time behaviour that sits next to it,
 * for the testers that expose {@code isValid()}: the number field and the
 * picker testers.
 * <p>
 * Emptying a required field is something the user can always do, so
 * {@code setValue(emptyValue)} commits the empty value and leaves the field
 * invalid instead of refusing it — the same reasoning that gives
 * {@code clear()} its unconditional behaviour. {@code clear()} remains the
 * explicit way to say it; both end in the same state.
 */
public interface CommitsEmptyValueContract extends ClearContract {

    /**
     * Invokes {@code setValue(emptyValue)} on the tester under test.
     */
    void setEmptyValue();

    /**
     * Invokes {@code isValid()} on the tester under test.
     *
     * @return whether the field under test reports itself valid
     */
    boolean isValid();

    @Test
    default void setEmptyValue_requiredField_isCommittedAndFieldIsInvalid() {
        HasValue<?, ?> field = ClearContracts.required(fieldUnderTest());

        setEmptyValue();

        Assertions.assertTrue(field.isEmpty(),
                "setValue() should commit the empty value the user can type");
        Assertions.assertFalse(isValid(),
                "emptying a required field should leave it invalid");
    }
}
