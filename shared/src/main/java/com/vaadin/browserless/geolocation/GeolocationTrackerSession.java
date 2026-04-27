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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.geolocation.GeolocationOptions;

/**
 * View into a tracker session managed by the test client. Returned by
 * {@link GeolocationTestController#activeTrackers()}.
 */
public interface GeolocationTrackerSession {

    /** The component that owns this tracker session. */
    Component owner();

    /** Options the tracker was started with, or {@code null}. */
    @Nullable
    GeolocationOptions options();

    /** Whether the tracker is currently receiving updates. */
    boolean isActive();
}
