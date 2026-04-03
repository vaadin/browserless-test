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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;

/**
 * Tests multi-user scenarios: two independent users sharing
 * application-level state via a static counter.
 */
class MultiUserTest {

    private BrowserlessApplicationContext<Void> app;

    @BeforeEach
    void setUp() {
        SharedCounterView.counter.set(0);
        Routes routes = new Routes().autoDiscoverViews(
                SharedCounterView.class.getPackageName());
        app = BrowserlessApplicationContext.create(routes);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void twoUsers_shareApplicationState() {
        var user1 = app.newUser();
        var w1 = user1.newWindow();
        w1.navigate(SharedCounterView.class);

        var user2 = app.newUser();
        var w2 = user2.newWindow();
        w2.navigate(SharedCounterView.class);

        // User1 increments counter
        w1.test(w1.find(Button.class).withText("Increment").single()).click();
        Assertions.assertEquals("Count: 1",
                w1.find(Paragraph.class).single().getText());

        // User2 doesn't see the change yet (own UI state)
        Assertions.assertEquals("Count: 0",
                w2.find(Paragraph.class).single().getText());

        // User2 refreshes to see updated shared state
        w2.test(w2.find(Button.class).withText("Refresh").single()).click();
        Assertions.assertEquals("Count: 1",
                w2.find(Paragraph.class).single().getText());
    }

    @Test
    void users_haveIndependentSessions() {
        var user1 = app.newUser();
        var w1 = user1.newWindow();

        var user2 = app.newUser();
        var w2 = user2.newWindow();

        Assertions.assertNotSame(user1.getSession(), user2.getSession(),
                "Users should have independent sessions");
        Assertions.assertNotSame(w1.getUI(), w2.getUI(),
                "Windows should have independent UIs");
    }
}
