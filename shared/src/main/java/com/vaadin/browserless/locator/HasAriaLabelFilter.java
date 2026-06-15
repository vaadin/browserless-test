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
import com.vaadin.flow.component.HasAriaLabel;

/**
 * Mixin for {@link Locator}s whose target component implements
 * {@link HasAriaLabel}. Exposes {@code aria-label}-based filter methods that
 * would be meaningless on components that don't expose the attribute, turning
 * an inapplicable call into a compile error rather than a silent no-op.
 *
 * @param <C>
 *            the component type, bound to {@link HasAriaLabel}
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 *
 * @since 1.1
 */
public interface HasAriaLabelFilter<C extends Component & HasAriaLabel, SELF extends Locator<C, SELF>> {

    /**
     * Requires the matched component's {@code aria-label} attribute to be
     * exactly the given value.
     */
    @SuppressWarnings("unchecked")
    default SELF withAriaLabel(String ariaLabel) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withAriaLabel(ariaLabel));
    }

    /**
     * Requires the matched component's {@code aria-label} attribute to contain
     * the given text.
     */
    @SuppressWarnings("unchecked")
    default SELF withAriaLabelContaining(String text) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withAriaLabelContaining(text));
    }
}
