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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
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
    void companionDefault_returnsTheBuiltInHook() {
        TestingLifecycleHook hook = TestingLifecycleHook.Companion.getDefault();
        Span child = new Span("child");
        Div parent = new Div(child);

        Assertions.assertEquals(List.of(child), hook.getAllChildren(parent));
    }

    @Test
    void defaultImplsGetAllChildren_matchesTheInterfaceDefault() {
        TestingLifecycleHook hook = new TestingLifecycleHook() {
        };
        Div parent = new Div(new Span("a"), new Span("b"));

        List<Component> expected = hook.getAllChildren(parent);
        Assertions.assertEquals(2, expected.size());
        Assertions.assertEquals(expected,
                TestingLifecycleHook.DefaultImpls.getAllChildren(hook, parent));
    }
}
