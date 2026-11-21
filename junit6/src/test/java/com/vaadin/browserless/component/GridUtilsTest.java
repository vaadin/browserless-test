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

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GridUtilsTest {

    @Test
    void get_negativeRow_throws() {
        Grid<String> grid = new Grid<>();
        grid.setItems("a");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> GridUtils._get(grid, -1));
        assertEquals("rowIndex must be 0 or greater: -1", error.getMessage());
    }

    @Test
    void get_rowPastTheEnd_failsWithTheFirstRowsOfTheGrid() {
        Grid<String> grid = new Grid<>();
        Grid.Column<String> name = grid.addColumn(s -> s).setHeader("Name")
                .setComparator(s -> s);
        grid.addColumn(String::length).setHeader("Length");
        grid.addColumn(String::toUpperCase).setHeader("Hidden")
                .setVisible(false);
        grid.setItems(
                IntStream.range(0, 12).mapToObj(i -> "item" + i).toList());
        grid.sort(GridSortOrder.asc(name).build());

        AssertionError error = assertThrows(AssertionError.class,
                () -> GridUtils._get(grid, 12));

        assertEquals("""
                Requested to get row 12 but the data provider only has 12 rows
                --[Name]v-[Length]--
                0: item0, 5
                1: item1, 5
                2: item10, 6
                3: item11, 6
                4: item2, 5
                5: item3, 5
                6: item4, 5
                7: item5, 5
                8: item6, 5
                9: item7, 5
                --and 2 more
                """, error.getMessage());
    }

    @Test
    void get_rowPastTheEndOfALazyGrid_failsWithTheRowsItHas() {
        Grid<String> grid = new Grid<>();
        grid.addColumn(s -> s).setHeader("Name");
        List<String> items = List.of("a", "b", "c");
        grid.setItems(query -> items.stream().skip(query.getOffset())
                .limit(query.getLimit()));

        AssertionError error = assertThrows(AssertionError.class,
                () -> GridUtils._get(grid, 5));

        assertEquals("""
                Requested to get row 5 but the data provider returned 0 rows
                --[Name]--
                0: a
                1: b
                2: c
                --
                """, error.getMessage());
    }

    @Test
    void get_rowPastTheEndOfALongLazyGrid_failsWithTheFirstRows() {
        Grid<String> grid = new Grid<>();
        Grid.Column<String> name = grid.addColumn(s -> s).setHeader("Name")
                .setSortable(true);
        List<String> items = IntStream.range(0, 12).mapToObj(i -> "item" + i)
                .toList();
        grid.setItems(query -> items.stream().skip(query.getOffset())
                .limit(query.getLimit()));
        grid.sort(GridSortOrder.desc(name).build());

        AssertionError error = assertThrows(AssertionError.class,
                () -> GridUtils._get(grid, 20));

        assertEquals("""
                Requested to get row 20 but the data provider returned 0 rows
                --[Name]^--
                0: item0
                1: item1
                2: item2
                3: item3
                4: item4
                5: item5
                6: item6
                7: item7
                8: item8
                9: item9
                --and possibly more
                """, error.getMessage());
    }

    @Test
    void get_rowPastTheEndOfATreeGrid_failsWithTheExpandedTree() {
        TreeGrid<String> treeGrid = new TreeGrid<>();
        treeGrid.addHierarchyColumn(s -> s).setHeader("Name");
        TreeData<String> data = new TreeData<>();
        data.addRootItems("root", "collapsed");
        data.addItem("root", "child");
        data.addItem("collapsed", "hidden");
        treeGrid.setDataProvider(new TreeDataProvider<>(data));
        treeGrid.expand("root");

        assertEquals(3, GridUtils._size(treeGrid));
        assertEquals("child", GridUtils._get(treeGrid, 1));
        AssertionError error = assertThrows(AssertionError.class,
                () -> GridUtils._get(treeGrid, 3));

        assertEquals("""
                Requested to get row 3 but the data provider returned 0 rows
                --[Name]--
                0:     ├── root
                1:     │   └── child
                2:     └── collapsed
                """, error.getMessage());
    }
}
