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

import com.vaadin.flow.dom.DomEvent
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.ElementUtil
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Returns the text of given [element] and all of its descendants, including the
 * `innerHTML` property, which [Element.getTextRecursively] ignores.
 */
fun getTextRecursively2(element: Element): String {
    // remove when this is fixed: https://github.com/vaadin/flow/issues/3668
    val node = ElementUtil.toJsoup(Document(""), element)
    return node.textRecursively
}

val Node.textRecursively: String
    get() = when (this) {
        is TextNode -> this.text()
        else -> childNodes().joinToString(separator = "", transform = { it.textRecursively })
    }

/**
 * Fires a DOM [event] on given [element].
 */
fun _fireDomEvent(element: Element, event: DomEvent) {
    element.node.getFeature(ElementListenerMap::class.java).fireEvent(event)
}
