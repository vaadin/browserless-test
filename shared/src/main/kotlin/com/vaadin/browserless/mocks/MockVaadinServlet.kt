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

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import com.vaadin.flow.function.DeploymentConfiguration
import com.vaadin.flow.server.*
import com.vaadin.browserless.internal.Routes
import com.vaadin.browserless.internal.UIFactory

/**
 * Makes sure that [routes] are properly registered, and that [MockService]
 * is used instead of vanilla [VaadinServletService].
 *
 * To use a custom servlet instead of this one, just pass it to [MockVaadin.setup].
 */
open class MockVaadinServlet @JvmOverloads constructor(
        val routes: Routes = Routes(),
        val uiFactory: UIFactory = UIFactory{ MockedUI() }
) : VaadinServlet() {

    override fun createDeploymentConfiguration(): DeploymentConfiguration {
        MockVaadinHelper.mockFlowBuildInfo(this)
        return super.createDeploymentConfiguration()
    }

    override fun createServletService(deploymentConfiguration: DeploymentConfiguration): VaadinServletService {
        val service: VaadinServletService = MockService(this, deploymentConfiguration, uiFactory)
        service.init()
        routes.register(service.context as VaadinServletContext)
        return service
    }
}

private val _WebBrowser_constructor: Constructor<WebBrowser> =
    WebBrowser::class.java.getDeclaredConstructor(VaadinRequest::class.java).apply {
        isAccessible = true
    }

internal fun WebBrowser(request: VaadinRequest): WebBrowser =
    _WebBrowser_constructor.newInstance(request)

private val _VaadinService_createVaadinSession: Method =
    VaadinService::class.java.getDeclaredMethod("createVaadinSession", VaadinRequest::class.java).apply {
        isAccessible = true
    }

internal fun VaadinService._createVaadinSession(request: VaadinRequest): VaadinSession {
    return _VaadinService_createVaadinSession.invoke(this, request) as VaadinSession
}
