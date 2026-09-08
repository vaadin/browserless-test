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

import java.util.ArrayList;
import java.util.List;
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
    void openDetails_viaTester_eventFiredWithFromClientTrue() {
        final List<Accordion.OpenedChangeEvent> events = new ArrayList<>();
        view.accordion.addOpenedChangeListener(events::add);

        test(view.accordion).openDetails("Green");

        Assertions.assertEquals(1, events.size(),
                "Opening a panel should fire a single OpenedChangeEvent");
        Accordion.OpenedChangeEvent event = events.get(0);
        Assertions.assertTrue(event.isFromClient(),
                "Tester open simulates a user interaction and should report isFromClient() == true");
        Assertions.assertEquals(OptionalInt.of(1), event.getOpenedIndex());
        Assertions.assertTrue(test(view.accordion).isOpen("Green"),
                "Green should be open");
    }

    @Test
    void closeDetails_viaTester_eventFiredWithFromClientTrue() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Green");

        final List<Accordion.OpenedChangeEvent> events = new ArrayList<>();
        view.accordion.addOpenedChangeListener(events::add);

        wrap.closeDetails();

        Assertions.assertEquals(1, events.size(),
                "Closing the open panel should fire a single OpenedChangeEvent");
        Accordion.OpenedChangeEvent event = events.get(0);
        Assertions.assertTrue(event.isFromClient(),
                "Tester close simulates a user interaction and should report isFromClient() == true");
        Assertions.assertTrue(event.getOpenedIndex().isEmpty(),
                "Closed accordion should report an empty opened index");
        Assertions.assertFalse(wrap.isOpen("Green"), "Green should be closed");
    }

    @Test
    void closeDetails_bySummary_closesThePanel() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Green");

        wrap.closeDetails("Green");

        Assertions.assertFalse(wrap.isOpen("Green"), "Green should be closed");
    }

    @Test
    void noOpenPanel_closeDetails_throws() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Green");
        wrap.closeDetails();

        Assertions.assertThrows(IllegalStateException.class, wrap::closeDetails,
                "Closing an accordion with no open panel should throw");
    }

    @Test
    void closedPanel_closeDetailsBySummary_throws() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Green");

        Assertions.assertThrows(IllegalStateException.class,
                () -> wrap.closeDetails("Red"),
                "Closing a panel that is not the open one should throw");
    }

    @Test
    void unknownSummary_closeAndToggle_throw() {
        final AccordionTester<Accordion> wrap = test(view.accordion);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> wrap.closeDetails("Orange"),
                "Closing a panel that does not exist should throw");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> wrap.toggleDetails("Orange"),
                "Toggling a panel that does not exist should throw");
    }

    @Test
    void toggleDetails_opensAndClosesTheSamePanel() {
        final AccordionTester<Accordion> wrap = test(view.accordion);

        wrap.toggleDetails("Green");
        Assertions.assertTrue(wrap.isOpen("Green"),
                "Toggling a closed panel should open it");

        wrap.toggleDetails("Green");
        Assertions.assertFalse(wrap.isOpen("Green"),
                "Toggling an open panel should close it");
        Assertions.assertTrue(view.accordion.getOpenedPanel().isEmpty(),
                "No panel should be open after toggling the open one closed");
    }

    @Test
    void otherPanelOpen_toggleDetails_switchesTheOpenPanel() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Red");

        final List<Accordion.OpenedChangeEvent> events = new ArrayList<>();
        view.accordion.addOpenedChangeListener(events::add);

        wrap.toggleDetails("Green");

        Assertions.assertTrue(wrap.isOpen("Green"),
                "Toggling a closed panel should open it");
        Assertions.assertFalse(wrap.isOpen("Red"),
                "Opening a panel should close whichever panel was open before");
        Assertions.assertEquals(1, events.size(),
                "Switching the open panel should fire a single OpenedChangeEvent");
        Assertions.assertEquals(OptionalInt.of(1),
                events.get(0).getOpenedIndex(),
                "The event should report the index of the newly opened panel");
    }

    @Test
    void notUsableAccordion_closeAndToggle_throw() {
        final AccordionTester<Accordion> wrap = test(view.accordion);
        wrap.openDetails("Green");
        view.accordion.getElement().setEnabled(false);

        Assertions.assertThrows(IllegalStateException.class, wrap::closeDetails,
                "Closing a disabled accordion should throw");
        Assertions.assertThrows(IllegalStateException.class,
                () -> wrap.closeDetails("Green"),
                "Closing a disabled accordion by summary should throw");
        Assertions.assertThrows(IllegalStateException.class,
                () -> wrap.toggleDetails("Green"),
                "Toggling a disabled accordion should throw");
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
