/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.mocks;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.browserless.internal.Utils;
import com.vaadin.flow.component.geolocation.BrowserlessGeolocationClientFactory;
import com.vaadin.flow.component.geolocation.GeolocationClientFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.HandlesTypes;

public final class MockVaadinHelper {

    private MockVaadinHelper() {
    }

    private static volatile boolean flowBuildInfoInitialized = false;
    private static volatile ObjectNode flowBuildInfoValue = null;

    private static ObjectNode flowBuildInfo() {
        if (!flowBuildInfoInitialized) {
            synchronized (MockVaadinHelper.class) {
                if (!flowBuildInfoInitialized) {
                    flowBuildInfoValue = getTokenFileFromClassloader();
                    flowBuildInfoInitialized = true;
                }
            }
        }
        return flowBuildInfoValue;
    }

    public static void mockFlowBuildInfo(VaadinServlet servlet) {
        // we need to skip the test at DeploymentConfigurationFactory.verifyMode otherwise
        // testing a Vaadin 15 component module in npm mode without webpack.config.js nor flow-build-info.json would fail.
        if (flowBuildInfo() == null) {
            // probably inside a Vaadin 15 component module. create a dummy token file so that
            // DeploymentConfigurationFactory.verifyMode() is happy.
            try {
                File tokenFile = File.createTempFile("flow-build-info", "json");
                Files.write(tokenFile.toPath(), "{}".getBytes(StandardCharsets.UTF_8));
                servlet.getServletContext().setInitParameter("vaadin.frontend.token.file", tokenFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        servlet.getServletContext().setInitParameter("compatibilityMode", "false");
    }

    public static ServletContext createMockContext() {
        return createMockContext(Collections.emptySet());
    }

    public static ServletContext createMockContext(Set<Class<?>> lookupServices) {
        MockContext ctx = new MockContext();
        init(ctx, lookupServices);
        return ctx;
    }

    public static VaadinContext createMockVaadinContext() {
        return new VaadinServletContext(createMockContext());
    }

    public static ObjectNode getTokenFileFromClassloader() {

        // Use DefaultApplicationConfigurationFactory.getTokenFileFromClassloader() to make sure to read
        // the same flow-build-info.json that Vaadin reads.

        VaadinContext ctx = createMockVaadinContext();
        Object acf;
        Class<?> dacfClass;
        try {
            acf = lookup(ctx, Utils.findClassOrThrow("com.vaadin.flow.server.startup.ApplicationConfigurationFactory"));
            dacfClass = Utils.findClassOrThrow("com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (acf == null) {
            throw new IllegalStateException("ApplicationConfigurationFactory is null");
        }
        if (dacfClass.isInstance(acf)) {
            try {
                Method m = dacfClass.getDeclaredMethod("getTokenFileFromClassloader", VaadinContext.class);
                m.setAccessible(true);
                String json = (String) m.invoke(acf, ctx);
                if (json == null) {
                    return null;
                }
                return (ObjectNode) new ObjectMapper().readTree(json);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * Calls `Lookup.lookup(Class)`.
     */
    public static Object lookup(VaadinContext ctx, Class<?> clazz) {
        Lookup lookup = verifyHasLookup(ctx);
        return lookup.lookup(clazz);
    }

    /**
     * Verifies that the ctx has an instance of `com.vaadin.flow.di.Lookup` set, and returns it.
     * @return the instance of `com.vaadin.flow.di.Lookup`.
     */
    private static Lookup verifyHasLookup(ServletContext ctx) {
        Object lookup = ctx.getAttribute("com.vaadin.flow.di.Lookup");
        if (lookup == null) {
            throw new IllegalStateException(
                    "The context doesn't contain the Vaadin 19 Lookup class. Available attributes: "
                            + Collections.list(ctx.getAttributeNames()));
        }
        return (Lookup) lookup;
    }

    private static Lookup verifyHasLookup(VaadinContext ctx) {
        return verifyHasLookup(((VaadinServletContext) ctx).getContext());
    }

    private static void init(ServletContext ctx, Set<Class<?>> lookupServices) {

        Set<Class<?>> loaders = new LinkedHashSet<>();
        loaders.addAll(lookupServices);
        loaders.add(LookupInitializer.class);
        try {
            loaders.add(Utils.findClassOrThrow("com.vaadin.flow.di.LookupInitializer$ResourceProviderImpl"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        tryLoad(loaders, "com.vaadin.flow.component.polymertemplate.rpc.PolymerPublishedEventRpcHandler");
        tryLoad(loaders, "com.vaadin.fusion.frontend.EndpointGeneratorTaskFactoryImpl");

        LookupServletContainerInitializer loaderInitializer = setupLookupInitializer(loaders);

        try {
            loaderInitializer.onStartup(loaders, ctx);
        } catch (jakarta.servlet.ServletException e) {
            throw new RuntimeException(e);
        }

        // verify that the Lookup has been set
        verifyHasLookup(ctx);
    }

    private static void tryLoad(Set<Class<?>> loaders, String className) {
        // sometimes customers don't include entire vaadin-core and exclude stuff like fusion on purpose.
        // load the class only if it exists.
        Class<?> clazz = Utils.findClass(className);
        if (clazz != null) {
            loaders.add(clazz);
        }
    }

    private static LookupServletContainerInitializer setupLookupInitializer(Set<Class<?>> services) {
        BrowserlessLookupInitializer initializer = new BrowserlessLookupInitializer();
        initializer.updateServices(services);
        return initializer;
    }

    public static class BrowserlessLookupInitializer extends LookupServletContainerInitializer {

        // Additional services wired through lookup that the testing environment can hook in,
        // supplementing ones defined in LookupServletContainerInitializer HandlesTypes annotations.
        //
        // Use Object class as a value placeholder for a service without default implementation,
        // but that can be hooked in by the test class in the services' set
        // Map.of(Service.class, Object.class)
        protected Map<Class<?>, Class<?>> additionalServices;

        public BrowserlessLookupInitializer() {
            Map<Class<?>, Class<?>> map = new LinkedHashMap<>();
            map.put(GeolocationClientFactory.class, BrowserlessGeolocationClientFactory.class);
            this.additionalServices = map;
        }

        public void updateServices(Set<Class<?>> services) {
            for (Map.Entry<Class<?>, Class<?>> entry : additionalServices.entrySet()) {
                // skip if the caller already supplied an implementation of the service interface
                boolean alreadySupplied = false;
                for (Class<?> s : services) {
                    if (entry.getKey().isAssignableFrom(s)) {
                        alreadySupplied = true;
                        break;
                    }
                }
                if (alreadySupplied) {
                    continue;
                }
                // ignore additional services without default implementation
                if (entry.getValue() == Object.class) {
                    continue;
                }
                services.add(entry.getValue());
            }
        }

        @Override
        protected Collection<Class<?>> getServiceTypes() {
            HandlesTypes annotation = LookupServletContainerInitializer.class.getAnnotation(HandlesTypes.class);
            if (annotation == null) {
                throw new IllegalStateException(
                        "Cannot collect service types based on "
                                + HandlesTypes.class.getSimpleName()
                                + " annotation. The default 'getServiceTypes' method implementation can't be used.");
            }
            Set<Class<?>> result = new LinkedHashSet<>();
            Collections.addAll(result, annotation.value());
            result.addAll(additionalServices.keySet());
            Set<Class<?>> filtered = new LinkedHashSet<>();
            for (Class<?> c : result) {
                if (c != LookupInitializer.class) {
                    filtered.add(c);
                }
            }
            return filtered;
        }
    }
}
