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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;

/**
 * Helpers for the element tree underneath the components: slots, virtual
 * children, class lists and DOM events.
 * <p>
 * A lookup cannot rely on {@code Component.getChildren()} alone, because a
 * component may keep parts of itself in a slot or as a virtual child. These
 * helpers reach those, and fire DOM events the way the client would.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ElementUtils {

    private ElementUtils() {
    }

    /**
     * Either calls {@link Element#setAttribute} (if the {@code value} is not
     * null), or {@link Element#removeAttribute} (if the {@code value} is null).
     * 
     * @param attribute
     *            the name of the attribute.
     */
    public static void setOrRemoveAttribute(Element element, String attribute,
            String value) {
        if (value == null) {
            element.removeAttribute(attribute);
        } else {
            element.setAttribute(attribute, value);
        }
    }

    /**
     * Toggles {@code className} - removes it if it was there, or adds it if it
     * wasn't there.
     * 
     * @param className
     *            the class name to toggle, cannot contain spaces.
     */
    public static void toggle(ClassList classList, String className) {
        if (Utils.containsWhitespace(className)) {
            throw new IllegalArgumentException(
                    "'" + className + "' cannot contain whitespace");
        }
        classList.set(className, !classList.contains(className));
    }

    /**
     * Inserts {@code newNode} as a child, right before an {@code existingNode}.
     * A counterpart for JavaScript DOM `Node.insertBefore()`.
     */
    public static void insertBefore(Element parent, Element newNode,
            Element existingNode) {
        Element existingParent = existingNode.getParent();
        if (existingParent == null) {
            throw new IllegalArgumentException(
                    existingNode + " has no parent element");
        }
        if (!existingParent.equals(parent)) {
            throw new IllegalArgumentException(
                    existingNode + " is not nested in " + parent);
        }
        parent.insertChild(parent.indexOfChild(existingNode), newNode);
    }

    /**
     * This function actually works, as opposed to
     * {@link Element#getTextRecursively}.
     */
    public static String textRecursively2(Element element) {
        // remove when this is fixed: https://github.com/vaadin/flow/issues/3668
        Node node = ElementUtil.toJsoup(new Document(""), element);
        return textRecursively(node);
    }

    public static String textRecursively(Node node) {
        if (node instanceof TextNode) {
            return ((TextNode) node).text();
        }
        StringBuilder sb = new StringBuilder();
        for (Node child : node.childNodes()) {
            sb.append(textRecursively(child));
        }
        return sb.toString();
    }

    /**
     * Returns all virtual child elements added via
     * {@link Element#appendVirtualChild}.
     */
    public static List<Element> getVirtualChildren(Element element) {
        if (element.getNode().hasFeature(VirtualChildrenList.class)) {
            VirtualChildrenList virtualChildrenList = element.getNode()
                    .getFeatureIfInitialized(VirtualChildrenList.class)
                    .orElse(null);
            if (virtualChildrenList != null) {
                List<Element> result = new ArrayList<>();
                Iterator<StateNode> it = virtualChildrenList.iterator();
                while (it.hasNext()) {
                    result.add(Element.get(it.next()));
                }
                return result;
            }
        }
        return List.of();
    }

    /**
     * Gets the element mapped to the given state node.
     */
    public static Element element(StateNode node) {
        return Element.get(node);
    }

    /**
     * Returns child elements with the `slot` attribute set to given
     * {@code slotName}.
     */
    public static List<Element> getChildrenInSlot(Element element,
            String slotName) {
        return element.getChildren()
                .filter(child -> slotName.equals(child.getAttribute("slot")))
                .collect(Collectors.toList());
    }

    /**
     * Removes all child elements from given slot, leaving it empty.
     */
    public static void clearSlot(Element element, String slotName) {
        if (slotName == null || slotName.isBlank()) {
            throw new IllegalArgumentException();
        }
        for (Element child : getChildrenInSlot(element, slotName)) {
            child.removeFromParent();
        }
    }

    /**
     * Returns all components that are closest to {@code this} element.
     */
    public static List<Component> _findComponents(Element element) {
        List<Component> components = new ArrayList<>();
        ComponentUtil.findComponents(element, components::add);
        return components;
    }

    /**
     * Fires a DOM {@code event} on this element.
     */
    public static void _fireDomEvent(Element element, DomEvent event) {
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(event);
    }
}
