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
package com.vaadin.browserless.quarkus;

import com.testapp.sessionscope.ReproView;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessApplicationContext;

/**
 * Guards
 * <a href= "https://github.com/vaadin/browserless-test/issues/110">#110</a> for
 * Quarkus: each multi-user {@code newUser()} must resolve
 * {@code @VaadinSessionScoped} CDI beans against that user's own
 * {@code VaadinSession}, so two users get independent instances. Unlike Spring
 * — whose {@code @SessionScope} resolves through {@code RequestContextHolder}
 * and needed an explicit per-user request-context binding — Vaadin's CDI scopes
 * resolve directly against {@code VaadinSession.getCurrent()}, which the
 * framework already rebinds per user. This test locks in that isolation.
 */
@QuarkusTest
class MultiUserSessionScopeReproTest {

    private BrowserlessApplicationContext app;

    @BeforeEach
    void setUp() {
        app = QuarkusBrowserlessApplicationContext
                .create("com.testapp.sessionscope");
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void singleUser_sessionScopedBeanResolves() {
        var alice = app.newUser();
        var aliceWindow = alice.newWindow();
        aliceWindow.navigate(ReproView.class);
        Assertions.assertInstanceOf(ReproView.class,
                aliceWindow.getCurrentView());
    }

    @Test
    void twoUsersEachGetOwnSessionScopedBean() {
        var alice = app.newUser();
        var bob = app.newUser();

        var aliceWindow = alice.newWindow();
        aliceWindow.navigate(ReproView.class);
        ReproView aliceView = (ReproView) aliceWindow.getCurrentView();

        // Navigating bob would throw a cross-session IllegalStateException if
        // bob reused alice's session-scoped bean (and therefore alice's
        // signal).
        var bobWindow = bob.newWindow();
        bobWindow.navigate(ReproView.class);
        ReproView bobView = (ReproView) bobWindow.getCurrentView();

        Assertions.assertNotSame(aliceView.boundSignal(), bobView.boundSignal(),
                "Each user must resolve its own session-scoped signal instance");
    }
}
