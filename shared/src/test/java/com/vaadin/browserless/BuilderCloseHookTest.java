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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;

/**
 * Contract tests for
 * {@link BrowserlessApplicationContext.Builder#withCloseHook} and the
 * hook-invocation behaviour of {@link BrowserlessApplicationContext#close()}.
 */
class BuilderCloseHookTest {

    private static Routes emptyRoutes() {
        return new Routes(new HashSet<>(), new HashSet<>(), new HashSet<>(),
                true);
    }

    @Test
    void hooksRunInRegistrationOrderOnClose() {
        List<String> log = new ArrayList<>();
        var app = new BrowserlessApplicationContext.Builder(emptyRoutes())
                .withCloseHook(() -> log.add("a"))
                .withCloseHook(() -> log.add("b"))
                .withCloseHook(() -> log.add("c")).build();

        app.close();

        Assertions.assertEquals(List.of("a", "b", "c"), log,
                "hooks must run in registration order");
    }

    @Test
    void hooksFireExactlyOnceAcrossRedundantCloseCalls() {
        AtomicInteger calls = new AtomicInteger();
        var app = new BrowserlessApplicationContext.Builder(emptyRoutes())
                .withCloseHook(calls::incrementAndGet).build();

        app.close();
        app.close();
        app.close();

        Assertions.assertEquals(1, calls.get(),
                "redundant close() calls must not re-run hooks");
    }

    @Test
    void allHooksRunEvenIfSomeThrow() {
        List<String> log = new ArrayList<>();
        var app = new BrowserlessApplicationContext.Builder(emptyRoutes())
                .withCloseHook(() -> log.add("a")).withCloseHook(() -> {
                    log.add("b-throws");
                    throw new IllegalStateException("boom-b");
                }).withCloseHook(() -> log.add("c")).withCloseHook(() -> {
                    log.add("d-throws");
                    throw new IllegalArgumentException("boom-d");
                }).build();

        var ex = Assertions.assertThrows(RuntimeException.class, app::close);

        Assertions.assertEquals(List.of("a", "b-throws", "c", "d-throws"), log,
                "a throwing hook must not prevent subsequent hooks from"
                        + " running");
        Throwable[] suppressed = ex.getSuppressed();
        Assertions.assertEquals(2, suppressed.length,
                "aggregate exception must collect all hook failures as"
                        + " suppressed");
        Assertions.assertInstanceOf(IllegalStateException.class, suppressed[0]);
        Assertions.assertEquals("boom-b", suppressed[0].getMessage());
        Assertions.assertInstanceOf(IllegalArgumentException.class,
                suppressed[1]);
        Assertions.assertEquals("boom-d", suppressed[1].getMessage());
    }

    @Test
    void singleThrowingHookSurfacesAsAggregateWithOneSuppressed() {
        var cause = new IllegalStateException("solo-boom");
        var app = new BrowserlessApplicationContext.Builder(emptyRoutes())
                .withCloseHook(() -> {
                    throw cause;
                }).build();

        var ex = Assertions.assertThrows(RuntimeException.class, app::close);

        Assertions.assertArrayEquals(new Throwable[] { cause },
                ex.getSuppressed(),
                "single hook failure must still surface via suppressed");
    }

    @Test
    void closeWithoutHooksDoesNotThrow() {
        try (var app = new BrowserlessApplicationContext.Builder(emptyRoutes())
                .build()) {
            // try-with-resources triggers close(); just asserting no throw
        }
    }

    @Test
    void withCloseHook_nullThrows() {
        var builder = new BrowserlessApplicationContext.Builder(emptyRoutes());
        Assertions.assertThrows(NullPointerException.class,
                () -> builder.withCloseHook(null));
    }
}
