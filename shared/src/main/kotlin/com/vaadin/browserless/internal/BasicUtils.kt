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
@file:Suppress("FunctionName")

package com.vaadin.browserless.internal

import com.vaadin.flow.component.BlurNotifier
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.FocusNotifier
import com.vaadin.flow.component.Focusable
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.internal.JacksonUtils
import tools.jackson.databind.node.ObjectNode

/**
 * Fires a DOM event on given [component].
 * @param eventType the event type, e.g. "click"
 * @param eventData optional event data, defaults to an empty object.
 */
@JvmOverloads
fun _fireDomEvent(component: Component, eventType: String, eventData: ObjectNode = JacksonUtils.createObjectNode()) {
    _fireDomEvent(component.element, DomEvent(component.element, eventType, eventData))
}

/**
 * Checks whether given [component] is visible (usually [Component.isVisible]
 * but for [Text] the text must be non-empty).
 */
fun _isVisible(component: Component): Boolean = when (component) {
    is Text -> !component.text.isNullOrBlank()   // workaround for https://github.com/vaadin/flow/issues/3201
    else -> component.isVisible
}

/**
 * Checks that given [component] is actually editable by the user:
 * * The component must be effectively visible: it itself must be visible, its parent must be visible and all of its ascendants must be visible.
 *   For the purpose of testing individual components not attached to the [UI], a component may be considered visible even though it's not
 *   currently nested in a [UI].
 * * The component must be effectively enabled: it itself must be enabled, its parent must be enabled and all of its ascendants must be enabled.
 * * If the component is [HasValue], it must not be [HasValue.isReadOnly].
 * @throws IllegalStateException if any of the above doesn't hold.
 */
fun checkEditableByUser(component: Component) {
    check(isEffectivelyVisible(component)) { "The ${toPrettyString(component)} is not effectively visible - either it is hidden, or its ascendant is hidden" }
    val parentNullOrEnabled = !component.parent.isPresent || isEffectivelyEnabled(component.parent.get())
    if (parentNullOrEnabled) {
        check(component.element.isEnabled) { "The ${toPrettyString(component)} is not enabled" }
    }
    check(isEffectivelyEnabled(component)) { "The ${toPrettyString(component)} is nested in a disabled component" }
    if (component is HasValue<*, *>) {
        check(!component.isReadOnly) { "The ${toPrettyString(component)} is read-only" }
    }
    check(component.isAttached) { " The ${toPrettyString(component)} is not attached" }
}

/**
 * Fails if given [component] is editable. See [checkEditableByUser] for more details.
 * @throws AssertionError if the component is editable.
 */
fun expectNotEditableByUser(component: Component) {
    try {
        checkEditableByUser(component)
        throw AssertionError("The ${toPrettyString(component)} is editable")
    } catch (ex: IllegalStateException) {
        // okay
    }
}

internal fun isEffectivelyVisible(component: Component): Boolean =
    _isVisible(component) && (!component.parent.isPresent || isEffectivelyVisible(component.parent.get()))

/**
 * Computes whether given [component] and all of its parents are enabled.
 *
 * Recursively checks that all ancestors are also enabled (the "implicitly disabled" effect, see
 * [com.vaadin.flow.component.HasEnabled.isEnabled] javadoc for more details).
 *
 * Also check that the component is not inert due to there being a modal component.
 *
 * @return false if this component or any of its parent is disabled or is inert.
 */
fun isEffectivelyEnabled(component: Component): Boolean =
    component.element.isEnabled && !component.element.node.isInert

/**
 * Checks whether given [component] matches given spec. All rules are matched except the [SearchSpec.count] rule. The
 * rules are matched against given component only (not against its children).
 */
fun matches(component: Component, spec: SearchSpec<Component>.() -> Unit): Boolean =
        SearchSpec(Component::class.java).apply { spec() }.toPredicate().invoke(component)

/**
 * Fires [FocusNotifier.FocusEvent] on given [component], but only if it's editable.
 */
fun <T> _focus(component: T) where T : Focusable<*>, T : Component {
    checkEditableByUser(component)
    ComponentUtil.fireEvent(component, FocusNotifier.FocusEvent<T>(component, true, null))
}

/**
 * Fires [BlurNotifier.BlurEvent] on given [component], but only if it's editable.
 */
fun <T> _blur(component: T) where T : Focusable<*>, T : Component {
    checkEditableByUser(component)
    ComponentUtil.fireEvent(component, BlurNotifier.BlurEvent<T>(component, true, null))
}

/**
 * Closes given [ui] and simulates the end of the request. The [UI.close] is called
 * unless the UI is already closing, and then the UI is removed from its session,
 * which fires the detach listeners and makes the UI and all of its components
 * detached.
 */
fun _close(ui: UI) {
    if (!ui.isClosing) {
        ui.close()
    }
    // Mock closing of UI after request handled.
    ui.internals.session.removeUI(ui)
}

val _saneFetchLimit: Int get() =
// don't use high value otherwise Vaadin 19+ will calculate negative limit and will pass it to SizeVerifier,
    // failing instantly.
    Int.MAX_VALUE / 1000
