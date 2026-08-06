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

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.gridpro.GridPro.CellEditStartedEvent;
import com.vaadin.flow.component.gridpro.GridPro.EditColumn;
import com.vaadin.flow.component.gridpro.GridPro.ItemPropertyChangedEvent;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for GridPro components.
 *
 * @param <T>
 *            component type
 * @param <Y>
 *            item type
 */
@Tests(fqn = { "com.vaadin.flow.component.gridpro.GridPro" })
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
     * be an EditColumn. The indexes for row and column are 0 based.
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
        var gridPro = getComponent();
        var column = getColumns().get(columnIndex);
        if (column instanceof EditColumn<Y> editColumn) {
            if (!isCellEditable(rowIndex, columnIndex, editColumn)) {
                throw new IllegalStateException("Cell on row " + rowIndex
                        + " at column " + columnIndex + " is not editable");
            }
            if ("select".equals(editColumn.getEditorType())
                    && !(value instanceof Enum<?>)
                    && !editColumn.getOptions().contains(value)) {
                throw new IllegalArgumentException("Value " + value
                        + " is not a valid option for the select editor");
            }
            Y item = getRow(rowIndex);
            if ("custom".equals(editColumn.getEditorType())) {
                var field = editColumn.getEditorField();
                ensureComponentIsUsable((Component) field,
                        f -> isUsable(f) && !field.isReadOnly());
                fireCellEditStartedEvent(gridPro, editColumn, item);
                field.setValue(value);
                fireItemPropertyChangedEvent(gridPro, editColumn, item, value);
            } else {
                fireCellEditStartedEvent(gridPro, editColumn, item);
                fireItemPropertyChangedEvent(gridPro, editColumn, item, value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Column at index " + columnIndex + " is not an EditColumn");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isCellEditable(int rowIndex, int columnIndex,
            EditColumn<Y> editColumn) {
        try {
            var cellEditableProvider = (SerializablePredicate<Y>) getField(
                    EditColumn.class, "cellEditableProvider").get(editColumn);
            return cellEditableProvider == null
                    || cellEditableProvider.test(getRow(rowIndex));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(
                    "Unable to determine whether cell on row " + rowIndex
                            + " at column " + columnIndex + " is editable",
                    exception);
        }
    }

    private void fireCellEditStartedEvent(GridPro<Y> gridPro,
            EditColumn<Y> editColumn, Y item) {
        ObjectNode itemNode = JacksonUtils.createObjectNode();
        itemNode.put("key",
                gridPro.getDataCommunicator().getKeyMapper().key(item));
        ComponentUtil.fireEvent(gridPro, new CellEditStartedEvent<Y>(gridPro,
                true, itemNode, editColumn.getInternalId()));
    }

    private void fireItemPropertyChangedEvent(GridPro<Y> gridPro,
            EditColumn<Y> editColumn, Y item, Object value) {
        ObjectNode itemNode = JacksonUtils.createObjectNode();
        itemNode.put("key",
                gridPro.getDataCommunicator().getKeyMapper().key(item));
        if (value instanceof Boolean booleanValue) {
            itemNode.put(editColumn.getInternalId(), booleanValue);
        } else if (value != null) {
            itemNode.put(editColumn.getInternalId(), String.valueOf(value));
        } else {
            itemNode.putNull(editColumn.getInternalId());
        }
        ComponentUtil.fireEvent(gridPro, new ItemPropertyChangedEvent<Y>(
                gridPro, true, itemNode, editColumn.getInternalId()));
    }

}
