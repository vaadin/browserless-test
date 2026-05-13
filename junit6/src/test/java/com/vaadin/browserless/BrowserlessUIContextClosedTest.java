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

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.example.SingleParam;
import com.example.multiuser.SimpleView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.flow.component.html.Div;

/**
 * Tests that calling methods on a closed {@link BrowserlessUIContext} throws
 * {@link IllegalStateException}.
 */
class BrowserlessUIContextClosedTest {

    private BrowserlessApplicationContext app;

    @BeforeEach
    void setUp() {
        Routes routes = new Routes()
                .autoDiscoverViews(SimpleView.class.getPackageName())
                .autoDiscoverViews(SingleParam.class.getPackageName());
        app = BrowserlessApplicationContext.create(routes);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dslMethodsThatRequireOpenContext")
    void dslMethod_afterClose_throws(String name,
            Consumer<BrowserlessUIContext> invocation) {
        var window = app.newUser().newWindow();
        window.close();

        Assertions.assertThrows(IllegalStateException.class,
                () -> invocation.accept(window),
                name + " on a closed context should throw");
    }

    static Stream<Arguments> dslMethodsThatRequireOpenContext() {
        return Stream.of(row("activate()", BrowserlessUIContext::activate),
                row("navigate(Class)", w -> w.navigate(SimpleView.class)),
                row("navigate(Class, parameter)",
                        w -> w.navigate(SingleParam.class, "x")),
                row("navigate(Class, parameters)",
                        w -> w.navigate(SimpleView.class, Map.of())),
                row("navigate(String, Class)",
                        w -> w.navigate("simple", SimpleView.class)),
                row("find(Class)", w -> w.find(Div.class)),
                row("find(Class, Component)",
                        w -> w.find(Div.class, new Div())),
                row("findInView(Class)", w -> w.findInView(SimpleView.class)),
                // SimpleView (VerticalLayout) has no TesterWrappers overload,
                // so it routes to the generic test(Y) which calls activate().
                row("test(Component)", w -> w.test(new SimpleView())),
                row("getCurrentView()", BrowserlessUIContext::getCurrentView),
                row("roundTrip()", BrowserlessUIContext::roundTrip),
                row("getExternalNavigationURL()",
                        BrowserlessUIContext::getExternalNavigationURL),
                row("getExternalNavigationURL(String)",
                        w -> w.getExternalNavigationURL("popup")),
                row("getOpenedWindows()",
                        BrowserlessUIContext::getOpenedWindows));
    }

    private static Arguments row(String name,
            Consumer<BrowserlessUIContext> invocation) {
        return Arguments.of(Named.of(name, name), invocation);
    }

    @Test
    void close_isIdempotent() {
        var window = app.newUser().newWindow();
        window.close();
        // Second close should not throw
        Assertions.assertDoesNotThrow(window::close);
    }

    @Test
    void newWindow_uiFactoryThrows_doesNotLeakActiveContext() {
        UIFactory throwingFactory = () -> {
            throw new IllegalStateException("simulated UI factory failure");
        };
        BrowserlessApplicationContext failingApp = new BrowserlessApplicationContext.Builder(
                new Routes()
                        .autoDiscoverViews(SimpleView.class.getPackageName()))
                .withUIFactory(throwingFactory).build();
        BrowserlessUIContext leakedActive;
        try {
            var failingUser = failingApp.newUser();
            Assertions.assertThrows(RuntimeException.class,
                    failingUser::newWindow,
                    "newWindow() should propagate the UI factory failure");
            leakedActive = BrowserlessUIContext.getActive();
        } finally {
            // Defensively clear so a failing assertion does not poison other
            // tests sharing this thread.
            BrowserlessUIContext.clearActiveContext();
            failingApp.close();
        }
        Assertions.assertNull(leakedActive,
                "A failed UI construction must not leave a half-built"
                        + " BrowserlessUIContext as the active context");
    }
}
