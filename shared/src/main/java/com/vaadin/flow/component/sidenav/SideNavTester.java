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
package com.vaadin.flow.component.sidenav;

import java.util.List;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * @since 1.0
 */
@Tests(SideNav.class)
public class SideNavTester<T extends SideNav> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public SideNavTester(T component) {
        super(component);
    }

    /**
     * Simulates a click on the item that matches the given label.
     *
     * For nested navigation item provide the label of each item in the
     * hierarchy.
     *
     * The path to the navigation item must reflect what is seen in the browser,
     * meaning that hidden items are ignored. For the same reason, items inside
     * collapsed nodes cannot be clicked and an exception is thrown. Use
     * {@link #expandAndClickItem(String, String...)} to expand intermediate
     * node and click on the desired item.
     * <p>
     * The click is fired as a DOM {@code click} event on the item, so its click
     * listeners run. Then, as in the browser, an item without a path is
     * expanded or collapsed, and an item with a path navigates there unless a
     * click listener prevents the default action. A router-ignored item
     * navigates outside the router, recorded as an external navigation.
     *
     * <pre>
     * {@code
     *
     * sideNav.addItem(new SideNavItem("Home", HomeView.class));
     * SideNavItem childNav = new SideNavItem("Messages");
     * childNav.addItem(new SideNavItem("Inbox", InboxView.class));
     * childNav.addItem(new SideNavItem("Sent", SentView.class));
     * sideNav.addItem(childNav);
     *
     * // clicks top level navigation item with label Home
     * tester.clickItem("Home");
     *
     * // clicks nested navigation item with label Sent
     * wrapper.clickItem("Messages", "Sent");
     * }
     * </pre>
     *
     * @param topLevelLabel
     *            the text content of the top level SideNav item label, not
     *            {@literal null}.
     * @param nestedItemLabels
     *            labels of the nested SideNav items
     * @throws IllegalArgumentException
     *             if the provided text does not identify a SideNav item.
     * @throws IllegalStateException
     *             if the item at given path is not usable.
     * @see #expandAndClickItem(String, String...)
     */
    public void clickItem(String topLevelLabel, String... nestedItemLabels) {
        doClickItem(false, topLevelLabel, nestedItemLabels);
    }

    /**
     * Simulates a click on the item that matches the given label, expanding
     * potential parent collapsed nodes.
     *
     * For nested navigation item provide the label of each item in the
     * hierarchy.
     *
     * The path to the navigation item must reflect what is seen in the browser,
     * meaning that hidden items are ignored. For the same reason, items inside
     * collapsed nodes cannot be clicked and an exception is thrown.
     * <p>
     * The click is fired as a DOM {@code click} event on the item, so its click
     * listeners run. Then, as in the browser, an item without a path is
     * expanded or collapsed, and an item with a path navigates there unless a
     * click listener prevents the default action. A router-ignored item
     * navigates outside the router, recorded as an external navigation.
     *
     * <pre>
     * {@code
     *
     * sideNav.addItem(new SideNavItem("Home", HomeView.class));
     * SideNavItem childNav = new SideNavItem("Messages");
     * childNav.addItem(new SideNavItem("Inbox", InboxView.class));
     * childNav.addItem(new SideNavItem("Sent", SentView.class));
     * sideNav.addItem(childNav);
     *
     * // clicks nested navigation item with label Sent
     * wrapper.expandAndClickItem("Messages", "Sent");
     * }
     * </pre>
     *
     * @param topLevelLabel
     *            the text content of the top level SideNav item label, not
     *            {@literal null}.
     * @param nestedItemLabels
     *            labels of the nested SideNav items
     * @throws IllegalArgumentException
     *             if the provided text does not identify a SideNav item.
     * @throws IllegalStateException
     *             if the item at given path is not usable.
     * @see #expandAndClickItem(String, String...)
     */
    public void expandAndClickItem(String topLevelLabel,
            String... nestedItemLabels) {
        doClickItem(true, topLevelLabel, nestedItemLabels);
    }

    /**
     * Simulates a click on the SideNav label, expanding or collapsing item
     * list.
     *
     * @throws IllegalStateException
     *             if the {@link SideNav} component is not collapsible or not
     *             usable.
     *
     * @see SideNav#setCollapsible(boolean)
     * @see SideNav#isCollapsible()
     */
    public void click() {
        ensureComponentIsUsable();
        if (getComponent().isCollapsible()) {
            getComponent().setExpanded(!getComponent().isExpanded());
        }
    }

    /**
     * Simulates a click on the item that matches the given label.
     *
     * For nested navigation item provide the label of each item in the
     * hierarchy.
     *
     * The path to the navigation item must reflect what is seen in the browser,
     * meaning that hidden items are ignored. For the same reason, items inside
     * collapsed nodes cannot be clicked and an exception is thrown. Use
     * {@link #expandAndClickItem(String, String...)} to expand intermediate
     * node and click on the desired item.
     *
     * <pre>
     * {@code
     *
     * sideNav.addItem(new SideNavItem("Home", HomeView.class));
     * SideNavItem childNav = new SideNavItem("Messages");
     * childNav.addItem(new SideNavItem("Inbox", InboxView.class));
     * childNav.addItem(new SideNavItem("Sent", SentView.class));
     * sideNav.addItem(childNav);
     *
     * // clicks top level navigation item with label Home
     * tester.clickItem("Home");
     *
     * // clicks nested navigation item with label Sent
     * wrapper.clickItem("Messages", "Sent");
     * }
     * </pre>
     *
     * @param topLevelLabel
     *            the text content of the top level SideNav item label, not
     *            {@literal null}.
     * @param nestedItemLabels
     *            labels of the nested SideNav items
     * @throws IllegalArgumentException
     *             if the provided text does not identify a SideNav item.
     * @throws IllegalStateException
     *             if the item at given path is not usable.
     * @see #expandAndClickItem(String, String...)
     */
    public void toggleItem(String topLevelLabel, String... nestedItemLabels) {
        doToggleItem(false, topLevelLabel, nestedItemLabels);
    }

    /**
     * Simulates a click on the SideNav toggle button, expanding or collapsing
     * item list.
     *
     * @throws IllegalStateException
     *             if the {@link SideNav} component is not collapsible or not
     *             usable.
     *
     * @see SideNav#setCollapsible(boolean)
     * @see SideNav#isCollapsible()
     */
    public void toggle() {
        ensureComponentIsUsable();
        if (getComponent().isCollapsible()) {
            getComponent().setExpanded(!getComponent().isExpanded());
        } else {
            throw new IllegalStateException(
                    "Toggle button cannot be clicked because SideNav is not collapsible");
        }
    }

    private void doToggleItem(boolean expandNodes, String topLevelLabel,
            String... nestedItemLabels) {
        ensureComponentIsUsable();
        SideNavItem navItem = findSideNavItemByPath(expandNodes, topLevelLabel,
                nestedItemLabels);
        if (!navItem.getItems().isEmpty()) {
            navItem.setExpanded(!navItem.isExpanded());
        } else {
            throw new IllegalStateException(
                    "Toggle button cannot be clicked because the SideNav item has no children");
        }
    }

    private void doClickItem(boolean expandNodes, String topLevelLabel,
            String... nestedItemLabels) {
        ensureComponentIsUsable();
        SideNavItem navItem = findSideNavItemByPath(expandNodes, topLevelLabel,
                nestedItemLabels);
        // The browser fires the click event on the item before the router
        // link handler sees it. The router skips the link when a listener
        // prevented the default action, and leaves a router-ignored item to
        // the browser, which then loads the path as a new page.
        fireDomEvent(new DomEvent(navItem.getElement(), "click",
                JacksonUtils.createObjectNode()));
        if (navItem.getPath() == null) {
            navItem.setExpanded(!navItem.isExpanded());
        } else if (!isClickDefaultPrevented(navItem)) {
            if (navItem.isRouterIgnore()) {
                UI.getCurrent().getPage().setLocation(navItem.getPath());
            } else {
                UI.getCurrent().navigate(navItem.getPath());
            }
        }
    }

    private static boolean isClickDefaultPrevented(SideNavItem navItem) {
        return navItem.getElement().getNode()
                .getFeature(ElementListenerMap.class).getExpressions("click")
                .contains("event.preventDefault()");
    }

    private SideNavItem findSideNavItemByPath(boolean expandNodes,
            String topLevelLabel, String... nestedItemLabels) {
        SideNavItem navItem = findSideNavItem(getComponent().getItems(),
                topLevelLabel, null);
        if (nestedItemLabels.length > 0) {
            String path = topLevelLabel + " / "
                    + String.join(" / ", nestedItemLabels);
            for (String label : nestedItemLabels) {
                if (expandNodes) {
                    navItem.setExpanded(true);
                }
                if (!navItem.isExpanded()) {
                    throw new IllegalStateException(
                            "Cannot find SideNav item with label '" + label
                                    + "' on path '" + path
                                    + "' because parent item '"
                                    + navItem.getLabel() + "' is collapsed.");
                }
                List<SideNavItem> children = navItem.getItems();
                if (!children.isEmpty()) {
                    navItem = findSideNavItem(children, label, path);
                } else {
                    throw new IllegalArgumentException(
                            "SideNav item with label " + navItem.getLabel()
                                    + " has no children. Make sure that the path is correct: "
                                    + path);
                }
            }
        }
        return navItem;
    }

    private SideNavItem findSideNavItem(List<SideNavItem> items, String label,
            String fullPath) {
        List<SideNavItem> navItems = items.stream()
                .filter(item -> label.equals(item.getLabel())).toList();

        if (navItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot find SideNav item '" + label + "'"
                            + (fullPath != null ? " on path " + fullPath : ""));
        } else if (navItems.size() > 1) {
            throw new IllegalStateException("Found " + navItems.size()
                    + " items with label '" + label + "'"
                    + (fullPath != null ? " on path " + fullPath : ""));
        }
        SideNavItem navItem = navItems.get(0);
        ensureComponentIsUsable(navItem, ComponentTester::isUsable);
        return navItem;
    }

}
