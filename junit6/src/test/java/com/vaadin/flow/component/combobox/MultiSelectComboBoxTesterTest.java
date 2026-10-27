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
package com.vaadin.flow.component.combobox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ClearButtonContract;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
public class MultiSelectComboBoxTesterTest extends BrowserlessTest
        implements ClearButtonContract {

    MultiSelectComboBoxView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(MultiSelectComboBoxView.class);
        view = navigate(MultiSelectComboBoxView.class);
    }

    @Test
    void readOnlyComboBox_isNotUsable() {
        view.combo.setReadOnly(true);

        Assertions.assertFalse(test(view.combo).isUsable(),
                "Read only MultiSelectComboBox shouldn't be usable");
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).selectItem("test-foo"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).selectItem((String[]) null));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).selectItems("test-foo"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).deselectItems("test-foo"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).deselectAll());
    }

    @Test
    void readOnlyComboBox_setFilter_throws() {
        view.combo.setReadOnly(true);

        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).setFilter("fo"));
    }

    @Test
    void notUsableComboBox_selectItem_throws() {
        view.combo.setEnabled(false);

        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).selectItem("test-foo"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).selectItem((String[]) null));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).selectItems("test-foo"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).deselectItems("test-foo"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.combo).deselectAll());
    }

    @Test
    void getSuggestionItems_noFilter_allItemsReturned() {
        final List<MultiSelectComboBoxView.Name> suggestions = test(view.combo)
                .getSuggestionItems();
        Assertions.assertIterableEquals(view.items, suggestions);
    }

    @Test
    void getSuggestions_noFilter_allItemsReturned() {
        final List<String> suggestions = test(view.combo).getSuggestions();
        Assertions.assertIterableEquals(Arrays.asList("test-foo", "test-bar"),
                suggestions);
    }

    @Test
    void setFilter_getSuggestions_filterIsApplied() {
        test(view.combo).setFilter("fo");
        final List<String> suggestions = test(view.combo).getSuggestions();
        Assertions.assertEquals(1, suggestions.size());
        Assertions.assertEquals("test-foo", suggestions.get(0));
    }

    @Test
    void selectItem_selectsCorrectItem() {
        Assertions.assertTrue(test(view.combo).getSelected().isEmpty());

        test(view.combo).selectItem("test-foo");

        Assertions.assertIterableEquals(Set.of(view.items.get(0)),
                test(view.combo).getSelected());

        test(view.combo).selectItem("test-foo", "test-bar");

        Assertions.assertTrue(
                test(view.combo).getSelected().contains(view.items.get(0)));
        Assertions.assertTrue(
                test(view.combo).getSelected().contains(view.items.get(1)));

        test(view.combo).selectItem(null);

        Assertions.assertTrue(test(view.combo).getSelected().isEmpty(),
                "Selecting null should clear selection");
    }

    @Test
    void selectItems_addsToSelection() {
        test(view.combo).selectItems("test-foo");

        Assertions.assertEquals(Set.of(view.items.get(0)),
                test(view.combo).getSelected());

        test(view.combo).selectItems("test-bar");

        Assertions.assertEquals(Set.copyOf(view.items),
                test(view.combo).getSelected());

        test(view.combo).selectItems("test-foo");

        Assertions.assertEquals(Set.copyOf(view.items),
                test(view.combo).getSelected(),
                "Selecting an already selected item should keep it selected");
    }

    @Test
    void deselectItems_removesOnlyGivenItems() {
        view.combo.setValue(Set.copyOf(view.items));

        test(view.combo).setFilter("bar");
        test(view.combo).deselectItems("test-foo");

        Assertions.assertEquals(Set.of(view.items.get(1)),
                test(view.combo).getSelected(),
                "An item hidden by the filter should still be deselectable");
    }

    @Test
    void deselectAll_emptiesSelectionWithoutClearButton() {
        view.combo.setValue(Set.copyOf(view.items));
        view.combo.setClearButtonVisible(false);

        test(view.combo).deselectAll();

        Assertions.assertTrue(test(view.combo).getSelected().isEmpty());
    }

    @Test
    void selectItemsAndDeselectItems_unknownOrNullLabel_throws() {
        view.combo.setValue(Set.of(view.items.get(0)));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.combo).selectItems("test-bar", "test-baz"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.combo).deselectItems("test-foo", "test-baz"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.combo).selectItems((String[]) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.combo).deselectItems((String[]) null));

        Assertions.assertEquals(Set.of(view.items.get(0)),
                test(view.combo).getSelected(),
                "A failed call should leave the selection unchanged");
    }

    @Test
    void selectionChangesLookLikeUserInteraction() {
        List<Boolean> fromClient = new ArrayList<>();
        view.combo.addValueChangeListener(
                ev -> fromClient.add(ev.isFromClient()));

        test(view.combo).selectItem("test-foo");
        test(view.combo).selectItems("test-bar");
        test(view.combo).deselectItems("test-foo");
        test(view.combo).deselectAll();

        Assertions.assertEquals(4, fromClient.size(),
                "Every interaction should fire a value change event");
        Assertions.assertFalse(fromClient.contains(false),
                "Tester driven value changes should report isFromClient() == true");
    }

    @Test
    void selectItems_selectionModelStaysInSyncWithValue() {
        test(view.combo).selectItems("test-bar");

        Assertions.assertEquals(Set.of(view.items.get(1)),
                view.combo.getSelectedItems());
        Assertions.assertTrue(view.combo.isSelected(view.items.get(1)));
        Assertions.assertFalse(view.combo.isSelected(view.items.get(0)));
    }

    @Override
    public HasValue<?, ?> fieldUnderTest() {
        view.combo.setValue(Set.of(view.items.get(0)));
        return view.combo;
    }

    @Override
    public void clickClearButton() {
        test(view.combo).clickClearButton();
    }
}
