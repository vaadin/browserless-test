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

import com.example.multiuser.SharedCounterView;
import com.example.multiuser.SimpleView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;

/**
 * Tests multi-tab scenarios: one user with multiple windows sharing a session
 * but with independent UI state.
 */
class MultiTabTest {

    private BrowserlessApplicationContext<Void> app;

    @BeforeEach
    void setUp() {
        SharedCounterView.counter.set(0);
        Routes routes = new Routes()
                .autoDiscoverViews(SharedCounterView.class.getPackageName());
        app = BrowserlessApplicationContext.create(routes);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void sameUser_multipleWindows_shareSession() {
        var user = app.newUser();
        var window1 = user.newWindow();
        var window2 = user.newWindow();

        // Same session
        Assertions.assertSame(user.getSession(), window1.getUI().getSession());
        Assertions.assertSame(user.getSession(), window2.getUI().getSession());

        // But different UI instances
        Assertions.assertNotSame(window1.getUI(), window2.getUI());
    }

    @Test
    void sameUser_multipleWindows_independentViews() {
        var user = app.newUser();
        var window1 = user.newWindow();
        var window2 = user.newWindow();

        // Navigate to different views
        window1.navigate(SharedCounterView.class);
        window2.navigate(SimpleView.class);

        // Each window shows its own view
        Assertions.assertInstanceOf(SharedCounterView.class,
                window1.getCurrentView());
        Assertions.assertInstanceOf(SimpleView.class, window2.getCurrentView());
    }
}
