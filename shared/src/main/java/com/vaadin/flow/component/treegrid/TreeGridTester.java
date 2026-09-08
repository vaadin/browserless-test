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

import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.grid.GridTester;

/**
 * Tester for TreeGrid components.
 * <p/>
 * Adds the hierarchy interactions a user can perform on top of everything
 * {@link GridTester} offers. Row indexes address the rows the user actually
 * sees, so children of collapsed nodes are not counted.
 *
 * @param <T>
 *            component type
 * @param <Y>
 *            item type
 */
@Tests(fqn = { "com.vaadin.flow.component.treegrid.TreeGrid" })
public class TreeGridTester<T extends TreeGrid<Y>, Y> extends GridTester<T, Y> {

    /**
     * Wrap tree grid for testing.
     *
     * @param component
     *            target tree grid
     */
    public TreeGridTester(T component) {
        super(component);
    }

    /**
     * Expands the node on the given row, as if the user clicked its expand
     * toggle in the browser.
     * <p/>
     * The index is 0 based and counts only rows that are currently displayed,
     * so children of collapsed nodes are skipped.
     * <p/>
     * The resulting {@link ExpandEvent} reports
     * {@link ExpandEvent#isFromClient()} as {@literal true}.
     *
     * @param row
     *            row of the node to expand
     * @throws IllegalStateException
     *             if the component is not usable, if the row has no children
     *             and thus no expand toggle, or if the node is already expanded
     */
    public void expand(int row) {
        ensureComponentIsUsable();
        final Y item = getRow(row);
        ensureHasChildren(row, item);
        if (getComponent().isExpanded(item)) {
            throw new IllegalStateException(
                    "Node on row " + row + " is already expanded");
        }
        getComponent().expand(List.of(item), true);
        roundTrip();
    }

    /**
     * Collapses the node on the given row, as if the user clicked its collapse
     * toggle in the browser.
     * <p/>
     * The index is 0 based and counts only rows that are currently displayed,
     * so children of collapsed nodes are skipped.
     * <p/>
     * The resulting {@link CollapseEvent} reports
     * {@link CollapseEvent#isFromClient()} as {@literal true}.
     *
     * @param row
     *            row of the node to collapse
     * @throws IllegalStateException
     *             if the component is not usable, if the row has no children
     *             and thus no collapse toggle, or if the node is not expanded
     */
    public void collapse(int row) {
        ensureComponentIsUsable();
        final Y item = getRow(row);
        ensureHasChildren(row, item);
        if (!getComponent().isExpanded(item)) {
            throw new IllegalStateException(
                    "Node on row " + row + " is not expanded");
        }
        getComponent().collapse(List.of(item), true);
        roundTrip();
    }

    /**
     * Checks whether the node on the given row is expanded.
     * <p/>
     * The index is 0 based and counts only rows that are currently displayed,
     * so children of collapsed nodes are skipped.
     *
     * @param row
     *            row to check
     * @return {@literal true} if the node on the row is expanded, otherwise
     *         {@literal false}
     * @throws IllegalStateException
     *             if the component is not visible
     */
    public boolean isExpanded(int row) {
        ensureVisible();
        return getComponent().isExpanded(getRow(row));
    }

    /**
     * Checks whether the node on the given row has children, meaning the user
     * is shown an expand toggle for it.
     * <p/>
     * The index is 0 based and counts only rows that are currently displayed,
     * so children of collapsed nodes are skipped.
     *
     * @param row
     *            row to check
     * @return {@literal true} if the node on the row has children, otherwise
     *         {@literal false}
     * @throws IllegalStateException
     *             if the component is not visible
     */
    public boolean hasChildren(int row) {
        ensureVisible();
        return getComponent().getDataCommunicator().hasChildren(getRow(row));
    }

    private void ensureHasChildren(int row, Y item) {
        if (!getComponent().getDataCommunicator().hasChildren(item)) {
            throw new IllegalStateException("Node on row " + row
                    + " has no children, so it has no expand toggle to click");
        }
    }

}
