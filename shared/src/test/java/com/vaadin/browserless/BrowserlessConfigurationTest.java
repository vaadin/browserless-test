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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.InitParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserlessConfigurationTest {

    @BrowserlessTestConfig(applicationProperties = { "first=one",
            "second=two=three" }, featureFlags = { "enabledFeature",
                    "disabledFeature=FALSE",
                    "explicitlyEnabled=true" }, lookupServices = {
                            ServiceA.class,
                            ServiceB.class }, javaScriptHandlers = {
                                    HandlerA.class, HandlerB.class })
    private static class AnnotatedClass {
    }

    private static class ServiceA {
    }

    private static class ServiceB {
    }

    private static class ServiceC {
    }

    public static class HandlerA implements JavaScriptHandler {
        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
        }
    }

    public static class HandlerB extends HandlerA {
    }

    private static class NotAnnotatedClass {
    }

    @BrowserlessTestConfig(applicationProperties = "missingSeparator")
    private static class InvalidPropertyClass {
    }

    @BrowserlessTestConfig(featureFlags = "feature=maybe")
    private static class InvalidFeatureFlagClass {
    }

    @Test
    void fromAnnotatedClass_entriesAreParsed() {
        BrowserlessConfiguration configuration = BrowserlessConfiguration
                .from(AnnotatedClass.class);

        assertEquals(Map.of("first", "one", "second", "two=three"),
                configuration.getApplicationProperties(),
                "Property values should be split on the first '=' only");
        assertEquals(
                Map.of("enabledFeature", true, "disabledFeature", false,
                        "explicitlyEnabled", true),
                configuration.getFeatureFlags());
        assertEquals(List.of(ServiceA.class, ServiceB.class),
                List.copyOf(configuration.getLookupServices()),
                "Lookup services should be parsed preserving declaration order");
        assertEquals(List.of(HandlerA.class, HandlerB.class),
                List.copyOf(configuration.getJavaScriptHandlers()),
                "JavaScript handlers should be parsed preserving declaration order");
    }

    @Test
    void merge_lookupServicesAndJavaScriptHandlersAccumulate() {
        BrowserlessConfiguration base = BrowserlessConfiguration.builder()
                .withLookupServices(ServiceA.class, ServiceB.class)
                .withJavaScriptHandlers(HandlerA.class).build();
        BrowserlessConfiguration overrides = BrowserlessConfiguration.builder()
                .withLookupServices(ServiceB.class, ServiceC.class)
                .withJavaScriptHandlers(HandlerB.class, HandlerA.class).build();

        BrowserlessConfiguration merged = base.merge(overrides);
        assertEquals(List.of(ServiceA.class, ServiceB.class, ServiceC.class),
                List.copyOf(merged.getLookupServices()),
                "Lookup services must accumulate rather than being replaced");
        assertEquals(List.of(HandlerA.class, HandlerB.class),
                List.copyOf(merged.getJavaScriptHandlers()),
                "JavaScript handlers must accumulate rather than being replaced");
    }

    @Test
    void lookupServicesAndJavaScriptHandlers_makeConfigurationNotEmpty() {
        assertFalse(BrowserlessConfiguration.builder()
                .withLookupServices(ServiceA.class).build().isEmpty());
        assertFalse(BrowserlessConfiguration.builder()
                .withJavaScriptHandlers(HandlerA.class).build().isEmpty());
        assertNotEquals(
                BrowserlessConfiguration.builder()
                        .withJavaScriptHandlers(HandlerA.class).build(),
                BrowserlessConfiguration.builder()
                        .withJavaScriptHandlers(HandlerB.class).build());
        assertTrue(BrowserlessConfiguration.builder().withLookupServices()
                .build().isEmpty(), "No arguments should be a no-op");
    }

    @Test
    void fromNotAnnotatedClass_isEmpty() {
        assertTrue(BrowserlessConfiguration.from(NotAnnotatedClass.class)
                .isEmpty());
        assertSame(BrowserlessConfiguration.empty(),
                BrowserlessConfiguration.from((BrowserlessTestConfig) null));
    }

    @Test
    void fromAnnotation_invalidEntries_throw() {
        assertThrows(IllegalArgumentException.class,
                () -> BrowserlessConfiguration.from(InvalidPropertyClass.class),
                "Application properties without a separator should be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> BrowserlessConfiguration
                        .from(InvalidFeatureFlagClass.class),
                "Feature flags with a non boolean value should be rejected");
    }

    @Test
    void reservedApplicationProperty_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BrowserlessConfiguration.builder()
                        .withApplicationProperty(InitParameters.BROWSERLESS,
                                "false"));
    }

    @Test
    void builder_featureConstantsAndIdentifiersAreEquivalent() {
        BrowserlessConfiguration fromConstant = BrowserlessConfiguration
                .builder()
                .withFeatureFlags(FeatureFlags.COLLABORATION_ENGINE_BACKEND)
                .build();
        BrowserlessConfiguration fromIdentifier = BrowserlessConfiguration
                .builder()
                .withFeatureFlags(
                        FeatureFlags.COLLABORATION_ENGINE_BACKEND.getId())
                .build();

        assertEquals(fromConstant, fromIdentifier);
    }

    @Test
    void merge_argumentWins() {
        BrowserlessConfiguration base = BrowserlessConfiguration.builder()
                .withApplicationProperty("shared", "base")
                .withApplicationProperty("onlyBase", "base")
                .withFeatureFlag("shared", true)
                .withFeatureFlag("onlyBase", true).build();
        BrowserlessConfiguration overrides = BrowserlessConfiguration.builder()
                .withApplicationProperty("shared", "override")
                .withFeatureFlag("shared", false).build();

        BrowserlessConfiguration merged = base.merge(overrides);

        assertEquals(Map.of("shared", "override", "onlyBase", "base"),
                merged.getApplicationProperties());
        assertEquals(Map.of("shared", false, "onlyBase", true),
                merged.getFeatureFlags());
        assertEquals("base", base.getApplicationProperties().get("shared"),
                "Merging should not modify the original configuration");
    }

    @Test
    void merge_emptyConfigurations_areNoOp() {
        BrowserlessConfiguration configuration = BrowserlessConfiguration
                .builder().withApplicationProperty("key", "value").build();

        assertSame(configuration,
                configuration.merge(BrowserlessConfiguration.empty()));
        assertSame(configuration,
                BrowserlessConfiguration.empty().merge(configuration));
    }

    @Test
    void emptyBuilder_buildsEmptyConfiguration() {
        BrowserlessConfiguration configuration = BrowserlessConfiguration
                .builder().build();
        assertTrue(configuration.isEmpty());
        assertFalse(
                BrowserlessConfiguration.builder()
                        .withFeatureFlag("feature", false).build().isEmpty(),
                "Disabling a feature is still a customization");
    }
}
