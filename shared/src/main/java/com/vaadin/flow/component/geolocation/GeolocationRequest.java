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

import org.jspecify.annotations.Nullable;

/**
 * A pending one-shot {@code Geolocation.get(...)} request observed by the test
 * controller. Tests can inspect the {@link #options()} the facade was called
 * with, and resolve the request manually via
 * {@link #respondWith(GeolocationPosition)} or
 * {@link #respondWith(GeolocationError)}.
 */
public interface GeolocationRequest {

    /** The options the application passed to get(...), or {@code null}. */
    @Nullable
    GeolocationOptions options();

    /** Whether this request still awaits a response. */
    boolean isPending();

    /**
     * Resolves the request with the given position. Throws
     * {@link IllegalStateException} if the request was already resolved.
     */
    void respondWith(GeolocationPosition position);

    /**
     * Resolves the request with the given error. Throws
     * {@link IllegalStateException} if the request was already resolved.
     */
    void respondWith(GeolocationError error);
}
