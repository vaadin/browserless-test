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
package com.vaadin.flow.component.contextmenu;

import java.util.List;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.browserless.internal.MenuItemNavigation;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for ContextMenu components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(ContextMenu.class)
public class ContextMenuTester<T extends ContextMenu>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public ContextMenuTester(T component) {
        super(component);
    }

    /**
     * Opens the context menu, as if the action is done in the browser.
     * <p>
     * It simulates, for example, a right click on a UI component with an
     * assigned {@link ContextMenu}.
     * <p>
     * It does not render any client-side overlay. In other words, it only
     * simulates the server-side state changes that would occur when a user
     * opens the menu in the browser.
     * <p>
     * A closed context menu is not attached to the UI, so, exactly as in the
     * browser, its items cannot be interacted with: open the menu before
     * calling {@code clickItem(...)}, {@code isItemChecked(...)} or
     * {@code getItemTooltipText(...)}, otherwise they throw an
     * {@link IllegalStateException}. Only {@code find(Class)} works on a closed
     * menu, since it queries the menu contents instead of the UI; the
     * components it returns are detached until the menu is opened.
     * <p>
     * A top level {@code find(...)} on the UI is a different matter: the menu
     * content is attached to the UI only while the menu is open, so it is found
     * after {@link #open()} and not before.
     *
     * @throws IllegalStateException
     *             if the menu is already opened.
     */
    public void open() {
        if (getComponent().isOpened()) {
            throw new IllegalStateException("Context menu is already open");
        }
        attachMenuToUI();
        roundTrip();
        ensureComponentIsUsableOrDetach();
        // Simulate the overlay reporting itself as opened so that the
        // resulting OpenedChangeEvent is seen as a user action.
        setPropertyAsUser("opened", true);
    }

    /**
     * Closes the context menu.
     */
    public void close() {
        ensureComponentIsUsable();
        setPropertyAsUser("opened", false);
    }

    /**
     * Simulates a click on the item that matches the given text.
     *
     * For a nested menu item, provide the text of each menu item in the
     * hierarchy.
     *
     * The path to the menu item must reflect what is seen in the browser,
     * meaning that hidden items are ignored. If there are multiple visible
     * items at the same level with the same text, an
     * {@link IllegalStateException} is thrown because the target is ambiguous.
     * Disabled or invisible items cannot be clicked and will also cause an
     * {@link IllegalStateException}.
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
     * tester.clickItem("Preview");
     *
     * // clicks nested menu item with text Email
     * tester.clickItem("Share", "Email");
     * }
     * </pre>
     *
     * Note: the menu must be opened with {@link #open()} before an item can be
     * clicked, since a closed menu is not attached to the UI.
     *
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}.
     * @param nestedItemsText
     *            text content of the nested menu items
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item.
     * @throws IllegalStateException
     *             if the menu is not opened, if there are multiple visible
     *             matching items at any level, or if the item at the given path
     *             is disabled or not visible.
     */
    public void clickItem(String topLevelText, String... nestedItemsText) {
        ensureComponentIsUsable();
        MenuItem menuItem = findMenuItemByPath(topLevelText, nestedItemsText);
        clickMenuItem(menuItem);
    }

    /**
     * Simulates a click on the item at the given position in the menu.
     *
     * For a nested menu item, provide the position of each sub menu that should
     * be navigated to reach the requested item.
     *
     * Positions are zero-based and refer only to items that are visible at each
     * menu level, i.e. hidden items are ignored (the same way as in the
     * browser). Disabled or invisible items cannot be clicked and will cause an
     * {@link IllegalStateException}.
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
     * tester.clickItem(0);
     *
     * // clicks then nested menu item at position 1 "Email" through the
     * // item "Share" at position 1
     * tester.clickItem(1, 1);
     * }
     * </pre>
     *
     * Note: the menu must be opened with {@link #open()} before an item can be
     * clicked, since a closed menu is not attached to the UI.
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
     *             if the menu is not opened, or if the item at the given
     *             position is disabled or not visible.
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
     * tester.isItemChecked("Preview");
     *
     * tester.isItemChecked("Share", "Email");
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
     *             if the menu is not opened, or if the item at given path is
     *             not usable.
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
     * tester.isItemChecked(0);
     *
     * // checks nested menu item at position 1 "Email" through the
     * // item "Share" at position 1
     * tester.isItemChecked(1, 1);
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
     *             if the menu is not opened, or if the item at given position
     *             is not usable.
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
     * For a nested menu item, provide the text of each menu item in the
     * hierarchy.
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
     * tester.getItemTooltipText("Preview");
     *
     * tester.getItemTooltipText("Share", "Email");
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
     *             if the menu is not opened, or if the item at given path is
     *             not usable.
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
     * For a nested menu item, provide the position of each sub menu that should
     * be navigated to reach the requested item.
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
     * tester.getItemTooltipText(0);
     *
     * // gets the tooltip of the nested "Email" item at position 0 through
     * // the item "Share" at position 1
     * tester.getItemTooltipText(1, 0);
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
     *             if the menu is not opened, or if the item at given position
     *             is not usable.
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
     * The menu has to be open, since its items are not part of the UI before
     * that.
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
     * tester.getItemTexts();
     * }
     * </pre>
     *
     * @return the texts of the visible top level menu items, in the order they
     *         are shown in
     * @throws IllegalStateException
     *             if the menu is not open, or is not visible
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
     * <p>
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
     * @since 25.3
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
     * Can be used to find components in the context menu. Returned components
     * are in a detached state unless {@link #open()} has been called
     * previously. Example usage:
     *
     * <pre>
     * // view:
     * ContextMenu menu = new ContextMenu();
     * menu.addItem(new VerticalLayout(new Div("Component Item")),
     *         click -> clickedItems.add("Component Item"));
     *
     * // test:
     * ContextMenuTester<ContextMenu> menuTester = test(view.menu);
     * menuTester.open();
     * Div div = menuTester.find(Div.class).withText("Component Item").single();
     * Assertions.assertTrue(div.isAttached());
     *
     * menuTester.close();
     * div = menuTester.find(Div.class).withText("Component Item").single();
     * Assertions.assertFalse(div.isAttached());
     * </pre>
     */
    @Override
    public <R extends Component> ComponentQuery<R> find(
            Class<R> componentType) {
        return super.find(componentType);
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

    private void attachMenuToUI() {
        Element target = getComponent().getTarget().getElement();
        DomEvent beforeOpen = new DomEvent(target,
                "vaadin-context-menu-before-open",
                JacksonUtils.createObjectNode());
        fireDomEvent(beforeOpen);
    }
}
