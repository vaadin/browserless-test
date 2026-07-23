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
package com.vaadin.browserless;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;

/**
 * Verifies that components added to a {@link Dialog}'s header and footer are
 * locatable by {@code find(...)}, not just the main content, even though the
 * dialog places them in slotted wrapper elements and excludes them from
 * {@code getChildren()}. The locator descends through the non-component wrapper
 * to reach them.
 */
class DialogSlotTraversalTest extends BrowserlessTest {

    @Test
    void dialogContent_headerFooterAndPlain_allLocatable() {
        Button headerButton = new Button("Header button");
        Button footerButton = new Button("Footer button");
        Button contentButton = new Button("Content button");

        Dialog dialog = new Dialog();
        dialog.getHeader().add(headerButton);
        dialog.getFooter().add(footerButton);
        dialog.add(contentButton);
        dialog.open();

        // Control: a button in the dialog content (regular getChildren()).
        Assertions.assertTrue(
                find(Button.class).withText("Content button").exists(),
                "button in dialog content must be locatable");

        // The slotted header/footer buttons must be locatable too: they live in
        // wrapper elements the dialog excludes from getChildren().
        Assertions.assertTrue(
                find(Button.class).withText("Header button").exists(),
                "button in dialog header slot must be locatable");
        Assertions.assertTrue(
                find(Button.class).withText("Footer button").exists(),
                "button in dialog footer slot must be locatable");
    }
}
