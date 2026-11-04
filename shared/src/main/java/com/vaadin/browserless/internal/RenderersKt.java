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

/**
 * Forwards to {@link Renderers}, for code compiled against the former Kotlin
 * file facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link Renderers} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class RenderersKt {

    private RenderersKt() {
    }

    public static <T> java.lang.String _getPresentationValue(
            com.vaadin.flow.data.renderer.Renderer<T> p0, T p1) {
        return Renderers._getPresentationValue(p0, p1);
    }

    public static <T> java.lang.String renderLitTemplate(java.lang.String p0,
            java.util.Map<java.lang.String, ? extends com.vaadin.flow.function.ValueProvider<T, ?>> p1,
            T p2) {
        return Renderers.renderLitTemplate(p0, p1, p2);
    }

    public static <T> java.lang.String renderText(
            com.vaadin.flow.data.renderer.TextRenderer<T> p0, T p1) {
        return Renderers.renderText(p0, p1);
    }

    public static <T, V> com.vaadin.flow.function.ValueProvider<T, V> getValueProvider(
            com.vaadin.flow.data.renderer.BasicRenderer<T, V> p0) {
        return Renderers.getValueProvider(p0);
    }
}
