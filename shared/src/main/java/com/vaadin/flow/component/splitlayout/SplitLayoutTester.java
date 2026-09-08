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
package com.vaadin.flow.component.splitlayout;

import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for SplitLayout components.
 * <p>
 * Simulates the user dragging the splitter and gives access to the components
 * shown in the primary and secondary splits.
 *
 * @param <T>
 *            component type
 */
@Tests(SplitLayout.class)
public class SplitLayoutTester<T extends SplitLayout>
        extends ComponentTester<T> {

    /**
     * Expressions the client sends as event data with the
     * {@code splitter-dragend} event, as declared by
     * {@link SplitLayout.SplitterDragEndEvent}.
     */
    private static final String PRIMARY_FLEX_BASIS = "element.querySelector(':scope > [slot=\"primary\"]').style.flexBasis";
    private static final String SECONDARY_FLEX_BASIS = "element.querySelector(':scope > [slot=\"secondary\"]').style.flexBasis";

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public SplitLayoutTester(T component) {
        super(component);
    }

    /**
     * Simulates the user dragging the splitter so that the primary split gets
     * the given percentage of the available space.
     * <p>
     * The drag is delivered the same way the browser delivers it, so the
     * component updates its {@link SplitLayout#getSplitterPosition() splitter
     * position} and notifies
     * {@link SplitLayout#addSplitterDragEndListener(com.vaadin.flow.component.ComponentEventListener)
     * splitter drag end listeners}.
     *
     * @param position
     *            the new splitter position, as a percentage between 0 and 100
     * @throws IllegalArgumentException
     *             if the position is not between 0 and 100
     * @throws IllegalStateException
     *             if the layout is not usable
     */
    public void dragSplitterTo(double position) {
        ensureComponentIsUsable();
        if (position < 0 || position > 100) {
            throw new IllegalArgumentException(
                    "Splitter position must be a percentage between 0 and 100, but was "
                            + position);
        }
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(PRIMARY_FLEX_BASIS, position + "%");
        eventData.put(SECONDARY_FLEX_BASIS, (100 - position) + "%");
        fireDomEvent("splitter-dragend", eventData);
        roundTrip();
    }

    /**
     * Gets the relative position of the splitter, as a percentage between 0 and
     * 100 of the space given to the primary split.
     *
     * @return the splitter position, or {@code null} if it has neither been set
     *         on the server nor been dragged by the user
     * @throws IllegalStateException
     *             if the layout is not visible to the user
     */
    @Nullable
    public Double getSplitterPosition() {
        ensureVisible();
        return getComponent().getSplitterPosition();
    }

    /**
     * Gets the component shown in the primary split.
     *
     * @return the primary component, or {@code null} if the primary split is
     *         empty
     * @throws IllegalStateException
     *             if the layout is not visible to the user
     */
    @Nullable
    public Component getPrimaryComponent() {
        ensureVisible();
        return getComponent().getPrimaryComponent();
    }

    /**
     * Gets the component shown in the secondary split.
     *
     * @return the secondary component, or {@code null} if the secondary split
     *         is empty
     * @throws IllegalStateException
     *             if the layout is not visible to the user
     */
    @Nullable
    public Component getSecondaryComponent() {
        ensureVisible();
        return getComponent().getSecondaryComponent();
    }
}
