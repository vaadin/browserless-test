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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenuBase;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.shared.SlotUtils;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;

/**
 * Utilities for working with {@link Component}s.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ComponentUtils {

    private ComponentUtils() {
    }

    /**
     * Returns the placeholder of given component, usually shown when there's no
     * value selected. Returns null for components that don't implement
     * {@link HasPlaceholder}.
     *
     * @param component
     *            the component, not null.
     * @return the placeholder, or null.
     */
    public static String getPlaceholder(Component component) {
        if (component instanceof HasPlaceholder hasPlaceholder) {
            return hasPlaceholder.getPlaceholder();
        }
        return null;
    }

    /**
     * Joins the (recursive) texts of all elements placed in the {@code label}
     * slot of given form item with a space, collapsing runs of whitespace and
     * trimming the result. This effectively returns whatever was provided in
     * the String label via {@link FormLayout#addFormItem(Component, String)}.
     *
     * @param formItem
     *            the form item, not null.
     * @return the label.
     */
    public static String getLabel(FormLayout.FormItem formItem) {
        String joined = SlotUtils.getElementsInSlot(formItem, "label")
                .map(Element::getTextRecursively)
                .collect(Collectors.joining(" "));
        return Utils.trim(joined.replaceAll("\\s+", " "));
    }

    /**
     * Determines the {@code label} of given component:
     * {@link HasLabel#getLabel()} for components that have one, the text in the
     * {@code label} slot for a {@link FormLayout.FormItem}, and the
     * {@code label} property of the element otherwise. Returns an empty string
     * when there is no label.
     *
     * @param component
     *            the component, not null.
     * @return the label, never null.
     */
    public static String getLabel(Component component) {
        String label;
        if (component instanceof HasLabel hasLabel) {
            label = hasLabel.getLabel();
        } else if (component instanceof Checkbox checkbox) {
            label = checkbox.getLabel();
        } else if (component instanceof FormLayout.FormItem formItem) {
            label = getLabel(formItem);
        } else if (component instanceof SideNav sideNav) {
            label = sideNav.getLabel();
        } else if (component instanceof SideNavItem sideNavItem) {
            label = sideNavItem.getLabel();
        } else {
            label = component.getElement().getProperty("label");
        }
        return Objects.requireNonNullElse(label, "");
    }

    /**
     * Determines the caption of given component: {@link Button#getText()} for
     * {@link Button}, {@link #getLabel(Component)} for everything else.
     *
     * @param component
     *            the component, not null.
     * @return the caption.
     */
    public static String getCaption(Component component) {
        if (component instanceof Button button) {
            return button.getText();
        }
        return getLabel(component);
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
        if (ComponentUtil.getData(component,
                "hasSimulatedClosedEvent") != null) {
            return;
        }
        if (component instanceof Dialog || component instanceof ConfirmDialog
                || component instanceof LoginOverlay
                || component instanceof ContextMenuBase<?, ?, ?>
                || component instanceof Notification) {
            ComponentUtil.setData(component, "hasSimulatedClosedEvent", true);
            component.getElement().addPropertyChangeListener("opened",
                    event -> {
                        if (Boolean.FALSE.equals(event.getValue())) {
                            BasicUtils._fireDomEvent(component, "closed");
                        }
                    });
        }
    }

    /**
     * Checks whether given component is a {@code PolymerTemplate}.
     *
     * @param component
     *            the component, not null.
     * @return true if the component is a {@code PolymerTemplate}.
     */
    static boolean isPolymerTemplate(Component component) {
        Class<?> polymerTemplateClass = Utils.getPolymerTemplateClass();
        return polymerTemplateClass != null
                && polymerTemplateClass.isAssignableFrom(component.getClass());
    }

    /**
     * Checks whether the given component is a Lit or Polymer template.
     *
     * @param component
     *            the component.
     * @return true if the component is a template.
     */
    public static boolean isTemplate(Component component) {
        return component instanceof LitTemplate
                || ComponentUtils.isPolymerTemplate(component);
    }

    /**
     * Checks whether given component is a dialog and needs to be removed from
     * the UI. See {@link #cleanupDialogs()} for more info.
     */
    private static boolean isDialogAndNeedsRemoval(Component component) {
        if (component instanceof Dialog dialog && !dialog.isOpened()) {
            return true;
        }
        if (component instanceof ConfirmDialog dialog && !dialog.isOpened()) {
            return true;
        }
        return false;
    }

    /**
     * Flow Server does not close the dialog when {@link Dialog#close()} is
     * called; instead it tells client-side dialog to close, which then fires
     * event back to the server that the dialog was closed, and removes itself
     * from the DOM. Since there's no browser with browserless testing, we need
     * to cleanup closed dialogs manually, hence this method.
     */
    public static void cleanupDialogs() {
        // Starting with Vaadin 23, nested dialogs are also nested within
        // respective modal dialog within the UI. This is probably related to
        // the "server-side modality curtain" feature. Also see
        // https://github.com/mvysny/karibu-testing/issues/102
        List<Component> dialogs = StreamSupport
                .stream(DepthFirstTreeIterator.walk(UI.getCurrent())
                        .spliterator(), false)
                .filter(ComponentUtils::isDialogAndNeedsRemoval)
                .collect(Collectors.toList());
        dialogs.forEach(it -> it.getElement().removeFromParent());
    }
}
