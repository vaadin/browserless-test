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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.browserless.mocks.MockUtils.putOrRemove;

/**
 * A standalone {@link ServletContext}, backed by the build output of the module
 * under test.
 * <p>
 * It models the three things Vaadin asks a context for: init parameters and
 * attributes, which are held in memory and are where the {@code Lookup} and the
 * {@code ApplicationConfiguration} end up; resource lookup, which serves the
 * files a deployed war would have; and logging, which goes to SLF4J.
 * <p>
 * Resources are resolved the way a war layout would resolve them, in this
 * order: the {@link #realPathRoots} of the current module, then
 * {@code META-INF/resources} on the classpath (which is where a jar contributes
 * static files), then, for a {@code /VAADIN/} path, the classpath root. A
 * {@code /VAADIN/../} path is normalized and refused, as a container refuses to
 * serve outside the exposed directory.
 * <p>
 * Everything a test has no use for — servlet, filter and listener registration,
 * request dispatch, session tracking configuration — throws
 * {@link UnsupportedOperationException} rather than pretending to succeed, so a
 * test that needs one fails loudly instead of running against a fiction. The
 * reported servlet API version is 3.0, which is what Vaadin's own version
 * checks accept.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockContext implements ServletContext, Serializable {

    /**
     * Creates an empty context.
     */
    public MockContext() {
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(MockContext.class);

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        // for example @HtmlImport("frontend://reviews-list.html") will expect
        // the resource to be present in the war file,
        // which is typically located in $CWD/src/main/webapp/frontend, so let's
        // search for that first
        String realPath = getRealPath(path);
        if (realPath != null) {
            return new File(realPath).toURI().toURL();
        }

        // nope, fall back to class loading.
        //
        // for example
        // @HtmlImport("frontend://bower_components/vaadin-button/src/vaadin-button.html")
        // will try to look up
        // the following resources:
        //
        // 1. /frontend/bower_components/vaadin-button/src/vaadin-button.html
        // 2. /webjars/vaadin-button/src/vaadin-button.html
        //
        // we need to match the latter one to a resource on classpath

        if (path.startsWith("/")) {
            URL resource = Thread.currentThread().getContextClassLoader()
                    .getResource("META-INF/resources" + path);
            if (resource != null) {
                return resource;
            }
        }

        if (path.startsWith("/VAADIN/")) {
            // Vaadin 8 exposed directory
            String p = path;
            if (p.contains("..")) {
                // to be able to resolve
                // ThemeResource("../othertheme/img/foo.png") which work from
                // the browser.
                p = Paths.get(p).normalize().toString();
                // convert Windows path separators to Linux ones, so that the
                // follow-up code works
                p = p.replace('\\', '/');
            }
            // reject to serve "/VAADIN/../" resources
            if (p.startsWith("/VAADIN/")) {
                String stripped = p.startsWith("/") ? p.substring(1) : p;
                URL resource = Thread.currentThread().getContextClassLoader()
                        .getResource(stripped);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return null;
    }

    @Override
    public void addListener(String className) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return attributes.keys();
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public void log(String msg) {
        LOG.error(msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return initParameters.putIfAbsent(name, value) == null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        try {
            URL url = getResource(path);
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        Set<SessionTrackingMode> set = new HashSet<>();
        set.add(SessionTrackingMode.COOKIE);
        set.add(SessionTrackingMode.URL);
        return set;
    }

    @Override
    public String getMimeType(String file) {
        String mime = URLConnection.guessContentTypeFromName(file);
        return mime != null ? mime : "application/octet-stream";
    }

    @Override
    public void declareRoles(String... roleNames) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The directories {@link #getRealPath} resolves paths against.
     */
    private List<String> realPathRoots = Arrays
            .asList("src/main/webapp/frontend", "src/main/webapp");

    /**
     * Returns the module-relative folders {@link #getRealPath(String)} resolves
     * a path against, in order; the first one holding the file wins.
     * <p>
     * These stand in for the war root of a deployed application. Defaults to
     * {@code src/main/webapp/frontend} and {@code src/main/webapp}.
     *
     * @return the folders searched for a real path
     */
    public List<String> getRealPathRoots() {
        return realPathRoots;
    }

    /**
     * Replaces the folders {@link #getRealPath(String)} resolves against. Point
     * them elsewhere for a project that keeps its static files in another
     * place.
     *
     * @param realPathRoots
     *            the module-relative folders to search, in order
     */
    public void setRealPathRoots(List<String> realPathRoots) {
        this.realPathRoots = realPathRoots;
    }

    @Override
    public String getRealPath(String path) {
        for (String realPathRoot : realPathRoots) {
            try {
                File realPath = new File(moduleDir(), realPathRoot + "/" + path)
                        .getCanonicalFile().getAbsoluteFile();
                if (realPath.getAbsolutePath()
                        .startsWith(new File(realPathRoot).getAbsolutePath())
                        && realPath.exists()) {
                    return realPath.getAbsolutePath();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * The context init parameters.
     */
    private final Map<String, String> initParameters = new HashMap<>();

    /**
     * Returns the context init parameters, live and directly modifiable.
     * <p>
     * Writing here is not the same as calling
     * {@link #setInitParameter(String, String)}, which follows the servlet
     * contract and refuses to overwrite a parameter that is already set. Most
     * parameters are read once, when {@code ApplicationConfiguration} is first
     * created and cached, so a value that has to be seen must be in place
     * before anything else touches the context.
     *
     * @return the init parameters
     */
    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getServletContextName() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            String className) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Filter filter) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Class<? extends Filter> filterClass) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getVirtualServerName() {
        return "mock/localhost"; // Tomcat returns "Catalina/localhost"
    }

    /**
     * The session timeout, in minutes.
     */
    private int sessionTimeout = 30;

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * The character encoding requests default to.
     */
    private String requestCharacterEncoding = null;

    @Override
    public String getRequestCharacterEncoding() {
        return requestCharacterEncoding;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        this.requestCharacterEncoding = encoding;
    }

    /**
     * The character encoding responses default to.
     */
    private String responseCharacterEncoding = null;

    @Override
    public String getResponseCharacterEncoding() {
        return responseCharacterEncoding;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        this.responseCharacterEncoding = encoding;
    }

    @Override
    public ServletContext getContext(String uripath) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The context attributes.
     */
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
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            String className) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet servlet) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName,
            String jspFile) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return new HashMap<>();
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return new HashSet<>();
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public String getServerInfo() {
        return "Mock";
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        Set<SessionTrackingMode> set = new HashSet<>();
        set.add(SessionTrackingMode.COOKIE);
        set.add(SessionTrackingMode.URL);
        return set;
    }

    private static File moduleDir() {
        File dir = new File("").getAbsoluteFile();
        // Workaround for https://youtrack.jetbrains.com/issue/IDEA-188466
        // When using $MODULE_DIR$, IDEA will set CWD to, say,
        // ui-testing/.idea/modules/ui-testing-module
        // We need to revert that back to ui-testing/ui-testing-module
        if (dir.getAbsolutePath().contains("/.idea/modules")) {
            dir = new File(dir.getAbsolutePath().replace("/.idea/modules", ""));
        }
        return dir;
    }
}
