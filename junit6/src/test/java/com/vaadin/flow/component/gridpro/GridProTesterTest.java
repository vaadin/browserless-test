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
package com.vaadin.flow.component.gridpro;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages
class GridProTesterTest extends BrowserlessTest {

    @Test
    void setCheckboxValue_updatesAllRows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        tester.setValue(0, 0, true);
        assertCellEditStarted("Bean 1");
        tester.setValue(1, 0, true);
        assertCellEditStarted("Bean 2");
        tester.setValue(2, 0, true);
        assertCellEditStarted("Bean 3");

        List<GridProView.Bean> items = getItems(tester.getComponent());
        assertEquals(Boolean.TRUE, items.get(0).getChecked());
        assertEquals(Boolean.TRUE, items.get(1).getChecked());
        assertEquals(Boolean.TRUE, items.get(2).getChecked());
    }

    @Test
    void setName_updatesAllRows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        tester.setValue(0, 1, "Updated Bean 1");
        assertCellEditStarted("Bean 1");
        tester.setValue(1, 1, "Updated Bean 2");
        assertCellEditStarted("Bean 2");
        tester.setValue(2, 1, "Updated Bean 3");
        assertCellEditStarted("Bean 3");

        List<GridProView.Bean> items = getItems(tester.getComponent());
        assertEquals("Updated Bean 1", items.get(0).getName());
        assertEquals("Updated Bean 2", items.get(1).getName());
        assertEquals("Updated Bean 3", items.get(2).getName());
        assertEquals("Updated Bean 1", tester.getCellText(0, 1));
        assertEquals("Updated Bean 2", tester.getCellText(1, 1));
        assertEquals("Updated Bean 3", tester.getCellText(2, 1));

    }

    @Test
    void setDescription_updatesAllRows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        tester.setValue(0, 2, "Updated Description 1");
        assertCellEditStarted("Bean 1");
        tester.setValue(1, 2, "Updated Description 2");
        assertCellEditStarted("Bean 2");
        tester.setValue(2, 2, "Updated Description 3");
        assertCellEditStarted("Bean 3");

        List<GridProView.Bean> items = getItems(tester.getComponent());
        assertEquals("Updated Description 1", items.get(0).getDescription());
        assertEquals("Updated Description 2", items.get(1).getDescription());
        assertEquals("Updated Description 3", items.get(2).getDescription());
    }

    @Test
    void setYesNo_updatesAllRows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        tester.setValue(0, 4, GridProView.YesNo.YES);
        assertCellEditStarted("Bean 1");
        tester.setValue(1, 4, GridProView.YesNo.NO);
        assertCellEditStarted("Bean 2");
        tester.setValue(2, 4, GridProView.YesNo.YES);
        assertCellEditStarted("Bean 3");

        List<GridProView.Bean> items = getItems(tester.getComponent());
        assertEquals(GridProView.YesNo.YES, items.get(0).getYesNo());
        assertEquals(GridProView.YesNo.NO, items.get(1).getYesNo());
        assertEquals(GridProView.YesNo.YES, items.get(2).getYesNo());
    }

    @Test
    void setValue_forNonEditColumn_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tester.setValue(0, 3, "Should fail"));

        assertEquals("Column at index 3 is not an EditColumn",
                exception.getMessage());
    }

    @Test
    void setValue_forDisabledField_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tester.setValue(0, 5, "Should fail"));

        assertEquals(true, exception.getMessage()
                .startsWith("Editor field is not usable:"));
    }

    @Test
    void setValue_forOutOfBoundsVisibleColumn_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = navigateToTester();

        assertThrows(IndexOutOfBoundsException.class,
                () -> tester.setValue(0, 6, true));
    }

    @SuppressWarnings("unchecked")
    private GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> navigateToTester() {
        navigate(GridProView.class);
        GridPro<GridProView.Bean> gridPro = find(GridPro.class).id("grid-pro");
        return new GridProTester<>(gridPro);
    }

    private List<GridProView.Bean> getItems(GridPro<GridProView.Bean> gridPro) {
        return gridPro.getGenericDataView().getItems().toList();
    }

    private void assertCellEditStarted(String itemName) {
        assertEquals("Cell edit: " + itemName,
                test(find(Span.class).id(itemName)).getText());
    }

}
