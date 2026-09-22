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
package com.vaadin.flow.component.grid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import tools.jackson.databind.node.ArrayNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.LitRendererTestUtil;
import com.vaadin.browserless.MetaKeys;
import com.vaadin.browserless.MouseButton;
import com.vaadin.browserless.Tests;
import com.vaadin.browserless.component.GridKt;
import com.vaadin.browserless.internal.GridContextMenuSupport;
import com.vaadin.browserless.internal.RenderedComponentSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenuTester;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for Grid components.
 *
 * @param <T>
 *            component type
 * @param <Y>
 *            item type
 * @since 1.0
 */
@Tests(fqn = { "com.vaadin.flow.component.grid.Grid" })
public class GridTester<T extends Grid<Y>, Y> extends ComponentTester<T> {
    /**
     * Wrap grid for testing.
     *
     * @param component
     *            target grid
     */
    public GridTester(T component) {
        super(component);
    }

    /**
     * Get the amount of items in the grid.
     *
     * @return items in grid
     */
    public int size() {
        return GridKt._size(getComponent());
    }

    /**
     * Get the item at the given row index.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row index of to get
     * @return grid item on row
     */
    public Y getRow(int row) {
        return GridKt._get(getComponent(), row);
    }

    /**
     * Click on grid row.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     */
    public void clickRow(int row) {
        clickRow(row, MouseButton.LEFT);
    }

    /**
     * Click on grid row with given button.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     * @param button
     *            MouseButton that was clicked
     * @see com.vaadin.flow.component.ClickEvent#getButton()
     */
    public void clickRow(int row, MouseButton button) {
        clickRow(row, button, new MetaKeys());
    }

    /**
     * Click on grid row with given meta keys pressed.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     * @param metaKeys
     *            meta key statuses for click
     */
    public void clickRow(int row, MetaKeys metaKeys) {
        clickRow(row, MouseButton.LEFT, metaKeys);
    }

    /**
     * Click on grid row with given button and meta keys pressed.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     * @param button
     *            MouseButton that was clicked
     * @param metaKeys
     *            meta key statuses for click
     * @see com.vaadin.flow.component.ClickEvent#getButton()
     */
    public void clickRow(int row, MouseButton button, MetaKeys metaKeys) {
        ensureComponentIsUsable();
        GridKt._clickItem(getComponent(), row, button.getButton(),
                metaKeys.isCtrl(), metaKeys.isShift(), metaKeys.isAlt(),
                metaKeys.isMeta());
    }

    /**
     * Double-click on grid row.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     */
    public void doubleClickRow(int row) {
        doubleClickRow(row, MouseButton.LEFT);
    }

    /**
     * Double-click on grid row with given button.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     * @param button
     *            MouseButton that was clicked
     * @see com.vaadin.flow.component.ClickEvent#getButton()
     */
    public void doubleClickRow(int row, MouseButton button) {
        doubleClickRow(row, button, new MetaKeys());
    }

    /**
     * Double-click on grid row with given meta keys pressed.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     * @param metaKeys
     *            meta key statuses for click
     */
    public void doubleClickRow(int row, MetaKeys metaKeys) {
        doubleClickRow(row, MouseButton.LEFT, metaKeys);
    }

    /**
     * Double-click on grid row with given button and meta keys pressed.
     * <p/>
     * The index is 0 based.
     *
     * @param row
     *            row to click
     * @param button
     *            MouseButton that was clicked
     * @param metaKeys
     *            meta key statuses for click
     * @see com.vaadin.flow.component.ClickEvent#getButton()
     */
    public void doubleClickRow(int row, MouseButton button, MetaKeys metaKeys) {
        ensureComponentIsUsable();
        GridKt._doubleClickItem(getComponent(), row, button.getButton(),
                metaKeys.isCtrl(), metaKeys.isShift(), metaKeys.isAlt(),
                metaKeys.isMeta());
    }

    /**
     * Select the item on given row.
     * <p/>
     * The index is 0 based.
     * <p/>
     * Single select will clear any old selections. Multi select will add to
     * selection.
     *
     * @param row
     *            row to select
     * @throws IllegalStateException
     *             if not usable
     */
    public void select(int row) {
        ensureComponentIsUsable();
        final Y item = getRow(row);
        GridKt._select(getComponent(), item);
    }

    /**
     * Select all items in grid, running the same code as when the select all
     * checkbox is checked.
     * <p/>
     * Only works for multi select, and only when the select all checkbox is
     * actually shown - if it isn't, the user has no way to trigger this.
     *
     * @throws IllegalStateException
     *             if not usable, not multi select or the select all checkbox is
     *             hidden
     */
    public void selectAll() {
        ensureComponentIsUsable();
        GridKt._selectAll(getComponent());
    }

    /**
     * Deselect the item on given row.
     * <p/>
     * The index is 0 based.
     * <p/>
     * Simulates the user deselecting a row: ctrl-clicking a selected row or
     * unchecking the row's selection checkbox in multi select, clicking the
     * selected row in single select.
     * <p/>
     * The row has to be selected. Deselecting a row that isn't selected is not
     * a gesture the user has, so it fails instead of doing nothing.
     * <p/>
     * The call is ignored, exactly as the user's click would be, when the item
     * is not selectable or when the grid is single select and deselecting is
     * not allowed.
     *
     * @param row
     *            row to deselect
     * @throws IllegalStateException
     *             if not usable, if the row is not selected or if the grid
     *             doesn't support selection
     * @since 25.3
     */
    public void deselect(int row) {
        ensureComponentIsUsable();
        final Y item = getRow(row);
        GridKt._deselect(getComponent(), item);
    }

    /**
     * Deselect all items in grid, running the same code as when the select all
     * checkbox is unchecked.
     * <p/>
     * Only works for multi select, and only when the select all checkbox is
     * actually shown - if it isn't, the user has no way to trigger this.
     * <p/>
     * This is the counterpart of {@link #selectAll()} and behaves like the
     * checkbox does: the selection is dropped in one selection event, without
     * the per row toggle events the user would cause by unchecking rows one by
     * one. Call {@link #deselect(int)} per row to model that instead.
     *
     * @throws IllegalStateException
     *             if not usable, not multi select or the select all checkbox is
     *             hidden
     * @since 25.3
     */
    public void deselectAll() {
        ensureComponentIsUsable();
        GridKt._deselectAll(getComponent());
    }

    /**
     * Get the text that is shown on the client for the cell in the given
     * position.
     * <p/>
     * The indexes for row and column are 0 based.
     * <p/>
     * For the default renderer ColumnPathRenderer the result is the sent text
     * for defined object path.
     * <p/>
     * For a ComponentRenderer the result is the text of the component the grid
     * rendered for the cell, read through {@link #getCellComponent(int, int)}:
     * a row the client has not asked for yet is scrolled into view first, and a
     * cell the grid renders no component for fails the same way. A renderer
     * that returns no component renders an empty cell, so the text is empty
     * rather than {@literal null}.
     * <p/>
     * More to be added as we find other renderers that need handling.
     *
     * @param row
     *            row of cell
     * @param column
     *            column of cell
     * @return cell content that is sent to the client
     * @throws IllegalStateException
     *             if component is not visible, or if the grid renders no
     *             component for a ComponentRenderer cell
     */
    public String getCellText(int row, int column) {
        ensureVisible();
        final Grid.Column<Y> targetColumn = getColumns().get(column);
        if (targetColumn.getRenderer() instanceof ComponentRenderer) {
            return getCellComponent(row, column).getElement()
                    .getTextRecursively();
        } else if (targetColumn.getRenderer() instanceof ColumnPathRenderer) {
            // This renderer just writes the object text using a path
            return getValueProviderString(row, targetColumn);
        }
        return null;
    }

    /**
     * Get the component the grid renders for the cell in the given position.
     *
     * <p>
     * A component renderer only produces a component when the grid renders a
     * row for the client, so a renderer component is never part of the
     * component tree that {@code find(...)} walks, and this method is the way
     * to reach it. What comes back is the very component the browser shows:
     * asking for the same cell twice returns the same instance, and the
     * instance is replaced when the grid re-renders the row, for example after
     * {@code refreshItem(...)}. A row the client has not asked for yet is
     * scrolled into view first, the way a user reaches it.
     *
     * <p>
     * Use {@link #renderCellComponent(int, int)} to render a cell on its own,
     * without the grid.
     *
     * @param row
     *            item row
     * @param column
     *            column to get
     * @return the component the grid rendered for the targeted cell
     * @throws IllegalArgumentException
     *             when the target column of the cell is not a component
     *             renderer
     * @throws IllegalStateException
     *             when the grid renders no component for the cell
     */
    public Component getCellComponent(int row, int column) {
        ensureVisible();
        final Grid.Column<Y> yColumn = getColumns().get(column);
        return getRenderedCellComponent(row, yColumn);
    }

    /**
     * Get the component the grid renders for the cell in the given row of the
     * column with the given key.
     *
     * <p>
     * A component renderer only produces a component when the grid renders a
     * row for the client, so a renderer component is never part of the
     * component tree that {@code find(...)} walks, and this method is the way
     * to reach it. What comes back is the very component the browser shows:
     * asking for the same cell twice returns the same instance, and the
     * instance is replaced when the grid re-renders the row, for example after
     * {@code refreshItem(...)}. A row the client has not asked for yet is
     * scrolled into view first, the way a user reaches it.
     *
     * <p>
     * Use {@link #renderCellComponent(int, String)} to render a cell on its
     * own, without the grid.
     *
     * @param row
     *            item row
     * @param columnName
     *            key/property of column
     * @return the component the grid rendered for the target cell
     * @throws IllegalArgumentException
     *             when column for property doesn't exist or the target column
     *             of the cell is not a component renderer
     * @throws IllegalStateException
     *             when the grid renders no component for the cell, which is the
     *             case for a hidden column
     */
    public Component getCellComponent(int row, String columnName) {
        ensureVisible();
        return getRenderedCellComponent(row, getColumnByKey(columnName));
    }

    /**
     * Render the component for the cell in the given position on its own,
     * without the grid, and attach it to the grid so that it can be used.
     *
     * <p>
     * This asks the column's component renderer for a component for the item on
     * the row, which is not the instance the grid renders for the client: every
     * call renders the cell again and attaches the new instance to the grid, so
     * asking twice for the same cell leaves two instances behind and a later
     * {@code find(...)} reports both.
     *
     * <p>
     * Prefer {@link #getCellComponent(int, int)}, which returns the component
     * the browser shows. This method is for tests written against the older
     * behaviour of that method. A column index addresses the visible columns,
     * so the cells the grid renders nothing for, those of a hidden column, are
     * only reachable through {@link #renderCellComponent(int, String)}.
     *
     * @param row
     *            item row
     * @param column
     *            column to render
     * @return a freshly rendered component for the targeted cell
     * @throws IllegalArgumentException
     *             when the target column of the cell is not a component
     *             renderer
     * @since 25.3
     */
    public Component renderCellComponent(int row, int column) {
        ensureVisible();
        return getRendererItem(row, getColumns().get(column));
    }

    /**
     * Render the component for the cell in the given row of the column with the
     * given key on its own, without the grid, and attach it to the grid so that
     * it can be used.
     *
     * <p>
     * This asks the column's component renderer for a component for the item on
     * the row, which is not the instance the grid renders for the client: every
     * call renders the cell again and attaches the new instance to the grid, so
     * asking twice for the same cell leaves two instances behind and a later
     * {@code find(...)} reports both.
     *
     * <p>
     * Prefer {@link #getCellComponent(int, String)}, which returns the
     * component the browser shows. This method is for the cases the grid does
     * not render itself, such as a hidden column.
     *
     * @param row
     *            item row
     * @param columnName
     *            key/property of column
     * @return a freshly rendered component for the target cell
     * @throws IllegalArgumentException
     *             when column for property doesn't exist or the target column
     *             of the cell is not a component renderer
     * @since 25.3
     */
    public Component renderCellComponent(int row, String columnName) {
        ensureVisible();
        return getRendererItem(row, getColumnByKey(columnName));
    }

    private Grid.Column<Y> getColumnByKey(String columnName) {
        final Grid.Column<Y> column = getComponent().getColumnByKey(columnName);
        if (column == null) {
            throw new IllegalArgumentException(
                    "No column for property '" + columnName + "' exists");
        }
        return column;
    }

    private Component getRenderedCellComponent(int row,
            Grid.Column<Y> yColumn) {
        ensureComponentRenderer(yColumn);
        if (!yColumn.isVisible()) {
            throw new IllegalStateException("Column '" + yColumn.getKey()
                    + "' is not visible, so the grid renders no component for "
                    + "it. Use renderCellComponent to render the cell without "
                    + "the grid.");
        }
        // pending row rendering happens when the response is written
        roundTrip();
        Component component = findRenderedCellComponent(row, yColumn);
        if (component == null) {
            // the client has not asked for the row yet, so the grid has not
            // rendered it - bring it into the viewport as a user scrolling
            // down to the row would
            getComponent().scrollToIndex(row);
            roundTrip();
            component = findRenderedCellComponent(row, yColumn);
        }
        if (component == null) {
            throw new IllegalStateException("Grid rendered no component for "
                    + "row " + row + " in column '" + yColumn.getKey()
                    + "'. The item on the row is not the one the grid rendered,"
                    + " which happens when the data provider returns items that"
                    + " are not equal across fetches. Use renderCellComponent"
                    + " to render the cell without the grid.");
        }
        return component;
    }

    private Component findRenderedCellComponent(int row,
            Grid.Column<Y> yColumn) {
        final Y item = getRow(row);
        final DataCommunicator<Y> dataCommunicator = getComponent()
                .getDataCommunicator();
        if (!dataCommunicator.isItemActive(item)) {
            return null;
        }
        return RenderedComponentSupport.getRenderedComponent(yColumn,
                dataCommunicator.getKeyMapper().key(item));
    }

    private void ensureComponentRenderer(Grid.Column<Y> yColumn) {
        if (!(yColumn.getRenderer() instanceof ComponentRenderer)) {
            throw new IllegalArgumentException(
                    "Target column doesn't have a ComponentRenderer.");
        }
    }

    private Component getRendererItem(int row, Grid.Column<Y> yColumn) {
        ensureComponentRenderer(yColumn);
        final Y item = getRow(row);
        var component = ((ComponentRenderer<?, Y>) yColumn.getRenderer())
                .createComponent(item);
        if (component != null) {
            getComponent().getElement().appendChild(component.getElement());
        }
        return component;
    }

    private <V> V getLitRendererPropertyValue(int row, Grid.Column<Y> column,
            String propertyName, Class<V> propertyClass) {
        ensureVisible();

        if (column.getRenderer() instanceof LitRenderer<Y> litRenderer) {
            return LitRendererTestUtil.getPropertyValue(litRenderer,
                    this::getField, this::getRow, row, propertyName,
                    propertyClass);
        } else {
            throw new IllegalArgumentException(
                    "Target column doesn't have a LitRenderer.");
        }
    }

    /**
     * Get property value for item's LitRenderer in column.
     *
     * @param row
     *            item row
     * @param columnName
     *            key/property of column
     * @param propertyName
     *            the name of the LitRenderer property
     * @param propertyClass
     *            the class of the value of the LitRenderer property
     * @param <V>
     *            the type of the LitRenderer property
     * @return value of renderer's property for the target cell
     * @throws IllegalArgumentException
     *             when column for property doesn't exist or the target column
     *             of the cell is not a LitRenderer or when the given type of
     *             the property does not match the actual property type
     */
    public <V> V getLitRendererPropertyValue(int row, String columnName,
            String propertyName, Class<V> propertyClass) {
        return getLitRendererPropertyValue(row, getColumn(columnName),
                propertyName, propertyClass);
    }

    /**
     * Get property value for item's LitRenderer in column.
     *
     * @param row
     *            item row
     * @param column
     *            column to get
     * @param propertyName
     *            the name of the LitRenderer property
     * @param propertyClass
     *            the class of the value of the LitRenderer property
     * @param <V>
     *            the type of the LitRenderer property
     * @return value of renderer's property for the target cell
     * @throws IllegalArgumentException
     *             when column for property doesn't exist or the target column
     *             of the cell is not a LitRenderer or when the given type of
     *             the property does not match the actual property type
     */
    public <V> V getLitRendererPropertyValue(int row, int column,
            String propertyName, Class<V> propertyClass) {
        return getLitRendererPropertyValue(row, getColumns().get(column),
                propertyName, propertyClass);
    }

    private void invokeLitRendererFunction(int row, Grid.Column<Y> column,
            String functionName, ArrayNode jsonArray) {
        ensureVisible();

        if (column.getRenderer() instanceof LitRenderer<Y> litRenderer) {
            LitRendererTestUtil.invokeFunction(litRenderer, this::getField,
                    this::getRow, row, functionName, jsonArray);
        } else {
            throw new IllegalArgumentException(
                    "Target column doesn't have a LitRenderer.");
        }
    }

    /**
     * Invoke named function for item's LitRenderer in column using the supplied
     * JSON arguments.
     *
     * @param row
     *            item row
     * @param columnName
     *            key/property of column
     * @param functionName
     *            the name of the LitRenderer function to invoke
     * @param jsonArray
     *            the arguments to pass to the function
     */
    public void invokeLitRendererFunction(int row, String columnName,
            String functionName, ArrayNode jsonArray) {
        invokeLitRendererFunction(row, getColumn(columnName), functionName,
                jsonArray);
    }

    /**
     * Invoke named function for item's LitRenderer in column.
     *
     * @param row
     *            item row
     * @param columnName
     *            key/property of column
     * @param functionName
     *            the name of the LitRenderer function to invoke
     */
    public void invokeLitRendererFunction(int row, String columnName,
            String functionName) {
        invokeLitRendererFunction(row, columnName, functionName,
                JacksonUtils.createArrayNode());
    }

    /**
     * Invoke named function for item's LitRenderer in column using the supplied
     * JSON arguments.
     *
     * @param row
     *            item row
     * @param column
     *            column to get
     * @param functionName
     *            the name of the LitRenderer function to invoke
     * @param jsonArray
     *            the arguments to pass to the function
     */
    public void invokeLitRendererFunction(int row, int column,
            String functionName, ArrayNode jsonArray) {
        invokeLitRendererFunction(row, getColumns().get(column), functionName,
                jsonArray);
    }

    /**
     * Invoke named function for item's LitRenderer in column.
     *
     * @param row
     *            item row
     * @param column
     *            column to get
     * @param functionName
     *            the name of the LitRenderer function to invoke
     */
    public void invokeLitRendererFunction(int row, int column,
            String functionName) {
        invokeLitRendererFunction(row, column, functionName,
                JacksonUtils.createArrayNode());
    }

    /**
     * Get content in header for given column.
     *
     * @param column
     *            column to get header for
     * @return header contents
     * @throws IllegalStateException
     *             if component is not visible
     *
     * @deprecated Use {@link Grid.Column#getHeaderText()} or
     *             {@link Grid.Column#getHeaderComponent()}
     */
    @Deprecated
    public String getHeaderCell(int column) {
        ensureVisible();
        return getColumns().get(column).getHeaderText();
    }

    /**
     * Return visible columns in the grid. The order of the columns is the same
     * as in the grid.
     * 
     * @return visible columns in the grid
     * @since 25.3
     */
    protected List<Grid.Column<Y>> getColumns() {
        return getComponent().getColumns().stream().filter(Component::isVisible)
                .toList();
    }

    /**
     * Get the column position by column property.
     *
     * @param property
     *            the property name of the column, not null
     * @return int position of column
     */
    public int getColumnPosition(String property) {
        Objects.requireNonNull(property, "property name must not be null");
        return getColumns().indexOf(getColumn(property));
    }

    /**
     * Gets the grid column by column property.
     *
     * @param property
     *            the property name of the column, not null
     * @return Grid.Column for property
     */
    public Grid.Column<Y> getColumn(String property) {
        Objects.requireNonNull(property, "property name must not be null");
        return getComponent().getColumnByKey(property);
    }

    /**
     * Get content in footer for given column.
     *
     * @param column
     *            column to get footer for
     * @return footer contents
     * @throws IllegalStateException
     *             if component is not visible
     * @deprecated Use {@link Grid.Column#getFooterText()} or
     *             {@link Grid.Column#getFooterComponent()} directly
     */
    @Deprecated(forRemoval = true)
    public String getFooterCell(int column) {
        ensureVisible();
        return getColumns().get(column).getFooterText();
    }

    /**
     * Get selected items.
     *
     * @return selected items
     */
    public Collection<Y> getSelected() {
        ensureComponentIsUsable();
        return getComponent().getSelectedItems();
    }

    /**
     * Checks if the column at the given index is sortable.
     * <p/>
     * The index is 0 based.
     *
     * @param column
     *            column index to check for sort feature
     * @return {@literal true} if the column is sortable, otherwise
     *         {@literal false}
     * @throws IndexOutOfBoundsException
     *             if column index is invalid
     */
    public boolean isColumnSortable(int column) {
        return getColumns().get(column).isSortable();
    }

    /**
     * Checks if the column for the given property is sortable.
     *
     * @param property
     *            the property name of the column, not null
     * @return {@literal true} if the column is sortable, otherwise
     *         {@literal false}
     * @throws IllegalArgumentException
     *             if property name does not identify a column
     */
    public boolean isColumnSortable(String property) {
        Grid.Column<Y> column = getColumn(property);
        if (column == null) {
            throw new IllegalArgumentException(
                    "No column found for property " + property);
        }
        return column.isSortable();
    }

    /**
     * Gets the current sort direction for column at the given index.
     *
     * Throws an exception if the column does not exist or is not sortable.
     *
     * @param column
     *            column index to get sort direction
     * @return sort direction for the column, or {@literal null} if grid is not
     *         sorted by given column
     * @throws IllegalArgumentException
     *             if the column at given index is not sortable
     * @throws IndexOutOfBoundsException
     *             if column index is invalid
     */
    public SortDirection getSortDirection(int column) {
        if (isColumnSortable(column)) {
            Grid.Column<Y> col = getColumns().get(column);
            return getComponent().getSortOrder().stream()
                    .filter(order -> col.equals(order.getSorted()))
                    .map(SortOrder::getDirection).findFirst().orElse(null);
        }
        throw new IllegalArgumentException(
                "Column at index " + column + " is not sortable");
    }

    /**
     * Gets the current sort direction for column corresponding to the at the
     * given property.
     *
     * Throws an exception if the column does not exist or is not sortable.
     *
     * @param property
     *            the property name of the column, not null
     * @return sort direction for the column, or {@literal null} if grid is not
     *         sorted by given column
     * @throws IllegalArgumentException
     *             if property name does not identify a column or if the column
     *             is not sortable
     */
    public SortDirection getSortDirection(String property) {
        if (isColumnSortable(property)) {
            Grid.Column<Y> col = getColumn(property);
            return getComponent().getSortOrder().stream()
                    .filter(order -> col.equals(order.getSorted()))
                    .map(SortOrder::getDirection).findFirst().orElse(null);
        }
        throw new IllegalArgumentException(
                "Column for property " + property + " is not sortable");
    }

    /**
     * Sorts the grid by the given column and sort direction, as if the column
     * header is pressed in the browser until the requested direction is
     * reached.
     *
     * Throws an exception if the column is not sortable or not visible.
     *
     * @param column
     *            column index
     * @param direction
     *            sort direction
     */
    public void sortByColumn(int column, SortDirection direction) {
        // the loop below may not run at all when the grid is already sorted in
        // the requested direction, so doSort() cannot be relied on to check
        ensureComponentIsUsable();
        while (getSortDirection(column) != direction) {
            sortByColumn(column);
        }
    }

    /**
     * Sorts the grid according to the given column sort status, as if the
     * column header is pressed in the browser.
     *
     * Throws an exception if the column is not sortable or not visible.
     *
     * @param column
     *            column index
     */
    public void sortByColumn(int column) {
        SortDirection currentDirection = getSortDirection(column);
        Grid.Column<Y> col = getColumns().get(column);
        doSort(currentDirection, col);
    }

    /**
     * Sorts the grid according to sort status ot the column identified by the
     * given property, as if the column header is pressed in the browser.
     *
     * Throws an exception if the column is not sortable or not visible.
     *
     * @param property
     *            the property name of the column, not null
     */
    public void sortByColumn(String property) {
        SortDirection currentDirection = getSortDirection(property);
        Grid.Column<Y> col = getColumn(property);
        doSort(currentDirection, col);
    }

    /**
     * Sorts the grid by the given column and sort direction, as if the column
     * header is pressed in the browser until the requested direction is
     * reached.
     *
     * Throws an exception if the column is not sortable or not visible.
     *
     * @param property
     *            the property name of the column, not null
     * @param direction
     *            sort direction
     */
    public void sortByColumn(String property, SortDirection direction) {
        // the loop below may not run at all when the grid is already sorted in
        // the requested direction, so doSort() cannot be relied on to check
        ensureComponentIsUsable();
        while (getSortDirection(property) != direction) {
            sortByColumn(property);
        }
    }

    private Grid.MultiSortPriority getMultiSortPriority() {
        return "append".equals(
                getComponent().getElement().getAttribute("multi-sort-priority"))
                        ? Grid.MultiSortPriority.APPEND
                        : Grid.MultiSortPriority.PREPEND;
    }

    private void doSort(SortDirection currentDirection, Grid.Column<Y> col) {
        ensureComponentIsUsable();
        List<GridSortOrder<Y>> sortOrders = new ArrayList<>(
                getComponent().getSortOrder());
        if (getComponent().isMultiSort()) {
            sortOrders.removeIf(so -> so.getSorted() == col);
        } else {
            sortOrders.clear();
        }
        final Grid.MultiSortPriority multiSortPriority = getMultiSortPriority();
        final int insertIndex = multiSortPriority == Grid.MultiSortPriority.PREPEND
                ? 0
                : sortOrders.size();
        if (currentDirection == null) {
            sortOrders.add(insertIndex, GridSortOrder.asc(col).build().get(0));
        } else if (currentDirection == SortDirection.ASCENDING) {
            sortOrders.add(insertIndex, GridSortOrder.desc(col).build().get(0));
        }
        getComponent().sort(sortOrders);
    }

    /**
     * Gets a tester for the context menu of this grid, targeting the given row.
     * <p/>
     * The index is 0 based and counts the rows the user sees. The menu is not
     * opened, so that assertions can be made on it first; open it with
     * {@link GridContextMenuTester#open()}.
     *
     * <pre>
     * var menu = test(grid).contextMenu(0);
     * menu.open();
     * menu.clickItem("Edit");
     * </pre>
     *
     * @param row
     *            row the context menu is about
     * @return a tester for the context menu of this grid, targeting the given
     *         row
     * @throws IllegalStateException
     *             if the grid is not usable, or if it has no context menu or
     *             more than one
     * @since 25.3
     */
    @SuppressWarnings("unchecked")
    public GridContextMenuTester<GridContextMenu<Y>, Y> contextMenu(int row) {
        ensureComponentIsUsable();
        GridContextMenu<Y> menu = (GridContextMenu<Y>) GridContextMenuSupport
                .getContextMenu(getComponent());
        return new GridContextMenuTester<>(menu, row);
    }

    private String getValueProviderString(int row, Grid.Column<Y> targetColumn)
            throws IllegalArgumentException {
        try {
            ColumnPathRenderer<Y> renderer = (ColumnPathRenderer<Y>) targetColumn
                    .getRenderer();

            Field f = ColumnPathRenderer.class.getDeclaredField("provider");
            f.setAccessible(true);

            @SuppressWarnings("unchecked")
            final ValueProvider<Y, ?> columnValueProvider = (ValueProvider<Y, ?>) f
                    .get(renderer);

            return columnValueProvider.apply(getRow(row)).toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Failed to get value provider for column", e);
        }
    }

}
