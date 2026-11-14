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
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jsoup.Jsoup;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.renderer.BasicRenderer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.ValueProvider;

/**
 * Functions for computing the output of renderers.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class Renderers {

    private Renderers() {
    }

    private static volatile Method basicRendererGetFormattedValue;

    private static volatile Field basicRendererValueProvider;

    private static Method getBasicRendererGetFormattedValue() {
        Method m = basicRendererGetFormattedValue;
        if (m == null) {
            m = Arrays.stream(BasicRenderer.class.getDeclaredMethods())
                    .filter(it -> it.getName().equals("getFormattedValue"))
                    .findFirst().orElseThrow(() -> new NoSuchElementException(
                            "Array contains no element matching the predicate."));
            m.setAccessible(true);
            basicRendererGetFormattedValue = m;
        }
        return m;
    }

    private static Field getBasicRendererValueProvider() {
        Field f = basicRendererValueProvider;
        if (f == null) {
            try {
                f = BasicRenderer.class.getDeclaredField("valueProvider");
            } catch (NoSuchFieldException e) {
                throw Utils.sneakyThrow(e);
            }
            f.setAccessible(true);
            basicRendererValueProvider = f;
        }
        return f;
    }

    /**
     * Returns the output of this renderer for given {@code rowObject} formatted
     * as close as possible to the client-side output.
     *
     * @param renderer
     *            the renderer, not null.
     * @param rowObject
     *            the item to render.
     * @param <T>
     *            the item type.
     * @return the rendered value, or null if the renderer is not supported.
     */
    @SuppressWarnings("unchecked")
    public static <T> String _getPresentationValue(Renderer<T> renderer,
            T rowObject) {
        try {
            if (renderer instanceof BasicRenderer<T, ?> basicRenderer) {
                Object value = getValueProvider(basicRenderer).apply(rowObject);
                return (String) getBasicRendererGetFormattedValue()
                        .invoke(basicRenderer, value);
            }
            if (renderer instanceof TextRenderer<T> textRenderer) {
                return renderText(textRenderer, rowObject);
            }
            if (renderer instanceof ComponentRenderer<?, T> componentRenderer) {
                Component component = componentRenderer
                        .createComponent(rowObject);
                return PrettyPrintTree.toPrettyString(component);
            }
            if ("LitRenderer".equals(renderer.getClass().getSimpleName())) {
                // LitRenderer re-declares private members
                Field templateProperty = renderer.getClass()
                        .getDeclaredField("templateExpression");
                templateProperty.setAccessible(true);
                String templateExpression = (String) templateProperty
                        .get(renderer);

                Field valueProvidersProperty = renderer.getClass()
                        .getDeclaredField("valueProviders");
                valueProvidersProperty.setAccessible(true);
                Map<String, ValueProvider<T, ?>> valueProviders = (Map<String, ValueProvider<T, ?>>) valueProvidersProperty
                        .get(renderer);

                String renderedLitTemplateHtml = renderLitTemplate(
                        templateExpression, valueProviders, rowObject);
                return ElementUtils.getTextRecursively(
                        Jsoup.parse(renderedLitTemplateHtml));
            }
            return null;
        } catch (ReflectiveOperationException e) {
            throw Utils.sneakyThrow(e);
        }
    }

    /**
     * Renders the given lit {@code template}, replacing each
     * <code>${item.key}</code> with the output of the corresponding value
     * provider.
     *
     * @param template
     *            the lit template expression.
     * @param valueProviders
     *            the value providers, keyed by property name.
     * @param item
     *            the item to render.
     * @param <T>
     *            the item type.
     * @return the rendered template.
     */
    public static <T> String renderLitTemplate(String template,
            Map<String, ? extends ValueProvider<T, ?>> valueProviders, T item) {
        String renderedTemplate = template;
        for (Map.Entry<String, ? extends ValueProvider<T, ?>> entry : valueProviders
                .entrySet()) {
            String placeholder = "${item." + entry.getKey() + "}";
            if (renderedTemplate.contains(placeholder)) {
                renderedTemplate = renderedTemplate.replace(placeholder,
                        entry.getValue().apply(item).toString());
            }
        }
        return renderedTemplate;
    }

    /**
     * Returns the text rendered for given {@code item}.
     *
     * @param renderer
     *            the text renderer, not null.
     * @param item
     *            the item to render.
     * @param <T>
     *            the item type.
     * @return the rendered text.
     */
    public static <T> String renderText(TextRenderer<T> renderer, T item) {
        return renderer.createComponent(item).getElement().getText();
    }

    /**
     * Returns the {@link ValueProvider} set to {@link BasicRenderer}.
     *
     * @param renderer
     *            the renderer, not null.
     * @param <T>
     *            the item type.
     * @param <V>
     *            the value type.
     * @return the value provider.
     */
    @SuppressWarnings("unchecked")
    public static <T, V> ValueProvider<T, V> getValueProvider(
            BasicRenderer<T, V> renderer) {
        try {
            return (ValueProvider<T, V>) getBasicRendererValueProvider()
                    .get(renderer);
        } catch (IllegalAccessException e) {
            throw Utils.sneakyThrow(e);
        }
    }

}
