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

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.ModalityMode
import com.vaadin.flow.component.UI
import com.vaadin.flow.shared.Registration
import java.util.concurrent.atomic.AtomicReference
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.router.Location
import com.vaadin.browserless.internal.simulateClosedEvent


/**
 * A simple no-op UI used by default by [com.vaadin.browserless.MockVaadin.setup].
 * The class is open, in order to be extensible in user's library
 */
open class MockedUI : UI() {

    override fun setChildComponentModal(childComponent: Component?, mode: ModalityMode) {
        super.setChildComponentModal(childComponent, mode)
        if (mode != ModalityMode.MODELESS) {
            val registrationCombination: AtomicReference<Registration?> = AtomicReference<Registration?>()
            registrationCombination.set(childComponent?.addDetachListener(ComponentEventListener {
                roundTrip()
                registrationCombination.getAndSet(null)?.remove()
            }))
        }
        roundTrip();
    }

    override fun addToModalComponent(component: Component?) {
        super.addToModalComponent(component)
        component?.simulateClosedEvent()
    }

    override fun navigate(locationString: String, queryParameters: QueryParameters) {

        // server-side routing only for tests as there is no client to handle routing.
        UI::class.java.getDeclaredMethod("renderViewForRoute", Location::class.java, NavigationTrigger::class.java)
                .apply { isAccessible = true }
                .invoke(this, Location(locationString, queryParameters), NavigationTrigger.UI_NAVIGATE)
        return
    }

    private fun roundTrip() {
        internals.stateTree.collectChanges { }
        internals.stateTree.runExecutionsBeforeClientResponse()
    }
}
