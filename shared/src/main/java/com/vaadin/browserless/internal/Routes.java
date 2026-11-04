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

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
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
 * A configuration object of all routes and error routes in the application.
 * Simply use {@link #autoDiscoverViews(String...)} to discover everything.
 * <p>
 * To speed up the tests, you can create one instance of this class only, then
 * reuse that instance in every call to {@link MockVaadin#setup}.
 * <ul>
 * <li>{@link #getRoutes() routes}: a list of all route views in your
 * application. Vaadin will ignore any routes not present here.</li>
 * <li>{@link #getErrorRoutes() errorRoutes}: a list of all error views in your
 * application. Vaadin will ignore any error views not present here.</li>
 * <li>{@link #getLayouts() layouts}: a list of all {@link Layout}-annotated
 * {@link RouterLayout} classes. These are automatically applied to routes that
 * do not explicitly specify a layout in their {@link Route} annotation.</li>
 * <li>{@link #getSkipPwaInit() skipPwaInit}: if true, the PWA initialization
 * code is skipped in Vaadin, which dramatically speeds up the
 * {@link MockVaadin#setup} from 2 seconds to 50ms. Since that's usually what
 * you want to do, this defaults to true.</li>
 * </ul>
 */
public final class Routes implements Serializable {

    private static final long serialVersionUID = 6806760595260168641L;

    /**
     * Kept for binary compatibility.
     */
    public static final Companion Companion = new Companion();

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    private final Set<Class<? extends Component>> routes;
    private final Set<Class<? extends HasErrorParameter<?>>> errorRoutes;
    private final Set<Class<? extends RouterLayout>> layouts;
    private boolean skipPwaInit;

    /**
     * Creates the configuration.
     *
     * @param routes
     *            the route views, mutable.
     * @param errorRoutes
     *            the error views, mutable.
     * @param layouts
     *            the {@link Layout}-annotated router layouts, mutable.
     * @param skipPwaInit
     *            whether to skip the PWA initialization.
     */
    public Routes(Set<Class<? extends Component>> routes,
            Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
            Set<Class<? extends RouterLayout>> layouts, boolean skipPwaInit) {
        this.routes = Objects.requireNonNull(routes, "routes");
        this.errorRoutes = Objects.requireNonNull(errorRoutes, "errorRoutes");
        this.layouts = Objects.requireNonNull(layouts, "layouts");
        this.skipPwaInit = skipPwaInit;
    }

    /**
     * Creates an empty configuration, with only {@link MockRouteNotFoundError}
     * registered as an error route, and with the PWA initialization skipped.
     */
    public Routes() {
        this(new LinkedHashSet<>(),
                new LinkedHashSet<>(Set.of(MockRouteNotFoundError.class)),
                new LinkedHashSet<>(), true);
    }

    /**
     * Returns all route views in the application.
     *
     * @return the mutable set of route views.
     */
    public Set<Class<? extends Component>> getRoutes() {
        return routes;
    }

    /**
     * Returns all error views in the application.
     *
     * @return the mutable set of error views.
     */
    public Set<Class<? extends HasErrorParameter<?>>> getErrorRoutes() {
        return errorRoutes;
    }

    /**
     * Returns all {@link Layout}-annotated {@link RouterLayout} classes.
     *
     * @return the mutable set of layouts.
     */
    public Set<Class<? extends RouterLayout>> getLayouts() {
        return layouts;
    }

    /**
     * Whether the PWA initialization code is skipped in Vaadin.
     *
     * @return true if the PWA initialization is skipped.
     */
    public boolean getSkipPwaInit() {
        return skipPwaInit;
    }

    /**
     * Sets whether the PWA initialization code is skipped in Vaadin.
     *
     * @param skipPwaInit
     *            true to skip the PWA initialization.
     */
    public void setSkipPwaInit(boolean skipPwaInit) {
        this.skipPwaInit = skipPwaInit;
    }

    /**
     * Registers all routes to the Vaadin registry. Automatically called from
     * {@link MockVaadin#setup}.
     *
     * @param sc
     *            the Vaadin context.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void register(VaadinContext sc) {
        Set<Class<?>> classSet = new LinkedHashSet<>(routes);
        classSet.addAll(layouts);
        try {
            new RouteRegistryInitializer().onStartup(classSet,
                    UtilsKt.getContext(sc));
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
        if (UtilsKt.getContext(sc).getAttribute(
                "com.vaadin.flow.server.startup.ApplicationRouteRegistry$ApplicationRouteRegistryWrapper") == null) {
            throw new IllegalStateException(
                    "RouteRegistryInitializer did not register the ApplicationRouteRegistry!");
        }
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(sc);
        registry.setErrorNavigationTargets(
                (Set) new LinkedHashSet<>(errorRoutes));
        if (skipPwaInit) {
            RoutesKt.clearPwaClass(registry);
        }
    }

    /**
     * Auto-discovers everything in the whole classpath, registers it into
     * {@code this} and returns {@code this}. This is quite slow; consider
     * {@link #autoDiscoverViews(String...)} with package names instead.
     *
     * @return this
     */
    public Routes autoDiscoverViews() {
        return autoDiscoverViews(new String[0]);
    }

    /**
     * Auto-discovers everything, registers it into {@code this} and returns
     * {@code this}.
     * <ul>
     * <li>{@link Route}-annotated views go into {@link #getRoutes()
     * routes}</li>
     * <li>{@link HasErrorParameter} error views go into
     * {@link #getErrorRoutes() errorRoutes}</li>
     * </ul>
     * After this function finishes, you can still modify the routes and
     * errorRoutes sets, for example you can clear the errorRoutes if there is
     * some kind of misdetection.
     *
     * @param packageNames
     *            set the package name for the detector to be faster; or provide
     *            null to scan the whole classpath, but this is quite slow.
     * @return this
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Routes autoDiscoverViews(String... packageNames) {
        String[] packages = new String[packageNames.length];
        for (int i = 0; i < packageNames.length; i++) {
            packages[i] = packageNames[i] == null ? "" : packageNames[i];
        }
        ClassGraph classGraph = new ClassGraph().enableClassInfo()
                .enableAnnotationInfo().ignoreClassVisibility()
                .acceptPackages(packages);
        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo info : scanResult
                    .getClassesWithAnnotation(Route.class.getName())) {
                routes.add(scanResult.loadClass(info.getName(), Component.class,
                        false));
            }
            for (ClassInfo info : scanResult.getClassesImplementing(
                    HasErrorParameter.class.getName())) {
                errorRoutes.add((Class) scanResult.loadClass(info.getName(),
                        HasErrorParameter.class, false));
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

        log.debug("Auto-discovered views: {}", this);
        return this;
    }

    /**
     * Creates a new configuration containing the routes, error routes and
     * layouts of both this and the other configuration.
     *
     * @param other
     *            the other configuration.
     * @return a new merged configuration; uses this configuration's
     *         {@link #getSkipPwaInit() skipPwaInit}.
     */
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

    @Override
    public String toString() {
        return "Routes(routes=" + simpleNames(routes) + ", errorRoutes="
                + simpleNames(errorRoutes) + ", layouts=" + simpleNames(layouts)
                + ")";
    }

    private static String simpleNames(Set<? extends Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
    }

    private void cleanupErrorRoutes() {
        // https://github.com/mvysny/karibu-testing/issues/50
        // if the app defines its own NotFoundException handler, remove
        // MockRouteNotFoundError
        if (errorRoutes.stream()
                .anyMatch(it -> it != MockRouteNotFoundError.class
                        && UtilsKt.isRouteNotFound(it))) {
            errorRoutes.remove(MockRouteNotFoundError.class);
        }

        // Replace default InternalServeError exception handler with an
        // implementation that exposes error details for PrettyPrinter
        errorRoutes.remove(InternalServerError.class);
        errorRoutes.add(MockInternalSeverError.class);
    }

    /**
     * Returns the {@link #getRoutes() routes}.
     *
     * @return the routes.
     */
    public Set<Class<? extends Component>> component1() {
        return routes;
    }

    /**
     * Returns the {@link #getErrorRoutes() errorRoutes}.
     *
     * @return the error routes.
     */
    public Set<Class<? extends HasErrorParameter<?>>> component2() {
        return errorRoutes;
    }

    /**
     * Returns the {@link #getLayouts() layouts}.
     *
     * @return the layouts.
     */
    public Set<Class<? extends RouterLayout>> component3() {
        return layouts;
    }

    /**
     * Returns the {@link #getSkipPwaInit() skipPwaInit} flag.
     *
     * @return the skipPwaInit flag.
     */
    public boolean component4() {
        return skipPwaInit;
    }

    /**
     * Creates a new configuration with the given values; the sets are not
     * copied.
     *
     * @param routes
     *            the route views.
     * @param errorRoutes
     *            the error views.
     * @param layouts
     *            the router layouts.
     * @param skipPwaInit
     *            whether to skip the PWA initialization.
     * @return the new configuration.
     */
    public Routes copy(Set<Class<? extends Component>> routes,
            Set<Class<? extends HasErrorParameter<?>>> errorRoutes,
            Set<Class<? extends RouterLayout>> layouts, boolean skipPwaInit) {
        return new Routes(routes, errorRoutes, layouts, skipPwaInit);
    }

    @Override
    public int hashCode() {
        int result = routes.hashCode();
        result = result * 31 + errorRoutes.hashCode();
        result = result * 31 + layouts.hashCode();
        result = result * 31 + Boolean.hashCode(skipPwaInit);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Routes that)) {
            return false;
        }
        return routes.equals(that.routes)
                && errorRoutes.equals(that.errorRoutes)
                && layouts.equals(that.layouts)
                && skipPwaInit == that.skipPwaInit;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(
            Throwable e) throws E {
        throw (E) e;
    }

    /**
     * Kept for binary compatibility; has no members.
     */
    public static final class Companion {
        private Companion() {
        }
    }
}
