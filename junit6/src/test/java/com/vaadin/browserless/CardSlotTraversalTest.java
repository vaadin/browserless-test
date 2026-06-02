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
import com.vaadin.flow.component.card.Card;

/**
 * Reproduces a locator-traversal bug: components placed in a {@link Card}'s
 * slots (header, header-suffix, header-prefix, footer, title, media) are not
 * found by {@code find(...)} or {@code ui.findButton()} even though they
 * render in the browser. The locator engine only walks the regular Flow
 * component tree and misses content attached via the Card's slot setters.
 * <p>
 * Reduced from the standalone repro in {@code tmp/card-locator-repro}.
 */
class CardSlotTraversalTest extends BrowserlessTest {

    @Test
    void cardContent_slottedAndPlain_allLocatable() {
        Button contentButton = new Button("Content button");
        Button headerSuffixButton = new Button("Header-suffix button");
        Button headerPrefixButton = new Button("Header-prefix button");
        Button headerButton = new Button("Header button");
        Button footerButton = new Button("Footer button");

        Card card = new Card();
        card.add(contentButton);
        card.setHeaderSuffix(headerSuffixButton);
        card.setHeaderPrefix(headerPrefixButton);
        card.setHeader(headerButton);
        card.addToFooter(footerButton);

        getCurrentView().getElement().appendChild(card.getElement());

        // Control: a button in the card content slot (regular getChildren()).
        Assertions.assertTrue(find(Button.class)
                .withText("Content button").exists(),
                "button in card content slot must be locatable");

        // The bug — each of the slotted buttons must be locatable too, but
        // currently isn't because the locator engine only walks the regular
        // Flow tree and misses content attached via Card's slot setters.
        Assertions.assertTrue(find(Button.class)
                .withText("Header-suffix button").exists(),
                "button in card header-suffix slot must be locatable");
        Assertions.assertTrue(find(Button.class)
                .withText("Header-prefix button").exists(),
                "button in card header-prefix slot must be locatable");
        Assertions.assertTrue(find(Button.class)
                .withText("Header button").exists(),
                "button in card header slot must be locatable");
        Assertions.assertTrue(find(Button.class)
                .withText("Footer button").exists(),
                "button in card footer slot must be locatable");
    }
}
