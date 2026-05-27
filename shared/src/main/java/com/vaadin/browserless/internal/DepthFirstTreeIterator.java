/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
public class DepthFirstTreeIterator<T> implements Iterator<T> {

    private final Deque<T> queue;
    private final Function<T, List<T>> children;

    /**
     * @param root start here.
     * @param children fetches children of given node.
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
     * Walks the component child tree, depth-first: first the component, then its descendants,
     * then its next sibling.
     */
    public static Iterable<Component> walk(Component root) {
        return () -> new DepthFirstTreeIterator<>(root,
                component -> component.getChildren().collect(Collectors.toList()));
    }
}
