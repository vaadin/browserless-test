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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;

/**
 * Browserless test driver for the {@link Geolocation} facade. Provides a
 * state-based simulation of the browser's geolocation: tests describe the world
 * (the user's permission and the sensor's reading), then exercise the
 * application, which observes that world through the {@code Geolocation} API.
 * <p>
 * Two orthogonal axes drive the model:
 * <ul>
 * <li><b>Permission</b> — {@link #grantPermission()},
 * {@link #denyPermission()}, {@link #resetPermission()},
 * {@link #simulateUnsupported()}. Default state is
 * {@link GeolocationAvailability#PROMPT PROMPT}: the browser would show a
 * permission dialog on the next call.</li>
 * <li><b>Sensor reading</b> — {@link #setLocation(double, double)},
 * {@link #setLocation(GeolocationPosition)}, {@link #clearLocation()},
 * {@link #setUnavailable(GeolocationErrorCode, String)},
 * {@link #clearUnavailable()}. Default state: no fix.</li>
 * </ul>
 * <p>
 * Resolution rules for {@code Geolocation.get(...)}:
 * <ul>
 * <li>{@link GeolocationAvailability#PROMPT PROMPT} or
 * {@link GeolocationAvailability#UNKNOWN UNKNOWN}: the call stays pending until
 * the permission is decided.</li>
 * <li>{@link GeolocationAvailability#DENIED DENIED} or
 * {@link GeolocationAvailability#UNSUPPORTED UNSUPPORTED}: the call resolves
 * with an error.</li>
 * <li>{@link GeolocationAvailability#GRANTED GRANTED}: the call resolves with
 * the cached fix if {@link #setLocation} was called, with the cached error if
 * {@link #setUnavailable} was called, or stays pending otherwise.</li>
 * </ul>
 * <p>
 * Trackers behave the same way: the active watch fires on
 * {@link #setLocation(double, double) setLocation} (when permission is granted)
 * and on {@link #setUnavailable(GeolocationErrorCode, String) setUnavailable};
 * calling {@link #denyPermission()} delivers a {@code PERMISSION_DENIED} error
 * to active watches and stops them.
 * <p>
 * Obtain via {@link #current()} or {@link #forUI(UI)}: idempotent, both create
 * the simulator on the first call and return the same instance afterward.
 */
public final class GeolocationSimulator implements Serializable {

    private final BrowserlessGeolocationClient client;

    private GeolocationSimulator(BrowserlessGeolocationClient client) {
        this.client = client;
    }

    /**
     * Returns the simulator bound to {@link UI#getCurrent()}.
     *
     * @return the simulator for the current UI
     */
    public static GeolocationSimulator current() {
        return forUI(UI.getCurrent());
    }

    /**
     * Returns the simulator bound to the given UI, attaching an in-memory
     * geolocation client on the first call. Idempotent.
     *
     * @param ui
     *            the UI to attach to
     * @return the simulator bound to the in-memory client
     */
    public static GeolocationSimulator forUI(UI ui) {
        GeolocationSimulator existing = ComponentUtil.getData(ui,
                GeolocationSimulator.class);
        if (existing != null) {
            return existing;
        }
        BrowserlessGeolocationClient client = new BrowserlessGeolocationClient();
        ui.getGeolocation().setClient(client);
        GeolocationSimulator simulator = new GeolocationSimulator(client);
        ComponentUtil.setData(ui, GeolocationSimulator.class, simulator);
        return simulator;
    }

    /**
     * Sets permission to {@link GeolocationAvailability#GRANTED GRANTED}.
     * Pending {@code get(...)} calls resolve with the current fix or error if
     * either has been set; otherwise they stay pending until one is.
     */
    public void grantPermission() {
        client.setAvailability(GeolocationAvailability.GRANTED);
    }

    /**
     * Sets permission to {@link GeolocationAvailability#DENIED DENIED}. Any
     * pending {@code get(...)} call resolves with a {@code PERMISSION_DENIED}
     * error. Active watches receive a {@code PERMISSION_DENIED} error and are
     * stopped.
     */
    public void denyPermission() {
        client.setAvailability(GeolocationAvailability.DENIED);
        client.deliverDeniedToWatches();
    }

    /**
     * Resets permission to {@link GeolocationAvailability#PROMPT PROMPT} — the
     * default starting state, equivalent to a fresh page where the user has not
     * yet responded to the permission dialog.
     */
    public void resetPermission() {
        client.setAvailability(GeolocationAvailability.PROMPT);
    }

    /**
     * Sets availability to {@link GeolocationAvailability#UNSUPPORTED
     * UNSUPPORTED}, simulating a browser without the Geolocation API or a page
     * context where it is unusable. Pending {@code get(...)} calls resolve with
     * a {@code POSITION_UNAVAILABLE} error.
     */
    public void simulateUnsupported() {
        client.setAvailability(GeolocationAvailability.UNSUPPORTED);
    }

    /**
     * Sets the cached sensor fix. When permission is granted, pending
     * {@code get(...)} calls resolve with this position and active watches
     * receive it.
     *
     * @param position
     *            the position the sensor reports
     */
    public void setLocation(GeolocationPosition position) {
        client.setCachedFix(position);
    }

    /**
     * Convenience overload constructing a {@link GeolocationPosition} with a
     * current-time timestamp; altitude, altitude accuracy, heading and speed
     * are {@code null}.
     *
     * @param latitude
     *            latitude in degrees
     * @param longitude
     *            longitude in degrees
     * @param accuracy
     *            horizontal accuracy in metres
     */
    public void setLocation(double latitude, double longitude,
            double accuracy) {
        GeolocationCoordinates coords = new GeolocationCoordinates(latitude,
                longitude, accuracy, null, null, null, null);
        setLocation(
                new GeolocationPosition(coords, System.currentTimeMillis()));
    }

    /**
     * Convenience overload using a default accuracy of 10 metres.
     *
     * @param latitude
     *            latitude in degrees
     * @param longitude
     *            longitude in degrees
     */
    public void setLocation(double latitude, double longitude) {
        setLocation(latitude, longitude, 10.0);
    }

    /**
     * Clears the cached sensor fix. Pending {@code get(...)} calls and active
     * watches are unaffected; they wait for the next state change.
     */
    public void clearLocation() {
        client.setCachedFix(null);
    }

    /**
     * Sets a sticky sensor error. When permission is granted, pending
     * {@code get(...)} calls resolve with this error and active watches receive
     * it. Setting an error clears any cached fix.
     *
     * @param code
     *            the error code
     * @param message
     *            the error message
     */
    public void setUnavailable(GeolocationErrorCode code, String message) {
        client.setCachedError(new GeolocationError(code.code(), message));
    }

    /**
     * Clears any cached sensor error. Pending {@code get(...)} calls stay
     * pending; active watches are unaffected.
     */
    public void clearUnavailable() {
        client.setCachedError(null);
    }

    /**
     * Returns the pending one-shot {@code Geolocation.get(...)} requests in
     * arrival order.
     *
     * @return an unmodifiable view of pending requests
     */
    public List<GeolocationRequest> requests() {
        List<GeolocationRequest> out = new ArrayList<>();
        for (BrowserlessGeolocationClient.PendingGet p : client.pending()) {
            out.add(new RequestAdapter(p, client));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Returns the most recent pending {@code Geolocation.get(...)} request, if
     * any.
     *
     * @return the most recent pending request, or empty
     */
    public Optional<GeolocationRequest> lastRequest() {
        BrowserlessGeolocationClient.PendingGet last = client.pending()
                .peekLast();
        return last == null ? Optional.empty()
                : Optional.of(new RequestAdapter(last, client));
    }

    /**
     * Returns currently active tracker sessions for this UI.
     *
     * @return an unmodifiable view of active tracker sessions
     */
    public List<GeolocationTrackerSession> activeTrackers() {
        List<GeolocationTrackerSession> out = new ArrayList<>();
        for (BrowserlessGeolocationClient.ActiveWatch w : client.watches()) {
            if (w.isActive()) {
                out.add(new TrackerSessionAdapter(w));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static final class RequestAdapter implements GeolocationRequest {
        private final BrowserlessGeolocationClient.PendingGet entry;
        private final BrowserlessGeolocationClient client;

        RequestAdapter(BrowserlessGeolocationClient.PendingGet entry,
                BrowserlessGeolocationClient client) {
            this.entry = entry;
            this.client = client;
        }

        @Override
        public @Nullable GeolocationOptions options() {
            return entry.options;
        }

        @Override
        public boolean isPending() {
            return !entry.resolved;
        }

        @Override
        public void respondWith(GeolocationPosition position) {
            client.pending().remove(entry);
            entry.respondWith(position);
        }

        @Override
        public void respondWith(GeolocationError error) {
            client.pending().remove(entry);
            entry.respondWith(error);
        }
    }

    private static final class TrackerSessionAdapter
            implements GeolocationTrackerSession {
        private final BrowserlessGeolocationClient.ActiveWatch watch;

        TrackerSessionAdapter(BrowserlessGeolocationClient.ActiveWatch watch) {
            this.watch = watch;
        }

        @Override
        public Component owner() {
            return watch.owner;
        }

        @Override
        public @Nullable GeolocationOptions options() {
            return watch.options;
        }

        @Override
        public boolean isActive() {
            return watch.isActive();
        }
    }
}
