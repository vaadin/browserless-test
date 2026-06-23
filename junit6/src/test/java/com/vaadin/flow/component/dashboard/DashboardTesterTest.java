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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.CommercialTesterWrappers;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class DashboardTesterTest extends BrowserlessTest
        implements CommercialTesterWrappers {

    DashboardView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(DashboardView.class);
        view = navigate(DashboardView.class);
    }

    @Test
    void moveWidget_reordersRootWidgets_firesEvent() {
        test(view.dashboard).moveWidget(view.widget3, 0);

        Assertions.assertEquals(
                List.of(view.widget3, view.widget1, view.widget2, view.section),
                view.dashboard.getChildren().toList());

        Assertions.assertEquals(1, view.movedEvents.size());
        DashboardItemMovedEvent event = view.movedEvents.get(0);
        Assertions.assertSame(view.widget3, event.getItem());
        Assertions.assertTrue(event.isFromClient());
        Assertions.assertTrue(event.getSection().isEmpty());
    }

    @Test
    void moveWidget_withinSection_reordersSection_firesEventWithSection() {
        test(view.dashboard).moveWidget(view.sectionWidget2, 0);

        Assertions.assertEquals(
                List.of(view.sectionWidget2, view.sectionWidget1),
                view.section.getWidgets());

        Assertions.assertEquals(1, view.movedEvents.size());
        DashboardItemMovedEvent event = view.movedEvents.get(0);
        Assertions.assertSame(view.sectionWidget2, event.getItem());
        Assertions.assertTrue(event.getSection().isPresent());
        Assertions.assertSame(view.section, event.getSection().get());
    }

    @Test
    void resizeWidget_updatesSpans_firesEvent() {
        test(view.dashboard).resizeWidget(view.widget1, 2, 3);

        Assertions.assertEquals(2, view.widget1.getColspan());
        Assertions.assertEquals(3, view.widget1.getRowspan());

        Assertions.assertEquals(1, view.resizedEvents.size());
        DashboardItemResizedEvent event = view.resizedEvents.get(0);
        Assertions.assertSame(view.widget1, event.getItem());
        Assertions.assertTrue(event.isFromClient());
    }

    @Test
    void removeWidget_removesWidget_firesEvent() {
        test(view.dashboard).removeWidget(view.widget2);

        Assertions.assertFalse(
                view.dashboard.getWidgets().contains(view.widget2));
        Assertions
                .assertEquals(
                        List.of(view.widget1, view.widget3, view.sectionWidget1,
                                view.sectionWidget2),
                        view.dashboard.getWidgets());

        Assertions.assertEquals(1, view.removedEvents.size());
        Assertions.assertSame(view.widget2,
                view.removedEvents.get(0).getItem());
    }

    @Test
    void removeWidget_withCustomHandler_letsApplicationDecide() {
        AtomicReference<DashboardItemRemoveEvent> captured = new AtomicReference<>();
        view.dashboard.setItemRemoveHandler(captured::set);

        test(view.dashboard).removeWidget(view.widget1);

        // The handler is invoked but the widget is not removed automatically.
        Assertions.assertNotNull(captured.get());
        Assertions.assertSame(view.widget1, captured.get().getItem());
        Assertions
                .assertTrue(view.dashboard.getWidgets().contains(view.widget1));
        Assertions.assertTrue(view.removedEvents.isEmpty());

        // The application removes it explicitly.
        captured.get().removeItem();
        Assertions.assertFalse(
                view.dashboard.getWidgets().contains(view.widget1));
        Assertions.assertEquals(1, view.removedEvents.size());
    }

    @Test
    void interactions_whenNotEditable_throw() {
        view.dashboard.setEditable(false);
        DashboardTester<Dashboard> tester = test(view.dashboard);

        Assertions.assertThrows(IllegalStateException.class,
                () -> tester.moveWidget(view.widget1, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> tester.resizeWidget(view.widget1, 2, 2));
        Assertions.assertThrows(IllegalStateException.class,
                () -> tester.removeWidget(view.widget1));
    }

    @Test
    void interactions_withForeignWidget_throw() {
        DashboardWidget stray = new DashboardWidget("Stray");
        DashboardTester<Dashboard> tester = test(view.dashboard);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tester.moveWidget(stray, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tester.resizeWidget(stray, 1, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tester.removeWidget(stray));
    }
}
