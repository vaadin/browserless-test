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

import java.util.concurrent.atomic.AtomicBoolean;

import com.example.reload.PreservedCounterView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

/**
 * Verifies reload semantics in the programmatic multi-window context: each
 * window has its own preserved-component chain (keyed by a distinct window
 * name), so reloading one window reuses that window's instance without
 * disturbing a sibling window.
 */
class ReloadMultiWindowTest {

    private BrowserlessApplicationContext app;

    @BeforeEach
    void setUp() {
        app = BrowserlessApplicationContext.create(PreservedCounterView.class);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void reloadingOneWindow_preservesItsInstance_andLeavesSiblingUntouched() {
        var user = app.newUser();
        var window1 = user.newWindow();
        var window2 = user.newWindow();

        var view1 = window1.navigate(PreservedCounterView.class);
        var view2 = window2.navigate(PreservedCounterView.class);

        // Same @PreserveOnRefresh route, but distinct per-window instances.
        Assertions.assertNotSame(view1, view2,
                "Each window must get its own preserved instance");

        window1.test(window1.find(Button.class).withId("increment").single())
                .click();
        Assertions.assertEquals(1, view1.getCount());

        var view1AfterReload = window1.reload(PreservedCounterView.class);

        Assertions.assertSame(view1, view1AfterReload,
                "Reloading window1 must reuse its preserved instance");
        Assertions.assertEquals(1, view1AfterReload.getCount(),
                "window1 state must survive its reload");
        Assertions.assertSame(view2, window2.getCurrentView(),
                "window2 must be untouched by window1's reload");
    }

    @Test
    void reloadTriggeredByTheView_windowFollowsTheNewUI() {
        var user = app.newUser();
        var window1 = user.newWindow();
        var window2 = user.newWindow();

        var view1 = window1.navigate(PreservedCounterView.class);
        var view2 = window2.navigate(PreservedCounterView.class);
        window1.test(window1.find(Button.class).withId("increment").single())
                .click();
        var uiBefore = window1.getUI();

        // The view itself asks the browser to refresh, without going through
        // the reload() DSL.
        window1.test(window1.find(Button.class).withId("self-reload").single())
                .click();

        Assertions.assertNotSame(uiBefore, window1.getUI(),
                "Page.reload() from application code must create a fresh UI");
        Assertions.assertSame(view1, window1.getCurrentView(),
                "The preserved instance must survive the refresh");

        // The window must operate on the new UI, not on the detached one.
        window1.test(window1.find(Button.class).withId("increment").single())
                .click();
        Assertions.assertEquals(2, view1.getCount(),
                "The window must interact with the live UI");
        Assertions.assertSame(view2, window2.getCurrentView(),
                "window2 must be untouched by window1's refresh");
    }

    @Test
    void closingWindowRightAfterAViewTriggeredReload_detachesTheLiveUI() {
        var user = app.newUser();
        var window = user.newWindow();

        var view = window.navigate(PreservedCounterView.class);
        window.test(window.find(Button.class).withId("self-reload").single())
                .click();

        // Read the live UI off the view, not off the window: any call on the
        // window would resolve the reloaded UI and hide a stale one from
        // close().
        UI liveUI = view.getUI().orElseThrow();
        AtomicBoolean detached = new AtomicBoolean();
        liveUI.addDetachListener(e -> detached.set(true));

        window.close();

        Assertions.assertTrue(detached.get(),
                "close() must detach the UI the window ended up on, not the one the reload discarded");
    }

    @Test
    void closingWindow_doesNotLeakItsPreservedInstanceToANewWindow() {
        var user = app.newUser();
        var window1 = user.newWindow();

        var view1 = window1.navigate(PreservedCounterView.class);
        window1.test(window1.find(Button.class).withId("increment").single())
                .click();
        Assertions.assertEquals(1, view1.getCount());
        window1.close();

        var window2 = user.newWindow();
        var view2 = window2.navigate(PreservedCounterView.class);

        // A new window must get a new window name: inheriting the closed
        // window's name would resurrect its preserved instance and state.
        Assertions.assertNotSame(view1, view2,
                "A new window must not reuse the closed window's preserved instance");
        Assertions.assertEquals(0, view2.getCount(),
                "A new window must start from a fresh view state");

        // That new name is stable, so the new window's own instance survives
        // its own reload.
        Assertions.assertSame(view2, window2.reload(),
                "The new window must keep its preserved instance across a reload");
    }

    @Test
    void reloadWithUnexpectedTarget_throws_andWindowKeepsUsingTheNewUI() {
        var user = app.newUser();
        var window = user.newWindow();

        var view = window.navigate(PreservedCounterView.class);
        window.test(window.find(Button.class).withId("increment").single())
                .click();

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> window.reload(Button.class),
                "Reloading into an unexpected target type must fail");

        // The reload swapped in a fresh UI before the target type was
        // validated, so the failure must not leave the window bound to the UI
        // that was detached: the window, and the preserved view on it, stay
        // usable.
        Assertions.assertSame(view, window.getCurrentView(),
                "The preserved view must still be the current view");
        window.test(window.find(Button.class).withId("increment").single())
                .click();
        Assertions.assertEquals(2, view.getCount(),
                "The window must still interact with the live UI");
    }
}
