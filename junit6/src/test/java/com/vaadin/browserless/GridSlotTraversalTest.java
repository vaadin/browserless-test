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
package com.vaadin.browserless;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;

/**
 * Verifies that components a {@link Grid} holds on behalf of its columns
 * (header cells, footer cells and editor components) are locatable by
 * {@code find(...)}, and that a component in a merged header cell is reported
 * exactly once even though it belongs to several columns.
 */
class GridSlotTraversalTest extends BrowserlessTest {

    private record Person(String firstName, String lastName) {
    }

    private Grid<Person> gridInView() {
        Grid<Person> grid = new Grid<>();
        grid.setItems(List.of(new Person("Ada", "Lovelace"),
                new Person("Grace", "Hopper")));
        getCurrentView().getElement().appendChild(grid.getElement());
        return grid;
    }

    @Test
    void columnComponents_headerFooterAndEditor_allLocatable() {
        Grid<Person> grid = gridInView();
        Grid.Column<Person> column = grid.addColumn(Person::firstName);
        column.setHeader(new Button("Header button"));
        column.setFooter(new Button("Footer button"));
        column.setEditorComponent(new Button("Editor button"));

        Assertions.assertTrue(
                find(Button.class).withText("Header button").exists(),
                "component set as a column header must be locatable");
        Assertions.assertTrue(
                find(Button.class).withText("Footer button").exists(),
                "component set as a column footer must be locatable");
        Assertions.assertTrue(
                find(Button.class).withText("Editor button").exists(),
                "component set as a column editor must be locatable");

        Assertions.assertEquals(3, find(Button.class).all().size(),
                "each column component must be reported exactly once");
    }

    @Test
    void mergedHeaderCellComponent_reportedOnce() {
        Grid<Person> grid = gridInView();
        Grid.Column<Person> first = grid.addColumn(Person::firstName)
                .setHeader("First");
        Grid.Column<Person> last = grid.addColumn(Person::lastName)
                .setHeader("Last");

        // the default header row holds the texts above; the joined cell needs
        // a row of its own on top of it
        HeaderRow joinedRow = grid.prependHeaderRow();
        joinedRow.join(first, last).setComponent(new Button("Name"));

        Assertions.assertEquals(1,
                find(Button.class).withText("Name").all().size(),
                "a component in a merged header cell spans several columns "
                        + "but must still be reported exactly once");
    }
}
