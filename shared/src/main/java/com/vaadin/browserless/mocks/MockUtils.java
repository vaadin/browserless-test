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
package com.vaadin.browserless.mocks;

import java.util.Map;

/**
 * Helpers shared by the mocks in this package.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class MockUtils {

    private MockUtils() {
    }

    /**
     * Stores the value under the key, or removes the key when the value is
     * {@code null}.
     * <p>
     * This is what the servlet API asks for: setting an attribute to
     * {@code null} removes it. It also keeps the mocks usable with a map that
     * rejects null values, such as
     * {@link java.util.concurrent.ConcurrentHashMap}.
     *
     * @param map
     *            the map to modify
     * @param key
     *            the key to store under or remove
     * @param value
     *            the value to store, or {@code null} to remove the key
     * @param <K>
     *            the key type
     * @param <V>
     *            the value type
     */
    public static <K, V> void putOrRemove(Map<K, V> map, K key, V value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }
}
