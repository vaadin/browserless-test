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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.flow.shared.communication.PushMode;

import com.vaadin.browserless.mocks.MockHttpSession;
import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.browserless.mocks.MockResponse;
import com.vaadin.browserless.mocks.MockServletConfig;
import com.vaadin.browserless.mocks.MockVaadinHelper;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.browserless.mocks.MockedUI;

import jakarta.servlet.ServletContext;

public final class MockVaadin {

    private MockVaadin() {
    }

    // prevent GC on Vaadin Session and Vaadin UI as they are only soft-referenced from the Vaadin itself.
    // use ThreadLocals so that multiple threads may initialize fresh Vaadin instances at the same time.
    private static final ThreadLocal<VaadinSession> strongRefSession = new ThreadLocal<>();
    private static final ThreadLocal<UI> strongRefUI = new ThreadLocal<>();
    private static final ThreadLocal<VaadinRequest> strongRefReq = new ThreadLocal<>();
    private static final ThreadLocal<VaadinResponse> strongRefRes = new ThreadLocal<>();
    private static final ThreadLocal<Location> lastNavigation = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> currentlyClosingSession = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Change &amp; call [setup] to set a different browser.
     *
     * The default is Firefox 94 on Ubuntu Linux.
     */
    public static String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:94.0) Gecko/20100101 Firefox/94.0";

    /**
     * Creates [MockRequest]; override if you need to return a class that extends [MockRequest]
     * and modifies its behavior.
     */
    public static Function<MockHttpSession, MockRequest> mockRequestFactory = MockRequest::new;

    /**
     * Mocks Vaadin for the current test method:
     * <pre>
     * MockVaadin.setup(Routes().autoDiscoverViews("com.myapp"))
     * </pre>
     *
     * The UI factory <em>must</em> provide a new, fresh instance of the UI, so that the
     * tests start from a pre-known state. If you're using Spring and you're getting UI
     * from the injector, you must reconfigure Spring to use prototype scope,
     * otherwise an old UI from the UI scope or Session Scope will be provided.
     *
     * Sometimes you wish to provide a specific [VaadinServletService],
     * e.g. to override
     * [VaadinServletService.loadInstantiators] and provide your own way of instantiating Views, e.g. via Spring or Guice.
     * Please do that by extending [MockVaadinServlet] and overriding [MockVaadinServlet.createServletService]
     * `createServletService(DeploymentConfiguration)`.
     * Please consult [MockService] on what methods you must override in your custom service.
     * Alternatively, see `MockSpringServlet` (in the `browserless-test-spring` module) on how to extend your custom servlet and
     * provide all necessary mocking code.
     *
     * @param routes all classes annotated with [com.vaadin.flow.router.Route]; use [Routes.autoDiscoverViews] to auto-discover all such classes.
     * @param uiFactory produces [UI] instances and sets them as current, by default simply instantiates [MockedUI] class.
     * @param lookupServices service classes to be provided to the lookup initializer
     */
    public static void setup(Routes routes, UIFactory uiFactory, Set<Class<?>> lookupServices) {
        // init servlet
        MockVaadinServlet servlet = new MockVaadinServlet(routes);
        setup(uiFactory, servlet, lookupServices);
    }

    public static void setup(Routes routes, UIFactory uiFactory) {
        setup(routes, uiFactory, Collections.emptySet());
    }

    public static void setup(Routes routes) {
        setup(routes, MockedUI::new, Collections.emptySet());
    }

    public static void setup() {
        setup(new Routes(), MockedUI::new, Collections.emptySet());
    }

    /**
     * Equivalent to {@code setup(new Routes(), uiFactory, Collections.emptySet())}.
     */
    public static void setup(UIFactory uiFactory) {
        setup(new Routes(), uiFactory, Collections.emptySet());
    }

    /**
     * Use this method when you need to provide a completely custom servlet (e.g. `SpringServlet`). Do not forget to create a specialized service
     * which works in mocked environment.
     *
     * @param uiFactory produces [UI] instances and sets them as current.
     * @param servlet allows you to provide your own implementation of [VaadinServlet].
     * @param lookupServices service classes to be provided to the lookup initializer
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet, Set<Class<?>> lookupServices) {
        VaadinServletService service = setupServlet(servlet, lookupServices);
        VaadinService.setCurrent(service);

        // init Vaadin Session
        createSession(servlet.getServletContext(), uiFactory);
    }

    public static void setup(UIFactory uiFactory, VaadinServlet servlet) {
        setup(uiFactory, servlet, Collections.emptySet());
    }

    public static void setup(VaadinServlet servlet) {
        setup((UIFactory) MockedUI::new, servlet, Collections.emptySet());
    }

    public static void setup(VaadinServlet servlet, Set<Class<?>> lookupServices) {
        setup((UIFactory) MockedUI::new, servlet, lookupServices);
    }

    /**
     * Initializes the given [servlet] and its service, but does NOT create a
     * session, UI, or set any thread-locals. Call this when you need to share
     * a single service across multiple independent sessions (multi-user testing).
     *
     * @return the initialized [VaadinServletService]
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet, Set<Class<?>> lookupServices) {
        if (!Utils.isInitialized(servlet)) {
            ServletContext ctx = MockVaadinHelper.createMockContext(lookupServices);
            MockServletConfig config = new MockServletConfig(ctx);
            config.servletInitParams.put(InitParameters.BROWSERLESS, "true");
            try {
                servlet.init(config);
            } catch (jakarta.servlet.ServletException e) {
                throw new RuntimeException(e);
            }
        }
        VaadinServletService service = MockVaadinServlet.serviceSafe(servlet);
        if (service == null) {
            throw new IllegalStateException("Service is null");
        }
        if (service.getRouter() == null) {
            throw new IllegalStateException(
                    servlet + " failed to call VaadinServletService.init() in createServletService()");
        }
        return service;
    }

    public static VaadinServletService setupServlet(VaadinServlet servlet) {
        return setupServlet(servlet, Collections.emptySet());
    }

    /**
     * Properly closes the current UI and fire the detach event on it.
     * Does nothing if there is no current UI.
     */
    public static void closeCurrentUI(boolean fireUIDetach) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        lastNavigation.set(ui.getInternals().getActiveViewLocation());
        if (ui.isClosing() && ui.getInternals().getSession() != null) {
            BasicUtils._close(ui);
        }
        if (fireUIDetach) {
            ComponentUtil.onComponentDetach(ui);
        }
        UI.setCurrent(null);
        strongRefUI.remove();
    }

    /**
     * Cleans up and removes the Vaadin UI and Vaadin Session. You can call this function in `afterEach{}` block,
     * to clean up after the test. This comes handy when you want to be extra-sure that the next test won't accidentally reuse old UI,
     * should you forget to call [setup] properly.
     *
     * You don't have to call this function though; [setup] will overwrite any current UI/Session instances with a fresh ones.
     */
    public static void tearDown() {
        clearVaadinInstances(false);
        VaadinService service = VaadinService.getCurrent();
        if (service != null) {
            fireServiceDestroyListeners(service, new ServiceDestroyEvent(service));
            VaadinService.setCurrent(null);
        }
        lastNavigation.remove();
    }

    private static void clearVaadinInstances(boolean fireUIDetach) {
        closeCurrentUI(fireUIDetach);
        closeCurrentSession();
        CurrentInstance.set(VaadinRequest.class, null);
        CurrentInstance.set(VaadinResponse.class, null);
        strongRefReq.remove();
        strongRefRes.remove();
    }

    private static void closeCurrentSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            fireSessionDestroyAndDrain(session);
        }
        strongRefSession.remove();
    }

    /**
     * Fires session-destroy listeners on [session] and drains pending
     * [VaadinSession.access] tasks scheduled during destruction. The
     * `currentlyClosingSession` flag is set for the duration so the
     * `afterSessionClose` recreation hook (used by single-user `setup`) is
     * suppressed — multi-user callers manage their own session lifecycle.
     */
    public static void fireSessionDestroyAndDrain(VaadinSession session) {
        VaadinService service = session.getService();
        service.fireSessionDestroy(session);
        VaadinSession.setCurrent(null);
        // service destroys session via session.access(); we need to run that action now.
        currentlyClosingSession.set(Boolean.TRUE);
        try {
            runUIQueue(false, session);
        } finally {
            currentlyClosingSession.set(Boolean.FALSE);
        }
    }

    /**
     * Creates a new session, request and response for the given [service],
     * but does NOT set any thread-locals or create a UI.
     */
    public static SessionObjects createSessionObjects(VaadinServletService service) {
        MockHttpSession httpSession = MockHttpSession.create(service.getServlet().getServletContext());

        // init Vaadin Request
        MockRequest mockRequest = mockRequestFactory.apply(httpSession);
        mockRequest.headers.put("User-Agent", Collections.singletonList(userAgent));
        MockRequestCustomizer customizer = (MockRequestCustomizer) service.getContext().getAttribute(Lookup.class)
                .lookup(MockRequestCustomizer.class);
        if (customizer != null) {
            customizer.apply(mockRequest);
        }
        com.vaadin.flow.server.VaadinServletRequest request = MockVaadinServlet
                .createVaadinServletRequest(mockRequest, service);

        // init Session.
        VaadinSession session = MockVaadinServlet.createVaadinSession(service, request);
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        httpSession.setAttribute(service.getServiceName() + ".lock", lock);
        httpSession.setAttribute(VaadinSession.class.getName() + "." + service.getServiceName(), session);
        session.refreshTransients(new WrappedHttpSession(httpSession), service);
        if (session.getLockInstance() == null) {
            throw new IllegalStateException(
                    session + " created from " + service + " has null lock. See the MockSession class on how to mock locks properly");
        }
        if (!((ReentrantLock) session.getLockInstance()).isLocked()) {
            throw new IllegalStateException(
                    session + " created from " + service + ": lock must be locked!");
        }

        session.setBrowser(MockVaadinServlet.createWebBrowser(request));
        if (session.getBrowser().getBrowserApplication() == null) {
            throw new IllegalStateException("The WebBrowser has not been mocked properly");
        }

        // init Vaadin Response
        com.vaadin.flow.server.VaadinServletResponse response = MockVaadinServlet
                .createVaadinServletResponse(new MockResponse(), service);

        return new SessionObjects(session, request, response, httpSession);
    }

    private static void createSession(ServletContext ctx, UIFactory uiFactory) {
        VaadinServletService service = (VaadinServletService) VaadinService.getCurrent();
        if (service == null) {
            throw new IllegalStateException("No current VaadinService");
        }
        SessionObjects objs = createSessionObjects(service);

        // install thread-locals
        strongRefReq.set(objs.request);
        CurrentInstance.set(VaadinRequest.class, objs.request);
        VaadinSession.setCurrent(objs.session);
        strongRefSession.set(objs.session);
        strongRefRes.set(objs.response);
        CurrentInstance.set(VaadinResponse.class, objs.response);

        // fire session init listeners
        fireSessionInitListeners(service, new SessionInitEvent(service, objs.session, objs.request));

        // create UI
        createUI(uiFactory, objs.session);
    }

    public static void createUI(UIFactory uiFactory, VaadinSession session) {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException("No current request");
        }
        UI ui = uiFactory.invoke();
        if (ui.getSession() != null) {
            throw new IllegalArgumentException(
                    "uiFactory produced UI " + ui + " which is already attached to a Session, "
                            + "yet we expect the UI to be a fresh new instance, not yet attached to a Session, so that the tests"
                            + " are able to always start with a fresh UI with a pre-known state. Perhaps you're "
                            + "using Spring which reuses a scoped instance of the UI?");
        }

        // hook into Page.reload() and recreate the UI
        try {
            Field pageField = UI.class.getDeclaredField("page");
            pageField.setAccessible(true);
            pageField.set(ui, new MockPage(ui, uiFactory, session));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        ui.doInit(request, 1, "ROOT");
        strongRefUI.set(ui);

        session.addUI(ui);
        session.getService().fireUIInitListeners(ui);

        // navigate to the initial page
        if (lastNavigation.get() != null) {
            UI.getCurrent().getInternals().getRouter().navigate(UI.getCurrent(), lastNavigation.get(),
                    NavigationTrigger.PROGRAMMATIC);
            lastNavigation.remove();
        } else {
            if (UI.getCurrent().getInternals().getRouter().getRegistry().getNavigationTarget("").isPresent()) {
                UI.getCurrent().navigate("");
            }
        }

        // make sure that UI.getCurrent().push() can be called.
        // https://github.com/mvysny/karibu-testing/issues/80
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
    }

    /**
     * Since Browserless Testing runs in the same JVM as the server and there is no browser, the boundaries between the client and
     * the server become unclear.
     *
     * Calls the following:
     * <ul>
     *   <li>[runUIQueue]
     *   <li>[StateTree.runExecutionsBeforeClientResponse] which runs all blocks scheduled via [UI.beforeClientResponse]
     *   <li>[cleanupDialogs]
     * </ul>
     *
     * If you'd like to test your [ErrorHandler] then take a look at [runUIQueue] instead.
     *
     * @throws IllegalStateException if the environment is not mocked
     */
    public static void clientRoundtrip() {
        if (VaadinSession.getCurrent() == null) {
            throw new IllegalStateException("No VaadinSession");
        }
        runUIQueue();
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        TestingLifecycleHooks.cleanupDialogs();
    }

    /**
     * Runs all tasks scheduled by [UI.access].
     *
     * If [VaadinSession.errorHandler] is not set or [propagateExceptionToHandler]
     * is false, any exceptions thrown from [Command]s scheduled via the [UI.access] will make this function fail.
     * The exceptions will be wrapped in [ExecutionException].
     *
     * @param propagateExceptionToHandler defaults to false. If true and [VaadinSession.errorHandler]
     * is set, any exceptions thrown from [Command]s scheduled via the [UI.access] will be
     * redirected to [VaadinSession.errorHandler] and will not be re-thrown from this method.
     * @throws IllegalStateException if the environment is not mocked
     */
    public static void runUIQueue(boolean propagateExceptionToHandler, VaadinSession session) {
        // we need to set up UI error handler which will be notified for every exception thrown out of the access{} block
        // otherwise the exceptions would simply be logged but unlock() wouldn't fail.
        final List<Throwable> errors = new ArrayList<>();
        ErrorHandler oldErrorHandler = session.getErrorHandler();
        if (oldErrorHandler == null || oldErrorHandler instanceof DefaultErrorHandler || !propagateExceptionToHandler) {
            session.setErrorHandler(e -> {
                Throwable t = e.getThrowable();
                if (!(t instanceof ExecutionException)) {
                    // for some weird reason t may not be ExecutionException when it originates from a coroutine :confused:
                    // the stacktrace would point someplace random. Wrap it in ExecutionException whose stacktrace will point to the test
                    t = new ExecutionException(t.getMessage(), t);
                }
                errors.add(t);
            });
        }

        try {
            // make sure the lock is held exactly once, otherwise the session.unlock() won't
            // process all Runnables registered via ui.access()
            int lockCount = ((ReentrantLock) session.getLockInstance()).getHoldCount();
            if (lockCount != 1) {
                throw new AssertionError("Expected 1 lock, actual " + lockCount);
            }

            session.unlock();  // this will process all Runnables registered via ui.access()
            // lock the session back, so that the test can continue running as-if in the UI thread.
            session.lock();
        } finally {
            session.setErrorHandler(oldErrorHandler);
        }

        if (!errors.isEmpty()) {
            Throwable first = errors.get(0);
            for (int i = 1; i < errors.size(); i++) {
                first.addSuppressed(errors.get(i));
            }
            sneakyThrow(first);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static void runUIQueue(boolean propagateExceptionToHandler) {
        runUIQueue(propagateExceptionToHandler, VaadinSession.getCurrent());
    }

    public static void runUIQueue() {
        runUIQueue(false, VaadinSession.getCurrent());
    }

    /**
     * Internal function, do not call directly.
     *
     * Only usable when you are providing your own implementation of [VaadinSession].
     * See [MockVaadinSession] on how to call this properly.
     */
    public static void afterSessionClose(VaadinSession session, UIFactory uiFactory) {
        // We need to simulate the actual browser + servlet container behavior here.
        // Imagine that we want a test scenario where the user logs out, and we want to check that a login prompt appears.

        // To log out the user, the code typically closes the session and tells the browser to reload
        // the page (Page.getCurrent().reload() or similar).
        // Thus the page is reloaded by the browser, and since the session is gone, the servlet container
        // will create a new, fresh session.

        // That's exactly what we need to do here. We need to close the current UI and eradicate it,
        // then we need to close the current session and eradicate it, and then we need to create a completely fresh
        // new UI and Session.

        // A problem appears when the uiFactory accidentally doesn't create a new, fresh instance of UI. Say that
        // we call Spring injector to provide us an instance of the UI, but we accidentally scoped the UI to Session.
        // Spring doesn't know that (since we haven't told Spring that the Session scope is gone) and provides
        // the previous UI instance which is still attached to the session. And it blows.

        if (!currentlyClosingSession.get()) {
            // Vaadin 20.0.5+: closing session also clears the wrapped VaadinSession.getSession().
            // Acquire the wrapped session beforehand.
            MockHttpSession mockSession = Utils.mock(session);
            clearVaadinInstances(true);
            mockSession.destroy();
            createSession(mockSession.getServletContext(), uiFactory);
        }
    }

    /**
     * Fires session init listeners on the given service.
     * Java-friendly static wrapper for the internal extension function.
     */
    public static void fireSessionInit(VaadinService service, VaadinSession session, VaadinRequest request) {
        fireSessionInitListeners(service, new SessionInitEvent(service, session, request));
    }

    /**
     * Fires service destroy listeners on the given service.
     * Java-friendly static wrapper for the internal extension function.
     */
    public static void fireServiceDestroy(VaadinService service) {
        fireServiceDestroyListeners(service, new ServiceDestroyEvent(service));
    }

    /**
     * Clears the current UI from thread-locals without firing detach events.
     * Useful for multi-user context where UIs are managed independently.
     */
    public static void clearCurrentUI() {
        UI.setCurrent(null);
        strongRefUI.remove();
    }

    /**
     * Clears the `lastNavigation` ThreadLocal recorded by [closeCurrentUI].
     */
    public static void clearLastNavigation() {
        lastNavigation.remove();
    }

    // ---------------------------------------------------------------
    // Package-private reflection helpers
    // ---------------------------------------------------------------

    private static volatile Field sessionInitListenersField;
    private static volatile Field serviceDestroyListenersField;

    private static Field sessionInitListenersField() {
        Field f = sessionInitListenersField;
        if (f == null) {
            try {
                f = VaadinService.class.getDeclaredField("sessionInitListeners");
                f.setAccessible(true);
                sessionInitListenersField = f;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return f;
    }

    private static Field serviceDestroyListenersField() {
        Field f = serviceDestroyListenersField;
        if (f == null) {
            try {
                f = VaadinService.class.getDeclaredField("serviceDestroyListeners");
                f.setAccessible(true);
                serviceDestroyListenersField = f;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return f;
    }

    @SuppressWarnings("unchecked")
    static void fireSessionInitListeners(VaadinService service, SessionInitEvent event) {
        try {
            Collection<SessionInitListener> listeners = (Collection<SessionInitListener>) sessionInitListenersField()
                    .get(service);
            for (SessionInitListener l : listeners) {
                try {
                    l.sessionInit(event);
                } catch (com.vaadin.flow.server.ServiceException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static void fireServiceDestroyListeners(VaadinService service, ServiceDestroyEvent event) {
        try {
            Collection<ServiceDestroyListener> listeners = (Collection<ServiceDestroyListener>) serviceDestroyListenersField()
                    .get(service);
            for (ServiceDestroyListener l : listeners) {
                l.serviceDestroy(event);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------
    // Nested types
    // ---------------------------------------------------------------

    /**
     * A Vaadin Session-recreate aware {@link Page} that recreates the UI on
     * reload (simulating the browser pressing F5) and records outbound
     * navigation calls so tests can assert on them.
     */
    public static class MockPage extends Page {

        private static final Set<String> SELF_NAMES = new HashSet<>(
                Arrays.asList("_self", "_parent", "_top", ""));

        private final UIFactory uiFactory;
        private final VaadinSession session;
        private final Map<String, List<String>> navigations = new LinkedHashMap<>();

        public MockPage(UI ui, UIFactory uiFactory, VaadinSession session) {
            super(ui);
            this.uiFactory = uiFactory;
            this.session = session;
        }

        public String getLastExternalNavigationURL() {
            List<String> list = navigations.get("_self");
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(list.size() - 1);
        }

        public String getExternalNavigationURL(String windowName) {
            List<String> list = navigations.get(normalizeWindowName(windowName));
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(list.size() - 1);
        }

        public Map<String, List<String>> getOpenedWindows() {
            Map<String, List<String>> result = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : navigations.entrySet()) {
                if (!"_self".equals(e.getKey())) {
                    result.put(e.getKey(), new ArrayList<>(e.getValue()));
                }
            }
            return result;
        }

        @Override
        public void open(String url, String windowName) {
            String normalized = normalizeWindowName(windowName);
            if ("_blank".equals(normalized)) {
                navigations.computeIfAbsent(normalized, k -> new ArrayList<>()).add(url);
            } else {
                List<String> list = new ArrayList<>();
                list.add(url);
                navigations.put(normalized, list);
            }
            super.open(url, windowName);
        }

        @Override
        public void reload() {
            // recreate the UI on reload(), to simulate browser's F5
            super.reload();
            MockVaadin.closeCurrentUI(true);
            MockVaadin.createUI(uiFactory, session);
        }

        private String normalizeWindowName(String windowName) {
            if (windowName == null || SELF_NAMES.contains(windowName)) {
                return "_self";
            }
            return windowName;
        }
    }
}
