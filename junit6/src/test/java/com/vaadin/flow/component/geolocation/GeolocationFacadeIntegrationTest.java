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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(packages = "com.vaadin.flow.component.geolocation")
public class GeolocationFacadeIntegrationTest extends BrowserlessTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    @Test
    void getPosition_withGrantedPermissionAndLocation_callbackReceivesPosition() {
        GeolocationSimulator geo = GeolocationSimulator.current();
        geo.grantPermission();
        geo.setLocation(60.1699, 24.9384, 10.0);

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        Geolocation.getPosition(received::add, errors::add);

        assertEquals(1, received.size());
        assertEquals(60.1699, received.getFirst().coords().latitude());

        assertTrue(errors.isEmpty(), "No errors should be reported");
    }

    @Test
    void getPosition_withDeniedPermission_callbackReceivesPermissionDeniedError() {
        GeolocationSimulator geo = GeolocationSimulator.current();
        geo.denyPermission();

        List<GeolocationPosition> received = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        Geolocation.getPosition(received::add, errors::add);

        assertEquals(1, errors.size());
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(),
                errors.getFirst().code());

        assertTrue(received.isEmpty(), "No positions should be reported");
    }

    @Test
    void watchPosition_setLocationFiresPositionEvent() {
        GeolocationSimulator geo = GeolocationSimulator.current();
        geo.grantPermission();
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        GeolocationCoordinates coords = new GeolocationCoordinates(60.1699,
                24.9384, 10.0, 25.5, 5.0, 90.0, 1.5);
        GeolocationPosition position = new GeolocationPosition(coords,
                1700000000000L);
        geo.setLocation(position);

        assertInstanceOf(GeolocationPosition.class,
                watcher.positionSignal().peek());
        GeolocationPosition pos = (GeolocationPosition) watcher.positionSignal()
                .peek();
        assertEquals(60.1699, pos.coords().latitude());
        assertEquals(25.5, pos.coords().altitude());
        assertEquals(1700000000000L, pos.timestamp());
    }

    @Test
    void watchPosition_setUnavailableFiresErrorEvent() {
        GeolocationSimulator geo = GeolocationSimulator.current();
        geo.grantPermission();
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        geo.setUnavailable(GeolocationErrorCode.PERMISSION_DENIED,
                "User denied geolocation");

        assertInstanceOf(GeolocationError.class,
                watcher.positionSignal().peek());
        GeolocationError error = (GeolocationError) watcher.positionSignal()
                .peek();
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(),
                error.code());
        assertEquals("User denied geolocation", error.debugInfo());
    }

    @Test
    void watchPosition_stateTransitionsFromErrorToPosition() {
        GeolocationSimulator geo = GeolocationSimulator.current();
        geo.grantPermission();
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        geo.setUnavailable(GeolocationErrorCode.TIMEOUT, "Timeout");
        assertInstanceOf(GeolocationError.class,
                watcher.positionSignal().peek());

        geo.setLocation(60.1699, 24.9384, 10.0);
        assertInstanceOf(GeolocationPosition.class,
                watcher.positionSignal().peek());
    }

    @Test
    void concreteView_respondsToPreSetLocation() {
        GeolocationSimulator geo = GeolocationSimulator.current();
        geo.grantPermission();
        geo.setLocation(60.1699, 24.9384, 10.0);

        SampleView view = navigate(SampleView.class);

        assertEquals("60.16990", view.lastLatitude.getText());
    }

    @Route("sample-geo")
    public static class SampleView extends Div {
        final Span lastLatitude = new Span();

        public SampleView() {
            add(lastLatitude);
            Geolocation.getPosition(pos -> {
                lastLatitude.setText(
                        String.format("%.5f", pos.coords().latitude()));
            }, err -> {
            });
        }
    }
}
