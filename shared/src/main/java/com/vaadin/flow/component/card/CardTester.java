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

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;

/**
 * Tester for Card components.
 * <p>
 * Gives access to the content of the card as the user sees it. A card that has
 * a {@link Card#setHeader(Component) header component} shows the header instead
 * of the title and subtitle, so the title and subtitle accessors reject that
 * case rather than reporting content that is not on screen.
 * <p>
 * The accessors return the component sitting in a slot. To reach a component
 * nested deeper inside a slot, use {@link #findInHeader(Class)},
 * {@link #findInFooter(Class)} or {@link #find(Class)}, which searches every
 * slot of the card and not only its default content slot.
 *
 * @param <T>
 *            component type
 */
@Tests(Card.class)
public class CardTester<T extends Card> extends ComponentTester<T> {

    /**
     * The slot a card puts its {@link Card#setHeader(Component) header} in.
     */
    private static final String HEADER_SLOT = "header";

    /**
     * The slot a card puts the components added with
     * {@link Card#addToFooter(Component...)} in.
     */
    private static final String FOOTER_SLOT = "footer";

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
     * Gets the subtitle text shown on the card.
     * <p>
     * {@link Card#setSubtitle(String)} wraps the text in a {@link Span}, so a
     * subtitle set as text is reported here, and so is the text of a
     * {@link Span} set with {@link Card#setSubtitle(Component)} — the card
     * stores the two the same way and cannot tell them apart. A subtitle set as
     * any other component is not text and reports {@code null}; use
     * {@link #getSubtitle()} to get the component itself.
     * <p>
     * Note that this differs from {@link #getTitleAsText()}, which reports an
     * empty string rather than {@code null} when the card shows no textual
     * title, because a card keeps its title text in a property of its own
     * instead of in a component.
     *
     * @return the subtitle text, or {@code null} if no subtitle is set or the
     *         subtitle is not a text subtitle
     * @throws IllegalStateException
     *             if the card is not visible to the user, or if a header
     *             component is set so the subtitle is not shown
     */
    @Nullable
    public String getSubtitleAsText() {
        ensureVisible();
        ensureHeaderNotShown("subtitle");
        return getComponent().getSubtitle() instanceof Span span
                ? span.getText()
                : null;
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

    /**
     * {@inheritDoc}
     * <p>
     * Searches the whole card, so components in the title, subtitle, header,
     * header prefix, header suffix, media and footer slots are found as well as
     * the ones in the default content slot. Example usage:
     *
     * <pre>
     * // view:
     * Card card = new Card();
     * card.setHeaderSuffix(new Button("Dismiss"));
     * card.addToFooter(new Button("Book"));
     *
     * // test:
     * CardTester&lt;Card&gt; cardTester = test(view.card);
     * cardTester.find(Button.class).withText("Dismiss").single().click();
     * Button book = cardTester.find(Button.class).withText("Book").single();
     * </pre>
     * <p>
     * To narrow the search to the header or the footer, use
     * {@link #findInHeader(Class)} or {@link #findInFooter(Class)}. Any other
     * slot is reachable with {@link ComponentQuery#withinSlot(String)}, for
     * example {@code find(Button.class).withinSlot("header-suffix")}.
     * <p>
     * The default content slot has no name, so it has no such filter:
     * components added with {@code Card.add(...)} carry no {@code slot}
     * attribute at all and are singled out with
     * {@code withoutAttribute("slot")}, which — unlike
     * {@link ComponentQuery#withinSlot(String)} — only sees the components
     * sitting directly in the slot, not the ones nested inside them.
     */
    @Override
    public <R extends Component> ComponentQuery<R> find(
            Class<R> componentType) {
        return super.find(componentType);
    }

    /**
     * Searches the card's header for components of the given type.
     * <p>
     * The header is the component set with {@link Card#setHeader(Component)},
     * shown instead of the title and subtitle. Components nested inside it are
     * found too, so a button inside a header layout is reached as well as a
     * button set as the header itself.
     *
     * @param componentType
     *            the type of the components to search for
     * @param <R>
     *            the type of the components to search for
     * @return a query for components of the given type in the card's header
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    public <R extends Component> ComponentQuery<R> findInHeader(
            Class<R> componentType) {
        ensureVisible();
        return find(componentType).withinSlot(HEADER_SLOT);
    }

    /**
     * Searches the card's footer for components of the given type.
     * <p>
     * The footer holds the components added with
     * {@link Card#addToFooter(Component...)}. Components nested inside them are
     * found too, so a button inside a footer layout is reached as well as a
     * button added to the footer directly.
     *
     * @param componentType
     *            the type of the components to search for
     * @param <R>
     *            the type of the components to search for
     * @return a query for components of the given type in the card's footer
     * @throws IllegalStateException
     *             if the card is not visible to the user
     */
    public <R extends Component> ComponentQuery<R> findInFooter(
            Class<R> componentType) {
        ensureVisible();
        return find(componentType).withinSlot(FOOTER_SLOT);
    }

    private void ensureHeaderNotShown(String part) {
        if (getComponent().getHeader() != null) {
            throw new IllegalStateException(
                    "Card has a header component, so the " + part
                            + " is not shown to the user");
        }
    }
}
