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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupI18n;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages
class AvatarGroupTesterTest extends BrowserlessTest {

    AvatarGroupView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(AvatarGroupView.class);
        view = navigate(AvatarGroupView.class);
    }

    @Test
    void getItems_returnsAllItems() {
        assertEquals(List.of("Alice", "Bob", "Carol", "Dave", "Eve"),
                names(test(view.avatarGroup).getItems()));
    }

    @Test
    void noMaxItemsVisible_allItemsVisibleAndNoOverflow() {
        assertEquals(List.of("Alice", "Bob", "Carol", "Dave", "Eve"),
                names(test(view.avatarGroup).getVisibleItems()));
        assertEquals(List.of(), test(view.avatarGroup).getOverflowItems());
        assertNull(test(view.avatarGroup).getOverflowAbbreviation(),
                "The overflow avatar is not shown when nothing overflows");
    }

    @Test
    void maxItemsVisibleNotReached_allItemsVisible() {
        view.avatarGroup.setMaxItemsVisible(5);

        assertEquals(5, test(view.avatarGroup).getVisibleItems().size());
        assertNull(test(view.avatarGroup).getOverflowAbbreviation());
    }

    /**
     * The overflow avatar occupies one of the visible slots, and the component
     * never collapses below two avatars, so a max of 0 or 1 behaves as 2.
     */
    @ParameterizedTest
    @CsvSource({ "3, 2, +3", "2, 1, +4", "1, 1, +4", "0, 1, +4" })
    void maxItemsVisibleExceeded_itemsSplitBetweenAvatarsAndOverflow(
            int maxItemsVisible, int expectedVisible,
            String expectedAbbreviation) {
        view.avatarGroup.setMaxItemsVisible(maxItemsVisible);

        var tester = test(view.avatarGroup);
        assertEquals(expectedVisible, tester.getVisibleItems().size());
        assertEquals(5 - expectedVisible, tester.getOverflowItems().size());
        assertEquals(expectedAbbreviation, tester.getOverflowAbbreviation());
        assertEquals(names(tester.getItems()).subList(expectedVisible, 5),
                names(tester.getOverflowItems()),
                "Overflowing items are the trailing ones");
    }

    @Test
    void getActiveUsersLabel_noI18nSet_returnsNull() {
        assertNull(test(view.avatarGroup).getActiveUsersLabel());
    }

    @Test
    void getActiveUsersLabel_manyUsers_countIsSubstituted() {
        view.avatarGroup.setI18n(new AvatarGroupI18n()
                .setManyActiveUsers("Currently {count} active users")
                .setOneActiveUser("Currently one active user"));

        assertEquals("Currently 5 active users",
                test(view.avatarGroup).getActiveUsersLabel());
    }

    @Test
    void getActiveUsersLabel_oneUser_usesSingularPhrase() {
        view.avatarGroup.setI18n(new AvatarGroupI18n()
                .setManyActiveUsers("Currently {count} active users")
                .setOneActiveUser("Currently one active user"));
        view.avatarGroup.setItems(new AvatarGroupItem("Alice"));

        assertEquals("Currently one active user",
                test(view.avatarGroup).getActiveUsersLabel());
    }

    @Test
    void accessors_groupHidden_throw() {
        view.avatarGroup.setVisible(false);

        var tester = test(view.avatarGroup);
        assertThrows(IllegalStateException.class, tester::getItems);
        assertThrows(IllegalStateException.class, tester::getVisibleItems);
        assertThrows(IllegalStateException.class, tester::getOverflowItems);
        assertThrows(IllegalStateException.class,
                tester::getOverflowAbbreviation);
        assertThrows(IllegalStateException.class, tester::getActiveUsersLabel);
    }

    private static List<String> names(List<AvatarGroupItem> items) {
        return items.stream().map(AvatarGroupItem::getName).toList();
    }
}
