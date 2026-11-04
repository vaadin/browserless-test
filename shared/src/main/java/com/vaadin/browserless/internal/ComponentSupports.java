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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.data.provider.DataProvider;

/**
 * The registered {@link ComponentSupport} implementations, and the lookups
 * built on them that the mocked environment uses instead of referring to
 * component classes.
 * <p>
 * This is internal API and may change without notice.
 */
public final class ComponentSupports {

    private static final List<ComponentSupport> SUPPORTS = load();

    private ComponentSupports() {
    }

    private static List<ComponentSupport> load() {
        List<ComponentSupport> supports = new ArrayList<>();
        ServiceLoader
                .load(ComponentSupport.class,
                        ComponentSupport.class.getClassLoader())
                .forEach(supports::add);
        return List.copyOf(supports);
    }

    /**
     * Returns the registered implementations, in {@link ServiceLoader} order.
     *
     * @return the implementations, not null.
     */
    public static List<ComponentSupport> getAll() {
        return SUPPORTS;
    }

    /**
     * Returns the component's children as reported by the first implementation
     * that handles it.
     *
     * @param component
     *            the component, not null.
     * @return the children, or {@code null} if no implementation handles the
     *         component.
     * @see ComponentSupport#getAllChildren(Component)
     */
    public static List<Component> getAllChildren(Component component) {
        for (ComponentSupport support : SUPPORTS) {
            List<Component> children = support.getAllChildren(component);
            if (children != null) {
                return children;
            }
        }
        return null;
    }

    /**
     * Checks whether the component is a closed dialog to remove from the UI.
     *
     * @param component
     *            the component, not null.
     * @return true if any implementation says so.
     * @see ComponentSupport#isClosedDialog(Component)
     */
    public static boolean isClosedDialog(Component component) {
        return SUPPORTS.stream()
                .anyMatch(support -> support.isClosedDialog(component));
    }

    /**
     * Returns the dialog types of all implementations.
     *
     * @return the dialog types, not null.
     * @see ComponentSupport#getDialogTypes()
     */
    public static List<Class<? extends Component>> getDialogTypes() {
        List<Class<? extends Component>> types = new ArrayList<>();
        for (ComponentSupport support : SUPPORTS) {
            Collection<Class<? extends Component>> supportTypes = support
                    .getDialogTypes();
            types.addAll(supportTypes);
        }
        return types;
    }

    /**
     * Checks whether the component fires a {@code closed} DOM event once it has
     * closed.
     *
     * @param component
     *            the component, not null.
     * @return true if any implementation says so.
     * @see ComponentSupport#firesClosedEvent(Component)
     */
    public static boolean firesClosedEvent(Component component) {
        return SUPPORTS.stream()
                .anyMatch(support -> support.firesClosedEvent(component));
    }

    /**
     * Lets every implementation add details to the component's description.
     *
     * @param component
     *            the component, not null.
     * @param description
     *            the description parts collected so far, to add to.
     * @see ComponentSupport#describe(Component, List)
     */
    public static void describe(Component component, List<String> description) {
        SUPPORTS.forEach(support -> support.describe(component, description));
    }

    /**
     * Returns the data provider currently set to the component.
     *
     * @param component
     *            the component, not null.
     * @return the data provider, or null.
     * @see ComponentSupport#getDataProvider(Component)
     */
    public static DataProvider<?, ?> getDataProvider(Component component) {
        for (ComponentSupport support : SUPPORTS) {
            DataProvider<?, ?> dataProvider = support
                    .getDataProvider(component);
            if (dataProvider != null) {
                return dataProvider;
            }
        }
        return null;
    }

    /**
     * Determines the component's label: {@link HasLabel#getLabel()}, then what
     * an implementation reports, then the element's {@code label} property.
     *
     * @param component
     *            the component, not null.
     * @return the label, never null.
     */
    public static String getLabel(Component component) {
        String label = null;
        if (component instanceof HasLabel hasLabel) {
            label = hasLabel.getLabel();
        } else {
            for (ComponentSupport support : SUPPORTS) {
                String supportLabel = support.getLabel(component);
                if (supportLabel != null) {
                    return supportLabel;
                }
            }
            label = component.getElement().getProperty("label");
        }
        return label != null ? label : "";
    }

    /**
     * Sets the component's label. See {@link #getLabel(Component)}.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the label, not null.
     */
    public static void setLabel(Component component, String value) {
        if (component instanceof HasLabel hasLabel) {
            hasLabel.setLabel(value);
            return;
        }
        for (ComponentSupport support : SUPPORTS) {
            if (support.setLabel(component, value)) {
                return;
            }
        }
        component.getElement().setProperty("label",
                Utils.isBlank(value) ? null : value);
    }

    /**
     * Returns the caption an implementation reports for the component, or its
     * label.
     *
     * @param component
     *            the component, not null.
     * @return the caption.
     */
    public static String getCaption(Component component) {
        for (ComponentSupport support : SUPPORTS) {
            String caption = support.getCaption(component);
            if (caption != null) {
                return caption;
            }
        }
        return getLabel(component);
    }

    /**
     * Sets the component's caption. See {@link #getCaption(Component)}.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the caption, not null.
     */
    public static void setCaption(Component component, String value) {
        for (ComponentSupport support : SUPPORTS) {
            if (support.setCaption(component, value)) {
                return;
            }
        }
        setLabel(component, value);
    }

    /**
     * Returns the placeholder of a {@link HasPlaceholder} component.
     *
     * @param component
     *            the component, not null.
     * @return the placeholder, or null.
     */
    public static String getPlaceholder(Component component) {
        if (component instanceof HasPlaceholder hasPlaceholder) {
            return hasPlaceholder.getPlaceholder();
        }
        return null;
    }

    /**
     * Sets the placeholder of a {@link HasPlaceholder} component.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the placeholder, may be null.
     * @throws IllegalStateException
     *             if the component doesn't support a placeholder.
     */
    public static void setPlaceholder(Component component, String value) {
        if (component instanceof HasPlaceholder hasPlaceholder) {
            hasPlaceholder.setPlaceholder(value);
        } else {
            throw new IllegalStateException(component.getClass().getSimpleName()
                    + " doesn't support setting placeholder");
        }
    }

    /**
     * Sets up an event listener for overlay components that fires a
     * {@code closed} DOM event when the component is closed.
     *
     * @param component
     *            the component, not null.
     */
    public static void simulateClosedEvent(Component component) {
        if (ComponentUtil.getData(component,
                "hasSimulatedClosedEvent") != null) {
            return;
        }
        if (firesClosedEvent(component)) {
            ComponentUtil.setData(component, "hasSimulatedClosedEvent", true);
            component.getElement().addPropertyChangeListener("opened",
                    event -> {
                        if (Boolean.FALSE.equals(event.getValue())) {
                            BasicUtils._fireDomEvent(component, "closed");
                        }
                    });
        }
    }
}
