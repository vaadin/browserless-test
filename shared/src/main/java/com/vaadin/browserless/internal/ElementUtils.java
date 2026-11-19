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

import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * Utilities for working with {@link Element}s.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ElementUtils {

    private ElementUtils() {
    }

    /**
     * This function actually works, as opposed to
     * {@link Element#getTextRecursively()}.
     *
     * @param element
     *            the element, not null.
     * @return the text contents of the element and all of its descendants.
     */
    public static String getTextIncludingInnerHtml(Element element) {
        // remove when this is fixed: https://github.com/vaadin/flow/issues/3668
        Node node = ElementUtil.toJsoup(new Document(""), element);
        return getTextRecursively(node);
    }

    /**
     * Returns the text contents of given jsoup node and all of its descendants.
     *
     * @param node
     *            the node, not null.
     * @return the text contents.
     */
    public static String getTextRecursively(Node node) {
        if (node instanceof TextNode textNode) {
            return textNode.text();
        }
        return node.childNodes().stream().map(ElementUtils::getTextRecursively)
                .collect(Collectors.joining(""));
    }

    /**
     * Fires a DOM {@code event} on given element.
     *
     * @param element
     *            the element, not null.
     * @param event
     *            the DOM event, not null.
     */
    public static void _fireDomEvent(Element element, DomEvent event) {
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(event);
    }
}
