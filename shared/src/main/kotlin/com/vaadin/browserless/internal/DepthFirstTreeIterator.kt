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

import java.util.Deque
import java.util.LinkedList
import kotlin.streams.toList
import com.vaadin.flow.component.Component

/**
 * Walks the child tree, depth-first: first the node, then its descendants,
 * then its next sibling.
 * @param root start here.
 * @param children fetches children of given node.
 */
class DepthFirstTreeIterator<out T>(root: T, private val children: (T) -> List<T>) : Iterator<T> {
    private val queue: Deque<T> = LinkedList(listOf(root))
    override fun hasNext(): Boolean = !queue.isEmpty()
    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        val result: T = queue.pop()
        children(result).asReversed().forEach { queue.push(it) }
        return result
    }
}

/**
 * Walks the component child tree, depth-first: first the component, then its descendants,
 * then its next sibling.
 */
fun Component.walk(): Iterable<Component> = Iterable {
    DepthFirstTreeIterator(this) { component: Component -> component.children.toList() }
}
