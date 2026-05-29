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
package com.vaadin.browserless.mocks

import java.io.Serializable
import com.vaadin.flow.component.UI
import com.vaadin.flow.di.Instantiator
import com.vaadin.flow.function.DeploymentConfiguration
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinServlet
import com.vaadin.flow.server.VaadinServletService
import com.vaadin.flow.server.VaadinSession

import com.vaadin.browserless.internal.UIFactory

/**
 * A mocking service that performs three very important tasks:
 * * Overrides [isAtmosphereAvailable] to tell Vaadin that we don't have Atmosphere (otherwise Vaadin will crash)
 * * Provides some dummy value as a root ID via [getMainDivId] (otherwise the mocked servlet env will crash).
 * * Provides a [MockVaadinSession].
 * The class is intentionally opened, to be extensible in user's library.
 *
 * To register your custom `MockService` instance, override [MockVaadinServlet.createServletService].
 */
open class MockService(servlet: VaadinServlet,
                              deploymentConfiguration: DeploymentConfiguration,
                              val uiFactory: UIFactory = UIFactory { MockedUI() }
) : VaadinServletService(servlet, deploymentConfiguration) {
    // need to have this override. Setting `VaadinService.atmosphereAvailable` to false via
    // reflection after the servlet has been initialized is too late, since Atmo is initialized
    // in VaadinService.init().
    override fun isAtmosphereAvailable(): Boolean = false
    override fun getMainDivId(session: VaadinSession?, request: VaadinRequest?): String = "ROOT-1"
    override fun createVaadinSession(request: VaadinRequest): VaadinSession = MockVaadinSession(this, uiFactory)
    override fun getInstantiator(): Instantiator = MockInstantiator.create(super.getInstantiator())
}
