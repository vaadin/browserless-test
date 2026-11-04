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
package com.vaadin.browserless.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Covers the entry points that {@link TestingLifecycleHook} keeps for code
 * compiled against the former Kotlin interface: {@code Companion.getDefault()}
 * and the {@code DefaultImpls} static methods.
 */
class TestingLifecycleHookTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void companionDefault_runsRoundtripBeforeLookup() {
        AtomicBoolean roundtrip = expectRoundtrip();

        TestingLifecycleHook.Companion.getDefault().awaitBeforeLookup();

        Assertions.assertTrue(roundtrip.get());
    }

    @Test
    void defaultImplsAwaitBeforeLookup_runsRoundtrip() {
        AtomicBoolean roundtrip = expectRoundtrip();

        TestingLifecycleHook.DefaultImpls
                .awaitBeforeLookup(new TestingLifecycleHook() {
                    @Override
                    public void awaitBeforeLookup() {
                        // overridden, so only DefaultImpls can run the
                        // roundtrip
                    }
                });

        Assertions.assertTrue(roundtrip.get());
    }

    @Test
    void defaultImplsGetAllChildren_ignoresTheOverride() {
        TestingLifecycleHook hook = new TestingLifecycleHook() {
            @Override
            public List<Component> getAllChildren(Component component) {
                return List.of();
            }
        };
        Span a = new Span("a");
        Span b = new Span("b");
        Div parent = new Div(a, b);

        Assertions.assertEquals(List.of(), hook.getAllChildren(parent));
        Assertions.assertEquals(List.of(a, b),
                TestingLifecycleHook.DefaultImpls.getAllChildren(hook, parent));
    }

    private static AtomicBoolean expectRoundtrip() {
        AtomicBoolean roundtrip = new AtomicBoolean();
        UI ui = UI.getCurrent();
        ui.beforeClientResponse(ui, context -> roundtrip.set(true));
        return roundtrip;
    }
}
