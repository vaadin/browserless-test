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
package com.vaadin.flow.component.listbox;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@ViewPackages
class MultiSelectListBoxTesterTest extends BrowserlessTest {
    ListBoxView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(ListBoxView.class);
        view = navigate(ListBoxView.class);
    }

    @Test
    void readOnlyListBox_isNotUsable() {
        view.multiSelectListBox.setReadOnly(true);

        Assertions.assertFalse(test(view.multiSelectListBox).isUsable(),
                "Read only MultiSelectListBox shouldn't be usable");
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.multiSelectListBox).selectItems("one"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.multiSelectListBox).deselectItems("one"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.multiSelectListBox).clearSelection());
    }

    @Test
    void getSuggestionItems_returnsAllItems() {
        assertIterableEquals(view.selection,
                test(view.multiSelectListBox).getSuggestionItems());
    }

    @Test
    void stringSelect_getSuggestions_valuesEqualItems() {
        assertIterableEquals(view.selection,
                test(view.multiSelectListBox).getSuggestions());
    }

    @Test
    void stringSelect_selectItemsDeselectItems_selectsCorrectItem() {
        final MultiSelectListBoxTester<MultiSelectListBox<String>, String> list_ = test(
                view.multiSelectListBox);
        Assertions.assertTrue(list_.getSelected().isEmpty());

        list_.selectItems("two");

        Assertions.assertIterableEquals(view.selection.subList(1, 2),
                list_.getSelected());

        list_.deselectItems("two");

        Assertions.assertTrue(list_.getSelected().isEmpty(),
                "Deselecting item should clear selection");
    }

    @Test
    void stringSelect_selectItems_addsToSelection() {
        final MultiSelectListBoxTester<MultiSelectListBox<String>, String> list_ = test(
                view.multiSelectListBox);
        Assertions.assertTrue(list_.getSelected().isEmpty());

        list_.selectItems("two");

        Assertions.assertIterableEquals(view.selection.subList(1, 2),
                list_.getSelected());

        list_.selectItems("one");

        Assertions.assertIterableEquals(view.selection, list_.getSelected());
    }

    @Test
    void selectDeselectAndClear_valueChangesLookLikeUserInteraction() {
        final MultiSelectListBoxTester<MultiSelectListBox<String>, String> list_ = test(
                view.multiSelectListBox);
        List<Boolean> fromClient = new ArrayList<>();
        view.multiSelectListBox.addValueChangeListener(
                ev -> fromClient.add(ev.isFromClient()));

        list_.selectItems("one", "two");
        list_.deselectItems("one");
        list_.clearSelection();

        Assertions.assertEquals(3, fromClient.size(),
                "Every interaction should fire a value change event");
        Assertions.assertFalse(fromClient.contains(false),
                "Tester driven value changes should report isFromClient() == true");
    }

    @Test
    void clearSelection_selectItems_addsToSelection() {
        final MultiSelectListBoxTester<MultiSelectListBox<String>, String> list_ = test(
                view.multiSelectListBox);
        Assertions.assertTrue(list_.getSelected().isEmpty());

        list_.selectItems("two", "one");

        Assertions.assertIterableEquals(view.selection, list_.getSelected());

        list_.clearSelection();

        Assertions.assertTrue(list_.getSelected().isEmpty());
    }

    @Test
    void notUsableListBox_clearSelection_throws() {
        final MultiSelectListBoxTester<MultiSelectListBox<String>, String> list_ = test(
                view.multiSelectListBox);
        list_.selectItems("one");
        view.multiSelectListBox.setEnabled(false);

        Assertions.assertThrows(IllegalStateException.class,
                list_::clearSelection);
    }

}
