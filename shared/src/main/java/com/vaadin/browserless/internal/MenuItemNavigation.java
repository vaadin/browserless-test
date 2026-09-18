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
package com.vaadin.browserless.internal;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.ContextMenuBase;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.contextmenu.SubMenuBase;

/**
 * Menu item lookup shared by the menu testers.
 * <p>
 * A path addresses an item the way the browser shows it: by the text of each
 * item in the hierarchy, or by the zero-based position of each item among the
 * visible ones. The item at the end of the path must be enabled and visible,
 * since a user cannot click anything else.
 * <p>
 * For internal use only.
 */
public final class MenuItemNavigation {

    private MenuItemNavigation() {
    }

    /**
     * Finds the menu item addressed by the given text path.
     *
     * @param rootItems
     *            the items of the top level menu
     * @param topLevelText
     *            the text content of the top level menu item, not
     *            {@literal null}
     * @param nestedItemsText
     *            text content of the nested menu items
     * @param <C>
     *            menu type
     * @param <I>
     *            menu item type
     * @param <S>
     *            sub menu type
     * @return the menu item at the given path
     * @throws IllegalArgumentException
     *             if the provided text does not identify a menu item
     * @throws IllegalStateException
     *             if there are multiple matching items at any level, or if the
     *             item at the given path is disabled or not visible
     */
    public static <C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> I findByPath(
            List<I> rootItems, String topLevelText, String... nestedItemsText) {
        I menuItem = findByText(rootItems, topLevelText, null);
        if (nestedItemsText.length > 0) {
            String path = topLevelText + " / "
                    + String.join(" / ", nestedItemsText);
            for (String text : nestedItemsText) {
                ensureParentItem(menuItem, path);
                menuItem = findByText(menuItem.getSubMenu().getItems(), text,
                        path);
            }
        }
        return menuItem;
    }

    /**
     * Finds the menu item addressed by the given position path.
     *
     * @param rootItems
     *            the items of the top level menu
     * @param topLevelPosition
     *            the zero-based position of the item in the menu, as it will be
     *            seen in the browser
     * @param nestedItemsPositions
     *            the zero-based position of the nested items, relative to the
     *            parent menu
     * @param <C>
     *            menu type
     * @param <I>
     *            menu item type
     * @param <S>
     *            sub menu type
     * @return the menu item at the given path
     * @throws IllegalArgumentException
     *             if the provided position does not identify a menu item
     * @throws IllegalStateException
     *             if the item at the given position is disabled or not visible
     */
    public static <C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> I findByPath(
            List<I> rootItems, int topLevelPosition,
            int... nestedItemsPositions) {
        I menuItem = findByPosition(rootItems, topLevelPosition, null);
        if (nestedItemsPositions.length > 0) {
            StringBuilder path = new StringBuilder().append(topLevelPosition);
            for (int position : nestedItemsPositions) {
                ensureParentItem(menuItem, path.toString());
                path.append(" / ").append(position);
                menuItem = findByPosition(menuItem.getSubMenu().getItems(),
                        position, path.toString());
            }
        }
        return menuItem;
    }

    private static <C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> I findByText(
            List<I> allItems, String text, String fullPath) {
        List<I> items = allItems.stream()
                .filter(item -> text.equals(item.getText()))
                .collect(Collectors.toList());
        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot find menu item with text " + text
                            + (fullPath != null ? " on path " + fullPath : ""));
        } else if (items.size() > 1) {
            throw new IllegalStateException(
                    "Expecting a single menu item with text " + text
                            + " but found " + items.size()
                            + (fullPath != null ? " on path " + fullPath : ""));
        }
        I menuItem = items.get(0);
        ensureMenuItemIsUsable(menuItem, fullPath);
        return menuItem;
    }

    private static <C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> I findByPosition(
            List<I> allItems, int position, String fullPath) {
        I menuItem = allItems.stream().filter(Component::isVisible)
                .skip(position).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot find menu item at position " + fullPath));
        ensureMenuItemIsUsable(menuItem, fullPath);
        return menuItem;
    }

    private static void ensureParentItem(MenuItemBase<?, ?, ?> menuItem,
            String path) {
        if (!menuItem.isParentItem()) {
            throw new IllegalArgumentException("Menu item with text "
                    + menuItem.getText()
                    + " has no children. Make sure that the path is correct: "
                    + path);
        }
    }

    private static void ensureMenuItemIsUsable(MenuItemBase<?, ?, ?> menuItem,
            String fullPath) {
        if (!menuItem.isEnabled() || !menuItem.isVisible()) {
            throw new IllegalStateException(
                    "Menu item " + fullPath + " is not usable. "
                            + PrettyPrintTreeKt.toPrettyTree(menuItem));
        }
    }
}
