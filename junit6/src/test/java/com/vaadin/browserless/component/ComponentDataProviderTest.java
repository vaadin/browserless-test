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
package com.vaadin.browserless.component;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.provider.DataProvider;

import static org.junit.jupiter.api.Assertions.assertSame;

class ComponentDataProviderTest {

    static Stream<Arguments> components() {
        Grid<String> grid = new Grid<>();
        grid.setItems("a", "b");
        ListBox<String> listBox = new ListBox<>();
        listBox.setItems("a", "b");
        CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setItems("a", "b");
        RadioButtonGroup<String> radioButtonGroup = new RadioButtonGroup<>();
        radioButtonGroup.setItems("a", "b");
        return Stream.of(Arguments.of(grid, grid.getDataProvider()),
                Arguments.of(listBox, listBox.getDataProvider()),
                Arguments.of(checkboxGroup, checkboxGroup.getDataProvider()),
                Arguments.of(radioButtonGroup,
                        radioButtonGroup.getDataProvider()));
    }

    @ParameterizedTest
    @MethodSource("components")
    void getDataProvider_returnsTheComponentsDataProvider(Component component,
            DataProvider<?, ?> expected) {
        assertSame(expected, GridKt.getDataProvider(component));
    }
}
