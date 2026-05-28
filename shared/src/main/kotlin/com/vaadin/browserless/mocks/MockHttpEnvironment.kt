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

import java.util.Collections
import java.util.Enumeration
import jakarta.servlet.ServletConfig
import jakarta.servlet.ServletContext


open class MockServletConfig(val context: ServletContext) : ServletConfig {

    /**
     * Per-servlet init parameters.
     */
    var servletInitParams: MutableMap<String, String> = mutableMapOf()

    override fun getInitParameter(name: String): String? = servletInitParams[name]

    override fun getInitParameterNames(): Enumeration<String> = Collections.enumeration(servletInitParams.keys)

    override fun getServletName(): String = "Vaadin Servlet"

    override fun getServletContext(): ServletContext = context
}

internal fun <K, V> MutableMap<K, V>.putOrRemove(key: K, value: V?) {
    if (value == null) remove(key) else set(key, value)
}

object MockHttpEnvironment {
    /**
     * [MockRequest.getLocalPort]
     */
    var localPort: Int = 8080

    /**
     * [MockRequest.getServerPort]
     */
    var serverPort: Int = 8080

    /**
     * [MockRequest.getRemotePort]
     */
    var remotePort: Int = 8080

    /**
     * [MockRequest.getAuthType]
     */
    var authType: String? = null

    /**
     * [MockRequest.isSecure]
     */
    var isSecure: Boolean = false
}
