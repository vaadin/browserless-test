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
package com.vaadin.browserless.mocks;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * {@link MockInstantiator} must forward every {@link Instantiator} method to
 * the environment's own instantiator, including the ones Flow declares as Java
 * {@code default} methods. Kotlin interface delegation only generates
 * forwarders for abstract members, so a {@code default} method that is not
 * overridden explicitly resolves to Flow's default implementation and never
 * reaches the delegate — silently dropping, for example, the Spring
 * {@link PageTitleGenerator} bean lookup.
 */
class MockInstantiatorDelegationTest {

    @Test
    void everyInstantiatorMethod_isOverriddenByMockInstantiator() {
        List<String> missing = new ArrayList<>();

        for (Method method : Instantiator.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    || method.isSynthetic()) {
                continue;
            }
            try {
                MockInstantiator.class.getDeclaredMethod(method.getName(),
                        method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                missing.add(method.getName());
            }
        }

        Assertions.assertTrue(missing.isEmpty(),
                () -> "MockInstantiator must override every Instantiator method to reach the delegate, but "
                        + missing
                        + " resolves to the interface default implementation instead");
    }

    @Test
    void pageTitleGenerator_isTakenFromTheDelegate() {
        PageTitleGenerator generator = context -> "generated";
        Instantiator mockInstantiator = MockInstantiator
                .create(instantiatorWithPageTitleGenerator(generator));

        Assertions.assertSame(generator,
                mockInstantiator.getPageTitleGenerator(),
                "The delegate's page title generator should be visible through MockInstantiator");
    }

    private static Instantiator instantiatorWithPageTitleGenerator(
            PageTitleGenerator generator) {
        return (Instantiator) Proxy.newProxyInstance(
                MockInstantiatorDelegationTest.class.getClassLoader(),
                new Class<?>[] { Instantiator.class },
                (proxy, method, args) -> {
                    if ("getPageTitleGenerator".equals(method.getName())) {
                        return generator;
                    }
                    return method.getReturnType() == Stream.class
                            ? Stream.empty()
                            : null;
                });
    }
}
