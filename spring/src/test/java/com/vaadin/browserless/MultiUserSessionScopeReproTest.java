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

import com.testapp.sessionscope.Prefs;
import com.testapp.sessionscope.ReproView;
import com.testapp.sessionscope.RequestScopeView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Reproduces and guards
 * <a href= "https://github.com/vaadin/browserless-test/issues/110">#110</a>:
 * each multi-user {@code newUser(...)} must resolve Spring session-scoped beans
 * against that user's own session, so two users get independent instances. A
 * {@code @SessionScope} bean holding a local {@code ValueSignal} is the
 * canonical trigger — without per-user request-context binding the second user
 * reuses the first user's signal, which fails the cross-session check when
 * bound in the UI.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MultiUserSessionScopeReproTest.TestConfig.class)
class MultiUserSessionScopeReproTest {

    @Autowired
    private ApplicationContext applicationContext;

    private SecuredBrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void setUp() {
        app = SpringBrowserlessApplicationContext
                .createSecured(applicationContext, ReproView.class);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void singleUser_sessionScopedBeanResolves() {
        var alice = app.newUser("alice", "USER");
        var aliceWindow = alice.newWindow();
        aliceWindow.navigate(ReproView.class);
        Assertions.assertInstanceOf(ReproView.class,
                aliceWindow.getCurrentView());
    }

    @Test
    void twoUsersEachGetOwnSessionScopedBean() {
        var alice = app.newUser("alice", "USER");
        var bob = app.newUser("bob", "USER");

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

    @Test
    void twoUsersEachGetOwnRequestScopedBean() {
        var alice = app.newUser("alice", "USER");
        var bob = app.newUser("bob", "USER");

        var aliceWindow = alice.newWindow();
        aliceWindow.navigate(RequestScopeView.class);
        RequestScopeView aliceView = (RequestScopeView) aliceWindow
                .getCurrentView();

        // Navigating bob would throw a cross-session IllegalStateException if
        // bob reused alice's request-scoped bean (and therefore alice's
        // signal).
        var bobWindow = bob.newWindow();
        bobWindow.navigate(RequestScopeView.class);
        RequestScopeView bobView = (RequestScopeView) bobWindow
                .getCurrentView();

        Assertions.assertNotSame(aliceView.boundSignal(), bobView.boundSignal(),
                "Each user must resolve its own request-scoped signal instance");
    }

    @Configuration
    @ComponentScan(basePackageClasses = Prefs.class)
    static class TestConfig {
    }
}
