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

import org.jetbrains.annotations.Nullable;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;

/**
 * Tester for Card components.
 * <p>
 * Gives access to the content of the card as the user sees it. A card that has
 * a {@link Card#setHeader(Component) header component} shows the header instead
 * of the title and subtitle, so the title and subtitle accessors reject that
 * case rather than reporting content that is not on screen.
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
     * Gets the title text shown on the card.
     *
     * @return the title text, or an empty string if no textual title is set
     * @throws IllegalStateException
     *             if the card is not visible to the user, or if a header
     *             component is set so the title is not shown
     */
    public String getTitleAsText() {
        ensureVisible();
        ensureHeaderNotShown("title");
        return getComponent().getTitleAsText();
    }

    /**
     * Gets the component used as the card's title.
     *
     * @return the title component, or {@code null} if the title is not a
     *         component
     * @throws IllegalStateException
     *             if the card is not visible to the user, or if a header
     *             component is set so the title is not shown
     */
    @Nullable
    public Component getTitle() {
        ensureVisible();
        ensureHeaderNotShown("title");
        return getComponent().getTitle();
    }

    /**
     * Gets the component used as the card's subtitle.
     *
     * @return the subtitle component, or {@code null} if no subtitle is set
     * @throws IllegalStateException
     *             if the card is not visible to the user, or if a header
     *             component is set so the subtitle is not shown
     */
    @Nullable
    public Component getSubtitle() {
        ensureVisible();
        ensureHeaderNotShown("subtitle");
        return getComponent().getSubtitle();
    }

    /**
     * Gets the component used as the card's header. When set, the header is
     * shown instead of the title and subtitle.
     *
     * @return the header component, or {@code null} if no header is set
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    @Nullable
    public Component getHeader() {
        ensureVisible();
        return getComponent().getHeader();
    }

    /**
     * Gets the component shown before the card's header content.
     *
     * @return the header prefix component, or {@code null} if none is set
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    @Nullable
    public Component getHeaderPrefix() {
        ensureVisible();
        return getComponent().getHeaderPrefix();
    }

    /**
     * Gets the component shown after the card's header content.
     *
     * @return the header suffix component, or {@code null} if none is set
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    @Nullable
    public Component getHeaderSuffix() {
        ensureVisible();
        return getComponent().getHeaderSuffix();
    }

    /**
     * Gets the component shown as the card's media.
     *
     * @return the media component, or {@code null} if none is set
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    @Nullable
    public Component getMedia() {
        ensureVisible();
        return getComponent().getMedia();
    }

    /**
     * Gets the components shown in the card's footer, in the order they were
     * added.
     *
     * @return the footer components, empty if the footer is empty
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    public List<Component> getFooterComponents() {
        ensureVisible();
        return List.of(getComponent().getFooterComponents());
    }

    private void ensureHeaderNotShown(String part) {
        if (getComponent().getHeader() != null) {
            throw new IllegalStateException(
                    "Card has a header component, so the " + part
                            + " is not shown to the user");
        }
    }
}
