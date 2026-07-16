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
package com.vaadin.flow.component.breadcrumbs;

import java.util.List;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.automation.PathActivatable;
import com.vaadin.flow.automation.Readable;

@Tests(Breadcrumbs.class)
public class BreadcrumbsTester<T extends Breadcrumbs>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public BreadcrumbsTester(T component) {
        super(component);
    }

    /**
     * Gets the labels of the breadcrumb items, in trail order, through the
     * shared {@link Readable} capability.
     *
     * @return the item labels
     */
    public List<String> getItemTexts() {
        ensureComponentIsUsable();
        return readAllOptions();
    }

    /**
     * Simulates a click on the item that matches the given label, navigating to
     * its path, through the shared {@link PathActivatable} capability.
     *
     * @param text
     *            the label of the breadcrumb item, not {@literal null}
     * @throws IllegalArgumentException
     *             if no item matches the label
     * @throws IllegalStateException
     *             if more than one item matches, or the item has no path (e.g.
     *             the current-page item)
     */
    public void clickItem(String text) {
        ensureComponentIsUsable();
        automation().of(getComponent()).as(PathActivatable.class)
                .activateItem(text);
    }

    /**
     * Simulates a click on the item at the given position in the trail,
     * navigating to its path, through the shared {@link PathActivatable}
     * capability.
     *
     * @param index
     *            the zero-based position of the item in the trail
     * @throws IllegalArgumentException
     *             if there is no item at the given index
     * @throws IllegalStateException
     *             if the item has no path (e.g. the current-page item)
     */
    public void clickItem(int index) {
        ensureComponentIsUsable();
        automation().of(getComponent()).as(PathActivatable.class)
                .activateItemAt(index);
    }
}
