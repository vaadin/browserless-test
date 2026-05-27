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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.renderer.BasicRenderer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.ValueProvider;

import org.jsoup.Jsoup;

public final class Renderers {

    private Renderers() {
    }

    private static final Method _BasicRenderer_getFormattedValue;
    private static final Field _BasicRenderer_valueProvider;
    private static final Field _Renderer_template;

    static {
        try {
            Method m = null;
            for (Method candidate : BasicRenderer.class.getDeclaredMethods()) {
                if (candidate.getName().equals("getFormattedValue")) {
                    m = candidate;
                    break;
                }
            }
            if (m == null) {
                throw new NoSuchMethodException("getFormattedValue");
            }
            m.setAccessible(true);
            _BasicRenderer_getFormattedValue = m;

            Field valueProviderField = BasicRenderer.class.getDeclaredField("valueProvider");
            valueProviderField.setAccessible(true);
            _BasicRenderer_valueProvider = valueProviderField;

            Field templateField = Renderer.class.getDeclaredField("template");
            templateField.setAccessible(true);
            _Renderer_template = templateField;
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the output of this renderer for given [rowObject] formatted as close as possible
     * to the client-side output.
     */
    @SuppressWarnings("unchecked")
    public static <T> String _getPresentationValue(Renderer<T> renderer, T rowObject) {
        try {
            if (renderer instanceof BasicRenderer) {
                BasicRenderer<T, ?> basicRenderer = (BasicRenderer<T, ?>) renderer;
                Object value = valueProvider(basicRenderer).apply(rowObject);
                return (String) _BasicRenderer_getFormattedValue.invoke(basicRenderer, value);
            }
            if (renderer instanceof TextRenderer) {
                return renderText((TextRenderer<T>) renderer, rowObject);
            }
            if (renderer instanceof ComponentRenderer) {
                ComponentRenderer<?, T> componentRenderer = (ComponentRenderer<?, T>) renderer;
                Component component = componentRenderer.createComponent(rowObject);
                return PrettyPrintTreeKt.toPrettyString(component);
            }
            if (renderer.getClass().getSimpleName().equals("LitRenderer")) {
                // LitRenderer re-declares private members
                Field templateProperty = renderer.getClass().getDeclaredField("templateExpression");
                templateProperty.setAccessible(true);
                String templateExpression = (String) templateProperty.get(renderer);

                Field valueProvidersProperty = renderer.getClass().getDeclaredField("valueProviders");
                valueProvidersProperty.setAccessible(true);
                Map<String, ValueProvider<T, ?>> valueProviders =
                        (Map<String, ValueProvider<T, ?>>) valueProvidersProperty.get(renderer);

                String renderedLitTemplateHtml = renderLitTemplate(templateExpression, valueProviders, rowObject);
                return ElementUtils.textRecursively(Jsoup.parse(renderedLitTemplateHtml));
            }
            return null;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String renderLitTemplate(String template, Map<String, ValueProvider<T, ?>> valueProviders,
            T item) {
        String renderedTemplate = template;
        for (Map.Entry<String, ValueProvider<T, ?>> entry : valueProviders.entrySet()) {
            String placeholder = "${item." + entry.getKey() + "}";
            if (renderedTemplate.contains(placeholder)) {
                renderedTemplate = renderedTemplate.replace(placeholder, String.valueOf(entry.getValue().apply(item)));
            }
        }
        return renderedTemplate;
    }

    /**
     * Returns the text rendered for given [item].
     */
    public static <T> String renderText(TextRenderer<T> renderer, T item) {
        return renderer.createComponent(item).getElement().getText();
    }

    /**
     * Returns the [ValueProvider] set to [BasicRenderer].
     */
    @SuppressWarnings("unchecked")
    public static <T, V> ValueProvider<T, V> valueProvider(BasicRenderer<T, V> renderer) {
        try {
            return (ValueProvider<T, V>) _BasicRenderer_valueProvider.get(renderer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Polymer Template set to the [Renderer].
     */
    public static String template(Renderer<?> renderer) {
        try {
            String template = (String) _Renderer_template.get(renderer);
            return template != null ? template : "";
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
