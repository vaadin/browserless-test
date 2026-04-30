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
public class GeolocationSimulatorTest extends BrowserlessTest {

    @Test
    void of_isIdempotent() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        GeolocationSimulator again = GeolocationSimulator.of(UI.getCurrent());
        assertSame(simulator, again,
                "of should return the same simulator on repeat call");
    }

    @Test
    void setAvailability_updatesFacadeSignal() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        simulator.setAvailability(GeolocationAvailability.GRANTED);
        assertEquals(GeolocationAvailability.GRANTED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void simulateUnsupported_setsAvailabilityToUnsupported() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        simulator.simulateUnsupported();
        assertEquals(GeolocationAvailability.UNSUPPORTED,
                simulator.currentAvailability());
        assertEquals(GeolocationAvailability.UNSUPPORTED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void respondWithPosition_resolvesOldestPendingGet() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());

        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        simulator.respondWithPosition(60.1699, 24.9384, 10.0);

        assertEquals(1, received.size());
        GeolocationPosition pos = (GeolocationPosition) received.get(0);
        assertEquals(60.1699, pos.coords().latitude());
        assertEquals(24.9384, pos.coords().longitude());
        assertEquals(10.0, pos.coords().accuracy());
    }

    @Test
    void respondWithError_resolvesPendingGetWithError() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());

        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        simulator.respondWithError(GeolocationErrorCode.PERMISSION_DENIED,
                "denied");

        assertEquals(1, received.size());
        GeolocationError err = (GeolocationError) received.get(0);
        assertEquals(1, err.code());
        assertEquals("denied", err.message());
    }

    @Test
    void respondWithPosition_withNoPendingRequest_throws() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        assertThrows(IllegalStateException.class,
                () -> simulator.respondWithPosition(60.0, 25.0, 10.0));
    }

    @Test
    void pushPosition_updatesTrackerSignal() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);

        simulator.pushPosition(60.0, 25.0, 10.0);

        GeolocationPosition pos = (GeolocationPosition) tracker.valueSignal()
                .peek();
        assertEquals(60.0, pos.coords().latitude());
    }

    @Test
    void pushError_updatesTrackerSignal() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);

        simulator.pushError(GeolocationErrorCode.TIMEOUT, "took too long");

        GeolocationError err = (GeolocationError) tracker.valueSignal().peek();
        assertEquals(3, err.code());
    }

    @Test
    void pushPosition_afterStop_doesNotUpdateStoppedTracker() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);
        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);
        // Record the value before stop
        Object valueBefore = tracker.valueSignal().peek();
        tracker.stop();

        // With broadcast semantics, pushPosition is a no-op when no watches
        // are active. The stopped tracker's signal must not be updated.
        simulator.pushPosition(60.0, 25.0, 10.0);

        assertSame(valueBefore, tracker.valueSignal().peek(),
                "Stopped tracker signal must not be updated by broadcast push");
        assertEquals(0, simulator.activeTrackers().size(),
                "No active trackers should remain after stop");
    }

    @Test
    void lastRequest_exposesOptions() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        GeolocationOptions opts = GeolocationOptions.builder()
                .highAccuracy(true).timeout(Duration.ofSeconds(7)).build();
        UI.getCurrent().getGeolocation().get(opts, r -> {
        });

        GeolocationRequest req = simulator.lastRequest().orElseThrow();
        assertNotNull(req.options());
        assertEquals(Boolean.TRUE, req.options().enableHighAccuracy());
        assertEquals(7000, req.options().timeout());
        assertTrue(req.isPending());
    }

    @Test
    void respondWith_resolvesViaInspection() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        GeolocationRequest req = simulator.lastRequest().orElseThrow();
        req.respondWith(new GeolocationPosition(
                new GeolocationCoordinates(1, 2, 3, null, null, null, null),
                100L));

        assertEquals(1, received.size());
        assertFalse(req.isPending());
    }

    @Test
    void activeTrackers_listsRunningTracker() {
        GeolocationSimulator simulator = GeolocationSimulator
                .of(UI.getCurrent());
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(owner);

        List<GeolocationTrackerSession> active = simulator.activeTrackers();
        assertEquals(1, active.size());
        assertSame(owner, active.get(0).owner());
        assertTrue(active.get(0).isActive());

        tracker.stop();
        assertEquals(0, simulator.activeTrackers().size());
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }
}
