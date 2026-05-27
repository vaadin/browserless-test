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

public final class MockHttpEnvironment {

    private MockHttpEnvironment() {
    }

    /**
     * [MockRequest.getLocalPort]
     */
    public static int localPort = 8080;

    /**
     * [MockRequest.getServerPort]
     */
    public static int serverPort = 8080;

    /**
     * [MockRequest.getRemotePort]
     */
    public static int remotePort = 8080;

    /**
     * [MockRequest.getAuthType]
     */
    public static String authType = null;

    /**
     * [MockRequest.isSecure]
     */
    public static boolean isSecure = false;
}
