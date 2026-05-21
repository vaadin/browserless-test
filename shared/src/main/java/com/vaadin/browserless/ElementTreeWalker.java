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
package com.vaadin.browserless;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;

/**
 * Static helpers for walking and selecting nodes in an
 * {@link com.vaadin.flow.dom.Element} subtree.
 * <p>
 * Think {@code document.querySelectorAll} at the Flow element level: handy
 * whenever a search needs to look beyond the component graph (e.g. matching by
 * tag, attribute, or any predicate over the raw element tree).
 */
public final class ElementTreeWalker {

    private ElementTreeWalker() {
        throw new AssertionError("Must not be instantiated");
    }

    /**
     * Depth-first stream containing {@code root} and all of its descendants.
     */
    public static Stream<Element> walk(Element root) {
        return Stream.concat(Stream.of(root),
                root.getChildren().flatMap(ElementTreeWalker::walk));
    }

    /**
     * Elements in the subtree rooted at {@code root} (including {@code root}
     * itself) that match the given predicate, in depth-first order.
     */
    public static Stream<Element> select(Element root,
            Predicate<Element> predicate) {
        return walk(root).filter(predicate);
    }
}
