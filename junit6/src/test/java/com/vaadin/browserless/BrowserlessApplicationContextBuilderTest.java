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

import com.example.base.signals.SignalsView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.builderfixtures.WidgetComponent;
import com.vaadin.browserless.builderfixtures.WidgetTester;

/**
 * Exercises the new {@link BrowserlessApplicationContext} factory and builder
 * surface introduced to close
 * <a href="https://github.com/vaadin/browserless-test/issues/61">issue #61</a>:
 * factory overloads that take view packages instead of an internal
 * {@code Routes}, and a builder method that scans for custom
 * {@link ComponentTester} implementations.
 */
class BrowserlessApplicationContextBuilderTest {

    @Test
    void builderWithComponentTesterPackages_resolvesCustomTester() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withViewPackages(SignalsView.class)
                        .withComponentTesterPackages(WidgetComponent.class))) {
            var window = app.newUser().newWindow();
            ComponentTester<?> tester = window.test(new WidgetComponent());
            Assertions.assertInstanceOf(WidgetTester.class, tester,
                    "Custom tester scanned via the builder should resolve");
        }
    }

    @Test
    void createWithViewPackageClass_opensContextWithoutRoutesArg() {
        try (var app = BrowserlessApplicationContext
                .create(SignalsView.class)) {
            var window = app.newUser().newWindow();
            var view = window.navigate(SignalsView.class);
            Assertions.assertNotNull(view);
        }
    }

    @Test
    void createWithViewPackageString_opensContextWithoutRoutesArg() {
        try (var app = BrowserlessApplicationContext
                .create(SignalsView.class.getPackageName())) {
            var window = app.newUser().newWindow();
            var view = window.navigate(SignalsView.class);
            Assertions.assertNotNull(view);
        }
    }

    @Test
    void createWithUnsecuredConfigurer_returnsUnsecuredContext() {
        try (var app = BrowserlessApplicationContext
                .create(b -> b.withViewPackages(SignalsView.class))) {
            // Compile-time check on the variable type is enough; runtime
            // assertion guards against a regression that hands back a
            // subclass with security wiring.
            Assertions.assertFalse(
                    app instanceof SecuredBrowserlessApplicationContext<?>,
                    "Unsecured configurer must not return a secured context");
            var window = app.newUser().newWindow();
            Assertions.assertNotNull(window.navigate(SignalsView.class));
        }
    }

    @Test
    void createSecuredWithConfigurer_returnsSecuredContext() {
        SecurityContextHandler<String> handler = new RecordingHandler();

        SecuredBrowserlessApplicationContext<String> app = BrowserlessApplicationContext
                .createSecured(b -> b.withViewPackages(SignalsView.class)
                        .withSecurityContextHandler(handler));
        try (app) {
            // The variable type is the proof that the configurer returned
            // a SecuredBuilder<C> and the factory returned the matching
            // secured context type.
            Assertions.assertSame(handler, app.getSecurityContextHandler(),
                    "Configurer-installed handler must propagate to the"
                            + " built context");
            var window = app.newUser("alice").newWindow();
            Assertions.assertNotNull(window.navigate(SignalsView.class));
        }
    }

    private static final class RecordingHandler
            implements SecurityContextHandler<String> {
        @Override
        public void setupAuthentication(String credentials) {
        }

        @Override
        public Object saveContext() {
            return null;
        }

        @Override
        public void restoreContext(Object snapshot) {
        }

        @Override
        public void clearContext() {
        }

        @Override
        public String createCredentials(String username, String... roles) {
            return username;
        }
    }
}
