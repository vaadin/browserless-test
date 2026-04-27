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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuilderLookupServicesTest {

    private static class ServiceA {
    }

    private static class ServiceB {
    }

    private static class ServiceC {
    }

    private BrowserlessApplicationContext.Builder<Void> newBuilder() {
        Routes routes = new Routes(new HashSet<>(), new HashSet<>(),
                new HashSet<>(), true);
        return BrowserlessApplicationContext.builder(routes);
    }

    @Test
    void successiveCallsAccumulate() {
        var builder = newBuilder().withLookupServices(ServiceA.class)
                .withLookupServices(ServiceB.class, ServiceC.class);
        assertEquals(List.of(ServiceA.class, ServiceB.class, ServiceC.class),
                List.copyOf(builder.getLookupServices()));
    }

    @Test
    void noArgsIsNoOp() {
        var builder = newBuilder().withLookupServices(ServiceA.class)
                .withLookupServices();
        assertEquals(Set.of(ServiceA.class), builder.getLookupServices());
    }

    @Test
    void duplicatesAreDeduplicated() {
        var builder = newBuilder().withLookupServices(ServiceA.class)
                .withLookupServices(ServiceA.class, ServiceB.class);
        assertEquals(Set.of(ServiceA.class, ServiceB.class),
                builder.getLookupServices());
    }

    @Test
    void nullArrayThrows() {
        var builder = newBuilder();
        assertThrows(NullPointerException.class,
                () -> builder.withLookupServices((Class<?>[]) null));
    }

    @Test
    void nullElementThrows() {
        var builder = newBuilder();
        assertThrows(NullPointerException.class, () -> builder
                .withLookupServices(ServiceA.class, null, ServiceB.class));
    }
}
