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

    final Grid<String> grid;
    final GridContextMenu<String> menu;
    final List<String> clickedItems = new ArrayList<>();
    final List<Optional<String>> clickedRows = new ArrayList<>();

    public GridContextMenuView() {
        grid = new Grid<>();
        grid.addColumn(name -> name).setHeader("Name");
        grid.setItems(ALICE, BOB);

        menu = grid.addContextMenu();
        menu.addItem("Edit", event -> {
            clickedItems.add("Edit");
            clickedRows.add(event.getItem());
        });
        menu.addItem(new Checkbox("Show inactive"), event -> {
            clickedItems.add("Show inactive");
            clickedRows.add(event.getItem());
        });

        add(grid);
    }
}
