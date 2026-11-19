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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.example.multiuser.BroadcastView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Tests that only the active window's session is locked, so cross-session UI
 * access behaves as in a running application.
 */
class SessionLockIsolationTest {

    private BrowserlessApplicationContext app;
    private BrowserlessUIContext alice;
    private BrowserlessUIContext bob;

    @BeforeEach
    void setUp() {
        app = BrowserlessApplicationContext.create(BroadcastView.class);
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

        var brokenBroadcast = alice.find(Button.class)
                .withText("Broken broadcast").single();
        var ex = Assertions.assertThrows(IllegalStateException.class,
                () -> alice.test(brokenBroadcast).click());
        Assertions.assertTrue(ex.getMessage().contains(
                "Cannot access state in VaadinSession or UI without locking the session"),
                ex.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    void lockIsolationDisabled_directUpdateOfAnotherUsersUI_isNotDetected() {
        try (var legacy = BrowserlessApplicationContext
                .create(b -> b.withViewPackages(BroadcastView.class)
                        .withSessionLockIsolation(false))) {
            var w1 = legacy.newUser().newWindow();
            var w2 = legacy.newUser().newWindow();
            w1.navigate(BroadcastView.class);
            var otherView = w2.navigate(BroadcastView.class);

            w1.test(w1.find(Button.class).withText("Broken broadcast").single())
                    .click();

            Assertions.assertEquals("Hello", otherView.getMessage());
            Assertions.assertTrue(w2.getUser().getSession().hasLock());
        }
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
    void userSwitch_whileTestHoldsLockAgain_leavesQueuedCommandsToTestsRelease() {
        var aliceSession = alice.getUser().getSession();
        AtomicReference<VaadinSession> ranIn = new AtomicReference<>();
        alice.activate();
        alice.getUI().access(() -> ranIn.set(VaadinSession.getCurrent()));
        aliceSession.lock();

        bob.activate();
        Assertions.assertNull(ranIn.get());
        assertLocked(true, bob);

        aliceSession.unlock();
        Assertions.assertSame(aliceSession, ranIn.get());
        assertLocked(false, alice);
    }

    @Test
    void userSwitch_whileTestHoldsLockAgain_runUIQueueSurfacesFailure() {
        var aliceSession = alice.getUser().getSession();
        alice.activate();
        queueFailingCommand(alice);
        aliceSession.lock();
        bob.activate();

        var ex = Assertions.assertThrows(ExecutionException.class,
                () -> MockVaadin.runUIQueue(false, aliceSession));

        Assertions.assertEquals("boom", ex.getCause().getMessage());
        aliceSession.unlock();
        assertLocked(false, alice);
        assertLocked(true, bob);
    }

    @Test
    void userSwitch_fromDrainedCommand_failsAndKeepsLocksBalanced() {
        alice.activate();
        alice.getUI().access(() -> bob.activate());

        var ex = Assertions.assertThrows(ExecutionException.class,
                () -> bob.activate());
        Assertions.assertInstanceOf(IllegalStateException.class, ex.getCause());
        Assertions.assertTrue(ex.getCause().getMessage()
                .contains("from a UI.access() command"));
        assertLocked(true, alice);
        assertLocked(false, bob);

        bob.activate();
        assertLocked(false, alice);
        assertLocked(true, bob);

        // A hold left over on bob's session would keep it locked
        alice.activate();
        assertLocked(false, bob);
    }

    @Test
    void closeFromDrainedCommand_failsAndChangesNothing() {
        alice.activate();
        alice.getUI().access(() -> alice.close());
        alice.getUI().access(() -> bob.getUser().close());
        alice.getUI().access(() -> app.close());

        var ex = Assertions.assertThrows(ExecutionException.class,
                () -> bob.activate());

        Assertions.assertEquals(2, ex.getSuppressed().length);
        for (Throwable failure : List.of(ex, ex.getSuppressed()[0],
                ex.getSuppressed()[1])) {
            Assertions.assertTrue(
                    failure.getCause().getMessage()
                            .contains("from a UI.access() command"),
                    failure.getCause().getMessage());
        }
        Assertions.assertFalse(alice.isClosed());
        Assertions.assertFalse(bob.isClosed());
        assertLocked(true, alice);
        bob.activate();
        assertLocked(false, alice);
        assertLocked(true, bob);
        Assertions.assertDoesNotThrow(() -> app.newUser().newWindow());
    }

    @Test
    void newUserInAnotherContext_doesNotTakeActiveWindowsLock() {
        AtomicReference<VaadinSession> ranIn = new AtomicReference<>();
        alice.activate();
        alice.getUI().access(() -> ranIn.set(VaadinSession.getCurrent()));
        try (var other = BrowserlessApplicationContext
                .create(BroadcastView.class)) {
            var stranger = other.newUser();

            assertLocked(true, alice);
            Assertions.assertFalse(stranger.getSession().hasLock());
            // Only alice's own hold remains, so switching away drains it.
            bob.activate();
            Assertions.assertSame(alice.getUser().getSession(), ranIn.get());
            alice.activate();

            Assertions
                    .assertDoesNotThrow(() -> stranger.newWindow().activate());
        }
        Assertions.assertDoesNotThrow(() -> alice.activate());
        bob.activate();
        assertLocked(false, alice);
    }

    @Test
    void closeAnotherUser_keepsActiveUserLockedAndUsable() {
        var aliceView = alice.navigate(BroadcastView.class);
        var bobSession = bob.getUser().getSession();

        bob.getUser().close();

        assertLocked(true, alice);
        Assertions.assertFalse(bobSession.hasLock());
        // Only alice's view is left, so this touches only her own session
        alice.test(
                alice.find(Button.class).withText("Broken broadcast").single())
                .click();
        Assertions.assertEquals("Hello", aliceView.getMessage());
    }

    @Test
    void closeActiveWindow_releasesItsLock() {
        var aliceOtherWindow = alice.getUser().newWindow();
        alice.activate();

        alice.close();

        // No window is active any more, so not even the user who still has
        // an open window keeps the lock.
        assertLocked(false, aliceOtherWindow);
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

    @Test
    void failingQueuedCommand_failsUserSwitchAndKeepsOutgoingLock() {
        alice.activate();
        queueFailingCommand(alice);

        var ex = Assertions.assertThrows(ExecutionException.class,
                () -> bob.activate());

        Assertions.assertEquals("boom", ex.getCause().getMessage());
        assertLocked(true, alice);
        assertLocked(false, bob);
    }

    @Test
    void failingQueuedCommand_failsWindowCloseAfterClosingIt() {
        alice.activate();
        queueFailingCommand(alice);

        var ex = Assertions.assertThrows(ExecutionException.class,
                () -> alice.close());

        Assertions.assertEquals("boom", ex.getCause().getMessage());
        Assertions.assertTrue(alice.isClosed());
        assertLocked(false, alice);
    }

    @Test
    void failingQueuedCommand_failsApplicationCloseAfterClosingEverything() {
        var hookRan = new AtomicBoolean();
        var closing = BrowserlessApplicationContext
                .create(b -> b.withViewPackages(BroadcastView.class)
                        .withCloseHook(() -> hookRan.set(true)));
        var first = closing.newUser().newWindow();
        var second = closing.newUser().newWindow();
        first.activate();
        queueFailingCommand(first);
        // A pending signal task releases the lock before the windows close
        closing.getSignalsTestEnvironment().getEffectDispatcher()
                .execute(() -> {
                });

        var ex = Assertions.assertThrows(ExecutionException.class,
                closing::close);

        Assertions.assertEquals("boom", ex.getCause().getMessage());
        Assertions.assertTrue(first.isClosed());
        Assertions.assertTrue(second.isClosed());
        Assertions.assertTrue(hookRan.get());
        Assertions.assertNull(closing.getSignalsTestEnvironment());
        Assertions.assertNull(VaadinService.getCurrent());
    }

    @Test
    void failingUserClose_stillReleasesGlobalState() {
        var hookRan = new AtomicBoolean();
        var closing = BrowserlessApplicationContext
                .create(b -> b.withViewPackages(BroadcastView.class)
                        .withCloseHook(() -> hookRan.set(true)));
        closing.newUser().getSession().getService()
                .addSessionDestroyListener(e -> {
                    throw new IllegalArgumentException("destroy");
                });

        var ex = Assertions.assertThrows(ExecutionException.class,
                closing::close);

        Assertions.assertEquals("destroy", ex.getCause().getMessage());
        Assertions.assertTrue(hookRan.get());
        Assertions.assertNull(closing.getSignalsTestEnvironment());
        Assertions.assertNull(VaadinService.getCurrent());
    }

    private static void queueFailingCommand(BrowserlessUIContext window) {
        window.getUI().access(() -> {
            throw new IllegalArgumentException("boom");
        });
    }

    private static void assertLocked(boolean expected,
            BrowserlessUIContext window) {
        Assertions.assertEquals(expected,
                window.getUser().getSession().hasLock());
    }
}
