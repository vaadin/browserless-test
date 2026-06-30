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
package com.vaadin.flow.component.dashboard;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for the {@link Dashboard} component.
 * <p>
 * Simulates the interactions a user performs on an editable dashboard:
 * reordering, resizing and removing widgets. Each method fires the same
 * client-side event the web component sends, so the dashboard updates its
 * server-side model and notifies any registered listeners exactly as it would
 * in a browser. All three actions require the dashboard to be editable
 * ({@link Dashboard#setEditable(boolean)}), mirroring the component: a user
 * cannot move, resize or remove widgets otherwise.
 *
 * @param <T>
 *            the dashboard type
 * @since 1.1
 */
@Tests(Dashboard.class)
public class DashboardTester<T extends Dashboard> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public DashboardTester(T component) {
        super(component);
    }

    /**
     * Simulates a user dragging the given widget to a new position within its
     * current container (the dashboard root, or the section it belongs to).
     *
     * @param widget
     *            the widget to move, not {@code null}
     * @param toIndex
     *            the target index among the widget's siblings
     * @throws IllegalStateException
     *             if the dashboard is not editable
     * @throws IllegalArgumentException
     *             if the widget is not in this dashboard or the target index is
     *             out of bounds
     */
    public void moveWidget(DashboardWidget widget, int toIndex) {
        ensureComponentIsUsable();
        ensureEditable();

        DashboardSection section = findSection(widget);
        List<Component> siblings = section == null
                ? new ArrayList<>(getComponent().getChildren().toList())
                : new ArrayList<>(section.getWidgets());

        int from = siblings.indexOf(widget);
        if (from < 0) {
            throw new IllegalArgumentException(
                    "The widget is not part of this dashboard");
        }
        if (toIndex < 0 || toIndex >= siblings.size()) {
            throw new IllegalArgumentException("Target index " + toIndex
                    + " is out of bounds (0.." + (siblings.size() - 1) + ")");
        }
        siblings.remove(from);
        siblings.add(toIndex, widget);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.item", nodeId(widget));
        if (section == null) {
            eventData.set("event.detail.items", idArray(siblings));
        } else {
            eventData.set("event.detail.items",
                    topLevelArrayWithSection(section, siblings));
            eventData.put("event.detail.section", nodeId(section));
        }
        fireDomEvent("dashboard-item-moved-flow", eventData);
        roundTrip();
    }

    /**
     * Simulates a user resizing the given widget to the given column and row
     * span.
     *
     * @param widget
     *            the widget to resize, not {@code null}
     * @param colspan
     *            the new column span
     * @param rowspan
     *            the new row span
     * @throws IllegalStateException
     *             if the dashboard is not editable
     * @throws IllegalArgumentException
     *             if the widget is not in this dashboard
     */
    public void resizeWidget(DashboardWidget widget, int colspan, int rowspan) {
        ensureComponentIsUsable();
        ensureEditable();
        ensureWidget(widget);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.item.id", nodeId(widget));
        eventData.put("event.detail.item.colspan", colspan);
        eventData.put("event.detail.item.rowspan", rowspan);
        fireDomEvent("dashboard-item-resized", eventData);
        roundTrip();
    }

    /**
     * Simulates a user removing the given widget by clicking its remove button.
     * <p>
     * If the dashboard has a custom
     * {@link Dashboard#setItemRemoveHandler(DashboardItemRemoveHandler) remove
     * handler}, it receives the event and decides whether to remove the widget;
     * otherwise the widget is removed immediately.
     *
     * @param widget
     *            the widget to remove, not {@code null}
     * @throws IllegalStateException
     *             if the dashboard is not editable
     * @throws IllegalArgumentException
     *             if the widget is not in this dashboard
     */
    public void removeWidget(DashboardWidget widget) {
        ensureComponentIsUsable();
        ensureEditable();
        ensureWidget(widget);

        DashboardSection section = findSection(widget);
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.item.id", nodeId(widget));
        if (section != null) {
            eventData.put("event.detail.section?.id", nodeId(section));
        }
        fireDomEvent("dashboard-item-before-remove", eventData);
        roundTrip();
    }

    private void ensureEditable() {
        if (!getComponent().isEditable()) {
            throw new IllegalStateException(
                    "Dashboard is not editable. A user can only move, resize or "
                            + "remove widgets when the dashboard is in edit mode "
                            + "(setEditable(true)).");
        }
    }

    private void ensureWidget(DashboardWidget widget) {
        if (!getComponent().getWidgets().contains(widget)) {
            throw new IllegalArgumentException(
                    "The widget is not part of this dashboard");
        }
    }

    private DashboardSection findSection(DashboardWidget widget) {
        return getComponent().getChildren()
                .filter(DashboardSection.class::isInstance)
                .map(DashboardSection.class::cast)
                .filter(section -> section.getWidgets().contains(widget))
                .findFirst().orElse(null);
    }

    private ArrayNode idArray(List<? extends Component> components) {
        ArrayNode array = JacksonUtils.createArrayNode();
        for (Component component : components) {
            array.add(JacksonUtils.createObjectNode().put("id",
                    nodeId(component)));
        }
        return array;
    }

    private ArrayNode topLevelArrayWithSection(DashboardSection section,
            List<Component> reorderedSectionWidgets) {
        ArrayNode array = JacksonUtils.createArrayNode();
        for (Component child : getComponent().getChildren().toList()) {
            ObjectNode entry = JacksonUtils.createObjectNode().put("id",
                    nodeId(child));
            if (child == section) {
                entry.set("items", idArray(reorderedSectionWidgets));
            }
            array.add(entry);
        }
        return array;
    }

    private static int nodeId(Component component) {
        return component.getElement().getNode().getId();
    }
}
