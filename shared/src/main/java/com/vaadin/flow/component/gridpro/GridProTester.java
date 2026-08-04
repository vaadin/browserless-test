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

import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.gridpro.GridPro.EditColumn;

public class GridProTester<T extends GridPro<Y>, Y> extends GridTester<T, Y> {

    /**
     * Wrap gridpro for testing.
     *
     * @param component
     *            target gridpro
     */
    public GridProTester(T component) {
        super(component);
    }

    /**
     * Set value for the cell at the given row and column index. The column must
     * be an EditColumn.
     *
     * @param rowIndex
     *            the index of the row
     * @param columnIndex
     *            the index of the column
     * @param value
     *            the value to set
     */
    @SuppressWarnings("unchecked")
    public void setValue(int rowIndex, int columnIndex, Object value) {
        ensureComponentIsUsable();
        var gridpro = getComponent();
        var column = gridpro.getColumns().get(columnIndex);
        if (column instanceof EditColumn editColumn) {
            var updater = (ItemUpdater<Y, String>) editColumn.getItemUpdater();
            Y item = getRow(rowIndex);
            if ("custom".equals(editColumn.getEditorType())) {
                editColumn.getEditorField().setValue(value);
                updater.accept(item, null);
            } else {
                updater.accept(item, String.valueOf(value));
            }
        } else {
            throw new IllegalArgumentException(
                    "Column at index " + columnIndex + " is not an EditColumn");
        }
    }
}
