/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.listbox.ListBoxBase;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Location;

public final class ComponentUtils {

    private ComponentUtils() {
    }

    /**
     * Fires given event on the component.
     */
    public static void fireEvent(Component component, ComponentEvent<?> event) {
        ComponentUtil.fireEvent(component, event);
    }

    /**
     * Adds [com.vaadin.flow.component.button.Button.click] functionality to all [ClickNotifier]s. This function directly calls
     * all click listeners, thus it avoids the roundtrip to client and back. It even works with browserless testing.
     */
    public static <T extends ClickNotifier<?>> void serverClick(T notifier) {
        serverClick(notifier, true, 0, 1, false, false, false, false);
    }

    public static <T extends ClickNotifier<?>> void serverClick(T notifier, boolean fromClient, int button,
            int clickCount, boolean shiftKey, boolean ctrlKey, boolean altKey, boolean metaKey) {
        Component component = (Component) notifier;
        fireEvent(component, new ClickEvent<>(component, fromClient, -1, -1, -1, -1, clickCount, button, ctrlKey,
                shiftKey, altKey, metaKey));
    }

    /**
     * Sets the alignment of the text in the component. One of `center`, `left`, `right`, `justify`.
     */
    public static String textAlign(Component component) {
        return component.getElement().getStyle().get("textAlign");
    }

    public static void textAlign(Component component, String value) {
        component.getElement().getStyle().set("textAlign", value);
    }

    /**
     * Sets or removes the `title` attribute on component's element.
     */
    public static String tooltip(Component component) {
        return component.getElement().getAttribute("title");
    }

    public static void tooltip(Component component, String value) {
        ElementUtils.setOrRemoveAttribute(component.getElement(), "title", value);
    }

    /**
     * Adds the right-click (context-menu) [listener] to the component. Also causes the right-click browser
     * menu not to be shown on this component (see [preventDefault]).
     */
    public static DomListenerRegistration addContextMenuListener(Component component, DomEventListener listener) {
        return preventDefault(component.getElement().addEventListener("contextmenu", listener));
    }

    /**
     * Makes the client-side listener call [Event.preventDefault()](https://developer.mozilla.org/en-US/docs/Web/API/Event/preventDefault)
     * on the event.
     *
     * @return this
     */
    public static DomListenerRegistration preventDefault(DomListenerRegistration registration) {
        return registration.addEventData("event.preventDefault()");
    }

    /**
     * Removes the component from its parent. Does nothing if the component is not attached to a parent.
     */
    public static void removeFromParent(Component component) {
        Component parent = component.getParent().orElse(null);
        if (parent instanceof HasComponents) {
            ((HasComponents) parent).remove(component);
        }
    }

    /**
     * Finds component's parent, parent's parent (etc) which satisfies given [predicate].
     * Returns null if there is no such parent.
     */
    public static Component findAncestor(Component component, Predicate<Component> predicate) {
        return findAncestorOrSelf(component, c -> c != component && predicate.test(c));
    }

    /**
     * Finds component, component's parent, parent's parent (etc) which satisfies given [predicate].
     * Returns null if no component on the ancestor-or-self axis satisfies.
     */
    public static Component findAncestorOrSelf(Component component, Predicate<Component> predicate) {
        Component current = component;
        while (current != null) {
            if (predicate.test(current)) {
                return current;
            }
            current = current.getParent().orElse(null);
        }
        return null;
    }

    /**
     * Checks if this component is nested in [potentialAncestor].
     */
    public static boolean isNestedIn(Component component, Component potentialAncestor) {
        return findAncestor(component, c -> c == potentialAncestor) != null;
    }

    /**
     * Checks whether this component is currently attached to a [UI].
     *
     * Returns true for attached components even if the UI itself is closed.
     */
    public static boolean isAttached(Component component) {
        // see https://github.com/vaadin/flow/issues/7911
        return component.getElement().getNode().isAttached();
    }

    /**
     * Returns the data provider currently set to this Component.
     *
     * Works both with Vaadin 16 and Vaadin 17: Vaadin 17 components no longer implement HasItems.
     */
    public static DataProvider<?, ?> dataProvider(Component component) {
        try {
            // until https://github.com/vaadin/flow/issues/6296 is resolved
            if (component instanceof Grid) {
                return ((Grid<?>) component).getDataProvider();
            }
            if (component instanceof Select) {
                return ((Select<?>) component).getDataProvider();
            }
            if (component instanceof ListBoxBase) {
                return (DataProvider<?, ?>) _ListBoxBase_getDataProvider.invoke(component);
            }
            if (component instanceof RadioButtonGroup) {
                return (DataProvider<?, ?>) _RadioButtonGroup_getDataProvider.invoke(component);
            }
            if (component instanceof CheckboxGroup) {
                return (DataProvider<?, ?>) _CheckboxGroup_getDataProvider.invoke(component);
            }
            if (component instanceof ComboBox) {
                return ((ComboBox<?>) component).getDataProvider();
            }
            return null;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts this component as a child, right before an [existing] one.
     *
     * In case the specified component has already been added to another parent,
     * it will be removed from there and added to this one.
     */
    public static void insertBefore(HasOrderedComponents container, Component newComponent, Component existing) {
        Component parent = existing.getParent().orElse(null);
        if (parent == null) {
            throw new IllegalArgumentException(existing + " has no parent");
        }
        if (parent != container) {
            throw new IllegalArgumentException(existing + " is not nested in " + container);
        }
        container.addComponentAtIndex(container.indexOf(existing), newComponent);
    }

    /**
     * Return the location of the currently shown view. The function will report the current (old)
     * view in [com.vaadin.flow.router.BeforeLeaveEvent] and [com.vaadin.flow.router.BeforeEnterEvent].
     */
    public static Location currentViewLocation(UI ui) {
        return ui.getInternals().getActiveViewLocation();
    }

    /**
     * True when the component has any children.
     */
    public static boolean hasChildren(HasComponents container) {
        return ((Component) container).getChildren().findFirst().isPresent();
    }

    /**
     * Splits [classNames] by whitespaces to obtain individual class names, then
     * calls [HasStyle.addClassName] on each class name. Does nothing if the string
     * is blank.
     */
    public static void addClassNames2(HasStyle target, String classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        for (String name : Utils.splitByWhitespaces(classNames)) {
            target.addClassName(name);
        }
    }

    /**
     * Splits [classNames] by whitespaces to obtain individual class names, then
     * calls [addClassNames2] on each class name. Does nothing if the string
     * is blank.
     */
    public static void addClassNames2(HasStyle target, String... classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        for (String c : classNames) {
            addClassNames2(target, c);
        }
    }

    /**
     * Splits [classNames] by whitespaces to obtain individual class names, then
     * calls [HasStyle.removeClassName] on each class name. Does nothing if the string
     * is blank.
     */
    public static void removeClassNames2(HasStyle target, String classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        for (String name : Utils.splitByWhitespaces(classNames)) {
            target.removeClassName(name);
        }
    }

    /**
     * Splits [classNames] by whitespaces to obtain individual class names, then
     * calls [removeClassNames2] on each class name. Does nothing if the string
     * is blank.
     */
    public static void removeClassNames2(HasStyle target, String... classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        for (String c : classNames) {
            removeClassNames2(target, c);
        }
    }

    /**
     * Splits [classNames] by whitespaces to obtain individual class names, then
     * clears the class names and calls [addClassNames2] on each class name. Does nothing if the string
     * is blank.
     */
    public static void setClassNames2(HasStyle target, String classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        target.getStyle().clear();
        addClassNames2(target, classNames);
    }

    /**
     * Splits [classNames] by whitespaces to obtain individual class names, then
     * clears the class names and calls [addClassNames2] on each class name. Does nothing if the string
     * is blank.
     */
    public static void setClassNames2(HasStyle target, String... classNames) {
        // workaround for https://github.com/vaadin/flow/issues/11709
        target.getStyle().clear();
        addClassNames2(target, classNames);
    }

    /**
     * A component placeholder, usually shown when there's no value selected.
     * Not all components support a placeholder; those that don't will return null.
     */
    public static String placeholder(Component component) {
        // modify when this is fixed: https://github.com/vaadin/flow/issues/4068
        if (component instanceof TextField) {
            return ((TextField) component).getPlaceholder();
        }
        if (component instanceof TextArea) {
            return ((TextArea) component).getPlaceholder();
        }
        if (component instanceof PasswordField) {
            return ((PasswordField) component).getPlaceholder();
        }
        if (component instanceof ComboBox) {
            return ((ComboBox<?>) component).getPlaceholder();
        }
        if (component instanceof DatePicker) {
            return ((DatePicker) component).getPlaceholder();
        }
        if (component instanceof HasPlaceholder) {
            return ((HasPlaceholder) component).getPlaceholder();
        }
        return null;
    }

    public static void placeholder(Component component, String value) {
        if (component instanceof TextField) {
            ((TextField) component).setPlaceholder(value);
        } else if (component instanceof TextArea) {
            ((TextArea) component).setPlaceholder(value);
        } else if (component instanceof PasswordField) {
            ((PasswordField) component).setPlaceholder(value);
        } else if (component instanceof ComboBox) {
            ((ComboBox<?>) component).setPlaceholder(value);
        } else if (component instanceof DatePicker) {
            ((DatePicker) component).setPlaceholder(value);
        } else if (component instanceof HasPlaceholder) {
            ((HasPlaceholder) component).setPlaceholder(value);
        } else {
            throw new IllegalStateException(
                    component.getClass().getSimpleName() + " doesn't support setting placeholder");
        }
    }

    /**
     * Concatenates texts from all elements placed in the `label` slot. This effectively
     * returns whatever was provided in the String label via [FormLayout.addFormItem].
     */
    public static String label(FormLayout.FormItem item) {
        List<Component> captions = item.getChildren()
                .filter(c -> "label".equals(c.getElement().getAttribute("slot")))
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (Component c : captions) {
            if (c instanceof HasText) {
                String text = ((HasText) c).getText();
                if (text != null) {
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }

    /**
     * The `HasLabel` interface has been introduced in Vaadin 21 but is missing in Vaadin 14.
     * Use reflection.
     */
    private static final Class<?> _HasLabel = Utils.findClass("com.vaadin.flow.component.HasLabel");
    private static final Method _HasLabel_getLabel;
    private static final Method _HasLabel_setLabel;
    static {
        Method get = null;
        Method set = null;
        if (_HasLabel != null) {
            try {
                get = _HasLabel.getDeclaredMethod("getLabel");
                set = _HasLabel.getDeclaredMethod("setLabel", String.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        _HasLabel_getLabel = get;
        _HasLabel_setLabel = set;
    }

    /**
     * Determines the component's `label` (usually it's the HTML element's `label` property, but it's [Checkbox.getLabel] for checkbox).
     * Intended to be used for fields such as [TextField].
     *
     * *For `FormItem`:* Concatenates texts from all elements placed in the `label` slot. This effectively
     * returns whatever was provided in the String label via [FormLayout.addFormItem].
     *
     * [Button.caption] is displayed directly on the component
     * while label is displayed next to the component in a layout (e.g. a [TextField] nested in a form layout).
     *
     * Vote for [issue #3241](https://github.com/vaadin/flow/issues/3241).
     *
     * **WARNING:** the label is displayed by the component itself, rather than by the parent layout.
     * If a component doesn't contain necessary machinery
     * to display a label, setting this property will have no visual effect.
     * For example, setting a label to a [FormLayout]
     * nested within a `VerticalLayout`
     * will show nothing since [FormLayout] doesn't display a label itself.
     * See [LabelWrapper] for a list of possible solutions.
     */
    public static String label(Component component) {
        try {
            if (_HasLabel != null && _HasLabel.isInstance(component)) {
                String value = (String) _HasLabel_getLabel.invoke(component);
                return value == null ? "" : value;
            }
            if (component instanceof Checkbox) {
                String v = ((Checkbox) component).getLabel();
                return v == null ? "" : v;
            }
            if (component instanceof FormLayout.FormItem) {
                return label((FormLayout.FormItem) component);
            }
            if (component instanceof SideNav) {
                String v = ((SideNav) component).getLabel();
                return v == null ? "" : v;
            }
            if (component instanceof SideNavItem) {
                String v = ((SideNavItem) component).getLabel();
                return v == null ? "" : v;
            }
            String v = component.getElement().getProperty("label");
            return v == null ? "" : v;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void label(Component component, String value) {
        try {
            if (_HasLabel != null && _HasLabel.isInstance(component)) {
                _HasLabel_setLabel.invoke(component, value);
            } else if (component instanceof Checkbox) {
                ((Checkbox) component).setLabel(value);
            } else if (component instanceof FormLayout.FormItem) {
                throw new IllegalArgumentException("Setting the caption of FormItem is currently unsupported");
            } else if (component instanceof SideNav) {
                ((SideNav) component).setLabel(value);
            } else if (component instanceof SideNavItem) {
                ((SideNavItem) component).setLabel(value);
            } else {
                component.getElement().setProperty("label", value == null || value.isBlank() ? null : value);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The Component's caption: [Button.getText] for [Button], [label] for fields such as [TextField].
     *
     * Caption is generally displayed directly on the component (e.g. the Button text),
     * while [label] is displayed next to the component in a layout (e.g. a [TextField] nested in a form layout).
     *
     * **Deprecated:** this property was intended to unify captions and labels, but only managed to
     * create confusion between the two concepts. Also, there's only a [Button] which
     * has the notion of a caption. Will be removed with no replacement.
     */
    @Deprecated
    public static String caption(Component component) {
        if (component instanceof Button) {
            return ((Button) component).getText();
        }
        return label(component);
    }

    @Deprecated
    public static void caption(Component component, String value) {
        if (component instanceof Button) {
            ((Button) component).setText(value);
        } else {
            label(component, value);
        }
    }

    /**
     * Sets up an event listener for overlay components that fires a `closed` DOM
     * event when the component is closed. This simulates the event being fired
     * from the browser after the closing animation has finished.
     */
    public static void simulateClosedEvent(Component component) {
        if (ComponentUtil.getData(component, "hasSimulatedClosedEvent") != null) {
            return;
        }
        if (component instanceof Dialog || component instanceof ConfirmDialog || component instanceof LoginOverlay
                || component instanceof ContextMenu || component instanceof Notification) {
            ComponentUtil.setData(component, "hasSimulatedClosedEvent", true);
            component.getElement().addPropertyChangeListener("opened", event -> {
                if (Boolean.FALSE.equals(event.getValue())) {
                    BasicUtils._fireDomEvent(component, "closed");
                }
            });
        }
    }

    static Component getChildComponentInSlot(HasElement parent, String slotName) {
        return ElementUtils.getChildrenInSlot(parent.getElement(), slotName).stream()
                .findFirst()
                .flatMap(Element::getComponent)
                .orElse(null);
    }

    static void setChildComponentToSlot(HasElement parent, String slotName, Component component) {
        ElementUtils.clearSlot(parent.getElement(), slotName);
        if (component != null) {
            component.getElement().setAttribute("slot", slotName);
            parent.getElement().appendChild(component.getElement());
        }
    }

    static boolean isPolymerTemplate(Component component) {
        return Utils.polymerTemplateClass != null
                && Utils.polymerTemplateClass.isAssignableFrom(component.getClass());
    }

    private static final Method _ListBoxBase_getDataProvider;
    private static final Method _CheckboxGroup_getDataProvider;
    private static final Method _RadioButtonGroup_getDataProvider;
    static {
        try {
            _ListBoxBase_getDataProvider = ListBoxBase.class.getDeclaredMethod("getDataProvider");
            _ListBoxBase_getDataProvider.setAccessible(true);
            _CheckboxGroup_getDataProvider = CheckboxGroup.class.getDeclaredMethod("getDataProvider");
            _CheckboxGroup_getDataProvider.setAccessible(true);
            _RadioButtonGroup_getDataProvider = RadioButtonGroup.class.getDeclaredMethod("getDataProvider");
            _RadioButtonGroup_getDataProvider.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
