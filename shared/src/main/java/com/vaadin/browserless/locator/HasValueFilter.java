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
package com.vaadin.browserless.locator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;

/**
 * Mixin for {@link Locator}s whose target component implements
 * {@link HasValue}. Exposes the {@code withValue} filter that would otherwise
 * be meaningless on components without a value, turning an inapplicable call
 * into a compile error rather than a silent no-op.
 * <p>
 * The value type {@code V} is threaded through the mixin header so the compiler
 * enforces it against the component's actual value type: e.g.
 * {@code findTextField().withValue(42)} fails to compile because
 * {@code TextField} is {@code HasValue<?, String>}, not
 * {@code HasValue<?, Integer>}.
 *
 * @param <C>
 *            the component type, bound to {@link HasValue} with value type
 *            {@code V}
 * @param <V>
 *            the value type exposed by the component's {@link HasValue}
 *            parameterization
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 */
public interface HasValueFilter<C extends Component & HasValue<?, V>, V, SELF extends Locator<C, SELF>> {

    /**
     * Requires the matched component to implement {@code HasValue} and to have
     * the given value. Has no effect when {@code expectedValue} is
     * {@code null}.
     */
    @SuppressWarnings("unchecked")
    default SELF withValue(V expectedValue) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withValue(expectedValue));
    }
}
