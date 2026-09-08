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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class TreeGridTesterTest extends BrowserlessTest {

    TreeGridView view;
    TreeGridTester<TreeGrid<String>, String> treeGrid_;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(TreeGridView.class);

        view = navigate(TreeGridView.class);
        treeGrid_ = test(view.treeGrid);
    }

    @Test
    void treeGrid_resolvesToTreeGridTester() {
        Grid<String> asGrid = view.treeGrid;
        Assertions.assertInstanceOf(TreeGridTester.class, test(asGrid),
                "TreeGrid should resolve to the TreeGrid specific tester");
    }

    @Test
    void expand_childRowsBecomeVisible_eventIsFromClient() {
        List<ExpandEvent<String, TreeGrid<String>>> events = new ArrayList<>();
        view.treeGrid.addExpandListener(events::add);

        Assertions.assertEquals(2, treeGrid_.size(),
                "only the root items should be displayed initially");

        treeGrid_.expand(0);

        Assertions.assertEquals(4, treeGrid_.size());
        Assertions.assertEquals(TreeGridView.CHILD_A1, treeGrid_.getRow(1));
        Assertions.assertEquals(TreeGridView.CHILD_A2, treeGrid_.getRow(2));
        Assertions.assertEquals(TreeGridView.ROOT_B, treeGrid_.getRow(3));

        Assertions.assertEquals(1, events.size());
        Assertions.assertTrue(events.get(0).isFromClient(),
                "ExpandEvent should be reported as coming from the client");
        Assertions.assertEquals(List.of(TreeGridView.ROOT_A),
                List.copyOf(events.get(0).getItems()));
    }

    @Test
    void expand_nestedRow_rowIndexesFollowTheDisplayedRows() {
        treeGrid_.expand(0);
        treeGrid_.expand(1);

        Assertions.assertEquals(5, treeGrid_.size());
        Assertions.assertEquals(TreeGridView.GRANDCHILD_A1A,
                treeGrid_.getRow(2));
        Assertions.assertEquals(TreeGridView.CHILD_A2, treeGrid_.getRow(3));
    }

    @Test
    void collapse_childRowsHidden_eventIsFromClient() {
        List<CollapseEvent<String, TreeGrid<String>>> events = new ArrayList<>();
        view.treeGrid.addCollapseListener(events::add);

        treeGrid_.expand(0);
        treeGrid_.collapse(0);

        Assertions.assertEquals(2, treeGrid_.size());
        Assertions.assertEquals(TreeGridView.ROOT_B, treeGrid_.getRow(1));

        Assertions.assertEquals(1, events.size());
        Assertions.assertTrue(events.get(0).isFromClient(),
                "CollapseEvent should be reported as coming from the client");
        Assertions.assertEquals(List.of(TreeGridView.ROOT_A),
                List.copyOf(events.get(0).getItems()));
    }

    @Test
    void isExpandedAndHasChildren_reflectRowState() {
        Assertions.assertFalse(treeGrid_.isExpanded(0));
        Assertions.assertTrue(treeGrid_.hasChildren(0));
        Assertions.assertFalse(treeGrid_.hasChildren(1),
                "Root B is a leaf and should have no expand toggle");

        treeGrid_.expand(0);

        Assertions.assertTrue(treeGrid_.isExpanded(0));
        Assertions.assertTrue(treeGrid_.hasChildren(1),
                "Child A1 has a child of its own");
        Assertions.assertFalse(treeGrid_.isExpanded(1));
        Assertions.assertFalse(treeGrid_.hasChildren(2),
                "Child A2 is a leaf and should have no expand toggle");
    }

    @Test
    void expandOrCollapseLeafRow_throws() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> treeGrid_.expand(1),
                "a leaf row has no expand toggle to click");
        Assertions.assertThrows(IllegalStateException.class,
                () -> treeGrid_.collapse(1),
                "a leaf row has no collapse toggle to click");
    }

    @Test
    void toggleRowAlreadyInTargetState_throws() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> treeGrid_.collapse(0),
                "collapsing a collapsed node is not something a user can do");

        treeGrid_.expand(0);

        Assertions.assertThrows(IllegalStateException.class,
                () -> treeGrid_.expand(0),
                "expanding an expanded node is not something a user can do");
    }

    @Test
    void expandOrCollapseHiddenTreeGrid_throws() {
        treeGrid_.expand(0);
        view.treeGrid.setVisible(false);

        Assertions.assertThrows(IllegalStateException.class,
                () -> treeGrid_.expand(1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> treeGrid_.collapse(0));
    }

}
