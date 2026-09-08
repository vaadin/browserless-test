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
package com.vaadin.flow.component.card;

import java.util.List;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;

/**
 * Tester for Card components.
 *
 * @param <T>
 *            component type
 */
@Tests(Card.class)
public class CardTester<T extends Card> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public CardTester(T component) {
        super(component);
    }

    /**
     * Get the title text the end user sees on the card.
     * <p>
     * Handles both title flavors: a string title set with
     * {@link Card#setTitle(String)} and the text content of a title component
     * set with {@link Card#setTitle(Component)}. A header component takes the
     * place of the title, so the title is reported as empty while one is set.
     *
     * @return the title text, or an empty string if the card shows no title
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public String getTitleText() {
        ensureComponentIsUsable();
        if (getComponent().getHeader() != null) {
            return "";
        }
        String title = getComponent().getTitleAsText();
        if (!title.isEmpty()) {
            return title;
        }
        return textOf(getComponent().getTitle());
    }

    /**
     * Get the subtitle text the end user sees on the card.
     * <p>
     * A header component takes the place of the subtitle, so the subtitle is
     * reported as empty while one is set.
     *
     * @return the subtitle text, or an empty string if the card shows no
     *         subtitle
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public String getSubtitleText() {
        ensureComponentIsUsable();
        if (getComponent().getHeader() != null) {
            return "";
        }
        return textOf(getComponent().getSubtitle());
    }

    /**
     * Get the components in the card footer.
     *
     * @return the footer components, empty if the card has no footer
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public List<Component> getFooterComponents() {
        ensureComponentIsUsable();
        return List.of(getComponent().getFooterComponents());
    }

    private String textOf(Component component) {
        return component == null ? ""
                : component.getElement().getTextRecursively();
    }
}
