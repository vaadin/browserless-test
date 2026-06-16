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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.googlecode.gentyref.GenericTypeReflector;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.browserless.internal.UtilsKt;
import com.vaadin.flow.component.Component;

/**
 * Process-wide registry that resolves a Vaadin component to its
 * {@link ComponentTester}.
 * <p>
 * The registry is seeded at class-load time with all testers found in
 * {@code com.vaadin.flow.component}. Additional packages can be contributed at
 * runtime through {@link #registerPackages(String...)}: each package is
 * classpath-scanned at most once across the lifetime of the JVM; subsequent
 * registrations of the same package are no-ops.
 * <p>
 * Resolution walks the component's superclass chain and returns the most
 * specific match, falling back to the base {@link ComponentTester} when none is
 * registered. If two testers claim the same component class, the most recent
 * registration wins and a warning is logged.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class TesterRegistry {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TesterRegistry.class);

    private static final Map<Class<?>, Class<? extends ComponentTester>> TESTERS = new HashMap<>();
    private static final Set<String> SCANNED_PACKAGES = new HashSet<>();

    static {
        registerPackages("com.vaadin.flow.component");
    }

    private TesterRegistry() {
    }

    /**
     * Scans the given packages for {@link ComponentTester} subclasses annotated
     * with {@link Tests} and adds them to the registry. Packages that have
     * already been scanned are skipped.
     *
     * @param packages
     *            package names; may be empty
     */
    static synchronized void registerPackages(String... packages) {
        Objects.requireNonNull(packages, "packages must not be null");
        if (packages.length == 0) {
            return;
        }
        String[] toScan = Arrays.stream(packages).filter(Objects::nonNull)
                .filter(SCANNED_PACKAGES::add).toArray(String[]::new);
        if (toScan.length == 0) {
            return;
        }
        scanForTesters(toScan).forEach(TesterRegistry::registerTester);
    }

    @SuppressWarnings("rawtypes")
    private static void registerTester(Class<?> component,
            Class<? extends ComponentTester> tester) {
        Class<? extends ComponentTester> previous = TESTERS.put(component,
                tester);
        if (previous != null && !previous.equals(tester)) {
            LOGGER.warn("Replacing tester for component {}: {} -> {}. "
                    + "Multiple testers declare themselves for the same "
                    + "component class; the most recent registration wins.",
                    component.getName(), previous.getName(), tester.getName());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<Class<?>, Class<? extends ComponentTester>> scanForTesters(
            String... packages) {
        try (ScanResult scan = new ClassGraph().enableClassInfo()
                .enableAnnotationInfo().acceptPackages(packages).scan(2)) {
            ClassInfoList testerList = scan
                    .getClassesWithAnnotation(Tests.class.getName());
            Map<Class<?>, Class<? extends ComponentTester>> testerMap = new HashMap<>();
            testerList
                    .filter(classInfo -> classInfo
                            .extendsSuperclass(ComponentTester.class))
                    .forEach(classInfo -> {
                        try {
                            final Class<?> tester = UtilsKt
                                    .findClassOrThrow(classInfo.getName());
                            final Class<? extends Component>[] annotation = tester
                                    .getAnnotation(Tests.class).value();
                            for (Class<? extends Component> component : annotation) {
                                testerMap.put(component,
                                        (Class<? extends ComponentTester>) tester);
                            }
                            // Enable annotation with fqn for components with
                            // generics that cannot be referenced as
                            // Class<? extends Component>
                            final String[] classes = tester
                                    .getAnnotation(Tests.class).fqn();
                            Arrays.stream(classes).map(clazz -> {
                                try {
                                    return UtilsKt.findClassOrThrow(clazz);
                                } catch (ClassNotFoundException e) {
                                    logTypeLoadingIssue(e,
                                            "Tester '{}' cannot be loaded because of missing component class '{}' on classpath",
                                            classInfo.getName(), clazz);
                                    return null;
                                }
                            }).filter(Objects::nonNull)
                                    .forEach(clazz -> testerMap.put(clazz,
                                            (Class<? extends ComponentTester>) tester));
                        } catch (TypeNotPresentException e) {
                            logTypeLoadingIssue(e,
                                    "Tester '{}' cannot be loaded because of missing class '{}' on classpath",
                                    classInfo.getName(), e.typeName());
                        } catch (ClassNotFoundException
                                | NoClassDefFoundError e) {
                            logTypeLoadingIssue(e,
                                    "Tester '{}' cannot be loaded because of missing class on classpath: {}",
                                    classInfo.getName(), e.getMessage());
                        }
                    });
            return Collections.unmodifiableMap(testerMap);
        }
    }

    private static void logTypeLoadingIssue(Throwable ex, String message,
            Object... args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args, ex);
        } else {
            LOGGER.warn(message, args);
        }
    }

    /**
     * Resolves the best matching tester class for the given component class.
     * The component's superclass chain is walked and the most specific
     * registered tester is returned, falling back to the base
     * {@link ComponentTester} when nothing matches.
     *
     * @param componentClass
     *            the component class to resolve a tester for
     * @return the matching tester class, never {@code null}
     */
    @SuppressWarnings("rawtypes")
    static synchronized <Y extends Component> Class<? extends ComponentTester> resolve(
            Class<Y> componentClass) {
        Class<?> latest = componentClass;
        do {
            Class<? extends ComponentTester> tester = TESTERS.get(latest);
            if (tester != null) {
                return tester;
            }
            latest = latest.getSuperclass();
        } while (latest != null && !Component.class.equals(latest));
        return ComponentTester.class;
    }

    /**
     * Resolves the best matching tester for the given component and returns an
     * initialized instance.
     *
     * @param component
     *            the component to wrap
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <T extends ComponentTester<Y>, Y extends Component> T wrap(
            Y component) {
        Class<? extends ComponentTester> testerClass = resolve(
                component.getClass());
        return (T) instantiate(testerClass, component);
    }

    /**
     * Initializes the given tester class with the given component, bypassing
     * the registry lookup.
     *
     * @param testerClass
     *            the tester class to instantiate
     * @param component
     *            the component the tester should wrap
     */
    static <T extends ComponentTester<Y>, Y extends Component> T instantiate(
            Class<T> testerClass, Y component) {
        try {
            return findConstructor(testerClass, component.getClass())
                    .newInstance(component);
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not instantiate "
                    + testerClass.getSimpleName() + " for component "
                    + component.getClass().getSimpleName(), e);
        }
    }

    /**
     * Finds the single-argument constructor of the given tester that best
     * accepts the supplied component type.
     * <p>
     * A custom tester may declare its constructor with a narrower parameter
     * type than the one resolved from the generic {@code ComponentTester<T>}
     * declaration. For example a tester extending {@code DialogTester} (which
     * binds {@code T} to {@code Dialog}) can wrap a {@code Dialog} subclass and
     * declare a constructor taking that subclass. Looking the constructor up
     * solely by the generic type would fail with a {@link NoSuchMethodException}
     * because {@link Class#getConstructor} matches parameter types exactly.
     * <p>
     * This method instead selects the most specific public constructor whose
     * sole parameter is assignable from the component's runtime type, falling
     * back to the generic component type so the original error is reported when
     * no compatible constructor exists.
     *
     * @param testerClass
     *            the tester class to instantiate
     * @param componentClass
     *            the runtime type of the component to wrap
     * @return a matching constructor
     * @throws NoSuchMethodException
     *             if no compatible constructor is found
     */
    @SuppressWarnings("unchecked")
    private static <T extends ComponentTester<?>> Constructor<T> findConstructor(
            Class<T> testerClass, Class<?> componentClass)
            throws NoSuchMethodException {
        Constructor<?> bestMatch = null;
        for (Constructor<?> candidate : testerClass.getConstructors()) {
            if (candidate.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = candidate.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(componentClass)) {
                continue;
            }
            if (bestMatch == null || bestMatch.getParameterTypes()[0]
                    .isAssignableFrom(parameterType)) {
                bestMatch = candidate;
            }
        }
        if (bestMatch != null) {
            return (Constructor<T>) bestMatch;
        }
        return testerClass.getConstructor(detectComponentType(testerClass));
    }

    /**
     * Detects the component type the given tester wraps from its generic
     * declaration by walking the class hierarchy and resolving the type
     * variable declared on {@link ComponentTester}.
     *
     * @param testerType
     *            the tester type
     * @return the component type the tester defines
     */
    @SuppressWarnings("rawtypes")
    static Class<?> detectComponentType(
            Class<? extends ComponentTester> testerType) {
        if (testerType == ComponentTester.class) {
            return Component.class;
        }
        Map<Type, Type> typeMap = new HashMap<>();
        Class<?> clazz = testerType;
        while (!clazz.equals(ComponentTester.class)) {
            extractTypeArguments(typeMap, clazz);
            clazz = clazz.getSuperclass();
        }
        return GenericTypeReflector.erase(
                typeMap.get(ComponentTester.class.getTypeParameters()[0]));
    }

    private static void extractTypeArguments(Map<Type, Type> typeMap,
            Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) {
            return;
        }
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] typeParameter = ((Class<?>) parameterizedType.getRawType())
                .getTypeParameters();
        Type[] actualTypeArgument = parameterizedType.getActualTypeArguments();
        for (int i = 0; i < typeParameter.length; i++) {
            if (typeMap.containsKey(actualTypeArgument[i])) {
                actualTypeArgument[i] = typeMap.get(actualTypeArgument[i]);
            }
            typeMap.put(typeParameter[i], actualTypeArgument[i]);
        }
    }
}
