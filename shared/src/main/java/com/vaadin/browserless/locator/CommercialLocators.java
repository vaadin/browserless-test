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
package com.vaadin.browserless.locator;

/**
 * Mixin offering typed locator entry points for commercial Vaadin components
 * (Charts, etc.).
 * <p>
 * Kept separate from {@link Locators} so that core-only consumers do not pull
 * commercial classes onto the compilation classpath — mirroring the existing
 * {@code TesterWrappers} / {@code CommercialTesterWrappers} split.
 * <p>
 * Most entries come from {@link GeneratedCommercialLocators}, which is emitted
 * by the locator annotation processor. Mix this into your own context subclass
 * or test class when you depend on commercial Vaadin components.
 */
public interface CommercialLocators
        extends Locators, GeneratedCommercialLocators {

    // Locators provides a default no-op; GeneratedCommercialLocators
    // re-declares the method as abstract. Java requires an explicit override
    // to resolve the conflict; we forward to the Locators default.
    @Override
    default void activateLocatorContext() {
        Locators.super.activateLocatorContext();
    }
}
