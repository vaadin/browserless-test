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
import jakarta.servlet.ServletException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.vaadin.browserless.BrowserlessConfiguration;
import com.vaadin.browserless.BrowserlessTestSetupException;
import com.vaadin.browserless.mocks.MockHttpSession;
import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.browserless.mocks.MockResponse;
import com.vaadin.browserless.mocks.MockServletConfig;
import com.vaadin.browserless.mocks.MockVaadinHelper;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Builds and tears down the mocked Vaadin environment.
 * <p>
 * {@link #setup()} creates the servlet, service, session, request, response and
 * UI a deployed application would have, and installs them as the thread-locals
 * Flow reads. {@link #tearDown()} takes them down again and fires the lifecycle
 * events a container would. Strong references to the session, UI, request and
 * response are held in thread-locals, because Flow only soft-references them
 * and they would otherwise be collected mid-test.
 * <p>
 * It also stands in for the browser where Flow expects one to act:
 * {@link #clientRoundtrip()} runs what a client response would have triggered,
 * and {@link MockPage} recreates the UI on {@code Page.reload()} the way a
 * browser refresh does.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class MockVaadin {

    private MockVaadin() {
    }

    // prevent GC on Vaadin Session and Vaadin UI as they are only
    // soft-referenced from the Vaadin itself.
    // use ThreadLocals so that multiple threads may initialize fresh Vaadin
    // instances at the same time.
    private static final ThreadLocal<VaadinSession> strongRefSession = new ThreadLocal<>();
    private static final ThreadLocal<UI> strongRefUI = new ThreadLocal<>();
    private static final ThreadLocal<VaadinRequest> strongRefReq = new ThreadLocal<>();
    private static final ThreadLocal<VaadinResponse> strongRefRes = new ThreadLocal<>();
    private static final ThreadLocal<Location> lastNavigation = new ThreadLocal<>();

    // The window name to assign to the next UI created by createUI. A reload
    // records the closing UI's window name here so the recreated UI reuses it
    // (see closeCurrentUI); a brand-new window leaves it unset and gets a
    // fresh unique name. Mirrors the lastNavigation hand-off.
    private static final ThreadLocal<String> lastWindowName = new ThreadLocal<>();

    private static final AtomicLong windowNameCounter = new AtomicLong();

    // Maps a UI detached by a page reload to the UI that replaced it. A reload
    // can be triggered by application code calling Page.reload(), so anything
    // holding on to a UI (notably BrowserlessUIContext) needs a way to follow
    // its window to the live UI. Weakly keyed, so an entry disappears as soon
    // as the detached UI is no longer referenced.
    private static final Map<UI, UI> reloadReplacements = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static final ThreadLocal<Boolean> currentlyClosingSession = ThreadLocal
            .withInitial(() -> Boolean.FALSE);

    private static String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:94.0) Gecko/20100101 Firefox/94.0";

    private static Function<MockHttpSession, MockRequest> mockRequestFactory = MockRequest::new;

    /**
     * Returns the {@code User-Agent} every mocked request reports. Defaults to
     * Firefox 94 on Ubuntu Linux.
     *
     * @return the user agent string
     */
    public static String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the {@code User-Agent} every mocked request reports. Call before
     * {@link #setup()}, since the value is read while the session is created.
     *
     * @param userAgent
     *            the user agent string
     */
    public static void setUserAgent(String userAgent) {
        MockVaadin.userAgent = userAgent;
    }

    /**
     * Returns the factory creating the {@link MockRequest} of each session.
     *
     * @return the request factory
     */
    public static Function<MockHttpSession, MockRequest> getMockRequestFactory() {
        return mockRequestFactory;
    }

    /**
     * Sets the factory creating the {@link MockRequest} of each session.
     * Override when a test needs a subclass of {@link MockRequest}.
     *
     * @param mockRequestFactory
     *            the request factory
     */
    public static void setMockRequestFactory(
            Function<MockHttpSession, MockRequest> mockRequestFactory) {
        MockVaadin.mockRequestFactory = mockRequestFactory;
    }

    /**
     * Records that {@code detached} was replaced by {@code created} during a
     * page reload, so that {@link #liveUI(UI)} can map the old UI to the new
     * one.
     *
     * @param detached
     *            the UI the reload tore down
     * @param created
     *            the UI that replaced it
     */
    public static void recordReloadReplacement(UI detached, UI created) {
        reloadReplacements.put(detached, created);
    }

    /**
     * Follows the reload chain starting at {@code ui} and returns the UI that
     * is live now: a reload detaches a UI and creates a fresh one for the same
     * window, so a caller holding the detached UI must be redirected to its
     * replacement.
     *
     * @param ui
     *            the UI to follow
     * @return the live UI, or {@code ui} itself if it was never replaced
     */
    public static UI liveUI(UI ui) {
        UI live = ui;
        UI next = reloadReplacements.get(live);
        while (next != null && next != live) {
            live = next;
            next = reloadReplacements.get(live);
        }
        return live;
    }

    /**
     * Mocks Vaadin for the current test method:
     * 
     * <pre>
     * MockVaadin.setup(Routes().autoDiscoverViews("com.myapp"))
     * </pre>
     *
     * The UI factory <em>must</em> provide a new, fresh instance of the UI, so
     * that the tests start from a pre-known state. If you're using Spring and
     * you're getting UI from the injector, you must reconfigure Spring to use
     * prototype scope, otherwise an old UI from the UI scope or Session Scope
     * will be provided.
     *
     * Sometimes you wish to provide a specific {@link VaadinServletService},
     * e.g. to override {@link VaadinServletService#loadInstantiators} and
     * provide your own way of instantiating Views, e.g. via Spring or Guice.
     * Please do that by extending {@link MockVaadinServlet} and overriding
     * {@link MockVaadinServlet#createServletService}
     * `createServletService(DeploymentConfiguration)`. Please consult
     * {@link com.vaadin.browserless.mocks.MockService MockService} on what
     * methods you must override in your custom service. Alternatively, see
     * `MockSpringServlet` (in the `browserless-test-spring` module) on how to
     * extend your custom servlet and provide all necessary mocking code.
     *
     * @param routes
     *            all classes annotated with
     *            {@code com.vaadin.flow.router.Route}; use
     *            {@link Routes#autoDiscoverViews} to auto-discover all such
     *            classes.
     * @param uiFactory
     *            produces {@link UI} instances and sets them as current, by
     *            default simply instantiates {@link MockedUI} class.
     * @param lookupServices
     *            service classes to be provided to the lookup initializer
     * @param configuration
     *            the deployment configuration to use
     */
    public static void setup(Routes routes, UIFactory uiFactory,
            Set<Class<?>> lookupServices,
            BrowserlessConfiguration configuration) {
        // init servlet
        MockVaadinServlet servlet = new MockVaadinServlet(routes);
        setup(uiFactory, servlet, lookupServices, configuration);
    }

    /**
     * Equivalent to
     * {@code setup(routes, uiFactory, lookupServices, BrowserlessConfiguration.empty())}.
     *
     * @param routes
     *            the routes to register
     * @param uiFactory
     *            creates the UI instance
     * @param lookupServices
     *            the lookup services to register
     */
    public static void setup(Routes routes, UIFactory uiFactory,
            Set<Class<?>> lookupServices) {
        setup(routes, uiFactory, lookupServices,
                BrowserlessConfiguration.empty());
    }

    /**
     * Equivalent to {@code setup(routes, uiFactory, Collections.emptySet())}.
     *
     * @param routes
     *            the routes to register
     * @param uiFactory
     *            creates the UI instance
     */
    public static void setup(Routes routes, UIFactory uiFactory) {
        setup(routes, uiFactory, Collections.emptySet());
    }

    /**
     * Equivalent to
     * {@code setup(routes, MockedUI::new, Collections.emptySet())}.
     *
     * @param routes
     *            the routes to register
     */
    public static void setup(Routes routes) {
        setup(routes, MockedUI::new, Collections.emptySet());
    }

    /**
     * Equivalent to
     * {@code setup(new Routes(), MockedUI::new, Collections.emptySet())}.
     */
    public static void setup() {
        setup(new Routes(), MockedUI::new, Collections.emptySet());
    }

    /**
     * Equivalent to
     * {@code setup(new Routes(), uiFactory, Collections.emptySet())}.
     *
     * @param uiFactory
     *            creates the UI instance
     */
    public static void setup(UIFactory uiFactory) {
        setup(new Routes(), uiFactory, Collections.emptySet());
    }

    /**
     * Use this method when you need to provide a completely custom servlet
     * (e.g. `SpringServlet`). Do not forget to create a specialized service
     * which works in mocked environment.
     *
     * @param uiFactory
     *            produces {@link UI} instances and sets them as current.
     * @param servlet
     *            allows you to provide your own implementation of
     *            {@link VaadinServlet}.
     * @param lookupServices
     *            service classes to be provided to the lookup initializer
     * @param configuration
     *            the deployment configuration to use
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet,
            Set<Class<?>> lookupServices,
            BrowserlessConfiguration configuration) {
        VaadinServletService service = setupServlet(servlet, lookupServices,
                configuration);
        VaadinService.setCurrent(service);

        // init Vaadin Session
        createSession(servlet.getServletContext(), uiFactory);
    }

    /**
     * Equivalent to
     * {@code setup(uiFactory, servlet, lookupServices, BrowserlessConfiguration.empty())}.
     *
     * @param uiFactory
     *            creates the UI instance
     * @param servlet
     *            the servlet to act on
     * @param lookupServices
     *            the lookup services to register
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet,
            Set<Class<?>> lookupServices) {
        setup(uiFactory, servlet, lookupServices,
                BrowserlessConfiguration.empty());
    }

    /**
     * Equivalent to {@code setup(uiFactory, servlet, Collections.emptySet())}.
     *
     * @param uiFactory
     *            creates the UI instance
     * @param servlet
     *            the servlet to act on
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet) {
        setup(uiFactory, servlet, Collections.emptySet());
    }

    /**
     * Equivalent to
     * {@code setup((UIFactory) MockedUI::new, servlet, Collections.emptySet())}.
     *
     * @param servlet
     *            the servlet to act on
     */
    public static void setup(VaadinServlet servlet) {
        setup((UIFactory) MockedUI::new, servlet, Collections.emptySet());
    }

    /**
     * Equivalent to
     * {@code setup((UIFactory) MockedUI::new, servlet, lookupServices)}.
     *
     * @param servlet
     *            the servlet to act on
     * @param lookupServices
     *            the lookup services to register
     */
    public static void setup(VaadinServlet servlet,
            Set<Class<?>> lookupServices) {
        setup((UIFactory) MockedUI::new, servlet, lookupServices);
    }

    /**
     * Initializes the given {@code servlet} and its service, but does NOT
     * create a session, UI, or set any thread-locals. Call this when you need
     * to share a single service across multiple independent sessions
     * (multi-user testing).
     *
     * @param servlet
     *            the servlet to act on
     * @param lookupServices
     *            the lookup services to register
     * @param configuration
     *            the deployment configuration to use
     * @return the initialized {@link VaadinServletService}
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet,
            Set<Class<?>> lookupServices,
            BrowserlessConfiguration configuration) {
        if (!Utils.isInitialized(servlet)) {
            // Lookup services can be given both explicitly and through the
            // configuration (e.g. by a @BrowserlessTestConfig annotation);
            // they accumulate.
            Set<Class<?>> allLookupServices = new LinkedHashSet<>(
                    lookupServices);
            allLookupServices.addAll(configuration.getLookupServices());
            ServletContext ctx = MockVaadinHelper
                    .createMockContext(allLookupServices);
            // Context init parameters are read by ApplicationConfiguration,
            // which is created and cached on first access, so they must be set
            // before anything else touches the context.
            configuration.getApplicationProperties()
                    .forEach(ctx::setInitParameter);
            // Installed before the servlet is initialized, so that feature
            // flags read during startup (e.g. by a VaadinServiceInitListener)
            // already observe the test configuration. Installed even without
            // overrides, so that tests toggling feature flags at runtime don't
            // write them into the project resources folder, from where other
            // tests would then read them.
            BrowserlessFeatureFlags.install(new VaadinServletContext(ctx),
                    configuration.getFeatureFlags());
            MockServletConfig config = new MockServletConfig(ctx);
            config.getServletInitParams()
                    .putAll(configuration.getApplicationProperties());
            // Enforced by the browserless environment, so it wins over test
            // configuration
            config.getServletInitParams().put(InitParameters.BROWSERLESS,
                    "true");
            try {
                servlet.init(config);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        } else if (!configuration.isEmpty()) {
            // Application properties, feature flags and lookup services are all
            // read while the servlet is initialized, so there is no way to
            // apply them afterwards. Failing here beats silently running the
            // test against an environment that never saw its own configuration.
            throw new BrowserlessTestSetupException(
                    "Cannot apply a custom Vaadin configuration to "
                            + servlet.getClass().getName()
                            + ", because the servlet has already been initialized. The configuration "
                            + "is read while the servlet initializes, so it must be provided to the "
                            + "setup creating the servlet. Provide a servlet factory returning a new, "
                            + "uninitialized servlet instance, or move the configuration to the setup "
                            + "that initializes it. Discarded configuration: "
                            + configuration);
        }
        VaadinServletService service = MockVaadinServlet.serviceSafe(servlet);
        if (service == null) {
            throw new IllegalStateException("Service is null");
        }
        if (service.getRouter() == null) {
            throw new IllegalStateException(servlet
                    + " failed to call VaadinServletService.init() in createServletService()");
        }
        return service;
    }

    /**
     * Equivalent to
     * {@code setupServlet(servlet, lookupServices, BrowserlessConfiguration.empty())}.
     *
     * @param servlet
     *            the servlet to act on
     * @param lookupServices
     *            the lookup services to register
     * @return the service the servlet was initialized with
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet,
            Set<Class<?>> lookupServices) {
        return setupServlet(servlet, lookupServices,
                BrowserlessConfiguration.empty());
    }

    /**
     * Equivalent to
     * {@code setupServlet(servlet, Collections.emptySet(), BrowserlessConfiguration.empty())}.
     *
     * @param servlet
     *            the servlet to act on
     * @return the service the servlet was initialized with
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet) {
        return setupServlet(servlet, Collections.emptySet(),
                BrowserlessConfiguration.empty());
    }

    /**
     * Properly closes the current UI and fire the detach event on it. Does
     * nothing if there is no current UI.
     *
     * @param fireUIDetach
     *            whether the UI detach listeners are fired
     */
    public static void closeCurrentUI(boolean fireUIDetach) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        lastNavigation.set(ui.getInternals().getActiveViewLocation());
        // Preserve the window name so a following createUI (reload / session
        // recreation) reuses it, keeping @PreserveOnRefresh's cache key stable.
        lastWindowName.set(
                ui.getInternals().getExtendedClientDetails().getWindowName());
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
     * Cleans up and removes the Vaadin UI and Vaadin Session. You can call this
     * function in `afterEach{}` block, to clean up after the test. This comes
     * handy when you want to be extra-sure that the next test won't
     * accidentally reuse old UI, should you forget to call {@code setup}
     * properly.
     *
     * You don't have to call this function though; {@code setup} will overwrite
     * any current UI/Session instances with a fresh ones.
     */
    public static void tearDown() {
        clearVaadinInstances(false);
        VaadinService service = VaadinService.getCurrent();
        if (service != null) {
            fireServiceDestroyListeners(service,
                    new ServiceDestroyEvent(service));
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
     * Fires session-destroy listeners on {@code session} and drains pending
     * {@link VaadinSession#access} tasks scheduled during destruction. The
     * `currentlyClosingSession` flag is set for the duration so the
     * `afterSessionClose` recreation hook (used by single-user `setup`) is
     * suppressed — multi-user callers manage their own session lifecycle.
     *
     * @param session
     *            the session to act on
     */
    public static void fireSessionDestroyAndDrain(VaadinSession session) {
        VaadinService service = session.getService();
        service.fireSessionDestroy(session);
        VaadinSession.setCurrent(null);
        // service destroys session via session.access(); we need to run that
        // action now.
        currentlyClosingSession.set(Boolean.TRUE);
        try {
            runUIQueue(false, session);
        } finally {
            currentlyClosingSession.set(Boolean.FALSE);
        }
    }

    /**
     * Creates a new session, request and response for the given
     * {@code service}, but does NOT set any thread-locals or create a UI.
     *
     * @param service
     *            the service to act on
     * @return a new session, request and response for the given {@code
     *         service}, but does NOT set any thread-locals or create a UI
     */
    public static SessionObjects createSessionObjects(
            VaadinServletService service) {
        MockHttpSession httpSession = MockHttpSession
                .create(service.getServlet().getServletContext());

        // init Vaadin Request
        MockRequest mockRequest = mockRequestFactory.apply(httpSession);
        mockRequest.getHeaders().put("User-Agent",
                Collections.singletonList(userAgent));
        MockRequestCustomizer customizer = (MockRequestCustomizer) service
                .getContext().getAttribute(Lookup.class)
                .lookup(MockRequestCustomizer.class);
        if (customizer != null) {
            customizer.apply(mockRequest);
        }
        com.vaadin.flow.server.VaadinServletRequest request = MockVaadinServlet
                .createVaadinServletRequest(mockRequest, service);

        // init Session.
        VaadinSession session = MockVaadinServlet.createVaadinSession(service,
                request);
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        httpSession.setAttribute(service.getServiceName() + ".lock", lock);
        httpSession.setAttribute(
                VaadinSession.class.getName() + "." + service.getServiceName(),
                session);
        session.refreshTransients(new WrappedHttpSession(httpSession), service);
        if (session.getLockInstance() == null) {
            throw new IllegalStateException(session + " created from " + service
                    + " has null lock. See the MockSession class on how to mock locks properly");
        }
        if (!((ReentrantLock) session.getLockInstance()).isLocked()) {
            throw new IllegalStateException(session + " created from " + service
                    + ": lock must be locked!");
        }

        session.setBrowser(MockVaadinServlet.createWebBrowser(request));
        if (session.getBrowser().getBrowserApplication() == null) {
            throw new IllegalStateException(
                    "The WebBrowser has not been mocked properly");
        }

        // init Vaadin Response
        com.vaadin.flow.server.VaadinServletResponse response = MockVaadinServlet
                .createVaadinServletResponse(new MockResponse(), service);

        return new SessionObjects(session, request, response, httpSession);
    }

    private static void createSession(ServletContext ctx, UIFactory uiFactory) {
        VaadinServletService service = (VaadinServletService) VaadinService
                .getCurrent();
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
        fireSessionInitListeners(service,
                new SessionInitEvent(service, objs.session, objs.request));

        // create UI
        createUI(uiFactory, objs.session);
    }

    /**
     * Creates a new UI in the given session and navigates it to the current
     * location, as a browser would on a fresh page load.
     *
     * @param uiFactory
     *            produces the UI instance
     * @param session
     *            the session the UI belongs to
     */
    public static void createUI(UIFactory uiFactory, VaadinSession session) {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException("No current request");
        }
        UI ui = uiFactory.invoke();
        if (ui.getSession() != null) {
            throw new IllegalArgumentException("uiFactory produced UI " + ui
                    + " which is already attached to a Session, "
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

        // Assign a stable, non-null window name via ExtendedClientDetails so
        // @PreserveOnRefresh works. Flow keys its preserved-component cache on
        // window.name, and the name must (a) be non-null and (b) stay constant
        // across reloads of the same window. A reload carries the previous UI's
        // name in lastWindowName; a brand-new window gets a fresh unique name.
        // The name is consumed before the UI is initialized, so a failing
        // initialization cannot leak it into the next createUI on this thread.
        // Every other detail stays at its placeholder default, exactly as in
        // the instance Flow would have created lazily: the window name is all
        // the router reads, so the mock does not fabricate a screen or
        // viewport geometry it has no way to know.
        String windowName = lastWindowName.get();
        if (windowName == null) {
            windowName = "window-" + windowNameCounter.incrementAndGet();
        }
        lastWindowName.remove();
        ui.getInternals()
                .setExtendedClientDetails(new ExtendedClientDetails(ui, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, windowName, null, null, null));

        UI.setCurrent(ui);
        ui.doInit(request, 1, "ROOT");
        strongRefUI.set(ui);

        session.addUI(ui);
        session.getService().getEventBus().fireEvent(
                new UIInitEvent(ui, session.getService()),
                rethrowListenerFailure);

        // navigate to the initial page
        if (lastNavigation.get() != null) {
            UI.getCurrent().getInternals().getRouter().navigate(UI.getCurrent(),
                    lastNavigation.get(), NavigationTrigger.PROGRAMMATIC);
            lastNavigation.remove();
        } else {
            if (UI.getCurrent().getInternals().getRouter().getRegistry()
                    .getNavigationTarget("").isPresent()) {
                UI.getCurrent().navigate("");
            }
        }

        // make sure that UI.getCurrent().push() can be called.
        // https://github.com/mvysny/karibu-testing/issues/80
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
    }

    /**
     * Since Browserless Testing runs in the same JVM as the server and there is
     * no browser, the boundaries between the client and the server become
     * unclear.
     *
     * Calls the following:
     * <ul>
     * <li>{@code runUIQueue}
     * <li>{@link com.vaadin.flow.internal.StateTree#runExecutionsBeforeClientResponse()
     * StateTree.runExecutionsBeforeClientResponse} which runs all blocks
     * scheduled via {@link UI#beforeClientResponse}
     * <li>{@link TestingLifecycleHooks#cleanupDialogs}
     * </ul>
     *
     * If you'd like to test your {@link ErrorHandler} then take a look at
     * {@code runUIQueue} instead.
     *
     * @throws IllegalStateException
     *             if the environment is not mocked
     */
    public static void clientRoundtrip() {
        if (VaadinSession.getCurrent() == null) {
            throw new IllegalStateException("No VaadinSession");
        }
        runUIQueue();
        UI.getCurrent().getInternals().getStateTree()
                .runExecutionsBeforeClientResponse();
        TestingLifecycleHooks.cleanupDialogs();
    }

    /**
     * Runs all tasks scheduled by {@link UI#access}.
     *
     * If {@link VaadinSession#errorHandler} is not set or
     * {@code propagateExceptionToHandler} is false, any exceptions thrown from
     * {@link com.vaadin.flow.server.Command Command}s scheduled via the
     * {@link UI#access} will make this function fail. The exceptions will be
     * wrapped in {@link ExecutionException}.
     *
     * @param propagateExceptionToHandler
     *            defaults to false. If true and
     *            {@link VaadinSession#errorHandler} is set, any exceptions
     *            thrown from {@link com.vaadin.flow.server.Command Command}s
     *            scheduled via the {@link UI#access} will be redirected to
     *            {@link VaadinSession#errorHandler} and will not be re-thrown
     *            from this method.
     * @param session
     *            the session to act on
     * @throws IllegalStateException
     *             if the environment is not mocked
     */
    public static void runUIQueue(boolean propagateExceptionToHandler,
            VaadinSession session) {
        // we need to set up UI error handler which will be notified for every
        // exception thrown out of the access{} block
        // otherwise the exceptions would simply be logged but unlock() wouldn't
        // fail.
        final List<Throwable> errors = new ArrayList<>();
        ErrorHandler oldErrorHandler = session.getErrorHandler();
        if (oldErrorHandler == null
                || oldErrorHandler instanceof DefaultErrorHandler
                || !propagateExceptionToHandler) {
            session.setErrorHandler(e -> {
                Throwable t = e.getThrowable();
                if (!(t instanceof ExecutionException)) {
                    // for some weird reason t may not be ExecutionException
                    // when it originates from a coroutine :confused:
                    // the stacktrace would point someplace random. Wrap it in
                    // ExecutionException whose stacktrace will point to the
                    // test
                    t = new ExecutionException(t.getMessage(), t);
                }
                errors.add(t);
            });
        }

        try {
            // make sure the lock is held exactly once, otherwise the
            // session.unlock() won't
            // process all Runnables registered via ui.access()
            int lockCount = ((ReentrantLock) session.getLockInstance())
                    .getHoldCount();
            if (lockCount != 1) {
                throw new AssertionError(
                        "Expected 1 lock, actual " + lockCount);
            }

            session.unlock(); // this will process all Runnables registered via
                              // ui.access()
            // lock the session back, so that the test can continue running
            // as-if in the UI thread.
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
    private static <T extends Throwable> void sneakyThrow(Throwable t)
            throws T {
        throw (T) t;
    }

    /**
     * Equivalent to
     * {@code runUIQueue(propagateExceptionToHandler, VaadinSession.getCurrent())}.
     *
     * @param propagateExceptionToHandler
     *            whether a failing command is handed to the session's error
     *            handler instead of being rethrown
     */
    public static void runUIQueue(boolean propagateExceptionToHandler) {
        runUIQueue(propagateExceptionToHandler, VaadinSession.getCurrent());
    }

    /**
     * Equivalent to {@code runUIQueue(false, VaadinSession.getCurrent())}.
     */
    public static void runUIQueue() {
        runUIQueue(false, VaadinSession.getCurrent());
    }

    /**
     * Internal function, do not call directly.
     *
     * Only usable when you are providing your own implementation of
     * {@link VaadinSession}. See
     * {@link com.vaadin.browserless.mocks.MockVaadinSession MockVaadinSession}
     * on how to call this properly.
     *
     * @param session
     *            the session to act on
     * @param uiFactory
     *            creates the UI instance
     */
    public static void afterSessionClose(VaadinSession session,
            UIFactory uiFactory) {
        // We need to simulate the actual browser + servlet container behavior
        // here.
        // Imagine that we want a test scenario where the user logs out, and we
        // want to check that a login prompt appears.

        // To log out the user, the code typically closes the session and tells
        // the browser to reload
        // the page (Page.getCurrent().reload() or similar).
        // Thus the page is reloaded by the browser, and since the session is
        // gone, the servlet container
        // will create a new, fresh session.

        // That's exactly what we need to do here. We need to close the current
        // UI and eradicate it,
        // then we need to close the current session and eradicate it, and then
        // we need to create a completely fresh
        // new UI and Session.

        // A problem appears when the uiFactory accidentally doesn't create a
        // new, fresh instance of UI. Say that
        // we call Spring injector to provide us an instance of the UI, but we
        // accidentally scoped the UI to Session.
        // Spring doesn't know that (since we haven't told Spring that the
        // Session scope is gone) and provides
        // the previous UI instance which is still attached to the session. And
        // it blows.

        if (!currentlyClosingSession.get()) {
            // Vaadin 20.0.5+: closing session also clears the wrapped
            // VaadinSession.getSession().
            // Acquire the wrapped session beforehand.
            MockHttpSession mockSession = Utils.mock(session);
            clearVaadinInstances(true);
            mockSession.destroy();
            createSession(mockSession.getServletContext(), uiFactory);
        }
    }

    /**
     * Fires session init listeners on the given service. Java-friendly static
     * wrapper for the internal extension function.
     *
     * @param service
     *            the service to act on
     * @param session
     *            the session to act on
     * @param request
     *            the request to act on
     */
    public static void fireSessionInit(VaadinService service,
            VaadinSession session, VaadinRequest request) {
        fireSessionInitListeners(service,
                new SessionInitEvent(service, session, request));
    }

    /**
     * Fires service destroy listeners on the given service. Java-friendly
     * static wrapper for the internal extension function.
     *
     * @param service
     *            the service to act on
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
     * Clears the `lastNavigation` ThreadLocal recorded by
     * {@code closeCurrentUI}.
     */
    public static void clearLastNavigation() {
        lastNavigation.remove();
        lastWindowName.remove();
    }

    // ---------------------------------------------------------------
    // Service lifecycle events
    // ---------------------------------------------------------------

    /**
     * Hands a listener failure back to the caller instead of logging it, which
     * is what {@code VaadinServiceEventBus.fireEvent} does by default. A
     * listener that throws during a test should fail that test rather than only
     * leave a line in the log.
     */
    private static final SerializableBiConsumer<EventObject, Exception> rethrowListenerFailure = (
            event, error) -> {
        sneakyThrow(error);
    };

    static void fireSessionInitListeners(VaadinService service,
            SessionInitEvent event) {
        service.getEventBus().fireEvent(event, rethrowListenerFailure);
    }

    static void fireServiceDestroyListeners(VaadinService service,
            ServiceDestroyEvent event) {
        service.getEventBus().fireEvent(event, rethrowListenerFailure);
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

        /**
         * The UI this page belongs to.
         */
        private final UI ui;
        /**
         * Produces the UI instance a reload puts in place of the old one.
         */
        private final UIFactory uiFactory;
        /**
         * The session the UI belongs to.
         */
        private final VaadinSession session;
        /**
         * The recorded outbound navigations, by window name.
         */
        private final Map<String, List<String>> navigations = new LinkedHashMap<>();

        /**
         * Creates the page of the given UI.
         *
         * @param ui
         *            the UI this page belongs to
         * @param uiFactory
         *            produces the UI a reload puts in place of the old one
         * @param session
         *            the session the UI belongs to
         */
        public MockPage(UI ui, UIFactory uiFactory, VaadinSession session) {
            super(ui);
            this.ui = ui;
            this.uiFactory = uiFactory;
            this.session = session;
        }

        /**
         * Returns the URL of the last navigation that replaced the current
         * page.
         *
         * @return the URL, or null if nothing navigated away
         */
        public String getLastExternalNavigationURL() {
            List<String> list = navigations.get("_self");
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(list.size() - 1);
        }

        /**
         * Returns the URL of the last navigation into the given window.
         *
         * @param windowName
         *            the target window name, as passed to {@code open()}
         * @return the URL, or null if nothing opened that window
         */
        public String getExternalNavigationURL(String windowName) {
            List<String> list = navigations
                    .get(normalizeWindowName(windowName));
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(list.size() - 1);
        }

        /**
         * Returns the URLs opened in each window other than the current one.
         *
         * @return the opened URLs, by window name
         */
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
                navigations.computeIfAbsent(normalized, k -> new ArrayList<>())
                        .add(url);
            } else {
                List<String> list = new ArrayList<>();
                list.add(url);
                navigations.put(normalized, list);
            }
            super.open(url, windowName);
        }

        @Override
        public void reload() {
            UI current = UI.getCurrent();
            if (current != ui) {
                if (ui.getSession() == null) {
                    // This UI lost its session, so it is the logout idiom -
                    // capture the UI, close its session, then ask its page to
                    // reload. Closing the session already tore this UI down and
                    // rendered a fresh one for its window, which is what the
                    // reload is asking for, so there is nothing left to do. Not
                    // even the superclass call, which would fail: it schedules
                    // JavaScript on a UI whose session is gone.
                    return;
                }
                // A live UI, but not the current one. Recreating the UI runs on
                // the thread-locals of the current UI, so going on would detach
                // and recreate whichever window is current. Fail before
                // anything
                // is changed.
                throw new IllegalStateException("Cannot reload the page of UI "
                        + ui + ", because the current UI is "
                        + (current != null ? current.toString() : "not set")
                        + ". Reloading recreates the current UI, so the "
                        + "reloaded window must be the current one. Either this "
                        + "UI belongs to another window - activate it first, "
                        + "e.g. through its BrowserlessUIContext or "
                        + "UI.access() - or an earlier reload already replaced "
                        + "it, in which case reload the current UI instead.");
            }

            // recreate the UI on reload(), to simulate browser's F5
            super.reload();
            MockVaadin.closeCurrentUI(true);
            MockVaadin.createUI(uiFactory, session);
            // Record the swap so holders of the detached UI
            // (BrowserlessUIContext) can follow the window to the new UI. This
            // path is also taken when application code calls Page.reload()
            // itself, not only from the reload() DSL.
            UI created = UI.getCurrent();
            if (created != null && created != ui) {
                MockVaadin.recordReloadReplacement(ui, created);
            }
        }

        private String normalizeWindowName(String windowName) {
            if (windowName == null || SELF_NAMES.contains(windowName)) {
                return "_self";
            }
            return windowName;
        }
    }
}
