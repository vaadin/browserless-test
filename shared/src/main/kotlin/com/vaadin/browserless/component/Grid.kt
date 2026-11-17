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
@file:Suppress("FunctionName")

package com.vaadin.browserless.component

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.checkbox.CheckboxGroup
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.grid.AbstractGridMultiSelectionModel
import com.vaadin.flow.component.grid.ColumnPathRenderer
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridMultiSelectionModel
import com.vaadin.flow.component.grid.GridNoneSelectionModel
import com.vaadin.flow.component.grid.GridSingleSelectionModel
import com.vaadin.flow.component.grid.GridSortOrder
import com.vaadin.flow.component.grid.ItemClickEvent
import com.vaadin.flow.component.grid.ItemDoubleClickEvent
import com.vaadin.flow.component.listbox.ListBoxBase
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.DataCommunicator
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.provider.SortDirection
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import com.vaadin.flow.data.renderer.Renderer
import com.vaadin.flow.function.SerializablePredicate
import com.vaadin.flow.function.ValueProvider
import com.vaadin.browserless.internal.DepthFirstTreeIterator
import com.vaadin.browserless.internal.PrettyPrintTree
import com.vaadin.browserless.internal._getPresentationValue
import com.vaadin.browserless.internal._saneFetchLimit
import com.vaadin.browserless.internal.checkEditableByUser
import com.vaadin.browserless.internal.filterNotBlank
import com.vaadin.browserless.internal.findClassOrThrow
import com.vaadin.browserless.internal.size
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Returns the item on given row of given [grid]. Fails if the row index is invalid. The data provider is
 * sorted according to given [Grid.getSortOrder] and filtered according to the current filter.
 *
 * For [TreeGrid] this returns the x-th displayed row; skips children of collapsed nodes.
 * Uses [_rowSequence].
 *
 * @param rowIndex the row, 0..size - 1
 * @return the item at given row.
 * @throws AssertionError if the row index is out of bounds.
 */
public fun <T : Any> _get(grid: Grid<T>, rowIndex: Int): T {
    require(rowIndex >= 0) { "rowIndex must be 0 or greater: $rowIndex" }
    if (grid !is TreeGrid && isDefinedSize(grid)) {
        // only perform this check for regular Grid. TreeGrid._fetch()'s Sequence consults size() internally.
        val size: Int = _size(grid)
        if (rowIndex >= size) {
            throw AssertionError("Requested to get row $rowIndex but the data provider only has $size rows\n${dump(grid)}")
        }
    }
    return getOrNull(grid, rowIndex)
        ?: throw AssertionError("Requested to get row $rowIndex but the data provider returned 0 rows\n${dump(grid)}")
}

private fun <T : Any> getOrNull(grid: Grid<T>, rowIndex: Int): T? {
    require(rowIndex >= 0) { "rowIndex must be 0 or greater: $rowIndex" }
    if (grid !is TreeGrid && isDefinedSize(grid)) {
        // only perform this check for regular Grid. TreeGrid.fetch()'s Sequence consults size() internally.
        if (rowIndex >= _size(grid)) {
            return null
        }
    }
    return fetch(grid, rowIndex, 1).firstOrNull()
}

private fun isDefinedSize(grid: Grid<*>): Boolean = grid.dataCommunicator.isDefinedSize

private fun <T> fetch(grid: Grid<T>, offset: Int, limit: Int): List<T> = when (grid) {
    is TreeGrid<T> -> rowSequence(grid).drop(offset).take(limit).toList()
    else -> fetch(grid.dataCommunicator, offset, limit)
}

private val _DataCommunicator_fetchFromProvider: Method =
    DataCommunicator::class.java.getDeclaredMethod("fetchFromProvider", Int::class.java, Int::class.java).apply {
        isAccessible = true
    }

private fun <T> fetch(dataCommunicator: DataCommunicator<T>, offset: Int, limit: Int): List<T> {
    require(limit <= _saneFetchLimit) { "Vaadin doesn't handle fetching of many items very well unfortunately. The sane limit is $_saneFetchLimit but you asked for $limit" }
    // make sure the DataCommunicator is not in paged mode: https://github.com/mvysny/karibu-testing/issues/99
    dataCommunicator.isPagingEnabled = false
    @Suppress("UNCHECKED_CAST")
    val fetched: Stream<T> = _DataCommunicator_fetchFromProvider.invoke(dataCommunicator, offset, limit) as Stream<T>
    return fetched.toList()
}

/**
 * Returns the number of items in given [grid]. For [TreeGrid] this returns the
 * number of displayed rows; children of collapsed nodes are not counted.
 */
public fun _size(grid: Grid<*>): Int {
    if (grid is TreeGrid<*>) {
        return rowSequence(grid).count()
    }
    if (!isDefinedSize(grid)) {
        return fetch(grid, 0, _saneFetchLimit).size
    }
    return grid.dataCommunicator.dataProviderSize
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> getPresentationValue(column: Grid.Column<T>, rowObject: T): Any? {
    val renderer: Renderer<T> = column.renderer
    if (renderer is ColumnPathRenderer) {
        val value: Any? = getValueProvider(renderer).apply(rowObject)
        return value.toString()
    }
    return _getPresentationValue(renderer, rowObject)
}

private fun <T : Any> getFormattedRow(grid: Grid<T>, rowObject: T): List<String> =
    grid.columns.filter { it.isVisible }.map { getPresentationValue(it, rowObject).toString() }

private fun <T> getSortIndicator(grid: Grid<T>, column: Grid.Column<T>): String {
    val so: GridSortOrder<T>? = grid.sortOrder.firstOrNull { it.sorted == column }
    return when {
        so == null -> ""
        so.direction == SortDirection.ASCENDING -> "v"
        else -> "^"
    }
}

/**
 * Dumps the first 10 rows of given [grid] as text, for error messages.
 */
private fun <T : Any> dump(grid: Grid<T>): String = buildString {
    val rows: IntRange = 0..9
    val visibleColumns: List<Grid.Column<T>> = grid.columns.filter { it.isVisible }
    visibleColumns.map { "[${it.headerText}]${getSortIndicator(grid, it)}" }.joinTo(this, prefix = "--", separator = "-", postfix = "--\n")
    val dsIndices: IntRange
    val displayIndices: Set<Int>
    if (grid is TreeGrid<T>) {
        val tree: PrettyPrintTree = dataSourceToPrettyTree(grid)
        val lines = tree.print().split('\n').filterNotBlank().drop(1)
        dsIndices = lines.indices
        displayIndices = rows.intersect(dsIndices)
        for (i in displayIndices) {
            append("$i: ${lines[i]}\n")
        }
        val andMore = dsIndices.size - displayIndices.size
        if (andMore > 0) {
            append("--and $andMore more\n")
        }
    } else if (isDefinedSize(grid)) {
        dsIndices = 0 until _size(grid)
        displayIndices = rows.intersect(dsIndices)
        for (i in displayIndices) {
            getFormattedRow(grid, _get(grid, i)).joinTo(this, prefix = "$i: ", postfix = "\n")
        }
        val andMore = dsIndices.size - displayIndices.size
        if (andMore > 0) {
            append("--and $andMore more\n")
        }
    } else {
        var rowsOutputted = 0
        for (i in rows) {
            val rowObject: T = getOrNull(grid, i) ?: break
            getFormattedRow(grid, rowObject).joinTo(this, prefix = "$i: ", postfix = "\n")
            rowsOutputted++
        }
        if (rowsOutputted == rows.size) {
            append("--and possibly more\n")
        } else {
            append("--\n")
        }
    }
}

private val _ColumnPathRenderer_provider: Field by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val f: Field = findClassOrThrow("com.vaadin.flow.component.grid.ColumnPathRenderer").getDeclaredField("provider")
    f.isAccessible = true
    f
}

@Suppress("UNCHECKED_CAST")
private fun <T> getValueProvider(renderer: ColumnPathRenderer<T>): ValueProvider<T, *> =
    _ColumnPathRenderer_provider.get(renderer) as (ValueProvider<T, *>)

/**
 * Simulates a user clicking on given row of given [grid]. Selects the item in
 * single selection mode (or deselects it when it is already selected), then
 * fires an [ItemClickEvent].
 *
 * @param rowIndex the row index, 0 or higher.
 * @param button the id of the pressed mouse button
 * @throws IllegalStateException if the grid was not editable.
 */
public fun <T : Any> _clickItem(grid: Grid<T>, rowIndex: Int, button: Int, ctrlKey: Boolean,
                                shiftKey: Boolean, altKey: Boolean, metaKey: Boolean) {
    checkEditableByUser(grid)
    // fire SelectionEvent if need be: https://github.com/mvysny/karibu-testing/issues/96
    val item: T = _get(grid, rowIndex)
    if (grid.selectionModel is GridSingleSelectionModel) {
        val selectedItem: T? = grid.selectedItems.firstOrNull()
        if (selectedItem != item) {
            grid.select(item)
        } else {
            // clicking on a selected item will de-select it.
            grid.deselectAll()
        }
    }
    val itemKey: String = grid.dataCommunicator.keyMapper.key(item)
    val event = ItemClickEvent<T>(grid, true, itemKey, null, -1, -1, -1, -1, 1, button, ctrlKey, shiftKey, altKey, metaKey)
    ComponentUtil.fireEvent(grid, event)
}

/**
 * Simulates a user double-clicking on given row of given [grid] by firing an
 * [ItemDoubleClickEvent].
 *
 * @param rowIndex the row index, 0 or higher.
 * @param button the id of the pressed mouse button
 * @throws IllegalStateException if the grid was not editable.
 */
public fun <T : Any> _doubleClickItem(grid: Grid<T>, rowIndex: Int, button: Int, ctrlKey: Boolean,
                                      shiftKey: Boolean, altKey: Boolean, metaKey: Boolean) {
    checkEditableByUser(grid)
    val itemKey: String = grid.dataCommunicator.keyMapper.key(_get(grid, rowIndex))
    val event = ItemDoubleClickEvent<T>(grid, true, itemKey, null, -1, -1, -1, -1, 2, button, ctrlKey, shiftKey, altKey, metaKey)
    ComponentUtil.fireEvent(grid, event)
}

/**
 * Returns a sequence which walks over all displayed rows of given [treeGrid]:
 * the children of collapsed nodes are skipped.
 */
private fun <T> rowSequence(treeGrid: TreeGrid<T>): Sequence<T> =
    rowSequence(treeGrid.dataProvider) { item: T -> treeGrid.isExpanded(item) }

private fun <T, F> rowSequence(dataProvider: HierarchicalDataProvider<T, F>, isExpanded: (T) -> Boolean): Sequence<T> {
    fun getChildrenOf(item: T?): List<T> = if (item == null || isExpanded(item)) {
        checkedFetch(dataProvider, HierarchicalQuery<T, F>(null, item))
    } else {
        listOf<T>()
    }

    fun itemSubtreeSequence(item: T): Sequence<T> =
            DepthFirstTreeIterator(item) { getChildrenOf(it) } .asSequence()

    val roots: List<T> = getChildrenOf(null)
    return roots.map { itemSubtreeSequence(it) } .asSequence().flatten()
}

private fun <T, F> checkedSize(dataProvider: HierarchicalDataProvider<T, F>, query: HierarchicalQuery<T, F>): Int {
    if (query.parent != null && !dataProvider.hasChildren(query.parent)) return 0
    val result: Int = dataProvider.size(query)
    check(result >= 0) { "size($query) returned negative count: $result" }
    return result
}

private fun <T, F> checkedFetch(dataProvider: HierarchicalDataProvider<T, F>, query: HierarchicalQuery<T, F>): List<T> = when {
    checkedSize(dataProvider, query) == 0 -> listOf()
    else -> dataProvider.fetchChildren(query).toList()
}

private fun <T : Any> dataSourceToPrettyTree(treeGrid: TreeGrid<T>): PrettyPrintTree {
    fun getChildrenOf(item: T?): List<T> =
            if (item == null || treeGrid.isExpanded(item)) {
                checkedFetch(treeGrid.dataProvider, HierarchicalQuery<T, SerializablePredicate<T>?>(null, item))
            } else {
                listOf<T>()
            }

    fun toPrettyTree(item: T): PrettyPrintTree {
        val self: String = getFormattedRow(treeGrid, item).joinToString(postfix = "\n")
        val children: List<T> = getChildrenOf(item)
        return PrettyPrintTree(self, children.map { toPrettyTree(it) } .toMutableList())
    }

    val roots: List<T> = getChildrenOf(null)
    return PrettyPrintTree("TreeGrid", roots.map { toPrettyTree(it) } .toMutableList())
}

/**
 * Returns the data provider currently set to given [component], or null if
 * the component has no data provider.
 */
public fun getDataProvider(component: Component): DataProvider<*, *>? = when (component) {
    // until https://github.com/vaadin/flow/issues/6296 is resolved
    is Grid<*> -> component.dataProvider
    is Select<*> -> component.dataProvider
    is ListBoxBase<*, *, *> -> component.dataProvider
    is RadioButtonGroup<*> -> component.dataProvider
    is CheckboxGroup<*> -> component.dataProvider
    is ComboBox<*> -> component.dataProvider
    else -> null
}

private val _Column_getInternalId: Method by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val m = Grid.Column::class.java.getDeclaredMethod("getInternalId")
    m.isAccessible = true
    m
}

private val _AbstractGridMultiSelectionModel_clientSelectAll: Method by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val m = AbstractGridMultiSelectionModel::class.java.getDeclaredMethod("clientSelectAll")
    m.isAccessible = true
    m
}

private val _AbstractGridMultiSelectionModel_clientDeselectAll: Method by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val m = AbstractGridMultiSelectionModel::class.java.getDeclaredMethod("clientDeselectAll")
    m.isAccessible = true
    m
}

private fun <T> getSelectAllCheckboxModel(grid: Grid<T>, action: String): GridMultiSelectionModel<T> {
    val model = grid.selectionModel
    if (model !is GridMultiSelectionModel) {
        throw IllegalStateException("$action requires multi selection mode")
    }
    if (!model.isSelectAllCheckboxVisible) {
        throw IllegalStateException("$action requires the select all checkbox to be visible")
    }
    return model
}

/**
 * Simulates a user selecting given [item] in given [grid]. In single selection
 * mode the previously selected item is deselected first.
 *
 * @throws IllegalStateException if the grid was not editable.
 */
public fun <T : Any> _select(grid: Grid<T>, item: T) {
    checkEditableByUser(grid)
    if (grid.selectionModel is GridSingleSelectionModel) {
        grid.deselectAll()
    }
    // fails properly if the Grid doesn't support selection.
    grid.selectionModel.selectFromClient(item)
}

/**
 * Simulates a user clicking the select all checkbox of given [grid].
 *
 * @throws IllegalStateException if the grid was not editable, is not in multi
 * selection mode or the select all checkbox is not visible.
 */
public fun <T> _selectAll(grid: Grid<T>) {
    checkEditableByUser(grid)
    val model = getSelectAllCheckboxModel(grid, "Select all")
    _AbstractGridMultiSelectionModel_clientSelectAll.invoke(model)
}

/**
 * Simulates a user deselecting given [item] in given [grid].
 *
 * @throws IllegalStateException if the grid was not editable or the item is
 * not selected.
 */
public fun <T : Any> _deselect(grid: Grid<T>, item: T) {
    checkEditableByUser(grid)
    if (grid.selectionModel !is GridNoneSelectionModel && !grid.selectionModel.isSelected(item)) {
        throw IllegalStateException("Can not deselect ${item}: the row is not selected")
    }
    // fails properly if the Grid doesn't support selection.
    grid.selectionModel.deselectFromClient(item)
}

/**
 * Simulates a user unchecking the select all checkbox of given [grid].
 *
 * @throws IllegalStateException if the grid was not editable, is not in multi
 * selection mode or the select all checkbox is not visible.
 */
public fun <T> _deselectAll(grid: Grid<T>) {
    checkEditableByUser(grid)
    val model = getSelectAllCheckboxModel(grid, "Deselect all")
    _AbstractGridMultiSelectionModel_clientDeselectAll.invoke(model)
}

/**
 * Returns the internal id of given [column], which the client uses to refer to
 * the column.
 */
public fun getInternalId(column: Grid.Column<*>): String =
    _Column_getInternalId.invoke(column) as String
