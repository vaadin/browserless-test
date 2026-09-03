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

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vaadin.experimental.Feature;
import com.vaadin.flow.server.InitParameters;

/**
 * Custom Vaadin configuration to apply to a mock Vaadin environment.
 * <p>
 * Holds Vaadin application properties (also known as init parameters) and
 * feature flag overrides. Both are scoped to the environment they are applied
 * to, so a configuration used by a test neither affects other tests nor
 * requires any clean up.
 * <p>
 * Instances are immutable and can be created with a {@link #builder()} or
 * derived from a {@link BrowserlessTestConfig} annotation.
 *
 * <pre>
 * BrowserlessConfiguration configuration = BrowserlessConfiguration.builder()
 *         .withApplicationProperty(
 *                 InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_SERIALIZE_SESSION,
 *                 "true")
 *         .withFeatureFlags("myExperimentalFeature") //
 *         .build();
 * </pre>
 *
 * @see BrowserlessTestConfig
 * @since 1.2
 */
public final class BrowserlessConfiguration implements Serializable {

    private static final BrowserlessConfiguration EMPTY = new BrowserlessConfiguration(
            Map.of(), Map.of(), Set.of());

    private final Map<String, String> applicationProperties;
    private final Map<String, Boolean> featureFlags;
    private final Set<Class<?>> lookupServices;

    private BrowserlessConfiguration(Map<String, String> applicationProperties,
            Map<String, Boolean> featureFlags, Set<Class<?>> lookupServices) {
        this.applicationProperties = Collections
                .unmodifiableMap(new LinkedHashMap<>(applicationProperties));
        this.featureFlags = Collections
                .unmodifiableMap(new LinkedHashMap<>(featureFlags));
        this.lookupServices = Collections
                .unmodifiableSet(new LinkedHashSet<>(lookupServices));
    }

    /**
     * Gets a configuration that does not customize the Vaadin environment.
     *
     * @return an empty configuration, never {@literal null}
     */
    public static BrowserlessConfiguration empty() {
        return EMPTY;
    }

    /**
     * Creates a new builder for a custom Vaadin configuration.
     *
     * @return a new builder, never {@literal null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a configuration from the {@link BrowserlessTestConfig} annotation
     * potentially present on the given element.
     * <p>
     * Meta annotations are not taken into account; use
     * {@link #from(BrowserlessTestConfig)} with an annotation resolved by other
     * means, such as JUnit {@code AnnotationSupport}, for a more thorough
     * lookup.
     *
     * @param element
     *            the annotated element to inspect, not {@literal null}
     * @return the configuration declared by the element, or an empty
     *         configuration if the annotation is not present, never
     *         {@literal null}
     */
    public static BrowserlessConfiguration from(AnnotatedElement element) {
        Objects.requireNonNull(element, "element must not be null");
        return from(element.getAnnotation(BrowserlessTestConfig.class));
    }

    /**
     * Creates a configuration from the given {@link BrowserlessTestConfig}
     * annotation.
     *
     * @param annotation
     *            the annotation to convert, may be {@literal null}
     * @return the configuration declared by the annotation, or an empty
     *         configuration if the annotation is {@literal null}, never
     *         {@literal null}
     * @throws IllegalArgumentException
     *             if an annotation entry is not well formed
     */
    public static BrowserlessConfiguration from(
            BrowserlessTestConfig annotation) {
        if (annotation == null) {
            return empty();
        }
        Builder builder = builder();
        for (String entry : annotation.applicationProperties()) {
            int separator = indexOfSeparator(entry, "applicationProperties");
            builder.withApplicationProperty(entry.substring(0, separator),
                    entry.substring(separator + 1));
        }
        for (String entry : annotation.featureFlags()) {
            int separator = entry.indexOf('=');
            if (separator < 0) {
                builder.withFeatureFlag(entry, true);
            } else {
                builder.withFeatureFlag(entry.substring(0, separator),
                        parseBoolean(entry.substring(separator + 1), entry));
            }
        }
        builder.withLookupServices(annotation.lookupServices());
        return builder.build();
    }

    private static int indexOfSeparator(String entry, String attribute) {
        int separator = entry == null ? -1 : entry.indexOf('=');
        if (separator < 0) {
            throw new IllegalArgumentException("Invalid " + attribute
                    + " entry '" + entry
                    + "'. Entries must be defined as 'name=value' pairs.");
        }
        return separator;
    }

    private static boolean parseBoolean(String value, String entry) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid featureFlags entry '"
                + entry + "'. Expecting either a feature identifier "
                + "or an 'id=true|false' pair.");
    }

    /**
     * Gets the Vaadin application properties (init parameters) to apply to the
     * mock Vaadin environment.
     *
     * @return an unmodifiable map of application properties, never
     *         {@literal null}
     */
    public Map<String, String> getApplicationProperties() {
        return applicationProperties;
    }

    /**
     * Gets the feature flag overrides to apply to the mock Vaadin environment,
     * as a map of feature identifiers to their enablement state.
     *
     * @return an unmodifiable map of feature flag overrides, never
     *         {@literal null}
     */
    public Map<String, Boolean> getFeatureFlags() {
        return featureFlags;
    }

    /**
     * Gets the service implementation classes to be used to initialize the
     * Vaadin {@link com.vaadin.flow.di.Lookup}.
     *
     * @return an unmodifiable set of service implementation classes, never
     *         {@literal null}
     */
    public Set<Class<?>> getLookupServices() {
        return lookupServices;
    }

    /**
     * Checks if this configuration does not customize the Vaadin environment at
     * all.
     *
     * @return {@literal true} if neither application properties, nor feature
     *         flags, nor lookup services are defined, {@literal false}
     *         otherwise
     */
    public boolean isEmpty() {
        return applicationProperties.isEmpty() && featureFlags.isEmpty()
                && lookupServices.isEmpty();
    }

    /**
     * Creates a new configuration by merging the given configuration into this
     * one. Application properties and feature flags defined by the given
     * configuration win over the ones defined by this configuration, while
     * lookup services of both configurations are accumulated.
     *
     * @param overrides
     *            the configuration to merge into this one, not {@literal null}
     * @return a new merged configuration, never {@literal null}
     */
    public BrowserlessConfiguration merge(BrowserlessConfiguration overrides) {
        Objects.requireNonNull(overrides, "overrides must not be null");
        if (overrides.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return overrides;
        }
        return builder().withConfiguration(this).withConfiguration(overrides)
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BrowserlessConfiguration other)) {
            return false;
        }
        return applicationProperties.equals(other.applicationProperties)
                && featureFlags.equals(other.featureFlags)
                && lookupServices.equals(other.lookupServices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationProperties, featureFlags,
                lookupServices);
    }

    @Override
    public String toString() {
        return "BrowserlessConfiguration{applicationProperties="
                + applicationProperties + ", featureFlags=" + featureFlags
                + ", lookupServices=" + lookupServices + "}";
    }

    /**
     * Builder for {@link BrowserlessConfiguration} instances.
     */
    public static final class Builder {

        private final Map<String, String> applicationProperties = new LinkedHashMap<>();
        private final Map<String, Boolean> featureFlags = new LinkedHashMap<>();
        private final Set<Class<?>> lookupServices = new LinkedHashSet<>();

        private Builder() {
        }

        /**
         * Sets a Vaadin application property (init parameter) to apply to the
         * mock Vaadin environment. A previously set value for the same name is
         * replaced.
         *
         * @param name
         *            the property name, not {@literal null}
         * @param value
         *            the property value, not {@literal null}
         * @return this builder
         * @throws IllegalArgumentException
         *             if the name is blank or reserved by the browserless
         *             environment
         */
        public Builder withApplicationProperty(String name, String value) {
            Objects.requireNonNull(name, "property name must not be null");
            Objects.requireNonNull(value, "property value must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "property name must not be blank");
            }
            if (InitParameters.BROWSERLESS.equals(name)) {
                throw new IllegalArgumentException("The '"
                        + InitParameters.BROWSERLESS
                        + "' application property is enforced by the browserless "
                        + "test environment and cannot be customized.");
            }
            applicationProperties.put(name, value);
            return this;
        }

        /**
         * Sets Vaadin application properties (init parameters) to apply to the
         * mock Vaadin environment. Previously set values for the same names are
         * replaced.
         *
         * @param properties
         *            the properties to set, not {@literal null}
         * @return this builder
         * @throws IllegalArgumentException
         *             if a name is blank or reserved by the browserless
         *             environment
         */
        public Builder withApplicationProperties(
                Map<String, String> properties) {
            Objects.requireNonNull(properties, "properties must not be null");
            properties.forEach(this::withApplicationProperty);
            return this;
        }

        /**
         * Enables the given Vaadin feature flags, overriding the values
         * potentially defined in the {@literal vaadin-featureflags.properties}
         * file or in system properties.
         *
         * @param featureIds
         *            the identifiers of the features to enable, not
         *            {@literal null}
         * @return this builder
         */
        public Builder withFeatureFlags(String... featureIds) {
            Objects.requireNonNull(featureIds, "featureIds must not be null");
            for (String featureId : featureIds) {
                withFeatureFlag(featureId, true);
            }
            return this;
        }

        /**
         * Enables the given Vaadin feature flags, overriding the values
         * potentially defined in the {@literal vaadin-featureflags.properties}
         * file or in system properties.
         *
         * @param features
         *            the features to enable, not {@literal null}
         * @return this builder
         */
        public Builder withFeatureFlags(Feature... features) {
            Objects.requireNonNull(features, "features must not be null");
            for (Feature feature : features) {
                Objects.requireNonNull(feature, "feature must not be null");
                withFeatureFlag(feature.getId(), true);
            }
            return this;
        }

        /**
         * Enables or disables the given Vaadin feature flag, overriding the
         * value potentially defined in the
         * {@literal vaadin-featureflags.properties} file or in system
         * properties.
         *
         * @param featureId
         *            the identifier of the feature, not {@literal null}
         * @param enabled
         *            {@literal true} to enable the feature, {@literal false} to
         *            disable it
         * @return this builder
         * @throws IllegalArgumentException
         *             if the feature identifier is blank
         */
        public Builder withFeatureFlag(String featureId, boolean enabled) {
            Objects.requireNonNull(featureId, "featureId must not be null");
            if (featureId.isBlank()) {
                throw new IllegalArgumentException(
                        "feature identifier must not be blank");
            }
            featureFlags.put(featureId, enabled);
            return this;
        }

        /**
         * Enables or disables the given Vaadin feature flag, overriding the
         * value potentially defined in the
         * {@literal vaadin-featureflags.properties} file or in system
         * properties.
         *
         * @param feature
         *            the feature, not {@literal null}
         * @param enabled
         *            {@literal true} to enable the feature, {@literal false} to
         *            disable it
         * @return this builder
         */
        public Builder withFeatureFlag(Feature feature, boolean enabled) {
            Objects.requireNonNull(feature, "feature must not be null");
            return withFeatureFlag(feature.getId(), enabled);
        }

        /**
         * Adds the given service implementation classes to the ones used to
         * initialize the Vaadin {@link com.vaadin.flow.di.Lookup}, such as
         * {@link com.vaadin.flow.di.InstantiatorFactory} or
         * {@link com.vaadin.flow.di.ResourceProvider} implementations.
         * <p>
         * Successive calls accumulate; lookup services are never replaced.
         * Calling with no arguments is a no-op.
         *
         * @param services
         *            the service implementation classes to add, not
         *            {@literal null}
         * @return this builder
         */
        public Builder withLookupServices(Class<?>... services) {
            Objects.requireNonNull(services, "services must not be null");
            for (Class<?> service : services) {
                lookupServices.add(Objects.requireNonNull(service,
                        "service must not be null"));
            }
            return this;
        }

        /**
         * Applies all entries of the given configuration to this builder,
         * replacing previously set application properties and feature flags
         * with the same names, and accumulating lookup services.
         *
         * @param configuration
         *            the configuration to apply, not {@literal null}
         * @return this builder
         */
        public Builder withConfiguration(
                BrowserlessConfiguration configuration) {
            Objects.requireNonNull(configuration,
                    "configuration must not be null");
            applicationProperties
                    .putAll(configuration.getApplicationProperties());
            featureFlags.putAll(configuration.getFeatureFlags());
            lookupServices.addAll(configuration.getLookupServices());
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return a new configuration, never {@literal null}
         */
        public BrowserlessConfiguration build() {
            if (applicationProperties.isEmpty() && featureFlags.isEmpty()
                    && lookupServices.isEmpty()) {
                return empty();
            }
            return new BrowserlessConfiguration(applicationProperties,
                    featureFlags, lookupServices);
        }
    }
}
