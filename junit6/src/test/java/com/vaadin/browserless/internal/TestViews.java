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

import java.util.Set;

import com.example.base.ErrorView;
import com.example.base.HelloWorldView;
import com.example.base.ParametrizedView;
import com.example.base.WelcomeView;
import com.example.base.child.ChildView;
import com.example.base.navigation.NavigationPostponeView;
import com.example.base.signals.SignalsView;

import com.vaadin.browserless.viewscan.byannotatedclass.ViewPackagesTestView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;

/**
 * The views route discovery is expected to find, shared by the tests that
 * assert on it.
 */
public final class TestViews {

    private TestViews() {
    }

    /**
     * Loaded by name because the view is package private on purpose: discovery
     * has to find it anyway.
     */
    public static final Class<? extends Component> PACKAGE_PRIVATE_VIEW = packagePrivateView();

    public static final Set<Class<? extends Component>> ALL_VIEWS = Set.of(
            HelloWorldView.class, WelcomeView.class, ParametrizedView.class,
            ChildView.class, NavigationPostponeView.class,
            ViewPackagesTestView.class, SignalsView.class,
            PACKAGE_PRIVATE_VIEW);

    public static final Set<Class<? extends HasErrorParameter<?>>> ALL_ERROR_ROUTES = Set
            .of(ErrorView.class, MockRouteNotFoundError.class,
                    MockInternalSeverError.class);

    private static Class<? extends Component> packagePrivateView() {
        try {
            return Class.forName("com.example.base.PackagePrivateView")
                    .asSubclass(Component.class);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
