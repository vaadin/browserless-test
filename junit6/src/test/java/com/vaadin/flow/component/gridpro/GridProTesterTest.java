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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages
class GridProTesterTest extends BrowserlessTest {

    GridPro<GridProView.Bean> gridPro;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(GridProView.class);
        navigate(GridProView.class);
        gridPro = find(GridPro.class).id("grid-pro");
    }

    @Test
    void setCheckboxValue_updatesAllRows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);
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
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

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
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

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
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        tester.setValue(0, 4, GridProView.YesNo.YES);
        assertCellEditStarted("Bean 1");
        tester.setValue(1, 4, "NO");
        assertCellEditStarted("Bean 2");
        tester.setValue(2, 4, "YES");
        assertCellEditStarted("Bean 3");

        List<GridProView.Bean> items = getItems(tester.getComponent());
        assertEquals(GridProView.YesNo.YES, items.get(0).getYesNo());
        assertEquals(GridProView.YesNo.NO, items.get(1).getYesNo());
        assertEquals(GridProView.YesNo.YES, items.get(2).getYesNo());
    }

    @Test
    void setYesNo_updatesAllRows_YepThrows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tester.setValue(0, 4, "YEP"));

        assertEquals("Value YEP is not a valid option for the select editor",
                exception.getMessage());
    }

    @Test
    void setYesNo_wronEnumThrows() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tester.setValue(0, 4, MaybeNo.MAYBE));

        assertEquals("Value MAYBE is not a valid option for the select editor",
                exception.getMessage());
    }

    @Test
    void setValue_forNonEditColumn_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tester.setValue(0, 3, "Should fail"));

        assertEquals("Column at index 3 is not an EditColumn",
                exception.getMessage());
    }

    @Test
    void setValue_forDisabledField_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tester.setValue(0, 5, "Should fail"));

        assertEquals(
                "TextField[DISABLED, value='', manualValidation='true'] is not usable because it is not enabled.",
                exception.getMessage());
    }

    @Test
    void setValue_forReadOnlyField_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tester.setValue(0, 6, "Should fail"));

        assertEquals(
                "TextField[RO, value='', readonly='true', manualValidation='true'] is not usable because it is read only.",
                exception.getMessage());
    }

    @Test
    void setValue_forUneditablCell_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tester.setValue(0, 7, "Should fail"));

        assertEquals(true, exception.getMessage()
                .startsWith("Cell on row 0 at column 7 is not editable"));
    }

    @Test
    void setValue_forOutOfBoundsVisibleColumn_throwsException() {
        GridProTester<GridPro<GridProView.Bean>, GridProView.Bean> tester = new GridProTester<>(
                gridPro);

        assertThrows(IndexOutOfBoundsException.class,
                () -> tester.setValue(0, 8, true));
    }

    private List<GridProView.Bean> getItems(GridPro<GridProView.Bean> gridPro) {
        return gridPro.getGenericDataView().getItems().toList();
    }

    private void assertCellEditStarted(String itemName) {
        assertEquals("Cell edit: " + itemName,
                test(find(Span.class).id(itemName)).getText());
    }

    public static enum MaybeNo {
        MAYBE, NO,
    }
}
