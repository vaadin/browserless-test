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
package com.vaadin.browserless.internal;

import java.util.EventObject;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;

/**
 * Helpers for firing Vaadin service events in the mocked environment.
 */
public final class MockVaadinKt {

    /**
     * Hands a listener failure back to the caller instead of logging it, which
     * is what {@link VaadinServiceEventBus#fireEvent} does by default. A
     * listener that throws during a test should fail that test rather than only
     * leave a line in the log.
     */
    static final SerializableBiConsumer<EventObject, Exception> RETHROW_LISTENER_FAILURE = (
            event, error) -> {
        throw UtilsKt.sneakyThrow(error);
    };

    private MockVaadinKt() {
    }

    public static void fireSessionInitListeners(VaadinService service,
            SessionInitEvent event) {
        service.getEventBus().fireEvent(event, RETHROW_LISTENER_FAILURE);
    }

    public static void fireServiceDestroyListeners(VaadinService service,
            ServiceDestroyEvent event) {
        service.getEventBus().fireEvent(event, RETHROW_LISTENER_FAILURE);
    }
}
