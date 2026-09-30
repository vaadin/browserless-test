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

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * A {@link VaadinSession} that is replaced by a fresh one when it is closed.
 * <p>
 * Closing the session is how an application logs a user out: it then tells the
 * browser to reload, and the servlet container hands the reloaded page a brand
 * new session. There is no browser here, so {@link #close()} does that itself
 * and the test can go on to assert that a login view is shown.
 * <p>
 * A subclass that overrides {@link #close()} has to keep both halves: call
 * {@code super.close()}, then
 * {@link MockVaadin#afterSessionClose(VaadinSession, UIFactory)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockVaadinSession extends VaadinSession {

    /**
     * Produces the UI instances this session hands out.
     */
    private final UIFactory uiFactory;

    /**
     * Returns the factory producing the {@link com.vaadin.flow.component.UI} of
     * the session that replaces this one once it is closed.
     *
     * @return the UI factory
     */
    public UIFactory getUiFactory() {
        return uiFactory;
    }

    /**
     * Creates a session for the given service.
     *
     * @param service
     *            the service owning this session
     * @param uiFactory
     *            produces the UI of the replacement session; it must return a
     *            fresh instance, not one already attached to a session
     */
    public MockVaadinSession(VaadinService service, UIFactory uiFactory) {
        super(service);
        this.uiFactory = uiFactory;
    }

    @Override
    public void close() {
        super.close();
        MockVaadin.afterSessionClose(this, uiFactory);
    }
}
