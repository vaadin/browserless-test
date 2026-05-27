/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.mocks;

import static com.vaadin.browserless.mocks.MockUtils.putOrRemove;

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

public class MockRequest implements HttpServletRequest {

    private HttpSession session;

    public MockRequest(HttpSession session) {
        this.session = session;
        headers.put("user-agent", Collections.singletonList("IntelliJ IDEA/182.4892.20"));
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
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
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

    public String characterEncodingInt = null;

    @Override
    public void setCharacterEncoding(String env) {
        characterEncodingInt = env;
    }

    public final Map<String, String[]> parameters = new HashMap<>();

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
     * Returns [MockHttpEnvironment.serverPort].
     */
    @Override
    public int getServerPort() {
        return MockHttpEnvironment.serverPort;
    }

    @Override
    public String getRequestedSessionId() {
        return session.getId();
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public HttpSession getSession(boolean create) {
        boolean isValid = !(session instanceof MockHttpSession) || ((MockHttpSession) session).isValid();
        if (create && !isValid) {
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

    public List<Part> partsInt = null;

    @Override
    public Part getPart(String name) {
        if (partsInt == null) {
            throw new IllegalStateException("Unable to process parts as no multi-part configuration has been provided");
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
     * Returns [MockHttpEnvironment.localPort].
     */
    @Override
    public int getLocalPort() {
        return MockHttpEnvironment.localPort;
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
            throw new IllegalStateException("Unable to process parts as no multi-part configuration has been provided");
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
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("async not supported in mock environment");
    }

    @Override
    public String getRequestURI() {
        return "/";
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("not implemented");
    }

    public BiPredicate<Principal, String> isUserInRole = (p, r) -> false;

    /**
     * Mirrors the JVM setter Kotlin would generate for `var isUserInRole`, so
     * existing Java callers like `request.setUserInRole(...)` keep working.
     */
    public void setUserInRole(BiPredicate<Principal, String> checker) {
        this.isUserInRole = checker;
    }

    /**
     * Sets the user-in-role checker using a [java.util.function.BiPredicate].
     * Java-friendly alternative to setting [isUserInRole] directly.
     */
    public void roleChecker(BiPredicate<Principal, String> checker) {
        this.isUserInRole = checker;
    }

    /**
     * Set [isUserInRole] to modify the outcome of this function.
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

    public Cookie[] cookiesInt = null;

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

    public Locale localeInt = Locale.US;

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
        return h == null ? Collections.emptyEnumeration() : Collections.enumeration(h);
    }

    public Principal userPrincipalInt = null;

    /**
     * Optional provider for [getUserPrincipal]. When set, takes precedence over
     * [userPrincipalInt], allowing the principal to be resolved lazily at
     * call time (e.g. from a security context that is populated after setup).
     * Set via [principalProvider].
     */
    private Supplier<Principal> userPrincipalProvider;

    public Supplier<Principal> getUserPrincipalProvider() {
        return userPrincipalProvider;
    }

    /**
     * Sets the user principal provider. Accepts a [java.util.function.Supplier]
     * so Java callers can pass a lambda directly; Kotlin callers can do the
     * same via SAM conversion.
     */
    public void principalProvider(Supplier<Principal> supplier) {
        this.userPrincipalProvider = supplier;
    }

    /**
     * Returns the principal from [userPrincipalProvider] if set, otherwise
     * falls back to [userPrincipalInt].
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
     * Returns [MockHttpEnvironment.authType]
     */
    @Override
    public String getAuthType() {
        return MockHttpEnvironment.authType;
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

    public final ConcurrentHashMap<String, List<String>> headers = new ConcurrentHashMap<>();

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
     * Returns [MockHttpEnvironment.remotePort].
     */
    @Override
    public int getRemotePort() {
        return MockHttpEnvironment.remotePort;
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
     * Returns [MockHttpEnvironment.isSecure]
     */
    @Override
    public boolean isSecure() {
        return MockHttpEnvironment.isSecure;
    }

    public void setParameter(String name, String... values) {
        parameters.put(name, Arrays.copyOf(values, values.length));
    }
}
