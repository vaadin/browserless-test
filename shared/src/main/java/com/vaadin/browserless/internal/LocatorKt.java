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
 * Forwards to {@link Locator}, for code compiled against the former Kotlin file
 * facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link Locator} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class LocatorKt {

    private LocatorKt() {
    }

    public static java.util.List<java.lang.String> filterNotBlank(
            java.lang.Iterable<java.lang.String> p0) {
        return Locator.filterNotBlank(p0);
    }

    public static <T extends com.vaadin.flow.component.Component> T _get(
            com.vaadin.flow.component.Component p0, java.lang.Class<T> p1,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p2) {
        return Locator._get(p0, p1, p2);
    }

    public static <T extends com.vaadin.flow.component.Component> T _get(
            java.lang.Class<T> p0,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p1) {
        return Locator._get(p0, p1);
    }

    public static <T extends com.vaadin.flow.component.Component> java.util.List<T> _find(
            com.vaadin.flow.component.Component p0, java.lang.Class<T> p1,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p2) {
        return Locator._find(p0, p1, p2);
    }

    public static <T extends com.vaadin.flow.component.Component> java.util.List<T> _find(
            java.lang.Class<T> p0,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p1) {
        return Locator._find(p0, p1);
    }

    public static java.lang.Iterable<com.vaadin.flow.component.Component> _walkAll(
            com.vaadin.flow.component.Component p0) {
        return Locator._walkAll(p0);
    }

    public static <T extends com.vaadin.flow.component.Component> void _expectNone(
            com.vaadin.flow.component.Component p0, java.lang.Class<T> p1,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p2) {
        Locator._expectNone(p0, p1, p2);
    }

    public static void _expectNoDialogs() {
        Locator._expectNoDialogs();
    }

    public static <T extends com.vaadin.flow.component.Component> void _expectNone(
            java.lang.Class<T> p0,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p1) {
        Locator._expectNone(p0, p1);
    }

    public static <T extends com.vaadin.flow.component.Component> void _expectOne(
            com.vaadin.flow.component.Component p0, java.lang.Class<T> p1,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p2) {
        Locator._expectOne(p0, p1, p2);
    }

    public static <T extends com.vaadin.flow.component.Component> void _expectOne(
            java.lang.Class<T> p0,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p1) {
        Locator._expectOne(p0, p1);
    }

    public static <T extends com.vaadin.flow.component.Component> void _expect(
            com.vaadin.flow.component.Component p0, java.lang.Class<T> p1,
            int p2,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p3) {
        Locator._expect(p0, p1, p2, p3);
    }

    public static <T extends com.vaadin.flow.component.Component> void _expect(
            java.lang.Class<T> p0, int p1,
            kotlin.jvm.functions.Function1<? super com.vaadin.browserless.internal.SearchSpec<T>, kotlin.Unit> p2) {
        Locator._expect(p0, p1, p2);
    }

    public static void _expectInternalServerError(java.lang.String p0) {
        Locator._expectInternalServerError(p0);
    }

    public static java.lang.String getCurrentPath() {
        return Locator.getCurrentPath();
    }
}
