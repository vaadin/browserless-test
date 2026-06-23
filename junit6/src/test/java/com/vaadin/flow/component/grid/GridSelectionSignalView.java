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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

@Tag("div")
@Route(value = "grid-selection-signal", registerAtStartup = false)
public class GridSelectionSignalView extends Component
        implements HasComponents {

    final Grid<Person> grid;
    final ValueSignal<Person> selectedPerson = new ValueSignal<>(null);
    final Person person1;
    final Person person2;

    public GridSelectionSignalView() {
        grid = new Grid<>();
        grid.addColumn(Person::getFirstName).setHeader("First Name");

        person1 = Person.createTestPerson1();
        person2 = Person.createTestPerson2();
        grid.setItems(person1, person2);

        grid.asSingleSelect().bindValue(selectedPerson, selectedPerson::set);

        add(grid);
    }
}
