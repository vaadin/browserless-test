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
package com.vaadin.flow.component.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;

@Tag("div")
@Route(value = "dashboard", registerAtStartup = false)
public class DashboardView extends Component implements HasComponents {

    final Dashboard dashboard = new Dashboard();

    final DashboardWidget widget1 = new DashboardWidget("Widget 1");
    final DashboardWidget widget2 = new DashboardWidget("Widget 2");
    final DashboardWidget widget3 = new DashboardWidget("Widget 3");

    final DashboardSection section;
    final DashboardWidget sectionWidget1 = new DashboardWidget(
            "Section Widget 1");
    final DashboardWidget sectionWidget2 = new DashboardWidget(
            "Section Widget 2");

    final List<DashboardItemMovedEvent> movedEvents = new ArrayList<>();
    final List<DashboardItemResizedEvent> resizedEvents = new ArrayList<>();
    final List<DashboardItemRemovedEvent> removedEvents = new ArrayList<>();

    public DashboardView() {
        dashboard.setEditable(true);
        dashboard.add(List.of(widget1, widget2, widget3));
        section = dashboard.addSection("Section");
        section.add(List.of(sectionWidget1, sectionWidget2));

        dashboard.addItemMovedListener(movedEvents::add);
        dashboard.addItemResizedListener(resizedEvents::add);
        dashboard.addItemRemovedListener(removedEvents::add);

        add(dashboard);
    }
}
