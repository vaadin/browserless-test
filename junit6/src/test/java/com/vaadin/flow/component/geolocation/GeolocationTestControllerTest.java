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
package com.vaadin.flow.component.geolocation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(packages = "com.vaadin.flow.component.geolocation")
public class GeolocationTestControllerTest extends BrowserlessTest {

    @Test
    void install_isIdempotent() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        GeolocationTestController again = GeolocationTestController
                .install(UI.getCurrent());
        assertSame(controller, again,
                "install should return the same controller on repeat call");
    }

    @Test
    void setAvailability_updatesFacadeSignal() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        controller.setAvailability(GeolocationAvailability.GRANTED);
        assertEquals(GeolocationAvailability.GRANTED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void simulateUnsupported_setsAvailabilityToUnsupported() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        controller.simulateUnsupported();
        assertEquals(GeolocationAvailability.UNSUPPORTED,
                controller.currentAvailability());
        assertEquals(GeolocationAvailability.UNSUPPORTED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void simulatePosition_resolvesOldestPendingGet() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());

        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        controller.simulatePosition(60.1699, 24.9384, 10.0);

        assertEquals(1, received.size());
        GeolocationPosition pos = (GeolocationPosition) received.get(0);
        assertEquals(60.1699, pos.coords().latitude());
        assertEquals(24.9384, pos.coords().longitude());
        assertEquals(10.0, pos.coords().accuracy());
    }

    @Test
    void simulateError_resolvesPendingGetWithError() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());

        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        controller.simulateError(GeolocationErrorCode.PERMISSION_DENIED,
                "denied");

        assertEquals(1, received.size());
        GeolocationError err = (GeolocationError) received.get(0);
        assertEquals(1, err.code());
        assertEquals("denied", err.message());
    }

    @Test
    void simulatePosition_withNoPendingRequest_throws() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        assertThrows(IllegalStateException.class,
                () -> controller.simulatePosition(60.0, 25.0, 10.0));
    }

    @Test
    void pushPosition_updatesTrackerSignal() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);

        controller.pushPosition(tracker, 60.0, 25.0, 10.0);

        GeolocationPosition pos = (GeolocationPosition) tracker.valueSignal()
                .peek();
        assertEquals(60.0, pos.coords().latitude());
    }

    @Test
    void pushError_updatesTrackerSignal() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);

        controller.pushError(tracker, GeolocationErrorCode.TIMEOUT,
                "took too long");

        GeolocationError err = (GeolocationError) tracker.valueSignal().peek();
        assertEquals(3, err.code());
    }

    @Test
    void pushPosition_afterStop_throws() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);
        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);
        tracker.stop();

        assertThrows(IllegalArgumentException.class,
                () -> controller.pushPosition(tracker, 60.0, 25.0, 10.0));
    }

    @Test
    void lastRequest_exposesOptions() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        GeolocationOptions opts = GeolocationOptions.builder()
                .highAccuracy(true).timeout(Duration.ofSeconds(7)).build();
        UI.getCurrent().getGeolocation().get(opts, r -> {
        });

        GeolocationRequest req = controller.lastRequest().orElseThrow();
        assertNotNull(req.options());
        assertEquals(Boolean.TRUE, req.options().enableHighAccuracy());
        assertEquals(7000, req.options().timeout());
        assertTrue(req.isPending());
    }

    @Test
    void respondWith_resolvesViaInspection() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        GeolocationRequest req = controller.lastRequest().orElseThrow();
        req.respondWith(new GeolocationPosition(
                new GeolocationCoordinates(1, 2, 3, null, null, null, null),
                100L));

        assertEquals(1, received.size());
        assertFalse(req.isPending());
    }

    @Test
    void activeTrackers_listsRunningTracker() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);

        List<GeolocationTrackerSession> active = controller.activeTrackers();
        assertEquals(1, active.size());
        assertSame(owner, active.get(0).owner());
        assertTrue(active.get(0).isActive());

        tracker.stop();
        assertEquals(0, controller.activeTrackers().size());
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }
}
