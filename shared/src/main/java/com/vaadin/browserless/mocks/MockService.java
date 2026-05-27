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

import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

/**
 * A mocking service that performs three very important tasks:
 * * Overrides [isAtmosphereAvailable] to tell Vaadin that we don't have Atmosphere (otherwise Vaadin will crash)
 * * Provides some dummy value as a root ID via [getMainDivId] (otherwise the mocked servlet env will crash).
 * * Provides a [MockVaadinSession].
 * The class is intentionally opened, to be extensible in user's library.
 *
 * To register your custom `MockService` instance, override [MockVaadinServlet.createServletService].
 */
public class MockService extends VaadinServletService {

    public final UIFactory uiFactory;

    public MockService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration) {
        this(servlet, deploymentConfiguration, MockedUI::new);
    }

    public MockService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration, UIFactory uiFactory) {
        super(servlet, deploymentConfiguration);
        this.uiFactory = uiFactory;
    }

    // need to have this override. Setting `VaadinService.atmosphereAvailable` to false via
    // reflection after the servlet has been initialized is too late, since Atmo is initialized
    // in VaadinService.init().
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

    @Override
    public Instantiator getInstantiator() {
        return MockInstantiator.create(super.getInstantiator());
    }
}
