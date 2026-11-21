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
package com.vaadin.browserless.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.vaadin.browserless.internal.BasicUtils;
import com.vaadin.browserless.internal.DepthFirstTreeIterator;
import com.vaadin.browserless.internal.Locator;
import com.vaadin.browserless.internal.PrettyPrintTree;
import com.vaadin.browserless.internal.Renderers;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.AbstractGridMultiSelectionModel;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridNoneSelectionModel;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.grid.GridSingleSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.listbox.ListBoxBase;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;

/**
 * Helpers the Grid testers use to read and drive a {@link Grid} the way a user
 * would.
 * <p>
 * A grid renders its rows on the client, so the rows a test wants to assert on
 * do not exist as server-side objects until something asks the data provider
 * for them. {@link #_get(Grid, int)} and {@link #_size(Grid)} do that asking,
 * and the click and selection helpers fire the events a click or a checkbox
 * would.
 * <p>
 * For internal use only. May be renamed or removed in a future release; use
 * {@code GridTester} and {@code TreeGridTester} instead.
 */
public final class GridUtils {

    private static final Method _DataCommunicator_fetchFromProvider;
    private static final Field _ColumnPathRenderer_provider;
    private static final Method _Column_getInternalId;
    private static final Method _AbstractGridMultiSelectionModel_clientSelectAll;
    private static final Method _AbstractGridMultiSelectionModel_clientDeselectAll;

    static {
        try {
            _DataCommunicator_fetchFromProvider = DataCommunicator.class
                    .getDeclaredMethod("fetchFromProvider", int.class,
                            int.class);
            _DataCommunicator_fetchFromProvider.setAccessible(true);
            _ColumnPathRenderer_provider = ColumnPathRenderer.class
                    .getDeclaredField("provider");
            _ColumnPathRenderer_provider.setAccessible(true);
            _Column_getInternalId = Grid.Column.class
                    .getDeclaredMethod("getInternalId");
            _Column_getInternalId.setAccessible(true);
            _AbstractGridMultiSelectionModel_clientSelectAll = AbstractGridMultiSelectionModel.class
                    .getDeclaredMethod("clientSelectAll");
            _AbstractGridMultiSelectionModel_clientSelectAll
                    .setAccessible(true);
            _AbstractGridMultiSelectionModel_clientDeselectAll = AbstractGridMultiSelectionModel.class
                    .getDeclaredMethod("clientDeselectAll");
            _AbstractGridMultiSelectionModel_clientDeselectAll
                    .setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private GridUtils() {
    }

    /**
     * Returns the item on given row of given grid. Fails if the row index is
     * invalid. The data provider is sorted according to given
     * {@link Grid#getSortOrder()} and filtered according to the current filter.
     * <p>
     * For {@link TreeGrid} this returns the x-th displayed row; skips children
     * of collapsed nodes.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row, 0..size - 1
     * @return the item at given row.
     * @throws AssertionError
     *             if the row index is out of bounds.
     */
    public static <T> T _get(Grid<T> grid, int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException(
                    "rowIndex must be 0 or greater: " + rowIndex);
        }
        if (!(grid instanceof TreeGrid) && isDefinedSize(grid)) {
            // only perform this check for regular Grid. The TreeGrid row
            // stream consults size() internally.
            int size = _size(grid);
            if (rowIndex >= size) {
                throw new AssertionError("Requested to get row " + rowIndex
                        + " but the data provider only has " + size + " rows\n"
                        + dump(grid));
            }
        }
        T result = getOrNull(grid, rowIndex);
        if (result == null) {
            throw new AssertionError("Requested to get row " + rowIndex
                    + " but the data provider returned 0 rows\n" + dump(grid));
        }
        return result;
    }

    private static <T> T getOrNull(Grid<T> grid, int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException(
                    "rowIndex must be 0 or greater: " + rowIndex);
        }
        if (!(grid instanceof TreeGrid) && isDefinedSize(grid)) {
            // only perform this check for regular Grid. The TreeGrid row
            // stream consults size() internally.
            if (rowIndex >= _size(grid)) {
                return null;
            }
        }
        List<T> fetched = fetch(grid, rowIndex, 1);
        return fetched.isEmpty() ? null : fetched.get(0);
    }

    private static boolean isDefinedSize(Grid<?> grid) {
        return grid.getDataCommunicator().isDefinedSize();
    }

    private static <T> List<T> fetch(Grid<T> grid, int offset, int limit) {
        if (grid instanceof TreeGrid<T> treeGrid) {
            return rowSequence(treeGrid).skip(offset).limit(limit)
                    .collect(Collectors.toList());
        }
        return fetch(grid.getDataCommunicator(), offset, limit);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> fetch(DataCommunicator<T> dataCommunicator,
            int offset, int limit) {
        int saneFetchLimit = BasicUtils.get_saneFetchLimit();
        if (limit > saneFetchLimit) {
            throw new IllegalArgumentException(
                    "Vaadin doesn't handle fetching of many items very well unfortunately. The sane limit is "
                            + saneFetchLimit + " but you asked for " + limit);
        }
        // make sure the DataCommunicator is not in paged mode:
        // https://github.com/mvysny/karibu-testing/issues/99
        dataCommunicator.setPagingEnabled(false);
        try {
            Stream<T> fetched = (Stream<T>) _DataCommunicator_fetchFromProvider
                    .invoke(dataCommunicator, offset, limit);
            return fetched.collect(Collectors.toList());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the number of items in given grid. For {@link TreeGrid} this
     * returns the number of displayed rows; children of collapsed nodes are not
     * counted.
     *
     * @param grid
     *            the grid to read
     * @return the number of rows
     */
    public static int _size(Grid<?> grid) {
        if (grid instanceof TreeGrid<?> treeGrid) {
            return (int) rowSequence(treeGrid).count();
        }
        if (!isDefinedSize(grid)) {
            return fetch(grid, 0, BasicUtils.get_saneFetchLimit()).size();
        }
        return grid.getDataCommunicator().getDataProviderSize();
    }

    @SuppressWarnings("unchecked")
    private static <T> Object getPresentationValue(Grid.Column<T> column,
            T rowObject) {
        Renderer<T> renderer = column.getRenderer();
        if (renderer instanceof ColumnPathRenderer) {
            Object value = getValueProvider((ColumnPathRenderer<T>) renderer)
                    .apply(rowObject);
            return String.valueOf(value);
        }
        return Renderers._getPresentationValue(renderer, rowObject);
    }

    private static <T> List<String> getFormattedRow(Grid<T> grid, T rowObject) {
        List<String> result = new ArrayList<>();
        for (Grid.Column<T> column : grid.getColumns()) {
            if (column.isVisible()) {
                result.add(String
                        .valueOf(getPresentationValue(column, rowObject)));
            }
        }
        return result;
    }

    private static <T> String getSortIndicator(Grid<T> grid,
            Grid.Column<T> column) {
        GridSortOrder<T> sortOrder = grid.getSortOrder().stream()
                .filter(it -> it.getSorted() == column).findFirst()
                .orElse(null);
        if (sortOrder == null) {
            return "";
        }
        return sortOrder.getDirection() == SortDirection.ASCENDING ? "v" : "^";
    }

    /**
     * Dumps the first 10 rows of given grid as text, for error messages.
     */
    private static <T> String dump(Grid<T> grid) {
        int maxRows = 10;
        StringBuilder sb = new StringBuilder();
        sb.append(grid.getColumns().stream().filter(Grid.Column::isVisible)
                .map(it -> "[" + it.getHeaderText() + "]"
                        + getSortIndicator(grid, it))
                .collect(Collectors.joining("-", "--", "--\n")));
        if (grid instanceof TreeGrid<T> treeGrid) {
            PrettyPrintTree tree = dataSourceToPrettyTree(treeGrid);
            List<String> allLines = Locator
                    .filterNotBlank(Arrays.asList(tree.print().split("\n")));
            List<String> lines = allLines.subList(Math.min(1, allLines.size()),
                    allLines.size());
            appendRows(sb, lines.size(), maxRows, i -> i + ": " + lines.get(i));
        } else if (isDefinedSize(grid)) {
            appendRows(sb, _size(grid), maxRows, i -> i + ": "
                    + String.join(", ", getFormattedRow(grid, _get(grid, i))));
        } else {
            int rowsOutputted = 0;
            for (int i = 0; i < maxRows; i++) {
                T rowObject = getOrNull(grid, i);
                if (rowObject == null) {
                    break;
                }
                sb.append(i).append(": ").append(
                        String.join(", ", getFormattedRow(grid, rowObject)))
                        .append("\n");
                rowsOutputted++;
            }
            sb.append(rowsOutputted == maxRows ? "--and possibly more\n"
                    : "--\n");
        }
        return sb.toString();
    }

    private static void appendRows(StringBuilder sb, int rowCount, int maxRows,
            Function<Integer, String> row) {
        int displayed = Math.min(rowCount, maxRows);
        for (int i = 0; i < displayed; i++) {
            sb.append(row.apply(i)).append("\n");
        }
        int andMore = rowCount - displayed;
        if (andMore > 0) {
            sb.append("--and ").append(andMore).append(" more\n");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ValueProvider<T, ?> getValueProvider(
            ColumnPathRenderer<T> renderer) {
        try {
            return (ValueProvider<T, ?>) _ColumnPathRenderer_provider
                    .get(renderer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simulates a user clicking on given row of given grid. Selects the item in
     * single selection mode (or deselects it when it is already selected), then
     * fires an {@link ItemClickEvent}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to click
     * @param rowIndex
     *            the row index, 0 or higher.
     * @param button
     *            the id of the pressed mouse button
     * @param ctrlKey
     *            whether the control key was pressed
     * @param shiftKey
     *            whether the shift key was pressed
     * @param altKey
     *            whether the alt key was pressed
     * @param metaKey
     *            whether the meta key was pressed
     * @throws IllegalStateException
     *             if the grid was not editable.
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex, int button,
            boolean ctrlKey, boolean shiftKey, boolean altKey,
            boolean metaKey) {
        BasicUtils.checkEditableByUser(grid);
        // fire SelectionEvent if need be:
        // https://github.com/mvysny/karibu-testing/issues/96
        T item = _get(grid, rowIndex);
        if (grid.getSelectionModel() instanceof GridSingleSelectionModel) {
            T selectedItem = grid.getSelectedItems().stream().findFirst()
                    .orElse(null);
            if (selectedItem == null || !selectedItem.equals(item)) {
                grid.select(item);
            } else {
                // clicking on a selected item will de-select it.
                grid.deselectAll();
            }
        }
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(item);
        ItemClickEvent<T> event = new ItemClickEvent<>(grid, true, itemKey,
                null, -1, -1, -1, -1, 1, button, ctrlKey, shiftKey, altKey,
                metaKey);
        ComponentUtil.fireEvent(grid, event);
    }

    /**
     * Simulates a user double-clicking on given row of given grid by firing an
     * {@link ItemDoubleClickEvent}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to click
     * @param rowIndex
     *            the row index, 0 or higher.
     * @param button
     *            the id of the pressed mouse button
     * @param ctrlKey
     *            whether the control key was pressed
     * @param shiftKey
     *            whether the shift key was pressed
     * @param altKey
     *            whether the alt key was pressed
     * @param metaKey
     *            whether the meta key was pressed
     * @throws IllegalStateException
     *             if the grid was not editable.
     */
    public static <T> void _doubleClickItem(Grid<T> grid, int rowIndex,
            int button, boolean ctrlKey, boolean shiftKey, boolean altKey,
            boolean metaKey) {
        BasicUtils.checkEditableByUser(grid);
        String itemKey = grid.getDataCommunicator().getKeyMapper()
                .key(_get(grid, rowIndex));
        ItemDoubleClickEvent<T> event = new ItemDoubleClickEvent<>(grid, true,
                itemKey, null, -1, -1, -1, -1, 2, button, ctrlKey, shiftKey,
                altKey, metaKey);
        ComponentUtil.fireEvent(grid, event);
    }

    /**
     * Returns a stream which walks over all displayed rows of given tree grid:
     * the children of collapsed nodes are skipped.
     */
    private static <T> Stream<T> rowSequence(TreeGrid<T> treeGrid) {
        return rowSequence(treeGrid.getDataProvider(), treeGrid::isExpanded);
    }

    private static <T, F> Stream<T> rowSequence(
            HierarchicalDataProvider<T, F> dataProvider,
            Predicate<T> isExpanded) {
        Function<T, List<T>> getChildrenOf = item -> item == null
                || isExpanded.test(item)
                        ? checkedFetch(dataProvider,
                                new HierarchicalQuery<>(null, item))
                        : Collections.emptyList();
        return getChildrenOf.apply(null).stream()
                .flatMap(root -> StreamSupport.stream(
                        ((Iterable<T>) () -> new DepthFirstTreeIterator<>(root,
                                getChildrenOf)).spliterator(),
                        false));
    }

    private static <T, F> int checkedSize(
            HierarchicalDataProvider<T, F> dataProvider,
            HierarchicalQuery<T, F> query) {
        if (query.getParent() != null
                && !dataProvider.hasChildren(query.getParent())) {
            return 0;
        }
        int result = dataProvider.size(query);
        if (result < 0) {
            throw new IllegalStateException(
                    "size(" + query + ") returned negative count: " + result);
        }
        return result;
    }

    private static <T, F> List<T> checkedFetch(
            HierarchicalDataProvider<T, F> dataProvider,
            HierarchicalQuery<T, F> query) {
        if (checkedSize(dataProvider, query) == 0) {
            return Collections.emptyList();
        }
        return dataProvider.fetchChildren(query).collect(Collectors.toList());
    }

    private static <T> PrettyPrintTree dataSourceToPrettyTree(
            TreeGrid<T> treeGrid) {
        Function<T, List<T>> getChildrenOf = item -> item == null
                || treeGrid.isExpanded(item)
                        ? checkedFetch(treeGrid.getDataProvider(),
                                new HierarchicalQuery<>(null, item))
                        : Collections.emptyList();
        return new PrettyPrintTree("TreeGrid", toPrettyTrees(treeGrid,
                getChildrenOf.apply(null), getChildrenOf));
    }

    private static <T> List<PrettyPrintTree> toPrettyTrees(TreeGrid<T> treeGrid,
            List<T> items, Function<T, List<T>> getChildrenOf) {
        List<PrettyPrintTree> result = new ArrayList<>();
        for (T item : items) {
            String self = String.join(", ", getFormattedRow(treeGrid, item))
                    + "\n";
            result.add(new PrettyPrintTree(self, toPrettyTrees(treeGrid,
                    getChildrenOf.apply(item), getChildrenOf)));
        }
        return result;
    }

    /**
     * Returns the data provider currently set to given component, or null if
     * the component has no data provider.
     *
     * @param component
     *            the component to read
     * @return the data provider, or null
     */
    public static DataProvider<?, ?> getDataProvider(Component component) {
        // until https://github.com/vaadin/flow/issues/6296 is resolved
        if (component instanceof Grid<?> grid) {
            return grid.getDataProvider();
        } else if (component instanceof Select<?> select) {
            return select.getDataProvider();
        } else if (component instanceof ListBoxBase<?, ?, ?> listBox) {
            return listBox.getDataProvider();
        } else if (component instanceof RadioButtonGroup<?> radioButtonGroup) {
            return radioButtonGroup.getDataProvider();
        } else if (component instanceof CheckboxGroup<?> checkboxGroup) {
            return checkboxGroup.getDataProvider();
        } else if (component instanceof ComboBox<?> comboBox) {
            return comboBox.getDataProvider();
        }
        return null;
    }

    private static <T> GridMultiSelectionModel<T> getSelectAllCheckboxModel(
            Grid<T> grid, String action) {
        GridSelectionModel<T> model = grid.getSelectionModel();
        if (!(model instanceof GridMultiSelectionModel<T> multiSelectionModel)) {
            throw new IllegalStateException(
                    action + " requires multi selection mode");
        }
        if (!multiSelectionModel.isSelectAllCheckboxVisible()) {
            throw new IllegalStateException(
                    action + " requires the select all checkbox to be visible");
        }
        return multiSelectionModel;
    }

    /**
     * Simulates a user selecting given item in given grid. In single selection
     * mode the previously selected item is deselected first.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to select in
     * @param item
     *            the item to select
     * @throws IllegalStateException
     *             if the grid was not editable.
     */
    public static <T> void _select(Grid<T> grid, T item) {
        BasicUtils.checkEditableByUser(grid);
        if (grid.getSelectionModel() instanceof GridSingleSelectionModel) {
            grid.deselectAll();
        }
        // fails properly if the Grid doesn't support selection.
        grid.getSelectionModel().selectFromClient(item);
    }

    /**
     * Simulates a user clicking the select all checkbox of given grid.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to select in
     * @throws IllegalStateException
     *             if the grid was not editable, is not in multi selection mode
     *             or the select all checkbox is not visible.
     */
    public static <T> void _selectAll(Grid<T> grid) {
        BasicUtils.checkEditableByUser(grid);
        invoke(_AbstractGridMultiSelectionModel_clientSelectAll,
                getSelectAllCheckboxModel(grid, "Select all"));
    }

    /**
     * Simulates a user deselecting given item in given grid.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to deselect in
     * @param item
     *            the item to deselect
     * @throws IllegalStateException
     *             if the grid was not editable or the item is not selected.
     */
    public static <T> void _deselect(Grid<T> grid, T item) {
        BasicUtils.checkEditableByUser(grid);
        if (!(grid.getSelectionModel() instanceof GridNoneSelectionModel)
                && !grid.getSelectionModel().isSelected(item)) {
            throw new IllegalStateException(
                    "Can not deselect " + item + ": the row is not selected");
        }
        // fails properly if the Grid doesn't support selection.
        grid.getSelectionModel().deselectFromClient(item);
    }

    /**
     * Simulates a user unchecking the select all checkbox of given grid.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to deselect in
     * @throws IllegalStateException
     *             if the grid was not editable, is not in multi selection mode
     *             or the select all checkbox is not visible.
     */
    public static <T> void _deselectAll(Grid<T> grid) {
        BasicUtils.checkEditableByUser(grid);
        invoke(_AbstractGridMultiSelectionModel_clientDeselectAll,
                getSelectAllCheckboxModel(grid, "Deselect all"));
    }

    /**
     * Returns the internal id of given column, which the client uses to refer
     * to the column.
     *
     * @param column
     *            the column to read
     * @return the column's internal id
     */
    public static String getInternalId(Grid.Column<?> column) {
        return (String) invoke(_Column_getInternalId, column);
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
