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

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;

/**
 * Tester for SplitLayout components.
 *
 * @param <T>
 *            component type
 */
@Tests(SplitLayout.class)
public class SplitLayoutTester<T extends SplitLayout>
        extends ComponentTester<T> {

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
     * Drag the splitter so that the primary split gets the given percentage of
     * the available space, as the end user would.
     * <p>
     * The drag is reported to the server the same way the client reports it,
     * which means the component recalculates its own splitter position from the
     * flex basis values carried by the event. {@link #getSplitterPosition()}
     * reflects the new position, and any registered splitter drag end listener
     * is notified with {@code fromClient} set to {@code true}.
     *
     * @param primaryPercentage
     *            the share of the space given to the primary split, between
     *            {@code 0} and {@code 100}
     * @throws IllegalStateException
     *             if the component is not usable
     * @throws IllegalArgumentException
     *             if the given percentage is not between 0 and 100
     */
    public void dragSplitterTo(double primaryPercentage) {
        ensureComponentIsUsable();
        if (primaryPercentage < 0 || primaryPercentage > 100) {
            throw new IllegalArgumentException(
                    "Splitter position should be between 0 and 100, but was "
                            + primaryPercentage);
        }
        T component = getComponent();
        ComponentUtil.fireEvent(component,
                new SplitLayout.SplitterDragEndEvent(component, true,
                        primaryPercentage + "%",
                        (100 - primaryPercentage) + "%"));
    }

    /**
     * Get the share of the space currently given to the primary split.
     *
     * @return the splitter position in percentages, or {@code null} if no
     *         position has been set
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public Double getSplitterPosition() {
        ensureComponentIsUsable();
        return getComponent().getSplitterPosition();
    }

    /**
     * Get the component in the primary split.
     *
     * @return the primary component, or {@code null} if none is set
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public Component getPrimaryComponent() {
        ensureComponentIsUsable();
        return getComponent().getPrimaryComponent();
    }

    /**
     * Get the component in the secondary split.
     *
     * @return the secondary component, or {@code null} if none is set
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public Component getSecondaryComponent() {
        ensureComponentIsUsable();
        return getComponent().getSecondaryComponent();
    }
}
