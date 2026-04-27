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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.geolocation.GeolocationAvailability;
import com.vaadin.flow.component.geolocation.GeolocationClient;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationOutcome;
import com.vaadin.flow.component.geolocation.GeolocationResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * In-memory {@link GeolocationClient}. Records every {@code get(...)} and
 * {@code startWatch(...)} call so the test controller can resolve them later.
 * Never touches {@code executeJs} or DOM events.
 */
final class TestGeolocationClient implements GeolocationClient {

    private final Deque<PendingGet> pending = new ArrayDeque<>();
    private final List<ActiveWatch> watches = new ArrayList<>();
    private final List<SerializableConsumer<GeolocationAvailability>> availabilityListeners = new ArrayList<>();
    private GeolocationAvailability availability = GeolocationAvailability.PROMPT;
    private boolean closed;

    @Override
    public CompletableFuture<GeolocationOutcome> get(
            @Nullable GeolocationOptions options) {
        PendingGet entry = new PendingGet(options);
        pending.add(entry);
        return entry.future;
    }

    @Override
    public WatchHandle startWatch(Component owner,
            @Nullable GeolocationOptions options,
            SerializableConsumer<GeolocationResult> onUpdate) {
        ActiveWatch watch = new ActiveWatch(owner, options, onUpdate);
        watches.add(watch);
        return watch;
    }

    @Override
    public Registration subscribeAvailability(
            SerializableConsumer<GeolocationAvailability> onChange) {
        availabilityListeners.add(onChange);
        return () -> availabilityListeners.remove(onChange);
    }

    @Override
    public GeolocationAvailability currentAvailability() {
        return availability;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        availabilityListeners.clear();
        for (ActiveWatch watch : new ArrayList<>(watches)) {
            watch.stop();
        }
        pending.clear();
    }

    // --- Test-controller surface (package-private) ---------------

    void setAvailability(GeolocationAvailability next) {
        if (next == availability) {
            return;
        }
        availability = next;
        for (SerializableConsumer<GeolocationAvailability> listener : new ArrayList<>(
                availabilityListeners)) {
            listener.accept(next);
        }
    }

    Deque<PendingGet> pending() {
        return pending;
    }

    List<ActiveWatch> watches() {
        return Collections.unmodifiableList(watches);
    }

    // --- Internal types -------------------------------------------

    static final class PendingGet {
        final CompletableFuture<GeolocationOutcome> future = new CompletableFuture<>();
        @Nullable
        final GeolocationOptions options;
        boolean resolved;

        PendingGet(@Nullable GeolocationOptions options) {
            this.options = options;
        }

        void respondWith(GeolocationOutcome outcome) {
            if (resolved) {
                throw new IllegalStateException(
                        "Geolocation request already resolved");
            }
            resolved = true;
            future.complete(outcome);
        }
    }

    final class ActiveWatch implements WatchHandle {
        final Component owner;
        @Nullable
        final GeolocationOptions options;
        final SerializableConsumer<GeolocationResult> onUpdate;
        private boolean active = true;

        ActiveWatch(Component owner, @Nullable GeolocationOptions options,
                SerializableConsumer<GeolocationResult> onUpdate) {
            this.owner = owner;
            this.options = options;
            this.onUpdate = onUpdate;
        }

        void push(GeolocationResult result) {
            if (!active) {
                throw new IllegalArgumentException(
                        "Cannot push to a stopped tracker");
            }
            onUpdate.accept(result);
        }

        @Override
        public void stop() {
            if (!active) {
                return;
            }
            active = false;
            watches.remove(this);
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
