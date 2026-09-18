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
package com.vaadin.flow.component.grid.contextmenu;

import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.browserless.component.GridUtils;
import com.vaadin.browserless.internal.GridContextMenuSupport;
import com.vaadin.browserless.internal.MenuItemNavigation;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for GridContextMenu components.
 * <p/>
 * A grid context menu is always about a row: the user right-clicks a row, and
 * the events the menu fires report that row. {@link #open(int)} therefore takes
 * the row to open the menu on, addressed by its zero-based index among the rows
 * the user sees, the same way {@link com.vaadin.flow.component.grid.GridTester}
 * addresses rows.
 * <p/>
 * The menu content is not part of the UI until the menu opens, so
 * {@code clickItem(...)} and {@link #find(Class)} only see the items of an open
 * menu.
 *
 * @param <T>
 *            component type
 * @param <Y>
 *            item type
 */
@Tests(fqn = { "com.vaadin.flow.component.grid.contextmenu.GridContextMenu" })
public class GridContextMenuTester<T extends GridContextMenu<Y>, Y>
        extends ComponentTester<T> {

    /** Row {@link #open()} opens the menu on, {@literal null} for none. */
    private final Integer targetRow;

    /**
     * Wrap grid context menu for testing.
     * <p/>
     * The menu targets no row, so it has to be opened with {@link #open(int)}.
     *
     * @param component
     *            target grid context menu
     */
    public GridContextMenuTester(T component) {
        super(component);
        this.targetRow = null;
    }

    /**
     * Wrap grid context menu for testing, targeting the given row.
     * <p/>
     * The index is 0 based and counts the rows the user sees. Opening the menu
     * with {@link #open()} opens it on that row.
     *
     * @param component
     *            target grid context menu
     * @param row
     *            row the menu is about
     */
    public GridContextMenuTester(T component, int row) {
        super(component);
        this.targetRow = row;
    }

    /**
     * Opens the context menu on the row it currently targets, as if the user
     * had asked for it in the browser.
     * <p/>
     * The target row is the one given to
     * {@link com.vaadin.flow.component.grid.GridTester#contextMenu(int)}, so
     * this method is the counterpart of that entry point. Use
     * {@link #open(int)} to open the menu on a row directly.
     * <p/>
     * It does not render any client-side overlay, it only simulates the
     * server-side state changes that opening the menu produces: the menu is
     * attached to the UI, so its items become findable and clickable, and a
     * {@code GridContextMenuOpenedEvent} reporting the target row is fired.
     *
     * @throws IllegalStateException
     *             if the menu does not target a row, if it is already open, or
     *             if a dynamic content handler prevented it from opening
     */
    public void open() {
        if (targetRow == null) {
            throw new IllegalStateException(
                    "Context menu does not target a row. Open it on a row with open(int row), "
                            + "or get the tester from test(grid).contextMenu(int row).");
        }
        open(targetRow);
    }

    /**
     * Opens the context menu on the given row, as if the user had asked for it
     * in the browser.
     * <p/>
     * The index is 0 based and counts the rows the user sees. The row is
     * reported by the events the menu fires, so
     * {@code GridContextMenuItemClickEvent.getItem()} and
     * {@code GridContextMenuOpenedEvent.getItem()} hold the item on that row.
     *
     * @param row
     *            row to open the menu on
     * @throws IllegalStateException
     *             if the menu is already open, or if a dynamic content handler
     *             prevented it from opening
     * @throws IndexOutOfBoundsException
     *             if the grid has no such row
     */
    public void open(int row) {
        open(row, null);
    }

    /**
     * Opens the context menu on the given row and column, as if the user had
     * asked for it in the browser.
     * <p/>
     * The column is reported by
     * {@code GridContextMenuOpenedEvent.getColumnId()}.
     *
     * @param row
     *            row to open the menu on, 0 based
     * @param columnKey
     *            key of the column to open the menu on, as set with
     *            {@code Grid.Column.setKey(String)}
     * @throws IllegalArgumentException
     *             if the grid has no column with the given key
     * @throws IllegalStateException
     *             if the column is not visible, if the menu is already open, or
     *             if a dynamic content handler prevented it from opening
     * @throws IndexOutOfBoundsException
     *             if the grid has no such row
     */
    public void open(int row, String columnKey) {
        if (getComponent().isOpened()) {
            throw new IllegalStateException("Context menu is already open");
        }
        Grid<Y> grid = getGrid();
        String itemKey = GridContextMenuSupport.getItemKey(grid,
                GridUtils._get(grid, row));
        String columnId = columnKey == null ? null
                : GridContextMenuSupport.getColumnInternalId(grid, columnKey);
        GridContextMenuSupport.setTargetItem(grid, itemKey, columnId);
        requestMenu(itemKey);
        roundTrip();
        if (!getComponent().isAttached()) {
            throw new IllegalStateException(
                    "Context menu did not open. Its dynamic content handler returned false for the target row.");
        }
        ensureComponentIsUsableOrDetach();
        // opened is a synchronized property, so pushing it through the
        // client path makes the GridContextMenuOpenedEvent report
        // isFromClient() as true, the way a real open does
        setPropertyAsUser("opened", true);
    }

    /**
     * Closes the context menu.
     *
     * @throws IllegalStateException
     *             if the menu is not open
     */
    public void close() {
        ensureComponentIsUsable();
        setPropertyAsUser("opened", false);
    }

    /**
     * Simulates a click on the item that matches the given text.
     * <p/>
     * For a nested menu item, provide the text of each menu item in the
     * hierarchy.
     * <p/>
     * The path to the menu item must reflect what is seen in the browser,
     * meaning that hidden items are ignored. If there are multiple visible
     * items at the same level with the same text, an
     * {@link IllegalStateException} is thrown because the target is ambiguous.
     * Disabled or invisible items cannot be clicked and will also cause an
     * {@link IllegalStateException}.
     * <p/>
     * The menu has to be open, since its items are not part of the UI before
     * that.
     *
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}.
     * @param nestedItemsText
     *            text content of the nested menu items
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item.
     * @throws IllegalStateException
     *             if the menu is not open, if there are multiple visible
     *             matching items at any level, or if the item at the given path
     *             is disabled or not visible.
     */
    public void clickItem(String topLevelText, String... nestedItemsText) {
        ensureComponentIsUsable();
        clickMenuItem(findMenuItemByPath(topLevelText, nestedItemsText));
    }

    /**
     * Simulates a click on the item at the given position in the menu.
     * <p/>
     * For a nested menu item, provide the position of each sub menu that should
     * be navigated to reach the requested item.
     * <p/>
     * Positions are zero-based and refer only to items that are visible at each
     * menu level, i.e. hidden items are ignored (the same way as in the
     * browser). Disabled or invisible items cannot be clicked and will cause an
     * {@link IllegalStateException}.
     * <p/>
     * The menu has to be open, since its items are not part of the UI before
     * that.
     *
     * @param topLevelPosition
     *            the zero-based position of the item in the menu, as it will be
     *            seen in the browser.
     * @param nestedItemsPositions
     *            the zero-based position of the nested items, relative to the
     *            parent menu
     * @throws IllegalArgumentException
     *             if the provided position does not identify a menu item.
     * @throws IllegalStateException
     *             if the menu is not open, or if the item at the given position
     *             is disabled or not visible.
     */
    public void clickItem(int topLevelPosition, int... nestedItemsPositions) {
        ensureComponentIsUsable();
        clickMenuItem(
                findMenuItemByPath(topLevelPosition, nestedItemsPositions));
    }

    /**
     * Checks if the checkable menu item matching given text is checked.
     * <p/>
     * For a nested menu item, provide the text of each menu item in the
     * hierarchy.
     *
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}.
     * @param nestedItemsText
     *            text content of the nested menu items
     * @return {@literal true} if the item at given path is checked, otherwise
     *         {@literal false}.
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item or if the
     *             menu item is not checkable.
     * @throws IllegalStateException
     *             if the menu is not open, or if the item at given path is not
     *             usable.
     */
    public boolean isItemChecked(String topLevelText,
            String... nestedItemsText) {
        ensureComponentIsUsable();
        GridMenuItem<Y> menuItem = findMenuItemByPath(topLevelText,
                nestedItemsText);
        MenuItemNavigation.requireCheckable(menuItem,
                MenuItemNavigation.pathToString(topLevelText, nestedItemsText));
        return menuItem.isChecked();
    }

    /**
     * Checks if the checkable menu item at given position is checked.
     * <p/>
     * For a nested menu item, provide the position of each sub menu that should
     * be navigated to reach the requested item.
     *
     * @param topLevelPosition
     *            the zero-based position of the item in the menu, as it will be
     *            seen in the browser.
     * @param nestedItemsPositions
     *            the zero-based position of the nested items, relative to the
     *            parent menu
     * @return {@literal true} if the item at given position is checked,
     *         otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the provided position does not identify a menu item or if
     *             the menu item is not checkable.
     * @throws IllegalStateException
     *             if the menu is not open, or if the item at given position is
     *             not usable.
     */
    public boolean isItemChecked(int topLevelPosition,
            int... nestedItemsPositions) {
        ensureComponentIsUsable();
        GridMenuItem<Y> menuItem = findMenuItemByPath(topLevelPosition,
                nestedItemsPositions);
        MenuItemNavigation.requireCheckable(menuItem, MenuItemNavigation
                .pathToString(topLevelPosition, nestedItemsPositions));
        return menuItem.isChecked();
    }

    /**
     * Gets the tooltip text of the menu item matching the given text.
     * <p/>
     * For a nested menu item, provide the text of each menu item in the
     * hierarchy.
     *
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}.
     * @param nestedItemsText
     *            text content of the nested menu items
     * @return the tooltip text of the menu item at given path, or
     *         {@literal null} if the item has no tooltip set.
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item.
     * @throws IllegalStateException
     *             if the menu is not open, or if the item at given path is not
     *             usable.
     */
    public String getItemTooltipText(String topLevelText,
            String... nestedItemsText) {
        ensureComponentIsUsable();
        return findMenuItemByPath(topLevelText, nestedItemsText).getElement()
                .getProperty("tooltip");
    }

    /**
     * Gets the tooltip text of the menu item at the given position in the menu.
     * <p/>
     * For a nested menu item, provide the position of each sub menu that should
     * be navigated to reach the requested item.
     *
     * @param topLevelPosition
     *            the zero-based position of the item in the menu, as it will be
     *            seen in the browser.
     * @param nestedItemsPositions
     *            the zero-based position of the nested items, relative to the
     *            parent menu
     * @return the tooltip text of the menu item at given position, or
     *         {@literal null} if the item has no tooltip set.
     * @throws IllegalArgumentException
     *             if the provided position does not identify a menu item.
     * @throws IllegalStateException
     *             if the menu is not open, or if the item at given position is
     *             not usable.
     */
    public String getItemTooltipText(int topLevelPosition,
            int... nestedItemsPositions) {
        ensureComponentIsUsable();
        return findMenuItemByPath(topLevelPosition, nestedItemsPositions)
                .getElement().getProperty("tooltip");
    }

    /**
     * Gets the texts of the menu items, as the browser shows them.
     * <p/>
     * Hidden items are ignored, so the returned texts are aligned with the
     * positions used by {@link #clickItem(int, int...)}. A text can also be
     * given to {@link #clickItem(String, String...)}, as long as it identifies
     * a single enabled item: a text that several visible items share is
     * ambiguous, and a disabled item cannot be clicked.
     * <p/>
     * An item created from a component has no text of its own, and is reported
     * as an empty string. Use {@link #find(Class)} to reach such an item.
     * <p/>
     * The menu has to be open, since its items are not part of the UI before
     * that. The items are the ones the menu offers for the row it was opened
     * on, so a dynamic content handler has run by then.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Edit", event -> {
     * });
     * menu.addItem("Hidden", event -> {
     * }).setVisible(false);
     * menu.addItem("Share");
     *
     * tester.open(0);
     *
     * // ["Edit", "Share"]
     * tester.getItemTexts();
     * }
     * </pre>
     *
     * @return the texts of the visible top level menu items, in the order they
     *         are shown in
     * @throws IllegalStateException
     *             if the menu is not open, or is not visible
     */
    public List<String> getItemTexts() {
        ensureVisible();
        return MenuItemNavigation.visibleTexts(getComponent().getItems());
    }

    /**
     * Gets the texts of the items of the sub menu of the item matching the
     * given text, as the browser shows them.
     * <p/>
     * For a nested sub menu, provide the text of each menu item in the
     * hierarchy, the same way as in {@link #clickItem(String, String...)}.
     * <p/>
     * Hidden items are ignored at every level, both when following the path and
     * in the returned texts.
     * <p/>
     * The menu has to be open, since its items are not part of the UI before
     * that.
     *
     * <pre>
     * {@code
     *
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Copy link", event -> {
     * });
     * subMenu.addItem("Email", event -> {
     * });
     *
     * tester.open(0);
     *
     * // ["Copy link", "Email"]
     * tester.getItemTexts("Share");
     * }
     * </pre>
     *
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}.
     * @param nestedItemsText
     *            text content of the nested menu items
     * @return the texts of the visible items of the sub menu, in the order they
     *         are shown in
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item, or if the
     *             item at the given path has no sub menu.
     * @throws IllegalStateException
     *             if the menu is not open or not visible, if there are multiple
     *             matching items at any level, or if the item at the given path
     *             is disabled or not visible.
     */
    public List<String> getItemTexts(String topLevelText,
            String... nestedItemsText) {
        ensureVisible();
        return MenuItemNavigation.visibleSubMenuTexts(getComponent().getItems(),
                topLevelText, nestedItemsText);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Can be used to find components in the context menu. The menu content is
     * only part of the UI while the menu is open, so components are returned in
     * a detached state unless {@link #open(int)} has been called previously.
     *
     * <pre>
     * // view:
     * GridContextMenu&lt;Person&gt; menu = grid.addContextMenu();
     * menu.addItem(new Checkbox("Show inactive"), event -> {
     * });
     *
     * // test:
     * GridContextMenuTester&lt;GridContextMenu&lt;Person&gt;, Person&gt; menuTester = test(
     *         view.menu);
     * menuTester.open(0);
     * Checkbox checkbox = menuTester.find(Checkbox.class).single();
     * </pre>
     */
    @Override
    public <R extends Component> ComponentQuery<R> find(
            Class<R> componentType) {
        return super.find(componentType);
    }

    @SuppressWarnings("unchecked")
    private Grid<Y> getGrid() {
        Component target = getComponent().getTarget();
        if (!(target instanceof Grid)) {
            throw new IllegalStateException(
                    "Context menu is not attached to a grid");
        }
        return (Grid<Y>) target;
    }

    /**
     * Fires the DOM event the grid sends when the user asks for the context
     * menu. It attaches the menu to the UI, unless a dynamic content handler
     * decides that the menu should not open for the target row.
     */
    private void requestMenu(String itemKey) {
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("key", itemKey);
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.set("event.detail", detail);
        fireDomEvent(new DomEvent(getGrid().getElement(),
                GridContextMenuSupport.BEFORE_OPEN_EVENT, eventData));
    }

    private GridMenuItem<Y> findMenuItemByPath(String topLevelText,
            String... nestedItemsText) {
        return MenuItemNavigation.findByPath(getComponent().getItems(),
                topLevelText, nestedItemsText);
    }

    private GridMenuItem<Y> findMenuItemByPath(int topLevelPosition,
            int... nestedItemsPositions) {
        return MenuItemNavigation.findByPath(getComponent().getItems(),
                topLevelPosition, nestedItemsPositions);
    }

    /**
     * Clicks the item the way the browser does. A {@code GridMenuItem} listens
     * to the DOM click event rather than to a Flow {@code ClickEvent}, so
     * firing a click event on the item would not run its listeners.
     */
    private void clickMenuItem(GridMenuItem<Y> menuItem) {
        fireDomEvent(new DomEvent(menuItem.getElement(), "click",
                JacksonUtils.createObjectNode()));
    }
}
