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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;

/**
 * {@link GeolocationClientFactory} that produces an in-memory test client and
 * publishes a {@link GeolocationSimulator} on the UI for tests to drive.
 * Registered through {@code META-INF/services}; Flow's
 * {@link com.vaadin.flow.di.Lookup Lookup} resolves it automatically when
 * browserless-test-shared is on the classpath.
 */
public final class BrowserlessGeolocationClientFactory
        implements GeolocationClientFactory {

    @Override
    public GeolocationClient create(UI ui) {
        BrowserlessGeolocationClient client = new BrowserlessGeolocationClient();
        ComponentUtil.setData(ui, GeolocationSimulator.class,
                new GeolocationSimulator(client));
        return client;
    }
}
