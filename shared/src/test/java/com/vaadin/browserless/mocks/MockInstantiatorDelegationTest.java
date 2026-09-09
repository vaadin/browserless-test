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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;

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

        for (Method method : instantiatorMethods()) {
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
    void everyInstantiatorMethod_reachesTheDelegate() {
        List<String> notForwarded = new ArrayList<>();

        for (Method method : instantiatorMethods()) {
            Set<String> called = new LinkedHashSet<>();
            invoke(MockInstantiator.create(recordingInstantiator(called)),
                    method);
            if (!called.contains(method.getName())) {
                notForwarded.add(method.getName());
            }
        }

        Assertions.assertTrue(notForwarded.isEmpty(),
                () -> "Calling these methods on MockInstantiator never reached the delegate: "
                        + notForwarded);
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

    private static List<Method> instantiatorMethods() {
        List<Method> methods = new ArrayList<>();
        for (Method method : Instantiator.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    || method.isSynthetic()) {
                continue;
            }
            methods.add(method);
        }
        return methods;
    }

    private static void invoke(Instantiator instantiator, Method method) {
        Object[] arguments = new Object[method.getParameterCount()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = argumentFor(method.getParameterTypes()[i]);
        }
        try {
            method.invoke(instantiator, arguments);
        } catch (InvocationTargetException e) {
            // What the call does with the stub value the recording delegate
            // returns is irrelevant here; only whether the delegate saw it
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "Failed to invoke Instantiator." + method.getName(), e);
        }
    }

    private static Object argumentFor(Class<?> parameterType) {
        if (parameterType == Class.class) {
            return String.class;
        }
        if (parameterType == Stream.class) {
            return Stream.empty();
        }
        if (parameterType == NavigationEvent.class) {
            return new NavigationEvent(new Router(routeRegistry()),
                    new Location(""), new UI(), NavigationTrigger.PROGRAMMATIC);
        }
        return new Object();
    }

    /**
     * An {@link Instantiator} recording the names of the methods called on it,
     * returning values benign enough for the caller to carry on.
     */
    private static Instantiator recordingInstantiator(Set<String> called) {
        return (Instantiator) Proxy.newProxyInstance(
                MockInstantiatorDelegationTest.class.getClassLoader(),
                new Class<?>[] { Instantiator.class },
                (proxy, method, args) -> {
                    called.add(method.getName());
                    return returnValueFor(method.getReturnType());
                });
    }

    private static Object returnValueFor(Class<?> returnType) {
        if (returnType == Stream.class) {
            return Stream.empty();
        }
        if (returnType == Class.class) {
            return String.class;
        }
        return null;
    }

    private static RouteRegistry routeRegistry() {
        return (RouteRegistry) Proxy.newProxyInstance(
                MockInstantiatorDelegationTest.class.getClassLoader(),
                new Class<?>[] { RouteRegistry.class },
                (proxy, method, args) -> null);
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
                    return returnValueFor(method.getReturnType());
                });
    }
}
