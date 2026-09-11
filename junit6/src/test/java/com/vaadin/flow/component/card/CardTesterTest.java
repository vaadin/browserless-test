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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class CardTesterTest extends BrowserlessTest {

    CardView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(CardView.class);
        view = navigate(CardView.class);
    }

    @Test
    void getTitleText_returnsStringAndComponentTitles() {
        Assertions.assertEquals("Lapland", test(view.card).getTitleText());

        view.card.setTitle(new Span("Kilpisjärvi"));
        Assertions.assertEquals("Kilpisjärvi", test(view.card).getTitleText());

        // Removing the title component leaves the card without any title,
        // the string title having been dropped by setTitle(Component)
        view.card.setTitle((Component) null);
        Assertions.assertEquals("", test(view.card).getTitleText());
    }

    @Test
    void getSubtitleText_returnsSubtitle() {
        Assertions.assertEquals("The Exotic North",
                test(view.card).getSubtitleText());

        view.card.setSubtitle((String) null);
        Assertions.assertEquals("", test(view.card).getSubtitleText());
    }

    @Test
    void headerComponent_hidesTitleAndSubtitle() {
        view.card.setHeader(new Span("Custom header"));

        Assertions.assertEquals("", test(view.card).getTitleText(),
                "A header component is shown instead of the title");
        Assertions.assertEquals("", test(view.card).getSubtitleText(),
                "A header component is shown instead of the subtitle");
    }

    @Test
    void getFooterComponents_returnsFooterContentOnly() {
        Assertions.assertEquals(List.of(view.footerButton),
                test(view.card).getFooterComponents());
    }

    @Test
    void getSlottedComponents_returnContentOfEachSlot() {
        Assertions.assertNull(test(view.card).getHeader());
        Assertions.assertNull(test(view.card).getHeaderPrefix());
        Assertions.assertNull(test(view.card).getHeaderSuffix());
        Assertions.assertNull(test(view.card).getMedia());

        Span header = new Span("Custom header");
        Span prefix = new Span("prefix");
        Span suffix = new Span("suffix");
        Span media = new Span("media");
        view.card.setHeader(header);
        view.card.setHeaderPrefix(prefix);
        view.card.setHeaderSuffix(suffix);
        view.card.setMedia(media);

        Assertions.assertSame(header, test(view.card).getHeader());
        Assertions.assertSame(prefix, test(view.card).getHeaderPrefix());
        Assertions.assertSame(suffix, test(view.card).getHeaderSuffix());
        Assertions.assertSame(media, test(view.card).getMedia());
    }

    @Test
    void getters_notUsable_throw() {
        view.card.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getTitleText);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getSubtitleText);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getFooterComponents);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getHeader);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getHeaderPrefix);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getHeaderSuffix);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.card)::getMedia);
    }
}
