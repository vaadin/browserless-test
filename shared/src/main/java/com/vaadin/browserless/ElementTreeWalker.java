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

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;

/**
 * Static helpers for walking and selecting nodes in an
 * {@link com.vaadin.flow.dom.Element} subtree.
 * <p>
 * Think {@code document.querySelectorAll} at the Flow element level: handy
 * whenever a search needs to look beyond the component graph (e.g. matching by
 * tag, attribute, or any predicate over the raw element tree).
 * <p>
 * Traversal includes both regular and virtual children, so components that
 * attach content via {@code Element.appendVirtualChild(...)} (Dialog overlay,
 * MasterDetailLayout detail slot, etc.) are still reachable.
 */
final class ElementTreeWalker {

    private ElementTreeWalker() {
        throw new AssertionError("Must not be instantiated");
    }

    /**
     * Depth-first stream containing {@code root} and all of its descendants,
     * including elements attached as virtual children.
     */
    static Stream<Element> walk(Element root) {
        return Stream.concat(Stream.of(root),
                Stream.concat(root.getChildren(), virtualChildren(root))
                        .flatMap(ElementTreeWalker::walk));
    }

    private static Stream<Element> virtualChildren(Element element) {
        if (!element.getNode().hasFeature(VirtualChildrenList.class)) {
            return Stream.empty();
        }
        return element.getNode()
                .getFeatureIfInitialized(VirtualChildrenList.class)
                .map(list -> StreamSupport.stream(
                        ((Iterable<StateNode>) list::iterator).spliterator(),
                        false).map(Element::get))
                .orElseGet(Stream::empty);
    }
}
