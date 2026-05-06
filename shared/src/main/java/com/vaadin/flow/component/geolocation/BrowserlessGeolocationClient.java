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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * In-memory {@link GeolocationClient}. The simulator drives this client through
 * two orthogonal axes — permission state and sensor reading — and the client
 * resolves pending {@code get(...)} calls and emits to active watches based on
 * the combination of those axes.
 */
final class BrowserlessGeolocationClient implements GeolocationClient {

    private final Deque<PendingGet> pending = new ArrayDeque<>();
    private final List<ActiveWatch> watches = new ArrayList<>();
    private final List<SerializableConsumer<GeolocationAvailability>> availabilityListeners = new ArrayList<>();

    private GeolocationAvailability availability = GeolocationAvailability.PROMPT;
    private @Nullable GeolocationPosition cachedFix;
    private @Nullable GeolocationError cachedError;
    private boolean closed;

    @Override
    public CompletableFuture<GeolocationOutcome> get(
            @Nullable GeolocationOptions options) {
        PendingGet entry = new PendingGet(options);
        pending.add(entry);
        tryResolve(entry);
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

    void setAvailability(GeolocationAvailability next) {
        if (next == availability) {
            return;
        }
        availability = next;
        for (SerializableConsumer<GeolocationAvailability> listener : new ArrayList<>(
                availabilityListeners)) {
            listener.accept(next);
        }
        flushPending();
    }

    void setCachedFix(@Nullable GeolocationPosition position) {
        this.cachedFix = position;
        if (position != null) {
            this.cachedError = null;
        }
        if (availability == GeolocationAvailability.GRANTED
                && position != null) {
            for (ActiveWatch watch : new ArrayList<>(watches)) {
                if (watch.isActive()) {
                    watch.push(position);
                }
            }
            flushPending();
        }
    }

    void setCachedError(@Nullable GeolocationError error) {
        this.cachedError = error;
        if (error != null) {
            this.cachedFix = null;
        }
        if (availability == GeolocationAvailability.GRANTED && error != null) {
            for (ActiveWatch watch : new ArrayList<>(watches)) {
                if (watch.isActive()) {
                    watch.push(error);
                }
            }
            flushPending();
        }
    }

    void deliverDeniedToWatches() {
        GeolocationError error = new GeolocationError(
                GeolocationErrorCode.PERMISSION_DENIED.code(),
                "Permission denied");
        for (ActiveWatch watch : new ArrayList<>(watches)) {
            if (watch.isActive()) {
                watch.push(error);
                watch.stop();
            }
        }
    }

    Deque<PendingGet> pending() {
        return pending;
    }

    List<ActiveWatch> watches() {
        return Collections.unmodifiableList(watches);
    }

    private void tryResolve(PendingGet entry) {
        attemptResolve(entry);
        if (entry.resolved) {
            pending.remove(entry);
        }
    }

    private void flushPending() {
        for (Iterator<PendingGet> it = pending.iterator(); it.hasNext();) {
            PendingGet entry = it.next();
            attemptResolve(entry);
            if (entry.resolved) {
                it.remove();
            }
        }
    }

    private void attemptResolve(PendingGet entry) {
        switch (availability) {
        case DENIED -> entry.respondWith(new GeolocationError(
                GeolocationErrorCode.PERMISSION_DENIED.code(),
                "Permission denied"));
        case UNSUPPORTED -> entry.respondWith(new GeolocationError(
                GeolocationErrorCode.POSITION_UNAVAILABLE.code(),
                "Geolocation is not supported"));
        case GRANTED -> {
            if (cachedFix != null) {
                entry.respondWith(cachedFix);
            } else if (cachedError != null) {
                entry.respondWith(cachedError);
            }
        }
        case PROMPT, UNKNOWN -> {
            // wait for permission to be decided
        }
        }
    }

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
