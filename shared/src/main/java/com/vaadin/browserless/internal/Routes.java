/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.server.startup.RouteRegistryInitializer;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration object of all routes and error routes in the application. Simply use [autoDiscoverViews] to discover everything.
 *
 * To speed up the tests, you can create one instance of this class only, then reuse that instance in every
 * call to [MockVaadin.setup].
 * @property routes a list of all route views in your application. Vaadin will ignore any routes not present here.
 * @property errorRoutes a list of all route views in your application. Vaadin will ignore any routes not present here.
 * @property layouts a list of all [Layout]-annotated [RouterLayout] classes. These are automatically applied to routes
 * that do not explicitly specify a layout in their [Route] annotation.
 * @property skipPwaInit if true, the PWA initialization code is skipped in Vaadin, which dramatically speeds up
 * the [MockVaadin.setup] from 2 seconds to 50ms. Since that's usually what you want to do, this defaults to true.
 */
public class Routes implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Routes.class);

    public final Set<Class<? extends Component>> routes;
    public final Set<Class<? extends HasErrorParameter<?>>> errorRoutes;
    public final Set<Class<? extends RouterLayout>> layouts;
    public boolean skipPwaInit;

    public Routes() {
        this(new HashSet<>(),
                defaultErrorRoutes(),
                new HashSet<>(),
                true);
    }

    public Routes(Set<Class<? extends Component>> routes,
                  Set<Class<? extends HasErrorParameter<?>>> errorRoutes) {
        this(routes, errorRoutes, new HashSet<>(), true);
    }

    public Routes(Set<Class<? extends Component>> routes,
                  Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
                  Set<Class<? extends RouterLayout>> layouts) {
        this(routes, errorRoutes, layouts, true);
    }

    public Routes(Set<Class<? extends Component>> routes,
                  Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
                  Set<Class<? extends RouterLayout>> layouts,
                  boolean skipPwaInit) {
        this.routes = routes;
        this.errorRoutes = errorRoutes;
        this.layouts = layouts;
        this.skipPwaInit = skipPwaInit;
    }

    private static Set<Class<? extends HasErrorParameter<?>>> defaultErrorRoutes() {
        Set<Class<? extends HasErrorParameter<?>>> s = new HashSet<>();
        s.add(MockRouteNotFoundError.class);
        return s;
    }

    /**
     * Registers all routes to Vaadin 15 registry. Automatically called from [MockVaadin.setup].
     */
    @SuppressWarnings("unchecked")
    public void register(VaadinContext sc) {
        Set<Class<?>> classSet = new HashSet<>();
        classSet.addAll(routes);
        classSet.addAll(layouts);
        ServletContext servletContext = Utils.getContext(sc);
        try {
            new RouteRegistryInitializer().onStartup(classSet, servletContext);
        } catch (jakarta.servlet.ServletException e) {
            throw new RuntimeException(e);
        }
        Object attr = servletContext.getAttribute(
                "com.vaadin.flow.server.startup.ApplicationRouteRegistry$ApplicationRouteRegistryWrapper");
        if (attr == null) {
            throw new IllegalStateException("RouteRegistryInitializer did not register the ApplicationRouteRegistry!");
        }
        ApplicationRouteRegistry registry = ApplicationRouteRegistry.getInstance(sc);
        Set<Class<? extends Component>> errorNavTargets = errorRoutes.stream()
                .map(c -> (Class<? extends Component>) c)
                .collect(Collectors.toSet());
        registry.setErrorNavigationTargets(errorNavTargets);
        if (skipPwaInit) {
            clearPwaClass(registry);
        }
    }

    /**
     * Auto-discovers everything, registers it into `this` and returns `this`.
     * * [Route]-annotated views go into [routes]
     * * [HasErrorParameter] error views go into [errorRoutes]
     * After this function finishes, you can still modify the [routes] and [errorRoutes] sets,
     * for example you can clear the [errorRoutes] if there is some kind of misdetection.
     * @param packageNames set the package name for the detector to be faster; or provide null to scan the whole classpath, but this is quite slow.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public Routes autoDiscoverViews(String... packageNames) {
        String[] effectivePackages = new String[packageNames.length];
        for (int i = 0; i < packageNames.length; i++) {
            effectivePackages[i] = packageNames[i] != null ? packageNames[i] : "";
        }
        ClassGraph classGraph = new ClassGraph().enableClassInfo()
                .enableAnnotationInfo()
                .ignoreClassVisibility()
                .acceptPackages(effectivePackages);
        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo info : scanResult.getClassesWithAnnotation(Route.class.getName())) {
                routes.add(scanResult.loadClass(info.getName(), Component.class, false));
            }
            for (ClassInfo info : scanResult.getClassesImplementing(HasErrorParameter.class.getName())) {
                @SuppressWarnings("rawtypes")
                Class<? extends HasErrorParameter> raw =
                        scanResult.loadClass(info.getName(), HasErrorParameter.class, false);
                errorRoutes.add((Class<? extends HasErrorParameter<?>>) raw);
            }
            for (ClassInfo info : scanResult.getClassesWithAnnotation(Layout.class.getName())) {
                if (info.implementsInterface(RouterLayout.class.getName())) {
                    layouts.add(scanResult.loadClass(info.getName(), RouterLayout.class, false));
                }
            }
        }

        cleanupErrorRoutes();

        LOG.debug("Auto-discovered views: {}", this);
        return this;
    }

    public Routes merge(Routes other) {
        Routes result = new Routes(
                new LinkedHashSet<>(this.routes),
                new LinkedHashSet<>(this.errorRoutes),
                new LinkedHashSet<>(this.layouts),
                this.skipPwaInit);
        result.routes.addAll(other.routes);
        result.errorRoutes.addAll(other.errorRoutes);
        result.layouts.addAll(other.layouts);
        result.cleanupErrorRoutes();
        return result;
    }

    private void cleanupErrorRoutes() {
        // https://github.com/mvysny/karibu-testing/issues/50
        // if the app defines its own NotFoundException handler, remove MockRouteNotFoundError
        boolean hasCustomNotFoundHandler = false;
        for (Class<? extends HasErrorParameter<?>> c : errorRoutes) {
            if (c != MockRouteNotFoundError.class && Utils.isRouteNotFound(c)) {
                hasCustomNotFoundHandler = true;
                break;
            }
        }
        if (hasCustomNotFoundHandler) {
            errorRoutes.remove(MockRouteNotFoundError.class);
        }

        // Replace default InternalServeError exception handler with an
        // implementation that exposes error details for PrettyPrinter
        errorRoutes.remove(InternalServerError.class);
        errorRoutes.add(MockInternalSeverError.class);
    }

    /**
     * Creates a copy of this Routes with optional field overrides. Mirrors the Kotlin data-class `copy(...)` method.
     */
    public Routes copy(Set<Class<? extends Component>> routes,
                       Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
                       Set<Class<? extends RouterLayout>> layouts,
                       boolean skipPwaInit) {
        return new Routes(routes, errorRoutes, layouts, skipPwaInit);
    }

    @Override
    public String toString() {
        return "Routes(routes=" + joinSimpleNames(routes)
                + ", errorRoutes=" + joinSimpleNames(errorRoutes)
                + ", layouts=" + joinSimpleNames(layouts) + ")";
    }

    private static String joinSimpleNames(Set<? extends Class<?>> classes) {
        StringBuilder sb = new StringBuilder();
        Iterator<? extends Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getSimpleName());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Routes)) {
            return false;
        }
        Routes other = (Routes) o;
        return skipPwaInit == other.skipPwaInit
                && Objects.equals(routes, other.routes)
                && Objects.equals(errorRoutes, other.errorRoutes)
                && Objects.equals(layouts, other.layouts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routes, errorRoutes, layouts, skipPwaInit);
    }

    /**
     * Clears the PWA class config from this registry.
     */
    @SuppressWarnings("unchecked")
    public static void clearPwaClass(ApplicationRouteRegistry registry) {
        try {
            Field pwaClassField = ApplicationRouteRegistry.class.getDeclaredField("pwaConfigurationClass");
            pwaClassField.setAccessible(true);
            AtomicReference<Class<?>> ref = (AtomicReference<Class<?>>) pwaClassField.get(registry);
            ref.set(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        if (registry.getPwaConfigurationClass() != null) {
            throw new AssertionError("PWA configuration class should have been removed");
        }
    }
}
