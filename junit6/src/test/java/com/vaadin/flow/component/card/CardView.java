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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Tag("div")
@Route(value = "card", registerAtStartup = false)
public class CardView extends Component implements HasComponents {

    Card card;
    Image media;
    Span headerPrefix;
    Span headerSuffix;
    Span footerAction;

    public CardView() {
        card = new Card();
        media = new Image("card.png", "Card media");
        headerPrefix = new Span("Prefix");
        headerSuffix = new Span("Suffix");
        footerAction = new Span("Footer action");

        card.setTitle("Card title");
        card.setSubtitle("Card subtitle");
        card.setMedia(media);
        card.setHeaderPrefix(headerPrefix);
        card.setHeaderSuffix(headerSuffix);
        card.addToFooter(footerAction);
        card.add(new Span("Card content"));
        add(card);
    }
}
