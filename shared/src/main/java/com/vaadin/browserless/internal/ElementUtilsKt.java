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
 * Forwards to {@link ElementUtils}, for code compiled against the former Kotlin
 * file facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link ElementUtils} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class ElementUtilsKt {

    private ElementUtilsKt() {
    }

    public static void setOrRemoveAttribute(com.vaadin.flow.dom.Element p0,
            java.lang.String p1, java.lang.String p2) {
        ElementUtils.setOrRemoveAttribute(p0, p1, p2);
    }

    public static void toggle(com.vaadin.flow.dom.ClassList p0,
            java.lang.String p1) {
        ElementUtils.toggle(p0, p1);
    }

    public static void insertBefore(com.vaadin.flow.dom.Element p0,
            com.vaadin.flow.dom.Element p1, com.vaadin.flow.dom.Element p2) {
        ElementUtils.insertBefore(p0, p1, p2);
    }

    public static java.lang.String getTextRecursively2(
            com.vaadin.flow.dom.Element p0) {
        return ElementUtils.getTextRecursively2(p0);
    }

    public static java.lang.String getTextRecursively(org.jsoup.nodes.Node p0) {
        return ElementUtils.getTextRecursively(p0);
    }

    public static java.util.List<com.vaadin.flow.dom.Element> getVirtualChildren(
            com.vaadin.flow.dom.Element p0) {
        return ElementUtils.getVirtualChildren(p0);
    }

    public static com.vaadin.flow.dom.Element getElement(
            com.vaadin.flow.internal.StateNode p0) {
        return ElementUtils.getElement(p0);
    }

    public static java.util.List<com.vaadin.flow.dom.Element> getChildrenInSlot(
            com.vaadin.flow.dom.Element p0, java.lang.String p1) {
        return ElementUtils.getChildrenInSlot(p0, p1);
    }

    public static void clearSlot(com.vaadin.flow.dom.Element p0,
            java.lang.String p1) {
        ElementUtils.clearSlot(p0, p1);
    }

    public static java.util.List<com.vaadin.flow.component.Component> _findComponents(
            com.vaadin.flow.dom.Element p0) {
        return ElementUtils._findComponents(p0);
    }

    public static void _fireDomEvent(com.vaadin.flow.dom.Element p0,
            com.vaadin.flow.dom.DomEvent p1) {
        ElementUtils._fireDomEvent(p0, p1);
    }
}
