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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.vaadin.browserless.component.GridKt;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * Helpers for driving a {@link GridContextMenu} the way the grid's client-side
 * connector does.
 * <p>
 * For internal use only.
 */
public final class GridContextMenuSupport {

    /**
     * DOM event the grid fires on its target element to let the menu know that
     * the user asked for it.
     */
    public static final String BEFORE_OPEN_EVENT = "vaadin-context-menu-before-open";

    /**
     * Grid element property holding the key of the row the context menu was
     * opened on. Read by {@code GridContextMenuItemClickEvent.getItem()} and
     * {@code GridContextMenuOpenedEvent.getItem()}.
     */
    public static final String TARGET_ITEM_KEY_PROPERTY = "_contextMenuTargetItemKey";

    /**
     * Grid element property holding the internal id of the column the context
     * menu was opened on. Read by
     * {@code GridContextMenuOpenedEvent.getColumnId()}.
     */
    public static final String TARGET_COLUMN_ID_PROPERTY = "_contextMenuTargetColumnId";

    private GridContextMenuSupport() {
    }

    /**
     * Gets the context menu added to the given grid.
     * <p>
     * A grid keeps no reference to its context menu, and the menu is not part
     * of the component tree until it opens, so the only link from the grid back
     * to the menu is the listener the menu registers on the grid element for
     * {@value #BEFORE_OPEN_EVENT}.
     *
     * @param grid
     *            the grid whose context menu to get
     * @return the context menu of the grid
     * @throws IllegalStateException
     *             if the grid has no context menu, if it has several, or if the
     *             menu cannot be resolved
     */
    public static GridContextMenu<?> getContextMenu(Grid<?> grid) {
        List<GridContextMenu<?>> menus = findContextMenus(grid);
        if (menus.isEmpty()) {
            throw new IllegalStateException(
                    "Grid has no context menu. Add one with Grid.addContextMenu().");
        }
        if (menus.size() > 1) {
            throw new IllegalStateException("Grid has " + menus.size()
                    + " context menus, so the one to test is ambiguous. "
                    + "Use test(gridContextMenu) with the menu to test.");
        }
        return menus.get(0);
    }

    /**
     * Records the row and column the context menu is about, the way the grid
     * does when the browser reports a context menu gesture.
     *
     * @param grid
     *            the grid the menu is attached to
     * @param itemKey
     *            key of the row the menu targets, or {@literal null} for none
     * @param columnInternalId
     *            internal id of the column the menu targets, or {@literal null}
     *            for none
     */
    public static void setTargetItem(Grid<?> grid, String itemKey,
            String columnInternalId) {
        Element element = grid.getElement();
        element.setProperty(TARGET_ITEM_KEY_PROPERTY, itemKey);
        element.setProperty(TARGET_COLUMN_ID_PROPERTY, columnInternalId);
    }

    /**
     * Gets the key the grid uses for the given item.
     *
     * @param grid
     *            the grid holding the item
     * @param item
     *            the item to get the key for
     * @param <Y>
     *            item type
     * @return the key of the item
     */
    public static <Y> String getItemKey(Grid<Y> grid, Y item) {
        return grid.getDataCommunicator().getKeyMapper().key(item);
    }

    /**
     * Gets the internal id the grid uses for the column with the given key.
     *
     * @param grid
     *            the grid holding the column
     * @param columnKey
     *            the key of the column, as set with
     *            {@code Grid.Column.setKey(String)}
     * @return the internal id of the column
     * @throws IllegalArgumentException
     *             if the grid has no column with the given key
     */
    public static String getColumnInternalId(Grid<?> grid, String columnKey) {
        Grid.Column<?> column = grid.getColumnByKey(columnKey);
        if (column == null) {
            throw new IllegalArgumentException(
                    "Grid has no column with key " + columnKey);
        }
        return GridKt.get_internalId(column);
    }

    private static List<GridContextMenu<?>> findContextMenus(Grid<?> grid) {
        List<GridContextMenu<?>> menus = new ArrayList<>();
        for (DomEventListener listener : beforeOpenListeners(grid)) {
            for (Field field : listener.getClass().getDeclaredFields()) {
                Object captured = read(field, listener);
                if (captured instanceof GridContextMenu<?> menu
                        && menu.getTarget() == grid && !menus.contains(menu)) {
                    menus.add(menu);
                }
            }
        }
        return menus;
    }

    /**
     * Collects the {@value #BEFORE_OPEN_EVENT} listeners registered on the grid
     * element. Flow exposes no API for this, so the listener map is read
     * reflectively.
     */
    private static List<DomEventListener> beforeOpenListeners(Grid<?> grid) {
        ElementListenerMap listenerMap = grid.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        List<DomEventListener> listeners = new ArrayList<>();
        for (Field field : ElementListenerMap.class.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            if (!(read(field, listenerMap) instanceof Map<?, ?> byEventType)) {
                continue;
            }
            if (!(byEventType.get(
                    BEFORE_OPEN_EVENT) instanceof Collection<?> wrappers)) {
                continue;
            }
            for (Object wrapper : wrappers) {
                for (Field wrapped : wrapper.getClass().getDeclaredFields()) {
                    if (read(wrapped,
                            wrapper) instanceof DomEventListener listener) {
                        listeners.add(listener);
                    }
                }
            }
        }
        return listeners;
    }

    private static Object read(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

}
