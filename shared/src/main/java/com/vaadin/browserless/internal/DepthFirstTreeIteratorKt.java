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

import com.vaadin.flow.component.Component;

/**
 * Functions for walking the component tree.
 */
public final class DepthFirstTreeIteratorKt {

    private DepthFirstTreeIteratorKt() {
    }

    /**
     * Walks the component child tree, depth-first: first the component, then
     * its descendants, then its next sibling.
     *
     * @param component
     *            the root component.
     * @return the iterable over the component and all of its descendants.
     */
    public static Iterable<Component> walk(Component component) {
        return () -> new DepthFirstTreeIterator<Component>(component,
                c -> c.getChildren().toList());
    }
}
