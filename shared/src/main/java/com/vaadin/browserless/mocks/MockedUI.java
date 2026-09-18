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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.browserless.internal.ComponentUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.shared.Registration;

/**
 * The {@link UI} the mocked environment creates by default.
 * <p>
 * It stands in for the browser in the two places the browser would normally
 * act: it renders a navigation server-side, since there is no client router to
 * hand the location to, and it flushes the state tree after a modality change,
 * since there is no client response to trigger it.
 * <p>
 * The class is not final, so a test that needs a different UI can pass a
 * factory producing a subclass, but the shape of what it overrides is not part
 * of the published API.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockedUI extends UI {

    /** {@link Location} keeps its own copies of these package private. */
    private static final String QUERY_SEPARATOR = "?";

    private static final String FRAGMENT_SEPARATOR = "#";

    @Override
    public void setChildComponentModal(Component childComponent,
            ModalityMode mode) {
        super.setChildComponentModal(childComponent, mode);
        if (mode != ModalityMode.MODELESS) {
            AtomicReference<Registration> registrationCombination = new AtomicReference<>();
            if (childComponent != null) {
                registrationCombination
                        .set(childComponent.addDetachListener(event -> {
                            roundTrip();
                            Registration r = registrationCombination
                                    .getAndSet(null);
                            if (r != null) {
                                r.remove();
                            }
                        }));
            }
        }
        roundTrip();
    }

    @Override
    public void addToModalComponent(Component component) {
        super.addToModalComponent(component);
        if (component != null) {
            ComponentUtils.simulateClosedEvent(component);
        }
    }

    /**
     * Renders the route server side, as a test has no client router to hand the
     * navigation to.
     * <p>
     * This replaces {@link UI#navigate(String, QueryParameters)} rather than
     * extending it — {@code super} is never called — so nothing the real
     * {@code navigate} does with a location reaches a browserless test. How a
     * location string becomes a {@link Location}, and how a fragment-only
     * location is treated, therefore has to be mirrored here to be observable
     * in a test.
     *
     * @param locationString
     *            the location to navigate to
     * @param queryParameters
     *            query parameters given separately from the location
     * @throws IllegalArgumentException
     *             if query parameters are given separately and the location
     *             carries a query string or a fragment of its own
     */
    @Override
    public void navigate(String locationString,
            QueryParameters queryParameters) {
        Location location = toLocation(locationString, queryParameters);

        // A location that consists only of a fragment does not identify a
        // route: a running application leaves it to the client router rather
        // than resolving the "" route and replacing the current view. A test
        // has no client router, so there is simply nothing to render.
        if (location.getPath().isEmpty()
                && location.getQueryParameters().getParameters().isEmpty()
                && locationString.contains(FRAGMENT_SEPARATOR)) {
            return;
        }

        try {
            Method renderViewForRoute = UI.class.getDeclaredMethod(
                    "renderViewForRoute", Location.class,
                    NavigationTrigger.class);
            renderViewForRoute.setAccessible(true);
            renderViewForRoute.invoke(this, location,
                    NavigationTrigger.UI_NAVIGATE);
        } catch (InvocationTargetException ex) {
            // Reflection is an implementation detail of the mocked routing:
            // report what the router threw, not an InvocationTargetException
            // whose own message says nothing.
            throw sneakyThrow(ex.getTargetException());
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Builds the {@link Location} to render the way
     * {@link UI#navigate(String, QueryParameters)} does in a running
     * application.
     * <p>
     * Without separate query parameters the location string is the only source
     * of them, so it is free to carry a query string and a fragment. Given
     * both, its own query string or fragment would be lost, which is a mistake
     * worth reporting rather than dropping silently.
     * <p>
     * This mirrors {@code UI.navigate(String, QueryParameters)} as of
     * <a href="https://github.com/vaadin/flow/pull/25591">flow#25591</a>,
     * backported to 25.3 in
     * <a href="https://github.com/vaadin/flow/pull/25618">flow#25618</a>.
     * Compare against that method rather than against a locally resolved
     * snapshot, which may predate it.
     */
    private Location toLocation(String locationString,
            QueryParameters queryParameters) {
        boolean separateParameters = !queryParameters.getParameters().isEmpty();
        if (separateParameters && (locationString.contains(QUERY_SEPARATOR)
                || locationString.contains(FRAGMENT_SEPARATOR))) {
            throw new IllegalArgumentException("The location '" + locationString
                    + "' must be a plain path when query parameters are given separately, "
                    + "since its own query string or fragment would be lost. Pass the whole URL to "
                    + "navigate(String) instead.");
        }
        return separateParameters
                ? new Location(locationString, queryParameters)
                : new Location(locationString);
    }

    private void roundTrip() {
        getInternals().getStateTree().collectChanges(change -> {
        });
        getInternals().getStateTree().runExecutionsBeforeClientResponse();
    }

    /**
     * Rethrows the given throwable as-is, the way Kotlin and the router itself
     * do, without wrapping a checked exception the caller is not expecting.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(
            Throwable throwable) throws T {
        throw (T) throwable;
    }
}
