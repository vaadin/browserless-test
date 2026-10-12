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
import java.util.List;
import java.util.Objects;
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
@Tests(fqn = "com.vaadin.flow.component.combobox.ComboBox")
public class ComboBoxTester<T extends ComboBox<Y>, Y>
        extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public ComboBoxTester(T component) {
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
     * Selects an item by its string representation (label).
     * <p>
     * This method searches for the item among the current suggestions, which
     * are affected by any filter set via {@link #setFilter(String)}. If no
     * filter is set, all items are available for selection. If a filter is set,
     * only the filtered items can be selected.
     * <p>
     * To select an item when the ComboBox uses custom filtering logic (e.g.,
     * set via {@code setItems(ItemFilter, Collection)}), first call
     * {@link #setFilter(String)} to apply the filter, then call this method to
     * select from the filtered results.
     * <p>
     * Example usage with filtering:
     *
     * <pre>
     * // Apply filter first
     * comboBoxTester.setFilter("search text");
     * // Then select from filtered items
     * comboBoxTester.selectItem("Matching Item Label");
     * </pre>
     *
     * @param selection
     *            the item's string representation (label) to select, or
     *            {@code null} to clear the selection
     * @throws IllegalArgumentException
     *             if no item with the given label is found among current
     *             suggestions
     * @throws IllegalStateException
     *             if the component is not usable
     * @see #setFilter(String)
     * @see #getSuggestions()
     * @see #getSuggestionItems()
     */
    public void selectItem(String selection) {
        ensureComponentIsUsable();
        if (selection == null) {
            setValueAsUser(null);
            return;
        }
        final List<Y> suggestionItems = getSuggestionItems();
        final ItemLabelGenerator<Y> itemLabelGenerator = getComponent()
                .getItemLabelGenerator();
        final List<Y> filtered = suggestionItems.stream().filter(
                item -> selection.equals(itemLabelGenerator.apply(item)))
                .collect(Collectors.toList());
        if (filtered.size() != 1) {
            throw new IllegalArgumentException(
                    "No item found for '" + selection + "'");
        }
        setValueAsUser(filtered.get(0));
    }

    /**
     * Selects the item matching a partial text, as if the user typed the text
     * into the combobox and picked the only remaining suggestion.
     * <p>
     * The text is applied as a filter, so matching follows the component's own
     * filtering: by default a case-insensitive "label contains" match, or the
     * {@code ItemFilter} / data provider filter the application configured.
     * Exactly one item has to match. The filter is cleared afterwards, as the
     * browser does once a value is committed.
     *
     * <pre>
     * // items "Berlin", "Bern", "Helsinki"
     * comboBoxTester.selectItemContaining("hel"); // selects "Helsinki"
     * </pre>
     *
     * @param text
     *            the text the user types to find the item, not {@code null}
     * @throws IllegalArgumentException
     *             if no item or more than one item matches the text
     * @throws IllegalStateException
     *             if the component is not usable
     * @see #selectItem(String)
     */
    public void selectItemContaining(String text) {
        ensureComponentIsUsable();
        Objects.requireNonNull(text, "text must not be null");
        final List<Y> matches;
        try {
            setFilter(text);
            matches = getSuggestionItems();
        } finally {
            setFilter("");
        }
        if (matches.size() != 1) {
            final ItemLabelGenerator<Y> itemLabelGenerator = getComponent()
                    .getItemLabelGenerator();
            throw new IllegalArgumentException(matches.isEmpty()
                    ? "No item found containing '" + text + "'"
                    : "Multiple items found containing '" + text + "': "
                            + matches.stream().map(itemLabelGenerator)
                                    .collect(Collectors.toList()));
        }
        setValueAsUser(matches.get(0));
    }

    /**
     * Clears the selection by clicking the clear button, as the user would.
     * <p/>
     * Requires the clear button to be visible — a hidden clear button is not
     * something the user can click. {@link #selectItem(String)} with
     * {@code null} clears the selection without that requirement.
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
    public Y getSelected() {
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
