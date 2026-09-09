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
import java.lang.reflect.InvocationTargetException


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

    /**
     * Renders the route server side, as a test has no client router to hand
     * the navigation to.
     *
     * This replaces [UI.navigate] rather than extending it — `super` is never
     * called — so nothing the real [UI.navigate] does with a location reaches a
     * browserless test. Any change to how Flow turns a location string into a
     * [Location] has to be mirrored in [toLocation] to be observable here.
     */
    override fun navigate(locationString: String, queryParameters: QueryParameters) {

        try {
            UI::class.java.getDeclaredMethod("renderViewForRoute", Location::class.java, NavigationTrigger::class.java)
                    .apply { isAccessible = true }
                    .invoke(this, toLocation(locationString, queryParameters), NavigationTrigger.UI_NAVIGATE)
        } catch (ex: InvocationTargetException) {
            // Reflection is an implementation detail of the mocked routing:
            // report what the router threw, not an InvocationTargetException
            // whose own message says nothing.
            throw ex.targetException
        }
        return
    }

    /**
     * Builds the [Location] to render, the way [UI.navigate] does in a running
     * application, but saying so when the location is one that application
     * would not navigate to either.
     *
     * [UI.navigate] takes the path and the query separately, so a location
     * such as `orders/1?tab=history` leaves the query string inside a path
     * segment: a running application silently binds it into a route parameter
     * (or fails to match the route at all), and under the assertions a test
     * runs with it later trips `Base path can not contain query separator=?`,
     * a message that names neither the location nor the API that was called.
     * Report it here instead, rather than mocking the location into working
     * and letting a test pass for navigation that is broken in production.
     */
    private fun toLocation(locationString: String, queryParameters: QueryParameters): Location {
        require(!locationString.contains(QUERY_SEPARATOR) && !locationString.contains(FRAGMENT_SEPARATOR)) {
            "Location '$locationString' must be a path: UI.navigate takes the query string as QueryParameters, " +
                    "and ignores a fragment. Pass the query as QueryParameters, or navigate with the browserless " +
                    "navigate(location, viewType), which parses the location the way the address bar spells it."
        }
        return Location(locationString, queryParameters)
    }

    private fun roundTrip() {
        internals.stateTree.collectChanges { }
        internals.stateTree.runExecutionsBeforeClientResponse()
    }

    private companion object {
        /** [Location] keeps its own copies of these package private. */
        private const val QUERY_SEPARATOR = "?"
        private const val FRAGMENT_SEPARATOR = "#"
    }
}
