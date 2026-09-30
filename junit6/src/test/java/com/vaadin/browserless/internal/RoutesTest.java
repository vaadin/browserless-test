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

import java.util.LinkedHashSet;
import java.util.Set;

import com.example.base.ErrorView;
import com.example.base.HelloWorldView;
import com.example.base.ParametrizedView;
import com.example.base.WelcomeView;
import com.example.base.child.ChildView;
import com.example.base.navigation.NavigationPostponeView;
import com.example.base.signals.SignalsView;
import com.testapp.MyRouteNotFoundError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.mocks.MockVaadinHelper;
import com.vaadin.browserless.viewscan.byannotatedclass.ViewPackagesTestView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteAccessDeniedError;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import static com.vaadin.browserless.TestAssertions.expectThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class RoutesTest {

    // TODO: restrict scan until we have component wrapper test views on this
    // codebase
    private static final String[] PACKAGES_TO_SCAN = { "com.example.base",
            "com.vaadin.browserless.viewscan",
            "com.vaadin.browserless.viewscan4" };

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void autoDiscoverViews_scannedPackages_findsEveryViewAndErrorRoute() {
        Routes routes = new Routes().autoDiscoverViews(PACKAGES_TO_SCAN);
        assertEquals(TestViews.ALL_VIEWS, Set.copyOf(routes.getRoutes()));
        assertEquals(TestViews.ALL_ERROR_ROUTES,
                Set.copyOf(routes.getErrorRoutes()));
    }

    @Test
    void autoDiscoverViews_packagePrivateView_isDiscovered() {
        Routes routes = new Routes().autoDiscoverViews(PACKAGES_TO_SCAN);
        assertTrue(
                routes.getRoutes().stream()
                        .anyMatch(c -> c.getName()
                                .equals("com.example.base.PackagePrivateView")),
                "PackagePrivateView should be discovered");
    }

    @Test
    void autoDiscoverViews_calledRepeatedly_yieldsTheSameViews() {
        assertEquals(TestViews.ALL_VIEWS, Set.copyOf(
                new Routes().autoDiscoverViews(PACKAGES_TO_SCAN).getRoutes()));
        assertEquals(TestViews.ALL_VIEWS, Set.copyOf(
                new Routes().autoDiscoverViews(PACKAGES_TO_SCAN).getRoutes()));
    }

    // https://github.com/mvysny/karibu-testing/issues/50
    @Test
    void autoDiscoverViews_appHandlesNotFoundItself_dropsMockRouteNotFoundError() {
        Routes routes = new Routes().autoDiscoverViews("com.example.base",
                "com.testapp", "com.vaadin.flow.router");
        assertEquals(
                Set.of(ErrorView.class, MockInternalSeverError.class,
                        MyRouteNotFoundError.class, RouteNotFoundError.class,
                        RouteAccessDeniedError.class),
                Set.copyOf(routes.getErrorRoutes()));
        // make sure that Vaadin initializes properly with this set of views
        MockVaadin.setup(routes);
    }

    @Test
    void register_byDefault_skipsPwaInitialization() {
        Routes routes = new Routes().autoDiscoverViews("com.example.base");
        VaadinContext ctx = MockVaadinHelper.createMockVaadinContext();
        routes.register(ctx);
        assertNull(ApplicationRouteRegistry.getInstance(ctx)
                .getPwaConfigurationClass());
    }

    @Test
    void register_skipPwaInitDisabled_discoversThePwaClass() {
        Routes routes = new Routes().autoDiscoverViews("com.example.base");
        routes.setSkipPwaInit(false);
        VaadinContext ctx = MockVaadinHelper.createMockVaadinContext();
        routes.register(ctx);
        assertEquals(WelcomeView.class, ApplicationRouteRegistry
                .getInstance(ctx).getPwaConfigurationClass());
    }

    @Test
    void navigate_unknownRoute_rendersErrorNamingTheAvailableRoutes() {
        Routes routes = new Routes().autoDiscoverViews("com.example.base");
        MockVaadin.setup(routes);
        UI.getCurrent().navigate("A_VIEW_THAT_DOESNT_EXIST");
        HasElement view = UI.getCurrent().getInternals()
                .getActiveRouterTargetsChain().get(0);
        assertEquals(MockRouteNotFoundError.class, view.getClass());
        expectThrows(NotFoundException.class,
                "No route found for 'A_VIEW_THAT_DOESNT_EXIST': Couldn't find route for 'A_VIEW_THAT_DOESNT_EXIST'\nAvailable routes:",
                () -> {
                    throw ((MockRouteNotFoundError) view).getCause();
                });
        String errorMessage = ElementUtils.textRecursively2(view.getElement());
        assertTrue(errorMessage
                .contains("Could not navigate to 'A_VIEW_THAT_DOESNT_EXIST'"));
    }

    @Test
    void merge_twoRouteSets_containsTheUnion() {
        Set<Class<? extends Component>> routes1Views = new LinkedHashSet<>(
                Set.of(HelloWorldView.class, WelcomeView.class,
                        ViewPackagesTestView.class, SignalsView.class,
                        TestViews.PACKAGE_PRIVATE_VIEW));
        Set<Class<? extends HasErrorParameter<?>>> routes1Errors = new LinkedHashSet<>(
                Set.of(ErrorView.class));
        Routes routes1 = new Routes(routes1Views, routes1Errors);

        Set<Class<? extends Component>> routes2Views = new LinkedHashSet<>(
                Set.of(ParametrizedView.class, ChildView.class,
                        NavigationPostponeView.class));
        Set<Class<? extends HasErrorParameter<?>>> routes2Errors = new LinkedHashSet<>(
                Set.of(MockRouteNotFoundError.class,
                        MockInternalSeverError.class));
        Routes routes2 = new Routes(routes2Views, routes2Errors);

        Routes merged = routes1.merge(routes2);
        assertEquals(TestViews.ALL_VIEWS, Set.copyOf(merged.getRoutes()));
        assertEquals(TestViews.ALL_ERROR_ROUTES,
                Set.copyOf(merged.getErrorRoutes()));
    }
}
