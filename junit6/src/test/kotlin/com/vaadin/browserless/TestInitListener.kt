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
package com.vaadin.browserless

import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener

/**
 * This class is picked up automatically by Vaadin (since it's registered via META-INF/services). We then test elsewhere
 * that MockVaadin-mocked env indeed picked up this init listener and executed it.
 */
class TestInitListener : VaadinServiceInitListener {
    override fun serviceInit(event: ServiceInitEvent) {
        serviceInitCalled = true
        event.source.addUIInitListener { e ->
            uiInitCalled = true
            e.ui.addBeforeEnterListener { uiBeforeEnterCalled = true }
        }
    }

    companion object {
        var serviceInitCalled: Boolean = false
        var uiInitCalled = false
        var uiBeforeEnterCalled = false
        fun clearInitFlags() {
            serviceInitCalled = false
            uiInitCalled = false
            uiBeforeEnterCalled = false
        }
    }
}
