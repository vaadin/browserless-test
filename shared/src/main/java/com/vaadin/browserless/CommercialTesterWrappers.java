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
package com.vaadin.browserless;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.ChartTester;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardTester;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.gridpro.GridProTester;

/**
 * Provides factory method to create testers for commercial components.
 *
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public interface CommercialTesterWrappers {

    /**
     * Create a tester for the given GridPro instance.
     *
     * @param grid
     *            the GridPro instance to be tested
     * @return a GridProTester instance wrapping the given GridPro
     */
    default <V> GridProTester<GridPro<V>, V> test(GridPro<V> grid) {
        return BaseBrowserlessTest.internalWrap(GridProTester.class, grid);
    }

    /**
     * Create a tester for the given GridPro instance.
     *
     * @param grid
     *            the GridPro instance to be tested
     * @param itemType
     *            the type of the items in the GridPro
     * @return a GridProTester instance wrapping the given GridPro
     */
    default <V> GridProTester<GridPro<V>, V> test(GridPro<V> grid,
            Class<V> itemType) {
        return BaseBrowserlessTest.internalWrap(GridProTester.class, grid);
    }

    /**
     * Create a tester for the given Chart instance.
     *
     * @param chart
     *            the chart instance to be tested
     * @return a ChartTester instance wrapping the given chart
     */
    default ChartTester<Chart> test(Chart chart) {
        return BaseBrowserlessTest.internalWrap(ChartTester.class, chart);
    }

    /**
     * Create a tester for the given Dashboard instance.
     *
     * @param dashboard
     *            the dashboard instance to be tested
     * @return a DashboardTester instance wrapping the given dashboard
     * @since 1.1
     */
    default DashboardTester<Dashboard> test(Dashboard dashboard) {
        return BaseBrowserlessTest.internalWrap(DashboardTester.class,
                dashboard);
    }

}
