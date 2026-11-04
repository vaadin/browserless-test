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

import java.util.List;
import java.util.stream.Collectors;

import kotlin.jvm.functions.Function1;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Location;

/**
 * Utilities for working with {@link Component}s.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ComponentUtils {

    private ComponentUtils() {
    }

    /**
     * Fires given event on the component.
     *
     * @param component
     *            the component, not null.
     * @param event
     *            the event, not null.
     */
    public static void fireEvent(Component component, ComponentEvent<?> event) {
        ComponentUtil.fireEvent(component, event);
    }

    /**
     * Adds {@link Button#click()} functionality to all {@link ClickNotifier}s.
     * This function directly calls all click listeners, thus it avoids the
     * roundtrip to client and back. It even works with browserless testing.
     *
     * @param component
     *            the component, not null.
     * @param fromClient
     *            see {@link ComponentEvent#isFromClient()}.
     * @param button
     *            see {@link ClickEvent#getButton()}.
     * @param clickCount
     *            see {@link ClickEvent#getClickCount()}.
     * @param shiftKey
     *            see {@link ClickEvent#isShiftKey()}.
     * @param ctrlKey
     *            see {@link ClickEvent#isCtrlKey()}.
     * @param altKey
     *            see {@link ClickEvent#isAltKey()}.
     * @param metaKey
     *            see {@link ClickEvent#isMetaKey()}.
     * @param <T>
     *            the component type
     */
    public static <T extends ClickNotifier<?>> void serverClick(T component,
            boolean fromClient, int button, int clickCount, boolean shiftKey,
            boolean ctrlKey, boolean altKey, boolean metaKey) {
        fireEvent((Component) component,
                new ClickEvent<Component>((Component) component, fromClient, -1,
                        -1, -1, -1, clickCount, button, ctrlKey, shiftKey,
                        altKey, metaKey));
    }

    /**
     * Gets the alignment of the text in the component. One of {@code center},
     * {@code left}, {@code right}, {@code justify}.
     *
     * @param component
     *            the component, not null.
     * @return the text alignment, or null.
     */
    public static String getTextAlign(Component component) {
        return component.getElement().getStyle().get("textAlign");
    }

    /**
     * Sets the alignment of the text in the component. One of {@code center},
     * {@code left}, {@code right}, {@code justify}.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the text alignment, may be null.
     */
    public static void setTextAlign(Component component, String value) {
        component.getElement().getStyle().set("textAlign", value);
    }

    /**
     * Gets the {@code title} attribute of component's element.
     *
     * @param component
     *            the component, not null.
     * @return the tooltip, or null.
     */
    public static String getTooltip(Component component) {
        return component.getElement().getAttribute("title");
    }

    /**
     * Sets or removes the {@code title} attribute on component's element.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the tooltip, or null to remove it.
     */
    public static void setTooltip(Component component, String value) {
        ElementUtils.setOrRemoveAttribute(component.getElement(), "title",
                value);
    }

    /**
     * Adds the right-click (context-menu) {@code listener} to the component.
     * Also causes the right-click browser menu not to be shown on this
     * component (see {@link #preventDefault(DomListenerRegistration)}).
     *
     * @param component
     *            the component, not null.
     * @param listener
     *            the listener, not null.
     * @return the listener registration.
     */
    public static DomListenerRegistration addContextMenuListener(
            Component component, DomEventListener listener) {
        return component.getElement().addEventListener("contextmenu", listener)
                .preventDefault();
    }

    /**
     * Makes the client-side listener call <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/Event/preventDefault">Event.preventDefault()</a>
     * on the event.
     *
     * @param registration
     *            the listener registration, not null.
     * @return the registration
     */
    public static DomListenerRegistration preventDefault(
            DomListenerRegistration registration) {
        return registration.addEventData("event.preventDefault()");
    }

    /**
     * Removes the component from its parent. Does nothing if the component is
     * not attached to a parent.
     *
     * @param component
     *            the component, not null.
     */
    public static void removeFromParent(Component component) {
        if (component.getParent()
                .orElse(null) instanceof HasComponents parent) {
            parent.remove(component);
        }
    }

    /**
     * Finds component, component's parent, parent's parent (etc) which
     * satisfies given {@code predicate}. Returns null if no component on the
     * ancestor-or-self axis satisfies.
     *
     * @param component
     *            the component to start from, not null.
     * @param predicate
     *            the predicate, not null.
     * @return the matching component, or null.
     */
    public static Component findAncestorOrSelf(Component component,
            Function1<? super Component, Boolean> predicate) {
        Component current = component;
        while (current != null) {
            if (predicate.invoke(current)) {
                return current;
            }
            current = current.getParent().orElse(null);
        }
        return null;
    }

    /**
     * Checks if this component is nested in {@code potentialAncestor}.
     *
     * @param component
     *            the component, not null.
     * @param potentialAncestor
     *            the potential ancestor, not null.
     * @return true if the component is nested in the potential ancestor.
     */
    public static boolean isNestedIn(Component component,
            Component potentialAncestor) {
        return component.findAncestor(it -> it.equals(potentialAncestor))
                .isPresent();
    }

    /**
     * Checks whether this component is currently attached to a {@link UI}.
     * <p>
     * Returns true for attached components even if the UI itself is closed.
     *
     * @param component
     *            the component, not null.
     * @return true if the component is attached.
     */
    public static boolean isAttached(Component component) {
        // see https://github.com/vaadin/flow/issues/7911
        return component.getElement().getNode().isAttached();
    }

    /**
     * Returns the data provider currently set to this Component.
     *
     * @param component
     *            the component, not null.
     * @return the data provider, or null.
     */
    public static DataProvider<?, ?> getDataProvider(Component component) {
        return ComponentSupports.getDataProvider(component);
    }

    /**
     * Inserts given component as a child, right before an {@code existing} one.
     * <p>
     * In case the specified component has already been added to another parent,
     * it will be removed from there and added to this one.
     *
     * @param container
     *            the parent component, not null.
     * @param newComponent
     *            the component to insert, not null.
     * @param existing
     *            the child before which to insert, not null.
     */
    public static void insertBefore(HasOrderedComponents container,
            Component newComponent, Component existing) {
        Component parent = existing.getParent().orElse(null);
        if (parent == null) {
            throw new IllegalArgumentException(existing + " has no parent");
        }
        if (!parent.equals(container)) {
            throw new IllegalArgumentException(
                    existing + " is not nested in " + container);
        }
        container.addComponentAtIndex(container.indexOf(existing),
                newComponent);
    }

    /**
     * Return the location of the currently shown view. The function will report
     * the current (old) view in {@link com.vaadin.flow.router.BeforeLeaveEvent}
     * and {@link com.vaadin.flow.router.BeforeEnterEvent}.
     *
     * @param ui
     *            the UI, not null.
     * @return the location of the current view.
     */
    public static Location getCurrentViewLocation(UI ui) {
        return ui.getInternals().getActiveViewLocation();
    }

    /**
     * True when the component has any children.
     *
     * @param container
     *            the component, not null.
     * @return true if the component has any children.
     */
    public static boolean getHasChildren(HasComponents container) {
        return ((Component) container).getChildren().findFirst().isPresent();
    }

    /**
     * Splits {@code classNames} by whitespaces to obtain individual class
     * names, then calls {@link HasStyle#addClassName(String)} on each class
     * name. Does nothing if the string is blank.
     *
     * @param component
     *            the component, not null.
     * @param classNames
     *            the class names, not null.
     */
    public static void addClassNames2(HasStyle component, String classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        Utils.splitByWhitespaces(classNames).forEach(component::addClassName);
    }

    /**
     * Splits {@code classNames} by whitespaces to obtain individual class
     * names, then calls {@link #addClassNames2(HasStyle, String)} on each class
     * name. Does nothing if the string is blank.
     *
     * @param component
     *            the component, not null.
     * @param classNames
     *            the class names, not null.
     */
    public static void addClassNames2(HasStyle component,
            String... classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        for (String className : classNames) {
            addClassNames2(component, className);
        }
    }

    /**
     * Splits {@code classNames} by whitespaces to obtain individual class
     * names, then calls {@link HasStyle#removeClassName(String)} on each class
     * name. Does nothing if the string is blank.
     *
     * @param component
     *            the component, not null.
     * @param classNames
     *            the class names, not null.
     */
    public static void removeClassNames2(HasStyle component,
            String classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        Utils.splitByWhitespaces(classNames)
                .forEach(component::removeClassName);
    }

    /**
     * Splits {@code classNames} by whitespaces to obtain individual class
     * names, then calls {@link #removeClassNames2(HasStyle, String)} on each
     * class name. Does nothing if the string is blank.
     *
     * @param component
     *            the component, not null.
     * @param classNames
     *            the class names, not null.
     */
    public static void removeClassNames2(HasStyle component,
            String... classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        for (String className : classNames) {
            removeClassNames2(component, className);
        }
    }

    /**
     * Splits {@code classNames} by whitespaces to obtain individual class
     * names, then clears the class names and calls
     * {@link #addClassNames2(HasStyle, String)} on each class name. Does
     * nothing if the string is blank.
     *
     * @param component
     *            the component, not null.
     * @param classNames
     *            the class names, not null.
     */
    public static void setClassNames2(HasStyle component, String classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        component.getStyle().clear();
        addClassNames2(component, classNames);
    }

    /**
     * Splits {@code classNames} by whitespaces to obtain individual class
     * names, then clears the class names and calls
     * {@link #addClassNames2(HasStyle, String...)} on each class name. Does
     * nothing if the string is blank.
     *
     * @param component
     *            the component, not null.
     * @param classNames
     *            the class names, not null.
     */
    public static void setClassNames2(HasStyle component,
            String... classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        component.getStyle().clear();
        addClassNames2(component, classNames);
    }

    /**
     * A component placeholder, usually shown when there's no value selected.
     * Not all components support a placeholder; those that don't will return
     * null.
     *
     * @param component
     *            the component, not null.
     * @return the placeholder, or null.
     */
    public static String getPlaceholder(Component component) {
        return ComponentSupports.getPlaceholder(component);
    }

    /**
     * Sets the component placeholder.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the placeholder, may be null.
     * @throws IllegalStateException
     *             if the component doesn't support a placeholder.
     */
    public static void setPlaceholder(Component component, String value) {
        ComponentSupports.setPlaceholder(component, value);
    }

    /**
     * Concatenates texts from all elements placed in the {@code label} slot.
     * This effectively returns whatever was provided in the String label via
     * {@link FormLayout#addFormItem(Component, String)}.
     *
     * @param formItem
     *            the form item, not null.
     * @return the label.
     */
    public static String getLabel(FormLayout.FormItem formItem) {
        List<Component> captions = formItem.getChildren().filter(
                it -> "label".equals(it.getElement().getAttribute("slot")))
                .toList();
        return captions.stream().map(it -> {
            String text = it instanceof HasText hasText ? hasText.getText()
                    : null;
            return text != null ? text : "";
        }).collect(Collectors.joining(""));
    }

    /**
     * Determines the component's {@code label} (usually it's the HTML element's
     * {@code label} property, but it's {@link Checkbox#getLabel()} for
     * checkbox). Intended to be used for fields such as {@link TextField}.
     * <p>
     * <em>For {@code FormItem}:</em> Concatenates texts from all elements
     * placed in the {@code label} slot. This effectively returns whatever was
     * provided in the String label via
     * {@link FormLayout#addFormItem(Component, String)}.
     * <p>
     * {@link #getCaption(Button)} is displayed directly on the component while
     * label is displayed next to the component in a layout (e.g. a
     * {@link TextField} nested in a form layout).
     * <p>
     * Vote for <a href="https://github.com/vaadin/flow/issues/3241">issue
     * #3241</a>.
     *
     * @param component
     *            the component, not null.
     * @return the label, never null.
     */
    public static String getLabel(Component component) {
        return ComponentSupports.getLabel(component);
    }

    /**
     * Sets the component's {@code label}. See {@link #getLabel(Component)}.
     * <p>
     * <b>WARNING:</b> the label is displayed by the component itself, rather
     * than by the parent layout. If a component doesn't contain necessary
     * machinery to display a label, setting this property will have no visual
     * effect. For example, setting a label to a {@link FormLayout} nested
     * within a {@code VerticalLayout} will show nothing since
     * {@link FormLayout} doesn't display a label itself.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the label, not null.
     */
    public static void setLabel(Component component, String value) {
        ComponentSupports.setLabel(component, value);
    }

    /**
     * The Component's caption: {@link Button#getText()} for {@link Button},
     * {@link #getLabel(Component)} for fields such as {@link TextField}.
     * <p>
     * Caption is generally displayed directly on the component (e.g. the Button
     * text), while label is displayed next to the component in a layout (e.g. a
     * {@link TextField} nested in a form layout).
     *
     * @param component
     *            the component, not null.
     * @return the caption.
     * @deprecated this property was intended to unify captions and labels, but
     *             only managed to create confusion between the two concepts.
     *             Also, there's only a {@link Button} which has the notion of a
     *             caption. Will be removed with no replacement.
     */
    @Deprecated
    public static String getCaption(Component component) {
        return ComponentSupports.getCaption(component);
    }

    /**
     * Sets the Component's caption. See {@link #getCaption(Component)}.
     *
     * @param component
     *            the component, not null.
     * @param value
     *            the caption, not null.
     * @deprecated this property was intended to unify captions and labels, but
     *             only managed to create confusion between the two concepts.
     *             Will be removed with no replacement.
     */
    @Deprecated
    public static void setCaption(Component component, String value) {
        ComponentSupports.setCaption(component, value);
    }

    /**
     * Sets up an event listener for overlay components that fires a
     * {@code closed} DOM event when the component is closed. This simulates the
     * event being fired from the browser after the closing animation has
     * finished.
     *
     * @param component
     *            the component, not null.
     */
    public static void simulateClosedEvent(Component component) {
        ComponentSupports.simulateClosedEvent(component);
    }

    /**
     * The Button's caption. Alias for {@link Button#getText()}.
     * <p>
     * Caption is generally displayed directly on the component (e.g. the Button
     * text), while label is displayed next to the component in a layout (e.g. a
     * {@link TextField} nested in a form layout).
     *
     * @param button
     *            the button, not null.
     * @return the button text.
     */
    public static String getCaption(Button button) {
        return button.getText();
    }

    /**
     * Sets the Button's caption. Alias for {@link Button#setText(String)}.
     *
     * @param button
     *            the button, not null.
     * @param value
     *            the button text.
     */
    public static void setCaption(Button button, String value) {
        button.setText(value);
    }

    /**
     * Returns the component placed in given slot of this component.
     *
     * @param component
     *            the component, not null.
     * @param slotName
     *            the slot name.
     * @return the first component in the slot, or null if the slot is empty.
     */
    public static Component getChildComponentInSlot(HasElement component,
            String slotName) {
        List<Element> children = ElementUtils
                .getChildrenInSlot(component.getElement(), slotName);
        return children.isEmpty() ? null : children.get(0).getComponent().get();
    }

    /**
     * Replaces the contents of given slot of this component with given
     * component.
     *
     * @param component
     *            the component, not null.
     * @param slotName
     *            the slot name.
     * @param child
     *            the component to place in the slot, or null to just clear the
     *            slot.
     */
    public static void setChildComponentToSlot(HasElement component,
            String slotName, Component child) {
        ElementUtils.clearSlot(component.getElement(), slotName);
        if (child != null) {
            child.getElement().setAttribute("slot", slotName);
            component.getElement().appendChild(child.getElement());
        }
    }

    /**
     * Checks whether given component is a {@code PolymerTemplate}.
     *
     * @param component
     *            the component, not null.
     * @return true if the component is a {@code PolymerTemplate}.
     */
    public static boolean isPolymerTemplate(Component component) {
        return Utils.isPolymerTemplate(component);
    }
}
