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

import jakarta.servlet.ServletContext;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.server.startup.RouteRegistryInitializer;

/**
 * The routes, error routes and layouts the mocked environment registers.
 * <p>
 * A deployment discovers these by classpath scanning at startup; here the set
 * is built explicitly, either by hand or with
 * {@link #autoDiscoverViews(String...)}. Vaadin ignores any route not present,
 * so a test navigating to a view that was never registered gets a
 * {@code not found} rather than the view.
 * <p>
 * Scanning is the slow part of setting a test up, so build one instance and
 * reuse it across setups.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class Routes implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Routes.class);

    private final Set<Class<? extends Component>> routes;

    private final Set<Class<? extends HasErrorParameter<?>>> errorRoutes;

    private final Set<Class<? extends RouterLayout>> layouts;

    private boolean skipPwaInit;

    public Routes() {
        this(new LinkedHashSet<>(), defaultErrorRoutes(), new LinkedHashSet<>(),
                true);
    }

    public Routes(Set<Class<? extends Component>> routes,
            Set<Class<? extends HasErrorParameter<?>>> errorRoutes) {
        this(routes, errorRoutes, new LinkedHashSet<>(), true);
    }

    public Routes(Set<Class<? extends Component>> routes,
            Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
            Set<Class<? extends RouterLayout>> layouts) {
        this(routes, errorRoutes, layouts, true);
    }

    public Routes(Set<Class<? extends Component>> routes,
            Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
            Set<Class<? extends RouterLayout>> layouts, boolean skipPwaInit) {
        this.routes = routes;
        this.errorRoutes = errorRoutes;
        this.layouts = layouts;
        this.skipPwaInit = skipPwaInit;
    }

    /**
     * Returns the route views to register, live and directly modifiable. Vaadin
     * ignores any route not in this set.
     *
     * @return the route views
     */
    public Set<Class<? extends Component>> getRoutes() {
        return routes;
    }

    /**
     * Returns the error views to register, live and directly modifiable.
     *
     * @return the error views
     */
    public Set<Class<? extends HasErrorParameter<?>>> getErrorRoutes() {
        return errorRoutes;
    }

    /**
     * Returns the {@link Layout}-annotated {@link RouterLayout} classes to
     * register, live and directly modifiable. These are applied automatically
     * to routes that do not name a layout in their {@link Route} annotation.
     *
     * @return the layouts
     */
    public Set<Class<? extends RouterLayout>> getLayouts() {
        return layouts;
    }

    /**
     * Tells whether PWA initialization is skipped.
     *
     * @return {@code true} if PWA initialization is skipped
     */
    public boolean getSkipPwaInit() {
        return skipPwaInit;
    }

    /**
     * Skips Vaadin's PWA initialization, which takes the environment setup from
     * roughly two seconds down to fifty milliseconds. Since that is almost
     * always what a test wants, it is skipped by default.
     *
     * @param skipPwaInit
     *            {@code false} to run PWA initialization
     */
    public void setSkipPwaInit(boolean skipPwaInit) {
        this.skipPwaInit = skipPwaInit;
    }

    private static Set<Class<? extends HasErrorParameter<?>>> defaultErrorRoutes() {
        Set<Class<? extends HasErrorParameter<?>>> s = new LinkedHashSet<>();
        s.add(MockRouteNotFoundError.class);
        return s;
    }

    /**
     * Registers all routes to Vaadin 15 registry. Automatically called from
     * {@link MockVaadin#setup}.
     */
    @SuppressWarnings("unchecked")
    public void register(VaadinContext sc) {
        Set<Class<?>> classSet = new LinkedHashSet<>();
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
            throw new IllegalStateException(
                    "RouteRegistryInitializer did not register the ApplicationRouteRegistry!");
        }
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(sc);
        Set<Class<? extends Component>> errorNavTargets = errorRoutes.stream()
                .map(c -> (Class<? extends Component>) c)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        registry.setErrorNavigationTargets(errorNavTargets);
        if (skipPwaInit) {
            clearPwaClass(registry);
        }
    }

    /**
     * Auto-discovers everything, registers it into `this` and returns `this`. *
     * {@link Route}-annotated views go into {@code routes} *
     * {@link HasErrorParameter} error views go into {@code errorRoutes} After
     * this function finishes, you can still modify the {@code routes} and
     * {@code errorRoutes} sets, for example you can clear the
     * {@code errorRoutes} if there is some kind of misdetection.
     * 
     * @param packageNames
     *            set the package name for the detector to be faster; or provide
     *            null to scan the whole classpath, but this is quite slow.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public Routes autoDiscoverViews(String... packageNames) {
        String[] effectivePackages = new String[packageNames.length];
        for (int i = 0; i < packageNames.length; i++) {
            effectivePackages[i] = packageNames[i] != null ? packageNames[i]
                    : "";
        }
        ClassGraph classGraph = new ClassGraph().enableClassInfo()
                .enableAnnotationInfo().ignoreClassVisibility()
                .acceptPackages(effectivePackages);
        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo info : scanResult
                    .getClassesWithAnnotation(Route.class.getName())) {
                routes.add(scanResult.loadClass(info.getName(), Component.class,
                        false));
            }
            for (ClassInfo info : scanResult.getClassesImplementing(
                    HasErrorParameter.class.getName())) {
                @SuppressWarnings("rawtypes")
                Class<? extends HasErrorParameter> raw = scanResult.loadClass(
                        info.getName(), HasErrorParameter.class, false);
                errorRoutes.add((Class<? extends HasErrorParameter<?>>) raw);
            }
            for (ClassInfo info : scanResult
                    .getClassesWithAnnotation(Layout.class.getName())) {
                if (info.implementsInterface(RouterLayout.class.getName())) {
                    layouts.add(scanResult.loadClass(info.getName(),
                            RouterLayout.class, false));
                }
            }
        }

        cleanupErrorRoutes();

        LOG.debug("Auto-discovered views: {}", this);
        return this;
    }

    public Routes merge(Routes other) {
        Routes result = new Routes(new LinkedHashSet<>(this.routes),
                new LinkedHashSet<>(this.errorRoutes),
                new LinkedHashSet<>(this.layouts), this.skipPwaInit);
        result.routes.addAll(other.routes);
        result.errorRoutes.addAll(other.errorRoutes);
        result.layouts.addAll(other.layouts);
        result.cleanupErrorRoutes();
        return result;
    }

    private void cleanupErrorRoutes() {
        // https://github.com/mvysny/karibu-testing/issues/50
        // if the app defines its own NotFoundException handler, remove
        // MockRouteNotFoundError
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
     * Creates a copy of this Routes with optional field overrides. Mirrors the
     * Kotlin data-class `copy(...)` method.
     */
    public Routes copy(Set<Class<? extends Component>> routes,
            Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
            Set<Class<? extends RouterLayout>> layouts, boolean skipPwaInit) {
        return new Routes(routes, errorRoutes, layouts, skipPwaInit);
    }

    @Override
    public String toString() {
        return "Routes(routes=" + joinSimpleNames(routes) + ", errorRoutes="
                + joinSimpleNames(errorRoutes) + ", layouts="
                + joinSimpleNames(layouts) + ")";
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
            Field pwaClassField = ApplicationRouteRegistry.class
                    .getDeclaredField("pwaConfigurationClass");
            pwaClassField.setAccessible(true);
            AtomicReference<Class<?>> ref = (AtomicReference<Class<?>>) pwaClassField
                    .get(registry);
            ref.set(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        if (registry.getPwaConfigurationClass() != null) {
            throw new AssertionError(
                    "PWA configuration class should have been removed");
        }
    }
}
