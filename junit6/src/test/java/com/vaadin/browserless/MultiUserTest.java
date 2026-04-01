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

import com.example.base.SharedCounterView;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Set;

/**
 * Multi-user test without Spring. Uses
 * {@link VaadinTestApplicationContext#create(Routes)} with plain Vaadin mocks.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ViewPackages(classes = SharedCounterView.class)
class MultiUserTest {

    VaadinTestApplicationContext app;

    @BeforeAll
    void setup() {
        Routes routes = VaadinTestApplicationContext.discoverRoutes(
                getClass(), Set.of());
        app = VaadinTestApplicationContext.create(routes);
    }

    @AfterAll
    void tearDown() {
        app.close();
    }

    @BeforeEach
    void resetCounter() {
        SharedCounterView.counter.set(0);
    }

    @Test
    void twoUsers_sharedState_visibleAcrossWindows() {
        VaadinTestUiContext ui1 = app.newUser().newWindow();
        VaadinTestUiContext ui2 = app.newUser().newWindow();

        ui1.navigate(SharedCounterView.class);
        ui2.navigate(SharedCounterView.class);

        // Initial state
        Assertions.assertEquals("Count:0",
                ui1.$(Paragraph.class).first().getText());
        Assertions.assertEquals("Count:0",
                ui2.$(Paragraph.class).first().getText());

        // User 1 clicks "Increment" three times
        for (int i = 0; i < 3; i++) {
            ui1.test(ui1.$(Button.class).withText("Increment").single())
                    .click();
        }

        // User 1 sees the updated count
        Assertions.assertEquals("Count:3",
                ui1.$(Paragraph.class).first().getText());

        // User 2 still sees the old count in their UI
        Assertions.assertEquals("Count:0",
                ui2.$(Paragraph.class).first().getText());

        // User 2 clicks "Refresh" to read the shared state
        ui2.test(ui2.$(Button.class).withText("Refresh").single()).click();

        // Now user 2 sees the updated count
        Assertions.assertEquals("Count:3",
                ui2.$(Paragraph.class).first().getText());
    }

    @Test
    void multipleWindows_sameUser_independentUIs() {
        VaadinTestUserContext user = app.newUser();
        VaadinTestUiContext window1 = user.newWindow();
        VaadinTestUiContext window2 = user.newWindow();

        window1.navigate(SharedCounterView.class);
        window2.navigate(SharedCounterView.class);

        // Increment in window 1
        window1.test(
                window1.$(Button.class).withText("Increment").single())
                .click();

        // Window 1 sees the change, window 2 does not (separate UI instances)
        Assertions.assertEquals("Count:1",
                window1.$(Paragraph.class).first().getText());
        Assertions.assertEquals("Count:0",
                window2.$(Paragraph.class).first().getText());

        // Refresh window 2
        window2.test(
                window2.$(Button.class).withText("Refresh").single())
                .click();
        Assertions.assertEquals("Count:1",
                window2.$(Paragraph.class).first().getText());
    }
}
