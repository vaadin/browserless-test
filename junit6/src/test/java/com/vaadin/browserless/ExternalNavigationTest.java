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

import java.util.Set;

import com.example.base.ExternalNavigationView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.button.Button;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ViewPackages(classes = ExternalNavigationView.class)
class ExternalNavigationTest {

    VaadinTestApplicationContext app;

    @BeforeAll
    void setup() {
        Routes routes = VaadinTestApplicationContext.discoverRoutes(getClass(),
                Set.of());
        app = VaadinTestApplicationContext.create(routes);
    }

    @AfterAll
    void tearDown() {
        app.close();
    }

    @Test
    void setLocation_capturesExternalURL() {
        VaadinTestUiContext ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        // No external navigation yet
        Assertions.assertNull(ui.getExternalNavigationURL());

        ui.$view(ExternalNavigationView.class);

        // Click "Go to Vaadin" which calls Page.setLocation()
        ui.test(ui.$(Button.class).withText("Go to Vaadin").single()).click();

        Assertions.assertEquals("https://vaadin.com/",
                ui.getExternalNavigationURL());
    }

    @Test
    void pageOpen_capturesExternalURL() {
        VaadinTestUiContext ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        // Click "Pay" which calls Page.open() (new tab)
        ui.test(ui.$(Button.class).withText("Pay").single()).click();

        Assertions.assertEquals("https://payment.example.com/checkout?id=123",
                ui.getExternalNavigationURL());
    }
}
