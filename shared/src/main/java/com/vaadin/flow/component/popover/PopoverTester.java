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
package com.vaadin.flow.component.popover;

import java.util.function.Consumer;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;

/**
 * Tester for Popover components.
 */
@Tests(Popover.class)
public class PopoverTester extends ComponentTester<Popover> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public PopoverTester(Popover component) {
        super(component);
    }

    /**
     * Creates a {@link PopoverTester} for the popover that targets the
     * component matching the given query.
     *
     * @param query
     *            a query that resolves to the target component
     * @return a tester for the popover targeting the matched component
     * @throws java.util.NoSuchElementException
     *             if no popover targets the matched component
     */
    public static PopoverTester forTarget(
            ComponentQuery<? extends Component> query) {
        return forTarget(query.single());
    }

    /**
     * Creates a {@link PopoverTester} for the popover that targets the given
     * component.
     *
     * @param target
     *            the target component of the popover
     * @return a tester for the popover targeting the given component
     * @throws java.util.NoSuchElementException
     *             if no popover targets the given component
     */
    public static PopoverTester forTarget(Component target) {
        Popover popover = new ComponentQuery<>(Popover.class)
                .withCondition(p -> p.getTarget() == target).single();
        return new PopoverTester(popover);
    }

    /**
     * Open the popover.
     */
    public void open() {
        getComponent().open();
        if (getComponent().isModal()) {
            setModal(true);
        }
        roundTrip();
        fireOpenedChangeEvent();
    }

    /**
     * Close the popover.
     */
    public void close() {
        if (getComponent().isModal()) {
            setModal(false);
        }
        getComponent().close();
        roundTrip();
        fireOpenedChangeEvent();
    }

    /**
     * Simulates clicking the target component to open the popover.
     *
     * @throws IllegalStateException
     *             if no target is set or open-on-click is disabled
     */
    public void clickTarget() {
        ensureTargetSet();
        if (!getComponent().isOpenOnClick()) {
            throw new IllegalStateException(
                    "open-on-click is disabled for this popover");
        }
        open();
    }

    /**
     * Simulates hovering the target component to open the popover.
     *
     * @throws IllegalStateException
     *             if no target is set or open-on-hover is disabled
     */
    public void hoverTarget() {
        ensureTargetSet();
        if (!getComponent().isOpenOnHover()) {
            throw new IllegalStateException(
                    "open-on-hover is disabled for this popover");
        }
        open();
    }

    /**
     * Simulates focusing the target component to open the popover.
     *
     * @throws IllegalStateException
     *             if no target is set or open-on-focus is disabled
     */
    public void focusTarget() {
        ensureTargetSet();
        if (!getComponent().isOpenOnFocus()) {
            throw new IllegalStateException(
                    "open-on-focus is disabled for this popover");
        }
        open();
    }

    /**
     * Simulates pressing Escape to close the popover.
     *
     * @throws IllegalStateException
     *             if the popover is not open or close-on-Esc is disabled
     */
    public void pressEscape() {
        if (!isOpen()) {
            throw new IllegalStateException("popover is not open");
        }
        if (!getComponent().isCloseOnEsc()) {
            throw new IllegalStateException(
                    "close-on-Esc is disabled for this popover");
        }
        close();
    }

    /**
     * Simulates clicking outside the popover to close it.
     *
     * @throws IllegalStateException
     *             if the popover is not open or close-on-outside-click is
     *             disabled
     */
    public void clickOutside() {
        if (!isOpen()) {
            throw new IllegalStateException("popover is not open");
        }
        if (!getComponent().isCloseOnOutsideClick()) {
            throw new IllegalStateException(
                    "close-on-outside-click is disabled for this popover");
        }
        close();
    }

    /**
     * Check if the popover is open.
     *
     * @return true if the popover is open
     */
    public boolean isOpen() {
        return getComponent().isOpened();
    }

    @Override
    public boolean isUsable() {
        Popover component = getComponent();
        return component.isVisible() && component.isAttached()
                && component.isOpened() && component.getElement().isEnabled();
    }

    private void fireOpenedChangeEvent() {
        ComponentUtil.fireEvent(getComponent(),
                new Popover.OpenedChangeEvent(getComponent(), true));
    }

    private void ensureTargetSet() {
        if (getComponent().getTarget() == null) {
            throw new IllegalStateException(
                    "no target component set on the popover");
        }
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        Popover component = getComponent();
        if (!component.isAttached()) {
            collector.accept("not attached");
        }
        if (!component.isVisible()) {
            collector.accept("not visible");
        }
        if (!component.isOpened()) {
            collector.accept("not opened");
        }
        if (!component.getElement().isEnabled()) {
            collector.accept("not enabled");
        }
    }
}
