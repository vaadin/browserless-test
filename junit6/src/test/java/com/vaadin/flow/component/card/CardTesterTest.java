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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages
class CardTesterTest extends BrowserlessTest {

    CardView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(CardView.class);
        view = navigate(CardView.class);
    }

    @Test
    void getTitleAsText_returnsTitle() {
        assertEquals("Card title", test(view.card).getTitleAsText());
    }

    @Test
    void getTitleAsText_noTitleSet_returnsEmptyString() {
        Card empty = new Card();
        view.add(empty);

        assertEquals("", test(empty).getTitleAsText());
    }

    @Test
    void getTitle_titleSetAsComponent_returnsComponent() {
        Span title = new Span("Component title");
        view.card.setTitle(title);

        assertSame(title, test(view.card).getTitle());
    }

    @Test
    void getTitle_titleSetAsText_returnsNull() {
        assertNull(test(view.card).getTitle(),
                "A textual title is not a title component");
    }

    @Test
    void getSubtitle_returnsSubtitleComponent() {
        Component subtitle = test(view.card).getSubtitle();

        assertEquals("Card subtitle", subtitle.getElement().getText());
    }

    @Test
    void getSubtitleAsText_textualSubtitle_returnsText() {
        assertEquals("Card subtitle", test(view.card).getSubtitleAsText());
    }

    @Test
    void getSubtitleAsText_nonTextualSubtitle_returnsNull() {
        view.card.setSubtitle(new Button("Subtitle button"));

        assertNull(test(view.card).getSubtitleAsText(),
                "A subtitle that is not a text subtitle has no text");

        view.card.setSubtitle((Component) null);

        assertNull(test(view.card).getSubtitleAsText(),
                "A card without a subtitle has no subtitle text");
    }

    /**
     * A {@link Span} subtitle set as a component is stored exactly like one set
     * as text, so both report text — unlike the title, where a component title
     * and the {@code cardTitle} property are separate and
     * {@link CardTester#getTitleAsText()} stays empty.
     */
    @Test
    void getSubtitleAsText_subtitleSetAsSpan_readsTextUnlikeTitle() {
        view.card.setSubtitle(new Span("Component subtitle"));
        view.card.setTitle(new Span("Component title"));

        assertEquals("Component subtitle", test(view.card).getSubtitleAsText());
        assertEquals("", test(view.card).getTitleAsText(),
                "A component title leaves the card's title property empty");
    }

    @Test
    void find_searchesEverySlotOfTheCard() {
        Button title = new Button("Title");
        Button media = new Button("Media");
        Button dismiss = new Button("Dismiss");
        Button content = new Button("Content");
        Button book = new Button("Book");
        Button elsewhere = new Button("Elsewhere");
        view.card.setTitle(title);
        view.card.setMedia(media);
        view.card.setHeaderSuffix(dismiss);
        view.card.add(content);
        view.card.addToFooter(book);
        view.add(elsewhere);

        var tester = test(view.card);
        assertSame(dismiss,
                tester.find(Button.class).withText("Dismiss").single(),
                "find() should reach components in the card's slots");
        assertSame(book, tester.find(Button.class).withText("Book").single());
        assertEquals(Set.of(title, media, dismiss, content, book),
                Set.copyOf(tester.find(Button.class).all()),
                "find() should reach every slot and be scoped to the card");
    }

    @Test
    void find_withSlotAttribute_narrowsTheSearchToOneSlot() {
        Button dismiss = new Button("Dismiss");
        Button book = new Button("Book");
        Button content = new Button("Content");
        view.card.setHeaderSuffix(dismiss);
        view.card.addToFooter(book);
        view.card.add(content);

        var tester = test(view.card);
        assertEquals(List.of(book), tester.find(Button.class)
                .withAttribute("slot", "footer").all());
        assertEquals(List.of(dismiss), tester.find(Button.class)
                .withAttribute("slot", "header-suffix").all());
        assertEquals(List.of(content),
                tester.find(Button.class).withoutAttribute("slot").all(),
                "Content added with add() sits in the unnamed default slot, so "
                        + "no named-slot filter matches it");
    }

    @Test
    void headerAccessors_returnHeaderComponents() {
        Span header = new Span("Header");
        view.card.setHeader(header);

        assertSame(header, test(view.card).getHeader());
        assertSame(view.headerPrefix, test(view.card).getHeaderPrefix());
        assertSame(view.headerSuffix, test(view.card).getHeaderSuffix());
    }

    @Test
    void getHeader_noHeaderSet_returnsNull() {
        assertNull(test(view.card).getHeader());
    }

    @Test
    void titleAndSubtitle_headerSet_throw() {
        view.card.setHeader(new Span("Header"));

        assertThrows(IllegalStateException.class,
                test(view.card)::getTitleAsText,
                "The header replaces the title, so it is not shown");
        assertThrows(IllegalStateException.class, test(view.card)::getTitle);
        assertThrows(IllegalStateException.class, test(view.card)::getSubtitle);
        assertThrows(IllegalStateException.class,
                test(view.card)::getSubtitleAsText);
    }

    @Test
    void getMedia_returnsMediaComponent() {
        assertSame(view.media, test(view.card).getMedia());
    }

    @Test
    void getFooterComponents_returnsComponentsInOrder() {
        Span second = new Span("Second footer action");
        view.card.addToFooter(second);

        assertEquals(List.of(view.footerAction, second),
                test(view.card).getFooterComponents());
    }

    @Test
    void getFooterComponents_emptyFooter_returnsEmptyList() {
        Card empty = new Card();
        view.add(empty);

        assertEquals(List.of(), test(empty).getFooterComponents());
    }

    @Test
    void accessors_cardHidden_throw() {
        view.card.setVisible(false);

        assertThrows(IllegalStateException.class,
                test(view.card)::getTitleAsText);
        assertThrows(IllegalStateException.class, test(view.card)::getTitle);
        assertThrows(IllegalStateException.class, test(view.card)::getSubtitle);
        assertThrows(IllegalStateException.class,
                test(view.card)::getSubtitleAsText);
        assertThrows(IllegalStateException.class, test(view.card)::getHeader);
        assertThrows(IllegalStateException.class,
                test(view.card)::getHeaderPrefix);
        assertThrows(IllegalStateException.class,
                test(view.card)::getHeaderSuffix);
        assertThrows(IllegalStateException.class, test(view.card)::getMedia);
        assertThrows(IllegalStateException.class,
                test(view.card)::getFooterComponents);
    }
}
