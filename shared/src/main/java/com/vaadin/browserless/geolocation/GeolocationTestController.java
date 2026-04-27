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
package com.vaadin.browserless.geolocation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationAvailability;
import com.vaadin.flow.component.geolocation.GeolocationClient;
import com.vaadin.flow.component.geolocation.GeolocationCoordinates;
import com.vaadin.flow.component.geolocation.GeolocationError;
import com.vaadin.flow.component.geolocation.GeolocationErrorCode;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.geolocation.GeolocationTracker;

/**
 * Browserless test driver for the {@link Geolocation} facade. Replaces the UI's
 * geolocation client with an in-memory implementation; tests then drive
 * outcomes (positions, errors, permission state) by calling methods on the
 * controller.
 * <p>
 * Obtain via {@link #install(UI)}; the call must precede any
 * {@code Geolocation.get(...)} or {@code Geolocation.track(...)} on the UI.
 */
public final class GeolocationTestController implements Serializable {

    private final TestGeolocationClient client;

    private GeolocationTestController(TestGeolocationClient client) {
        this.client = client;
    }

    /**
     * Replaces the given UI's geolocation client with an in-memory test client.
     * Idempotent: a second call on the same UI returns the controller already
     * installed.
     *
     * @param ui
     *            the UI to install on
     * @return the controller bound to the in-memory test client
     */
    public static GeolocationTestController install(UI ui) {
        GeolocationTestController existing = ComponentUtil.getData(ui,
                GeolocationTestController.class);
        if (existing != null) {
            return existing;
        }
        TestGeolocationClient client = new TestGeolocationClient();
        ui.getGeolocation().setClient(client);
        GeolocationTestController controller = new GeolocationTestController(
                client);
        ComponentUtil.setData(ui, GeolocationTestController.class, controller);
        return controller;
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
    public void simulatePosition(GeolocationPosition position) {
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
     */
    public void simulatePosition(double latitude, double longitude,
            double accuracy) {
        GeolocationCoordinates coords = new GeolocationCoordinates(latitude,
                longitude, accuracy, null, null, null, null);
        simulatePosition(
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
    public void simulateError(GeolocationErrorCode code, String message) {
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
    public void simulateError(GeolocationErrorCode code) {
        simulateError(code, code.name());
    }

    private TestGeolocationClient.PendingGet nextPending() {
        TestGeolocationClient.PendingGet entry = client.pending().pollFirst();
        if (entry == null) {
            throw new IllegalStateException(
                    "No pending Geolocation.get() request to resolve");
        }
        return entry;
    }

    /**
     * Pushes a position update to the given tracker, exactly as a
     * {@code vaadin-geolocation-position} DOM event would.
     *
     * @param tracker
     *            the tracker to push to
     * @param position
     *            the position to deliver
     * @throws IllegalArgumentException
     *             if the tracker has no active watch (already stopped or
     *             detached) or was not started under the test client
     */
    public void pushPosition(GeolocationTracker tracker,
            GeolocationPosition position) {
        activeWatchFor(tracker).push(position);
    }

    /**
     * Convenience overload constructing a position with current-time timestamp
     * and {@code null} altitude/heading/speed fields.
     *
     * @param tracker
     *            the tracker to push to
     * @param latitude
     *            latitude in degrees
     * @param longitude
     *            longitude in degrees
     * @param accuracy
     *            horizontal accuracy in metres
     */
    public void pushPosition(GeolocationTracker tracker, double latitude,
            double longitude, double accuracy) {
        GeolocationCoordinates coords = new GeolocationCoordinates(latitude,
                longitude, accuracy, null, null, null, null);
        pushPosition(tracker,
                new GeolocationPosition(coords, System.currentTimeMillis()));
    }

    /**
     * Pushes an error to the given tracker, exactly as a
     * {@code vaadin-geolocation-error} DOM event would.
     *
     * @param tracker
     *            the tracker to push to
     * @param code
     *            the error code
     * @param message
     *            the error message
     * @throws IllegalArgumentException
     *             if the tracker has no active watch
     */
    public void pushError(GeolocationTracker tracker, GeolocationErrorCode code,
            String message) {
        activeWatchFor(tracker)
                .push(new GeolocationError(code.code(), message));
    }

    private TestGeolocationClient.ActiveWatch activeWatchFor(
            GeolocationTracker tracker) {
        GeolocationClient.WatchHandle handle = tracker.handle();
        if (handle == null || !handle.isActive()) {
            throw new IllegalArgumentException(
                    "Tracker has no active watch (already stopped or detached)");
        }
        if (!(handle instanceof TestGeolocationClient.ActiveWatch watch)) {
            throw new IllegalArgumentException(
                    "Tracker was not started under the test client");
        }
        return watch;
    }

    /**
     * Returns the pending one-shot {@code Geolocation.get(...)} requests in
     * arrival order. Resolved requests are not included.
     *
     * @return an unmodifiable view of pending requests
     */
    public List<GeolocationRequest> requests() {
        List<GeolocationRequest> out = new ArrayList<>();
        for (TestGeolocationClient.PendingGet p : client.pending()) {
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
        TestGeolocationClient.PendingGet last = client.pending().peekLast();
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
        for (TestGeolocationClient.ActiveWatch w : client.watches()) {
            if (w.isActive()) {
                out.add(new TrackerSessionAdapter(w));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static final class RequestAdapter implements GeolocationRequest {
        private final TestGeolocationClient.PendingGet entry;
        private final TestGeolocationClient client;

        RequestAdapter(TestGeolocationClient.PendingGet entry,
                TestGeolocationClient client) {
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
        private final TestGeolocationClient.ActiveWatch watch;

        TrackerSessionAdapter(TestGeolocationClient.ActiveWatch watch) {
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
