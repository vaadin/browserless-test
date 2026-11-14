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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import kotlin.ranges.IntRange;

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
 * General-purpose utilities: serialization, string helpers, class lookup and
 * accessors for the current request, response and UI.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class Utils {

    private static final Pattern REGEX_WHITESPACE = Pattern.compile("\\s+");

    private static final Class<?> POLYMER_TEMPLATE_CLASS = findClass(
            "com.vaadin.flow.component.polymertemplate.PolymerTemplate");

    private Utils() {
    }

    /**
     * Serializes given object into a byte array using Java serialization.
     *
     * @param obj
     *            the object to serialize, not null.
     * @return the serialized form of the object.
     */
    public static byte[] serializeToBytes(Serializable obj) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (bout) {
                new ObjectOutputStream(bout).writeObject(obj);
            }
            return bout.toByteArray();
        } catch (IOException e) {
            throw sneakyThrow(e);
        }
    }

    /**
     * Deserializes an object from given byte array using Java serialization.
     *
     * @param bytes
     *            the serialized form, not null.
     * @param <T>
     *            the type of the deserialized object
     * @return the deserialized object.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(byte[] bytes) {
        try {
            return (T) new ObjectInputStream(new ByteArrayInputStream(bytes))
                    .readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw sneakyThrow(e);
        }
    }

    /**
     * Serializes given object and deserializes it back, returning the copy.
     *
     * @param obj
     *            the object to serialize, not null.
     * @param <T>
     *            the type of the object
     * @return a deserialized copy of the object.
     */
    public static <T extends Serializable> T serializeDeserialize(T obj) {
        return deserialize(serializeToBytes(obj));
    }

    /**
     * Returns the number of items in given range, or 0 if the range is empty.
     *
     * @param range
     *            the range, not null.
     * @return the size of the range.
     */
    public static int getSize(IntRange range) {
        return Math.max(range.getEndInclusive() + 1 - range.getStart(), 0);
    }

    /**
     * Returns the major JVM version, e.g. 6 for Java 1.6, 8 for Java 8, 11 for
     * Java 11 etc.
     *
     * @return the major JVM version.
     */
    public static int getJvmVersion() {
        return parseJvmVersion(System.getProperty("java.version"));
    }

    /**
     * Returns the major JVM version, 1 for 1.1, 2 for 1.2, 3 for 1.3, 4 for
     * 1.4, 5 for 1.5 etc.
     *
     * @param javaVersion
     *            the value of the {@code java.version} system property.
     * @return the major JVM version.
     */
    public static int parseJvmVersion(String javaVersion) {
        // taken from
        // https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
        String version = javaVersion.startsWith("1.") ? javaVersion.substring(2)
                : javaVersion;
        int end = 0;
        while (end < version.length()
                && Character.isDigit(version.charAt(end))) {
            end++;
        }
        return Integer.parseInt(version.substring(0, end));
    }

    /**
     * Splits given string by whitespaces, omitting blank parts.
     *
     * @param string
     *            the string to split, not null.
     * @return the non-blank parts.
     */
    public static List<String> splitByWhitespaces(String string) {
        List<String> result = new ArrayList<>();
        for (String part : REGEX_WHITESPACE.split(string, -1)) {
            if (!isBlank(part)) {
                result.add(part);
            }
        }
        return result;
    }

    /**
     * Checks whether given string contains any whitespace character.
     *
     * @param string
     *            the string to check, not null.
     * @return true if the string contains a whitespace character.
     */
    public static boolean containsWhitespace(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (isWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shortens given string to at most {@code maxLength} characters, ending it
     * with {@code ellipsize} if it had to be shortened.
     *
     * @param string
     *            the string to shorten, not null.
     * @param maxLength
     *            the maximum length of the result.
     * @param ellipsize
     *            the suffix to append to a shortened string, e.g.
     *            {@code "..."}.
     * @return the possibly shortened string.
     */
    public static String ellipsize(String string, int maxLength,
            String ellipsize) {
        if (maxLength < ellipsize.length()) {
            throw new IllegalArgumentException(
                    "maxLength must be at least the size of ellipsize "
                            + ellipsize + " but it was " + maxLength);
        }
        if (string.length() <= maxLength
                || string.length() <= ellipsize.length()) {
            return string;
        }
        return string.substring(0, maxLength - ellipsize.length()) + ellipsize;
    }

    /**
     * For a class implementing the {@link HasErrorParameter} interface,
     * determines the type of the exception handled (the type of {@code T}).
     * Returns null if the Class doesn't implement the {@link HasErrorParameter}
     * interface.
     *
     * @param clazz
     *            the class to inspect, not null.
     * @return the handled exception type, or null.
     */
    public static Class<?> getErrorParameterType(Class<?> clazz) {
        return ReflectTools.getGenericInterfaceType(clazz,
                HasErrorParameter.class);
    }

    /**
     * Checks whether given class is an error view handling
     * {@link NotFoundException}.
     *
     * @param clazz
     *            the class to inspect, not null.
     * @return true if the class handles {@link NotFoundException}.
     */
    public static boolean isRouteNotFound(Class<?> clazz) {
        return getErrorParameterType(clazz) == NotFoundException.class;
    }

    /**
     * Returns {@link VaadinService#getCurrentRequest()}; fails with an
     * informative error message if there is no current request.
     *
     * @return the current request, not null.
     */
    public static VaadinRequest getCurrentRequest() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request == null) {
            throw new IllegalStateException(
                    "No current request. Have you called MockVaadin.setup()?");
        }
        return request;
    }

    /**
     * Returns {@link VaadinService#getCurrentResponse()}; fails with an
     * informative error message if there is no current response.
     *
     * @return the current response, not null.
     */
    public static VaadinResponse getCurrentResponse() {
        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response == null) {
            throw new IllegalStateException(
                    "No current response. Have you called MockVaadin.setup()?");
        }
        return response;
    }

    /**
     * Returns the {@link UI#getCurrent()}; fails with informative error message
     * if the UI.getCurrent() is null.
     *
     * @return the current UI, not null.
     */
    public static UI getCurrentUI() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "UI.getCurrent() is null. Have you called MockVaadin.setup()?");
        }
        return ui;
    }

    /**
     * Retrieves the mock request which backs up given {@link VaadinRequest}.
     *
     * <pre>
     * Utils.getMock(Utils.getCurrentRequest())
     *         .addCookie(new Cookie("foo", "bar"));
     * </pre>
     *
     * @param request
     *            the Vaadin request, not null.
     * @return the backing mock request.
     */
    public static MockRequest getMock(VaadinRequest request) {
        return (MockRequest) ((VaadinServletRequest) request).getRequest();
    }

    /**
     * Retrieves the mock response which backs up given {@link VaadinResponse}.
     *
     * <pre>
     * Utils.getMock(Utils.getCurrentResponse()).getCookie("foo").getValue();
     * </pre>
     *
     * @param response
     *            the Vaadin response, not null.
     * @return the backing mock response.
     */
    public static MockResponse getMock(VaadinResponse response) {
        return (MockResponse) ((VaadinServletResponse) response).getResponse();
    }

    /**
     * Retrieves the mock session which backs up given {@link VaadinSession}.
     *
     * <pre>
     * Utils.getMock(VaadinSession.getCurrent())
     * </pre>
     *
     * @param session
     *            the Vaadin session, not null.
     * @return the backing mock HTTP session.
     */
    public static MockHttpSession getMock(VaadinSession session) {
        return (MockHttpSession) ((WrappedHttpSession) session.getSession())
                .getHttpSession();
    }

    /**
     * Returns the servlet context which backs up given {@link VaadinContext}.
     *
     * @param context
     *            the Vaadin context, not null.
     * @return the backing servlet context.
     */
    public static ServletContext getContext(VaadinContext context) {
        return ((VaadinServletContext) context).getContext();
    }

    /**
     * Checks whether given servlet has been initialized.
     *
     * @param servlet
     *            the servlet, not null.
     * @return true if the servlet has a servlet config.
     */
    public static boolean isInitialized(Servlet servlet) {
        return servlet.getServletConfig() != null;
    }

    /**
     * Checks whether given class overrides {@link Object#toString()}.
     *
     * @param clazz
     *            the class to inspect, not null.
     * @return true if {@code toString()} is not declared by {@link Object}.
     */
    public static boolean hasCustomToString(Class<?> clazz) {
        try {
            return clazz.getMethod("toString")
                    .getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            throw sneakyThrow(e);
        }
    }

    /**
     * Returns the {@code PolymerTemplate} class, or null if it's not on the
     * classpath.
     *
     * @return the {@code PolymerTemplate} class, or null.
     */
    public static Class<?> getPolymerTemplateClass() {
        return POLYMER_TEMPLATE_CLASS;
    }

    /**
     * Checks whether the {@code PolymerTemplate} class is on the classpath.
     *
     * @return true if Polymer templates are available.
     */
    public static boolean hasPolymerTemplates() {
        return POLYMER_TEMPLATE_CLASS != null;
    }

    /**
     * Looks up a class by name, first via {@link Class#forName(String)} and
     * then via the context class loader.
     *
     * @param className
     *            the fully qualified class name, not null.
     * @return the class, or null if it can't be found.
     */
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

    /**
     * Looks up a class by name like {@link #findClass(String)}, but fails if
     * the class can't be found.
     *
     * @param className
     *            the fully qualified class name, not null.
     * @return the class, not null.
     * @throws ClassNotFoundException
     *             if the class can't be found.
     */
    public static Class<?> findClassOrThrow(String className)
            throws ClassNotFoundException {
        Class<?> clazz = findClass(className);
        if (clazz == null) {
            throw new ClassNotFoundException(className);
        }
        return clazz;
    }

    /**
     * Removes leading and trailing whitespace from given string, using the same
     * definition as Kotlin's {@code String.trim()}.
     */
    static String trim(String string) {
        int start = 0;
        int end = string.length();
        while (start < end && isWhitespace(string.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(string.charAt(end - 1))) {
            end--;
        }
        return string.substring(start, end);
    }

    /**
     * Checks whether given character is a whitespace, using the same definition
     * as Kotlin's {@code Char.isWhitespace()}.
     */
    static boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch);
    }

    /**
     * Checks whether given string is null, empty or consists solely of
     * whitespace characters, using the same definition as Kotlin's
     * {@code CharSequence.isNullOrBlank()}.
     */
    static boolean isBlank(CharSequence string) {
        if (string == null) {
            return true;
        }
        for (int i = 0; i < string.length(); i++) {
            if (!isWhitespace(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rethrows given checked exception without declaring it, the same way
     * Kotlin code propagates checked exceptions.
     */
    @SuppressWarnings("unchecked")
    static <E extends Throwable> RuntimeException sneakyThrow(Throwable e)
            throws E {
        throw (E) e;
    }
}
