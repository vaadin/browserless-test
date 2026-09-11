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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
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
