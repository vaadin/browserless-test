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

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.vaadin.browserless.mocks.MockHttpSession;
import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.browserless.mocks.MockResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;

/**
 * Assorted helpers: reaching the current Vaadin instances, unwrapping them to
 * the mocks behind them, and loading classes that may not be on the classpath.
 * <p>
 * The current-instance accessors fail with an explanatory message rather than
 * returning {@code null}, because a missing current UI almost always means the
 * test forgot to set the environment up, and that is worth saying once here
 * instead of debugging a {@link NullPointerException} at the call site.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Returns the major JVM version, e.g. 6 for Java 1.6, 8 for Java 8, 11 for
     * Java 11 etc.
     */
    public static int jvmVersion() {
        return parseJvmVersion(System.getProperty("java.version"));
    }

    /**
     * Returns the major JVM version, 1 for 1.1, 2 for 1.2, 3 for 1.3, 4 for
     * 1.4, 5 for 1.5 etc.
     */
    static int parseJvmVersion(String version) {
        // taken from
        // https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
        String v = version;
        if (v.startsWith("1.")) {
            v = v.substring(2);
        }
        int end = 0;
        while (end < v.length() && Character.isDigit(v.charAt(end))) {
            end++;
        }
        return Integer.parseInt(v.substring(0, end));
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    static List<String> splitByWhitespaces(String s) {
        List<String> result = new ArrayList<>();
        for (String token : WHITESPACE.split(s)) {
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }

    static boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    static String ellipsize(String s, int maxLength) {
        return ellipsize(s, maxLength, "...");
    }

    static String ellipsize(String s, int maxLength, String ellipsize) {
        if (maxLength < ellipsize.length()) {
            throw new IllegalArgumentException(
                    "maxLength must be at least the size of ellipsize "
                            + ellipsize + " but it was " + maxLength);
        }
        if (s.length() <= maxLength || s.length() <= ellipsize.length()) {
            return s;
        }
        return s.substring(0, maxLength - ellipsize.length()) + ellipsize;
    }

    /**
     * For a class implementing the {@link HasErrorParameter} interface,
     * determines the type of the exception handled (the type of `T`). Returns
     * null if the Class doesn't implement the {@link HasErrorParameter}
     * interface.
     */
    static Class<?> getErrorParameterType(Class<?> clazz) {
        return ReflectTools.getGenericInterfaceType(clazz,
                HasErrorParameter.class);
    }

    static boolean isRouteNotFound(Class<?> clazz) {
        return getErrorParameterType(clazz) == NotFoundException.class;
    }

    public static VaadinRequest currentRequest() {
        VaadinRequest req = VaadinService.getCurrentRequest();
        if (req == null) {
            throw new IllegalStateException(
                    "No current request. Have you called MockVaadin.setup()?");
        }
        return req;
    }

    public static VaadinResponse currentResponse() {
        VaadinResponse resp = VaadinService.getCurrentResponse();
        if (resp == null) {
            throw new IllegalStateException(
                    "No current response. Have you called MockVaadin.setup()?");
        }
        return resp;
    }

    /**
     * Returns the {@link UI#getCurrent}; fails with informative error message
     * if the UI.getCurrent() is null.
     */
    public static UI currentUI() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "UI.getCurrent() is null. Have you called MockVaadin.setup()?");
        }
        return ui;
    }

    /**
     * Retrieves the mock request which backs up {@link VaadinRequest}. ```
     * currentRequest.mock.addCookie(Cookie("foo", "bar")) ```
     */
    public static MockRequest mock(VaadinRequest request) {
        return (MockRequest) ((VaadinServletRequest) request).getRequest();
    }

    /**
     * Retrieves the mock response which backs up {@link VaadinResponse}. ```
     * currentResponse.mock.getCookie("foo").value ```
     */
    public static MockResponse mock(VaadinResponse response) {
        return (MockResponse) ((VaadinServletResponse) response).getResponse();
    }

    /**
     * Retrieves the mock session which backs up {@link VaadinSession}. ```
     * VaadinSession.getCurrent().mock ```
     */
    public static MockHttpSession mock(VaadinSession session) {
        return (MockHttpSession) ((WrappedHttpSession) session.getSession())
                .getHttpSession();
    }

    public static ServletContext getContext(VaadinContext context) {
        return ((VaadinServletContext) context).getContext();
    }

    public static boolean isInitialized(Servlet servlet) {
        return servlet.getServletConfig() != null;
    }

    static boolean hasCustomToString(Class<?> clazz) {
        try {
            return clazz.getMethod("toString")
                    .getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static final Class<?> polymerTemplateClass = findClass(
            "com.vaadin.flow.component.polymertemplate.PolymerTemplate");

    static boolean hasPolymerTemplates() {
        return polymerTemplateClass != null;
    }

    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            try {
                return Class.forName(className, true,
                        Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException ex2) {
                return null;
            }
        }
    }

    public static Class<?> findClassOrThrow(String className)
            throws ClassNotFoundException {
        Class<?> clazz = findClass(className);
        if (clazz == null) {
            throw new ClassNotFoundException(className);
        }
        return clazz;
    }
}
