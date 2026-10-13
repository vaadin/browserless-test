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
package com.vaadin.browserless;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.vaadin.browserless.internal.MockInternalSeverError;
import com.vaadin.browserless.internal.MockRouteNotFoundError;
import com.vaadin.browserless.internal.ShortcutsKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteParameters;

/**
 * Shared DSL logic for browserless testing. All methods are static utilities
 * that take a {@link UI} parameter, so callers can supply the UI from
 * thread-locals or from a stored reference as appropriate.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class BrowserlessDSL {

    private BrowserlessDSL() {
    }

    static <T extends Component> T navigate(UI ui, Class<T> target) {
        ui.navigate(target);
        return validateNavigationTarget(ui, target);
    }

    static <C, T extends Component & HasUrlParameter<C>> T navigate(UI ui,
            Class<T> target, C parameter) {
        ui.navigate(target, parameter);
        return validateNavigationTarget(ui, target);
    }

    static <T extends Component> T navigate(UI ui, Class<T> target,
            Map<String, String> parameters) {
        ui.navigate(target, new RouteParameters(parameters));
        return validateNavigationTarget(ui, target);
    }

    static <T extends Component> T navigate(UI ui, String location,
            Class<T> expectedTarget) {
        // UI.navigate parses a location that carries a query string or a
        // fragment, so a test can spell it the way the address bar does
        ui.navigate(location);
        return validateNavigationTarget(ui, expectedTarget);
    }

    // The reload recreates the UI on the current thread-locals, so the mock
    // page rejects a UI that is not the current one; the entry points activate
    // their window before getting here. The UI that replaces the reloaded one
    // is only reachable as the new current UI.
    static HasElement reload(UI ui) {
        ui.getPage().reload();
        return getCurrentView(UI.getCurrent());
    }

    static <T extends Component> T reload(UI ui, Class<T> expectedTarget) {
        ui.getPage().reload();
        return validateNavigationTarget(UI.getCurrent(), expectedTarget);
    }

    static <T extends Component> T validateNavigationTarget(UI ui,
            Class<T> target) {
        HasElement currentView = getCurrentView(ui);
        if (!target.isAssignableFrom(currentView.getClass())) {
            if (currentView instanceof MockInternalSeverError) {
                System.err.println(
                        currentView.getElement().getProperty("stackTrace"));
            }
            String message = "Navigation resulted in unexpected class "
                    + currentView.getClass().getName() + " instead of "
                    + target.getName();
            if (currentView instanceof MockRouteNotFoundError notFound
                    && notFound.getCause() != null) {
                throw new IllegalArgumentException(
                        message + "\n" + describeRouteNotFound(notFound),
                        notFound.getCause());
            }
            throw new IllegalArgumentException(message);
        }
        return target.cast(currentView);
    }

    // Flow routes a NotFoundException thrown while creating the view, such as
    // the one a RouterLink to an unregistered target throws, to the same error
    // view as a missing route. The exception Flow caught then wraps the
    // view's own failure, which is dug out to name the actual missing target.
    private static String describeRouteNotFound(
            MockRouteNotFoundError notFound) {
        Throwable caught = notFound.getCause().getCause();
        NotFoundException innermost = null;
        Throwable t = caught == null ? null : caught.getCause();
        while (t != null) {
            if (t instanceof NotFoundException nfe) {
                innermost = nfe;
            }
            t = t.getCause();
        }
        if (innermost == null) {
            return notFound.getCause().getMessage();
        }
        return "The view threw " + innermost.getClass().getName() + ": "
                + innermost.getMessage()
                + "\nIf a RouterLink or another route lookup in the view threw"
                + " it, its target is not a registered route. Make sure the"
                + " test discovers the target, for example by adding its"
                + " package to @ViewPackages.";
    }

    static HasElement getCurrentView(UI ui) {
        return ui.getInternals().getActiveRouterTargetsChain().get(0);
    }

    static <T extends Component> ComponentQuery<T> find(UI ui,
            Class<T> componentType) {
        return new ComponentQuery<>(componentType);
    }

    static <T extends Component> ComponentQuery<T> find(UI ui,
            Class<T> componentType, Component fromThis) {
        return new ComponentQuery<>(componentType).from(fromThis);
    }

    static <T extends Component> ComponentQuery<T> findView(UI ui,
            Class<T> componentType) {
        Component viewComponent = getCurrentView(ui).getElement().getComponent()
                .orElseThrow(() -> new AssertionError(
                        "Cannot get Component instance for current view"));
        return new ComponentQuery<>(componentType).from(viewComponent);
    }

    static void fireShortcut(UI ui, Key key, KeyModifier... modifiers) {
        if (ui.hasModalComponent()) {
            ShortcutsKt._fireShortcut(
                    ui.getInternals().getActiveModalComponent(), key,
                    modifiers);
        } else {
            ShortcutsKt.fireShortcut(key, modifiers);
        }
    }

    static void roundTrip(UI ui) {
        ui.getInternals().getStateTree().collectChanges(nodeChange -> {
        });
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        FocusTracker.flush(ui);
    }

    static boolean runPendingSignalsTasks(
            TestSignalEnvironment signalsTestEnvironment) {
        return runPendingSignalsTasks(signalsTestEnvironment, 100,
                TimeUnit.MILLISECONDS);
    }

    static boolean runPendingSignalsTasks(
            TestSignalEnvironment signalsTestEnvironment, long maxWaitTime,
            TimeUnit unit) {
        if (signalsTestEnvironment != null) {
            return signalsTestEnvironment.runPendingTasks(maxWaitTime, unit);
        }
        return false;
    }
}
