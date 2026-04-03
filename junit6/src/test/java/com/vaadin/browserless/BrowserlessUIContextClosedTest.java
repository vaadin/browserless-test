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

import com.example.multiuser.SimpleView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.html.Div;

/**
 * Tests that calling methods on a closed {@link BrowserlessUIContext}
 * throws {@link IllegalStateException}.
 */
class BrowserlessUIContextClosedTest {

    private BrowserlessApplicationContext<Void> app;

    @BeforeEach
    void setUp() {
        Routes routes = new Routes().autoDiscoverViews(
                SimpleView.class.getPackageName());
        app = BrowserlessApplicationContext.create(routes);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void activate_afterClose_throws() {
        var window = app.newUser().newWindow();
        window.close();

        Assertions.assertThrows(IllegalStateException.class,
                window::activate,
                "activate() on a closed context should throw");
    }

    @Test
    void navigate_afterClose_throws() {
        var window = app.newUser().newWindow();
        window.close();

        Assertions.assertThrows(IllegalStateException.class,
                () -> window.navigate(SimpleView.class),
                "navigate() on a closed context should throw");
    }

    @Test
    void query_afterClose_throws() {
        var window = app.newUser().newWindow();
        window.close();

        Assertions.assertThrows(IllegalStateException.class,
                () -> window.$(Div.class),
                "$() on a closed context should throw");
    }

    @Test
    void close_isIdempotent() {
        var window = app.newUser().newWindow();
        window.close();
        // Second close should not throw
        Assertions.assertDoesNotThrow(window::close);
    }
}
