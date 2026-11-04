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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Covers the deprecated {@code *Kt} classes that keep code compiled against the
 * former Kotlin file facades working.
 */
@SuppressWarnings("removal")
class KtFacadeForwardingTest {

    private static final Map<Class<?>, Class<?>> FORWARDERS = Map.ofEntries(
            Map.entry(BasicUtilsKt.class, BasicUtils.class),
            Map.entry(ComponentUtilsKt.class, ComponentUtils.class),
            Map.entry(DepthFirstTreeIteratorKt.class,
                    DepthFirstTreeIterator.class),
            Map.entry(ElementUtilsKt.class, ElementUtils.class),
            Map.entry(LocatorKt.class, Locator.class),
            Map.entry(MockVaadinKt.class, MockVaadin.class),
            Map.entry(PrettyPrintTreeKt.class, PrettyPrintTree.class),
            Map.entry(RenderersKt.class, Renderers.class),
            Map.entry(RoutesKt.class, Routes.class),
            Map.entry(ShortcutsKt.class, Shortcuts.class),
            Map.entry(TestingLifecycleHookKt.class,
                    TestingLifecycleHooks.class),
            Map.entry(UtilsKt.class, Utils.class));

    static Stream<Arguments> forwarders() {
        return FORWARDERS.entrySet().stream()
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @ParameterizedTest
    @MethodSource("forwarders")
    void everyForwarderMethod_existsOnTheTarget(Class<?> forwarder,
            Class<?> target) {
        Assertions.assertTrue(forwarder.isAnnotationPresent(Deprecated.class));
        List<String> missing = new ArrayList<>();
        for (Method method : forwarder.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            try {
                Method targetMethod = target.getMethod(method.getName(),
                        method.getParameterTypes());
                if (!Modifier.isStatic(targetMethod.getModifiers())
                        || !targetMethod.getReturnType()
                                .equals(method.getReturnType())) {
                    missing.add(method.toGenericString());
                }
            } catch (NoSuchMethodException e) {
                missing.add(method.toGenericString());
            }
        }
        Assertions.assertEquals(List.of(), missing);
    }

    @Test
    void testingLifecycleHookKt_forwardsToTestingLifecycleHooks() {
        TestingLifecycleHook original = TestingLifecycleHooks
                .getTestingLifecycleHook();
        TestingLifecycleHook hook = new TestingLifecycleHook() {
        };
        try {
            TestingLifecycleHookKt.setTestingLifecycleHook(hook);
            Assertions.assertSame(hook,
                    TestingLifecycleHooks.getTestingLifecycleHook());
        } finally {
            TestingLifecycleHooks.setTestingLifecycleHook(original);
        }
    }

    @Test
    void depthFirstTreeIteratorKt_walksTheSameTree() {
        Span child = new Span("child");
        Div root = new Div(child);

        Assertions.assertEquals(List.of(root, child),
                toList(DepthFirstTreeIteratorKt.walk(root)));
    }

    @Test
    void overloadedForwarders_reachTheMatchingOverload() {
        Div div = new Div();
        ComponentUtilsKt.addClassNames2(div, "a b");
        ComponentUtilsKt.addClassNames2(div, "c", "d e");
        Assertions.assertEquals(List.of("a", "b", "c", "d", "e"),
                List.copyOf(div.getClassNames()));

        Span span = new Span();
        span.setId("x");
        Assertions.assertEquals(PrettyPrintTree.toPrettyString(span),
                PrettyPrintTreeKt.toPrettyString(span));
    }

    private static List<Component> toList(Iterable<Component> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }
}
