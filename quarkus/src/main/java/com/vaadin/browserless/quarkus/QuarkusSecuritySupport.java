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
package com.vaadin.browserless.quarkus;

import jakarta.enterprise.inject.spi.CDI;

import java.util.function.BooleanSupplier;

import com.vaadin.browserless.internal.UtilsKt;

/**
 * Detects whether Quarkus Security is active in the running application.
 *
 * <p>
 * Used to gate optional Quarkus Security wiring (security context handler, mock
 * request customizer) so that test applications without an active
 * {@code quarkus-security} extension can still bootstrap the browserless
 * context.
 *
 * <p>
 * The classpath probe alone is not enough: extensions like
 * {@code quarkus-undertow} bring {@code quarkus-security} in transitively, so
 * the {@code CurrentIdentityAssociation} class can be present even when no bean
 * is registered with Arc. We therefore also ask CDI whether the bean is
 * resolvable.
 *
 * <p>
 * For internal use only.
 */
final class QuarkusSecuritySupport {

    private static final String CURRENT_IDENTITY_ASSOCIATION_CLASS = "io.quarkus.security.identity.CurrentIdentityAssociation";

    private static final BooleanSupplier DEFAULT_DETECTOR = () -> {
        Class<?> beanClass = UtilsKt
                .findClass(CURRENT_IDENTITY_ASSOCIATION_CLASS);
        if (beanClass == null) {
            return false;
        }
        try {
            return CDI.current().select(beanClass).isResolvable();
        } catch (RuntimeException ex) {
            // No active CDI container, or CDI failed during resolution
            return false;
        }
    };

    private static volatile BooleanSupplier detector = DEFAULT_DETECTOR;

    private QuarkusSecuritySupport() {
    }

    static boolean isPresent() {
        return detector.getAsBoolean();
    }

    /**
     * Test seam: override the detector. Pass {@code null} to reset to the
     * default probe.
     */
    static void overrideDetector(BooleanSupplier override) {
        detector = override == null ? DEFAULT_DETECTOR : override;
    }
}
