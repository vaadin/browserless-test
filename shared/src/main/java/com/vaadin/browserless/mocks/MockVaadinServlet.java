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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;

/**
 * The {@link VaadinServlet} of the mocked environment.
 * <p>
 * It registers the routes it was given into the route registry, which a
 * deployment would otherwise populate by classpath scanning at startup, and it
 * creates a {@link MockService} in place of a vanilla
 * {@link VaadinServletService}.
 * <p>
 * It also carries the static helpers that reach into Flow for things a test
 * cannot construct directly — a {@link VaadinServletRequest}, a
 * {@link VaadinServletResponse}, a {@link WebBrowser} or a
 * {@link VaadinSession}.
 * <p>
 * To use a custom servlet instead of this one, pass it to
 * {@link com.vaadin.browserless.internal.MockVaadin#setup}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockVaadinServlet extends VaadinServlet {

    private final Routes routes;

    private final UIFactory uiFactory;

    /**
     * Creates a servlet with no routes, producing {@link MockedUI} instances.
     */
    public MockVaadinServlet() {
        this(new Routes(), MockedUI::new);
    }

    /**
     * Creates a servlet serving the given routes, producing {@link MockedUI}
     * instances.
     *
     * @param routes
     *            the routes to register
     */
    public MockVaadinServlet(Routes routes) {
        this(routes, MockedUI::new);
    }

    /**
     * Creates a servlet serving the given routes, producing UIs from the given
     * factory.
     *
     * @param routes
     *            the routes to register
     * @param uiFactory
     *            produces a fresh UI per session
     */
    public MockVaadinServlet(Routes routes, UIFactory uiFactory) {
        this.routes = routes;
        this.uiFactory = uiFactory;
    }

    /**
     * Returns the routes this servlet registers.
     *
     * @return the routes
     */
    public Routes getRoutes() {
        return routes;
    }

    /**
     * Returns the factory producing the {@link com.vaadin.flow.component.UI} of
     * every session this servlet's service creates.
     *
     * @return the UI factory
     */
    public UIFactory getUiFactory() {
        return uiFactory;
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration()
            throws ServletException {
        MockVaadinHelper.mockFlowBuildInfo(this);
        return super.createDeploymentConfiguration();
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = new MockService(this,
                deploymentConfiguration, uiFactory);
        service.init();
        routes.register((VaadinServletContext) service.getContext());
        return service;
    }

    private static final Method _VaadinServlet_getService;
    private static final Constructor<?> _VaadinServletRequest_constructor;
    private static final Constructor<?> _VaadinServletResponse_constructor;
    private static final Constructor<WebBrowser> _WebBrowser_constructor;
    private static final Method _VaadinService_createVaadinSession;

    static {
        try {
            _VaadinServlet_getService = VaadinServlet.class
                    .getDeclaredMethod("getService");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
        _VaadinServletRequest_constructor = findTwoArgConstructor(
                VaadinServletRequest.class);
        _VaadinServletResponse_constructor = findTwoArgConstructor(
                VaadinServletResponse.class);
        try {
            _WebBrowser_constructor = WebBrowser.class
                    .getDeclaredConstructor(VaadinRequest.class);
            _WebBrowser_constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
        try {
            _VaadinService_createVaadinSession = VaadinService.class
                    .getDeclaredMethod("createVaadinSession",
                            VaadinRequest.class);
            _VaadinService_createVaadinSession.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Constructor<?> findTwoArgConstructor(Class<?> cls) {
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterCount() == 2) {
                return c;
            }
        }
        throw new ExceptionInInitializerError("No 2-arg constructor in " + cls);
    }

    /**
     * Workaround for https://github.com/mvysny/karibu-testing/issues/66
     */
    public static VaadinServletService serviceSafe(VaadinServlet servlet) {
        // we need to use the reflection. The problem is that the signature
        // of the method differs between Vaadin versions:
        //
        // Vaadin 14.6: getService() returns VaadinService
        // Vaadin 20+: getService() returns VaadinServletService
        //
        // calling the method directly will cause MethodNotFoundError on Vaadin
        // 20+
        try {
            return (VaadinServletService) _VaadinServlet_getService
                    .invoke(servlet);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Workaround for https://github.com/mvysny/karibu-testing/issues/66
     */
    public static VaadinServletRequest createVaadinServletRequest(
            HttpServletRequest request, VaadinService service) {
        // we need to use the reflection. The problem is that the signature
        // of the constructor differs between Vaadin versions:
        //
        // Vaadin 14.6: VaadinServletRequest(HttpServletRequest,
        // VaadinServletService)
        // Vaadin 20+: VaadinServletRequest(HttpServletRequest, VaadinService)
        //
        // calling the constructor directly will cause MethodNotFoundError.
        try {
            return (VaadinServletRequest) _VaadinServletRequest_constructor
                    .newInstance(request, service);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Workaround for https://github.com/mvysny/karibu-testing/issues/66
     */
    public static VaadinServletResponse createVaadinServletResponse(
            HttpServletResponse response, VaadinService service) {
        // we need to use the reflection. The problem is that the signature
        // of the constructor differs between Vaadin versions:
        //
        // Vaadin 14.6: VaadinServletResponse(HttpServletResponse,
        // VaadinServletService)
        // Vaadin 20+: VaadinServletResponse(HttpServletResponse, VaadinService)
        //
        // calling the constructor directly will cause MethodNotFoundError.
        try {
            return (VaadinServletResponse) _VaadinServletResponse_constructor
                    .newInstance(response, service);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static WebBrowser createWebBrowser(VaadinRequest request) {
        try {
            return _WebBrowser_constructor.newInstance(request);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static VaadinSession createVaadinSession(VaadinService service,
            VaadinRequest request) {
        try {
            return (VaadinSession) _VaadinService_createVaadinSession
                    .invoke(service, request);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
