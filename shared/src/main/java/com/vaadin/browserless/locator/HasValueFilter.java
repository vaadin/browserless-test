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
 * The {@code HasValue<?, ?>} bound keeps this mixin signature simple: the
 * underlying {@link com.vaadin.browserless.ComponentQuery#withValue} takes a
 * raw {@code V}, so threading the value type through here buys nothing.
 *
 * @param <C>
 *            the component type, bound to {@link HasValue}
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 */
public interface HasValueFilter<C extends Component & HasValue<?, ?>, SELF extends Locator<C, SELF>> {

    /**
     * Requires the matched component to implement {@code HasValue} and to have
     * the given value. Has no effect when {@code expectedValue} is
     * {@code null}.
     */
    @SuppressWarnings("unchecked")
    default <V> SELF withValue(V expectedValue) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withValue(expectedValue));
    }
}
