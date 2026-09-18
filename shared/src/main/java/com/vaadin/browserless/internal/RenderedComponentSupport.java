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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.AbstractComponentDataGenerator;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.renderer.ComponentRenderer;

/**
 * Reaches the components a {@link ComponentRenderer} has rendered for the rows
 * the client has asked for.
 * <p>
 * For internal use only.
 */
public final class RenderedComponentSupport {

    private RenderedComponentSupport() {
    }

    /**
     * Gets the component the column has rendered for the item with the given
     * key.
     * <p>
     * A component renderer keeps one component per item for as long as the
     * client has the row, and that is the component the browser shows. Flow
     * exposes no API for it: the mapping lives in
     * {@code AbstractComponentDataGenerator.renderedComponents}, and the
     * generator itself is reachable only through the private data generators of
     * the column, so both are read reflectively. The accessor belongs upstream,
     * on {@code Grid.Column} in {@code vaadin/flow-components} or on
     * {@link AbstractComponentDataGenerator} in {@code vaadin/flow}.
     *
     * @param column
     *            the column that renders the cell
     * @param itemKey
     *            key of the item the cell shows
     * @return the rendered component, or {@literal null} when the column has
     *         rendered no component for the item
     */
    public static Component getRenderedComponent(Grid.Column<?> column,
            String itemKey) {
        AbstractComponentDataGenerator<?> generator = findComponentDataGenerator(
                columnDataGenerator(column));
        if (generator == null) {
            return null;
        }
        try {
            Method getRenderedComponent = AbstractComponentDataGenerator.class
                    .getDeclaredMethod("getRenderedComponent", String.class);
            getRenderedComponent.setAccessible(true);
            return (Component) getRenderedComponent.invoke(generator, itemKey);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to read the component the column rendered for the item",
                    e);
        }
    }

    private static DataGenerator<?> columnDataGenerator(Grid.Column<?> column) {
        return (DataGenerator<?>) read(Grid.Column.class,
                "compositeDataGenerator", column);
    }

    private static AbstractComponentDataGenerator<?> findComponentDataGenerator(
            DataGenerator<?> generator) {
        if (generator instanceof AbstractComponentDataGenerator<?> componentDataGenerator) {
            return componentDataGenerator;
        }
        if (generator instanceof CompositeDataGenerator<?> composite
                && read(CompositeDataGenerator.class, "dataGenerators",
                        composite) instanceof Collection<?> nested) {
            for (Object child : nested) {
                AbstractComponentDataGenerator<?> found = findComponentDataGenerator(
                        (DataGenerator<?>) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Object read(Class<?> owner, String fieldName,
            Object instance) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to read " + owner.getSimpleName() + "." + fieldName,
                    e);
        }
    }
}
