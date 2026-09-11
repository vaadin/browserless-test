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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class AvatarGroupTesterTest extends BrowserlessTest {

    AvatarGroupView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(AvatarGroupView.class);
        view = navigate(AvatarGroupView.class);
    }

    @Test
    void noMaxItemsVisible_allItemsVisible() {
        Assertions.assertEquals(5,
                test(view.avatarGroup).getVisibleItems().size());
        Assertions.assertEquals(List.of(),
                test(view.avatarGroup).getOverflowItems());
        Assertions.assertNull(test(view.avatarGroup).getOverflowAbbreviation());
    }

    @Test
    void maxItemsVisible_overflowAvatarTakesLastVisibleSlot() {
        view.avatarGroup.setMaxItemsVisible(3);

        Assertions.assertEquals(names("Aria Bailey", "Aaliyah Butler"),
                names(test(view.avatarGroup).getVisibleItems()),
                "The third slot is taken by the overflow avatar");
        Assertions.assertEquals(names("Eula Lane", "Jose Wade", "Sonya Winter"),
                names(test(view.avatarGroup).getOverflowItems()));
        Assertions.assertEquals("+3",
                test(view.avatarGroup).getOverflowAbbreviation());
    }

    @Test
    void maxItemsVisibleBelowTwo_stillShowsTwoSlots() {
        view.avatarGroup.setMaxItemsVisible(1);

        Assertions.assertEquals(names("Aria Bailey"),
                names(test(view.avatarGroup).getVisibleItems()),
                "The client never collapses below two slots");
        Assertions.assertEquals("+4",
                test(view.avatarGroup).getOverflowAbbreviation());

        // Clamped to two slots, which the two items fit, so no overflow avatar
        view.avatarGroup.setItems(new AvatarGroupItem("Aria Bailey"),
                new AvatarGroupItem("Aaliyah Butler"));

        Assertions.assertEquals(names("Aria Bailey", "Aaliyah Butler"),
                names(test(view.avatarGroup).getVisibleItems()));
        Assertions.assertEquals(List.of(),
                test(view.avatarGroup).getOverflowItems());
    }

    @Test
    void maxItemsVisibleFitsAllItems_noOverflow() {
        view.avatarGroup.setMaxItemsVisible(5);

        Assertions.assertEquals(5,
                test(view.avatarGroup).getVisibleItems().size());
        Assertions.assertEquals(List.of(),
                test(view.avatarGroup).getOverflowItems());
    }

    @Test
    void getters_notUsable_throw() {
        view.avatarGroup.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.avatarGroup)::getVisibleItems);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.avatarGroup)::getOverflowItems);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.avatarGroup)::getOverflowAbbreviation);
    }

    private static List<String> names(String... names) {
        return List.of(names);
    }

    private static List<String> names(List<AvatarGroupItem> items) {
        return items.stream().map(AvatarGroupItem::getName).toList();
    }
}
