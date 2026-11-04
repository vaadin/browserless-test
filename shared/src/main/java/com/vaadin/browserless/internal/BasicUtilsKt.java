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
 * Forwards to {@link BasicUtils}, for code compiled against the former Kotlin
 * file facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link BasicUtils} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class BasicUtilsKt {

    private BasicUtilsKt() {
    }

    public static void _fireEvent(com.vaadin.flow.component.Component p0,
            com.vaadin.flow.component.ComponentEvent<?> p1) {
        BasicUtils._fireEvent(p0, p1);
    }

    public static void _fireDomEvent(com.vaadin.flow.component.Component p0,
            java.lang.String p1, tools.jackson.databind.node.ObjectNode p2) {
        BasicUtils._fireDomEvent(p0, p1, p2);
    }

    public static void _fireDomEvent(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        BasicUtils._fireDomEvent(p0, p1);
    }

    public static java.lang.String getId_(
            com.vaadin.flow.component.Component p0) {
        return BasicUtils.getId_(p0);
    }

    public static void setId_(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        BasicUtils.setId_(p0, p1);
    }

    public static boolean get_isVisible(
            com.vaadin.flow.component.Component p0) {
        return BasicUtils.get_isVisible(p0);
    }

    public static java.lang.String get_text(
            com.vaadin.flow.component.Component p0) {
        return BasicUtils.get_text(p0);
    }

    public static void checkEditableByUser(
            com.vaadin.flow.component.Component p0) {
        BasicUtils.checkEditableByUser(p0);
    }

    public static void expectNotEditableByUser(
            com.vaadin.flow.component.Component p0) {
        BasicUtils.expectNotEditableByUser(p0);
    }

    public static boolean isEffectivelyVisible(
            com.vaadin.flow.component.Component p0) {
        return BasicUtils.isEffectivelyVisible(p0);
    }

    public static boolean isEffectivelyEnabled(
            com.vaadin.flow.component.Component p0) {
        return BasicUtils.isEffectivelyEnabled(p0);
    }

    public static boolean matches(com.vaadin.flow.component.Component p0,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<com.vaadin.flow.component.Component>, kotlin.Unit> p1) {
        return BasicUtils.matches(p0, p1);
    }

    public static <T extends com.vaadin.flow.component.Component & com.vaadin.flow.component.Focusable<?>> void _focus(
            T p0) {
        BasicUtils._focus(p0);
    }

    public static <T extends com.vaadin.flow.component.Component & com.vaadin.flow.component.Focusable<?>> void _blur(
            T p0) {
        BasicUtils._blur(p0);
    }

    public static void _close(com.vaadin.flow.component.UI p0) {
        BasicUtils._close(p0);
    }

    public static java.util.List<com.vaadin.flow.component.Component> _getVirtualChildren(
            com.vaadin.flow.component.Component p0) {
        return BasicUtils._getVirtualChildren(p0);
    }

    public static java.lang.String getErrorMessage(
            com.vaadin.flow.router.InternalServerError p0) {
        return BasicUtils.getErrorMessage(p0);
    }

    public static int get_saneFetchLimit() {
        return BasicUtils.get_saneFetchLimit();
    }
}
