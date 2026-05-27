/*
 * Copyright (C) 2020-2026 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.browserless.mocks

import com.github.mvysny.dynatest.DynaTest
import com.vaadin.flow.di.InstantiatorFactory
import com.vaadin.flow.di.LookupInitializer
import kotlin.test.expect

class BrowserlessLookupInitializerTest : DynaTest({

    test("updateServices adds default impl when interface is absent") {
        val initializer = TestInitializer(
                mapOf(FakeService::class.java to FakeServiceImpl::class.java))
        val services = mutableSetOf<Class<*>>()

        initializer.updateServices(services)

        expect(true) { services.contains(FakeServiceImpl::class.java) }
    }

    test("updateServices skips entry when caller already supplied an implementation") {
        val initializer = TestInitializer(
                mapOf(FakeService::class.java to FakeServiceImpl::class.java))
        val services = mutableSetOf<Class<*>>(OtherFakeServiceImpl::class.java)

        initializer.updateServices(services)

        expect(true) { services.contains(OtherFakeServiceImpl::class.java) }
        expect(false) { services.contains(FakeServiceImpl::class.java) }
    }

    test("updateServices skips entries whose default is the Object placeholder") {
        val initializer = TestInitializer(
                mapOf(NoDefaultService::class.java to Object::class.java))
        val services = mutableSetOf<Class<*>>()

        initializer.updateServices(services)

        expect(true) { services.isEmpty() }
    }

    test("getServiceTypes extends parent service types with additionalServices keys") {
        val initializer = TestInitializer(
                mapOf(FakeService::class.java to FakeServiceImpl::class.java))

        val types = initializer.serviceTypes

        expect(true) { types.contains(FakeService::class.java) }
        expect(true) { types.contains(InstantiatorFactory::class.java) }
    }

    test("getServiceTypes excludes LookupInitializer even when added as an additional service") {
        val initializer = TestInitializer(
                mapOf(LookupInitializer::class.java to FakeServiceImpl::class.java))

        val types = initializer.serviceTypes

        expect(false) { types.contains(LookupInitializer::class.java) }
    }
})

private interface FakeService
private class FakeServiceImpl : FakeService
private class OtherFakeServiceImpl : FakeService
private interface NoDefaultService

private class TestInitializer(
        services: Map<Class<*>, Class<*>>
) : MockVaadinHelper.BrowserlessLookupInitializer() {
    init {
        this.additionalServices = services
    }
    public override fun getServiceTypes(): Collection<Class<*>?> = super.getServiceTypes()
}
