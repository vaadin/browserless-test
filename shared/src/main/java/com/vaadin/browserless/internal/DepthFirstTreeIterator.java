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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;

/**
 * Walks the child tree, depth-first: first the node, then its descendants,
 * then its next sibling.
 */
/**
 * Walks a tree depth-first: a node, then its descendants, then its next
 * sibling.
 * <p>
 * The traversal order is what makes a component tree dump and a lookup failure
 * message readable, because it matches the order the components appear in.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the node type
 */
public class DepthFirstTreeIterator<T> implements Iterator<T> {

    private final Deque<T> queue;
    private final Function<T, List<T>> children;

    /**
     * Creates an iterator over the tree below the given node.
     *
     * @param root
     *            start here.
     * @param children
     *            fetches children of given node.
     */
    public DepthFirstTreeIterator(T root, Function<T, List<T>> children) {
        this.children = children;
        this.queue = new ArrayDeque<>();
        this.queue.push(root);
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T result = queue.pop();
        List<T> kids = children.apply(result);
        ListIterator<T> it = kids.listIterator(kids.size());
        while (it.hasPrevious()) {
            queue.push(it.previous());
        }
        return result;
    }

    /**
     * Walks the component child tree, depth-first: first the component, then
     * its descendants, then its next sibling.
     *
     * @param root
     *            the component to start from
     * @return the components, in depth-first order
     */
    public static Iterable<Component> walk(Component root) {
        return () -> new DepthFirstTreeIterator<>(root, component -> component
                .getChildren().collect(Collectors.toList()));
    }
}
