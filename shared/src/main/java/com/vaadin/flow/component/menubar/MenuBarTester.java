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
package com.vaadin.flow.component.menubar;

import java.util.List;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.browserless.internal.MenuItemNavigation;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.contextmenu.MenuItem;

/**
 * Tester for MenuBar components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(MenuBar.class)
public class MenuBarTester<T extends MenuBar> extends ComponentTester<T> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public MenuBarTester(T component) {
        super(component);
    }

    /**
     * Simulates a click on the item that matches the given text.
     *
     * For nested menu item provide the text of each menu item in the hierarchy.
     *
     * The path to the menu item must reflect what is seen in the browser,
     * meaning that hidden items are ignored.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * });
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Copy link", event -> {
     * });
     * subMenu.addItem("Email", event -> {
     * });
     *
     * // clicks top level menu item with text Preview
     * wrapper.clickItem("Preview");
     *
     * // clicks nested menu item with text Email
     * wrapper.clickItem("Share", "Email");
     * }
     * </pre>
     *
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}.
     * @param nestedItemsText
     *            text content of the nested menu items
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item.
     * @throws IllegalStateException
     *             if the item at given path is not usable.
     */
    public void clickItem(String topLevelText, String... nestedItemsText) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelText, nestedItemsText);
        clickMenuItem(menuItem);
    }

    /**
     * Simulates a click on the item at the given position in the menu.
     *
     * For nested menu item provide the position of each sub menu that should be
     * navigated to reach the request item.
     *
     * The position reflects what is seen in the browser, so hidden items are
     * ignored.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * });
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Copy link", event -> {
     * });
     * subMenu.addItem("Email", event -> {
     * });
     *
     * // clicks top level "Preview" menu item at position 0
     * wrapper.clickItem(0);
     *
     * // clicks then nested menu item at position 1 "Email" through the
     * // item "Share" at position 1
     * wrapper.clickItem(1, 1);
     * }
     * </pre>
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
     *             if the item at given position is not usable.
     */
    public void clickItem(int topLevelPosition, int... nestedItemsPositions) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelPosition,
                nestedItemsPositions);
        clickMenuItem(menuItem);
    }

    /**
     * Checks if the checkable menu item matching given text is checked.
     *
     * For nested menu item provide the text of each menu item in the hierarchy.
     *
     * The path to the menu item must reflect what is seen in the browser,
     * meaning that hidden items are ignored.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * }).setCheckable(true);
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Copy link", event -> {
     * }).setCheckable(true);
     * subMenu.addItem("Email", event -> {
     * }).setCheckable(true);
     *
     * wrapper.isItemChecked("Preview");
     *
     * wrapper.isItemChecked("Share", "Email");
     * }
     * </pre>
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
     *             if the item at given path is not usable.
     */
    public boolean isItemChecked(String topLevelText,
            String... nestedItemsText) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelText, nestedItemsText);
        MenuItemNavigation.requireCheckable(menuItem,
                MenuItemNavigation.pathToString(topLevelText, nestedItemsText));
        return menuItem.isChecked();
    }

    /**
     * Checks if the checkable menu item at given position is checked.
     *
     * For nested menu item provide the position of each sub menu that should be
     * navigated to reach the requested item.
     *
     * The position reflects what is seen in the browser, so hidden items are
     * ignored.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * }).setCheckable(true);
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Copy link", event -> {
     * }).setCheckable(true);
     * subMenu.addItem("Email", event -> {
     * }).setCheckable(true);
     *
     * // checks top level "Preview" menu item at position 0
     * wrapper.isItemChecked(0);
     *
     * // checks nested menu item at position 1 "Email" through the
     * // item "Share" at position 1
     * wrapper.isItemChecked(1, 1);
     * }
     * </pre>
     *
     * @param topLevelPosition
     *            the zero-based position of the item in the menu, as it will be
     *            seen in the browser.
     * @param nestedItemsPositions
     *            the zero-based position of the nested items, relative to the
     *            parent menu
     * @throws IllegalArgumentException
     *             if the provided position does not identify a menu item or if
     *             the menu item is not checkable.
     * @throws IllegalStateException
     *             if the item at given position is not usable.
     */
    public boolean isItemChecked(int topLevelPosition,
            int... nestedItemsPositions) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelPosition,
                nestedItemsPositions);
        MenuItemNavigation.requireCheckable(menuItem, MenuItemNavigation
                .pathToString(topLevelPosition, nestedItemsPositions));
        return menuItem.isChecked();
    }

    /**
     * Gets the tooltip text of the menu item matching the given text.
     *
     * For nested menu item provide the text of each menu item in the hierarchy.
     *
     * The path to the menu item must reflect what is seen in the browser,
     * meaning that hidden items are ignored.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * }).setTooltipText("Preview the document");
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Email", event -> {
     * }).setTooltipText("Send as email");
     *
     * wrapper.getItemTooltipText("Preview");
     *
     * wrapper.getItemTooltipText("Share", "Email");
     * }
     * </pre>
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
     *             if the item at given path is not usable.
     * @since 1.1
     */
    public String getItemTooltipText(String topLevelText,
            String... nestedItemsText) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelText, nestedItemsText);
        return menuItem.getElement().getProperty("tooltip");
    }

    /**
     * Gets the tooltip text of the menu item at the given position in the menu.
     *
     * For nested menu item provide the position of each sub menu that should be
     * navigated to reach the requested item.
     *
     * The position reflects what is seen in the browser, so hidden items are
     * ignored.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * }).setTooltipText("Preview the document");
     * var subMenu = menu.addItem("Share").getSubMenu();
     * subMenu.addItem("Email", event -> {
     * }).setTooltipText("Send as email");
     *
     * // gets the tooltip of the top level "Preview" item at position 0
     * wrapper.getItemTooltipText(0);
     *
     * // gets the tooltip of the nested "Email" item at position 0 through
     * // the item "Share" at position 1
     * wrapper.getItemTooltipText(1, 0);
     * }
     * </pre>
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
     *             if the item at given position is not usable.
     * @since 1.1
     */
    public String getItemTooltipText(int topLevelPosition,
            int... nestedItemsPositions) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelPosition,
                nestedItemsPositions);
        return menuItem.getElement().getProperty("tooltip");
    }

    /**
     * Gets the texts of the menu items, as the browser shows them.
     * <p>
     * Hidden items are ignored, so the returned texts are aligned with the
     * positions used by {@link #clickItem(int, int...)}. A text can also be
     * given to {@link #clickItem(String, String...)}, as long as it identifies
     * a single enabled item: a text that several visible items share is
     * ambiguous, and a disabled item cannot be clicked.
     * <p>
     * An item created from a component has no text of its own, and is reported
     * as an empty string. Use {@link #find(Class)} to reach such an item.
     * <p>
     * All items of the menu bar are reported. A browser collapses the items
     * that do not fit into an overflow menu, which cannot be simulated without
     * a layout.
     *
     * <pre>
     * {@code
     *
     * menu.addItem("Preview", event -> {
     * });
     * menu.addItem("Hidden", event -> {
     * }).setVisible(false);
     * menu.addItem("Share");
     *
     * // ["Preview", "Share"]
     * wrapper.getItemTexts();
     * }
     * </pre>
     *
     * @return the texts of the visible top level menu items, in the order they
     *         are shown in
     * @throws IllegalStateException
     *             if the menu bar is not visible
     * @since 25.3
     */
    public List<String> getItemTexts() {
        ensureVisible();
        return MenuItemNavigation.visibleTexts(getComponent().getItems());
    }

    /**
     * Gets the texts of the items of the sub menu of the item matching the
     * given text, as the browser shows them.
     * <p>
     * For a nested sub menu, provide the text of each menu item in the
     * hierarchy, the same way as in {@link #clickItem(String, String...)}.
     * <p>
     * Hidden items are ignored at every level, both when following the path and
     * in the returned texts.
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
     * // ["Copy link", "Email"]
     * wrapper.getItemTexts("Share");
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
     *             if the menu bar is not visible, if there are multiple
     *             matching items at any level, or if the item at the given path
     *             is disabled or not visible.
     * @since 25.3
     */
    public List<String> getItemTexts(String topLevelText,
            String... nestedItemsText) {
        ensureVisible();
        return MenuItemNavigation.visibleSubMenuTexts(getComponent().getItems(),
                topLevelText, nestedItemsText);
    }

    private MenuItem findMenuItemByPath(String topLevelText,
            String... nestedItemsText) {
        return MenuItemNavigation.findByPath(getComponent().getItems(),
                topLevelText, nestedItemsText);
    }

    private MenuItem findMenuItemByPath(int topLevelPosition,
            int... nestedItemsPositions) {
        return MenuItemNavigation.findByPath(getComponent().getItems(),
                topLevelPosition, nestedItemsPositions);
    }

    private void clickMenuItem(MenuItem menuItem) {
        if (menuItem.isCheckable()) {
            menuItem.setChecked(!menuItem.isChecked());
        }
        ComponentUtil.fireEvent(menuItem, new ClickEvent<>(menuItem, true, 0, 0,
                0, 0, 1, 0, false, false, false, false));
    }

}
