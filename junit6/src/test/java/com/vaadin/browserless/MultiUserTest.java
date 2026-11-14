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

import java.util.concurrent.atomic.AtomicReference;

import com.example.multiuser.SharedCounterView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.server.VaadinSession;

/**
 * Tests multi-user scenarios: two independent users sharing application-level
 * state via a static counter.
 */
class MultiUserTest {

    private BrowserlessApplicationContext app;

    @BeforeEach
    void setUp() {
        SharedCounterView.counter.set(0);
        app = BrowserlessApplicationContext.create(SharedCounterView.class);
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

    @Test
    void typedTestOverload_activatesItsWindow() {
        var user1 = app.newUser();
        var w1 = user1.newWindow();
        w1.navigate(SharedCounterView.class);
        Button increment = w1.find(Button.class).withText("Increment").single();
        AtomicReference<UI> listenerUI = new AtomicReference<>();
        AtomicReference<VaadinSession> listenerSession = new AtomicReference<>();
        increment.addClickListener(e -> {
            listenerUI.set(UI.getCurrent());
            listenerSession.set(VaadinSession.getCurrent());
        });

        var w2 = app.newUser().newWindow();
        w2.navigate(SharedCounterView.class);

        w1.test(increment).click();

        Assertions.assertSame(w1.getUI(), listenerUI.get(),
                "Listener should run with the clicked window's UI current");
        Assertions.assertSame(user1.getSession(), listenerSession.get(),
                "Listener should run with the clicked window's session current");
    }

    @Test
    void popoverFor_searchesItsOwnWindow() {
        var w1 = app.newUser().newWindow();
        SharedCounterView view = w1.navigate(SharedCounterView.class);
        Button target = new Button("Target");
        Popover popover = new Popover();
        popover.setTarget(target);
        view.add(target, popover);

        var w2 = app.newUser().newWindow();
        w2.navigate(SharedCounterView.class);

        w1.popoverFor(target).clickTarget();

        Assertions.assertTrue(popover.isOpened(),
                "popoverFor should find the popover in its own window");
    }
}
