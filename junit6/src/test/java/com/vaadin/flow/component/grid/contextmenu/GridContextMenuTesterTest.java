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
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.router.RouteConfiguration;

/**
 * Captures the {@code GridContextMenu} gap reported in #213: a grid context
 * menu cannot be driven from a browserless test.
 * <p>
 * These tests are written against the existing API only, since the shape of the
 * tester is still open. They fail today and are meant to be rewritten against
 * whichever tester API is chosen.
 */
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
    void gridContextMenu_resolvesToDedicatedTester() {
        ComponentTester<Component> tester = test((Component) view.menu);

        Assertions.assertNotEquals(ComponentTester.class, tester.getClass(),
                "GridContextMenu should resolve to a menu tester that can open "
                        + "the menu and click its items, but fell back to the "
                        + "generic ComponentTester");
    }

    @Test
    void openMenuOnRow_menuContentIsAttachedAndFindable() {
        Assertions.assertEquals(0, find(Checkbox.class).all().size(),
                "menu content should not be in the tree before the menu opens");

        // Nothing in the tester API opens a GridContextMenu, so the component
        // item of the menu never becomes part of the UI: it cannot be found,
        // and clicking it fails with "is not usable because it is not
        // attached".

        Assertions.assertEquals(1, find(Checkbox.class).all().size(),
                "opening the menu should attach its content to the UI");
    }

    @Test
    void clickItem_listenerReceivesClickedRow() {
        GridMenuItem<String> editItem = view.menu.getItems().get(0);

        test(editItem).click();

        Assertions.assertIterableEquals(List.of("Edit"), view.clickedItems,
                "clicking a grid context menu item should run its listener");
        Assertions.assertIterableEquals(List.of(Optional.of("Bob")),
                view.clickedRows,
                "the click event should report the row the menu was opened on");
    }
}
