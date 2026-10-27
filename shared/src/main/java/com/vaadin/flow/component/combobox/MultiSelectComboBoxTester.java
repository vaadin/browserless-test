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
package com.vaadin.flow.component.combobox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.browserless.internal.BasicUtilsKt;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * @since 1.0
 */
@Tests(fqn = "com.vaadin.flow.component.combobox.MultiSelectComboBox")
public class MultiSelectComboBoxTester<T extends MultiSelectComboBox<Y>, Y>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public MultiSelectComboBoxTester(T component) {
        super(component);
    }

    /**
     * Simulate writing a filter to the combobox.
     * <p/>
     * Use {@link #getSuggestions()} to get the string values show in the
     * dropdown or {@link #getSuggestionItems()} to get the actual items in the
     * suggestion.
     *
     * @param filter
     *            string to use for filtering
     */
    public void setFilter(String filter) {
        ensureComponentIsUsable();
        try {

            final Field dataControllerField = getField(ComboBoxBase.class,
                    "dataController");
            ComboBoxDataController<T> dataController = (ComboBoxDataController) dataControllerField
                    .get(getComponent());
            final Field filterSlot = getField(ComboBoxDataController.class,
                    "filterSlot");
            ((SerializableConsumer<String>) filterSlot.get(dataController))
                    .accept(filter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Select items by client string representation.
     * <p/>
     * Replaces the whole selection with the matching items, and empties it
     * when {@code selection} is {@code null}.
     *
     * @param selection
     *            item representations as strings
     * @throws IllegalStateException
     *             if the component is not usable
     * @deprecated use {@link #selectItems(String...)}, which adds the items
     *             to the current selection as ticking them in the overlay
     *             does. To replace the selection, call
     *             {@link #deselectAll()} first; to empty it, call
     *             {@link #deselectAll()} instead of passing {@code null}.
     */
    @Deprecated
    public void selectItem(String... selection) {
        ensureComponentIsUsable();
        if (selection == null) {
            setValueAsUser(getComponent().getEmptyValue());
            return;
        }
        List<String> toBeSelected = Arrays.asList(selection);
        final List<Y> suggestionItems = getSuggestionItems();
        final ItemLabelGenerator<Y> itemLabelGenerator = getComponent()
                .getItemLabelGenerator();
        final Set<Y> filtered = suggestionItems.stream().filter(
                item -> toBeSelected.contains(itemLabelGenerator.apply(item)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    "No item found for '" + Arrays.toString(selection) + "'");
        }
        setValueAsUser(filtered);
    }

    /**
     * Selects items by client string representation, as ticking them in the
     * overlay does.
     * <p/>
     * The items are added to the current selection; items that are already
     * selected stay selected.
     *
     * @param selection
     *            item representations as strings, not {@code null}
     * @throws IllegalStateException
     *             if the component is not usable
     * @throws IllegalArgumentException
     *             if {@code selection} is {@code null} or contains a label
     *             that matches no item
     */
    public void selectItems(String... selection) {
        ensureComponentIsUsable();
        Set<Y> value = new LinkedHashSet<>(getSelected());
        value.addAll(getItemsForLabels(selection));
        setValueAsUser(value);
    }

    /**
     * Deselects items by client string representation, as unticking them in
     * the overlay does.
     * <p/>
     * Other selected items stay selected.
     *
     * @param selection
     *            item representations as strings, not {@code null}
     * @throws IllegalStateException
     *             if the component is not usable
     * @throws IllegalArgumentException
     *             if {@code selection} is {@code null} or contains a label
     *             that matches no item
     */
    public void deselectItems(String... selection) {
        ensureComponentIsUsable();
        Set<Y> value = new LinkedHashSet<>(getSelected());
        value.removeAll(getItemsForLabels(selection));
        setValueAsUser(value);
    }

    /**
     * Deselects all items, as unticking every selected item in the overlay
     * does.
     * <p/>
     * Unlike {@link #clickClearButton()}, this does not require the clear
     * button to be visible.
     *
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void deselectAll() {
        ensureComponentIsUsable();
        setValueAsUser(getComponent().getEmptyValue());
    }

    private Set<Y> getItemsForLabels(String[] labels) {
        if (labels == null) {
            throw new IllegalArgumentException("Selection must not be null");
        }
        final ItemLabelGenerator<Y> itemLabelGenerator = getComponent()
                .getItemLabelGenerator();
        final List<Y> suggestionItems = getSuggestionItems();
        Set<Y> items = new LinkedHashSet<>();
        for (String label : labels) {
            List<Y> matches = suggestionItems.stream()
                    .filter(item -> itemLabelGenerator.apply(item)
                            .equals(label))
                    .toList();
            if (matches.isEmpty()) {
                throw new IllegalArgumentException(
                        "No item found for '" + label + "'");
            }
            items.addAll(matches);
        }
        return items;
    }

    /**
     * Clears the selection by clicking the clear button, as the user would.
     * <p/>
     * Requires the clear button to be visible — a hidden clear button is not
     * something the user can click. {@link #deselectAll()} clears the
     * selection without that requirement.
     *
     * @throws IllegalStateException
     *             if the component is not usable, or its clear button is not
     *             visible
     * @since 25.3
     */
    public void clickClearButton() {
        clickClearButtonAsUser();
    }

    /**
     * Get the currently selected item.
     *
     * @return current selection
     */
    public Set<Y> getSelected() {
        return getComponent().getValue();
    }

    /**
     * Get dropdown suggestions as String representations sent to the client.
     * Any filter that is set is taken into account.
     *
     * @return List of item representation strings
     */
    public List<String> getSuggestions() {
        final List<Y> suggestionItems = getSuggestionItems();
        final ItemLabelGenerator<Y> itemLabelGenerator = getComponent()
                .getItemLabelGenerator();

        return suggestionItems.stream()
                .map(item -> itemLabelGenerator.apply(item))
                .collect(Collectors.toList());
    }

    /**
     * Get the actual items for the dropdown as a List. Any filter that is set
     * is taken into account.
     *
     * @return List of items
     */
    public List<Y> getSuggestionItems() {
        try {
            final Field dataControllerField = getField(ComboBoxBase.class,
                    "dataController");
            ComboBoxDataController<T> dataController = (ComboBoxDataController) dataControllerField
                    .get(getComponent());
            final Field dataCommunicatorField = getField(
                    ComboBoxDataController.class, "dataCommunicator");
            final DataCommunicator<T> dataCommunicator = (DataCommunicator) dataCommunicatorField
                    .get(dataController);

            final Method fetchFromProvider = getMethod(DataCommunicator.class,
                    "fetchFromProvider", int.class, int.class);
            List<Y> result = ((Stream<Y>) fetchFromProvider.invoke(
                    dataCommunicator, 0, BasicUtilsKt.get_saneFetchLimit()))
                    .collect(Collectors.toList());
            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
