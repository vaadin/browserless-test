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
package com.vaadin.browserless.mocks;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import static com.vaadin.browserless.mocks.MockUtils.putOrRemove;

/**
 * A standalone {@link HttpServletRequest}, backed by the mocked session.
 * <p>
 * Only the parts of a request a Vaadin service reads are modeled: headers,
 * parameters, cookies, attributes, locale, the session and the security
 * principal. The rest throws {@link UnsupportedOperationException} rather than
 * returning a plausible-looking value, so a test that depends on it fails
 * loudly instead of asserting on a fiction. Ports, the auth type and the secure
 * flag come from {@link MockHttpEnvironment} and can be changed there.
 * <p>
 * Container failure modes are reproduced where application code depends on
 * them: {@link #getSession(boolean)} reports no session once it has been
 * invalidated, and {@link #changeSessionId()} refuses to run without one.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockRequest implements HttpServletRequest {

    private HttpSession session;

    /**
     * The ID of the session the client asked for. Just like in a servlet
     * container it stays the same for the lifetime of the request, even if the
     * session ID is rotated via {@link #changeSessionId()} or a new session is
     * created after invalidation.
     */
    private final String initiallyRequestedSessionId;

    /**
     * Creates a request bound to the given session.
     *
     * @param session
     *            the session this request belongs to
     */
    public MockRequest(HttpSession session) {
        this.session = session;
        this.initiallyRequestedSessionId = session.getId();
        headers.put("user-agent",
                Collections.singletonList("IntelliJ IDEA/182.4892.20"));
    }

    @Override
    public ServletInputStream getInputStream() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public AsyncContext startAsync() {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest,
            ServletResponse servletResponse) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer("http://localhost:8080/");
    }

    private String characterEncodingInt = null;

    /**
     * Returns the encoding set through {@link #setCharacterEncoding(String)},
     * or {@code null} if none was.
     *
     * @return the character encoding, or {@code null}
     */
    public String getCharacterEncodingInt() {
        return characterEncodingInt;
    }

    /**
     * Sets the encoding {@link #getCharacterEncoding()} reports.
     *
     * @param characterEncodingInt
     *            the character encoding, or {@code null}
     */
    public void setCharacterEncodingInt(String characterEncodingInt) {
        this.characterEncodingInt = characterEncodingInt;
    }

    @Override
    public void setCharacterEncoding(String env) {
        characterEncodingInt = env;
    }

    private final Map<String, String[]> parameters = new HashMap<>();

    /**
     * Returns the request parameters, live and directly modifiable. Prefer
     * {@link #setParameter(String, String...)} for a single parameter.
     *
     * @return the parameters, keyed by name
     */
    public Map<String, String[]> getParameters() {
        return parameters;
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public long getContentLengthLong() {
        return -1;
    }

    @Override
    public void login(String username, String password) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Returns {@link MockHttpEnvironment#getServerPort()}.
     */
    @Override
    public int getServerPort() {
        return MockHttpEnvironment.getServerPort();
    }

    @Override
    public String getRequestedSessionId() {
        return initiallyRequestedSessionId;
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public HttpSession getSession(boolean create) {
        boolean isValid = !(session instanceof MockHttpSession mockSession)
                || mockSession.isValid();
        if (!isValid) {
            // Mirror the servlet container contract: once the session has been
            // invalidated there is no current session, so getSession(false)
            // must return null (and getSession(true) must create a fresh one).
            // Returning the stale, invalidated session instead made code that
            // legitimately touches it after logout (e.g. Spring Security's
            // HttpSessionSecurityContextRepository.saveContext) throw
            // IllegalStateException. See issue #115.
            if (!create) {
                return null;
            }
            session = MockHttpSession.create(session.getServletContext());
        }
        return session;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String getServerName() {
        return "127.0.0.1";
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    private List<Part> partsInt = null;

    /**
     * Returns the multipart parts of this request, or {@code null} when no
     * multipart configuration has been provided.
     *
     * @return the parts, or {@code null}
     */
    public List<Part> getPartsInt() {
        return partsInt;
    }

    /**
     * Sets the multipart parts of this request. Leave {@code null} to have
     * {@link #getPart(String)} and {@link #getParts()} fail the way a servlet
     * container does without a multipart configuration.
     *
     * @param partsInt
     *            the parts, or {@code null}
     */
    public void setPartsInt(List<Part> partsInt) {
        this.partsInt = partsInt;
    }

    @Override
    public Part getPart(String name) {
        if (partsInt == null) {
            throw new IllegalStateException(
                    "Unable to process parts as no multi-part configuration has been provided");
        }
        for (Part p : partsInt) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    /**
     * Returns {@link MockHttpEnvironment#getLocalPort()}.
     */
    @Override
    public int getLocalPort() {
        return MockHttpEnvironment.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return session.getServletContext();
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getRequestId() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getProtocolRequestId() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ServletConnection getServletConnection() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<Part> getParts() {
        if (partsInt == null) {
            throw new IllegalStateException(
                    "Unable to process parts as no multi-part configuration has been provided");
        }
        return partsInt;
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getLocalName() {
        return "localhost";
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getPathTranslated() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int getIntHeader(String name) {
        String h = getHeader(name);
        return h == null ? -1 : Integer.parseInt(h);
    }

    @Override
    public String changeSessionId() {
        // Mirrors the servlet container contract: the session keeps its
        // identity and its attributes (in particular the VaadinSession), only
        // the ID changes, and there has to be a session to begin with. Apps
        // which are not backed by Spring Security implement session-fixation
        // protection by calling this after a successful login.
        HttpSession current = getSession(false);
        if (current == null) {
            throw new IllegalStateException(
                    "no session is associated with this request");
        }
        if (!(current instanceof MockHttpSession mockSession)) {
            throw new UnsupportedOperationException(
                    "changeSessionId() is only supported for MockHttpSession but got "
                            + current.getClass());
        }
        return mockSession.changeSessionId();
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException(
                "async not supported in mock environment");
    }

    @Override
    public String getRequestURI() {
        return "/";
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Decides whether the request's principal holds a given role. Defaults to
     * granting nothing.
     */
    private BiPredicate<Principal, String> isUserInRole = (p, r) -> false;

    /**
     * Returns the user-in-role checker.
     *
     * @return the checker deciding whether a principal holds a role
     */
    public BiPredicate<Principal, String> isUserInRole() {
        return isUserInRole;
    }

    /**
     * Sets the user-in-role checker.
     *
     * @param checker
     *            decides whether a principal holds a role
     */
    public void setUserInRole(BiPredicate<Principal, String> checker) {
        this.isUserInRole = checker;
    }

    /**
     * Sets the user-in-role checker using a
     * {@link java.util.function.BiPredicate}. Java-friendly alternative to
     * {@link #setUserInRole(BiPredicate)}.
     *
     * @param checker
     *            decides whether a principal holds a role
     * @since 1.1
     */
    public void roleChecker(BiPredicate<Principal, String> checker) {
        this.isUserInRole = checker;
    }

    /**
     * Set {@link #setUserInRole(BiPredicate)} to modify the outcome of this
     * function.
     */
    @Override
    public boolean isUserInRole(String role) {
        Principal p = getUserPrincipal();
        if (p == null) {
            return false;
        }
        return isUserInRole.test(p, role);
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    private Cookie[] cookiesInt = null;

    /**
     * Returns the cookies {@link #getCookies()} reports, or {@code null} if
     * none were added.
     *
     * @return the cookies, or {@code null}
     */
    public Cookie[] getCookiesInt() {
        return cookiesInt;
    }

    /**
     * Replaces the cookies {@link #getCookies()} reports.
     *
     * @param cookiesInt
     *            the cookies, or {@code null} for none
     */
    public void setCookiesInt(Cookie[] cookiesInt) {
        this.cookiesInt = cookiesInt;
    }

    /**
     * Adds a cookie to the request, as the browser would send it.
     *
     * @param cookie
     *            the cookie to add
     */
    public void addCookie(Cookie cookie) {
        if (cookiesInt == null) {
            cookiesInt = new Cookie[] { cookie };
        } else {
            Cookie[] next = Arrays.copyOf(cookiesInt, cookiesInt.length + 1);
            next[cookiesInt.length] = cookie;
            cookiesInt = next;
        }
    }

    @Override
    public Cookie[] getCookies() {
        return cookiesInt;
    }

    private Locale localeInt = Locale.US;

    /**
     * Returns the locale {@link #getLocale()} reports. Defaults to
     * {@link Locale#US}.
     *
     * @return the locale
     */
    public Locale getLocaleInt() {
        return localeInt;
    }

    /**
     * Sets the locale {@link #getLocale()} reports.
     *
     * @param localeInt
     *            the locale
     */
    public void setLocaleInt(Locale localeInt) {
        this.localeInt = localeInt;
    }

    @Override
    public Locale getLocale() {
        return localeInt;
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return attributes.keys();
    }

    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> h = headers.get(name);
        return h == null ? Collections.emptyEnumeration()
                : Collections.enumeration(h);
    }

    private Principal userPrincipalInt = null;

    /**
     * Returns the principal reported unless
     * {@link #principalProvider(java.util.function.Supplier)} has been set.
     *
     * @return the principal, or {@code null}
     */
    public Principal getUserPrincipalInt() {
        return userPrincipalInt;
    }

    /**
     * Sets the principal to report, unless
     * {@link #principalProvider(java.util.function.Supplier)} has been set.
     *
     * @param userPrincipalInt
     *            the principal, or {@code null} for an anonymous request
     */
    public void setUserPrincipalInt(Principal userPrincipalInt) {
        this.userPrincipalInt = userPrincipalInt;
    }

    /**
     * Optional provider for {@link #getUserPrincipal()}. When set, takes
     * precedence over {@link #userPrincipalInt}, allowing the principal to be
     * resolved lazily at call time (e.g. from a security context that is
     * populated after setup). Set via
     * {@link #principalProvider(java.util.function.Supplier)}.
     *
     * @since 1.1
     */
    private Supplier<Principal> userPrincipalProvider;

    /**
     * Returns the principal provider, if one has been set.
     *
     * @return the provider, or {@code null} if the principal comes from
     *         {@link #userPrincipalInt}
     * @since 1.1
     */
    public Supplier<Principal> getUserPrincipalProvider() {
        return userPrincipalProvider;
    }

    /**
     * Sets the user principal provider. Accepts a
     * {@link java.util.function.Supplier} so Java callers can pass a lambda
     * directly; Kotlin callers can do the same via SAM conversion.
     *
     * @param supplier
     *            resolves the principal at call time
     * @since 1.1
     */
    public void principalProvider(Supplier<Principal> supplier) {
        this.userPrincipalProvider = supplier;
    }

    /**
     * Returns the principal from {@link #userPrincipalProvider} if set,
     * otherwise falls back to {@link #userPrincipalInt}.
     */
    @Override
    public Principal getUserPrincipal() {
        if (userPrincipalProvider != null) {
            return userPrincipalProvider.get();
        }
        return userPrincipalInt;
    }

    @Override
    public BufferedReader getReader() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Collections.singletonList(getLocale()));
    }

    /**
     * Returns {@link MockHttpEnvironment#getAuthType()}
     */
    @Override
    public String getAuthType() {
        return MockHttpEnvironment.getAuthType();
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    private final ConcurrentHashMap<String, List<String>> headers = new ConcurrentHashMap<>();

    /**
     * Returns the request headers, live and directly modifiable.
     *
     * @return the headers, keyed by name
     */
    public ConcurrentHashMap<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public String getHeader(String headerName) {
        List<String> h = headers.get(headerName);
        return h == null ? null : h.get(0);
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return headers.keys();
    }

    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        putOrRemove(attributes, name, value);
    }

    @Override
    public String getParameter(String parameter) {
        String[] v = parameters.get(parameter);
        return v == null ? null : v[0];
    }

    /**
     * Returns {@link MockHttpEnvironment#getRemotePort()}.
     */
    @Override
    public int getRemotePort() {
        return MockHttpEnvironment.getRemotePort();
    }

    @Override
    public long getDateHeader(String name) {
        return -1;
    }

    @Override
    public String getRemoteHost() {
        return "127.0.0.1";
    }

    /**
     * Returns {@link MockHttpEnvironment#isSecure()}
     */
    @Override
    public boolean isSecure() {
        return MockHttpEnvironment.isSecure();
    }

    /**
     * Equivalent to
     * {@code parameters.put(name, Arrays.copyOf(values, values.length))}.
     *
     * @param name
     *            the attribute name
     * @param values
     *            the values of the parameter
     */
    public void setParameter(String name, String... values) {
        parameters.put(name, Arrays.copyOf(values, values.length));
    }
}
