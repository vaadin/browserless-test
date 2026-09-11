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
package com.vaadin.flow.component.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class BasicGridTesterTest extends BrowserlessTest {

    BasicGridView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(BasicGridView.class);

        view = navigate(BasicGridView.class);
    }

    @Test
    void basicGrid_verifyColumnContent() {
        Assertions.assertEquals(2, test(view.basicGrid).size());

        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty());

        Assertions.assertEquals("Jorma",
                test(view.basicGrid).getCellText(0, 0));
        // second column is hidden
        Assertions.assertEquals("46", test(view.basicGrid).getCellText(0, 1));

        Assertions.assertEquals("Maya", test(view.basicGrid).getCellText(1, 0));
        // second column is hidden
        Assertions.assertEquals("18", test(view.basicGrid).getCellText(1, 1));
    }

    @Test
    void basicGrid_selectionOnClick() {
        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty());

        test(view.basicGrid).clickRow(0);
        Assertions.assertEquals(1, test(view.basicGrid).getSelected().size());
        Assertions.assertSame(view.person1,
                test(view.basicGrid).getSelected().iterator().next());

        test(view.basicGrid).clickRow(1);
        Assertions.assertEquals(1, test(view.basicGrid).getSelected().size());
        Assertions.assertSame(view.person2,
                test(view.basicGrid).getSelected().iterator().next());
    }

    @Test
    void basicGrid_deselectSelectedOnClick() {
        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty());

        test(view.basicGrid).clickRow(0);
        Assertions.assertEquals(1, test(view.basicGrid).getSelected().size());
        Assertions.assertSame(view.person1,
                test(view.basicGrid).getSelected().iterator().next());

        test(view.basicGrid).clickRow(0);
        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty(),
                "Clicking selected row should deselect");
    }

    @Test
    void basicGrid_selectWillChangeSelection() {
        test(view.basicGrid).select(1);
        Assertions.assertEquals(1, test(view.basicGrid).getSelected().size());
        Assertions.assertSame(view.person2,
                test(view.basicGrid).getSelected().iterator().next());

        test(view.basicGrid).select(0);
        Assertions.assertEquals(1, test(view.basicGrid).getSelected().size(),
                "Single select should only change selection.");
        Assertions.assertSame(view.person1,
                test(view.basicGrid).getSelected().iterator().next());
    }

    @Test
    void basicGrid_headerContent() {
        Assertions.assertEquals("First Name", test(view.basicGrid)
                .getColumn(BasicGridView.FIRST_NAME_KEY).getHeaderText());
        Assertions.assertEquals("Age", test(view.basicGrid)
                .getColumn(BasicGridView.AGE_KEY).getHeaderText());
        Assertions.assertEquals("Subscriber", test(view.basicGrid)
                .getColumn(BasicGridView.SUBSCRIBER_KEY).getHeaderText());
        Assertions.assertEquals("Deceased", test(view.basicGrid)
                .getColumn(BasicGridView.DECEASED_KEY).getHeaderText());
    }

    @Test
    void basicGrid_multiselect() {
        // This is not normally appropriate for a test, but we are testing
        // features.
        view.basicGrid.setSelectionMode(Grid.SelectionMode.MULTI);

        test(view.basicGrid).clickRow(0);
        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty(),
                "Multiselect doesn't select for row click!");

        test(view.basicGrid).select(0);
        test(view.basicGrid).select(1);
        Assertions.assertSame(2, test(view.basicGrid).getSelected().size());
    }

    @Test
    void basicGrid_multiselectAll() {
        // This is not normally appropriate for a test, but we are testing
        // features.
        view.basicGrid.setSelectionMode(Grid.SelectionMode.MULTI);

        test(view.basicGrid).selectAll();
        Assertions.assertSame(2, test(view.basicGrid).getSelected().size());
    }

    @Test
    void basicGrid_singleSelectThrowsForSelectAllAndDeselectAll() {
        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);
        Assertions.assertThrows(IllegalStateException.class, grid_::selectAll,
                "Select all should throw for single select");
        Assertions.assertThrows(IllegalStateException.class, grid_::deselectAll,
                "Deselect all should throw for single select");
    }

    @Test
    void basicGrid_deselectClearsSingleSelection() {
        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);
        grid_.select(0);

        grid_.deselect(0);
        Assertions.assertTrue(grid_.getSelected().isEmpty(),
                "Deselecting the selected row should clear the selection");

        Assertions.assertThrows(IllegalStateException.class,
                () -> grid_.deselect(0),
                "Deselecting a row that is not selected should throw");
    }

    @Test
    void basicGrid_deselect_firesClientSideSelectionEvent() {
        test(view.basicGrid).select(0);

        AtomicReference<SelectionEvent<Grid<Person>, Person>> lastEvent = new AtomicReference<>();
        view.basicGrid.addSelectionListener(lastEvent::set);

        test(view.basicGrid).deselect(0);

        Assertions.assertNotNull(lastEvent.get(),
                "Deselect should fire a selection event");
        Assertions.assertTrue(lastEvent.get().isFromClient(),
                "Deselect should be seen as a client-side change");
        Assertions.assertTrue(lastEvent.get().getAllSelectedItems().isEmpty(),
                "Nothing should be selected after the deselect");
    }

    @Test
    void basicGrid_deselectNotAllowed_selectionIsKept() {
        ((GridSingleSelectionModel<Person>) view.basicGrid.getSelectionModel())
                .setDeselectAllowed(false);
        test(view.basicGrid).select(0);

        test(view.basicGrid).deselect(0);
        Assertions.assertSame(view.person1,
                test(view.basicGrid).getSelected().iterator().next(),
                "Deselect should be ignored when deselect is not allowed");
    }

    @Test
    void basicGrid_itemNotSelectable_selectionIsKept() {
        // This is not normally appropriate for a test, but we are testing
        // features.
        view.basicGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        view.basicGrid
                .setItemSelectableProvider(person -> person != view.person1);
        // not selectable from the client, so select it as the application
        view.basicGrid.select(view.person1);

        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);
        grid_.deselect(0);
        Assertions.assertSame(view.person1,
                grid_.getSelected().iterator().next(),
                "Deselect should be ignored for an item that is not selectable");

        Assertions.assertThrows(IllegalStateException.class, grid_::deselectAll,
                "A selectable provider hides the select all checkbox, so deselect all isn't available");
    }

    @Test
    void basicGrid_multiselectDeselect() {
        // This is not normally appropriate for a test, but we are testing
        // features.
        view.basicGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        GridMultiSelectionModel<Person> selectionModel = (GridMultiSelectionModel<Person>) view.basicGrid
                .getSelectionModel();

        test(view.basicGrid).select(0);
        test(view.basicGrid).select(1);

        AtomicInteger rowToggles = new AtomicInteger();
        selectionModel.addClientItemToggleListener(
                event -> rowToggles.incrementAndGet());

        test(view.basicGrid).deselect(0);
        Assertions.assertEquals(List.of(view.person2),
                List.copyOf(test(view.basicGrid).getSelected()),
                "Deselect should only remove the targeted row");
        Assertions.assertEquals(1, rowToggles.get(),
                "Unchecking a row should toggle that one row");

        test(view.basicGrid).deselectAll();
        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty(),
                "Deselect all should clear the selection");
    }

    @Test
    void basicGrid_deselectAll_firesOneSelectionEventWithoutRowToggles() {
        // This is not normally appropriate for a test, but we are testing
        // features.
        view.basicGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        GridMultiSelectionModel<Person> selectionModel = (GridMultiSelectionModel<Person>) view.basicGrid
                .getSelectionModel();

        test(view.basicGrid).select(0);
        test(view.basicGrid).select(1);

        AtomicInteger selectionEvents = new AtomicInteger();
        AtomicInteger rowToggles = new AtomicInteger();
        view.basicGrid.addSelectionListener(
                event -> selectionEvents.incrementAndGet());
        selectionModel.addClientItemToggleListener(
                event -> rowToggles.incrementAndGet());

        test(view.basicGrid).deselectAll();

        Assertions.assertTrue(test(view.basicGrid).getSelected().isEmpty(),
                "Deselect all should clear the selection");
        Assertions.assertEquals(1, selectionEvents.get(),
                "Unchecking the select all checkbox drops the selection in one event");
        Assertions.assertEquals(0, rowToggles.get(),
                "The select all checkbox doesn't toggle the rows one by one");
    }

    @Test
    void basicGrid_hiddenSelectAllCheckbox_deselectAllThrows() {
        // This is not normally appropriate for a test, but we are testing
        // features.
        view.basicGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        ((GridMultiSelectionModel<Person>) view.basicGrid.getSelectionModel())
                .setSelectAllCheckboxVisibility(
                        GridMultiSelectionModel.SelectAllCheckboxVisibility.HIDDEN);

        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);
        grid_.select(0);
        grid_.select(1);

        Assertions.assertThrows(IllegalStateException.class, grid_::deselectAll,
                "Deselect all shouldn't be available when the checkbox is hidden");
        Assertions.assertEquals(2, grid_.getSelected().size(),
                "The selection should be untouched");
    }

    @Test
    void basicGrid_selectionModeNone_deselectThrows() {
        view.basicGrid.setSelectionMode(Grid.SelectionMode.NONE);
        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);

        Assertions.assertThrows(IllegalStateException.class,
                () -> grid_.deselect(0),
                "Deselect should throw when the grid doesn't support selection");
        Assertions.assertThrows(IllegalStateException.class, grid_::deselectAll,
                "Deselect all should throw when the grid doesn't support selection");
    }

    @Test
    void basicGrid_disabled_deselectThrows() {
        test(view.basicGrid).select(0);
        view.basicGrid.setEnabled(false);
        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);

        Assertions.assertThrows(IllegalStateException.class,
                () -> grid_.deselect(0),
                "Deselect shouldn't be available for a disabled grid");
        Assertions.assertThrows(IllegalStateException.class, grid_::deselectAll,
                "Deselect all shouldn't be available for a disabled grid");
    }

    @Test
    void basicGrid_Hidden_getTextThrows() {
        view.basicGrid.setVisible(false);

        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);

        Assertions.assertThrows(IllegalStateException.class,
                () -> grid_.getHeaderCell(0),
                "Header cell shouldn't be available for hidden grid");
        Assertions.assertThrows(IllegalStateException.class,
                () -> grid_.getCellText(0, 0),
                "Cell content shouldn't be available for hidden grid");
    }

    @Test
    void basicGrid_doubleClick() {
        AtomicInteger doubleClicks = new AtomicInteger(0);
        view.basicGrid.addItemDoubleClickListener(
                event -> doubleClicks.incrementAndGet());

        test(view.basicGrid).clickRow(0);
        Assertions.assertEquals(0, doubleClicks.get(),
                "Click should not generate a double click event");

        test(view.basicGrid).doubleClickRow(0);
        Assertions.assertEquals(1, doubleClicks.get(),
                "Double click event should have fired");

    }

    @Test
    void getCellComponent_columnByKey_canClickAButton() {
        final Component cellComponent = test(view.basicGrid).getCellComponent(1,
                BasicGridView.BUTTON_KEY);
        Assertions.assertInstanceOf(Button.class, cellComponent);
        var button = (Button) cellComponent;
        test(button).click();
        var notification = find(Notification.class).last();
        Assertions.assertEquals("Clicked!", test(notification).getText());
    }

    @Test
    void getCellComponent_columnByKey_returnsInstantiatedComponent() {
        final Component cellComponent = test(view.basicGrid).getCellComponent(1,
                BasicGridView.SUBSCRIBER_KEY);
        Assertions.assertInstanceOf(CheckBox.class, cellComponent);
        Assertions.assertFalse(((CheckBox) cellComponent).isChecked());
    }

    @Test
    void getCellComponentByFaultyKey_throwsException() {
        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> grid_.getCellComponent(1, "property"));
    }

    @Test
    void getCellComponent_columnByPosition_returnsInstantiatedComponent() {
        final Component cellComponent = test(view.basicGrid).getCellComponent(1,
                2);
        Assertions.assertInstanceOf(CheckBox.class, cellComponent);
        Assertions.assertFalse(((CheckBox) cellComponent).isChecked());
    }

    @Test
    void getCellComponent_columnByPosition_stringColumnThrows() {
        GridTester<Grid<Person>, Person> grid_ = test(view.basicGrid);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> grid_.getCellComponent(1, 1));
    }

    @Test
    void basicGrid_reorderColumns() {
        List<Grid.Column<Person>> columns = new ArrayList<>(
                view.basicGrid.getColumns());
        Collections.reverse(columns);
        view.basicGrid.setColumnOrder(columns);

        // person 1
        Assertions.assertEquals("Jorma",
                test(view.basicGrid).getCellText(0, 4));
        Assertions.assertEquals("46", test(view.basicGrid).getCellText(0, 3));

        // person 2
        Assertions.assertEquals("Maya", test(view.basicGrid).getCellText(1, 4));
        Assertions.assertEquals("18", test(view.basicGrid).getCellText(1, 3));
    }

    @Test
    void basicGrid_toggleLitRenderedColumn() {
        boolean deceased = test(view.basicGrid).getRow(0).getDeceased();

        Assertions.assertEquals(deceased,
                test(view.basicGrid).getLitRendererPropertyValue(0,
                        BasicGridView.DECEASED_KEY, "deceased", Boolean.class));

        test(view.basicGrid).invokeLitRendererFunction(0,
                BasicGridView.DECEASED_KEY, "onClick");

        Assertions.assertEquals(!deceased,
                test(view.basicGrid).getLitRendererPropertyValue(0,
                        BasicGridView.DECEASED_KEY, "deceased", Boolean.class));
    }

}
