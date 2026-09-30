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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.vaadin.browserless.internal.BasicUtils;
import com.vaadin.browserless.internal.DepthFirstTreeIterator;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.PrettyPrintTree;
import com.vaadin.browserless.internal.Renderers;
import com.vaadin.browserless.internal.Utils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.AbstractGridMultiSelectionModel;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridNoneSelectionModel;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.grid.GridSingleSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.listbox.ListBoxBase;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

/**
 * Helpers for reading and driving a {@link Grid} the way a user would.
 * <p>
 * A grid renders its rows on the client, so the rows a test wants to assert on
 * do not exist as server-side objects until something asks the data provider
 * for them. These helpers do that asking: {@link #_get(Grid, int)} and
 * {@link #_findAll(Grid)} fetch items, {@link #_getFormattedRow(Grid, int)} and
 * {@link #_dump(Grid)} render what a cell actually shows, and
 * {@link #_clickItem(Grid, int)} and the selection helpers fire the events a
 * click or a checkbox would.
 * <p>
 * Most of this is reachable through {@code GridTester}, which is the friendlier
 * entry point; these statics are what it delegates to.
 */
public final class GridUtils {

    private GridUtils() {
    }

    // ---------------------------------------------------------------------
    // Reflection lookups (cached)
    // ---------------------------------------------------------------------

    private static final Method _DataCommunicator_isDefinedSize;
    private static final Method _DataCommunicator_fetchFromProvider;
    private static final Method _DataCommunicator_setPagingEnabled;
    private static final Method _DataCommunicator_getDataProviderSize;
    private static final Method _AbstractCell_getColumn;
    private static final Method _AbstractColumn_getBottomLevelColumn;
    private static final Method _Column_getInternalId;
    private static final Field _ColumnPathRenderer_provider;
    private static final Class<?> _AbstractColumn_class;

    private static final Method _ListBoxBase_getDataProvider;
    private static final Method _CheckboxGroup_getDataProvider;
    private static final Method _RadioButtonGroup_getDataProvider;

    static {
        try {
            _DataCommunicator_isDefinedSize = DataCommunicator.class
                    .getDeclaredMethod("isDefinedSize");

            _DataCommunicator_fetchFromProvider = DataCommunicator.class
                    .getDeclaredMethod("fetchFromProvider", int.class,
                            int.class);
            _DataCommunicator_fetchFromProvider.setAccessible(true);

            Method setPagingEnabled = null;
            for (Method m : DataCommunicator.class.getDeclaredMethods()) {
                if (m.getName().equals("setPagingEnabled")) {
                    setPagingEnabled = m;
                    break;
                }
            }
            _DataCommunicator_setPagingEnabled = setPagingEnabled;

            _DataCommunicator_getDataProviderSize = DataCommunicator.class
                    .getDeclaredMethod("getDataProviderSize");
            _DataCommunicator_getDataProviderSize.setAccessible(true);

            Class<?> abstractCellClass = Utils.findClassOrThrow(
                    "com.vaadin.flow.component.grid.AbstractRow$AbstractCell");
            _AbstractCell_getColumn = abstractCellClass
                    .getDeclaredMethod("getColumn");
            _AbstractCell_getColumn.setAccessible(true);

            _AbstractColumn_class = Utils.findClassOrThrow(
                    "com.vaadin.flow.component.grid.AbstractColumn");
            _AbstractColumn_getBottomLevelColumn = _AbstractColumn_class
                    .getDeclaredMethod("getBottomLevelColumn");
            _AbstractColumn_getBottomLevelColumn.setAccessible(true);

            _Column_getInternalId = Grid.Column.class
                    .getDeclaredMethod("getInternalId");
            _Column_getInternalId.setAccessible(true);

            _ColumnPathRenderer_provider = Utils
                    .findClassOrThrow(
                            "com.vaadin.flow.component.grid.ColumnPathRenderer")
                    .getDeclaredField("provider");
            _ColumnPathRenderer_provider.setAccessible(true);

            _ListBoxBase_getDataProvider = ListBoxBase.class
                    .getDeclaredMethod("getDataProvider");
            _ListBoxBase_getDataProvider.setAccessible(true);
            _CheckboxGroup_getDataProvider = CheckboxGroup.class
                    .getDeclaredMethod("getDataProvider");
            _CheckboxGroup_getDataProvider.setAccessible(true);
            _RadioButtonGroup_getDataProvider = RadioButtonGroup.class
                    .getDeclaredMethod("getDataProvider");
            _RadioButtonGroup_getDataProvider.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException
                | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ---------------------------------------------------------------------
    // DataProvider helpers
    // ---------------------------------------------------------------------

    /**
     * Returns the item on given row. Fails if the row index is invalid. The
     * data provider is sorted according to given {@code sortOrders} (empty by
     * default) and filtered according to given {@code filter} (null by default)
     * first.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param rowIndex
     *            the row, 0..size - 1
     * @param sortOrders
     *            the sort orders to pass to the query
     * @param inMemorySorting
     *            the comparator applied in memory, or null for none
     * @param filter
     *            the filter to pass to the query, or null for none
     * @return the item at given row.
     * @throws AssertionError
     *             if the row index is out of bounds.
     */
    public static <T, F> T _get(DataProvider<T, F> provider, int rowIndex,
            List<QuerySortOrder> sortOrders, Comparator<T> inMemorySorting,
            F filter) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException(
                    "rowIndex must be 0 or greater: " + rowIndex);
        }
        Stream<T> fetched = provider.fetch(
                new Query<>(rowIndex, 1, sortOrders, inMemorySorting, filter));
        List<T> list = fetched.collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new AssertionError("Requested to get row " + rowIndex
                    + " but the data provider only has "
                    + _size(provider, filter) + " rows matching filter "
                    + filter);
        }
        return list.get(0);
    }

    /**
     * Equivalent to
     * {@code _get(provider, rowIndex, Collections.emptyList(), null, null)}.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param rowIndex
     *            the row index, 0-based
     * @return the item at the given row
     */
    public static <T, F> T _get(DataProvider<T, F> provider, int rowIndex) {
        return _get(provider, rowIndex, Collections.emptyList(), null, null);
    }

    /**
     * Returns all items in given data provider, sorted according to given
     * {@code sortOrders} (empty by default) and filtered according to given
     * {@code filter} (null by default).
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param sortOrders
     *            the sort orders to pass to the query
     * @param inMemorySorting
     *            the comparator applied in memory, or null for none
     * @param filter
     *            the filter to pass to the query, or null for none
     * @return the list of items.
     */
    public static <T, F> List<T> _findAll(DataProvider<T, F> provider,
            List<QuerySortOrder> sortOrders, Comparator<T> inMemorySorting,
            F filter) {
        Stream<T> fetched = provider.fetch(new Query<>(0, Integer.MAX_VALUE,
                sortOrders, inMemorySorting, filter));
        return fetched.collect(Collectors.toList());
    }

    /**
     * Equivalent to
     * {@code _findAll(provider, Collections.emptyList(), null, null)}.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @return all items the data provider returns
     */
    public static <T, F> List<T> _findAll(DataProvider<T, F> provider) {
        return _findAll(provider, Collections.emptyList(), null, null);
    }

    /**
     * Returns the item on given row. Fails if the row index is invalid. Uses
     * current Grid sorting.
     *
     * For {@link TreeGrid} this returns the x-th displayed row; skips children
     * of collapsed nodes. Uses {@code _rowSequence}.
     *
     * WARNING: Very slow operation for {@link TreeGrid}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row, 0..size - 1
     * @return the item at given row, not null.
     */
    public static <T> T _get(Grid<T> grid, int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException(
                    "rowIndex must be 0 or greater: " + rowIndex);
        }
        if (!(grid instanceof TreeGrid) && _dataProviderSupportsSizeOp(grid)) {
            // only perform this check for regular Grid. TreeGrid._fetch()'s
            // Sequence consults size() internally.
            int size = _size(grid);
            if (rowIndex >= size) {
                throw new AssertionError("Requested to get row " + rowIndex
                        + " but the data provider only has " + size + " rows\n"
                        + _dump(grid));
            }
        }
        T result = _getOrNull(grid, rowIndex);
        if (result == null) {
            throw new AssertionError("Requested to get row " + rowIndex
                    + " but the data provider returned 0 rows\n" + _dump(grid));
        }
        return result;
    }

    /**
     * Returns the item on given row, or null if the {@code rowIndex} is larger
     * than the number of items the data provider can provide. Uses current Grid
     * sorting.
     *
     * For {@link TreeGrid} this returns the x-th displayed row; skips children
     * of collapsed nodes. Uses {@code _rowSequence}.
     *
     * WARNING: Very slow operation for {@link TreeGrid}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row, 0 or larger.
     * @return the item at given row or null if the data provider provides less
     *         rows.
     */
    public static <T> T _getOrNull(Grid<T> grid, int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException(
                    "rowIndex must be 0 or greater: " + rowIndex);
        }
        if (!(grid instanceof TreeGrid) && _dataProviderSupportsSizeOp(grid)) {
            // only perform this check for regular Grid. TreeGrid._fetch()'s
            // Sequence consults size() internally.
            int size = _size(grid);
            if (rowIndex >= size) {
                return null;
            }
        }
        List<T> fetched = _fetch(grid, rowIndex, 1);
        return fetched.isEmpty() ? null : fetched.get(0);
    }

    /**
     * Vaadin 19+ Grids support setting data providers which do not support
     * retrieving the number of available rows. See `FetchCallback` for more
     * details.
     *
     * @param grid
     *            the grid to read
     * @return true if the current data provider supports {@code _size}
     *         retrieval, false if not. Returns true for Vaadin 14.
     */
    public static boolean _dataProviderSupportsSizeOp(Grid<?> grid) {
        try {
            return (Boolean) _DataCommunicator_isDefinedSize
                    .invoke(grid.getDataCommunicator());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns items in given range from Grid's data provider. Uses current Grid
     * sorting.
     *
     * For {@link TreeGrid} this walks the {@code _rowSequence}.
     *
     * The Grid never sets any filters into the data provider, however any
     * ConfigurableFilterDataProvider will automatically apply its filters.
     *
     * WARNING: Very slow operation for {@link TreeGrid}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param offset
     *            the index of the first item to fetch
     * @param limit
     *            the maximum number of items to fetch
     * @return items in given range from Grid's data provider
     */
    public static <T> List<T> _fetch(Grid<T> grid, int offset, int limit) {
        if (grid instanceof TreeGrid) {
            @SuppressWarnings("unchecked")
            TreeGrid<T> treeGrid = (TreeGrid<T>) grid;
            return _rowSequence(treeGrid).skip(offset).limit(limit)
                    .collect(Collectors.toList());
        }
        return fetch(grid.getDataCommunicator(), offset, limit);
    }

    /**
     * Returns items in given range from this data communicator. Uses current
     * Grid sorting. Any ConfigurableFilterDataProvider will automatically apply
     * its filters.
     *
     * This is an internal stuff, most probably you wish to call {@code _fetch}.
     *
     * @param <T>
     *            the item type
     * @param dc
     *            the data communicator to read
     * @param offset
     *            the index of the first item to fetch
     * @param limit
     *            the maximum number of items to fetch
     * @return items in given range from this data communicator
     */
    public static <T> List<T> fetch(DataCommunicator<T> dc, int offset,
            int limit) {
        if (limit > BasicUtils._saneFetchLimit()) {
            throw new IllegalArgumentException(
                    "Vaadin doesn't handle fetching of many items very well unfortunately. The sane limit is "
                            + BasicUtils._saneFetchLimit()
                            + " but you asked for " + limit);
        }
        try {
            // make sure the DataCommunicator is not in paged mode:
            // https://github.com/mvysny/karibu-testing/issues/99
            if (_DataCommunicator_setPagingEnabled != null) {
                _DataCommunicator_setPagingEnabled.invoke(dc, false);
            }
            @SuppressWarnings("unchecked")
            Stream<T> fetched = (Stream<T>) _DataCommunicator_fetchFromProvider
                    .invoke(dc, offset, limit);
            return fetched.collect(Collectors.toList());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns all items from this data communicator. Uses current Grid sorting.
     * Any ConfigurableFilterDataProvider will automatically apply its filters.
     *
     * This is an internal stuff, most probably you wish to call {@code _fetch}.
     *
     * @param <T>
     *            the item type
     * @param dc
     *            the data communicator to read
     * @return all items from this data communicator
     */
    public static <T> List<T> fetchAll(DataCommunicator<T> dc) {
        return fetch(dc, 0, BasicUtils._saneFetchLimit());
    }

    /**
     * Returns all items in given data provider. Uses current Grid sorting.
     *
     * For {@link TreeGrid} this returns all displayed rows; skips children of
     * collapsed nodes.
     *
     * The Grid never sets any filters into the data provider, however any
     * ConfigurableFilterDataProvider will automatically apply its filters.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @return the list of items.
     */
    public static <T> List<T> _findAll(Grid<T> grid) {
        return _fetch(grid, 0, BasicUtils._saneFetchLimit());
    }

    /**
     * Returns the number of items in this data provider.
     *
     * In case of {@link HierarchicalDataProvider} this returns the number of
     * ALL items including all leafs.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param filter
     *            the filter to pass to the query, or null for none
     * @return the number of items in this data provider
     */
    public static <T, F> int _size(DataProvider<T, F> provider, F filter) {
        if (provider instanceof HierarchicalDataProvider) {
            @SuppressWarnings("unchecked")
            HierarchicalDataProvider<T, F> hp = (HierarchicalDataProvider<T, F>) provider;
            return _size(hp, null, filter);
        }
        return provider.size(new Query<>(filter));
    }

    /**
     * Equivalent to {@code _size(provider, null)}.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @return the number of items in this data provider
     */
    public static <T, F> int _size(DataProvider<T, F> provider) {
        return _size(provider, null);
    }

    /**
     * Returns the number of items in this data provider, including child items.
     * The function traverses recursively until all children are found; then a
     * total size is returned. The function uses
     * {@link HierarchicalDataProvider#size(com.vaadin.flow.data.provider.Query)}
     * mostly, but also uses
     * {@link HierarchicalDataProvider#fetchChildren(HierarchicalQuery)} to
     * discover children. Only children matching {@code filter} are considered
     * for recursive computation of the size.
     *
     * Note that this can differ to `Grid._size()` since `Grid._size()` ignores
     * children of collapsed tree nodes.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param root
     *            start with this item; defaults to null to iterate all items
     * @param filter
     *            filter to pass to {@link HierarchicalQuery}
     * @return the number of items in this data provider, including child items
     */
    public static <T, F> int _size(HierarchicalDataProvider<T, F> provider,
            T root, F filter) {
        return (int) _rowSequence(provider, root, item -> true, filter).count();
    }

    /**
     * Equivalent to {@code _size(provider, null, null)}.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @return the number of items in this data provider, including child items
     */
    public static <T, F> int _size(HierarchicalDataProvider<T, F> provider) {
        return _size(provider, null, null);
    }

    /**
     * Equivalent to {@code _size(provider, root, null)}.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param root
     *            start with this item; null iterates all items
     * @return the number of items below the given root, including child items
     */
    public static <T, F> int _size(HierarchicalDataProvider<T, F> provider,
            T root) {
        return _size(provider, root, null);
    }

    /**
     * Returns the number of items in this Grid.
     *
     * For {@link TreeGrid} this computes the number of items the
     * {@link TreeGrid} is actually showing on-screen, ignoring children of
     * collapsed nodes.
     *
     * A very slow operation for {@link TreeGrid} since it walks through all
     * items returned by {@code _rowSequence}.
     *
     * If {@code _dataProviderSupportsSizeOp} is false, this function will fetch
     * all the data and count the result returned, which is also very slow.
     *
     * @param grid
     *            the grid to read
     * @return the number of items in this Grid
     */
    public static int _size(Grid<?> grid) {
        if (grid instanceof TreeGrid) {
            return _size((TreeGrid<?>) grid);
        }
        if (!_dataProviderSupportsSizeOp(grid)) {
            return _findAll(grid).size();
        }
        try {
            return (Integer) _DataCommunicator_getDataProviderSize
                    .invoke(grid.getDataCommunicator());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a {@link Grid.Column} of this grid by its {@code columnKey}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param columnKey
     *            the column key
     * @return a {@link Grid.Column} of this grid by its {@code columnKey}
     * @throws AssertionError
     *             if no such column exists.
     */
    public static <T> Grid.Column<T> _getColumnByKey(Grid<T> grid,
            String columnKey) {
        Grid.Column<T> col = grid.getColumnByKey(columnKey);
        if (col == null) {
            List<String> keys = grid.getColumns().stream()
                    .map(Grid.Column::getKey).filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            throw new AssertionError(PrettyPrintTree.toPrettyString(grid)
                    + ": No such column with key '" + columnKey
                    + "'; available columns: " + keys);
        }
        return col;
    }

    /**
     * Retrieves a component produced by {@link ComponentRenderer} in given
     * {@link Grid} cell. Fails if the renderer is not a
     * {@link ComponentRenderer}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0 or higher.
     * @param columnKey
     *            the column key {@link Grid.Column#getKey()}
     * @return a component produced by {@link ComponentRenderer} in given
     *         {@link Grid} cell
     * @throws IllegalStateException
     *             if the renderer is not {@link ComponentRenderer}.
     */
    public static <T> Component _getCellComponent(Grid<T> grid, int rowIndex,
            String columnKey) {
        Grid.Column<T> column = _getColumnByKey(grid, columnKey);
        Renderer<T> renderer = column.getRenderer();
        if (!(renderer instanceof ComponentRenderer)) {
            throw new IllegalArgumentException(
                    PrettyPrintTree.toPrettyString(grid) + " column "
                            + columnKey + " uses renderer " + renderer
                            + " but we expect a ComponentRenderer here");
        }
        if (renderer instanceof NativeButtonRenderer) {
            throw new IllegalArgumentException(PrettyPrintTree
                    .toPrettyString(grid) + " column " + columnKey
                    + " uses NativeButtonRenderer which is not supported by this function");
        }
        T item = _get(grid, rowIndex);
        @SuppressWarnings("unchecked")
        ComponentRenderer<?, T> cr = (ComponentRenderer<?, T>) renderer;
        return cr.createComponent(item);
    }

    /**
     * Returns the formatted value of given column as a String. Uses
     * {@code getPresentationValue} and converts the result to string (even if
     * the result is a {@link Component}).
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0 or higher.
     * @param columnKey
     *            the column ID.
     * @return the formatted value of given column as a String
     */
    public static <T> String _getFormatted(Grid<T> grid, int rowIndex,
            String columnKey) {
        T rowObject = _get(grid, rowIndex);
        Grid.Column<T> column = _getColumnByKey(grid, columnKey);
        return _getFormatted(column, rowObject);
    }

    /**
     * Returns the formatted value as a String. Uses
     * {@code getPresentationValue} and converts the result to string (even if
     * the result is a {@link Component}).
     *
     * @param <T>
     *            the item type
     * @param column
     *            the column to read
     * @param rowObject
     *            the bean
     * @return the formatted value as a String
     */
    public static <T> String _getFormatted(Grid.Column<T> column, T rowObject) {
        Object value = getPresentationValue(column, rowObject);
        return String.valueOf(value);
    }

    /**
     * Returns the formatted row as a list of Strings, one for every visible
     * column. Uses {@code _getFormatted}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowObject
     *            the bean
     * @return the formatted row as a list of Strings, one for every visible
     *         column
     */
    public static <T> List<String> _getFormattedRow(Grid<T> grid, T rowObject) {
        List<String> result = new ArrayList<>();
        for (Grid.Column<T> c : grid.getColumns()) {
            if (c.isVisible()) {
                result.add(_getFormatted(c, rowObject));
            }
        }
        return result;
    }

    /**
     * Returns the formatted row as a list of Strings, one for every visible
     * column. Uses {@code _getFormatted}. Fails if the {@code rowIndex} is not
     * within the limits.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the index of the row, 0..size-1.
     * @return the formatted row as a list of Strings, one for every visible
     *         column
     */
    public static <T> List<String> _getFormattedRow(Grid<T> grid,
            int rowIndex) {
        T rowObject = _get(grid, rowIndex);
        return _getFormattedRow(grid, rowObject);
    }

    /**
     * Returns the formatted row as a list of Strings, one for every visible
     * column. Uses {@code _getFormatted}. Returns null if the {@code rowIndex}
     * is not within the limits.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the index of the row, 0-based.
     * @return the formatted row as a list of Strings, one for every visible
     *         column
     */
    public static <T> List<String> _getFormattedRowOrNull(Grid<T> grid,
            int rowIndex) {
        T rowObject = _getOrNull(grid, rowIndex);
        if (rowObject == null) {
            return null;
        }
        return _getFormattedRow(grid, rowObject);
    }

    /**
     * Returns the output of renderer set for this column for given
     * {@code rowObject} formatted as close as possible to the client-side
     * output, using the column's renderer.
     *
     * @param <T>
     *            the item type
     * @param column
     *            the column to read
     * @param rowObject
     *            the item the row shows
     * @return the output of renderer set for this column for given {@code
     *         rowObject} formatted as close as possible to the client-side
     *         output, using the column's renderer
     */
    public static <T> Object getPresentationValue(Grid.Column<T> column,
            T rowObject) {
        Renderer<T> renderer = column.getRenderer();
        if (renderer instanceof ColumnPathRenderer) {
            @SuppressWarnings("unchecked")
            ColumnPathRenderer<T> cpr = (ColumnPathRenderer<T>) renderer;
            ValueProvider<T, ?> valueProvider = providerOf(cpr);
            if (valueProvider == null) {
                return null;
            }
            Object value = valueProvider.apply(rowObject);
            return String.valueOf(value);
        }
        return Renderers._getPresentationValue(renderer, rowObject);
    }

    private static <T> String getSortIndicator(Grid<T> grid,
            Grid.Column<T> column) {
        GridSortOrder<T> so = null;
        for (GridSortOrder<T> o : grid.getSortOrder()) {
            if (o.getSorted() == column) {
                so = o;
                break;
            }
        }
        if (so == null) {
            return "";
        }
        return so.getDirection() == SortDirection.ASCENDING ? "v" : "^";
    }

    /**
     * Dumps given range of rows of the Grid, formatting the values using the
     * {@code _getFormatted} function. Does not consider header groups. The
     * output example:
     *
     * <pre>
     * --[Name]--[Age]--[Occupation]--
     * 0: John, 25, Service Worker
     * 1: Fred, 40, Supervisor
     * --and 198 more
     * </pre>
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @return the grid contents, formatted as a table
     */
    public static <T> String _dump(Grid<T> grid) {
        return _dump(grid, 0, 9);
    }

    /**
     * Dumps the given range of rows of the grid, formatting the values with
     * {@code _getFormatted}. Does not consider header groups.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to dump
     * @param from
     *            the first row to dump, 0-based
     * @param to
     *            the row to stop before, exclusive
     * @return the rows, formatted as a table
     */
    public static <T> String _dump(Grid<T> grid, int from, int to) {
        StringBuilder sb = new StringBuilder();
        List<Grid.Column<T>> visibleColumns = new ArrayList<>();
        for (Grid.Column<T> c : grid.getColumns()) {
            if (c.isVisible()) {
                visibleColumns.add(c);
            }
        }
        sb.append("--");
        for (int i = 0; i < visibleColumns.size(); i++) {
            Grid.Column<T> c = visibleColumns.get(i);
            sb.append("[").append(c.getHeaderText()).append("]")
                    .append(getSortIndicator(grid, c));
            if (i < visibleColumns.size() - 1) {
                sb.append("-");
            }
        }
        sb.append("--\n");
        int rangeSize = Math.max(0, to + 1 - from);
        if (grid instanceof TreeGrid) {
            @SuppressWarnings("unchecked")
            TreeGrid<T> treeGrid = (TreeGrid<T>) grid;
            PrettyPrintTree tree = _dataSourceToPrettyTree(treeGrid);
            String printed = tree.print();
            List<String> lines = filterNotBlankAll(
                    Arrays.asList(printed.split("\n", -1)));
            if (!lines.isEmpty()) {
                lines = lines.subList(1, lines.size());
            }
            int dsSize = lines.size();
            int displayed = 0;
            for (int i = from; i <= to && i < dsSize; i++) {
                if (i >= 0) {
                    sb.append(i).append(": ").append(lines.get(i)).append("\n");
                    displayed++;
                }
            }
            int andMore = dsSize - displayed;
            if (andMore > 0) {
                sb.append("--and ").append(andMore).append(" more\n");
            }
        } else if (_dataProviderSupportsSizeOp(grid)) {
            int dsSize = _size(grid);
            int displayed = 0;
            for (int i = from; i <= to && i < dsSize; i++) {
                if (i >= 0) {
                    List<String> row = _getFormattedRow(grid, i);
                    sb.append(i).append(": ");
                    for (int j = 0; j < row.size(); j++) {
                        sb.append(row.get(j));
                        if (j < row.size() - 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append("\n");
                    displayed++;
                }
            }
            int andMore = dsSize - displayed;
            if (andMore > 0) {
                sb.append("--and ").append(andMore).append(" more\n");
            }
        } else {
            int rowsOutputted = 0;
            for (int i = from; i <= to; i++) {
                List<String> row = _getFormattedRowOrNull(grid, i);
                if (row == null) {
                    break;
                }
                sb.append(i).append(": ");
                for (int j = 0; j < row.size(); j++) {
                    sb.append(row.get(j));
                    if (j < row.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("\n");
                rowsOutputted++;
            }
            if (rowsOutputted == rangeSize) {
                sb.append("--and possibly more\n");
            } else {
                sb.append("--\n");
            }
        }
        return sb.toString();
    }

    /**
     * Asserts that this grid's provider returns given {@code count} of items.
     * If not, an {@link AssertionError} is thrown with the Grid {@code _dump}.
     *
     * @param grid
     *            the grid to read
     * @param count
     *            the expected number of items
     */
    public static void expectRows(Grid<?> grid, int count) {
        int actual = _size(grid);
        if (actual != count) {
            throw new AssertionError(
                    PrettyPrintTree.toPrettyString(grid) + ": expected " + count
                            + " rows but got " + actual + "\n" + _dump(grid));
        }
    }

    /**
     * Asserts that this grid's {@code rowIndex} row is formatted as expected.
     *
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param row
     *            the expected row formatting.
     */
    public static void expectRow(Grid<?> grid, int rowIndex, String... row) {
        List<String> expected = Arrays.asList(row);
        List<String> actual = _getFormattedRow(grid, rowIndex);
        if (!expected.equals(actual)) {
            throw new AssertionError(PrettyPrintTree.toPrettyString(grid)
                    + " at " + rowIndex + ": expected " + expected + " but got "
                    + actual + "\n" + _dump(grid));
        }
    }

    /**
     * Returns the column object backing the given HeaderCell. Returns an
     * instance of {@code com.vaadin.flow.component.grid.AbstractColumn}.
     */
    static Object column(HeaderRow.HeaderCell cell) {
        try {
            return _AbstractCell_getColumn.invoke(cell);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the column object backing the given FooterCell. Returns an
     * instance of {@code com.vaadin.flow.component.grid.AbstractColumn}.
     */
    static Object column(FooterRow.FooterCell cell) {
        try {
            return _AbstractCell_getColumn.invoke(cell);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@link ValueProvider} backing this
     * {@link ColumnPathRenderer}.
     */
    @SuppressWarnings("unchecked")
    static <T> ValueProvider<T, ?> providerOf(ColumnPathRenderer<T> renderer) {
        try {
            return (ValueProvider<T, ?>) _ColumnPathRenderer_provider
                    .get(renderer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves column key from an `AbstractColumn` instance. The problem here
     * is that the argument can be `ColumnGroup` which doesn't have a key.
     */
    private static String columnKey(Object abstractColumn) {
        try {
            _AbstractColumn_class.cast(abstractColumn);
            Grid.Column<?> gridColumn = (Grid.Column<?>) _AbstractColumn_getBottomLevelColumn
                    .invoke(abstractColumn);
            return gridColumn.getKey();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the cell for given {@link Grid.Column#getKey()}.
     *
     * @param row
     *            the row index, 0-based
     * @param key
     *            the key that was pressed
     * @return the corresponding cell
     * @throws IllegalArgumentException
     *             if no such column exists.
     */
    public static HeaderRow.HeaderCell getCell(HeaderRow row, String key) {
        for (HeaderRow.HeaderCell cell : row.getCells()) {
            Grid.Column<?> col = (Grid.Column<?>) column(cell);
            if (java.util.Objects.equals(col.getKey(), key)) {
                return cell;
            }
        }
        throw new IllegalArgumentException("This grid has no property named "
                + key + ": " + row.getCells());
    }

    /**
     * Retrieves the cell for given {@link Grid.Column#getKey()}.
     *
     * @param row
     *            the row index, 0-based
     * @param key
     *            the key that was pressed
     * @return the corresponding cell
     * @throws IllegalArgumentException
     *             if no such column exists.
     */
    public static FooterRow.FooterCell getCell(FooterRow row, String key) {
        for (FooterRow.FooterCell cell : row.getCells()) {
            if (java.util.Objects.equals(columnKey(column(cell)), key)) {
                return cell;
            }
        }
        throw new IllegalArgumentException("This grid has no property named "
                + key + ": " + row.getCells());
    }

    /**
     * Sorts given grid. Affects {@code _findAll}, {@code _get} and other
     * data-fetching functions.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param sortOrder
     *            the sort order to apply
     */
    public static <T> void sort(Grid<T> grid, QuerySortOrder... sortOrder) {
        List<GridSortOrder<T>> orders = new ArrayList<>();
        for (QuerySortOrder qso : sortOrder) {
            orders.add(
                    new GridSortOrder<>(_getColumnByKey(grid, qso.getSorted()),
                            qso.getDirection()));
        }
        grid.sort(orders);
    }

    // ---------------------------------------------------------------------
    // Click / select helpers
    // ---------------------------------------------------------------------

    /**
     * Fires the {@link ItemClickEvent} event for given {@code rowIndex} which
     * invokes all item click listeners registered via
     * {@link Grid#addItemClickListener(com.vaadin.flow.component.ComponentEventListener)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param button
     *            the id of the pressed mouse button (0 is the default button,
     *            see ClickEvent.getButton)
     * @param ctrlKey
     *            `true` if the control key was down when the event was fired,
     *            `false` otherwise
     * @param shiftKey
     *            `true` if the shift key was down when the event was fired,
     *            `false` otherwise
     * @param altKey
     *            `true` if the alt key was down when the event was fired,
     *            `false` otherwise
     * @param metaKey
     *            `true` if the meta key was down when the event was fired,
     *            `false` otherwise
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex, int button,
            boolean ctrlKey, boolean shiftKey, boolean altKey,
            boolean metaKey) {
        _clickItem(grid, rowIndex, (Grid.Column<?>) null, button, ctrlKey,
                shiftKey, altKey, metaKey);
    }

    /**
     * Equivalent to
     * {@code _clickItem(grid, rowIndex, 0, false, false, false, false)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex) {
        _clickItem(grid, rowIndex, 0, false, false, false, false);
    }

    /**
     * Fires the {@link ItemClickEvent} event for given {@code rowIndex} and a
     * {@code column} which invokes all item click listeners registered via
     * {@link Grid#addItemClickListener(com.vaadin.flow.component.ComponentEventListener)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param column
     *            optional column to be clicked
     * @param button
     *            the id of the pressed mouse button
     * @param ctrlKey
     *            `true` if the control key was down when the event was fired,
     *            `false` otherwise
     * @param shiftKey
     *            `true` if the shift key was down when the event was fired,
     *            `false` otherwise
     * @param altKey
     *            `true` if the alt key was down when the event was fired,
     *            `false` otherwise
     * @param metaKey
     *            `true` if the meta key was down when the event was fired,
     *            `false` otherwise
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex,
            Grid.Column<?> column, int button, boolean ctrlKey,
            boolean shiftKey, boolean altKey, boolean metaKey) {
        BasicUtils.checkEditableByUser(grid);
        // fire SelectionEvent if need be:
        // https://github.com/mvysny/karibu-testing/issues/96
        T item = _get(grid, rowIndex);
        if (grid.getSelectionModel() instanceof GridSingleSelectionModel) {
            Set<T> selected = grid.getSelectedItems();
            T selectedItem = selected.isEmpty() ? null
                    : selected.iterator().next();
            if (!java.util.Objects.equals(selectedItem, item)) {
                grid.select(item);
            } else {
                grid.deselectAll();
            }
        }

        // fire ItemClickEvent
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(item);
        String internalColumnId = column == null ? null : _internalId(column);
        ItemClickEvent<T> event = new ItemClickEvent<>(grid, true, itemKey,
                internalColumnId, -1, -1, -1, -1, 1, button, ctrlKey, shiftKey,
                altKey, metaKey);
        BasicUtils._fireEvent(grid, event);
    }

    /**
     * Equivalent to
     * {@code _clickItem(grid, rowIndex, column, 1, false, false, false, false)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param column
     *            the column to read
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex,
            Grid.Column<?> column) {
        _clickItem(grid, rowIndex, column, 1, false, false, false, false);
    }

    /**
     * Fires the {@link ItemClickEvent} event for given {@code rowIndex} and a
     * {@code columnKey} which invokes all item click listeners registered via
     * {@link Grid#addItemClickListener(com.vaadin.flow.component.ComponentEventListener)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param columnKey
     *            the key of the column to be clicked
     * @param button
     *            the id of the pressed mouse button
     * @param ctrlKey
     *            `true` if the control key was down when the event was fired,
     *            `false` otherwise
     * @param shiftKey
     *            `true` if the shift key was down when the event was fired,
     *            `false` otherwise
     * @param altKey
     *            `true` if the alt key was down when the event was fired,
     *            `false` otherwise
     * @param metaKey
     *            `true` if the meta key was down when the event was fired,
     *            `false` otherwise
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex,
            String columnKey, int button, boolean ctrlKey, boolean shiftKey,
            boolean altKey, boolean metaKey) {
        _clickItem(grid, rowIndex, _getColumnByKey(grid, columnKey), button,
                ctrlKey, shiftKey, altKey, metaKey);
    }

    /**
     * Equivalent to
     * {@code _clickItem(grid, rowIndex, columnKey, 1, false, false, false, false)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param columnKey
     *            the column key
     */
    public static <T> void _clickItem(Grid<T> grid, int rowIndex,
            String columnKey) {
        _clickItem(grid, rowIndex, columnKey, 1, false, false, false, false);
    }

    /**
     * Fires the {@link ItemDoubleClickEvent} event for given {@code rowIndex}
     * which invokes all item click listeners registered via
     * {@link Grid#addItemDoubleClickListener(com.vaadin.flow.component.ComponentEventListener)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     * @param button
     *            the id of the pressed mouse button
     * @param ctrlKey
     *            `true` if the control key was down when the event was fired,
     *            `false` otherwise
     * @param shiftKey
     *            `true` if the shift key was down when the event was fired,
     *            `false` otherwise
     * @param altKey
     *            `true` if the alt key was down when the event was fired,
     *            `false` otherwise
     * @param metaKey
     *            `true` if the meta key was down when the event was fired,
     *            `false` otherwise
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
        BasicUtils._fireEvent(grid, event);
    }

    /**
     * Equivalent to
     * {@code _doubleClickItem(grid, rowIndex, 1, false, false, false, false)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param rowIndex
     *            the row index, 0-based
     */
    public static <T> void _doubleClickItem(Grid<T> grid, int rowIndex) {
        _doubleClickItem(grid, rowIndex, 1, false, false, false, false);
    }

    // ---------------------------------------------------------------------
    // TreeGrid helpers
    // ---------------------------------------------------------------------

    /**
     * Returns a stream which walks over all rows the {@link TreeGrid} is
     * actually showing. The stream will <em>skip</em> children of collapsed
     * nodes.
     *
     * Iterating the entire stream is a very slow operation since it will
     * repeatedly poll {@link HierarchicalDataProvider} for list of children.
     *
     * Honors current grid ordering.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param filter
     *            the filter to pass to the query, or null for none
     * @return a stream which walks over all rows the {@link TreeGrid} is
     *         actually showing
     */
    public static <T> Stream<T> _rowSequence(TreeGrid<T> grid,
            SerializablePredicate<T> filter) {
        Predicate<T> isExpanded = grid::isExpanded;
        return _rowSequence(grid.getDataProvider(), null, isExpanded, filter);
    }

    /**
     * Equivalent to {@code _rowSequence(grid, null)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @return a stream over the rows the grid is actually showing
     */
    public static <T> Stream<T> _rowSequence(TreeGrid<T> grid) {
        return _rowSequence(grid, null);
    }

    /**
     * Returns a stream which walks over all rows the {@link TreeGrid} is
     * actually showing. The stream will <em>skip</em> children of collapsed
     * nodes.
     *
     * Iterating the entire stream is a very slow operation since it will
     * repeatedly poll {@link HierarchicalDataProvider} for list of children.
     *
     * Honors current grid ordering.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @param root
     *            start with this item; defaults to null to iterate all items
     * @param isExpanded
     *            if returns false for an item, children of that item are
     *            skipped
     * @param filter
     *            filter to pass to {@link HierarchicalQuery}
     * @return a stream which walks over all rows the {@link TreeGrid} is
     *         actually showing
     */
    public static <T, F> Stream<T> _rowSequence(
            HierarchicalDataProvider<T, F> provider, T root,
            Predicate<T> isExpanded, F filter) {
        Function<T, List<T>> getChildrenOf = item -> {
            if (item == null || isExpanded.test(item)) {
                return checkedFetch(provider,
                        new HierarchicalQuery<>(filter, item));
            }
            return Collections.emptyList();
        };
        List<T> roots = getChildrenOf.apply(root);
        Stream<T> result = Stream.empty();
        for (T r : roots) {
            Iterable<T> subtree = () -> new DepthFirstTreeIterator<>(r,
                    getChildrenOf::apply);
            result = Stream.concat(result,
                    StreamSupport.stream(subtree.spliterator(), false));
        }
        return result;
    }

    /**
     * Equivalent to {@code _rowSequence(provider, null, item -> true, null)}.
     *
     * @param <T>
     *            the item type
     * @param <F>
     *            the filter type
     * @param provider
     *            the data provider to read
     * @return a stream over all items the data provider returns
     */
    public static <T, F> Stream<T> _rowSequence(
            HierarchicalDataProvider<T, F> provider) {
        return _rowSequence(provider, null, item -> true, null);
    }

    /**
     * Returns the number of items the {@link TreeGrid} is actually showing. For
     * example it doesn't count in children of collapsed nodes.
     *
     * A very slow operation since it walks through all items returned by
     * {@code _rowSequence}.
     *
     * @param grid
     *            the grid to read
     * @return the number of items the {@link TreeGrid} is actually showing
     */
    public static int _size(TreeGrid<?> grid) {
        return (int) _rowSequence(grid).count();
    }

    private static <T, F> int checkedSize(
            HierarchicalDataProvider<T, F> provider,
            HierarchicalQuery<T, F> query) {
        if (query.getParent() != null
                && !provider.hasChildren(query.getParent())) {
            return 0;
        }
        int result = provider.size(query);
        if (result < 0) {
            throw new IllegalStateException(
                    "size(" + query + ") returned negative count: " + result);
        }
        return result;
    }

    private static <T, F> List<T> checkedFetch(
            HierarchicalDataProvider<T, F> provider,
            HierarchicalQuery<T, F> query) {
        if (checkedSize(provider, query) == 0) {
            return Collections.emptyList();
        }
        return provider.fetchChildren(query).collect(Collectors.toList());
    }

    /**
     * Renders the tree grid's data source as a pretty tree, expanding every
     * node regardless of what the grid currently shows.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the tree grid to read
     * @return the data source, as a printable tree
     */
    public static <T> PrettyPrintTree _dataSourceToPrettyTree(
            TreeGrid<T> grid) {
        Function<T, List<T>> getChildrenOf = item -> {
            if (item == null || grid.isExpanded(item)) {
                return checkedFetch(grid.getDataProvider(),
                        new HierarchicalQuery<>(null, item));
            }
            return Collections.emptyList();
        };
        List<T> roots = getChildrenOf.apply(null);
        List<PrettyPrintTree> rootTrees = new ArrayList<>();
        for (T r : roots) {
            rootTrees.add(toPrettyTree(grid, getChildrenOf, r));
        }
        return new PrettyPrintTree("TreeGrid", rootTrees);
    }

    private static <T> PrettyPrintTree toPrettyTree(TreeGrid<T> grid,
            Function<T, List<T>> getChildrenOf, T item) {
        List<String> row = _getFormattedRow(grid, item);
        StringBuilder self = new StringBuilder();
        for (int j = 0; j < row.size(); j++) {
            self.append(row.get(j));
            if (j < row.size() - 1) {
                self.append(", ");
            }
        }
        self.append("\n");
        List<T> children = getChildrenOf.apply(item);
        List<PrettyPrintTree> childTrees = new ArrayList<>();
        for (T c : children) {
            childTrees.add(toPrettyTree(grid, getChildrenOf, c));
        }
        return new PrettyPrintTree(self.toString(), childTrees);
    }

    /**
     * Equivalent to
     * {@code grid.getDataProvider().fetch(new HierarchicalQuery<>(null, null)) .collect(Collectors.toList())}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @return the root items of the tree
     */
    public static <T> List<T> _getRootItems(TreeGrid<T> grid) {
        return grid.getDataProvider().fetch(new HierarchicalQuery<>(null, null))
                .collect(Collectors.toList());
    }

    /**
     * Expands all nodes. May invoke massive data loading.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     * @param depth
     *            how deep the walk has gone, 0 for the root
     */
    public static <T> void _expandAll(TreeGrid<T> grid, int depth) {
        grid.expandRecursively(_getRootItems(grid), depth);
    }

    /**
     * Equivalent to {@code _expandAll(grid, 100)}.
     *
     * @param <T>
     *            the item type
     * @param grid
     *            the grid to read
     */
    public static <T> void _expandAll(TreeGrid<T> grid) {
        _expandAll(grid, 100);
    }

    // ---------------------------------------------------------------------
    // Component.dataProvider (full implementation)
    // ---------------------------------------------------------------------

    /**
     * Returns the data provider currently set to this Component.
     *
     * Works both with Vaadin 16 and Vaadin 17: Vaadin 17 components no longer
     * implement HasItems.
     *
     * @param c
     *            the component to inspect
     * @return the data provider currently set to this Component
     */
    public static DataProvider<?, ?> dataProvider(Component c) {
        try {
            // until https://github.com/vaadin/flow/issues/6296 is resolved
            if (c instanceof Grid) {
                return ((Grid<?>) c).getDataProvider();
            }
            if (c instanceof Select) {
                return ((Select<?>) c).getDataProvider();
            }
            if (c instanceof ListBoxBase) {
                return (DataProvider<?, ?>) _ListBoxBase_getDataProvider
                        .invoke(c);
            }
            if (c instanceof RadioButtonGroup) {
                return (DataProvider<?, ?>) _RadioButtonGroup_getDataProvider
                        .invoke(c);
            }
            if (c instanceof CheckboxGroup) {
                return (DataProvider<?, ?>) _CheckboxGroup_getDataProvider
                        .invoke(c);
            }
            if (c instanceof ComboBox) {
                return ((ComboBox<?>) c).getDataProvider();
            }
            return null;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Editor / selection
    // ---------------------------------------------------------------------

    /**
     * Call this instead of {@link Editor#editItem(Object)} - this function
     * makes sure that the editor opening is mocked properly, calls the editor
     * bindings, and fires the editor-open-event.
     *
     * @param <T>
     *            the item type
     * @param editor
     *            the editor to act on
     * @param item
     *            the item to act on
     */
    public static <T> void _editItem(Editor<T> editor, T item) {
        if (!editor.getGrid().isAttached()) {
            throw new IllegalStateException(
                    "Grid is not attached so can not edit");
        }
        editor.editItem(item);
        MockVaadin.clientRoundtrip();
    }

    private static volatile Method clientSelectAll;

    private static volatile Method clientDeselectAll;

    private static Method _AbstractGridMultiSelectionModel_clientSelectAll()
            throws NoSuchMethodException {
        if (clientSelectAll == null) {
            Method m = AbstractGridMultiSelectionModel.class
                    .getDeclaredMethod("clientSelectAll");
            m.setAccessible(true);
            clientSelectAll = m;
        }
        return clientSelectAll;
    }

    private static Method _AbstractGridMultiSelectionModel_clientDeselectAll()
            throws NoSuchMethodException {
        if (clientDeselectAll == null) {
            Method m = AbstractGridMultiSelectionModel.class
                    .getDeclaredMethod("clientDeselectAll");
            m.setAccessible(true);
            clientDeselectAll = m;
        }
        return clientDeselectAll;
    }

    private static <T> GridMultiSelectionModel<T> _selectAllCheckboxModel(
            Grid<T> grid, String action) {
        GridSelectionModel<T> model = grid.getSelectionModel();
        if (!(model instanceof GridMultiSelectionModel)) {
            throw new IllegalStateException(
                    action + " requires multi selection mode");
        }
        GridMultiSelectionModel<T> multi = (GridMultiSelectionModel<T>) model;
        if (!multi.isSelectAllCheckboxVisible()) {
            throw new IllegalStateException(
                    action + " requires the select all checkbox to be visible");
        }
        return multi;
    }

    /**
     * In single select clears the selection and selects only the given item; in
     * multi-select adds it to the selection.
     *
     * @param <T>
     *            the grid item type
     * @param grid
     *            the grid to select in
     * @param item
     *            the item to select
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
     * Selects all items in the Grid; runs the same code as when the "select
     * all" checkbox is checked.
     *
     * @param <T>
     *            the grid item type
     * @param grid
     *            the grid to select in
     * @throws IllegalStateException
     *             if the grid is not multi-select, or the "select all" checkbox
     *             is hidden
     */
    public static <T> void _selectAll(Grid<T> grid) {
        BasicUtils.checkEditableByUser(grid);
        GridMultiSelectionModel<T> model = _selectAllCheckboxModel(grid,
                "Select all");
        try {
            _AbstractGridMultiSelectionModel_clientSelectAll().invoke(model);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deselects the given item: the same code that runs when the user
     * ctrl-clicks a selected row or unchecks the row's selection checkbox in
     * multi-select, or clicks the selected row in single select.
     * <p>
     * The call is ignored — just as the user's click would be — if the item is
     * not selectable ({@link Grid#setItemSelectableProvider}), or if the grid
     * is single-select with
     * {@link GridSingleSelectionModel#isDeselectAllowed()} set to false.
     *
     * @param <T>
     *            the grid item type
     * @param grid
     *            the grid to deselect in
     * @param item
     *            the item to deselect; it must be selected, since deselecting a
     *            row that is not selected is not a gesture the user has
     * @throws IllegalStateException
     *             if the item is not selected
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
     * Deselects all items in the Grid; runs the same code as when the "select
     * all" checkbox is unchecked.
     * <p>
     * This is the counterpart of {@link #_selectAll} and behaves like the
     * checkbox does: the whole selection is dropped in one selection event,
     * without the per-row {@code ClientItemToggleEvent}s the user would cause
     * by unchecking rows one by one. Call {@link #_deselect} per row to model
     * that instead.
     *
     * @param <T>
     *            the grid item type
     * @param grid
     *            the grid to deselect in
     * @throws IllegalStateException
     *             if the grid is not multi-select, or the "select all" checkbox
     *             is hidden
     */
    public static <T> void _deselectAll(Grid<T> grid) {
        BasicUtils.checkEditableByUser(grid);
        GridMultiSelectionModel<T> model = _selectAllCheckboxModel(grid,
                "Deselect all");
        try {
            _AbstractGridMultiSelectionModel_clientDeselectAll().invoke(model);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the column's Internal ID. Not related to
     * {@link Grid.Column#getKey()} in any way. Auto-generated by the Grid. Not
     * really useful; mostly used internally by Vaadin.
     *
     * @param column
     *            the column to read
     * @return the column's internal id
     */
    public static String _internalId(Grid.Column<?> column) {
        try {
            return (String) _Column_getInternalId.invoke(column);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // private helpers
    // ---------------------------------------------------------------------

    private static List<String> filterNotBlankAll(List<String> source) {
        List<String> result = new ArrayList<>();
        for (String s : source) {
            if (s != null && !s.isBlank()) {
                result.add(s);
            }
        }
        return result;
    }
}
