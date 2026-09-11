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

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.shared.HasClearButton;

/**
 * Fixture helpers shared by {@link ClearContract}, {@link ClearButtonContract}
 * and {@link RefusesEmptyValueContract}.
 */
final class ClearContracts {

    private ClearContracts() {
    }

    /**
     * Puts the field in the state the contracts assert against: holding a
     * value, and required — which is exactly the state a tester's
     * {@code setValue} refuses the empty value in, so every assertion below
     * also exercises the validity-check bypass.
     */
    static HasValue<?, ?> required(HasValue<?, ?> field) {
        field.setRequiredIndicatorVisible(true);

        Assertions.assertFalse(field.isEmpty(),
                "fieldUnderTest() should return a field holding a value");
        return field;
    }

    static void setClearButtonVisible(HasValue<?, ?> field, boolean visible) {
        if (field instanceof HasClearButton clearButton) {
            clearButton.setClearButtonVisible(visible);
        }
    }
}
