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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.ranges.IntRange;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasEnabled;
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
 * Basic utilities for firing events on components and checking their state.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class BasicUtilsKt {

    private BasicUtilsKt() {
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
     * Fires a DOM event on given component.
     *
     * @param component
     *            the component, not null.
     * @param eventType
     *            the event type, e.g. "click"
     * @param eventData
     *            the event data, not null.
     */
    public static void _fireDomEvent(Component component, String eventType,
            ObjectNode eventData) {
        Element element = component.getElement();
        ElementUtilsKt._fireDomEvent(element,
                new DomEvent(element, eventType, eventData));
    }

    /**
     * Fires a DOM event with empty event data on given component.
     *
     * @param component
     *            the component, not null.
     * @param eventType
     *            the event type, e.g. "click"
     */
    public static void _fireDomEvent(Component component, String eventType) {
        _fireDomEvent(component, eventType, JacksonUtils.createObjectNode());
    }

    /**
     * The same as {@link Component#getId()} but without Optional.
     * <p>
     * Workaround for https://github.com/vaadin/flow/issues/664
     *
     * @param component
     *            the component, not null.
     * @return the id, or null.
     */
    public static String getId_(Component component) {
        return component.getId().orElse(null);
    }

    /**
     * Sets the id of given component.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the id, may be null.
     */
    public static void setId_(Component component, String value) {
        component.setId(value);
    }

    /**
     * Checks whether the component is visible (usually
     * {@link Component#isVisible()} but for {@link Text} the text must be
     * non-empty).
     *
     * @param component
     *            the component, not null.
     * @return true if the component is visible.
     */
    public static boolean get_isVisible(Component component) {
        if (component instanceof Text text) {
            // workaround for https://github.com/vaadin/flow/issues/3201
            return !UtilsKt.isBlank(text.getText());
        }
        return component.isVisible();
    }

    /**
     * Returns direct text contents (it doesn't peek into the child elements).
     *
     * @param component
     *            the component, not null.
     * @return the text, or null if the component is not {@link HasText}.
     */
    public static String get_text(Component component) {
        if (component instanceof HasText hasText) {
            return hasText.getText();
        }
        return null;
    }

    /**
     * Checks that a component is actually editable by the user:
     * <ul>
     * <li>The component must be effectively visible: it itself must be visible,
     * its parent must be visible and all of its ascendants must be visible. For
     * the purpose of testing individual components not attached to the
     * {@link UI}, a component may be considered visible even though it's not
     * currently nested in a {@link UI}.</li>
     * <li>The component must be effectively enabled: it itself must be enabled,
     * its parent must be enabled and all of its ascendants must be
     * enabled.</li>
     * <li>If the component is {@link HasValue}, it must not be
     * {@link HasValue#isReadOnly()}.</li>
     * </ul>
     *
     * @param component
     *            the component, not null.
     * @throws IllegalStateException
     *             if any of the above doesn't hold.
     */
    public static void checkEditableByUser(Component component) {
        if (!isEffectivelyVisible(component)) {
            throw new IllegalStateException("The "
                    + PrettyPrintTreeKt.toPrettyString(component)
                    + " is not effectively visible - either it is hidden, or its ascendant is hidden");
        }
        boolean parentNullOrEnabled = component.getParent().isEmpty()
                || isEffectivelyEnabled(component.getParent().get());
        if (parentNullOrEnabled && !component.getElement().isEnabled()) {
            throw new IllegalStateException(
                    "The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is not enabled");
        }
        if (!isEffectivelyEnabled(component)) {
            throw new IllegalStateException(
                    "The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is nested in a disabled component");
        }
        if (component instanceof HasValue<?, ?> hasValue
                && hasValue.isReadOnly()) {
            throw new IllegalStateException(
                    "The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is read-only");
        }
        if (!component.isAttached()) {
            throw new IllegalStateException(
                    " The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is not attached");
        }
    }

    /**
     * Fails if the component is editable. See
     * {@link #checkEditableByUser(Component)} for more details.
     *
     * @param component
     *            the component, not null.
     * @throws AssertionError
     *             if the component is editable.
     */
    public static void expectNotEditableByUser(Component component) {
        try {
            checkEditableByUser(component);
            throw new AssertionError(
                    "The " + PrettyPrintTreeKt.toPrettyString(component)
                            + " is editable");
        } catch (IllegalStateException ex) {
            // okay
        }
    }

    /**
     * Computes whether this component and all of its parents are visible.
     *
     * @param component
     *            the component, not null.
     * @return true if the component and all of its ascendants are visible.
     */
    public static boolean isEffectivelyVisible(Component component) {
        return get_isVisible(component) && (component.getParent().isEmpty()
                || isEffectivelyVisible(component.getParent().get()));
    }

    /**
     * Computes whether this component and all of its parents are enabled.
     * <p>
     * Recursively checks that all ancestors are also enabled (the "implicitly
     * disabled" effect, see {@link HasEnabled#isEnabled()} javadoc for more
     * details).
     * <p>
     * Also check that the component is not inert due to there being a modal
     * component.
     *
     * @param component
     *            the component, not null.
     * @return false if this component or any of its parent is disabled or is
     *         inert.
     */
    public static boolean isEffectivelyEnabled(Component component) {
        return component.getElement().isEnabled()
                && !component.getElement().getNode().isInert();
    }

    /**
     * Checks whether this component matches given spec. All rules are matched
     * except the {@code count} rule. The rules are matched against given
     * component only (not against its children).
     *
     * @param component
     *            the component, not null.
     * @param spec
     *            configures the search spec, not null.
     * @return true if the component matches the spec.
     */
    public static boolean matches(Component component,
            Function1<? super SearchSpec<Component>, Unit> spec) {
        SearchSpec<Component> searchSpec = new SearchSpec<>(Component.class,
                null, null, null, null, null,
                new IntRange(0, Integer.MAX_VALUE), null, null, null,
                new ArrayList<>(), null, null);
        spec.invoke(searchSpec);
        return searchSpec.toPredicate().invoke(component);
    }

    /**
     * Fires {@link FocusNotifier.FocusEvent} on the component, but only if it's
     * editable.
     *
     * @param component
     *            the component, not null.
     * @param <T>
     *            the component type
     */
    public static <T extends Component & Focusable<?>> void _focus(
            T component) {
        checkEditableByUser(component);
        _fireEvent(component,
                new FocusNotifier.FocusEvent<T>(component, true, null));
    }

    /**
     * Fires {@link BlurNotifier.BlurEvent} on the component, but only if it's
     * editable.
     *
     * @param component
     *            the component, not null.
     * @param <T>
     *            the component type
     */
    public static <T extends Component & Focusable<?>> void _blur(T component) {
        checkEditableByUser(component);
        _fireEvent(component,
                new BlurNotifier.BlurEvent<T>(component, true, null));
    }

    /**
     * Closes the UI and simulates the end of the request. The
     * {@link UI#close()} is called, but also the session is set to null which
     * fires the detach listeners and makes the UI and all of its components
     * detached.
     *
     * @param ui
     *            the UI, not null.
     */
    public static void _close(UI ui) {
        ui.close();
        // Mock closing of UI after request handled.
        VaadinSession.getCurrent().removeUI(ui);
    }

    /**
     * Returns child components which were added to this component via
     * {@link Element#appendVirtualChild(Element...)}.
     *
     * @param component
     *            the component, not null.
     * @return the virtual child components.
     */
    public static List<Component> _getVirtualChildren(Component component) {
        List<Component> result = new ArrayList<>();
        for (Element child : ElementUtilsKt
                .getVirtualChildren(component.getElement())) {
            result.addAll(ElementUtilsKt._findComponents(child));
        }
        return result;
    }

    /**
     * Returns the error message shown by given error view.
     *
     * @param error
     *            the error view, not null.
     * @return the error message.
     */
    public static String getErrorMessage(InternalServerError error) {
        return error.getElement().getText();
    }

    /**
     * Returns a fetch limit that is safe to pass to data providers.
     *
     * @return the fetch limit.
     */
    public static int get_saneFetchLimit() {
        // don't use high value otherwise Vaadin 19+ will calculate negative
        // limit and will pass it to SizeVerifier, failing instantly.
        return Integer.MAX_VALUE / 1000;
    }
}
