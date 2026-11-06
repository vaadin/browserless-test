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
 * installed as thread-locals. Used by
 * {@link MockVaadin#createSessionObjects(com.vaadin.flow.server.VaadinServletService)}
 * to allow callers (e.g. multi-user context) to manage thread-locals
 * themselves.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class SessionObjects {
    private final VaadinSession session;
    private final VaadinRequest request;
    private final VaadinResponse response;
    private final MockHttpSession httpSession;

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

    public VaadinSession component1() {
        return session;
    }

    public VaadinRequest component2() {
        return request;
    }

    public VaadinResponse component3() {
        return response;
    }

    public MockHttpSession component4() {
        return httpSession;
    }

    public SessionObjects copy(VaadinSession session, VaadinRequest request,
            VaadinResponse response, MockHttpSession httpSession) {
        return new SessionObjects(session, request, response, httpSession);
    }

    @Override
    public String toString() {
        return "SessionObjects(session=" + session + ", request=" + request
                + ", response=" + response + ", httpSession=" + httpSession
                + ")";
    }

    @Override
    public int hashCode() {
        int result = session.hashCode();
        result = result * 31 + request.hashCode();
        result = result * 31 + response.hashCode();
        result = result * 31 + httpSession.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SessionObjects)) {
            return false;
        }
        SessionObjects that = (SessionObjects) other;
        return Objects.equals(session, that.session)
                && Objects.equals(request, that.request)
                && Objects.equals(response, that.response)
                && Objects.equals(httpSession, that.httpSession);
    }
}
