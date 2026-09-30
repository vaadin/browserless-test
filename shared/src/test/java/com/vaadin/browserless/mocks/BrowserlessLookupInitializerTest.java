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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.LookupInitializer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserlessLookupInitializerTest {

    @Test
    void updateServices_addsDefaultImplWhenInterfaceIsAbsent() {
        TestInitializer initializer = new TestInitializer(
                Map.of(FakeService.class, FakeServiceImpl.class));
        Set<Class<?>> services = new LinkedHashSet<>();

        initializer.updateServices(services);

        assertTrue(services.contains(FakeServiceImpl.class));
    }

    @Test
    void updateServices_skipsEntryWhenCallerAlreadySuppliedAnImplementation() {
        TestInitializer initializer = new TestInitializer(
                Map.of(FakeService.class, FakeServiceImpl.class));
        Set<Class<?>> services = new LinkedHashSet<>(
                Set.of(OtherFakeServiceImpl.class));

        initializer.updateServices(services);

        assertTrue(services.contains(OtherFakeServiceImpl.class));
        assertFalse(services.contains(FakeServiceImpl.class));
    }

    @Test
    void updateServices_skipsEntriesWhoseDefaultIsTheObjectPlaceholder() {
        TestInitializer initializer = new TestInitializer(
                Map.of(NoDefaultService.class, Object.class));
        Set<Class<?>> services = new LinkedHashSet<>();

        initializer.updateServices(services);

        assertTrue(services.isEmpty());
    }

    @Test
    void getServiceTypes_extendsParentServiceTypesWithAdditionalServicesKeys() {
        TestInitializer initializer = new TestInitializer(
                Map.of(FakeService.class, FakeServiceImpl.class));

        Collection<Class<?>> types = initializer.getServiceTypes();

        assertTrue(types.contains(FakeService.class));
        assertTrue(types.contains(InstantiatorFactory.class));
    }

    @Test
    void getServiceTypes_excludesLookupInitializerEvenWhenAddedAsAnAdditionalService() {
        TestInitializer initializer = new TestInitializer(
                Map.of(LookupInitializer.class, FakeServiceImpl.class));

        Collection<Class<?>> types = initializer.getServiceTypes();

        assertFalse(types.contains(LookupInitializer.class));
    }

    private interface FakeService {
    }

    private static class FakeServiceImpl implements FakeService {
    }

    private static class OtherFakeServiceImpl implements FakeService {
    }

    private interface NoDefaultService {
    }

    private static class TestInitializer
            extends MockVaadinHelper.BrowserlessLookupInitializer {

        TestInitializer(Map<Class<?>, Class<?>> services) {
            this.additionalServices = services;
        }

        @Override
        public Collection<Class<?>> getServiceTypes() {
            return super.getServiceTypes();
        }
    }
}
