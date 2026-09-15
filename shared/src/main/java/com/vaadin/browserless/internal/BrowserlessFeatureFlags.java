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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.browserless.BrowserlessTestSetupException;
import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

/**
 * {@link FeatureFlags} implementation that applies test defined feature flag
 * overrides on top of the ones resolved by Vaadin.
 * <p>
 * The instance is installed into the {@link VaadinContext} of the mock
 * environment before the Vaadin servlet is initialized, so that everything
 * reading feature flags during startup, for example a
 * {@link com.vaadin.flow.server.VaadinServiceInitListener}, already observes
 * the test configuration.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.2
 */
public class BrowserlessFeatureFlags extends FeatureFlags {

    /*
     * NOTE: assigned only after the super constructor completes, so it is null
     * while the super constructor invokes loadProperties().
     */
    private Map<String, Boolean> overrides;

    private BrowserlessFeatureFlags(Lookup lookup,
            Map<String, Boolean> overrides) {
        super(lookup);
        this.overrides = new LinkedHashMap<>(overrides);
        applyOverrides();
    }

    /**
     * Installs a {@link FeatureFlags} instance applying the given overrides
     * into the given Vaadin context.
     * <p>
     * Must be invoked before anything requests the feature flags for the
     * context, otherwise Vaadin creates and caches its own instance.
     *
     * @param context
     *            the Vaadin context of the mock environment, not
     *            {@literal null}
     * @param overrides
     *            feature identifiers mapped to their enablement state, not
     *            {@literal null}
     * @throws BrowserlessTestSetupException
     *             if a feature identifier is not known by Vaadin
     */
    public static void install(VaadinContext context,
            Map<String, Boolean> overrides) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(overrides, "overrides must not be null");
        BrowserlessFeatureFlags featureFlags = new BrowserlessFeatureFlags(
                context.getAttribute(Lookup.class), overrides);
        context.setAttribute(FeatureFlagsWrapper.class,
                new FeatureFlagsWrapper(featureFlags));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Re-applies the test defined overrides, so that they are not silently
     * discarded by code reloading the feature flags, for example through
     * {@link #setPropertiesLocation(java.io.File)}.
     */
    @Override
    public void loadProperties() {
        super.loadProperties();
        applyOverrides();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Toggles the feature for the current test only. Unlike the default
     * implementation, it neither requires development mode nor stores the new
     * state into the {@literal vaadin-featureflags.properties} file of the
     * project.
     */
    @Override
    public void setEnabled(String featureId, boolean enabled) {
        feature(featureId).setEnabled(enabled);
    }

    private void applyOverrides() {
        if (overrides == null) {
            // invoked by the super constructor, before overrides are known
            return;
        }
        overrides.forEach(
                (featureId, enabled) -> feature(featureId).setEnabled(enabled));
    }

    private Feature feature(String featureId) {
        return getFeatures().stream()
                .filter(feature -> feature.getId().equals(featureId))
                .findFirst()
                .orElseThrow(() -> new BrowserlessTestSetupException(
                        "Unknown Vaadin feature flag '" + featureId
                                + "'. Available feature flags: "
                                + getFeatures().stream().map(Feature::getId)
                                        .sorted()
                                        .collect(Collectors.joining(", "))
                                + ". Feature flags are contributed by the Vaadin "
                                + "modules on the classpath, so a missing one may "
                                + "also indicate a missing dependency."));
    }
}
