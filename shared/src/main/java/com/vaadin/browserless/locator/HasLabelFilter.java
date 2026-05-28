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
import com.vaadin.flow.component.HasLabel;

/**
 * Mixin for {@link Locator}s whose target component implements
 * {@link HasLabel}. Exposes label-based filter methods that would be
 * meaningless on components without a label, turning a call like
 * {@code findButton().withLabel("Save")} (Button is {@link
 * com.vaadin.flow.component.HasText}, not {@code HasLabel}) into a compile
 * error rather than a silent no-op.
 *
 * @param <C>
 *            the component type, bound to {@link HasLabel}
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 */
public interface HasLabelFilter<C extends Component & HasLabel, SELF extends Locator<C, SELF>> {

    /**
     * Requires the matched component's {@code label} property to be exactly the
     * given value. Use this for form fields where the end user identifies a
     * field by its label.
     */
    @SuppressWarnings("unchecked")
    default SELF withLabel(String label) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withLabel(label));
    }

    /**
     * Requires the matched component's {@code label} property to contain the
     * given text.
     */
    @SuppressWarnings("unchecked")
    default SELF withLabelContaining(String text) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withLabelContaining(text));
    }
}
