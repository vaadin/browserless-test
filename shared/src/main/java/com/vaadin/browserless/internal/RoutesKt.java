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

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Utilities for the route registry.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class RoutesKt {

    private RoutesKt() {
    }

    /**
     * Clears the PWA class config from the given registry.
     *
     * @param registry
     *            the registry.
     */
    @SuppressWarnings("unchecked")
    public static void clearPwaClass(ApplicationRouteRegistry registry) {
        AtomicReference<Class<?>> ref;
        try {
            Field pwaClassField = ApplicationRouteRegistry.class
                    .getDeclaredField("pwaConfigurationClass");
            pwaClassField.setAccessible(true);
            ref = (AtomicReference<Class<?>>) pwaClassField.get(registry);
        } catch (ReflectiveOperationException e) {
            throw UtilsKt.sneakyThrow(e);
        }
        ref.set(null);
        if (registry.getPwaConfigurationClass() != null) {
            throw new AssertionError(
                    "PWA configuration class should have been removed");
        }
    }

}
