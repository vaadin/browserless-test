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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class SessionObjects {

    /**
     * The session.
     */
    private final VaadinSession session;

    /**
     * The request bound to the session.
     */
    private final VaadinRequest request;

    /**
     * The response bound to the session.
     */
    private final VaadinResponse response;

    /**
     * The HTTP session the Vaadin session is stored in.
     */
    private final MockHttpSession httpSession;

    /**
     * Groups the four objects that make up one mocked user's session.
     *
     * @param session
     *            the session
     * @param request
     *            the request bound to the session
     * @param response
     *            the response bound to the session
     * @param httpSession
     *            the HTTP session the Vaadin session is stored in
     */
    public SessionObjects(VaadinSession session, VaadinRequest request,
            VaadinResponse response, MockHttpSession httpSession) {
        this.session = session;
        this.request = request;
        this.response = response;
        this.httpSession = httpSession;
    }

    /**
     * Returns the session.
     *
     * @return the session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Returns the request bound to the session.
     *
     * @return the request
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Returns the response bound to the session.
     *
     * @return the response
     */
    public VaadinResponse getResponse() {
        return response;
    }

    /**
     * Returns the HTTP session the Vaadin session is stored in.
     *
     * @return the HTTP session
     */
    public MockHttpSession getHttpSession() {
        return httpSession;
    }

    /**
     * Returns a copy of this group with the given objects in place of the
     * current ones.
     *
     * @param session
     *            the session
     * @param request
     *            the request bound to the session
     * @param response
     *            the response bound to the session
     * @param httpSession
     *            the HTTP session the Vaadin session is stored in
     * @return the new group
     */
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
                + ", response=" + response + ", httpSession=" + httpSession
                + ")";
    }
}
