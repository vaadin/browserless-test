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

import java.util.List;
import java.util.Map;

import com.example.multiuser.ExternalNavigationView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.button.Button;

/**
 * Tests capturing external navigation URLs triggered by
 * {@code Page.setLocation()} and {@code Page.open()}.
 */
class ExternalNavigationTest {

    private BrowserlessApplicationContext<Void> app;

    @BeforeEach
    void setUp() {
        Routes routes = new Routes().autoDiscoverViews(
                ExternalNavigationView.class.getPackageName());
        app = BrowserlessApplicationContext.create(routes);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void getExternalNavigationURL_capturesSetLocation() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        // No navigation yet
        Assertions.assertNull(ui.getExternalNavigationURL());

        // Click Page.setLocation button
        ui.test(ui.$(Button.class).withText("Go to Vaadin").single()).click();

        Assertions.assertEquals("https://vaadin.com/",
                ui.getExternalNavigationURL());
    }

    @Test
    void getExternalNavigationURL_capturesPageOpen() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        // Page.open() uses _blank, so getExternalNavigationURL() (which
        // returns _self navigations) should remain null
        ui.test(ui.$(Button.class).withText("Pay").single()).click();

        Assertions.assertNull(ui.getExternalNavigationURL());
        Assertions.assertEquals("https://payment.example.com/checkout?id=123",
                ui.getExternalNavigationURL("_blank"));
    }

    @Test
    void getExternalNavigationURL_namedWindow() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Open Help").single()).click();

        Assertions.assertEquals("https://help.example.com/",
                ui.getExternalNavigationURL("helpWindow"));
        // Should not appear as a _self navigation
        Assertions.assertNull(ui.getExternalNavigationURL());
    }

    @Test
    void getExternalNavigationURL_parentNormalizesToSelf() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Open Parent").single()).click();

        Assertions.assertEquals("https://parent.example.com/",
                ui.getExternalNavigationURL());
    }

    @Test
    void getExternalNavigationURL_topNormalizesToSelf() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Open Top").single()).click();

        Assertions.assertEquals("https://top.example.com/",
                ui.getExternalNavigationURL());
    }

    @Test
    void getExternalNavigationURL_setLocationOverwritesPrevious() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Go to Vaadin").single()).click();
        ui.test(ui.$(Button.class).withText("Open Parent").single()).click();

        // _parent is normalized to _self, so it overwrites the setLocation
        Assertions.assertEquals("https://parent.example.com/",
                ui.getExternalNavigationURL());
    }

    @Test
    void getExternalNavigationURL_isNonDestructive() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Go to Vaadin").single()).click();

        // Calling multiple times returns the same value
        Assertions.assertEquals("https://vaadin.com/",
                ui.getExternalNavigationURL());
        Assertions.assertEquals("https://vaadin.com/",
                ui.getExternalNavigationURL());
    }

    @Test
    void getOpenedWindows_blankAccumulatesAllCalls() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Open Tab 1").single()).click();
        ui.test(ui.$(Button.class).withText("Open Tab 2").single()).click();

        Map<String, List<String>> opened = ui.getOpenedWindows();
        Assertions.assertEquals(List.of("https://tab1.example.com/",
                "https://tab2.example.com/"), opened.get("_blank"));
    }

    @Test
    void getOpenedWindows_excludesSelfNavigations() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Go to Vaadin").single()).click();
        ui.test(ui.$(Button.class).withText("Open Parent").single()).click();
        ui.test(ui.$(Button.class).withText("Open Top").single()).click();

        // All three are _self navigations, so openedWindows should be empty
        Assertions.assertTrue(ui.getOpenedWindows().isEmpty());
    }

    @Test
    void getOpenedWindows_namedWindowLastUrlWins() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        // Open two different URLs in the same named window
        ui.test(ui.$(Button.class).withText("Open Help").single()).click();
        // Navigate the same named window to a different URL via setLocation
        ui.getUI().getPage().open("https://help.example.com/updated",
                "helpWindow");

        Map<String, List<String>> opened = ui.getOpenedWindows();
        Assertions.assertEquals(List.of("https://help.example.com/updated"),
                opened.get("helpWindow"));
    }

    @Test
    void getOpenedWindows_mixedWindowTypes() {
        var ui = app.newUser().newWindow();
        ui.navigate(ExternalNavigationView.class);

        ui.test(ui.$(Button.class).withText("Go to Vaadin").single()).click();
        ui.test(ui.$(Button.class).withText("Open Help").single()).click();
        ui.test(ui.$(Button.class).withText("Open Tab 1").single()).click();
        ui.test(ui.$(Button.class).withText("Open Tab 2").single()).click();

        // _self navigation captured separately
        Assertions.assertEquals("https://vaadin.com/",
                ui.getExternalNavigationURL());

        // openedWindows contains only non-self entries
        Map<String, List<String>> opened = ui.getOpenedWindows();
        Assertions.assertEquals(2, opened.size());
        Assertions.assertEquals(List.of("https://help.example.com/"),
                opened.get("helpWindow"));
        Assertions.assertEquals(List.of("https://tab1.example.com/",
                "https://tab2.example.com/"), opened.get("_blank"));
    }
}
