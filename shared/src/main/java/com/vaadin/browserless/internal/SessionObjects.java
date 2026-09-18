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
                + ", response=" + response + ", httpSession=" + httpSession
                + ")";
    }
}
