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

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.gridpro.GridPro.EditColumn;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Tag("div")
@Route(value = "grid-pro", registerAtStartup = false)
public class GridProView extends Component implements HasComponents {

    TextField customEditor = new TextField();
    CompositeEditor compositeEditor = new CompositeEditor();
    ForeignEditor foreignEditor = new ForeignEditor();

    public GridProView() {
        var gridPro = new GridPro<Bean>();
        gridPro.setId("grid-pro");

        var beans = new Bean[] { new Bean("Bean 1", "Description 1"),
                new Bean("Bean 2", "Description 2"),
                new Bean("Bean 3", "Description 3") };

        gridPro.addEditColumn(Bean::getChecked).checkbox(Bean::setChecked);
        gridPro.addEditColumn(Bean::getName).text(Bean::setName);
        gridPro.addEditColumn(Bean::getDescription).custom(customEditor,
                Bean::setDescription);
        gridPro.addColumn(Bean::getName);
        gridPro.addEditColumn(Bean::getChecked).checkbox(Bean::setChecked)
                .setVisible(false);
        gridPro.addEditColumn(Bean::getYesNo).select(Bean::setYesNo,
                YesNo.class);
        var disabledField = new TextField();
        disabledField.setEnabled(false);
        gridPro.addEditColumn(Bean::getDescription).custom(disabledField,
                Bean::setDescription);
        var readOnlyField = new TextField();
        readOnlyField.setReadOnly(true);
        gridPro.addEditColumn(Bean::getDescription).custom(readOnlyField,
                Bean::setDescription);
        gridPro.addEditColumn(Bean::getName).text(Bean::setName)
                .setKey("uneditable");
        var column = (EditColumn<Bean>) gridPro.getColumnByKey("uneditable");
        column.setCellEditableProvider(item -> false);
        gridPro.addEditColumn(Bean::getDescription).custom(compositeEditor,
                Bean::setDescription);
        gridPro.addEditColumn(Bean::getDescription).custom(foreignEditor,
                Bean::setDescription);
        gridPro.setItems(beans);
        gridPro.addCellEditStartedListener(e -> {
            if (e.isFromClient()) {
                var span = new Span("Cell edit: " + e.getItem().getName());
                span.setId(e.getItem().getName());
                add(span);
            }
        });
        add(gridPro);
    }

    /**
     * A custom editor that is an AbstractCompositeField rather than an
     * AbstractField.
     */
    public static class CompositeEditor
            extends AbstractCompositeField<TextField, CompositeEditor, String> {

        public CompositeEditor() {
            super("");
            getContent().addValueChangeListener(
                    event -> setModelValue(event.getValue(), true));
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            getContent().setValue(
                    newPresentationValue == null ? "" : newPresentationValue);
        }
    }

    /**
     * A custom editor implementing HasValueAndElement directly, so it has no
     * AbstractFieldSupport backed client value path.
     */
    @Tag("div")
    public static class ForeignEditor extends Component implements
            HasValueAndElement<ComponentValueChangeEvent<ForeignEditor, String>, String> {

        private String value = "";

        @Override
        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public Registration addValueChangeListener(
                HasValue.ValueChangeListener<? super ComponentValueChangeEvent<ForeignEditor, String>> listener) {
            return () -> {
            };
        }
    }

    public enum YesNo {
        YES, NO
    }

    public static class Bean {
        private String name;
        private String description;
        private Boolean checked;
        private YesNo yesNo;

        public Bean(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public Boolean getChecked() {
            return checked;
        }

        public void setChecked(Boolean checked) {
            this.checked = checked;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public YesNo getYesNo() {
            return yesNo;
        }

        public void setYesNo(YesNo yesNo) {
            this.yesNo = yesNo;
        }
    }
}
