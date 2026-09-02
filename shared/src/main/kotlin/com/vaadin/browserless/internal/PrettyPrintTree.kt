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


import java.util.*
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.Html
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.icon.Icon
import com.vaadin.browserless.internal.PrettyPrintTree.Companion.ofVaadin


/**
 * If true, [PrettyPrintTree] will use `\--` instead of `└──` which tend to render on some terminals as `???`.
 */
var prettyPrintUseAscii: Boolean = false

/**
 * Utility class to create a pretty-printed ASCII tree of arbitrary nodes that can be printed to the console.
 * You can build the tree out of any tree structure, just fill in this node [name] and its [children].
 *
 * To create a pretty tree dump of a Vaadin component, just use [ofVaadin].
 */
class PrettyPrintTree(val name: String, val children: MutableList<PrettyPrintTree>) {

    private val pipe = if (!prettyPrintUseAscii) '│' else '|'
    private val branchTail = if (!prettyPrintUseAscii) "└── " else "\\-- "
    private val branch = if (!prettyPrintUseAscii) "├── " else "|-- "

    fun print(): String {
        val sb = StringBuilder()
        print(sb, "", true)
        return sb.toString()
    }

    private fun print(sb: StringBuilder, prefix: String, isTail: Boolean) {
        sb.append(prefix + (if (isTail) branchTail else branch) + name + "\n")
        for (i in 0 until children.size - 1) {
            children[i].print(sb, prefix + if (isTail) "    " else "$pipe   ", false)
        }
        if (children.size > 0) {
            children[children.size - 1]
                    .print(sb, prefix + if (isTail) "    " else "$pipe   ", true)
        }
    }

    companion object {

        fun ofVaadin(root: Component): PrettyPrintTree {
            val result = PrettyPrintTree(root.toPrettyString(), mutableListOf())
            for (child: Component in testingLifecycleHook.getAllChildren(root)) {
                result.children.add(ofVaadin(child))
            }
            return result
        }
    }
}

fun Component.toPrettyTree(): String = PrettyPrintTree.ofVaadin(this).print()

/**
 * Returns the most basic properties of the component, formatted as a concise string:
 * * The component class
 * * The [Component.getId]
 * * Whether the component is [Component.isVisible]
 * * Whether it is a [HasValue] that is read-only
 * * the styles
 * * The [Component.label] and text
 * * The [HasValue.getValue]
 */
@Suppress("UNCHECKED_CAST")
fun Component.toPrettyString(): String {
    val list = LinkedList<String>()
    if (id.isPresent) {
        list.add("#${id.get()}")
    }
    if (!_isVisible) {
        list.add("INVIS")
    }
    if (this is HasValue<*, *> && (this as HasValue<HasValue.ValueChangeEvent<Any?>, Any?>).isReadOnly) {
        list.add("RO")
    }
    if (!element.isEnabled) {
        list.add("DISABLED")
    }
    if (label.isNotBlank()) {
        list.add("label='$label'")
    }
    if (label != caption && caption.isNotBlank()) {
        list.add("caption='$caption'")
    }
    if (!_text.isNullOrBlank() && _text != caption) {
        list.add("text='$_text'")
    }
    if (this is HasValue<*, *>) {
        list.add("value='${(this as HasValue<HasValue.ValueChangeEvent<Any?>, Any?>).value}'")
    }
    if (this is HasValidation) {
        if (this.isInvalid) {
            list.add("INVALID")
        }
        if (!this.errorMessage.isNullOrBlank()) {
            list.add("errorMessage='$errorMessage'")
        }
    }
    /* TODO: uncomment when importing Grid stuff
    if (this is Grid.Column<*>) {
        if (this.header2.isNotBlank()) {
            list.add("header='${this.header2}'")
        }
        if (!this.key.isNullOrBlank()) {
            list.add("key='${this.key}'")
        }
    }
     */
    // TODO: add a system property to allow verbose pretty print with ignored attributes
    val ignoredAttr = mutableListOf("value", "invalid", "openOn", "label", "errorMessage", "innerHTML", "i18n","error", "stackTrace")
    this.element.propertyNames.forEach {
        val propertyValue = this.element.getProperty(it)
        if(propertyValue != null && !ignoredAttr.contains(it) && propertyValue.isNotEmpty() && !it.startsWith("_")) {
            list.add("${it}='${propertyValue}'")
        }
    }
    // Any component with href should output it not only Anchor
    hrefValue()?.let {
        list.add("href='$it'")
    }
    if (this is Button && icon is Icon) {
        list.add("icon='${(icon as Icon).element.getAttribute("icon")}'")
    }
    if (this is Html) {
        val outerHtml: String = this.element.outerHTML.trim().replace(Regex("\\s+"), " ")
        list.add(outerHtml.ellipsize(100))
    }
    if (this is Grid<*> && this.beanType != null) {
        list.add("<${this.beanType.simpleName}>")
    }
    if (this.dataProvider != null) {
        list.add("dataprovider='${this.dataProvider}'")
    }
    element.attributeNames
        .filter { !dontDumpAttributes.contains(it) }
        .sorted() // the attributes may come in arbitrary order; make sure to sort them, in order to have predictable order and repeatable tests.
        .forEach { attributeName ->
            val value = element.getAttribute(attributeName)
            if (!value.isNullOrBlank()) {
                list.add("@$attributeName='$value'")
            }
        }
    if (this !is Html && !element.getProperty("innerHTML").isNullOrBlank()) {
        val innerHTML =
            element.getProperty("innerHTML").trim().replace(Regex("\\s+"), " ")
        list.add("innerHTML='$innerHTML'")
    }
    if (this.javaClass.hasCustomToString()) {
        // by default Vaadin components do not introduce toString() at all;
        // toString() therefore defaults to Object's toString() which is useless. However,
        // if a component does introduce a toString() then use it - it could provide
        // valuable information.
        list.add(this.toString())
    }
    prettyStringHook(this, list)
    var name: String = javaClass.simpleName
    if (name.isEmpty()) {
        // anonymous classes
        name = javaClass.name
    }
    return name + list
}

/**
 * Reads the `href` value of this component, so that [toPrettyString] can dump it for
 * any component declaring it, not just [Anchor].
 *
 * Matched, at any visibility and anywhere in the class hierarchy: a no-arg `href()`
 * method - including a default one inherited from an interface - and an `href` field,
 * which is what a Kotlin `href` property with a backing field compiles to. A bean
 * getter is not matched, mirroring the previous kotlin-reflect based lookup where a
 * Java getter shows up as `getHref` and never matched the searched `href` name: the
 * dump therefore stays unchanged for components such as
 * [com.vaadin.flow.router.RouterLink] which only expose `getHref()`. The flip side is
 * that a Kotlin `href` property without a backing field (a custom getter, or a
 * delegated property) compiles to a bean getter as well and is not dumped either,
 * whereas the kotlin-reflect based lookup did report it.
 *
 * The Java reflection API is used on purpose since it resolves member metadata lazily:
 * some components have methods referencing classes (e.g. Spring ones) which may not be
 * present in all projects, and eagerly scanning all metadata (parameters, generics, ...)
 * would fail for those.
 *
 * @return the `href` value, or null if this component has none.
 */
private fun Component.hrefValue(): Any? {
    var clazz: Class<*>? = javaClass
    while (clazz != null) {
        clazz.declaredMethods
            .firstOrNull { it.name == "href" && it.parameterCount == 0 }
            ?.let { return it.apply { isAccessible = true }.invoke(this) }
        clazz.declaredFields
            .firstOrNull { it.name == "href" }
            ?.let { return it.apply { isAccessible = true }.get(this) }
        clazz = clazz.superclass
    }
    // the walk above covers the classes only; this also covers href() default
    // methods inherited from an interface
    javaClass.methods
        .firstOrNull { it.name == "href" && it.parameterCount == 0 }
        ?.let { return it.invoke(this) }
    return null
}

/**
 * Invoked by [toPrettyString] to add additional properties for your custom component.
 * Add additional properties to the `list` provided, e.g. `list.add("icon='$icon'")`.
 *
 * By default does nothing.
 */
var prettyStringHook: (component: Component, list: LinkedList<String>) -> Unit = { _, _ -> }

/**
 * Never dump these attributes in [toPrettyString]. By default these attributes are ignored:
 *
 * * `disabled` - dumped separately as "DISABLED" string.
 * * `id` - dumped as Component.id
 * * `href` - there's special processing for [Anchor._href].
 */
var dontDumpAttributes: MutableSet<String> = mutableSetOf("disabled", "id", "href")
