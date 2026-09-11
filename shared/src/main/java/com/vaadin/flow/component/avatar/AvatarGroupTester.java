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

import org.jetbrains.annotations.Nullable;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupI18n;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;

/**
 * Tester for AvatarGroup components.
 * <p>
 * AvatarGroup has no server-side listeners: everything the user can do with it
 * happens in the browser without a round trip. This tester is therefore
 * assertion-only, and reports which items are shown as individual avatars and
 * which ones are grouped behind the overflow avatar.
 * <p>
 * The split is derived from {@link AvatarGroup#getMaxItemsVisible()} only.
 * Without a browser there is no layout, so the additional truncation the
 * component applies when the avatars do not fit the available width cannot be
 * taken into account.
 *
 * @param <T>
 *            component type
 */
@Tests(AvatarGroup.class)
public class AvatarGroupTester<T extends AvatarGroup>
        extends ComponentTester<T> {

    /**
     * The component never collapses below two avatars, whatever
     * {@code maxItemsVisible} is set to.
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
     * Gets all items of the group, whether or not they are shown as individual
     * avatars.
     *
     * @return the items of the group
     * @throws IllegalStateException
     *             if the group is not visible to the user
     */
    public List<AvatarGroupItem> getItems() {
        ensureVisible();
        return getComponent().getItems();
    }

    /**
     * Gets the items that are shown as individual avatars.
     *
     * @return the items shown as individual avatars
     * @throws IllegalStateException
     *             if the group is not visible to the user
     */
    public List<AvatarGroupItem> getVisibleItems() {
        ensureVisible();
        List<AvatarGroupItem> items = getComponent().getItems();
        return List.copyOf(items.subList(0, visibleItemCount(items)));
    }

    /**
     * Gets the items that do not fit {@link AvatarGroup#getMaxItemsVisible()}
     * and are grouped behind the overflow avatar.
     *
     * @return the overflowing items, empty if nothing overflows
     * @throws IllegalStateException
     *             if the group is not visible to the user
     */
    public List<AvatarGroupItem> getOverflowItems() {
        ensureVisible();
        List<AvatarGroupItem> items = getComponent().getItems();
        return List
                .copyOf(items.subList(visibleItemCount(items), items.size()));
    }

    /**
     * Gets the abbreviation shown on the overflow avatar, such as {@code "+3"}.
     *
     * @return the overflow abbreviation, or {@code null} if no items overflow
     *         and the overflow avatar is not shown
     * @throws IllegalStateException
     *             if the group is not visible to the user
     */
    @Nullable
    public String getOverflowAbbreviation() {
        ensureVisible();
        List<AvatarGroupItem> items = getComponent().getItems();
        int overflowing = items.size() - visibleItemCount(items);
        return overflowing == 0 ? null : "+" + overflowing;
    }

    /**
     * Gets the accessible label of the group, that is, the
     * {@link AvatarGroupI18n#setOneActiveUser(String) one} or
     * {@link AvatarGroupI18n#setManyActiveUsers(String) many} active users
     * phrase with {@code {count}} replaced by the total number of items.
     *
     * @return the accessible label, or {@code null} if no matching translation
     *         has been set on the component
     * @throws IllegalStateException
     *             if the group is not visible to the user
     */
    @Nullable
    public String getActiveUsersLabel() {
        ensureVisible();
        AvatarGroupI18n i18n = getComponent().getI18n();
        if (i18n == null) {
            return null;
        }
        int count = getComponent().getItems().size();
        String phrase = count == 1 ? i18n.getOneActiveUser()
                : i18n.getManyActiveUsers();
        return phrase == null ? null
                : phrase.replace("{count}", String.valueOf(count));
    }

    private int visibleItemCount(List<AvatarGroupItem> items) {
        Integer maxItemsVisible = getComponent().getMaxItemsVisible();
        if (maxItemsVisible == null) {
            return items.size();
        }
        int max = Math.max(maxItemsVisible, MINIMUM_DISPLAYED_AVATARS);
        // The overflow avatar takes one of the slots when it is shown
        return max < items.size() ? max - 1 : items.size();
    }
}
