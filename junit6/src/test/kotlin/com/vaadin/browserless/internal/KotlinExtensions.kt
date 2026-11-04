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

// BasicUtilsKt

fun Component._fireDomEvent(eventType: String, eventData: ObjectNode? = null) {
    if (eventData == null) BasicUtilsKt._fireDomEvent(this, eventType)
    else BasicUtilsKt._fireDomEvent(this, eventType, eventData)
}

var Component.id_: String?
    get() = BasicUtilsKt.getId_(this)
    set(value) = BasicUtilsKt.setId_(this, value)

val Component._text: String? get() = BasicUtilsKt.get_text(this)

fun Component.checkEditableByUser() = BasicUtilsKt.checkEditableByUser(this)

fun Component.expectNotEditableByUser() = BasicUtilsKt.expectNotEditableByUser(this)

fun Component.matches(spec: SearchSpec<Component>.() -> Unit): Boolean =
    BasicUtilsKt.matches(this, spec)

fun <T> T._focus() where T : Focusable<*>, T : Component = BasicUtilsKt._focus(this)

fun <T> T._blur() where T : Focusable<*>, T : Component = BasicUtilsKt._blur(this)

fun UI._close() = BasicUtilsKt._close(this)

// ComponentUtilsKt

fun <T : ClickNotifier<*>> T.serverClick() =
    ComponentUtilsKt.serverClick(this, true, 0, 1, false, false, false, false)

fun Component.addContextMenuListener(listener: DomEventListener): DomListenerRegistration =
    ComponentUtilsKt.addContextMenuListener(this, listener)

fun Component.findAncestorOrSelf(predicate: (Component) -> Boolean): Component? =
    ComponentUtilsKt.findAncestorOrSelf(this, predicate)

fun Component.isNestedIn(potentialAncestor: Component): Boolean =
    ComponentUtilsKt.isNestedIn(this, potentialAncestor)

fun HasOrderedComponents.insertBefore(newComponent: Component, existing: Component) =
    ComponentUtilsKt.insertBefore(this, newComponent, existing)

val HasComponents.hasChildren: Boolean get() = ComponentUtilsKt.getHasChildren(this)

fun HasStyle.addClassNames2(vararg classNames: String) =
    ComponentUtilsKt.addClassNames2(this, *classNames)

fun HasStyle.removeClassNames2(vararg classNames: String) =
    ComponentUtilsKt.removeClassNames2(this, *classNames)

fun HasStyle.setClassNames2(vararg classNames: String) =
    ComponentUtilsKt.setClassNames2(this, *classNames)

var Component.placeholder: String?
    get() = ComponentUtilsKt.getPlaceholder(this)
    set(value) = ComponentUtilsKt.setPlaceholder(this, value)

var Component.label: String
    get() = ComponentUtilsKt.getLabel(this)
    set(value) = ComponentUtilsKt.setLabel(this, value)

@Suppress("DEPRECATION")
var Component.caption: String
    get() = ComponentUtilsKt.getCaption(this)
    set(value) = ComponentUtilsKt.setCaption(this, value)

var Button.caption: String
    get() = ComponentUtilsKt.getCaption(this)
    set(value) = ComponentUtilsKt.setCaption(this, value)

// ElementUtilsKt

fun Element.setOrRemoveAttribute(attribute: String, value: String?) =
    ElementUtilsKt.setOrRemoveAttribute(this, attribute, value)

fun ClassList.toggle(className: String) = ElementUtilsKt.toggle(this, className)

fun Element.insertBefore(newNode: Element, existingNode: Element) =
    ElementUtilsKt.insertBefore(this, newNode, existingNode)

val Element.textRecursively2: String get() = ElementUtilsKt.getTextRecursively2(this)

fun Element.getVirtualChildren(): List<Element> = ElementUtilsKt.getVirtualChildren(this)

fun Element.getChildrenInSlot(slotName: String): List<Element> =
    ElementUtilsKt.getChildrenInSlot(this, slotName)

fun Element.clearSlot(slotName: String) = ElementUtilsKt.clearSlot(this, slotName)

fun Element._fireDomEvent(event: DomEvent) = ElementUtilsKt._fireDomEvent(this, event)

// UtilsKt

val currentRequest: VaadinRequest get() = UtilsKt.getCurrentRequest()

val currentResponse: VaadinResponse get() = UtilsKt.getCurrentResponse()

val currentUI: UI get() = UtilsKt.getCurrentUI()

val VaadinRequest.mock: MockRequest get() = UtilsKt.getMock(this)

val VaadinResponse.mock: MockResponse get() = UtilsKt.getMock(this)

val VaadinSession.mock: MockHttpSession get() = UtilsKt.getMock(this)

// LocatorKt

inline fun <reified T : Component> Component._get(noinline block: SearchSpec<T>.() -> Unit = {}): T =
    LocatorKt._get(this, T::class.java, block)

inline fun <reified T : Component> _get(noinline block: SearchSpec<T>.() -> Unit = {}): T =
    LocatorKt._get(T::class.java, block)

inline fun <reified T : Component> Component._find(noinline block: SearchSpec<T>.() -> Unit = {}): List<T> =
    LocatorKt._find(this, T::class.java, block)

inline fun <reified T : Component> _find(noinline block: SearchSpec<T>.() -> Unit = {}): List<T> =
    LocatorKt._find(T::class.java, block)

inline fun <reified T : Component> Component._expectNone(noinline block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectNone(this, T::class.java, block)

inline fun <reified T : Component> _expectNone(noinline block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectNone(T::class.java, block)

inline fun <reified T : Component> Component._expectOne(noinline block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectOne(this, T::class.java, block)

inline fun <reified T : Component> _expectOne(noinline block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectOne(T::class.java, block)

inline fun <reified T : Component> Component._expect(count: Int = 1, noinline block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expect(this, T::class.java, count, block)

inline fun <reified T : Component> _expect(count: Int = 1, noinline block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expect(T::class.java, count, block)

fun <T : Component> Component._get(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): T =
    LocatorKt._get(this, clazz, block)

fun <T : Component> _get(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): T =
    LocatorKt._get(clazz, block)

fun <T : Component> Component._find(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): List<T> =
    LocatorKt._find(this, clazz, block)

fun <T : Component> _find(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): List<T> =
    LocatorKt._find(clazz, block)

fun <T : Component> Component._expectNone(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectNone(this, clazz, block)

fun <T : Component> _expectNone(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectNone(clazz, block)

fun <T : Component> Component._expectOne(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectOne(this, clazz, block)

fun <T : Component> _expectOne(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expectOne(clazz, block)

fun <T : Component> Component._expect(clazz: Class<T>, count: Int = 1, block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expect(this, clazz, count, block)

fun <T : Component> _expect(clazz: Class<T>, count: Int = 1, block: SearchSpec<T>.() -> Unit = {}) =
    LocatorKt._expect(clazz, count, block)

fun _expectNoDialogs() = LocatorKt._expectNoDialogs()

fun _expectInternalServerError(expectedErrorMessage: String = "") =
    LocatorKt._expectInternalServerError(expectedErrorMessage)

// PrettyPrintTreeKt, TestingLifecycleHookKt, ShortcutsKt, DepthFirstTreeIteratorKt

fun Component.toPrettyTree(): String = PrettyPrintTreeKt.toPrettyTree(this)

fun Component.toPrettyString(): String = PrettyPrintTreeKt.toPrettyString(this)

var testingLifecycleHook: TestingLifecycleHook
    get() = TestingLifecycleHookKt.getTestingLifecycleHook()
    set(value) = TestingLifecycleHookKt.setTestingLifecycleHook(value)

val Component.isTemplate: Boolean get() = TestingLifecycleHookKt.isTemplate(this)

fun cleanupDialogs() = TestingLifecycleHookKt.cleanupDialogs()

fun fireShortcut(key: Key, vararg modifiers: Key) = ShortcutsKt.fireShortcut(key, *modifiers)

fun Component.walk(): Iterable<Component> = DepthFirstTreeIteratorKt.walk(this)
