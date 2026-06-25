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

import com.vaadin.browserless.BrowserlessAutomationTestSupport;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.automation.Indexable;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
public class GridSelectByIndexAutomationTest extends BrowserlessTest {

    private BasicGridView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(BasicGridView.class);
        view = navigate(BasicGridView.class);
    }

    @Test
    public void grid_resolves_indexable_via_provider_spi() {
        Assertions.assertTrue(
                BrowserlessAutomationTestSupport.driving(view.basicGrid)
                        .of(view.basicGrid).has(Indexable.class),
                "Grid must resolve Indexable via the provider SPI (no concrete cast)");
    }

    @Test
    public void select_by_index_selects_that_row_without_explicit_roundtrip() {
        @SuppressWarnings("unchecked")
        GridTester<?, ?> tester = test(GridTester.class, view.basicGrid);

        tester.select(1); // routes through Indexable; interceptor supplies the
                          // round-trip

        Assertions.assertEquals(Set.of(tester.getRow(1)),
                view.basicGrid.getSelectedItems(),
                "select(1) selects the row at displayed index 1");
    }
}
