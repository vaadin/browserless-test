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
package com.vaadin.browserless.internal

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.HasLabel
import com.vaadin.flow.component.HasPlaceholder
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.contextmenu.ContextMenuBase
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.login.LoginOverlay
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.shared.SlotUtils
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import java.util.stream.Collectors

/**
 * Returns the placeholder of given [component], usually shown when there's no
 * value selected. Returns null for components that don't implement
 * [HasPlaceholder].
 */
fun getPlaceholder(component: Component): String? =
    (component as? HasPlaceholder)?.placeholder

/**
 * Joins the (recursive) texts of all elements placed in the `label` slot of
 * given [formItem] with a space, collapsing runs of whitespace and trimming the
 * result. This effectively returns whatever was provided in the String label
 * via [FormLayout.addFormItem].
 */
fun getLabel(formItem: FormLayout.FormItem): String =
    SlotUtils.getElementsInSlot(formItem, "label")
        .map { it.textRecursively }
        .collect(Collectors.joining(" "))
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Determines the `label` of given [component]: [HasLabel.getLabel] for
 * components that have one, the text in the `label` slot for a
 * [FormLayout.FormItem], and the `label` property of the element otherwise.
 * Returns an empty string when there is no label.
 */
fun getLabel(component: Component): String = when (component) {
    is HasLabel -> component.label ?: ""
    is Checkbox -> component.label ?: ""
    is FormLayout.FormItem -> getLabel(component)
    is SideNav -> component.label ?: ""
    is SideNavItem -> component.label ?: ""
    else -> component.element.getProperty("label") ?: ""
}

/**
 * Determines the caption of given [component]: [Button.getText] for [Button],
 * [getLabel] for everything else.
 */
fun getCaption(component: Component): String = when (component) {
    is Button -> component.text
    else -> getLabel(component)
}

/**
 * Sets up an event listener for overlay components that fires a `closed` DOM
 * event when the component is closed. This simulates the event being fired
 * from the browser after the closing animation has finished.
 */
fun simulateClosedEvent(component: Component) {
    if (ComponentUtil.getData(component, "hasSimulatedClosedEvent") != null) {
        return
    }
    when (component) {
        is Dialog, is ConfirmDialog, is LoginOverlay, is ContextMenuBase<*, *, *>, is Notification -> {
            ComponentUtil.setData(component, "hasSimulatedClosedEvent", true)
            component.element.addPropertyChangeListener("opened") { event ->
                if (event.value == false) {
                    _fireDomEvent(component, "closed")
                }
            }
        }
    }
}

internal fun isPolymerTemplate(component: Component): Boolean {
    return polymerTemplateClass != null
            && polymerTemplateClass.isAssignableFrom(component.javaClass);
}
