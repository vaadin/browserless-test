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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.base.WelcomeView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that lookup services can be declared with
 * {@link BrowserlessTestConfig}, and that they accumulate with the ones
 * provided by the deprecated {@link BaseBrowserlessTest#lookupServices()} hook.
 */
@ViewPackages(classes = WelcomeView.class)
class BrowserlessTestConfigLookupServicesTest {

    @Nested
    @BrowserlessTestConfig(lookupServices = TestCustomInstantiatorFactory.class)
    class DeclaredOnClass extends BrowserlessTest {

        @Test
        void annotatedService_isAvailableInLookup() {
            Assertions.assertInstanceOf(TestCustomInstantiatorFactory.class,
                    lookup().lookup(InstantiatorFactory.class));
        }

        @Test
        @BrowserlessTestConfig(lookupServices = TestNoOpInstantiatorFactory.class)
        void methodServices_accumulateWithClassOnes() {
            assertRegistered(TestCustomInstantiatorFactory.class,
                    TestNoOpInstantiatorFactory.class);
        }
    }

    @Nested
    @BrowserlessTestConfig(lookupServices = TestNoOpInstantiatorFactory.class)
    class DeclaredOnBothClassAndDeprecatedHook extends BrowserlessTest {

        @SuppressWarnings("deprecation")
        @Override
        protected Set<Class<?>> lookupServices() {
            return Set.of(TestCustomInstantiatorFactory.class);
        }

        @Test
        void deprecatedHookAndAnnotation_areBothHonored() {
            assertRegistered(TestCustomInstantiatorFactory.class,
                    TestNoOpInstantiatorFactory.class);
        }
    }

    private static void assertRegistered(Class<?>... expectedServices) {
        Collection<InstantiatorFactory> factories = lookup()
                .lookupAll(InstantiatorFactory.class);
        Set<Class<?>> registered = factories.stream().map(Object::getClass)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Class<?> expected : expectedServices) {
            Assertions.assertTrue(registered.contains(expected),
                    "Expecting " + expected.getSimpleName()
                            + " to be registered in Lookup, but found "
                            + registered);
        }
    }

    private static Lookup lookup() {
        Lookup lookup = VaadinService.getCurrent().getContext()
                .getAttribute(Lookup.class);
        Assertions.assertNotNull(lookup, "Expecting Lookup to be initialized");
        return lookup;
    }
}
