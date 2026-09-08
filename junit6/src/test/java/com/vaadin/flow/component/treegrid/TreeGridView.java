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
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider.HierarchyFormat;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Route;

/**
 * View with a tree grid holding a two-level hierarchy plus one leaf root:
 *
 * <pre>
 * Root A
 *   Child A1
 *     Grandchild A1a
 *   Child A2
 * Root B
 * </pre>
 */
@Tag("div")
@Route(value = "tree-grid", registerAtStartup = false)
public class TreeGridView extends Component implements HasComponents {

    static final String ROOT_A = "Root A";
    static final String ROOT_B = "Root B";
    static final String CHILD_A1 = "Child A1";
    static final String CHILD_A2 = "Child A2";
    static final String GRANDCHILD_A1A = "Grandchild A1a";

    final TreeGrid<String> treeGrid;

    public TreeGridView() {
        treeGrid = new TreeGrid<>();
        treeGrid.addHierarchyColumn(item -> item).setKey("name")
                .setHeader("Name");

        TreeData<String> treeData = new TreeData<>();
        treeData.addRootItems(ROOT_A, ROOT_B);
        treeData.addItems(ROOT_A, List.of(CHILD_A1, CHILD_A2));
        treeData.addItems(CHILD_A1, List.of(GRANDCHILD_A1A));
        treeGrid.setDataProvider(
                new TreeDataProvider<>(treeData, HierarchyFormat.NESTED));

        add(treeGrid);
    }

}
