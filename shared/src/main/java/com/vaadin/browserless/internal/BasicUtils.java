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
package com.vaadin.browserless.internal;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.server.VaadinSession;

/**
 * Helpers for driving a component the way a browser would: firing events on it,
 * asking whether a user could actually interact with it, and reading the few
 * properties Flow does not expose directly.
 * <p>
 * {@link #checkEditableByUser(Component)} is the one to reach for before any
 * simulated interaction. A component that is invisible, disabled, read-only or
 * detached cannot be touched by a user, and a test that pretends otherwise
 * asserts on a state the application can never reach.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class BasicUtils {

    private BasicUtils() {
    }

    /**
     * Allows us to fire any Vaadin event on any Vaadin component.
     * 
     * @param component
     *            the component, not null.
     * @param event
     *            the event, not null.
     */
    public static void _fireEvent(Component component,
            ComponentEvent<?> event) {
        ComponentUtil.fireEvent(component, event);
    }

    /**
     * Fires a DOM event on this component.
     * 
     * @param eventType
     *            the event type, e.g. "click"
     */
    public static void _fireDomEvent(Component component, String eventType) {
        _fireDomEvent(component, eventType, JacksonUtils.createObjectNode());
    }

    /**
     * Fires a DOM event on this component.
     * 
     * @param eventType
     *            the event type, e.g. "click"
     * @param eventData
     *            optional event data, defaults to an empty object.
     */
    public static void _fireDomEvent(Component component, String eventType,
            ObjectNode eventData) {
        Element element = component.getElement();
        ElementUtils._fireDomEvent(element,
                new DomEvent(element, eventType, eventData));
    }

    /**
     * The same as {@link Component#getId} but without Optional.
     *
     * Workaround for https://github.com/vaadin/flow/issues/664
     */
    public static String id_(Component component) {
        return component.getId().orElse(null);
    }

    public static void id_(Component component, String value) {
        component.setId(value);
    }

    /**
     * Checks whether the component is visible (usually
     * {@link Component#isVisible} but for {@link Text} the text must be
     * non-empty).
     */
    public static boolean _isVisible(Component component) {
        if (component instanceof Text) {
            // workaround for https://github.com/vaadin/flow/issues/3201
            String text = ((Text) component).getText();
            return text != null && !text.isBlank();
        }
        return component.isVisible();
    }

    /**
     * Returns direct text contents (it doesn't peek into the child elements).
     */
    public static String _text(Component component) {
        if (component instanceof HasText) {
            return ((HasText) component).getText();
        }
        return null;
    }

    /**
     * Checks that a component is actually editable by the user: * The component
     * must be effectively visible: it itself must be visible, its parent must
     * be visible and all of its ascendants must be visible. For the purpose of
     * testing individual components not attached to the {@link UI}, a component
     * may be considered visible even though it's not currently nested in a
     * {@link UI}. * The component must be effectively enabled: it itself must
     * be enabled, its parent must be enabled and all of its ascendants must be
     * enabled. * If the component is {@link HasValue}, it must not be
     * {@link HasValue#isReadOnly}.
     * 
     * @throws IllegalStateException
     *             if any of the above doesn't hold.
     */
    public static void checkEditableByUser(Component component) {
        if (!isEffectivelyVisible(component)) {
            throw new IllegalStateException("The "
                    + PrettyPrintTreeKt.toPrettyString(component)
                    + " is not effectively visible - either it is hidden, or its ascendant is hidden");
        }
        boolean parentNullOrEnabled = !component.getParent().isPresent()
                || isEffectivelyEnabled(component.getParent().get());
        if (parentNullOrEnabled) {
            if (!component.getElement().isEnabled()) {
                throw new IllegalStateException(
                        "The " + PrettyPrintTreeKt.toPrettyString(component)
                                + " is not enabled");
            }
        }
        if (!isEffectivelyEnabled(component)) {
            throw new IllegalStateException(
                    "The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is nested in a disabled component");
        }
        if (component instanceof HasValue) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            HasValue<HasValue.ValueChangeEvent<Object>, Object> hasValue = (HasValue) component;
            if (hasValue.isReadOnly()) {
                throw new IllegalStateException(
                        "The " + PrettyPrintTreeKt.toPrettyString(component)
                                + " is read-only");
            }
        }
        if (!component.isAttached()) {
            throw new IllegalStateException(
                    " The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is not attached");
        }
    }

    /**
     * Fails if the component is editable. See {@link #checkEditableByUser} for
     * more details.
     * 
     * @throws AssertionError
     *             if the component is editable.
     */
    public static void expectNotEditableByUser(Component component) {
        try {
            checkEditableByUser(component);
        } catch (IllegalStateException ex) {
            // okay
            return;
        }
        throw new AssertionError("The "
                + PrettyPrintTreeKt.toPrettyString(component) + " is editable");
    }

    static boolean isEffectivelyVisible(Component component) {
        return _isVisible(component) && (!component.getParent().isPresent()
                || isEffectivelyVisible(component.getParent().get()));
    }

    /**
     * Computes whether this component and all of its parents are enabled.
     *
     * Recursively checks that all ancestors are also enabled (the "implicitly
     * disabled" effect, see {@link HasEnabled#isEnabled} javadoc for more
     * details).
     *
     * Also check that the component is not inert due to there being a modal
     * component.
     *
     * @return false if this component or any of its parent is disabled or is
     *         inert.
     */
    public static boolean isEffectivelyEnabled(Component component) {
        return component.getElement().isEnabled()
                && !component.getElement().getNode().isInert();
    }

    /**
     * Fires {@link FocusNotifier.FocusEvent} on the component, but only if it's
     * editable.
     */
    public static <T extends Component & Focusable<T>> void _focus(
            T component) {
        checkEditableByUser(component);
        _fireEvent(component,
                new FocusNotifier.FocusEvent<>(component, true, null));
    }

    /**
     * Fires {@link BlurNotifier.BlurEvent} on the component, but only if it's
     * editable.
     */
    public static <T extends Component & Focusable<T>> void _blur(T component) {
        checkEditableByUser(component);
        _fireEvent(component,
                new BlurNotifier.BlurEvent<>(component, true, null));
    }

    /**
     * Closes the UI and simulates the end of the request. The {@link UI#close}
     * is called, but also the session is set to null which fires the detach
     * listeners and makes the UI and all of its components detached.
     */
    public static void _close(UI ui) {
        ui.close();
        // Mock closing of UI after request handled.
        VaadinSession.getCurrent().removeUI(ui);
    }

    /**
     * Returns child components which were added to this component via
     * {@code com.vaadin.flow.dom.Element.appendVirtualChild}.
     */
    public static List<Component> _getVirtualChildren(Component component) {
        List<Component> result = new ArrayList<>();
        for (Element child : ElementUtils
                .getVirtualChildren(component.getElement())) {
            result.addAll(ElementUtils._findComponents(child));
        }
        return result;
    }

    static String errorMessage(InternalServerError error) {
        return error.getElement().getText();
    }

    public static int _saneFetchLimit() {
        // don't use high value otherwise Vaadin 19+ will calculate negative
        // limit and will pass it to SizeVerifier,
        // failing instantly.
        return Integer.MAX_VALUE / 1000;
    }
}
