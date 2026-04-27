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

import java.util.function.BooleanSupplier;

import com.vaadin.browserless.internal.UtilsKt;

/**
 * Detects whether Spring Security is present on the classpath.
 *
 * <p>
 * Used to gate optional Spring Security wiring (security context handler, mock
 * request customizer) so that test applications without Spring Security can
 * still bootstrap the browserless context.
 *
 * <p>
 * For internal use only.
 */
final class SpringSecuritySupport {

    private static final String SECURITY_CONTEXT_HOLDER_CLASS = "org.springframework.security.core.context.SecurityContextHolder";

    private static final BooleanSupplier DEFAULT_DETECTOR = () -> UtilsKt
            .findClass(SECURITY_CONTEXT_HOLDER_CLASS) != null;

    private static volatile BooleanSupplier detector = DEFAULT_DETECTOR;

    private SpringSecuritySupport() {
    }

    static boolean isPresent() {
        return detector.getAsBoolean();
    }

    /**
     * Test seam: override the detector. Pass {@code null} to reset to the
     * default classpath probe.
     */
    static void overrideDetector(BooleanSupplier override) {
        detector = override == null ? DEFAULT_DETECTOR : override;
    }
}
