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
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.shared.ThemeVariant;

/**
 * Mixin for {@link Locator}s whose target component implements
 * {@link HasTheme}. Exposes theme-based filter methods that would be
 * meaningless on components without theme support, turning an inapplicable call
 * into a compile error rather than a silent no-op.
 *
 * @param <C>
 *            the component type, bound to {@link HasTheme}
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 */
public interface HasThemeFilter<C extends Component & HasTheme, SELF extends Locator<C, SELF>> {

    /**
     * Requires the matched component to have the given theme variant set.
     * Prefer this over the raw-string {@link #withTheme(String)} so the IDE can
     * autocomplete the variant and a typo becomes a compile error.
     *
     * <pre>{@code
     * ui.findButton().withTheme(ButtonVariant.LUMO_PRIMARY).click();
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    default SELF withTheme(ThemeVariant variant) {
        return ((Locator<C, SELF>) this).applyFilter(q -> q.withTheme(variant));
    }

    /**
     * Requires the matched component to have the given theme set.
     *
     * @deprecated use {@link #withTheme(ThemeVariant)} with the component's
     *             typed variant enum where possible. This raw-string overload
     *             remains for themes not surfaced through a {@code
     *             ThemeVariant} enum.
     */
    @Deprecated
    @SuppressWarnings({ "unchecked", "deprecation" })
    default SELF withTheme(String theme) {
        return ((Locator<C, SELF>) this).applyFilter(q -> q.withTheme(theme));
    }

    /**
     * Requires the matched component to not have the given theme variant set.
     * Prefer this over the raw-string {@link #withoutTheme(String)} for the
     * same reasons as {@link #withTheme(ThemeVariant)}.
     */
    @SuppressWarnings("unchecked")
    default SELF withoutTheme(ThemeVariant variant) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withoutTheme(variant));
    }

    /**
     * Requires the matched component to not have the given theme set.
     *
     * @deprecated use {@link #withoutTheme(ThemeVariant)} with the component's
     *             typed variant enum where possible.
     */
    @Deprecated
    @SuppressWarnings({ "unchecked", "deprecation" })
    default SELF withoutTheme(String theme) {
        return ((Locator<C, SELF>) this)
                .applyFilter(q -> q.withoutTheme(theme));
    }
}
