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
package com.vaadin.flow.component.treegrid;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider.HierarchyFormat;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Route;

/**
 * View with tree grids over a two-level hierarchy plus one leaf root:
 *
 * <pre>
 * Root A
 *   Child A1
 *     Grandchild A1a
 *   Child A2
 * Root B
 * </pre>
 *
 * The same data is shown three ways, so that the tester can be exercised
 * against a tree grid with one, zero and several hierarchy columns.
 */
@Tag("div")
@Route(value = "tree-grid", registerAtStartup = false)
public class TreeGridView extends Component implements HasComponents {

    static final String ROOT_A = "Root A";
    static final String ROOT_B = "Root B";
    static final String CHILD_A1 = "Child A1";
    static final String CHILD_A2 = "Child A2";
    static final String GRANDCHILD_A1A = "Grandchild A1a";

    static final String NAME_KEY = "name";
    static final String UPPERCASE_KEY = "uppercase";
    static final String LENGTH_KEY = "length";

    /** One hierarchy column, as most applications have. */
    final TreeGrid<String> treeGrid;

    /** No hierarchy column at all, so the user is shown no expand toggle. */
    final TreeGrid<String> noHierarchyColumnTreeGrid;

    /** Two hierarchy columns, one text based and one component based. */
    final TreeGrid<String> multiHierarchyColumnTreeGrid;

    public TreeGridView() {
        treeGrid = new TreeGrid<>();
        treeGrid.addHierarchyColumn(item -> item).setKey(NAME_KEY)
                .setHeader("Name");
        treeGrid.addColumn(String::length).setKey(LENGTH_KEY)
                .setHeader("Length");
        setItems(treeGrid);

        noHierarchyColumnTreeGrid = new TreeGrid<>();
        noHierarchyColumnTreeGrid.addColumn(item -> item).setKey(NAME_KEY)
                .setHeader("Name");
        setItems(noHierarchyColumnTreeGrid);

        multiHierarchyColumnTreeGrid = new TreeGrid<>();
        multiHierarchyColumnTreeGrid.addHierarchyColumn(item -> item)
                .setKey(NAME_KEY).setHeader("Name");
        multiHierarchyColumnTreeGrid
                .addComponentHierarchyColumn(
                        item -> new Span(item.toUpperCase()))
                .setKey(UPPERCASE_KEY).setHeader("Uppercase");
        setItems(multiHierarchyColumnTreeGrid);

        add(treeGrid, noHierarchyColumnTreeGrid, multiHierarchyColumnTreeGrid);
    }

    private static void setItems(TreeGrid<String> target) {
        TreeData<String> treeData = new TreeData<>();
        treeData.addRootItems(ROOT_A, ROOT_B);
        treeData.addItems(ROOT_A, List.of(CHILD_A1, CHILD_A2));
        treeData.addItems(CHILD_A1, List.of(GRANDCHILD_A1A));
        target.setDataProvider(
                new TreeDataProvider<>(treeData, HierarchyFormat.NESTED));
    }

}
