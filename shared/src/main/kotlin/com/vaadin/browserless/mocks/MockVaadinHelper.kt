/**
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.mocks

import com.vaadin.browserless.internal.findClass
import com.vaadin.browserless.internal.findClassOrThrow
import com.vaadin.flow.di.Lookup
import com.vaadin.flow.di.LookupInitializer
import com.vaadin.flow.server.VaadinContext
import com.vaadin.flow.server.VaadinServlet
import com.vaadin.flow.server.VaadinServletContext
import com.vaadin.flow.server.startup.LookupServletContainerInitializer
import jakarta.servlet.ServletContext
import jakarta.servlet.annotation.HandlesTypes
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import java.io.File
import kotlin.reflect.KClass

object MockVaadinHelper {

    private val flowBuildInfo: ObjectNode? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        getTokenFileFromClassloader()
    }

    @JvmStatic
    fun mockFlowBuildInfo(servlet: VaadinServlet) {
        // we need to skip the test at DeploymentConfigurationFactory.verifyMode otherwise
        // testing a Vaadin 15 component module in npm mode without webpack.config.js nor flow-build-info.json would fail.
        if (flowBuildInfo == null) {
            // probably inside a Vaadin 15 component module. create a dummy token file so that
            // DeploymentConfigurationFactory.verifyMode() is happy.
            val tokenFile: File = File.createTempFile("flow-build-info", "json")
            tokenFile.writeText("{}")
            servlet.servletContext.setInitParameter("vaadin.frontend.token.file", tokenFile.absolutePath)
        }

        servlet.servletContext.setInitParameter("compatibilityMode", "false")
    }

    fun createMockContext(lookupServices: Set<Class<*>> = emptySet()): ServletContext {
        val ctx = MockContext()
        init(ctx, lookupServices)
        return ctx
    }

    fun createMockVaadinContext(): VaadinContext =
            VaadinServletContext(createMockContext())

    fun getTokenFileFromClassloader(): ObjectNode? {

        // Use DefaultApplicationConfigurationFactory.getTokenFileFromClassloader() to make sure to read
        // the same flow-build-info.json that Vaadin reads.

        val ctx: VaadinContext = MockVaadinHelper.createMockVaadinContext()
        val acf = lookup(ctx, findClassOrThrow("com.vaadin.flow.server.startup.ApplicationConfigurationFactory"))
        checkNotNull(acf) { "ApplicationConfigurationFactory is null" }
        val dacfClass = findClassOrThrow("com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory")
        if (dacfClass.isInstance(acf)) {
            val m = dacfClass.getDeclaredMethod("getTokenFileFromClassloader", VaadinContext::class.java)
            m.isAccessible = true
            val json = m.invoke(acf, ctx) as String? ?: return null
            return ObjectMapper().readTree(json) as ObjectNode
        }
        return null
    }

    /**
     * Calls `Lookup.lookup(Class)`.
     */
    fun lookup(ctx: VaadinContext, clazz: Class<*>): Any? {
        val lookup = verifyHasLookup(ctx)
        return lookup.lookup(clazz)
    }

    /**
     * Verifies that the ctx has an instance of `com.vaadin.flow.di.Lookup` set, and returns it.
     * @return the instance of `com.vaadin.flow.di.Lookup`.
     */
    private fun verifyHasLookup(ctx: ServletContext): Lookup {
        val lookup: Any? = ctx.getAttribute("com.vaadin.flow.di.Lookup")
        checkNotNull(lookup) {
            "The context doesn't contain the Vaadin 19 Lookup class. Available attributes: " + ctx.attributeNames.toList()
        }
        return lookup as Lookup
    }
    private fun verifyHasLookup(ctx: VaadinContext): Lookup =
            verifyHasLookup((ctx as VaadinServletContext).context)

    private fun init(ctx: ServletContext, lookupServices: Set<Class<*>> = emptySet()) {

        val loaders = mutableSetOf(
                *lookupServices.toTypedArray(),
                LookupInitializer::class.java,
                findClassOrThrow("com.vaadin.flow.di.LookupInitializer${'$'}ResourceProviderImpl")
        )
        fun tryLoad(className: String) {
            // sometimes customers don't include entire vaadin-core and exclude stuff like fusion on purpose.
            // load the class only if it exists.
            findClass(className)?.let { clazz -> loaders.add(clazz) }
        }

        tryLoad("com.vaadin.flow.component.polymertemplate.rpc.PolymerPublishedEventRpcHandler")
        tryLoad("com.vaadin.fusion.frontend.EndpointGeneratorTaskFactoryImpl")

        val loaderInitializer = setupLookupInitializer(loaders)

        loaderInitializer.onStartup(loaders, ctx)

        // verify that the Lookup has been set
        verifyHasLookup(ctx)
    }

    private fun setupLookupInitializer(services: MutableSet<Class<*>>) : LookupServletContainerInitializer{
        val initializer = BrowserlessLookupInitializer()
        initializer.updateServices(services)
        return initializer
    }

    internal open class BrowserlessLookupInitializer() : LookupServletContainerInitializer() {

        // Additional services wired through lookup that the testing environment can hook in,
        // supplementing ones defined in LookupServletContainerInitializer HandlesTypes annotations.
        //
        // Example:
        // mapOf(GeolocationClientFactory::class to BrowserlessGeolocationClientFactory::class)
        //
        // Use Object class as a value placeholder for a service without default implementation,
        // but that can be hooked in by the test class in the services' set
        // mapOf(Service::class to Object::class)
        protected open val additionalServices: Map<KClass<*>, KClass<*>> = emptyMap()

        fun updateServices(services: MutableSet<Class<*>>) {
            additionalServices
                // skip if the caller already supplied an implementation of the service interface
                .filterNot { entry -> services.any { entry.key.java.isAssignableFrom(it) } }
                // ignore additional services without default implementation
                .filter { it.value != Object::class }
                .forEach { services.add(it.value.java) }
        }

        override fun getServiceTypes(): Collection<Class<*>?> {
            val annotation = LookupServletContainerInitializer::class.java.getAnnotation(HandlesTypes::class.java)
            checkNotNull(annotation) {
                ("Cannot collect service types based on "
                        + HandlesTypes::class.java.getSimpleName()
                        + " annotation. The default 'getServiceTypes' method implementation can't be used.")
            }
            val allServices = annotation.value + additionalServices.keys
            return setOf(*allServices.map { it.java }.toTypedArray())
                .filter { clazz: Class<*>? -> clazz != LookupInitializer::class.java }
                .toSet()

        }
    }
}
