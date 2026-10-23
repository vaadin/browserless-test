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
     * Simulate writing a filter to the combobox. As typing does in the browser,
     * the filter is applied to the data provider and to the component's
     * {@code filter} property.
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
            setPropertyAsUser("filter", filter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Select items by client string representation.
     *
     * @param selection
     *            item representations as strings
     * @throws IllegalStateException
     *             if the component is not usable
     */
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
     * Selects the item matching a partial text, as if the user typed the text
     * into the combobox and picked the only remaining suggestion.
     * <p>
     * The text is applied as a filter, so matching follows the component's own
     * filtering: by default a case-insensitive "label contains" match, or the
     * {@code ItemFilter} / data provider filter the application configured.
     * Exactly one item has to match; when several do, the one whose label
     * equals the text (ignoring case) is picked, as the browser commits an
     * exact label match on Enter. Use {@link #selectItemsContaining(String...)}
     * to pick every item a text matches.
     * <p>
     * As in the browser, the item is added to the current selection. Unlike the
     * browser, where picking an already selected item deselects it, an already
     * selected item stays selected. The filter is cleared afterwards, as the
     * browser does once a value is committed. When nothing is selected, the
     * filter that was set before is restored.
     *
     * <pre>
     * // items "Berlin", "Bern", "Bernau", "Helsinki"
     * comboBoxTester.selectItemContaining("hel"); // adds "Helsinki"
     * comboBoxTester.selectItemContaining("bern"); // adds "Bern"
     * </pre>
     *
     * @param text
     *            the text the user types to find the item, not {@code null}
     * @throws IllegalArgumentException
     *             if the text is {@code null}, matches no item, or matches more
     *             than one item without a single exact label match
     * @throws IllegalStateException
     *             if the component is not usable
     * @see #selectItem(String...)
     */
    public void selectItemContaining(String text) {
        ensureComponentIsUsable();
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        final String previousFilter = getFilter();
        final Y item;
        try {
            setFilter(text);
            item = getSingleSuggestionMatching(text);
        } catch (RuntimeException e) {
            setFilter(previousFilter);
            throw e;
        }
        setFilter("");
        final Set<Y> selection = new LinkedHashSet<>(getComponent().getValue());
        selection.add(item);
        setValueAsUser(selection);
    }

    /**
     * Selects every item matching partial texts, as if the user typed each text
     * into the combobox and ticked all remaining suggestions.
     * <p>
     * Each text is applied as a filter, so matching follows the component's own
     * filtering: by default a case-insensitive "label contains" match, or the
     * {@code ItemFilter} / data provider filter the application configured.
     * Every text has to match at least one item. Use
     * {@link #selectItemContaining(String)} to pick exactly one item.
     * <p>
     * As in the browser, the matched items are added to the current selection.
     * Unlike the browser, where picking an already selected item deselects it,
     * already selected matches stay selected, so an item matched by more than
     * one text is selected once. The filter is cleared afterwards, as the
     * browser does once a value is committed. When nothing is selected, the
     * filter that was set before is restored.
     *
     * <pre>
     * // items "Berlin", "Bern", "Helsinki"
     * comboBoxTester.selectItemsContaining("ber"); // adds Berlin, Bern
     * comboBoxTester.selectItemsContaining("hel", "lin"); // adds Helsinki,
     *                                                     // Berlin
     * </pre>
     *
     * @param texts
     *            the texts the user types to find the items, at least one, none
     *            of them {@code null}
     * @throws IllegalArgumentException
     *             if no text is given, a text is {@code null}, or a text
     *             matches no item
     * @throws IllegalStateException
     *             if the component is not usable
     * @see #selectItem(String...)
     */
    public void selectItemsContaining(String... texts) {
        ensureComponentIsUsable();
        if (texts == null || texts.length == 0) {
            throw new IllegalArgumentException(
                    "At least one text has to be given");
        }
        if (Arrays.asList(texts).contains(null)) {
            throw new IllegalArgumentException("texts must not contain null");
        }
        final String previousFilter = getFilter();
        final Set<Y> selection = new LinkedHashSet<>(getComponent().getValue());
        try {
            for (String text : texts) {
                setFilter(text);
                final List<Y> matches = getSuggestionItems();
                if (matches.isEmpty()) {
                    throw new IllegalArgumentException(
                            "No item found containing '" + text + "'");
                }
                selection.addAll(matches);
            }
        } catch (RuntimeException e) {
            setFilter(previousFilter);
            throw e;
        }
        setFilter("");
        setValueAsUser(selection);
    }

    private Y getSingleSuggestionMatching(String text) {
        final List<Y> matches = getSuggestionItems();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        final ItemLabelGenerator<Y> itemLabelGenerator = getComponent()
                .getItemLabelGenerator();
        final List<Y> exactMatches = matches.stream().filter(
                item -> text.equalsIgnoreCase(itemLabelGenerator.apply(item)))
                .toList();
        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }
        throw new IllegalArgumentException(matches.isEmpty()
                ? "No item found containing '" + text + "'"
                : "Multiple items found containing '" + text + "': "
                        + matches.stream().map(itemLabelGenerator).toList());
    }

    private String getFilter() {
        final String filter = getComponent().getFilter();
        return filter == null ? "" : filter;
    }

    /**
     * Clears the selection by clicking the clear button, as the user would.
     * <p/>
     * Requires the clear button to be visible — a hidden clear button is not
     * something the user can click. {@link #selectItem(String...)} with
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
