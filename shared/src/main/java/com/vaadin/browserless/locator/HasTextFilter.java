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
import com.vaadin.flow.component.HasText;

/**
 * Mixin for {@link Locator}s whose target component implements {@link HasText}.
 * Exposes text-based filter methods that would be meaningless on components
 * without textual content, turning a call like
 * {@code findTextField().withText("foo")} ({@code TextField} is
 * {@link com.vaadin.flow.component.HasValue}/{@link com.vaadin.flow.component.HasLabel},
 * not {@code HasText}) into a compile error rather than a silent no-op.
 *
 * @param <C>
 *            the component type, bound to {@link HasText}
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 */
public interface HasTextFilter<C extends Component & HasText, SELF extends Locator<C, SELF>> {

    /** Requires the text content of the component to equal the given text. */
    @SuppressWarnings("unchecked")
    default SELF withText(String text) {
        return ((Locator<C, SELF>) this).applyFilter(q -> q.withText(text));
    }

    /** Requires the text content of the component to contain the given text. */
    @SuppressWarnings("unchecked")
    default SELF withTextContaining(String text) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withTextContaining(text));
    }
}
