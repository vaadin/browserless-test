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

import jakarta.servlet.ServletContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockSpringServlet;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.mocks.SpringSecurityRequestCustomizer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Represents the application-level context for browserless testing. Holds the
 * VaadinService and servlet, shared across all simulated users and windows.
 *
 * <p>
 * Create user contexts via {@link #newUser()} to simulate multiple concurrent
 * users interacting with the same application.
 *
 * <pre>
 * {@code
 * VaadinTestApplicationContext app = VaadinTestApplicationContext
 *         .forSpring(routes, springCtx);
 * VaadinTestUserContext user1 = app.newUser();
 * VaadinTestUserContext user2 = app.newUser();
 * }
 * </pre>
 */
public class VaadinTestApplicationContext implements AutoCloseable {

    private final VaadinServletService service;
    private final VaadinServlet servlet;
    private final UIFactory uiFactory;
    private final List<VaadinTestUserContext> users = new ArrayList<>();

    private VaadinTestApplicationContext(VaadinServlet servlet,
            Set<Class<?>> lookupServices, UIFactory uiFactory) {
        this.servlet = servlet;
        this.uiFactory = uiFactory;
        this.service = MockVaadin.setupService(servlet, lookupServices);
    }

    /**
     * Creates an application context without Spring, using plain Vaadin mocks.
     *
     * @param routes
     *            discovered routes
     * @return the application context
     */
    public static VaadinTestApplicationContext create(Routes routes) {
        return new VaadinTestApplicationContext(
                new MockVaadinServlet(routes, MockedUI::new), Set.of(),
                MockedUI::new);
    }

    /**
     * Creates an application context backed by a Spring
     * {@link ApplicationContext}, enabling dependency injection in views.
     *
     * @param routes
     *            discovered routes
     * @param springCtx
     *            the Spring application context
     * @return the application context
     */
    public static VaadinTestApplicationContext forSpring(Routes routes,
            ApplicationContext springCtx) {
        BrowserlessTestSpringLookupInitializer
                .setCurrentApplicationContext(springCtx);
        // Register SpringSecurityRequestCustomizer as a Spring bean so the
        // Spring-based Lookup can find it as a MockRequestCustomizer
        if (springCtx instanceof org.springframework.context.ConfigurableApplicationContext cac
                && !springCtx.containsBean(
                        SpringSecurityRequestCustomizer.class.getName())) {
            cac.getBeanFactory().registerSingleton(
                    SpringSecurityRequestCustomizer.class.getName(),
                    new SpringSecurityRequestCustomizer());
        }
        Set<Class<?>> services = new HashSet<>();
        services.add(BrowserlessTestSpringLookupInitializer.class);
        services.add(SpringSecurityRequestCustomizer.class);
        MockSpringServlet servlet = new MockSpringServlet(routes, springCtx,
                MockedUI::new);
        return new VaadinTestApplicationContext(servlet, services,
                MockedUI::new);
    }

    /**
     * Creates a new simulated user (VaadinSession) within this application.
     *
     * @return a new user context
     */
    public VaadinTestUserContext newUser() {
        VaadinTestUserContext user = new VaadinTestUserContext(this);
        users.add(user);
        return user;
    }

    /**
     * Tears down the application context and all associated user contexts.
     */
    @Override
    public void close() {
        for (VaadinTestUserContext user : users) {
            user.close();
        }
        users.clear();
        VaadinService.setCurrent(null);
        BrowserlessTestSpringLookupInitializer.clearCurrentApplicationContext();
    }

    VaadinServletService getService() {
        return service;
    }

    ServletContext getServletContext() {
        return servlet.getServletContext();
    }

    UIFactory getUiFactory() {
        return uiFactory;
    }

    /**
     * Discovers routes from the given packages.
     */
    static Routes discoverRoutes(Set<String> packages) {
        return BaseBrowserlessTest.discoverRoutes(packages);
    }

    /**
     * Discovers routes from the given test class annotations and programmatic
     * packages.
     */
    static Routes discoverRoutes(Class<?> testClass,
            Set<String> extraPackages) {
        Set<String> packages = new HashSet<>(extraPackages);
        ViewPackages vpAnnotation = testClass.getAnnotation(ViewPackages.class);
        if (vpAnnotation != null) {
            Stream.of(vpAnnotation.classes()).map(Class::getPackageName)
                    .forEach(packages::add);
            packages.addAll(Arrays.asList(vpAnnotation.packages()));
            if (packages.isEmpty()) {
                packages.add(testClass.getPackageName());
            }
        }
        packages.removeIf(Objects::isNull);
        return BaseBrowserlessTest.discoverRoutes(packages);
    }
}
