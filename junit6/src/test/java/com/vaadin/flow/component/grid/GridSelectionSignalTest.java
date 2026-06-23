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

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class GridSelectionSignalTest extends BrowserlessTest {

    GridSelectionSignalView view;
    GridTester<Grid<Person>, Person> grid_;

    @BeforeEach
    void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(GridSelectionSignalView.class);
        view = navigate(GridSelectionSignalView.class);
        grid_ = test(view.grid);
    }

    @Test
    void signalValueChanged_gridSelectionUpdated() {
        Assertions.assertTrue(grid_.getSelected().isEmpty(),
                "No selection expected before the signal has a value");

        view.selectedPerson.set(view.person1);
        Assertions.assertEquals(Set.of(view.person1),
                Set.copyOf(grid_.getSelected()),
                "Grid selection should follow the bound signal value");

        view.selectedPerson.set(view.person2);
        Assertions.assertEquals(Set.of(view.person2),
                Set.copyOf(grid_.getSelected()),
                "Grid selection should follow the bound signal value");

        view.selectedPerson.set(null);
        Assertions.assertTrue(grid_.getSelected().isEmpty(),
                "Clearing the signal value should deselect the grid row");
    }

    @Test
    void selectInGrid_signalValueUpdated() {
        grid_.select(0);
        Assertions.assertSame(view.person1, view.selectedPerson.peek(),
                "Selecting a row should propagate to the bound signal");

        grid_.select(1);
        Assertions.assertSame(view.person2, view.selectedPerson.peek(),
                "Selecting another row should propagate to the bound signal");
    }
}
