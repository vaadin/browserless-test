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

import com.vaadin.flow.component.button.ButtonLocator;
import com.vaadin.flow.component.grid.GridLocator;
import com.vaadin.flow.component.textfield.TextFieldLocator;

/**
 * Prototype mixin offering typed entry points for the {@code get*} tester API.
 * <p>
 * Mixed into test base classes and context objects so that tests can write
 * {@code getButton().withCaption("Save").click()} without naming a
 * {@code Class.class} token or wrapping a component instance with
 * {@code test(...)}.
 * <p>
 * The prototype exposes a small set of component types (Button, TextField,
 * Grid). The end state would auto-generate an entry method per registered
 * tester via an annotation processor.
 */
public interface Locators {

    /**
     * Hook for context-bound implementations (e.g.
     * {@code BrowserlessUIContext}) to install Vaadin thread-locals before a
     * locator is built. The default is a no-op so plain test base classes work
     * out of the box.
     */
    default void activateLocatorContext() {
    }

    /** Locator for a {@link com.vaadin.flow.component.button.Button}. */
    default ButtonLocator getButton() {
        activateLocatorContext();
        return new ButtonLocator();
    }

    /** Locator for a {@link com.vaadin.flow.component.textfield.TextField}. */
    default TextFieldLocator getTextField() {
        activateLocatorContext();
        return new TextFieldLocator();
    }

    /**
     * Locator for a {@link com.vaadin.flow.component.grid.Grid} carrying items
     * of the given value type.
     *
     * @param valueType
     *            the item type of the grid; serves as a type witness so the
     *            returned locator can expose typed row accessors
     */
    default <V> GridLocator<V> getGrid(Class<V> valueType) {
        activateLocatorContext();
        return new GridLocator<>(valueType);
    }

    /**
     * Generic entry point for user-defined locators. Activates the locator
     * context (so thread-locals and the security snapshot are restored on a
     * window switch) and invokes the supplied factory.
     *
     * <pre>
     * window.get(CheckoutFormLocator::new).withId("checkout").submit();
     * </pre>
     *
     * @param factory
     *            constructor reference (or any supplier) for the user locator
     * @param <L>
     *            the user locator type
     * @return a fresh locator instance
     */
    default <L extends Locator<?, L>> L get(Supplier<L> factory) {
        activateLocatorContext();
        return factory.get();
    }
}
