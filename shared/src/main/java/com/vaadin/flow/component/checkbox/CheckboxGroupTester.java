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
package com.vaadin.flow.component.checkbox;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.automation.NotUsableException;
import com.vaadin.flow.automation.Readable;
import com.vaadin.flow.automation.Selectable;

/**
 * Tester for CheckboxGroup components.
 *
 * @param <T>
 *            component type
 */
@Tests(fqn = "com.vaadin.flow.component.checkbox.CheckboxGroup")
public class CheckboxGroupTester<T extends CheckboxGroup<V>, V>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public CheckboxGroupTester(T component) {
        super(component);
    }

    /**
     * Selects an item by its client string representation.
     *
     * @param selection
     *            item string representation
     */
    public void selectItem(String selection) {
        ensureComponentIsUsable();
        try {
            automation().of(getComponent()).as(Selectable.class)
                    .selectByContent(selection);
        } catch (NotUsableException e) {
            // a disabled option is rejected by the capability layer; preserve
            // this tester's not-usable contract (IllegalStateException)
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Selects multiple items by client string representation.
     *
     * @param selection
     *            items string representation
     */
    public void selectItems(String... selection) {
        ensureComponentIsUsable();
        selectItems(List.of(selection));
    }

    /**
     * Selects multiple items by client string representation.
     *
     * @param selection
     *            items string representation
     */
    public void selectItems(Collection<String> selection) {
        ensureComponentIsUsable();
        try {
            Selectable selectable = automation().of(getComponent())
                    .as(Selectable.class);
            selection.forEach(selectable::selectByContent);
        } catch (NotUsableException e) {
            // a disabled option is rejected by the capability layer; preserve
            // this tester's not-usable contract (IllegalStateException)
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Selects all client usable items.
     */
    public void selectAll() {
        ensureComponentIsUsable();
        automation().of(getComponent()).as(Selectable.class).selectAll();
    }

    /**
     * Deselects an item by its client string representation.
     *
     * @param selection
     *            item string representation
     */
    public void deselectItem(String selection) {
        ensureComponentIsUsable();
        deselectItems(List.of(selection));
    }

    /**
     * Deselects multiple items by client string representation.
     *
     * @param selection
     *            items string representation
     */
    public void deselectItems(String... selection) {
        ensureComponentIsUsable();
        deselectItems(List.of(selection));
    }

    /**
     * Deselects items by client string representation.
     *
     * @param selection
     *            items string representation
     */
    public void deselectItems(Collection<String> selection) {
        ensureComponentIsUsable();
        try {
            Selectable selectable = automation().of(getComponent())
                    .as(Selectable.class);
            selection.forEach(selectable::deselectByContent);
        } catch (NotUsableException e) {
            // a disabled option is rejected by the capability layer; preserve
            // this tester's not-usable contract (IllegalStateException)
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Deselects all client usable items.
     */
    public void deselectAll() {
        ensureComponentIsUsable();
        automation().of(getComponent()).as(Selectable.class).deselectAll();
    }

    /**
     * Get the list of currently selected items.
     *
     * @return current selection, or an empty list. Never {@literal null}.
     */
    @SuppressWarnings("unchecked")
    public Set<V> getSelected() {
        Set<V> selected = new LinkedHashSet<>();
        for (Object value : automation().of(getComponent()).as(Readable.class)
                .values()) {
            selected.add((V) value);
        }
        return selected;
    }

}
