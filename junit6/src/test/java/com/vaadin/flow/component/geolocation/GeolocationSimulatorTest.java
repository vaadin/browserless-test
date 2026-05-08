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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(packages = "com.vaadin.flow.component.geolocation")
public class GeolocationSimulatorTest extends BrowserlessTest {

    @Test
    void current_isIdempotent() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        GeolocationSimulator again = GeolocationSimulator.current();
        assertSame(simulator, again,
                "current() should return the same simulator on repeat call");
    }

    @Test
    void defaultAvailability_isPrompt() {
        GeolocationSimulator.current();
        assertEquals(GeolocationAvailability.PROMPT,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void grantPermission_updatesFacadeSignal() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        assertEquals(GeolocationAvailability.GRANTED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void denyPermission_updatesFacadeSignal() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.denyPermission();
        assertEquals(GeolocationAvailability.DENIED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void simulateUnsupported_setsAvailabilityToUnsupported() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.simulateUnsupported();
        assertEquals(GeolocationAvailability.UNSUPPORTED,
                UI.getCurrent().getGeolocation().availabilitySignal().peek());
    }

    @Test
    void grantPermission_thenSetLocation_resolvesPendingGet() {
        GeolocationSimulator simulator = GeolocationSimulator.current();

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        UI.getCurrent().getGeolocation().getPosition(received::add,
                errors::add);
        assertTrue(received.isEmpty(),
                "get() should stay pending while permission is PROMPT");

        simulator.grantPermission();
        assertTrue(received.isEmpty(),
                "get() should stay pending until a fix is set");

        simulator.setLocation(60.1699, 24.9384, 10.0);
        assertEquals(1, received.size());
        GeolocationPosition pos = received.getFirst();
        assertEquals(60.1699, pos.coords().latitude());

        assertTrue(errors.isEmpty(), "No errors should be reported");
    }

    @Test
    void setLocationFirst_thenGrantPermission_resolvesPendingGet() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.setLocation(60.1699, 24.9384, 10.0);

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        UI.getCurrent().getGeolocation().getPosition(received::add,
                errors::add);
        assertTrue(received.isEmpty(),
                "get() should stay pending until permission is granted");

        simulator.grantPermission();
        assertEquals(1, received.size());

        assertTrue(errors.isEmpty(), "No errors should be reported");
    }

    @Test
    void setLocationBeforeGet_withGrantedPermission_resolvesImmediately() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        simulator.setLocation(60.1699, 24.9384, 10.0);

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        UI.getCurrent().getGeolocation().getPosition(received::add,
                errors::add);

        assertEquals(1, received.size());
        assertTrue(errors.isEmpty(), "No errors should be reported");
    }

    @Test
    void denyPermission_resolvesPendingGetWithPermissionDenied() {
        GeolocationSimulator simulator = GeolocationSimulator.current();

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        UI.getCurrent().getGeolocation().getPosition(received::add,
                errors::add);

        simulator.denyPermission();

        assertEquals(1, errors.size());
        GeolocationError err = errors.getFirst();
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(), err.code());

        assertTrue(received.isEmpty(), "No positions should be reported");
    }

    @Test
    void setUnavailable_withGrantedPermission_resolvesPendingGetWithError() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        UI.getCurrent().getGeolocation().getPosition(received::add,
                errors::add);

        simulator.setUnavailable(GeolocationErrorCode.POSITION_UNAVAILABLE,
                "no fix");

        assertEquals(1, errors.size());
        GeolocationError err = errors.getFirst();
        assertEquals(GeolocationErrorCode.POSITION_UNAVAILABLE.code(),
                err.code());
        assertEquals("no fix", err.message());

        assertTrue(received.isEmpty(), "No positions should be reported");
    }

    @Test
    void setLocation_updatesActiveTrackerSignal() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationWatcher watcher = UI.getCurrent().getGeolocation()
                .watchPosition(owner);

        simulator.setLocation(60.0, 25.0, 10.0);

        GeolocationPosition pos = (GeolocationPosition) watcher.valueSignal()
                .peek();
        assertEquals(60.0, pos.coords().latitude());
    }

    @Test
    void setUnavailable_updatesActiveTrackerSignalWithError() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationWatcher watcher = UI.getCurrent().getGeolocation()
                .watchPosition(owner);

        simulator.setUnavailable(GeolocationErrorCode.TIMEOUT, "took too long");

        GeolocationError err = (GeolocationError) watcher.valueSignal().peek();
        assertEquals(GeolocationErrorCode.TIMEOUT.code(), err.code());
    }

    @Test
    void denyPermission_stopsActiveTrackersWithError() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationWatcher watcher = UI.getCurrent().getGeolocation()
                .watchPosition(owner);

        simulator.denyPermission();

        GeolocationError err = (GeolocationError) watcher.valueSignal().peek();
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(), err.code());
        assertEquals(0, simulator.activeTrackers().size());
    }

    @Test
    void setLocation_multipleTimes_pushesEveryPositionToTracker() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);
        GeolocationWatcher watcher = UI.getCurrent().getGeolocation()
                .watchPosition(owner);

        simulator.setLocation(60.0, 25.0, 10.0);
        assertEquals(60.0, ((GeolocationPosition) watcher.valueSignal().peek())
                .coords().latitude());

        simulator.setLocation(61.0, 25.0, 10.0);
        assertEquals(61.0, ((GeolocationPosition) watcher.valueSignal().peek())
                .coords().latitude());

        simulator.setLocation(62.0, 25.0, 10.0);
        assertEquals(62.0, ((GeolocationPosition) watcher.valueSignal().peek())
                .coords().latitude());
    }

    @Test
    void setLocation_withMultipleTrackers_pushesPositionToAll() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        TestComponent ownerA = new TestComponent();
        TestComponent ownerB = new TestComponent();
        UI.getCurrent().add(ownerA, ownerB);

        GeolocationWatcher watcherA = UI.getCurrent().getGeolocation()
                .watchPosition(ownerA);
        GeolocationWatcher watcherB = UI.getCurrent().getGeolocation()
                .watchPosition(ownerB);

        simulator.setLocation(60.0, 25.0, 10.0);

        GeolocationPosition posA = (GeolocationPosition) watcherA.valueSignal()
                .peek();
        GeolocationPosition posB = (GeolocationPosition) watcherB.valueSignal()
                .peek();
        assertEquals(60.0, posA.coords().latitude());
        assertEquals(60.0, posB.coords().latitude());
        assertEquals(2, simulator.activeTrackers().size());
    }

    @Test
    void setLocation_afterTrackerStop_doesNotUpdateStoppedTracker() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        simulator.grantPermission();
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);
        GeolocationWatcher watcher = UI.getCurrent().getGeolocation()
                .watchPosition(owner);
        Object valueBefore = watcher.valueSignal().peek();
        watcher.stop();

        simulator.setLocation(60.0, 25.0, 10.0);

        assertSame(valueBefore, watcher.valueSignal().peek(),
                "Stopped tracker signal must not be updated");
        assertEquals(0, simulator.activeTrackers().size());
    }

    @Test
    void lastRequest_exposesOptions_whilePending() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        GeolocationOptions opts = GeolocationOptions.builder()
                .highAccuracy(true).timeout(Duration.ofSeconds(7)).build();
        UI.getCurrent().getGeolocation().getPosition(r -> {
        }, e -> {
        }, opts);

        GeolocationRequest req = simulator.lastRequest().orElseThrow();
        assertNotNull(req.options());
        assertEquals(Boolean.TRUE, req.options().enableHighAccuracy());
        assertEquals(7000, req.options().timeout());
        assertTrue(req.isPending());
    }

    @Test
    void activeTrackers_listsRunningTracker() {
        GeolocationSimulator simulator = GeolocationSimulator.current();
        TestComponent owner = new TestComponent();
        UI.getCurrent().add(owner);

        GeolocationWatcher watcher = UI.getCurrent().getGeolocation()
                .watchPosition(owner);

        List<GeolocationTrackerSession> active = simulator.activeTrackers();
        assertEquals(1, active.size());
        assertSame(owner, active.getFirst().owner());
        assertTrue(active.getFirst().isActive());

        watcher.stop();
        assertEquals(0, simulator.activeTrackers().size());
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }
}
