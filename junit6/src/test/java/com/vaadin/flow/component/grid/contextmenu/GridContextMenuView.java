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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.Route;

@Tag("div")
@Route(value = "grid-contextmenu", registerAtStartup = false)
public class GridContextMenuView extends Component implements HasComponents {

    static final String ALICE = "Alice";
    static final String BOB = "Bob";
    static final String NAME_COLUMN = "name";
    static final String LENGTH_COLUMN = "length";
    static final String HIDDEN_COLUMN = "hidden";

    final Grid<String> grid;
    final Grid<String> gridWithoutMenu;
    final GridContextMenu<String> menu;
    final GridMenuItem<String> checkableItem;
    final List<String> clickedItems = new ArrayList<>();
    final List<Optional<String>> clickedRows = new ArrayList<>();

    public GridContextMenuView() {
        grid = new Grid<>();
        grid.addColumn(name -> name).setKey(NAME_COLUMN).setHeader("Name");
        grid.addColumn(String::length).setKey(LENGTH_COLUMN)
                .setHeader("Length");
        grid.addColumn(String::toUpperCase).setKey(HIDDEN_COLUMN)
                .setHeader("Hidden").setVisible(false);
        grid.setItems(ALICE, BOB);

        menu = grid.addContextMenu();
        menu.addItem("Edit", event -> record("Edit", event.getItem()))
                .setTooltipText("Edit the selected person");
        menu.addItem(new Checkbox("Show inactive"),
                event -> record("Show inactive", event.getItem()));
        checkableItem = menu.addItem("Checkable",
                event -> record("Checkable", event.getItem()));
        checkableItem.setCheckable(true);
        menu.addItem("Disabled", event -> record("Disabled", event.getItem()))
                .setEnabled(false);
        menu.addItem("Hidden", event -> record("Hidden", event.getItem()))
                .setVisible(false);

        GridSubMenu<String> subMenu = menu.addItem("Share").getSubMenu();
        subMenu.addItem("Copy link",
                event -> record("Share / Copy link", event.getItem()));
        subMenu.addItem("Email",
                event -> record("Share / Email", event.getItem()));

        gridWithoutMenu = new Grid<>();
        gridWithoutMenu.addColumn(name -> name).setHeader("Name");
        gridWithoutMenu.setItems(ALICE);

        add(grid, gridWithoutMenu);
    }

    private void record(String item, Optional<String> row) {
        clickedItems.add(item);
        clickedRows.add(row);
    }
}
