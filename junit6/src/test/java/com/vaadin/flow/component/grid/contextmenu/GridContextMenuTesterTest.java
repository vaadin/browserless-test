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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.browserless.internal.GridContextMenuSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class GridContextMenuTesterTest extends BrowserlessTest {

    GridContextMenuView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(GridContextMenuView.class);
        view = navigate(GridContextMenuView.class);
    }

    @Test
    void gridContextMenu_resolvesToGridContextMenuTester() {
        Assertions.assertInstanceOf(GridContextMenuTester.class,
                test((Component) view.menu),
                "GridContextMenu should resolve to a GridContextMenuTester");
    }

    @Test
    void openOnRow_menuContentIsAttachedAndDetachedOnClose() {
        Assertions.assertEquals(0, find(Checkbox.class).all().size(),
                "menu content should not be in the tree before the menu opens");

        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(0);

        Assertions.assertTrue(view.menu.isAttached(),
                "context menu should be attached to the UI, but was not");
        Assertions.assertEquals(1, find(Checkbox.class).all().size(),
                "component item of the open menu should be findable");
        Assertions.assertTrue(menu_.find(Checkbox.class).single().isAttached(),
                "component item of the open menu should be attached");

        menu_.close();

        Assertions.assertFalse(view.menu.isAttached(),
                "context menu should be detached from the UI, but was not");
    }

    @Test
    void clickItem_byText_actionExecutedWithTargetRow() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(1);

        menu_.clickItem("Edit");

        Assertions.assertIterableEquals(List.of("Edit"), view.clickedItems);
        Assertions.assertIterableEquals(
                List.of(Optional.of(GridContextMenuView.BOB)), view.clickedRows,
                "the click event should report the row the menu was opened on");
    }

    @Test
    void clickItem_byPositionAndNestedPath_actionExecuted() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(0);

        // "Hidden" is skipped, so "Share" is the item at position 4
        menu_.clickItem(4, 1);
        menu_.clickItem("Share", "Copy link");

        Assertions.assertIterableEquals(
                List.of("Share / Email", "Share / Copy link"),
                view.clickedItems);
        Assertions.assertIterableEquals(
                List.of(Optional.of(GridContextMenuView.ALICE),
                        Optional.of(GridContextMenuView.ALICE)),
                view.clickedRows);
    }

    @Test
    void clickItem_checkableItem_isToggled() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(0);

        Assertions.assertFalse(menu_.isItemChecked("Checkable"));

        menu_.clickItem("Checkable");

        Assertions.assertTrue(menu_.isItemChecked("Checkable"),
                "clicking a checkable item should check it");
        Assertions.assertIterableEquals(List.of("Checkable"),
                view.clickedItems);
    }

    @Test
    void getItemTooltipText_returnsTooltipOfItem() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(0);

        Assertions.assertEquals("Edit the selected person",
                menu_.getItemTooltipText("Edit"));
        Assertions.assertNull(menu_.getItemTooltipText(1));
    }

    @Test
    void clickItem_disabledItem_throws() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(0);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, () -> menu_.clickItem("Disabled"));
        Assertions.assertTrue(exception.getMessage().contains("not usable"));
    }

    @Test
    void clickItem_menuNotOpened_throws() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.menu).clickItem("Edit"));
    }

    @Test
    void openOnRowAndColumn_openedEventReportsRowAndColumn() {
        List<Optional<String>> openedOn = new ArrayList<>();
        List<Optional<String>> openedColumns = new ArrayList<>();
        view.menu.addGridContextMenuOpenedListener(event -> {
            openedOn.add(event.getItem());
            openedColumns.add(event.getColumnId());
        });

        test(view.menu).open(1, GridContextMenuView.LENGTH_COLUMN);

        Assertions.assertIterableEquals(
                List.of(Optional.of(GridContextMenuView.BOB)), openedOn);
        Assertions.assertIterableEquals(
                List.of(Optional.of(GridContextMenuSupport.getColumnInternalId(
                        view.grid, GridContextMenuView.LENGTH_COLUMN))),
                openedColumns);
    }

    @Test
    void openOnRow_unknownColumn_throws() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> test(view.menu).open(0, "nosuchcolumn"));
        Assertions.assertTrue(exception.getMessage().contains("nosuchcolumn"));
    }

    @Test
    void openOnRow_alreadyOpen_throws() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.menu);
        menu_.open(0);

        IllegalStateException exception = Assertions
                .assertThrows(IllegalStateException.class, () -> menu_.open(0));
        Assertions.assertTrue(exception.getMessage().contains("already open"));
    }

    @Test
    void open_noTargetRow_throws() {
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, test(view.menu)::open);
        Assertions.assertTrue(
                exception.getMessage().contains("does not target a row"));
    }

    @Test
    void openOnRow_dynamicContentHandlerRefuses_throws() {
        view.menu.setDynamicContentHandler(
                item -> GridContextMenuView.BOB.equals(item));

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, () -> test(view.menu).open(0));
        Assertions.assertTrue(
                exception.getMessage().contains("dynamic content handler"));
        Assertions.assertFalse(view.menu.isAttached(),
                "menu should stay closed when its dynamic content handler refuses");

        test(view.menu).open(1);

        Assertions.assertTrue(view.menu.isAttached(),
                "menu should open on a row its dynamic content handler accepts");
    }

    @Test
    void contextMenuFromGridTester_targetsRowWithoutOpening() {
        GridContextMenuTester<GridContextMenu<String>, String> menu_ = test(
                view.grid).contextMenu(1);

        Assertions.assertFalse(view.menu.isAttached(),
                "contextMenu(row) should not open the menu");

        menu_.open();
        menu_.clickItem("Edit");

        Assertions.assertIterableEquals(List.of("Edit"), view.clickedItems);
        Assertions.assertIterableEquals(
                List.of(Optional.of(GridContextMenuView.BOB)),
                view.clickedRows);
    }

    @Test
    void contextMenuFromGridTester_gridWithoutContextMenu_throws() {
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> test(view.gridWithoutMenu).contextMenu(0));
        Assertions.assertTrue(
                exception.getMessage().contains("has no context menu"));
    }
}
