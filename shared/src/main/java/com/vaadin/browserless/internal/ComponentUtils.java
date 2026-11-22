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
 * Utilities for working with {@link Component}s.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ComponentUtils {

    private static final List<ComponentSupport> SUPPORTS = loadComponentSupports();

    private ComponentUtils() {
    }

    private static List<ComponentSupport> loadComponentSupports() {
        List<ComponentSupport> supports = new ArrayList<>();
        ServiceLoader
                .load(ComponentSupport.class,
                        ComponentSupport.class.getClassLoader())
                .forEach(supports::add);
        return List.copyOf(supports);
    }

    /**
     * Returns the data provider currently set to this Component.
     *
     * @param component
     *            the component, not null.
     * @return the data provider, or null.
     */
    static DataProvider<?, ?> getDataProvider(Component component) {
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
     * A component placeholder, usually shown when there's no value selected.
     * Not all components support a placeholder; those that don't will return
     * null.
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
     * Determines the {@code label} of given component:
     * {@link HasLabel#getLabel()} for components that have one, otherwise the
     * first non-null label provided by a registered {@link ComponentSupport}
     * (for example the text in the {@code label} slot of a {@code FormItem}),
     * otherwise the {@code label} property of the element. Returns an empty
     * string when there is no label.
     * <p>
     * {@link #getCaption(Component)} is displayed directly on the component
     * while label is displayed next to the component in a layout (e.g. a
     * {@code TextField} nested in a form layout).
     * <p>
     * Vote for <a href="https://github.com/vaadin/flow/issues/3241">issue
     * #3241</a>.
     *
     * @param component
     *            the component, not null.
     * @return the label, never null.
     * @see ComponentSupport#getLabel(Component)
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
     * Determines the caption of given component: the first non-null caption
     * provided by a registered {@link ComponentSupport} (for example the text
     * of a {@code Button}), otherwise {@link #getLabel(Component)}.
     * <p>
     * Caption is generally displayed directly on the component (e.g. the Button
     * text), while label is displayed next to the component in a layout (e.g. a
     * {@code TextField} nested in a form layout).
     *
     * @param component
     *            the component, not null.
     * @return the caption.
     * @see ComponentSupport#getCaption(Component)
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
     * Sets up an event listener for overlay components that fires a
     * {@code closed} DOM event when the component is closed. This simulates the
     * event being fired from the browser after the closing animation has
     * finished.
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

    /**
     * Checks whether given component is a {@code PolymerTemplate}.
     *
     * @param component
     *            the component, not null.
     * @return true if the component is a {@code PolymerTemplate}.
     */
    static boolean isPolymerTemplate(Component component) {
        return Utils.isPolymerTemplate(component);
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
    static List<Component> getAllChildren(Component component) {
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
    static boolean isClosedDialog(Component component) {
        return SUPPORTS.stream()
                .anyMatch(support -> support.isClosedDialog(component));
    }

    /**
     * Returns the dialog types of all implementations.
     *
     * @return the dialog types, not null.
     * @see ComponentSupport#getDialogTypes()
     */
    static List<Class<? extends Component>> getDialogTypes() {
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
    static boolean firesClosedEvent(Component component) {
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
    static void describe(Component component, List<String> description) {
        SUPPORTS.forEach(support -> support.describe(component, description));
    }
}
