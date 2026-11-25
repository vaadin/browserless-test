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
package com.vaadin.flow.component.tabs;

import java.util.List;
import java.util.Objects;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;

/**
 * Tester for TabSheet component.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(TabSheet.class)
public class TabSheetTester<T extends TabSheet> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public TabSheetTester(T component) {
        super(component);
    }

    /**
     * Selects the tab with the given label.
     *
     * @param label
     *            the tab label
     * @throws IllegalArgumentException
     *             if the tab with given {@code label} is not visible.
     * @throws IllegalStateException
     *             if tab is not usable in the browser
     */
    public void select(String label) {
        Objects.requireNonNull(label, "label must not be null");
        ensureComponentIsUsable();
        Tab tab = findTab(label);
        doSelectTab(tab, "Tab with label '" + label
                + "' cannot be selected because it is not usable");
    }

    /**
     * Selects a visible tab based on its zero-based index.
     * <p>
     * The {@code index} refers to the zero-base position of the currently
     * visible tabs. For example, if there are three tabs
     * {@literal A (position 0), B (position 1) and C (position 2)}, but tab 'B'
     * is hidden, then to select tab 'C' the call must be {@code select(1)} and
     * not {@code select(2)}.
     *
     * @param index
     *            the zero-based index of the selected tab, negative value to
     *            unselect
     * @throws IllegalStateException
     *             if tab at give index is not usable in the browser
     */
    public void select(int index) {
        ensureComponentIsUsable();
        if (index >= 0) {
            doSelectTab(findTab(index), "Tab at index " + index
                    + " cannot be selected because it is not usable");
        } else {
            getComponent().setSelectedTab(null);
        }
    }

    /**
     * Checks if the tab with the given label is currently selected.
     *
     * @param label
     *            the tab label
     * @return {@literal true} if the tab is selected, {@literal false}
     *         otherwise.
     * @throws IllegalArgumentException
     *             if the tab with given {@code label} is not visible.
     */
    public boolean isSelected(String label) {
        ensureComponentIsUsable();
        return findTab(label).isSelected();
    }

    /**
     * Checks if the visible tab at the given index is currently selected.
     * <p>
     * The {@code index} refers to the zero-base position of the currently
     * visible tabs. For example, if there are three tabs
     * {@literal A (position 0), B (position 1) and C (position 2)}, but tab 'B'
     * is hidden, then to check if tab 'C' is selected the call must be
     * {@code isSelected(1)} and not {@code isSelected(2)}.
     *
     * @param index
     *            the zero-based index of the tab
     * @return {@literal true} if the tab is selected, {@literal false}
     *         otherwise.
     * @throws IllegalStateException
     *             if tab at given index is not visible in the browser
     */
    public boolean isSelected(int index) {
        ensureComponentIsUsable();
        return findTab(index).isSelected();
    }

    /**
     * Gets the visible tab with the given label.
     *
     * @param label
     *            the tab label
     * @return the visible tab with the given label
     * @throws IllegalStateException
     *             if tab is not visible
     */
    public Tab getTab(String label) {
        Objects.requireNonNull(label, "label must not be null");
        ensureComponentIsUsable();
        Tab tab = findTab(label);
        if (tab != null && !tab.isVisible()) {
            throw new IllegalStateException("Tab with label '" + label
                    + "' cannot be selected because it is not usable");
        }
        return tab;
    }

    /**
     * Gets the visible tab at given index.
     * <p>
     * The {@code index} refers to the zero-base position of the currently
     * visible tabs. For example, if there are three tabs
     * {@literal A (position 0), B (position 1) and C (position 2)}, but tab 'B'
     * is hidden, then to get tab 'C', the call must be {@code getTab(1)} and
     * not {@code getTab(2)}.
     *
     * @param index
     *            the zero-based index of the selected tab, negative value to
     *            unselect
     * @return the tab at the given index
     * @throws IllegalStateException
     *             if tab at given index is not visible in the browser
     * @throws IllegalArgumentException
     *             if the {@code index} is less than zero or greater than the
     *             number of visible tabs.
     */
    public Tab getTab(int index) {
        ensureComponentIsUsable();
        Tab tab = findTab(index);
        if (tab != null && !tab.isVisible()) {
            throw new IllegalStateException("Tab at index " + index
                    + " cannot be selected because it is not usable");
        }
        return tab;
    }

    /**
     * Gets the content of the visible tab with the given label.
     *
     * @param <E>
     *            the event type
     * @param label
     *            the tab label
     * @return the content of the tab with the given label
     * @throws IllegalStateException
     *             if tab is not visible
     */
    @SuppressWarnings("unchecked")
    public <E extends Component> E getTabContent(String label) {
        Objects.requireNonNull(label, "label must not be null");
        ensureComponentIsUsable();
        Tab tab = findTab(label);
        if (tab != null && !tab.isVisible()) {
            throw new IllegalStateException("Tab with label '" + label
                    + "' cannot be selected because it is not usable");
        }
        return (E) getComponent().getComponent(tab);
    }

    /**
     * Gets the content of the visible tab at given index.
     * <p>
     * The {@code index} refers to the zero-base position of the currently
     * visible tabs. For example, if there are three tabs
     * {@literal A (position 0), B (position 1) and C (position 2)}, but tab 'B'
     * is hidden, then to get the content of tab 'C', the call must be
     * {@code getTabContent(1)} and not {@code getTabContent(2)}.
     *
     * @param <E>
     *            the event type
     * @param index
     *            the zero-based index of the selected tab, negative value to
     *            unselect
     * @return the content of the tab at the given index
     * @throws IllegalStateException
     *             if tab is not visible
     * @throws IllegalArgumentException
     *             if the {@code index} is less than zero or greater than the
     *             number of visible tabs.
     */
    @SuppressWarnings("unchecked")
    public <E extends Component> E getTabContent(int index) {
        ensureComponentIsUsable();
        Tab tab = findTab(index);
        if (tab != null && !tab.isVisible()) {
            throw new IllegalStateException("Tab at index " + index
                    + " cannot be selected because it is not usable");
        }
        return (E) getComponent().getComponent(tab);
    }

    /**
     * Gets the labels of the visible tabs, in the order the user sees them.
     * <p>
     * Hidden tabs are left out, so the position of a label in the returned list
     * is the index that {@link #select(int)} and {@link #getTab(int)} accept.
     * <p>
     * Each entry is the tab's {@link Tab#getLabel() label}, the same text that
     * {@link #select(String)} and {@link #getTab(String)} match. A tab whose
     * caption is made of components, such as an icon and a {@code Span}, has no
     * label of its own and is listed as an empty string.
     * <p>
     * This only reads the tabs, so it also works when the component is
     * disabled.
     *
     * @return the labels of the visible tabs, never {@code null}
     * @throws IllegalStateException
     *             if the component is not visible
     */
    public List<String> getTabLabels() {
        ensureVisible();
        return getTabs().getChildren().filter(Tab.class::isInstance)
                .map(Tab.class::cast).filter(Component::isVisible)
                .map(Tab::getLabel).toList();
    }

    /**
     * Gets the label of the selected tab.
     * <p>
     * This only reads the selection, so it also works when the component is
     * disabled. A selected tab that is hidden is still reported, the same way
     * {@link #isSelected(String)} reports it.
     *
     * @return the label of the selected tab, or {@code null} if no tab is
     *         selected
     * @throws IllegalStateException
     *             if the component is not visible
     */
    public String getSelectedTabLabel() {
        ensureVisible();
        Tab selected = getComponent().getSelectedTab();
        return selected == null ? null : selected.getLabel();
    }

    private Tab findTab(String label) {
        return getComponent().getChildren().filter(Tabs.class::isInstance)
                .flatMap(tabs -> tabs.getChildren()
                        .filter(Tab.class::isInstance))
                .map(Tab.class::cast).filter(t -> label.equals(t.getLabel()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Tab with label '" + label + "' does not exist"));
    }

    private Tab findTab(int index) {
        if (index < 0) {
            throw new IllegalArgumentException(
                    "The 'index' argument should be greater than or equal to 0. It was: "
                            + index);
        }
        return (Tab) getTabs().getChildren().sequential()
                .filter(Component::isVisible).skip(index).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The 'index' argument should not be greater than or equals to the number of visible children tabs. It was: "
                                + index));
    }

    private Tabs getTabs() {
        return getComponent().getChildren().filter(Tabs.class::isInstance)
                .map(Tabs.class::cast).findFirst().orElseThrow();
    }

    private void doSelectTab(Tab tab, String errorMessage) {
        if (tab != null && (!tab.isEnabled() || !tab.isVisible())) {
            throw new IllegalStateException(errorMessage);
        }
        getComponent().setSelectedTab(tab);
    }

}
