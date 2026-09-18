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

import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

/**
 * The {@link VaadinServletService} of the mocked environment.
 * <p>
 * It stands in for a deployed service in the three places a browserless test
 * has nothing to offer:
 * <ul>
 * <li>{@link #isAtmosphereAvailable()} reports that Atmosphere is absent, which
 * it is. Vaadin initializes push during {@code VaadinService.init()} and
 * crashes otherwise.</li>
 * <li>{@link #getMainDivId(VaadinSession, VaadinRequest)} returns a fixed root
 * id, since there is no bootstrap page to read one from.</li>
 * <li>{@link #createVaadinSession(VaadinRequest)} returns a
 * {@link MockVaadinSession}, which recreates the session after it is closed,
 * the way a servlet container does when the browser reloads after a
 * logout.</li>
 * </ul>
 * Object creation is <em>not</em> mocked. The service uses whatever
 * {@link com.vaadin.flow.di.Instantiator} the environment's
 * {@link com.vaadin.flow.di.Lookup} provides, which is how views and beans
 * still come from the real container.
 * <p>
 * The class is not final, so a custom service can be registered by overriding
 * {@link MockVaadinServlet#createServletService(DeploymentConfiguration)}, but
 * the shape of what it overrides is not part of the published API.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockService extends VaadinServletService {

    private final UIFactory uiFactory;

    /**
     * Returns the factory producing the {@link com.vaadin.flow.component.UI} of
     * every session this service creates.
     *
     * @return the UI factory
     */
    public UIFactory getUiFactory() {
        return uiFactory;
    }

    /**
     * Creates a service whose sessions produce {@link MockedUI} instances.
     *
     * @param servlet
     *            the servlet owning this service
     * @param deploymentConfiguration
     *            the configuration to run with
     */
    public MockService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        this(servlet, deploymentConfiguration, MockedUI::new);
    }

    /**
     * Creates a service whose sessions produce UIs from the given factory.
     *
     * @param servlet
     *            the servlet owning this service
     * @param deploymentConfiguration
     *            the configuration to run with
     * @param uiFactory
     *            produces a fresh UI per session; it must return a new instance
     *            every time, or a test inherits the previous test's state
     */
    public MockService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration,
            UIFactory uiFactory) {
        super(servlet, deploymentConfiguration);
        this.uiFactory = uiFactory;
    }

    // Has to be an override: clearing VaadinService.atmosphereAvailable
    // reflectively once the servlet is initialized is too late, since
    // Atmosphere is set up in VaadinService.init().
    @Override
    protected boolean isAtmosphereAvailable() {
        return false;
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request) {
        return "ROOT-1";
    }

    @Override
    protected VaadinSession createVaadinSession(VaadinRequest request) {
        return new MockVaadinSession(this, uiFactory);
    }
}
