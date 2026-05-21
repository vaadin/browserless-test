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
package com.example.multiuser;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * A view with buttons that trigger external navigation via
 * {@link com.vaadin.flow.component.page.Page#setLocation(String)} and
 * {@link com.vaadin.flow.component.page.Page#open(String)}.
 */
@Route("external-nav")
public class ExternalNavigationView extends VerticalLayout {

    public ExternalNavigationView() {
        Button setLocation = new Button("Go to Vaadin", e -> UI.getCurrent()
                .getPage().setLocation("https://vaadin.com/"));

        Button openNew = new Button("Pay", e -> UI.getCurrent().getPage()
                .open("https://payment.example.com/checkout?id=123"));

        Button openNamedWindow = new Button("Open Help", e -> UI.getCurrent()
                .getPage().open("https://help.example.com/", "helpWindow"));

        Button openParent = new Button("Open Parent", e -> UI.getCurrent()
                .getPage().open("https://parent.example.com/", "_parent"));

        Button openTop = new Button("Open Top", e -> UI.getCurrent().getPage()
                .open("https://top.example.com/", "_top"));

        Button openBlank1 = new Button("Open Tab 1", e -> UI.getCurrent()
                .getPage().open("https://tab1.example.com/"));

        Button openBlank2 = new Button("Open Tab 2", e -> UI.getCurrent()
                .getPage().open("https://tab2.example.com/"));

        add(setLocation, openNew, openNamedWindow, openParent, openTop,
                openBlank1, openBlank2);
    }
}
