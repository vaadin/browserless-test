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
 * Forwards to {@link ComponentUtils}, for code compiled against the former
 * Kotlin file facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link ComponentUtils} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class ComponentUtilsKt {

    private ComponentUtilsKt() {
    }

    public static void fireEvent(com.vaadin.flow.component.Component p0,
            com.vaadin.flow.component.ComponentEvent<?> p1) {
        ComponentUtils.fireEvent(p0, p1);
    }

    public static <T extends com.vaadin.flow.component.ClickNotifier<?>> void serverClick(
            T p0, boolean p1, int p2, int p3, boolean p4, boolean p5,
            boolean p6, boolean p7) {
        ComponentUtils.serverClick(p0, p1, p2, p3, p4, p5, p6, p7);
    }

    public static java.lang.String getTextAlign(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.getTextAlign(p0);
    }

    public static void setTextAlign(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        ComponentUtils.setTextAlign(p0, p1);
    }

    public static java.lang.String getTooltip(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.getTooltip(p0);
    }

    public static void setTooltip(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        ComponentUtils.setTooltip(p0, p1);
    }

    public static com.vaadin.flow.dom.DomListenerRegistration addContextMenuListener(
            com.vaadin.flow.component.Component p0,
            com.vaadin.flow.dom.DomEventListener p1) {
        return ComponentUtils.addContextMenuListener(p0, p1);
    }

    public static com.vaadin.flow.dom.DomListenerRegistration preventDefault(
            com.vaadin.flow.dom.DomListenerRegistration p0) {
        return ComponentUtils.preventDefault(p0);
    }

    public static void removeFromParent(
            com.vaadin.flow.component.Component p0) {
        ComponentUtils.removeFromParent(p0);
    }

    public static com.vaadin.flow.component.Component findAncestorOrSelf(
            com.vaadin.flow.component.Component p0,
            kotlin.jvm.functions.Function1<? super com.vaadin.flow.component.Component, java.lang.Boolean> p1) {
        return ComponentUtils.findAncestorOrSelf(p0, p1);
    }

    public static boolean isNestedIn(com.vaadin.flow.component.Component p0,
            com.vaadin.flow.component.Component p1) {
        return ComponentUtils.isNestedIn(p0, p1);
    }

    public static boolean isAttached(com.vaadin.flow.component.Component p0) {
        return ComponentUtils.isAttached(p0);
    }

    public static com.vaadin.flow.data.provider.DataProvider<?, ?> getDataProvider(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.getDataProvider(p0);
    }

    public static void insertBefore(
            com.vaadin.flow.component.HasOrderedComponents p0,
            com.vaadin.flow.component.Component p1,
            com.vaadin.flow.component.Component p2) {
        ComponentUtils.insertBefore(p0, p1, p2);
    }

    public static com.vaadin.flow.router.Location getCurrentViewLocation(
            com.vaadin.flow.component.UI p0) {
        return ComponentUtils.getCurrentViewLocation(p0);
    }

    public static boolean getHasChildren(
            com.vaadin.flow.component.HasComponents p0) {
        return ComponentUtils.getHasChildren(p0);
    }

    public static void addClassNames2(com.vaadin.flow.component.HasStyle p0,
            java.lang.String p1) {
        ComponentUtils.addClassNames2(p0, p1);
    }

    public static void addClassNames2(com.vaadin.flow.component.HasStyle p0,
            java.lang.String... p1) {
        ComponentUtils.addClassNames2(p0, p1);
    }

    public static void removeClassNames2(com.vaadin.flow.component.HasStyle p0,
            java.lang.String p1) {
        ComponentUtils.removeClassNames2(p0, p1);
    }

    public static void removeClassNames2(com.vaadin.flow.component.HasStyle p0,
            java.lang.String... p1) {
        ComponentUtils.removeClassNames2(p0, p1);
    }

    public static void setClassNames2(com.vaadin.flow.component.HasStyle p0,
            java.lang.String p1) {
        ComponentUtils.setClassNames2(p0, p1);
    }

    public static void setClassNames2(com.vaadin.flow.component.HasStyle p0,
            java.lang.String... p1) {
        ComponentUtils.setClassNames2(p0, p1);
    }

    public static java.lang.String getPlaceholder(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.getPlaceholder(p0);
    }

    public static void setPlaceholder(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        ComponentUtils.setPlaceholder(p0, p1);
    }

    public static java.lang.String getLabel(
            com.vaadin.flow.component.formlayout.FormLayout.FormItem p0) {
        return ComponentUtils.getLabel(p0);
    }

    public static java.lang.String getLabel(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.getLabel(p0);
    }

    public static void setLabel(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        ComponentUtils.setLabel(p0, p1);
    }

    public static java.lang.String getCaption(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.getCaption(p0);
    }

    public static void setCaption(com.vaadin.flow.component.Component p0,
            java.lang.String p1) {
        ComponentUtils.setCaption(p0, p1);
    }

    public static void simulateClosedEvent(
            com.vaadin.flow.component.Component p0) {
        ComponentUtils.simulateClosedEvent(p0);
    }

    public static java.lang.String getCaption(
            com.vaadin.flow.component.button.Button p0) {
        return ComponentUtils.getCaption(p0);
    }

    public static void setCaption(com.vaadin.flow.component.button.Button p0,
            java.lang.String p1) {
        ComponentUtils.setCaption(p0, p1);
    }

    public static com.vaadin.flow.component.Component getChildComponentInSlot(
            com.vaadin.flow.component.HasElement p0, java.lang.String p1) {
        return ComponentUtils.getChildComponentInSlot(p0, p1);
    }

    public static void setChildComponentToSlot(
            com.vaadin.flow.component.HasElement p0, java.lang.String p1,
            com.vaadin.flow.component.Component p2) {
        ComponentUtils.setChildComponentToSlot(p0, p1, p2);
    }

    public static boolean isPolymerTemplate(
            com.vaadin.flow.component.Component p0) {
        return ComponentUtils.isPolymerTemplate(p0);
    }
}
