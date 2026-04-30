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
 * Browserless test driver for the {@link Geolocation} facade. Attaches an
 * in-memory geolocation client to the UI; tests then drive outcomes (positions,
 * errors, permission state) by calling methods on the simulator.
 * <p>
 * Obtain via {@link #of(UI)}: idempotent, creates and attaches a
 * {@link BrowserlessGeolocationClient} on the first call and returns the same
 * simulator on subsequent calls for the same UI. The simulator is pre-attached
 * to every {@link com.vaadin.browserless.mocks.MockedUI}, so test code only
 * needs to call {@code GeolocationSimulator.of(UI.getCurrent())} to retrieve
 * it.
 */
public final class GeolocationSimulator implements Serializable {

    private final BrowserlessGeolocationClient client;

    private GeolocationSimulator(BrowserlessGeolocationClient client) {
        this.client = client;
    }

    /**
     * Attaches an in-memory geolocation client to the given UI and returns the
     * simulator bound to it. Idempotent: a second call on the same UI returns
     * the simulator already attached.
     *
     * @param ui
     *            the UI to attach to
     * @return the simulator bound to the in-memory client
     */
    public static GeolocationSimulator of(UI ui) {
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
     * Sets the simulated availability and notifies subscribers (including the
     * UI's {@code availabilitySignal()}).
     *
     * @param availability
     *            the new availability state
     */
    public void setAvailability(GeolocationAvailability availability) {
        client.setAvailability(availability);
    }

    /**
     * Convenience: sets availability to
     * {@link GeolocationAvailability#UNSUPPORTED}, simulating an insecure page
     * context or a Permissions-Policy block.
     */
    public void simulateUnsupported() {
        setAvailability(GeolocationAvailability.UNSUPPORTED);
    }

    /**
     * Returns the current simulated availability.
     *
     * @return the current availability
     */
    public GeolocationAvailability currentAvailability() {
        return client.currentAvailability();
    }

    /**
     * Resolves the oldest pending {@code Geolocation.get(...)} request with the
     * given position.
     *
     * @param position
     *            the position to deliver
     * @throws IllegalStateException
     *             if no get() request is pending
     */
    public void respondWithPosition(GeolocationPosition position) {
        nextPending().respondWith(position);
    }

    /**
     * Convenience overload that constructs a {@link GeolocationPosition} with
     * the given coordinates and a current-time timestamp; altitude, altitude
     * accuracy, heading and speed are {@code null}.
     *
     * @param latitude
     *            latitude in degrees
     * @param longitude
     *            longitude in degrees
     * @param accuracy
     *            horizontal accuracy in metres
     * @throws IllegalStateException
     *             if no get() request is pending
     */
    public void respondWithPosition(double latitude, double longitude,
            double accuracy) {
        GeolocationCoordinates coords = new GeolocationCoordinates(latitude,
                longitude, accuracy, null, null, null, null);
        respondWithPosition(
                new GeolocationPosition(coords, System.currentTimeMillis()));
    }

    /**
     * Resolves the oldest pending {@code Geolocation.get(...)} request with the
     * given error.
     *
     * @param code
     *            the error code
     * @param message
     *            the error message
     * @throws IllegalStateException
     *             if no get() request is pending
     */
    public void respondWithError(GeolocationErrorCode code, String message) {
        nextPending().respondWith(new GeolocationError(code.code(), message));
    }

    /**
     * Convenience overload that uses {@code code.name()} as the error message.
     *
     * @param code
     *            the error code
     * @throws IllegalStateException
     *             if no get() request is pending
     */
    public void respondWithError(GeolocationErrorCode code) {
        respondWithError(code, code.name());
    }

    private BrowserlessGeolocationClient.PendingGet nextPending() {
        BrowserlessGeolocationClient.PendingGet entry = client.pending()
                .pollFirst();
        if (entry == null) {
            throw new IllegalStateException(
                    "No pending Geolocation.get() request to resolve");
        }
        return entry;
    }

    /**
     * Broadcasts a position update to every active watch, exactly as a
     * {@code vaadin-geolocation-position} DOM event would. If no watches are
     * active the call is a no-op.
     *
     * @param position
     *            the position to deliver
     */
    public void pushPosition(GeolocationPosition position) {
        for (BrowserlessGeolocationClient.ActiveWatch w : new ArrayList<>(
                client.watches())) {
            if (w.isActive()) {
                w.push(position);
            }
        }
    }

    /**
     * Convenience overload constructing a position with current-time timestamp
     * and {@code null} altitude/heading/speed fields, then broadcasting it to
     * every active watch.
     *
     * @param latitude
     *            latitude in degrees
     * @param longitude
     *            longitude in degrees
     * @param accuracy
     *            horizontal accuracy in metres
     */
    public void pushPosition(double latitude, double longitude,
            double accuracy) {
        GeolocationCoordinates coords = new GeolocationCoordinates(latitude,
                longitude, accuracy, null, null, null, null);
        pushPosition(
                new GeolocationPosition(coords, System.currentTimeMillis()));
    }

    /**
     * Broadcasts an error to every active watch, exactly as a
     * {@code vaadin-geolocation-error} DOM event would. If no watches are
     * active the call is a no-op.
     *
     * @param code
     *            the error code
     * @param message
     *            the error message
     */
    public void pushError(GeolocationErrorCode code, String message) {
        GeolocationError error = new GeolocationError(code.code(), message);
        for (BrowserlessGeolocationClient.ActiveWatch w : new ArrayList<>(
                client.watches())) {
            if (w.isActive()) {
                w.push(error);
            }
        }
    }

    /**
     * Returns the pending one-shot {@code Geolocation.get(...)} requests in
     * arrival order. Resolved requests are not included.
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
