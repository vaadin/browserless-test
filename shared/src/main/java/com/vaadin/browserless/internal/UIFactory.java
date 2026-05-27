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

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Creates a UI instance, typically when a new Vaadin session is set up by
 * MockVaadin.
 *
 * The single abstract method is intentionally named {@code invoke()} to match
 * the bytecode shape of the original Kotlin {@code () -> UI} interface, so
 * existing Java lambdas (e.g. {@code MockedUI::new}, {@code () -> new MockedUI()})
 * and Kotlin {@code uiFactory::invoke} method references continue to work.
 */
@FunctionalInterface
public interface UIFactory extends Serializable {
    UI invoke();
}
