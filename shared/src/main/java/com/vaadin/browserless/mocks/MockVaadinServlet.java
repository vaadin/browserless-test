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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Makes sure that [routes] are properly registered, and that [MockService]
 * is used instead of vanilla [VaadinServletService].
 *
 * To use a custom servlet instead of this one, just pass it to [MockVaadin.setup].
 */
public class MockVaadinServlet extends VaadinServlet {

    public final Routes routes;
    public final UIFactory uiFactory;

    public MockVaadinServlet() {
        this(new Routes(), MockedUI::new);
    }

    public MockVaadinServlet(Routes routes) {
        this(routes, MockedUI::new);
    }

    public MockVaadinServlet(Routes routes, UIFactory uiFactory) {
        this.routes = routes;
        this.uiFactory = uiFactory;
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration() throws jakarta.servlet.ServletException {
        MockVaadinHelper.mockFlowBuildInfo(this);
        return super.createDeploymentConfiguration();
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
            throws com.vaadin.flow.server.ServiceException {
        VaadinServletService service = new MockService(this, deploymentConfiguration, uiFactory);
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
            _VaadinServlet_getService = VaadinServlet.class.getDeclaredMethod("getService");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
        _VaadinServletRequest_constructor = findTwoArgConstructor(VaadinServletRequest.class);
        _VaadinServletResponse_constructor = findTwoArgConstructor(VaadinServletResponse.class);
        try {
            _WebBrowser_constructor = WebBrowser.class.getDeclaredConstructor(VaadinRequest.class);
            _WebBrowser_constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
        try {
            _VaadinService_createVaadinSession = VaadinService.class
                    .getDeclaredMethod("createVaadinSession", VaadinRequest.class);
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
        // calling the method directly will cause MethodNotFoundError on Vaadin 20+
        try {
            return (VaadinServletService) _VaadinServlet_getService.invoke(servlet);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Workaround for https://github.com/mvysny/karibu-testing/issues/66
     */
    public static VaadinServletRequest createVaadinServletRequest(HttpServletRequest request, VaadinService service) {
        // we need to use the reflection. The problem is that the signature
        // of the constructor differs between Vaadin versions:
        //
        // Vaadin 14.6: VaadinServletRequest(HttpServletRequest, VaadinServletService)
        // Vaadin 20+: VaadinServletRequest(HttpServletRequest, VaadinService)
        //
        // calling the constructor directly will cause MethodNotFoundError.
        try {
            return (VaadinServletRequest) _VaadinServletRequest_constructor.newInstance(request, service);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Workaround for https://github.com/mvysny/karibu-testing/issues/66
     */
    public static VaadinServletResponse createVaadinServletResponse(HttpServletResponse response, VaadinService service) {
        // we need to use the reflection. The problem is that the signature
        // of the constructor differs between Vaadin versions:
        //
        // Vaadin 14.6: VaadinServletResponse(HttpServletResponse, VaadinServletService)
        // Vaadin 20+: VaadinServletResponse(HttpServletResponse, VaadinService)
        //
        // calling the constructor directly will cause MethodNotFoundError.
        try {
            return (VaadinServletResponse) _VaadinServletResponse_constructor.newInstance(response, service);
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

    public static VaadinSession createVaadinSession(VaadinService service, VaadinRequest request) {
        try {
            return (VaadinSession) _VaadinService_createVaadinSession.invoke(service, request);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
