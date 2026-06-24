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
import com.vaadin.flow.component.snapshot.DescriptionContributors
import com.vaadin.flow.component.snapshot.NodeFacts
import com.vaadin.flow.component.snapshot.Snapshot

/**
 * Spike: browserless's ASCII tree rendered over the commons Snapshot.walk (shared
 * traversal + selectability + generic facts), with per-node detail injected via the
 * DescriptionContributor SPI. Runs ALONGSIDE the legacy [toPrettyTree]; nothing shipped changes.
 */
fun Component.toPrettyTreeViaCommons(): String {
    val path = ArrayDeque<PrettyPrintTree>()
    var root: PrettyPrintTree? = null
    Snapshot.walk(this) { component, facts, depth ->
        while (path.size > depth) path.removeLast()
        val node = PrettyPrintTree(nodeName(component, facts), mutableListOf())
        if (path.isEmpty()) root = node else path.last().children.add(node)
        path.addLast(node)
        true
    }
    return root?.print() ?: ""
}

private fun nodeName(c: Component, f: NodeFacts): String {
    val frags = mutableListOf<String>()
    f.id()?.let { frags.add("#$it") }
    if (!f.visible()) frags.add("INVIS")
    if (!f.enabled()) frags.add("DISABLED")
    f.label()?.takeIf { it.isNotBlank() }?.let { frags.add("label='$it'") }
    f.value()?.let { frags.add("value='$it'") }
    if (!f.selectable()) frags.add("(not selectable)")
    frags.addAll(DescriptionContributors.global().describe(c))
    val name = c.javaClass.simpleName.ifEmpty { c.javaClass.name }
    return name + frags  // Kotlin renders the list as [a, b], mirroring legacy `name + list`
}
