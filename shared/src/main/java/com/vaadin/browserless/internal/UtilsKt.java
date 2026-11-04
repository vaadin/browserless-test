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
 * Forwards to {@link Utils}, for code compiled against the former Kotlin file
 * facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link Utils} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class UtilsKt {

    private UtilsKt() {
    }

    public static byte[] serializeToBytes(java.io.Serializable p0) {
        return Utils.serializeToBytes(p0);
    }

    public static <T extends java.io.Serializable> T deserialize(byte[] p0) {
        return Utils.deserialize(p0);
    }

    public static <T extends java.io.Serializable> T serializeDeserialize(
            T p0) {
        return Utils.serializeDeserialize(p0);
    }

    public static int getSize(kotlin.ranges.IntRange p0) {
        return Utils.getSize(p0);
    }

    public static int getJvmVersion() {
        return Utils.getJvmVersion();
    }

    public static int parseJvmVersion(java.lang.String p0) {
        return Utils.parseJvmVersion(p0);
    }

    public static java.util.List<java.lang.String> splitByWhitespaces(
            java.lang.String p0) {
        return Utils.splitByWhitespaces(p0);
    }

    public static boolean containsWhitespace(java.lang.String p0) {
        return Utils.containsWhitespace(p0);
    }

    public static java.lang.String ellipsize(java.lang.String p0, int p1,
            java.lang.String p2) {
        return Utils.ellipsize(p0, p1, p2);
    }

    public static java.lang.Class<?> getErrorParameterType(
            java.lang.Class<?> p0) {
        return Utils.getErrorParameterType(p0);
    }

    public static boolean isRouteNotFound(java.lang.Class<?> p0) {
        return Utils.isRouteNotFound(p0);
    }

    public static com.vaadin.flow.server.VaadinRequest getCurrentRequest() {
        return Utils.getCurrentRequest();
    }

    public static com.vaadin.flow.server.VaadinResponse getCurrentResponse() {
        return Utils.getCurrentResponse();
    }

    public static com.vaadin.flow.component.UI getCurrentUI() {
        return Utils.getCurrentUI();
    }

    public static com.vaadin.browserless.mocks.MockRequest getMock(
            com.vaadin.flow.server.VaadinRequest p0) {
        return Utils.getMock(p0);
    }

    public static com.vaadin.browserless.mocks.MockResponse getMock(
            com.vaadin.flow.server.VaadinResponse p0) {
        return Utils.getMock(p0);
    }

    public static com.vaadin.browserless.mocks.MockHttpSession getMock(
            com.vaadin.flow.server.VaadinSession p0) {
        return Utils.getMock(p0);
    }

    public static jakarta.servlet.ServletContext getContext(
            com.vaadin.flow.server.VaadinContext p0) {
        return Utils.getContext(p0);
    }

    public static boolean isInitialized(jakarta.servlet.Servlet p0) {
        return Utils.isInitialized(p0);
    }

    public static boolean hasCustomToString(java.lang.Class<?> p0) {
        return Utils.hasCustomToString(p0);
    }

    public static java.lang.Class<?> getPolymerTemplateClass() {
        return Utils.getPolymerTemplateClass();
    }

    public static boolean hasPolymerTemplates() {
        return Utils.hasPolymerTemplates();
    }

    public static java.lang.Class<?> findClass(java.lang.String p0) {
        return Utils.findClass(p0);
    }

    public static java.lang.Class<?> findClassOrThrow(java.lang.String p0)
            throws java.lang.ClassNotFoundException {
        return Utils.findClassOrThrow(p0);
    }
}
