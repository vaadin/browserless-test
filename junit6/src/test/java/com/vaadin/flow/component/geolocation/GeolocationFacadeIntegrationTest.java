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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ViewPackages(packages = "com.vaadin.flow.component.geolocation")
public class GeolocationFacadeIntegrationTest extends BrowserlessTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    @Test
    void get_callbackReceivesPosition() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());

        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        controller.simulatePosition(60.1699, 24.9384, 10.0);

        assertEquals(1, received.size());
        assertInstanceOf(GeolocationPosition.class, received.get(0));
        assertEquals(60.1699,
                ((GeolocationPosition) received.get(0)).coords().latitude());
    }

    @Test
    void get_callbackReceivesError() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());

        List<GeolocationOutcome> received = new ArrayList<>();
        UI.getCurrent().getGeolocation().get(received::add);

        controller.simulateError(GeolocationErrorCode.PERMISSION_DENIED,
                "denied");

        assertEquals(1, received.size());
        assertInstanceOf(GeolocationError.class, received.get(0));
        assertEquals(1, ((GeolocationError) received.get(0)).code());
    }

    @Test
    void track_signalUpdatesOnPositionEvent() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(component);

        GeolocationCoordinates coords = new GeolocationCoordinates(60.1699,
                24.9384, 10.0, 25.5, 5.0, 90.0, 1.5);
        GeolocationPosition position = new GeolocationPosition(coords,
                1700000000000L);
        controller.pushPosition(tracker, position);

        assertInstanceOf(GeolocationPosition.class,
                tracker.valueSignal().peek());
        GeolocationPosition pos = (GeolocationPosition) tracker.valueSignal()
                .peek();
        assertEquals(60.1699, pos.coords().latitude());
        assertEquals(24.9384, pos.coords().longitude());
        assertEquals(10.0, pos.coords().accuracy());
        assertEquals(25.5, pos.coords().altitude());
        assertEquals(5.0, pos.coords().altitudeAccuracy());
        assertEquals(90.0, pos.coords().heading());
        assertEquals(1.5, pos.coords().speed());
        assertEquals(1700000000000L, pos.timestamp());
    }

    @Test
    void track_signalUpdatesOnErrorEvent() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(component);

        controller.pushError(tracker, GeolocationErrorCode.PERMISSION_DENIED,
                "User denied geolocation");

        assertInstanceOf(GeolocationError.class, tracker.valueSignal().peek());
        GeolocationError error = (GeolocationError) tracker.valueSignal()
                .peek();
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(),
                error.code());
        assertEquals("User denied geolocation", error.message());
    }

    @Test
    void track_stateTransitionsFromErrorToPosition() {
        GeolocationTestController controller = GeolocationTestController
                .install(UI.getCurrent());
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        GeolocationTracker tracker = UI.getCurrent().getGeolocation()
                .track(component);

        controller.pushError(tracker, GeolocationErrorCode.TIMEOUT, "Timeout");
        assertInstanceOf(GeolocationError.class, tracker.valueSignal().peek());

        controller.pushPosition(tracker, 60.1699, 24.9384, 10.0);
        assertInstanceOf(GeolocationPosition.class,
                tracker.valueSignal().peek());
    }
}
