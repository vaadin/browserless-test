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

import java.util.function.Supplier;

/**
 * Mixin offering typed entry points for the {@code find*} tester API.
 * <p>
 * Most entry points come from {@link GeneratedLocators}, which is emitted by
 * the locator annotation processor at build time. This interface adds the
 * generic {@link #find(Supplier)} for user-defined locators and the
 * {@link #activateLocatorContext()} hook that context-bound implementations
 * (e.g. {@code BrowserlessUIContext}) override.
 *
 * @since 1.1
 */
public interface Locators extends GeneratedLocators {

    /**
     * Hook for context-bound implementations to install Vaadin thread-locals
     * before a locator is built. Default is a no-op.
     */
    @Override
    default void activateLocatorContext() {
    }

    /**
     * Generic entry point for user-defined locators. Activates the locator
     * context (so thread-locals and the security snapshot are restored on a
     * window switch) and invokes the supplied factory.
     *
     * <pre>
     * window.find(CheckoutFormLocator::new).withId("checkout").submit();
     * </pre>
     *
     * @param factory
     *            supplier that creates a fresh locator instance, typically a
     *            constructor reference
     * @param <L>
     *            the locator type created by the factory
     * @return the locator created by the factory, ready for chaining
     * @throws IllegalStateException
     *             if the factory returns {@code null} instead of a fresh
     *             locator instance.
     */
    default <L extends Locator<?, L>> L find(Supplier<L> factory) {
        activateLocatorContext();
        L locator = factory.get();
        if (locator == null) {
            throw new IllegalStateException(
                    "Locators.find factory must return a non-null Locator.");
        }
        return locator;
    }
}
