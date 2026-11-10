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

import com.example.multiuser.BroadcastView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinSession;

/**
 * Tests that, with session lock isolation, only the active window's session is
 * locked, so cross-session UI access behaves as in a running application.
 */
class SessionLockIsolationTest {

    private BrowserlessApplicationContext app;
    private BrowserlessUIContext alice;
    private BrowserlessUIContext bob;

    @BeforeEach
    void setUp() {
        app = BrowserlessApplicationContext
                .create(b -> b.withViewPackages(BroadcastView.class)
                        .withSessionLockIsolation(true));
        alice = app.newUser().newWindow();
        bob = app.newUser().newWindow();
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void onlyActiveUsersSessionIsLocked() {
        alice.activate();
        assertLocked(true, alice);
        assertLocked(false, bob);

        bob.activate();
        assertLocked(false, alice);
        assertLocked(true, bob);
    }

    @Test
    void newUser_whileAnotherUserIsActive_leavesActiveUserLocked() {
        alice.activate();
        var carol = app.newUser();

        assertLocked(true, alice);
        Assertions.assertFalse(carol.getSession().hasLock());
    }

    @Test
    void directUpdateOfAnotherUsersUI_fails() {
        alice.navigate(BroadcastView.class);
        bob.navigate(BroadcastView.class);

        var broadcast = alice.find(Button.class).withText("Broadcast").single();
        // The mocked session checks the lock with an assert, so the failure
        // is an AssertionError rather than the IllegalStateException a
        // development-mode server throws.
        var ex = Assertions.assertThrows(AssertionError.class,
                () -> alice.test(broadcast).click());
        Assertions.assertTrue(ex.getMessage().contains(
                "Cannot access state in VaadinSession or UI without locking the session"),
                ex.getMessage());
    }

    @Test
    void accessToAnotherUsersUI_runsInThatUsersSession() {
        alice.navigate(BroadcastView.class);
        var bobView = bob.navigate(BroadcastView.class);

        alice.test(alice.find(Button.class).withText("Broadcast with access")
                .single()).click();

        Assertions.assertEquals("Hello", bobView.getMessage());
        Assertions.assertSame(bob.getUser().getSession(),
                bobView.getUpdatedInSession());
    }

    @Test
    void accessQueuedWhileActive_runsOnUserSwitchOnly() {
        var sameUserWindow = alice.getUser().newWindow();
        AtomicReference<VaadinSession> ranIn = new AtomicReference<>();
        alice.activate();
        alice.getUI().access(() -> ranIn.set(VaadinSession.getCurrent()));

        sameUserWindow.activate();
        Assertions.assertNull(ranIn.get());

        bob.activate();
        Assertions.assertSame(alice.getUser().getSession(), ranIn.get());
    }

    @Test
    void closeAnotherUser_keepsActiveUserLockedAndUsable() {
        var aliceView = alice.navigate(BroadcastView.class);
        var bobSession = bob.getUser().getSession();

        bob.getUser().close();

        assertLocked(true, alice);
        Assertions.assertFalse(bobSession.hasLock());
        alice.test(alice.find(Button.class).withText("Broadcast").single())
                .click();
        Assertions.assertEquals("Hello", aliceView.getMessage());
    }

    @Test
    void closeActiveWindow_releasesItsLock() {
        alice.activate();

        alice.close();

        assertLocked(false, alice);
        assertLocked(false, bob);
    }

    @Test
    void close_runsQueuedCommandsAndReleasesAllSessionLocks() {
        var aliceSession = alice.getUser().getSession();
        var bobSession = bob.getUser().getSession();
        AtomicReference<VaadinSession> ranIn = new AtomicReference<>();
        alice.activate();
        alice.getUI().access(() -> ranIn.set(VaadinSession.getCurrent()));

        app.close();

        Assertions.assertSame(aliceSession, ranIn.get());
        Assertions.assertFalse(aliceSession.hasLock());
        Assertions.assertFalse(bobSession.hasLock());
    }

    private static void assertLocked(boolean expected,
            BrowserlessUIContext window) {
        Assertions.assertEquals(expected,
                window.getUser().getSession().hasLock());
    }
}
