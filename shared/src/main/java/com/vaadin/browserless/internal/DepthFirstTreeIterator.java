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

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import kotlin.jvm.functions.Function1;

/**
 * Walks the child tree, depth-first: first the node, then its descendants, then
 * its next sibling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the type of the tree nodes.
 */
public final class DepthFirstTreeIterator<T> implements Iterator<T> {

    private final Function1<? super T, ? extends List<? extends T>> children;
    private final Deque<T> queue = new LinkedList<>();

    /**
     * Creates the iterator.
     *
     * @param root
     *            start here.
     * @param children
     *            fetches children of given node.
     */
    public DepthFirstTreeIterator(T root,
            Function1<? super T, ? extends List<? extends T>> children) {
        this.children = children;
        queue.push(root);
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
        List<? extends T> resultChildren = children.invoke(result);
        for (int i = resultChildren.size() - 1; i >= 0; i--) {
            queue.push(resultChildren.get(i));
        }
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Operation is not supported for read-only collection");
    }
}
