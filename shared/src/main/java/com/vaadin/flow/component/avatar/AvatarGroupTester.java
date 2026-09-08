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
package com.vaadin.flow.component.avatar;

import java.util.List;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;

/**
 * Tester for AvatarGroup components.
 * <p>
 * The overflow avatar itself is rendered and opened purely on the client, so
 * there is no user action to simulate; what this tester adds is the split
 * between the items the end user sees as individual avatars and the items
 * collapsed behind the overflow avatar, following
 * {@link AvatarGroup#setMaxItemsVisible(Integer)}. The client additionally
 * collapses avatars that do not fit the available width, which cannot be
 * emulated without a layout, so only the configured limit is taken into
 * account.
 *
 * @param <T>
 *            component type
 */
@Tests(AvatarGroup.class)
public class AvatarGroupTester<T extends AvatarGroup>
        extends ComponentTester<T> {

    /**
     * The client never collapses below two avatars, whatever
     * {@code maxItemsVisible} says.
     */
    private static final int MINIMUM_DISPLAYED_AVATARS = 2;

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public AvatarGroupTester(T component) {
        super(component);
    }

    /**
     * Get the items the end user sees as individual avatars.
     * <p>
     * When the items do not fit {@code maxItemsVisible}, the last visible slot
     * is taken by the overflow avatar, so one item less than the limit is
     * shown.
     *
     * @return the visible items, in the order they are rendered
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public List<AvatarGroupItem> getVisibleItems() {
        ensureComponentIsUsable();
        List<AvatarGroupItem> items = getComponent().getItems();
        Integer limit = overflowLimit(items);
        return limit == null ? items : List.copyOf(items.subList(0, limit));
    }

    /**
     * Get the items collapsed behind the overflow avatar.
     *
     * @return the overflowing items, in the order they are rendered, empty if
     *         all items are visible
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public List<AvatarGroupItem> getOverflowItems() {
        ensureComponentIsUsable();
        List<AvatarGroupItem> items = getComponent().getItems();
        Integer limit = overflowLimit(items);
        return limit == null ? List.of()
                : List.copyOf(items.subList(limit, items.size()));
    }

    /**
     * Get the abbreviation shown on the overflow avatar.
     *
     * @return the overflow abbreviation, for example {@code +3}, or
     *         {@code null} if all items are visible
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public String getOverflowAbbreviation() {
        List<AvatarGroupItem> overflowing = getOverflowItems();
        return overflowing.isEmpty() ? null : "+" + overflowing.size();
    }

    /**
     * Number of items rendered as individual avatars, or {@code null} when all
     * items are visible.
     */
    private Integer overflowLimit(List<AvatarGroupItem> items) {
        Integer maxItemsVisible = getComponent().getMaxItemsVisible();
        if (maxItemsVisible == null) {
            return null;
        }
        int max = Math.max(maxItemsVisible, MINIMUM_DISPLAYED_AVATARS);
        return max < items.size() ? max - 1 : null;
    }
}
