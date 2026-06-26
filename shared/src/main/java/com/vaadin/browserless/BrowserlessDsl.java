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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasUrlParameter;

/**
 * Mixin exposing the browserless testing DSL ({@link #navigate}, {@link #find},
 * {@link #findInView}, {@link #getCurrentView}, {@link #fireShortcut},
 * {@link #roundTrip}, {@link #test}) as {@code default} methods, so the DSL can
 * be applied to any class via {@code implements BrowserlessDsl}, not only to
 * subclasses of {@link BaseBrowserlessTest}.
 * <p>
 * All methods delegate to the static {@link BrowserlessDslImpl} helper,
 * operating on the {@link UI} returned by the {@link #currentUI()} hook.
 * Implementations decide how that UI is resolved (e.g. from
 * {@link UI#getCurrent()} or from a stored reference) and perform any required
 * activation/validation.
 * <p>
 * The mixin provides only the DSL. The implementing class is still responsible
 * for setting up the Vaadin test environment so that {@link #currentUI()} can
 * return a live UI.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public interface BrowserlessDsl {

    /**
     * Returns the {@link UI} the DSL methods operate on, performing any
     * activation or validation required by the implementation.
     * <p>
     * Internal SPI hook implemented by mixin consumers.
     *
     * @return the UI the DSL methods operate on
     */
    UI currentUI();

    /**
     * Navigate to the given view class if it is registered.
     *
     * @param navigationTarget
     *            view class to navigate to
     * @param <T>
     *            view type
     * @return instantiated view
     */
    default <T extends Component> T navigate(Class<T> navigationTarget) {
        return BrowserlessDslImpl.navigate(currentUI(), navigationTarget);
    }

    /**
     * Navigate to view with url parameter.
     *
     * @param navigationTarget
     *            view class to navigate to
     * @param parameter
     *            parameter to send to view
     * @param <T>
     *            view type
     * @param <C>
     *            parameter type
     * @return instantiated view
     */
    default <C, T extends Component & HasUrlParameter<C>> T navigate(
            Class<T> navigationTarget, C parameter) {
        return BrowserlessDslImpl.navigate(currentUI(), navigationTarget,
                parameter);
    }

    /**
     * Navigate to view corresponding to the given navigation target with the
     * specified parameters.
     *
     * @param navigationTarget
     *            view class to navigate to
     * @param parameters
     *            parameters to pass to view.
     * @param <T>
     *            view type
     * @return instantiated view
     */
    default <T extends Component> T navigate(Class<T> navigationTarget,
            Map<String, String> parameters) {
        return BrowserlessDslImpl.navigate(currentUI(), navigationTarget,
                parameters);
    }

    /**
     * Navigate to given location string. Check that location navigated to is
     * the expected view or throw exception.
     *
     * @param location
     *            location string for navigating
     * @param expectedTarget
     *            class that is expected for navigation
     * @param <T>
     *            view type
     * @return instantiated view
     */
    default <T extends Component> T navigate(String location,
            Class<T> expectedTarget) {
        return BrowserlessDslImpl.navigate(currentUI(), location,
                expectedTarget);
    }

    /**
     * Gets a query object for finding a component inside the UI
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     */
    default <T extends Component> ComponentQuery<T> find(
            Class<T> componentType) {
        return BrowserlessDslImpl.find(currentUI(), componentType);
    }

    /**
     * Gets a query object for finding a component nested inside the given
     * component.
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param fromThis
     *            component used as starting element for search.
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     */
    default <T extends Component> ComponentQuery<T> find(Class<T> componentType,
            Component fromThis) {
        return BrowserlessDslImpl.find(currentUI(), componentType, fromThis);
    }

    /**
     * Gets a query object for finding a component inside the current view
     *
     * @param componentType
     *            the type of the component(s) to search for
     * @param <T>
     *            the type of the component(s) to search for
     * @return a query object for finding components
     */
    default <T extends Component> ComponentQuery<T> findInView(
            Class<T> componentType) {
        return BrowserlessDslImpl.findView(currentUI(), componentType);
    }

    /**
     * Get the current view instance that is shown on the ui.
     *
     * @return current view
     */
    default HasElement getCurrentView() {
        return BrowserlessDslImpl.getCurrentView(currentUI());
    }

    /**
     * Simulates a keyboard shortcut performed on the browser.
     *
     * @param key
     *            Primary key of the shortcut. This must not be a
     *            {@link KeyModifier}.
     * @param modifiers
     *            Key modifiers. Can be empty.
     */
    default void fireShortcut(Key key, KeyModifier... modifiers) {
        BrowserlessDslImpl.fireShortcut(currentUI(), key, modifiers);
    }

    /**
     * Simulates a server round-trip, flushing pending component changes.
     */
    default void roundTrip() {
        BrowserlessDslImpl.roundTrip(currentUI());
    }

    /**
     * Wrap component with ComponentTester best matching component type.
     *
     * @param component
     *            component to get test wrapper for
     * @param <T>
     *            tester type
     * @param <Y>
     *            component type
     * @return component in wrapper with test helpers
     */
    default <T extends ComponentTester<Y>, Y extends Component> T test(
            Y component) {
        currentUI();
        return BaseBrowserlessTest.internalWrap(component);
    }
}
