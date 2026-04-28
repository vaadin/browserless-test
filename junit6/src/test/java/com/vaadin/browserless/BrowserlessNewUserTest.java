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

import com.example.multiuser.SimpleView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.Routes;

/**
 * Tests construction-time semantics of {@link BrowserlessUserContext}: order of
 * security setup vs. session-init listener firing.
 */
class BrowserlessNewUserTest {

    private Routes routes;

    @BeforeEach
    void setUp() {
        routes = new Routes()
                .autoDiscoverViews(SimpleView.class.getPackageName());
    }

    @Test
    void newUser_sessionInitListenerSeesThisUsersSecurity() {
        var handler = new CapturingSecurityHandler();
        try (var app = BrowserlessApplicationContext.<String> builder(routes)
                .withSecurityContextHandler(handler).build()) {

            var observed = new AtomicReference<String>();
            app.getService().addSessionInitListener(
                    event -> observed.set(handler.live.get()));

            app.newUser("alice");

            Assertions.assertEquals("alice", observed.get(),
                    "session-init listener should observe the new user's"
                            + " identity, mirroring the Vaadin+Spring flow"
                            + " where the security filter chain runs"
                            + " before the servlet — not whatever auth"
                            + " the calling thread happened to carry"
                            + " before newUser()");
        }
    }
}
