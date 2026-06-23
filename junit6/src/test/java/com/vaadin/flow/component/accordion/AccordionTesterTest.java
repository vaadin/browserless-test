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
package com.vaadin.flow.component.accordion;

import java.util.OptionalInt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class AccordionTesterTest extends BrowserlessTest {

    AccordionView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(AccordionView.class);
        view = navigate(AccordionView.class);
    }

    @Test
    void getPanelBySummary_returnsCorrectPanel() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Red");
        Assertions.assertSame(view.redPanel, wrap.getPanel("Red"));
        wrap.openDetails("Disabled");
        Assertions.assertSame(view.disabledPanel, wrap.getPanel("Disabled"));
    }

    @Test
    void closedPanel_getPanelThrows() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.accordion).getPanel("Green"));
    }

    @Test
    void isOpen_seesCorrectPanel() {
        view.accordion.open(view.redPanel);

        final AccordionTester<Accordion> wrap = test(view.accordion);
        Assertions.assertTrue(wrap.isOpen("Red"), "Red should be open");
        Assertions.assertFalse(wrap.isOpen("Green"), "Only red should be open");

        view.accordion.open(view.greenPanel);

        Assertions.assertFalse(wrap.isOpen("Red"),
                "Red should close after green is open");
    }

    @Test
    void hasPanel_returnsTrueForExistingPanel() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        Assertions.assertTrue(wrap.hasPanel("Green"),
                "Green panel should exist");
        Assertions.assertFalse(wrap.hasPanel("Orange"),
                "No Orange panel is added");
    }

    @Test
    void attach_noInitialOpenedChangeEventFired() {
        Assertions.assertTrue(view.openedChangeEvents.isEmpty(),
                "No OpenedChangeEvent should be fired on initial attach, but got "
                        + view.openedChangeEvents.size());
    }

    @Test
    void openAndClose_serverSide_eventsFiredWithFromClientFalse() {
        view.accordion.open(view.greenPanel);

        Assertions.assertEquals(1, view.openedChangeEvents.size(),
                "Opening a panel should fire a single OpenedChangeEvent");
        Accordion.OpenedChangeEvent openedEvent = view.openedChangeEvents
                .get(0);
        Assertions.assertFalse(openedEvent.isFromClient(),
                "Server-side open should report isFromClient() == false");
        Assertions.assertEquals(OptionalInt.of(1),
                openedEvent.getOpenedIndex());

        view.accordion.close();

        Assertions.assertEquals(2, view.openedChangeEvents.size(),
                "Closing the accordion should fire an OpenedChangeEvent");
        Accordion.OpenedChangeEvent closedEvent = view.openedChangeEvents
                .get(1);
        Assertions.assertFalse(closedEvent.isFromClient(),
                "Server-side close should report isFromClient() == false");
        Assertions.assertTrue(closedEvent.getOpenedIndex().isEmpty(),
                "Closed accordion should report an empty opened index");
    }
}
