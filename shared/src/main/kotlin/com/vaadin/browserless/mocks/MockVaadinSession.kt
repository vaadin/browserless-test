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

import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinSession
import com.vaadin.browserless.internal.MockVaadin
import com.vaadin.browserless.internal.UIFactory

/**
 * A Vaadin Session with one important difference:
 *
 * * Creates a new session when this one is closed. This is used to simulate a logout
 *   which closes the session - we need to have a new fresh session to be able to continue testing.
 *   In order to do that, simply override [close], call `super.close()` then call
 *   [MockVaadin.afterSessionClose].
 */
open class MockVaadinSession(service: VaadinService,
                             val uiFactory: UIFactory
) : VaadinSession(service) {
    override fun close() {
        super.close()
        MockVaadin.afterSessionClose(this, uiFactory)
    }
}
