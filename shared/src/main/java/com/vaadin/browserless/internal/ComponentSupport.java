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

import java.util.Collection;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.provider.DataProvider;

/**
 * Teaches the mocked environment about specific component types, so the
 * environment itself only depends on Flow.
 * <p>
 * The component tree walk, the pretty printer, the dialog clean-up and the
 * label, caption and data provider lookups ask every registered implementation
 * before falling back to what Flow alone can tell. An implementation is
 * registered with {@link java.util.ServiceLoader}, in
 * {@code META-INF/services/com.vaadin.browserless.internal.ComponentSupport}.
 * Every method has a default that handles nothing, so an implementation only
 * overrides what its components need.
 * <p>
 * Implementations are consulted in {@link java.util.ServiceLoader} order; for
 * the methods that return a value, the first implementation that handles the
 * component wins.
 * <p>
 * This is an internal extension point and may change without notice.
 */
public interface ComponentSupport {

    /**
     * Returns all children of the component as seen by component lookups,
     * including the ones that are not part of the regular component hierarchy,
     * such as a Grid's header components.
     *
     * @param component
     *            the component, not null.
     * @return the children, or {@code null} if this implementation doesn't
     *         handle the component.
     */
    default List<Component> getAllChildren(Component component) {
        return null;
    }

    /**
     * Checks whether the component is a closed dialog that a browser would have
     * removed from the DOM after closing it. Such components are removed from
     * the UI after every round trip.
     *
     * @param component
     *            the component, not null.
     * @return true if the component should be removed.
     */
    default boolean isClosedDialog(Component component) {
        return false;
    }

    /**
     * Returns the dialog types that {@link LocatorKt#_expectNoDialogs()} checks
     * for.
     *
     * @return the dialog types, not null.
     */
    default Collection<Class<? extends Component>> getDialogTypes() {
        return List.of();
    }

    /**
     * Checks whether the component is an overlay whose client-side part fires a
     * {@code closed} DOM event once it has closed, which the mocked environment
     * then simulates.
     *
     * @param component
     *            the component, not null.
     * @return true if the {@code closed} event should be simulated.
     */
    default boolean firesClosedEvent(Component component) {
        return false;
    }

    /**
     * Adds component-specific details to the one-line description produced by
     * {@link PrettyPrintTreeKt#toPrettyString(Component)}.
     *
     * @param component
     *            the component, not null.
     * @param description
     *            the description parts collected so far, to add to.
     */
    default void describe(Component component, List<String> description) {
    }

    /**
     * Returns the data provider currently set to the component.
     *
     * @param component
     *            the component, not null.
     * @return the data provider, or {@code null} if this implementation doesn't
     *         handle the component or none is set.
     */
    default DataProvider<?, ?> getDataProvider(Component component) {
        return null;
    }

    /**
     * Returns the label of a component that doesn't implement
     * {@link com.vaadin.flow.component.HasLabel}.
     *
     * @param component
     *            the component, not null.
     * @return the label, an empty string if the component has none, or
     *         {@code null} if this implementation doesn't handle the component.
     */
    default String getLabel(Component component) {
        return null;
    }

    /**
     * Sets the label of a component that doesn't implement
     * {@link com.vaadin.flow.component.HasLabel}.
     *
     * @param component
     *            the component, not null.
     * @param label
     *            the label.
     * @return true if this implementation handled the component.
     */
    default boolean setLabel(Component component, String label) {
        return false;
    }

    /**
     * Returns the caption of a component that shows it directly on itself, such
     * as a button's text.
     *
     * @param component
     *            the component, not null.
     * @return the caption, or {@code null} if this implementation doesn't
     *         handle the component.
     */
    default String getCaption(Component component) {
        return null;
    }

    /**
     * Sets the caption of a component that shows it directly on itself.
     *
     * @param component
     *            the component, not null.
     * @param caption
     *            the caption.
     * @return true if this implementation handled the component.
     */
    default boolean setCaption(Component component, String caption) {
        return false;
    }
}
