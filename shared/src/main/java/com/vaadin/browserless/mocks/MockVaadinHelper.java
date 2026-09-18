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

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.HandlesTypes;

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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.internal.UtilsKt;
import com.vaadin.flow.component.geolocation.BrowserlessGeolocationClientFactory;
import com.vaadin.flow.component.geolocation.GeolocationClientFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

/**
 * Builds the {@link ServletContext} the mocked environment runs in.
 * <p>
 * A deployed application gets its context from the servlet container, which
 * runs Vaadin's {@code ServletContainerInitializer}s to install the
 * {@link Lookup}. There is no container here, so
 * {@link #createMockContext(Set)} creates a {@link MockContext} and runs the
 * lookup initializer over it by hand, seeding it with Vaadin's own services
 * plus whatever the test asked for. Everything downstream — the instantiator,
 * the application configuration, the route registry — is then resolved through
 * that {@link Lookup}, exactly as in a real deployment.
 * <p>
 * It also papers over the one thing a component module cannot provide:
 * {@link #mockFlowBuildInfo(VaadinServlet)} writes a dummy token file when
 * there is no {@code flow-build-info.json} on the classpath, which Vaadin's
 * mode detection would otherwise reject.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
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

    /**
     * Makes sure the servlet can read a {@code flow-build-info.json}, writing a
     * dummy one when the classpath has none.
     *
     * <p>
     * Without it {@code DeploymentConfigurationFactory.verifyMode()} rejects
     * the environment, which is what happens when the tests run inside a Vaadin
     * component module rather than an application.
     *
     * @param servlet
     *            the servlet whose context to configure
     */
    public static void mockFlowBuildInfo(VaadinServlet servlet) {
        // we need to skip the test at DeploymentConfigurationFactory.verifyMode
        // otherwise
        // testing a Vaadin 15 component module in npm mode without
        // webpack.config.js nor flow-build-info.json would fail.
        if (flowBuildInfo() == null) {
            // probably inside a Vaadin 15 component module. create a dummy
            // token file so that
            // DeploymentConfigurationFactory.verifyMode() is happy.
            try {
                File tokenFile = File.createTempFile("flow-build-info", "json");
                Files.write(tokenFile.toPath(),
                        "{}".getBytes(StandardCharsets.UTF_8));
                servlet.getServletContext().setInitParameter(
                        "vaadin.frontend.token.file",
                        tokenFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        servlet.getServletContext().setInitParameter("compatibilityMode",
                "false");
    }

    /**
     * Creates a context holding a {@link Lookup} with Vaadin's own services.
     *
     * @return the new context
     */
    public static ServletContext createMockContext() {
        return createMockContext(Collections.emptySet());
    }

    /**
     * Creates a context holding a {@link Lookup} with Vaadin's own services
     * plus the given ones.
     *
     * @param lookupServices
     *            extra service implementations to register; one supplied here
     *            wins over the default for the same service interface
     * @return the new context
     */
    public static ServletContext createMockContext(
            Set<Class<?>> lookupServices) {
        MockContext ctx = new MockContext();
        init(ctx, lookupServices);
        return ctx;
    }

    /**
     * Creates a {@link VaadinContext} wrapping a fresh
     * {@link #createMockContext()}.
     *
     * @return the new context
     */
    public static VaadinContext createMockVaadinContext() {
        return new VaadinServletContext(createMockContext());
    }

    /**
     * Reads {@code flow-build-info.json} from the classpath the same way Vaadin
     * does, through {@code DefaultApplicationConfigurationFactory}.
     *
     * @return the parsed token file, or {@code null} if the classpath has none
     */
    public static ObjectNode getTokenFileFromClassloader() {

        // Use
        // DefaultApplicationConfigurationFactory.getTokenFileFromClassloader()
        // to make sure to read
        // the same flow-build-info.json that Vaadin reads.

        VaadinContext ctx = createMockVaadinContext();
        Object acf;
        Class<?> dacfClass;
        try {
            acf = lookup(ctx, UtilsKt.findClassOrThrow(
                    "com.vaadin.flow.server.startup.ApplicationConfigurationFactory"));
            dacfClass = UtilsKt.findClassOrThrow(
                    "com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (acf == null) {
            throw new IllegalStateException(
                    "ApplicationConfigurationFactory is null");
        }
        if (dacfClass.isInstance(acf)) {
            try {
                Method m = dacfClass.getDeclaredMethod(
                        "getTokenFileFromClassloader", VaadinContext.class);
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
     * Looks a service up in the context's {@link Lookup}.
     *
     * @param ctx
     *            the context to look in; it must already hold a {@link Lookup}
     * @param clazz
     *            the service interface to resolve
     * @return the service, or {@code null} if none is registered
     * @throws IllegalStateException
     *             if the context holds no {@link Lookup}
     */
    public static Object lookup(VaadinContext ctx, Class<?> clazz) {
        Lookup lookup = verifyHasLookup(ctx);
        return lookup.lookup(clazz);
    }

    /**
     * Verifies that the ctx has an instance of `com.vaadin.flow.di.Lookup` set,
     * and returns it.
     * 
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
            loaders.add(UtilsKt.findClassOrThrow(
                    "com.vaadin.flow.di.LookupInitializer$ResourceProviderImpl"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        tryLoad(loaders,
                "com.vaadin.flow.component.polymertemplate.rpc.PolymerPublishedEventRpcHandler");
        tryLoad(loaders,
                "com.vaadin.fusion.frontend.EndpointGeneratorTaskFactoryImpl");

        LookupServletContainerInitializer loaderInitializer = setupLookupInitializer(
                loaders);

        try {
            loaderInitializer.onStartup(loaders, ctx);
        } catch (jakarta.servlet.ServletException e) {
            throw new RuntimeException(e);
        }

        // verify that the Lookup has been set
        verifyHasLookup(ctx);
    }

    private static void tryLoad(Set<Class<?>> loaders, String className) {
        // sometimes customers don't include entire vaadin-core and exclude
        // stuff like fusion on purpose.
        // load the class only if it exists.
        Class<?> clazz = UtilsKt.findClass(className);
        if (clazz != null) {
            loaders.add(clazz);
        }
    }

    private static LookupServletContainerInitializer setupLookupInitializer(
            Set<Class<?>> services) {
        BrowserlessLookupInitializer initializer = new BrowserlessLookupInitializer();
        initializer.updateServices(services);
        return initializer;
    }

    /**
     * The lookup initializer of the mocked environment.
     * <p>
     * It extends Vaadin's own with the services a browserless test needs on top
     * of the ones {@code LookupServletContainerInitializer} declares in its
     * {@link HandlesTypes} annotation — the browserless geolocation client, for
     * one. Subclass it and replace {@link #additionalServices} to add more.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     */
    public static class BrowserlessLookupInitializer
            extends LookupServletContainerInitializer {

        /**
         * Services the testing environment wires through the {@link Lookup},
         * mapping a service interface to its default implementation.
         * <p>
         * An entry is skipped when the caller already supplied an
         * implementation of that interface. Map an interface to {@link Object}
         * to declare a service that has no default but can be hooked in by a
         * test, e.g. {@code Map.of(Service.class, Object.class)}.
         */
        protected Map<Class<?>, Class<?>> additionalServices;

        /**
         * Creates an initializer registering the browserless environment's own
         * services.
         */
        public BrowserlessLookupInitializer() {
            Map<Class<?>, Class<?>> map = new LinkedHashMap<>();
            map.put(GeolocationClientFactory.class,
                    BrowserlessGeolocationClientFactory.class);
            this.additionalServices = map;
        }

        /**
         * Adds the default implementation of every {@link #additionalServices}
         * entry the caller has not already covered.
         *
         * @param services
         *            the service implementations to register, modified in place
         */
        public void updateServices(Set<Class<?>> services) {
            for (Map.Entry<Class<?>, Class<?>> entry : additionalServices
                    .entrySet()) {
                // skip if the caller already supplied an implementation of the
                // service interface
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
            HandlesTypes annotation = LookupServletContainerInitializer.class
                    .getAnnotation(HandlesTypes.class);
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
