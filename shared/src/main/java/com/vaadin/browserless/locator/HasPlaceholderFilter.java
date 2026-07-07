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
import com.vaadin.flow.component.HasPlaceholder;

/**
 * Mixin for {@link Locator}s whose target component implements
 * {@link HasPlaceholder}. Exposes placeholder-based filter methods that would
 * be meaningless on components without a placeholder, turning a call like
 * {@code findButton().withPlaceholder("...")} (Button is not
 * {@code HasPlaceholder}) into a compile error rather than a silent no-op.
 *
 * @param <C>
 *            the component type, bound to {@link HasPlaceholder}
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 * @since 1.1
 */
public interface HasPlaceholderFilter<C extends Component & HasPlaceholder, SELF extends Locator<C, SELF>> {

    /**
     * Requires the matched component's {@code placeholder} to be exactly the
     * given value. Useful for toolbar / search fields that intentionally omit a
     * stacked label and identify themselves to the user via placeholder text
     * instead.
     */
    @SuppressWarnings("unchecked")
    default SELF withPlaceholder(String placeholder) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withPlaceholder(placeholder));
    }

    /**
     * Requires the matched component's {@code placeholder} to contain the given
     * text.
     */
    @SuppressWarnings("unchecked")
    default SELF withPlaceholderContaining(String text) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withPlaceholderContaining(text));
    }
}
