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
import com.vaadin.flow.data.renderer.BasicRenderer
import com.vaadin.flow.data.renderer.CompiledLitTemplate
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.data.renderer.Renderer
import com.vaadin.flow.data.renderer.TextRenderer
import com.vaadin.flow.function.ValueProvider
import com.vaadin.flow.js.JsDefinitionProxy
import org.jsoup.Jsoup
import java.lang.reflect.Field
import java.lang.reflect.Method

private val _BasicRenderer_getFormattedValue: Method by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val getFormattedValueM: Method = BasicRenderer::class.java.declaredMethods
        .first { it.name == "getFormattedValue" }
    getFormattedValueM.isAccessible = true
    getFormattedValueM
}

/**
 * Returns the output of this renderer for given [rowObject] formatted as close as possible
 * to the client-side output.
 */
fun <T> Renderer<T>._getPresentationValue(rowObject: T): String? = when {
    this is BasicRenderer<T, *> -> {
        val value: Any? = this.valueProvider.apply(rowObject)
        _BasicRenderer_getFormattedValue.invoke(this, value) as String?
    }
    this is TextRenderer<T> -> {
        renderText(rowObject)
    }
    this is ComponentRenderer<*, T> -> {
        val component: Component = createComponent(rowObject)
        component.toPrettyString()
    }
    this::class.simpleName == "LitRenderer" -> {
        // LitRenderer re-declares private members
        val templateExpression: String = getLitTemplateHtml(this)

        val valueProvidersProperty = this::class.java.getDeclaredField("valueProviders")
        valueProvidersProperty.isAccessible = true
        val valueProviders = valueProvidersProperty.get(this) as Map<String, ValueProvider<T, *>>

        val renderedLitTemplateHtml: String = renderLitTemplate(templateExpression, valueProviders, rowObject)
        Jsoup.parse(renderedLitTemplateHtml).textRecursively
    }
    else -> null
}

/**
 * Returns the HTML of the Lit template that [renderer] renders with: either
 * the template string it was created with, or the `html` tagged template that
 * the body of its declared [CompiledLitTemplate] returns.
 */
private fun getLitTemplateHtml(renderer: Renderer<*>): String {
    // LitRenderer has no public accessor for its template
    val templateField = renderer::class.java.getDeclaredField("template")
    templateField.isAccessible = true
    val template = templateField.get(renderer) as CompiledLitTemplate?
    if (template == null) {
        val templateExpressionField = renderer::class.java.getDeclaredField("templateExpression")
        templateExpressionField.isAccessible = true
        return templateExpressionField.get(renderer) as String
    }
    val body: String = checkNotNull(JsDefinitionProxy.findDeclaredCall(template)) {
        "The template of $renderer is not declared by a @JsDefinition method"
    }.expression
    // The body is a function body like return html`<span>${item.label}</span>`;
    val start: Int = body.indexOf("html`")
    val end: Int = body.lastIndexOf('`')
    return if (start >= 0 && end > start + 4) body.substring(start + 5, end) else body
}

fun <T> renderLitTemplate(template: String, valueProviders: Map<String, ValueProvider<T, *>>, item: T): String {
    var renderedTemplate = template;
    valueProviders.forEach { (k: String, v: ValueProvider<T, *>) ->
        if (renderedTemplate.contains("\${item.$k}")) {
            renderedTemplate = renderedTemplate.replace("\${item.$k}", v.apply(item).toString())
        }
    }
    return renderedTemplate
}

/**
 * Returns the text rendered for given [item].
 */
@Suppress("UNCHECKED_CAST")
fun <T> TextRenderer<T>.renderText(item: T): String =
    createComponent(item).element.text

private val _BasicRenderer_valueProvider: Field by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val javaField: Field = BasicRenderer::class.java.getDeclaredField("valueProvider")
    javaField.isAccessible = true
    javaField
}

/**
 * Returns the [ValueProvider] set to [BasicRenderer].
 */
@Suppress("UNCHECKED_CAST", "ConflictingExtensionProperty")
val <T, V> BasicRenderer<T, V>.valueProvider: ValueProvider<T, V>
    get() = _BasicRenderer_valueProvider.get(this) as ValueProvider<T, V>

