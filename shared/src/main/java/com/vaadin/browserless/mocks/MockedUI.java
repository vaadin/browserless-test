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
 * A simple no-op UI used by default by [com.vaadin.browserless.MockVaadin.setup].
 * The class is open, in order to be extensible in user's library
 */
public class MockedUI extends UI {

    @Override
    public void setChildComponentModal(Component childComponent, ModalityMode mode) {
        super.setChildComponentModal(childComponent, mode);
        if (mode != ModalityMode.MODELESS) {
            AtomicReference<Registration> registrationCombination = new AtomicReference<>();
            if (childComponent != null) {
                registrationCombination.set(childComponent.addDetachListener(event -> {
                    roundTrip();
                    Registration r = registrationCombination.getAndSet(null);
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

    @Override
    public void navigate(String locationString, QueryParameters queryParameters) {
        // server-side routing only for tests as there is no client to handle routing.
        try {
            Method m = UI.class.getDeclaredMethod("renderViewForRoute", Location.class, NavigationTrigger.class);
            m.setAccessible(true);
            m.invoke(this, new Location(locationString, queryParameters), NavigationTrigger.UI_NAVIGATE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void roundTrip() {
        getInternals().getStateTree().collectChanges(change -> { });
        getInternals().getStateTree().runExecutionsBeforeClientResponse();
    }
}
