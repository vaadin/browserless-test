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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;

/**
 * Tester for TreeGrid components.
 * <p/>
 * Adds the hierarchy interactions a user can perform on top of everything
 * {@link GridTester} offers. Row indexes address the rows the user actually
 * sees, so children of collapsed nodes are not counted.
 * <p/>
 * The expand toggle lives in the hierarchy column, and a {@code TreeGrid} can
 * be configured with none, one, or several of them. {@link #expand(int)} and
 * {@link #collapse(int)} require at least one visible hierarchy column, since
 * without one the user has nothing to click. Several hierarchy columns render
 * several toggles for the same node, and clicking any of them has the same
 * effect, so they need no special handling here.
 *
 * @param <T>
 *            component type
 * @param <Y>
 *            item type
 * @since 25.3
 */
@Tests(fqn = { "com.vaadin.flow.component.treegrid.TreeGrid" })
public class TreeGridTester<T extends TreeGrid<Y>, Y> extends GridTester<T, Y> {

    /** Element the hierarchy column renders as the clickable expand toggle. */
    private static final String TREE_TOGGLE_ELEMENT = "vaadin-grid-tree-toggle";

    /** LitRenderer property the tree toggle renders as its label. */
    private static final String HIERARCHY_NAME_PROPERTY = "name";

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
     *             if the component is not usable, if the tree grid has no
     *             visible hierarchy column, if the row has no children and thus
     *             no expand toggle, or if the node is already expanded
     */
    public void expand(int row) {
        ensureComponentIsUsable();
        ensureHierarchyColumn();
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
     *             if the component is not usable, if the tree grid has no
     *             visible hierarchy column, if the row has no children and thus
     *             no collapse toggle, or if the node is not expanded
     */
    public void collapse(int row) {
        ensureComponentIsUsable();
        ensureHierarchyColumn();
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

    /**
     * Get the text that is shown on the client for the cell in the given
     * position.
     * <p/>
     * A hierarchy column added with
     * {@link TreeGrid#addHierarchyColumn(com.vaadin.flow.function.ValueProvider)}
     * renders the item through a {@code vaadin-grid-tree-toggle}, so its text
     * is read from the toggle's own value provider rather than from a column
     * path. Every other column is read exactly as {@link GridTester} does.
     *
     * @param row
     *            row of cell
     * @param column
     *            column of cell
     * @return cell content that is sent to the client
     * @throws IllegalStateException
     *             if component is not visible
     */
    @Override
    public String getCellText(int row, int column) {
        ensureVisible();
        final Grid.Column<Y> targetColumn = getColumns().get(column);
        final LitRenderer<Y> toggleRenderer = getTreeToggleRenderer(
                targetColumn);
        if (toggleRenderer != null) {
            // The toggle's "name" value provider already stringifies the item,
            // substituting an empty string for null.
            return (String) toggleRenderer.getValueProviders()
                    .get(HIERARCHY_NAME_PROPERTY).apply(getRow(row));
        }
        return super.getCellText(row, column);
    }

    private void ensureHasChildren(int row, Y item) {
        if (!getComponent().getDataCommunicator().hasChildren(item)) {
            throw new IllegalStateException("Node on row " + row
                    + " has no children, so it has no expand toggle to click");
        }
    }

    private void ensureHierarchyColumn() {
        if (getColumns().stream().noneMatch(this::isHierarchyColumn)) {
            throw new IllegalStateException(
                    "TreeGrid has no visible hierarchy column, so there is no "
                            + "expand toggle for the user to click. Add one "
                            + "with addHierarchyColumn or "
                            + "addComponentHierarchyColumn.");
        }
    }

    private boolean isHierarchyColumn(Grid.Column<Y> column) {
        return column.getRenderer() instanceof HierarchyColumnComponentRenderer
                || getTreeToggleRenderer(column) != null;
    }

    /**
     * Returns the renderer of a hierarchy column added through a value
     * provider, or {@literal null} when the column is not one. Such a column
     * renders a {@code vaadin-grid-tree-toggle} straight from a
     * {@link LitRenderer}; the component variant is a {@link ComponentRenderer}
     * instead and is handled by {@link GridTester}.
     */
    private LitRenderer<Y> getTreeToggleRenderer(Grid.Column<Y> column) {
        final Renderer<Y> renderer = column.getRenderer();
        if (renderer instanceof ComponentRenderer
                || !(renderer instanceof LitRenderer<Y> litRenderer)) {
            return null;
        }
        try {
            final String template = (String) getField(LitRenderer.class,
                    "templateExpression").get(litRenderer);
            return template != null && template.contains(TREE_TOGGLE_ELEMENT)
                    && litRenderer.getValueProviders()
                            .containsKey(HIERARCHY_NAME_PROPERTY) ? litRenderer
                                    : null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Unable to read the template of the column renderer", e);
        }
    }

}
