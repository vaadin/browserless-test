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
package com.vaadin.browserless;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.browserless.internal.Routes;

/**
 * Process-wide cache of {@link Routes} discovered by classpath scan.
 * <p>
 * Each requested package is scanned at most once per JVM; subsequent calls
 * return the cached {@link Routes} for that package. Callers requesting
 * multiple packages get a merged {@link Routes} composed from the cached
 * per-package entries.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class RouteDiscovery {

    private static final ConcurrentHashMap<String, Routes> CACHE = new ConcurrentHashMap<>();

    private RouteDiscovery() {
    }

    /**
     * Discovers the routes for the given packages, reusing previously cached
     * results when available. A {@code null} or empty package set falls back to
     * a full classpath scan (the empty-string package marker used by
     * {@link Routes#autoDiscoverViews(String...)}).
     *
     * @param packageNames
     *            package names to scan; may be {@code null} or empty
     * @return the merged {@link Routes}
     */
    static synchronized Routes discover(Set<String> packageNames) {
        Set<String> effective = packageNames == null || packageNames.isEmpty()
                ? Set.of("")
                : packageNames;
        return effective.stream()
                .map(pkg -> CACHE.computeIfAbsent(pkg,
                        p -> new Routes().autoDiscoverViews(p)))
                .reduce(new Routes(), Routes::merge);
    }
}
