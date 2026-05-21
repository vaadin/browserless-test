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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reflection assertions pinning structural invariants of the locator
 * processor's output:
 * <ul>
 * <li>{@code GeneratedLocators} is core-only — no commercial entries leak in,
 * so loading it does not require commercial Vaadin classes on the classpath.
 * <li>{@code GeneratedCommercialLocators} carries the commercial entries.
 * <li>{@code CommercialLocators} unions both via interface inheritance.
 * <li>The end-user-style aggregator emitted by junit6's test-compile
 * ({@code com.example.locator.AppLocators}) is scoped to this module's own
 * {@code @Tests}-annotated testers and does not regenerate shared.jar's.
 * </ul>
 */
class GeneratedAggregatorsTest {

    @Test
    void defaultAggregatorDoesNotContainCommercialEntries() throws Exception {
        Class<?> agg = Class
                .forName("com.vaadin.browserless.locator.GeneratedLocators");
        Set<String> methods = methodNames(agg.getDeclaredMethods());
        assertTrue(methods.contains("findButton"),
                "core aggregator should expose findButton, was: " + methods);
        assertFalse(methods.contains("findChart"),
                "core aggregator must not expose findChart: " + methods);
    }

    @Test
    void commercialAggregatorContainsChartAndNotCoreEntries() throws Exception {
        Class<?> agg = Class.forName(
                "com.vaadin.browserless.locator.GeneratedCommercialLocators");
        Set<String> methods = methodNames(agg.getDeclaredMethods());
        assertTrue(methods.contains("findChart"),
                "commercial aggregator should expose findChart, was: "
                        + methods);
        assertFalse(methods.contains("findButton"),
                "commercial aggregator should not duplicate core entries: "
                        + methods);
    }

    @Test
    void commercialLocatorsMixinUnionsCoreAndCommercial() throws Exception {
        Class<?> mixin = Class
                .forName("com.vaadin.browserless.locator.CommercialLocators");
        // getMethods() walks inherited interfaces; getDeclaredMethods()
        // would only show what's declared on CommercialLocators itself.
        Set<String> methods = methodNames(mixin.getMethods());
        assertTrue(methods.contains("findButton"),
                "CommercialLocators should surface core findButton, was: "
                        + methods);
        assertTrue(methods.contains("findChart"),
                "CommercialLocators should surface commercial findChart, was: "
                        + methods);
    }

    @Test
    void downstreamAggregatorEmittedAtConfiguredFqn() throws Exception {
        // junit6's test-compile wires the processor with
        // -Alocator.entrypoint.fqn=com.example.locator.AppLocators
        // and the processor only scans junit6's own @Tests-annotated test
        // sources, not shared's testers (which are pre-compiled in
        // shared.jar). This pins both behaviours.
        Class<?> downstream = Class.forName("com.example.locator.AppLocators");
        assertTrue(downstream.isInterface(),
                "downstream aggregator should be an interface");

        Set<String> methods = methodNames(downstream.getDeclaredMethods());
        assertTrue(methods.contains("findTestComponent"),
                "downstream aggregator should include local TestComponent, was: "
                        + methods);
        assertTrue(methods.contains("findTestComponentForConcreteTester"),
                "downstream aggregator should include local TestComponentForConcreteTester, was: "
                        + methods);
        assertFalse(methods.contains("findButton"),
                "downstream aggregator should not regenerate framework entries: "
                        + methods);
        assertFalse(methods.contains("findChart"),
                "downstream aggregator should not regenerate framework entries: "
                        + methods);
    }

    private static Set<String> methodNames(Method[] methods) {
        return Arrays.stream(methods).map(Method::getName)
                .collect(Collectors.toSet());
    }
}
