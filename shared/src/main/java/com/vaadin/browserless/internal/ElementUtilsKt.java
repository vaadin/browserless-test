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
 * Utilities for working with {@link Element}s.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ElementUtilsKt {

    private ElementUtilsKt() {
    }

    /**
     * Either calls {@link Element#setAttribute(String, String)} (if the
     * {@code value} is not null), or {@link Element#removeAttribute(String)}
     * (if the {@code value} is null).
     *
     * @param element
     *            the element, not null.
     * @param attribute
     *            the name of the attribute.
     * @param value
     *            the value of the attribute, may be null.
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
     * @param classList
     *            the class list, not null.
     * @param className
     *            the class name to toggle, cannot contain spaces.
     */
    public static void toggle(ClassList classList, String className) {
        if (UtilsKt.containsWhitespace(className)) {
            throw new IllegalArgumentException(
                    "'" + className + "' cannot contain whitespace");
        }
        classList.set(className, !classList.contains(className));
    }

    /**
     * Inserts {@code newNode} as a child, right before an {@code existingNode}.
     * A counterpart for JavaScript DOM {@code Node.insertBefore()}.
     *
     * @param element
     *            the parent element, not null.
     * @param newNode
     *            the element to insert, not null.
     * @param existingNode
     *            the child before which to insert, not null.
     */
    public static void insertBefore(Element element, Element newNode,
            Element existingNode) {
        Element parent = existingNode.getParent();
        if (parent == null) {
            throw new IllegalArgumentException(
                    existingNode + " has no parent element");
        }
        if (!parent.equals(element)) {
            throw new IllegalArgumentException(
                    existingNode + " is not nested in " + element);
        }
        element.insertChild(element.indexOfChild(existingNode), newNode);
    }

    /**
     * This function actually works, as opposed to
     * {@link Element#getTextRecursively()}.
     *
     * @param element
     *            the element, not null.
     * @return the text contents of the element and all of its descendants.
     */
    public static String getTextRecursively2(Element element) {
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
        return node.childNodes().stream()
                .map(ElementUtilsKt::getTextRecursively)
                .collect(Collectors.joining(""));
    }

    /**
     * Returns all virtual child elements added via
     * {@link Element#appendVirtualChild(Element...)}.
     *
     * @param element
     *            the element, not null.
     * @return the virtual child elements.
     */
    public static List<Element> getVirtualChildren(Element element) {
        StateNode node = element.getNode();
        if (node.hasFeature(VirtualChildrenList.class)) {
            VirtualChildrenList virtualChildrenList = node
                    .getFeatureIfInitialized(VirtualChildrenList.class)
                    .orElse(null);
            if (virtualChildrenList != null) {
                List<Element> result = new ArrayList<>();
                Iterator<StateNode> iterator = virtualChildrenList.iterator();
                while (iterator.hasNext()) {
                    result.add(getElement(iterator.next()));
                }
                return result;
            }
        }
        return List.of();
    }

    /**
     * Gets the element mapped to the given state node.
     *
     * @param node
     *            the state node, not null.
     * @return the element.
     */
    public static Element getElement(StateNode node) {
        return Element.get(node);
    }

    /**
     * Returns child elements with the {@code slot} attribute set to given
     * {@code slotName}.
     *
     * @param element
     *            the parent element, not null.
     * @param slotName
     *            the slot name.
     * @return the child elements in the slot.
     */
    public static List<Element> getChildrenInSlot(Element element,
            String slotName) {
        return element.getChildren()
                .filter(child -> slotName.equals(child.getAttribute("slot")))
                .collect(Collectors.toList());
    }

    /**
     * Removes all child elements from given slot, leaving it empty.
     *
     * @param element
     *            the parent element, not null.
     * @param slotName
     *            the slot name, not blank.
     */
    public static void clearSlot(Element element, String slotName) {
        if (UtilsKt.isBlank(slotName)) {
            throw new IllegalArgumentException("Failed requirement.");
        }
        getChildrenInSlot(element, slotName).forEach(Element::removeFromParent);
    }

    /**
     * Returns all components that are closest to given element.
     *
     * @param element
     *            the element, not null.
     * @return the closest components.
     */
    public static List<Component> _findComponents(Element element) {
        List<Component> components = new ArrayList<>();
        ComponentUtil.findComponents(element, components::add);
        return components;
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
