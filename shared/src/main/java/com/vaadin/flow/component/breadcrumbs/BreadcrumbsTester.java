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
import java.util.stream.Stream;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.UI;

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
     * Gets the labels of the breadcrumb items, in trail order.
     *
     * @return the item labels
     */
    public List<String> getItemTexts() {
        ensureComponentIsUsable();
        return items().map(BreadcrumbsItem::getText).toList();
    }

    /**
     * Simulates a click on the item that matches the given label, navigating to
     * its path.
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
        navigateTo(findItemByText(text));
    }

    /**
     * Simulates a click on the item at the given position in the trail,
     * navigating to its path.
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
        List<BreadcrumbsItem> all = items().toList();
        if (index < 0 || index >= all.size()) {
            throw new IllegalArgumentException(
                    "Breadcrumbs has no item at index " + index);
        }
        navigateTo(all.get(index));
    }

    private void navigateTo(BreadcrumbsItem item) {
        ensureComponentIsUsable(item, ComponentTester::isUsable);
        String path = item.getPath();
        if (path == null) {
            throw new IllegalStateException(
                    "Breadcrumbs item '" + item.getText()
                            + "' has no path and cannot be navigated to");
        }
        UI.getCurrent().navigate(path);
    }

    private BreadcrumbsItem findItemByText(String text) {
        List<BreadcrumbsItem> matches = items()
                .filter(item -> text.equals(item.getText())).toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot find Breadcrumbs item '" + text + "'");
        } else if (matches.size() > 1) {
            throw new IllegalStateException("Found " + matches.size()
                    + " Breadcrumbs items with label '" + text + "'");
        }
        return matches.get(0);
    }

    private Stream<BreadcrumbsItem> items() {
        return getComponent().getChildren()
                .filter(BreadcrumbsItem.class::isInstance)
                .map(BreadcrumbsItem.class::cast);
    }
}
