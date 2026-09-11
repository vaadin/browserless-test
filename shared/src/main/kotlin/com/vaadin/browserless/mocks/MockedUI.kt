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
     * browserless test. How a location string becomes a [Location], and how a
     * fragment-only location is treated, therefore has to be mirrored here to
     * be observable in a test.
     */
    override fun navigate(locationString: String, queryParameters: QueryParameters) {
        val location = toLocation(locationString, queryParameters)

        // A location that consists only of a fragment does not identify a
        // route: a running application leaves it to the client router rather
        // than resolving the "" route and replacing the current view. A test
        // has no client router, so there is simply nothing to render.
        if (location.path.isEmpty() && location.queryParameters.parameters.isEmpty() &&
                locationString.contains(FRAGMENT_SEPARATOR)) {
            return
        }

        try {
            UI::class.java.getDeclaredMethod("renderViewForRoute", Location::class.java, NavigationTrigger::class.java)
                    .apply { isAccessible = true }
                    .invoke(this, location, NavigationTrigger.UI_NAVIGATE)
        } catch (ex: InvocationTargetException) {
            // Reflection is an implementation detail of the mocked routing:
            // report what the router threw, not an InvocationTargetException
            // whose own message says nothing.
            throw ex.targetException
        }
        return
    }

    /**
     * Builds the [Location] to render the way [UI.navigate] does in a running
     * application.
     *
     * Without separate query parameters the location string is the only source
     * of them, so it is free to carry a query string and a fragment. Given
     * both, its own query string or fragment would be lost, which is a mistake
     * worth reporting rather than dropping silently.
     *
     * This mirrors `UI.navigate(String, QueryParameters)` as of
     * https://github.com/vaadin/flow/pull/25591, backported to 25.3 in
     * https://github.com/vaadin/flow/pull/25618. Compare against that method
     * rather than against a locally resolved snapshot, which may predate it.
     */
    private fun toLocation(locationString: String, queryParameters: QueryParameters): Location {
        val separateParameters = queryParameters.parameters.isNotEmpty()
        require(!separateParameters ||
                (!locationString.contains(QUERY_SEPARATOR) && !locationString.contains(FRAGMENT_SEPARATOR))) {
            "The location '$locationString' must be a plain path when query parameters are given separately, " +
                    "since its own query string or fragment would be lost. Pass the whole URL to " +
                    "navigate(String) instead."
        }
        return if (separateParameters) Location(locationString, queryParameters) else Location(locationString)
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
