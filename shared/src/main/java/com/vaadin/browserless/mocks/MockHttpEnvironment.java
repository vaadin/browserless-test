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

/**
 * The connection details every {@link MockRequest} reports.
 * <p>
 * There is no HTTP connection behind a browserless test, so the values a
 * container would derive from one are read from here instead. Set one to put
 * the application under test into the situation you want to assert on —
 * {@code setSecure(true)} to exercise a code path that requires HTTPS, for
 * example.
 * <p>
 * The values are global and are <em>not</em> reset between tests, so a test
 * that changes one has to restore it afterwards, or later tests will see it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class MockHttpEnvironment {

    private MockHttpEnvironment() {
    }

    private static int localPort = 8080;

    private static int serverPort = 8080;

    private static int remotePort = 8080;

    private static String authType = null;

    private static boolean isSecure = false;

    /**
     * Returns the port {@link MockRequest#getLocalPort()} reports, i.e. the one
     * the request was received on. Defaults to {@code 8080}.
     *
     * @return the local port
     */
    public static int getLocalPort() {
        return localPort;
    }

    /**
     * Sets the port {@link MockRequest#getLocalPort()} reports.
     *
     * @param localPort
     *            the local port
     */
    public static void setLocalPort(int localPort) {
        MockHttpEnvironment.localPort = localPort;
    }

    /**
     * Returns the port {@link MockRequest#getServerPort()} reports, i.e. the
     * one the request was addressed to. Defaults to {@code 8080}.
     *
     * @return the server port
     */
    public static int getServerPort() {
        return serverPort;
    }

    /**
     * Sets the port {@link MockRequest#getServerPort()} reports.
     *
     * @param serverPort
     *            the server port
     */
    public static void setServerPort(int serverPort) {
        MockHttpEnvironment.serverPort = serverPort;
    }

    /**
     * Returns the port {@link MockRequest#getRemotePort()} reports, i.e. the
     * client's. Defaults to {@code 8080}.
     *
     * @return the remote port
     */
    public static int getRemotePort() {
        return remotePort;
    }

    /**
     * Sets the port {@link MockRequest#getRemotePort()} reports.
     *
     * @param remotePort
     *            the remote port
     */
    public static void setRemotePort(int remotePort) {
        MockHttpEnvironment.remotePort = remotePort;
    }

    /**
     * Returns the authentication scheme {@link MockRequest#getAuthType()}
     * reports, such as
     * {@link jakarta.servlet.http.HttpServletRequest#FORM_AUTH}.
     * <p>
     * Defaults to {@code null}, i.e. the request is not authenticated — which
     * is independent of the principal the request carries, set through
     * {@link MockRequest#setUserPrincipalInt(java.security.Principal)}.
     *
     * @return the authentication scheme, or {@code null}
     */
    public static String getAuthType() {
        return authType;
    }

    /**
     * Sets the authentication scheme {@link MockRequest#getAuthType()} reports.
     *
     * @param authType
     *            the authentication scheme, or {@code null} for an
     *            unauthenticated request
     */
    public static void setAuthType(String authType) {
        MockHttpEnvironment.authType = authType;
    }

    /**
     * Tells whether {@link MockRequest#isSecure()} reports the request as
     * having arrived over HTTPS. Defaults to {@code false}; nothing else about
     * the request changes with it.
     *
     * @return {@code true} if requests are reported as secure
     */
    public static boolean isSecure() {
        return isSecure;
    }

    /**
     * Sets whether {@link MockRequest#isSecure()} reports the request as having
     * arrived over HTTPS.
     *
     * @param isSecure
     *            {@code true} to report requests as secure
     */
    public static void setSecure(boolean isSecure) {
        MockHttpEnvironment.isSecure = isSecure;
    }
}
