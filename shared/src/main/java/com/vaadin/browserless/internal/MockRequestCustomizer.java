/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import com.vaadin.browserless.mocks.MockRequest;

/**
 * Applies last-minute customisations to a freshly created {@link MockRequest}
 * before it is wrapped in a {@code VaadinRequest}. Resolved via the Vaadin
 * {@code Lookup}; used by Spring/Quarkus to inject authentication details.
 */
@FunctionalInterface
public interface MockRequestCustomizer {
    void apply(MockRequest request);
}
