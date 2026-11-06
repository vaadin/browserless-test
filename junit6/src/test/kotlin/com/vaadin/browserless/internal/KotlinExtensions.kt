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

import com.vaadin.flow.component.ClickNotifier
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Focusable
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.HasOrderedComponents
import com.vaadin.flow.component.HasStyle
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.dom.ClassList
import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.dom.DomEventListener
import com.vaadin.flow.dom.DomListenerRegistration
import com.vaadin.flow.dom.Element
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.VaadinSession
import com.vaadin.browserless.mocks.MockHttpSession
import com.vaadin.browserless.mocks.MockRequest
import com.vaadin.browserless.mocks.MockResponse
import tools.jackson.databind.node.ObjectNode

/*
 * Kotlin extension-syntax wrappers over the Java helpers in this package, so
 * the Kotlin tests can keep calling them as `component.toPrettyString()`,
 * `_get<Button> { ... }` and so on.
 */

// BasicUtils

fun Component._fireDomEvent(eventType: String, eventData: ObjectNode? = null) {
    if (eventData == null) BasicUtils._fireDomEvent(this, eventType)
    else BasicUtils._fireDomEvent(this, eventType, eventData)
}

var Component.id_: String?
    get() = BasicUtils.getId_(this)
    set(value) = BasicUtils.setId_(this, value)

val Component._text: String? get() = BasicUtils.get_text(this)

fun Component.checkEditableByUser() = BasicUtils.checkEditableByUser(this)

fun Component.expectNotEditableByUser() = BasicUtils.expectNotEditableByUser(this)

fun Component.matches(spec: SearchSpec<Component>.() -> Unit): Boolean =
    BasicUtils.matches(this, spec)

fun <T> T._focus() where T : Focusable<*>, T : Component = BasicUtils._focus(this)

fun <T> T._blur() where T : Focusable<*>, T : Component = BasicUtils._blur(this)

fun UI._close() = BasicUtils._close(this)

// ComponentUtils

fun <T : ClickNotifier<*>> T.serverClick() =
    ComponentUtils.serverClick(this, true, 0, 1, false, false, false, false)

fun Component.addContextMenuListener(listener: DomEventListener): DomListenerRegistration =
    ComponentUtils.addContextMenuListener(this, listener)

fun Component.findAncestorOrSelf(predicate: (Component) -> Boolean): Component? =
    ComponentUtils.findAncestorOrSelf(this, predicate)

fun Component.isNestedIn(potentialAncestor: Component): Boolean =
    ComponentUtils.isNestedIn(this, potentialAncestor)

fun HasOrderedComponents.insertBefore(newComponent: Component, existing: Component) =
    ComponentUtils.insertBefore(this, newComponent, existing)

val HasComponents.hasChildren: Boolean get() = ComponentUtils.getHasChildren(this)

fun HasStyle.addClassNames2(vararg classNames: String) =
    ComponentUtils.addClassNames2(this, *classNames)

fun HasStyle.removeClassNames2(vararg classNames: String) =
    ComponentUtils.removeClassNames2(this, *classNames)

fun HasStyle.setClassNames2(vararg classNames: String) =
    ComponentUtils.setClassNames2(this, *classNames)

var Component.placeholder: String?
    get() = ComponentUtils.getPlaceholder(this)
    set(value) = ComponentUtils.setPlaceholder(this, value)

var Component.label: String
    get() = ComponentUtils.getLabel(this)
    set(value) = ComponentUtils.setLabel(this, value)

@Suppress("DEPRECATION")
var Component.caption: String
    get() = ComponentUtils.getCaption(this)
    set(value) = ComponentUtils.setCaption(this, value)

var Button.caption: String
    get() = ComponentUtils.getCaption(this)
    set(value) = ComponentUtils.setCaption(this, value)

// ElementUtils

fun Element.setOrRemoveAttribute(attribute: String, value: String?) =
    ElementUtils.setOrRemoveAttribute(this, attribute, value)

fun ClassList.toggle(className: String) = ElementUtils.toggle(this, className)

fun Element.insertBefore(newNode: Element, existingNode: Element) =
    ElementUtils.insertBefore(this, newNode, existingNode)

val Element.textRecursively2: String get() = ElementUtils.getTextRecursively2(this)

fun Element.getVirtualChildren(): List<Element> = ElementUtils.getVirtualChildren(this)

fun Element.getChildrenInSlot(slotName: String): List<Element> =
    ElementUtils.getChildrenInSlot(this, slotName)

fun Element.clearSlot(slotName: String) = ElementUtils.clearSlot(this, slotName)

fun Element._fireDomEvent(event: DomEvent) = ElementUtils._fireDomEvent(this, event)

// Utils

val currentRequest: VaadinRequest get() = Utils.getCurrentRequest()

val currentResponse: VaadinResponse get() = Utils.getCurrentResponse()

val currentUI: UI get() = Utils.getCurrentUI()

val VaadinRequest.mock: MockRequest get() = Utils.getMock(this)

val VaadinResponse.mock: MockResponse get() = Utils.getMock(this)

val VaadinSession.mock: MockHttpSession get() = Utils.getMock(this)

// Locator

inline fun <reified T : Component> Component._get(noinline block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(this, T::class.java, block)

inline fun <reified T : Component> _get(noinline block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(T::class.java, block)

inline fun <reified T : Component> Component._find(noinline block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(this, T::class.java, block)

inline fun <reified T : Component> _find(noinline block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(T::class.java, block)

inline fun <reified T : Component> Component._expectNone(noinline block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectNone(this, T::class.java, block)

inline fun <reified T : Component> _expectNone(noinline block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectNone(T::class.java, block)

inline fun <reified T : Component> Component._expectOne(noinline block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectOne(this, T::class.java, block)

inline fun <reified T : Component> _expectOne(noinline block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectOne(T::class.java, block)

inline fun <reified T : Component> Component._expect(count: Int = 1, noinline block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expect(this, T::class.java, count, block)

inline fun <reified T : Component> _expect(count: Int = 1, noinline block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expect(T::class.java, count, block)

fun <T : Component> Component._get(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(this, clazz, block)

fun <T : Component> _get(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(clazz, block)

fun <T : Component> Component._find(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(this, clazz, block)

fun <T : Component> _find(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(clazz, block)

fun <T : Component> Component._expectNone(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectNone(this, clazz, block)

fun <T : Component> _expectNone(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectNone(clazz, block)

fun <T : Component> Component._expectOne(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectOne(this, clazz, block)

fun <T : Component> _expectOne(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expectOne(clazz, block)

fun <T : Component> Component._expect(clazz: Class<T>, count: Int = 1, block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expect(this, clazz, count, block)

fun <T : Component> _expect(clazz: Class<T>, count: Int = 1, block: SearchSpec<T>.() -> Unit = {}) =
    Locator._expect(clazz, count, block)

fun _expectNoDialogs() = Locator._expectNoDialogs()

fun _expectInternalServerError(expectedErrorMessage: String = "") =
    Locator._expectInternalServerError(expectedErrorMessage)

// PrettyPrintTree, TestingLifecycleHooks, Shortcuts, DepthFirstTreeIterator

fun Component.toPrettyTree(): String = PrettyPrintTree.toPrettyTree(this)

fun Component.toPrettyString(): String = PrettyPrintTree.toPrettyString(this)

var testingLifecycleHook: TestingLifecycleHook
    get() = TestingLifecycleHooks.getTestingLifecycleHook()
    set(value) = TestingLifecycleHooks.setTestingLifecycleHook(value)

val Component.isTemplate: Boolean get() = TestingLifecycleHooks.isTemplate(this)

fun cleanupDialogs() = TestingLifecycleHooks.cleanupDialogs()

fun fireShortcut(key: Key, vararg modifiers: Key) = Shortcuts.fireShortcut(key, *modifiers)

fun Component.walk(): Iterable<Component> = DepthFirstTreeIterator.walk(this)
