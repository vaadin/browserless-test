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
package com.vaadin.flow.component.login;

import java.util.function.Consumer;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;

/**
 * Tester for LoginOverlay components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(LoginOverlay.class)
public class LoginOverlayTester<T extends LoginOverlay>
        extends AbstractLoginTester<T> {

    /**
     * The slot an overlay puts the content of its
     * {@link LoginOverlay#getFooter() footer} in.
     */
    private static final String FOOTER_SLOT = "footer";

    /**
     * The slot an overlay puts the content of its
     * {@link LoginOverlay#getCustomFormArea() custom form area} in.
     */
    private static final String CUSTOM_FORM_AREA_SLOT = "custom-form-area";

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public LoginOverlayTester(T component) {
        super(component);
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && getComponent().isOpened();
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        if (!getComponent().isOpened()) {
            collector.accept("not opened");
        }
    }

    /**
     * Open LoginOverlay to enable logging in through it.
     */
    public void openOverlay() {
        getComponent().setOpened(true);
    }

    /**
     * Check if login overlay is open.
     *
     * @return {@code true} if overlay is open and visible
     */
    public boolean isOpen() {
        return getComponent().isOpened() && getComponent().isVisible();
    }

    /**
     * Searches the overlay's footer for components of the given type.
     * <p>
     * The footer is the area below the login form, filled with
     * {@code login.getFooter().add(...)}. Components nested inside the footer
     * content are found too, so a button inside a footer layout is reached as
     * well as a button added to the footer directly.
     *
     * @param componentType
     *            the type of the components to search for
     * @param <R>
     *            the type of the components to search for
     * @return a query for components of the given type in the overlay's footer
     * @throws IllegalStateException
     *             if the overlay is not usable, e.g. not open
     */
    public <R extends Component> ComponentQuery<R> findInFooter(
            Class<R> componentType) {
        ensureComponentIsUsable();
        return find(componentType).withinSlot(FOOTER_SLOT);
    }

    /**
     * Searches the overlay's custom form area for components of the given type.
     * <p>
     * The custom form area is the area inside the login form, filled with
     * {@code login.getCustomFormArea().add(...)}. Components nested inside it
     * are found too, so a field inside a layout added to the area is reached as
     * well as a field added to the area directly.
     *
     * @param componentType
     *            the type of the components to search for
     * @param <R>
     *            the type of the components to search for
     * @return a query for components of the given type in the overlay's custom
     *         form area
     * @throws IllegalStateException
     *             if the overlay is not usable, e.g. not open
     */
    public <R extends Component> ComponentQuery<R> findInCustomFormArea(
            Class<R> componentType) {
        ensureComponentIsUsable();
        return find(componentType).withinSlot(CUSTOM_FORM_AREA_SLOT);
    }
}
