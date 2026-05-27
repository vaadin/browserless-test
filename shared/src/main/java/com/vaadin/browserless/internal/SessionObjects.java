/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.util.Objects;

import com.vaadin.browserless.mocks.MockHttpSession;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Holds the objects created during session initialization, before they are
 * installed as thread-locals. Used by {@link MockVaadin#createSessionObjects}
 * to allow callers (e.g. multi-user context) to manage thread-locals
 * themselves.
 */
public final class SessionObjects {

    public final VaadinSession session;
    public final VaadinRequest request;
    public final VaadinResponse response;
    public final MockHttpSession httpSession;

    public SessionObjects(VaadinSession session, VaadinRequest request,
            VaadinResponse response, MockHttpSession httpSession) {
        this.session = session;
        this.request = request;
        this.response = response;
        this.httpSession = httpSession;
    }

    public VaadinSession getSession() {
        return session;
    }

    public VaadinRequest getRequest() {
        return request;
    }

    public VaadinResponse getResponse() {
        return response;
    }

    public MockHttpSession getHttpSession() {
        return httpSession;
    }

    public SessionObjects copy(VaadinSession session, VaadinRequest request,
            VaadinResponse response, MockHttpSession httpSession) {
        return new SessionObjects(session, request, response, httpSession);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionObjects)) {
            return false;
        }
        SessionObjects other = (SessionObjects) o;
        return Objects.equals(session, other.session)
                && Objects.equals(request, other.request)
                && Objects.equals(response, other.response)
                && Objects.equals(httpSession, other.httpSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, request, response, httpSession);
    }

    @Override
    public String toString() {
        return "SessionObjects(session=" + session + ", request=" + request
                + ", response=" + response + ", httpSession=" + httpSession + ")";
    }
}
