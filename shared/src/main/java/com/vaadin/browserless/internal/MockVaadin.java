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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import kotlin.jvm.functions.Function1;

import com.vaadin.browserless.BrowserlessConfiguration;
import com.vaadin.browserless.BrowserlessTestSetupException;
import com.vaadin.browserless.mocks.MockHttpSession;
import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.browserless.mocks.MockResponse;
import com.vaadin.browserless.mocks.MockServletConfig;
import com.vaadin.browserless.mocks.MockVaadinHelper;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.browserless.mocks.MockVaadinServletKt;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateTree;
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
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Mocks the Vaadin environment: the servlet, the service, the session and the
 * UI.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class MockVaadin {

    public static final MockVaadin INSTANCE = new MockVaadin();

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

    // The application property Copilot reads, outside Spring, to decide
    // whether it initializes. Copilot does not expose a constant for it.
    private static final String COPILOT_ENABLE = "copilot.enable";

    private static final ThreadLocal<Boolean> currentlyClosingSession = ThreadLocal
            .withInitial(() -> false);

    private static String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:94.0) Gecko/20100101 Firefox/94.0";

    private static Function1<? super MockHttpSession, ? extends MockRequest> mockRequestFactory = MockRequest::new;

    private MockVaadin() {
    }

    /**
     * Mocks Vaadin for the current test method:
     *
     * <pre>
     * MockVaadin.setup(new Routes().autoDiscoverViews("com.myapp"));
     * </pre>
     *
     * The UI factory <em>must</em> provide a new, fresh instance of the UI, so
     * that the tests start from a pre-known state. If you're using Spring and
     * you're getting UI from the injector, you must reconfigure Spring to use
     * prototype scope, otherwise an old UI from the UI scope or Session Scope
     * will be provided.
     * <p>
     * Sometimes you wish to provide a specific {@link VaadinServletService},
     * e.g. to override {@link VaadinServletService#loadInstantiators()} and
     * provide your own way of instantiating Views, e.g. via Spring or Guice.
     * Please do that by extending {@link MockVaadinServlet} and overriding
     * {@code MockVaadinServlet.createServletService(DeploymentConfiguration)}.
     * Please consult {@code MockService} on what methods you must override in
     * your custom service. Alternatively, see {@code MockSpringServlet} (in the
     * {@code browserless-test-spring} module) on how to extend your custom
     * servlet and provide all necessary mocking code.
     *
     * @param routes
     *            all classes annotated with
     *            {@link com.vaadin.flow.router.Route}; use
     *            {@link Routes#autoDiscoverViews} to auto-discover all such
     *            classes.
     * @param uiFactory
     *            produces {@link UI} instances and sets them as current, by
     *            default simply instantiates {@link MockedUI} class.
     * @param lookupServices
     *            service classes to be provided to the lookup initializer
     * @param configuration
     *            custom Vaadin configuration, such as application properties,
     *            feature flags and lookup services, to apply to the mocked
     *            environment.
     */
    public static void setup(Routes routes, UIFactory uiFactory,
            Set<? extends Class<?>> lookupServices,
            BrowserlessConfiguration configuration) {
        // init servlet
        MockVaadinServlet servlet = new MockVaadinServlet(routes);
        setup(uiFactory, servlet, lookupServices, configuration);
    }

    /**
     * See {@link #setup(Routes, UIFactory, Set, BrowserlessConfiguration)}.
     */
    public static void setup(Routes routes, UIFactory uiFactory,
            Set<? extends Class<?>> lookupServices) {
        setup(routes, uiFactory, lookupServices,
                BrowserlessConfiguration.empty());
    }

    /**
     * See {@link #setup(Routes, UIFactory, Set, BrowserlessConfiguration)}.
     */
    public static void setup(Routes routes, UIFactory uiFactory) {
        setup(routes, uiFactory, Collections.emptySet());
    }

    /**
     * See {@link #setup(Routes, UIFactory, Set, BrowserlessConfiguration)}.
     */
    public static void setup(Routes routes) {
        setup(routes, defaultUIFactory());
    }

    /**
     * See {@link #setup(Routes, UIFactory, Set, BrowserlessConfiguration)}.
     */
    public static void setup() {
        setup(new Routes());
    }

    /**
     * Use this method when you need to provide a completely custom servlet
     * (e.g. {@code SpringServlet}). Do not forget to create a specialized
     * service which works in mocked environment. See below for details on how
     * to do this.
     * <p>
     * The UI factory <em>must</em> provide a new, fresh instance of the UI, so
     * that the tests start from a pre-known state. If you're using Spring and
     * you're getting UI from the injector, you must reconfigure Spring to use
     * prototype scope, otherwise an old UI from the UI scope or Session Scope
     * will be provided.
     *
     * @param uiFactory
     *            produces {@link UI} instances and sets them as current, by
     *            default simply instantiates {@link MockedUI} class.
     * @param servlet
     *            allows you to provide your own implementation of
     *            {@link VaadinServlet}. You MUST override
     *            {@code VaadinServlet.createServletService} and construct a
     *            custom service which overrides important methods. Please
     *            consult {@code com.vaadin.browserless.mocks.MockService} on
     *            what methods you must override in your custom service.
     * @param lookupServices
     *            service classes to be provided to the lookup initializer
     * @param configuration
     *            custom Vaadin configuration, such as application properties,
     *            feature flags and lookup services, to apply to the mocked
     *            environment.
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet,
            Set<? extends Class<?>> lookupServices,
            BrowserlessConfiguration configuration) {
        VaadinServletService service = setupServlet(servlet, lookupServices,
                configuration);
        VaadinService.setCurrent(service);

        // init Vaadin Session
        createSession(servlet.getServletContext(), uiFactory);
    }

    /**
     * See
     * {@link #setup(UIFactory, VaadinServlet, Set, BrowserlessConfiguration)}.
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet,
            Set<? extends Class<?>> lookupServices) {
        setup(uiFactory, servlet, lookupServices,
                BrowserlessConfiguration.empty());
    }

    /**
     * See
     * {@link #setup(UIFactory, VaadinServlet, Set, BrowserlessConfiguration)}.
     */
    public static void setup(UIFactory uiFactory, VaadinServlet servlet) {
        setup(uiFactory, servlet, Collections.emptySet());
    }

    /**
     * See
     * {@link #setup(UIFactory, VaadinServlet, Set, BrowserlessConfiguration)}.
     */
    public static void setup(VaadinServlet servlet) {
        setup(defaultUIFactory(), servlet);
    }

    /**
     * Initializes the given {@code servlet} and its service, but does NOT
     * create a session, UI, or set any thread-locals. Call this when you need
     * to share a single service across multiple independent sessions
     * (multi-user testing).
     *
     * @param configuration
     *            custom Vaadin configuration, such as application properties,
     *            feature flags and lookup services, to apply to the mocked
     *            environment. Lookup services defined by the configuration are
     *            used in addition to the ones given by {@code lookupServices}.
     * @return the initialized {@link VaadinServletService}
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet,
            Set<? extends Class<?>> lookupServices,
            BrowserlessConfiguration configuration) {
        if (!Utils.isInitialized(servlet)) {
            // Lookup services can be given both explicitly and through the
            // configuration (e.g. by a @BrowserlessTestConfig annotation);
            // they accumulate.
            Set<Class<?>> allLookupServices = new LinkedHashSet<>(
                    lookupServices);
            allLookupServices.addAll(configuration.getLookupServices());
            ServletContext ctx = MockVaadinHelper.INSTANCE
                    .createMockContext(allLookupServices);
            // Context init parameters are read by ApplicationConfiguration,
            // which is created and cached on first access, so they must be
            // set before anything else touches the context.
            // Copilot is a development tool for an application running in a
            // browser, and its init listener scans the classpath on every
            // servlet init. It is disabled by default, but the test
            // configuration can enable it again.
            Map<String, String> applicationProperties = new LinkedHashMap<>();
            applicationProperties.put(COPILOT_ENABLE, "false");
            applicationProperties
                    .putAll(configuration.getApplicationProperties());
            applicationProperties.forEach(ctx::setInitParameter);
            // Installed before the servlet is initialized, so that feature
            // flags read during startup (e.g. by a VaadinServiceInitListener)
            // already observe the test configuration. Installed even without
            // overrides, so that tests toggling feature flags at runtime don't
            // write them into the project resources folder, from where other
            // tests would then read them.
            BrowserlessFeatureFlags.install(new VaadinServletContext(ctx),
                    configuration.getFeatureFlags());
            MockServletConfig config = new MockServletConfig(ctx);
            config.getServletInitParams().putAll(applicationProperties);
            // Enforced by the browserless environment, so it wins over test
            // configuration
            config.getServletInitParams().put(InitParameters.BROWSERLESS,
                    "true");
            try {
                servlet.init(config);
            } catch (Exception e) {
                throw Utils.sneakyThrow(e);
            }
        } else if (!configuration.isEmpty()) {
            // Application properties, feature flags and lookup services are
            // all read while the servlet is initialized, so there is no way
            // to apply them afterwards. Failing here beats silently running
            // the test against an environment that never saw its own
            // configuration.
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
        VaadinServletService service = checkNotNull(servlet.getService());
        if (service.getRouter() == null) {
            throw new IllegalStateException(servlet
                    + " failed to call VaadinServletService.init() in createServletService()");
        }
        return service;
    }

    /**
     * See {@link #setupServlet(VaadinServlet, Set, BrowserlessConfiguration)}.
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet,
            Set<? extends Class<?>> lookupServices) {
        return setupServlet(servlet, lookupServices,
                BrowserlessConfiguration.empty());
    }

    /**
     * See {@link #setupServlet(VaadinServlet, Set, BrowserlessConfiguration)}.
     */
    public static VaadinServletService setupServlet(VaadinServlet servlet) {
        return setupServlet(servlet, Collections.emptySet());
    }

    /**
     * Records that {@code detached} was replaced by {@code created} during a
     * page reload, so that {@link #liveUI(UI)} can map the old UI to the new
     * one.
     */
    public static void recordReloadReplacement(UI detached, UI created) {
        reloadReplacements.put(detached, created);
    }

    /**
     * Follows the reload chain starting at {@code ui} and returns the UI that
     * is live now: a reload detaches a UI and creates a fresh one for the same
     * window, so a caller holding the detached UI must be redirected to its
     * replacement. Returns {@code ui} itself if it was never replaced.
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
     * Properly closes the current UI and fire the detach event on it. Does
     * nothing if there is no current UI.
     */
    public static void closeCurrentUI(boolean fireUIDetach) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        lastNavigation.set(ui.getInternals().getActiveViewLocation());
        // Preserve the window name so a following createUI (reload / session
        // recreation) reuses it, keeping @PreserveOnRefresh's cache key
        // stable.
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
     * function in {@code afterEach{}} block, to clean up after the test. This
     * comes handy when you want to be extra-sure that the next test won't
     * accidentally reuse old UI, should you forget to call {@link #setup()}
     * properly.
     * <p>
     * You don't have to call this function though; {@link #setup()} will
     * overwrite any current UI/Session instances with a fresh ones.
     */
    public static void tearDown() {
        clearVaadinInstances(false);
        VaadinService service = VaadinService.getCurrent();
        if (service != null) {
            MockVaadin.fireServiceDestroyListeners(service,
                    new ServiceDestroyEvent(service));
            VaadinService.setCurrent(null);
        }
        lastNavigation.remove();
        lastWindowName.remove();
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
     * {@code currentlyClosingSession} flag is set for the duration so the
     * {@code afterSessionClose} recreation hook (used by single-user
     * {@code setup}) is suppressed — multi-user callers manage their own
     * session lifecycle.
     */
    public static void fireSessionDestroyAndDrain(VaadinSession session) {
        VaadinService service = session.getService();
        service.fireSessionDestroy(session);
        VaadinSession.setCurrent(null);
        // service destroys session via session.access(); we need to run that
        // action now.
        currentlyClosingSession.set(true);
        try {
            runUIQueue(false, session);
        } finally {
            currentlyClosingSession.set(false);
        }
    }

    /**
     * Change &amp; call {@link #setup()} to set a different browser.
     * <p>
     * The default is Firefox 94 on Ubuntu Linux.
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Change &amp; call {@link #setup()} to set a different browser.
     */
    public void setUserAgent(String userAgent) {
        MockVaadin.userAgent = userAgent;
    }

    /**
     * Creates {@link MockRequest}; override if you need to return a class that
     * extends {@link MockRequest} and modifies its behavior.
     */
    @SuppressWarnings("unchecked")
    public Function1<MockHttpSession, MockRequest> getMockRequestFactory() {
        return (Function1<MockHttpSession, MockRequest>) mockRequestFactory;
    }

    /**
     * Sets the factory creating {@link MockRequest}.
     */
    public void setMockRequestFactory(
            Function1<? super MockHttpSession, ? extends MockRequest> mockRequestFactory) {
        MockVaadin.mockRequestFactory = mockRequestFactory;
    }

    /**
     * Creates a new session, request and response for the given
     * {@code service}, but does NOT set any thread-locals or create a UI.
     * <p>
     * Use this when you need to create multiple independent sessions for
     * multi-user testing. The caller is responsible for setting thread-locals
     * (via {@link VaadinSession#setCurrent}, {@link CurrentInstance}, etc.) and
     * for creating the UI via {@link #createUI}.
     *
     * @param service
     *            the initialized Vaadin service (from {@link #setupServlet})
     * @return the created session objects
     */
    public static SessionObjects createSessionObjects(
            VaadinServletService service) {
        MockHttpSession httpSession = MockHttpSession.Companion
                .create(service.getServlet().getServletContext());

        // init Vaadin Request
        MockRequest mockRequest = mockRequestFactory.invoke(httpSession);
        mockRequest.getHeaders().put("User-Agent",
                Collections.singletonList(userAgent));
        MockRequestCustomizer customizer = service.getContext()
                .getAttribute(Lookup.class).lookup(MockRequestCustomizer.class);
        if (customizer != null) {
            customizer.apply(mockRequest);
        }
        VaadinServletRequest request = new VaadinServletRequest(mockRequest,
                service);

        // init Session.
        VaadinSession session = MockVaadinServletKt
                ._createVaadinSession(service, request);
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

        session.setBrowser(MockVaadinServletKt.WebBrowser(request));
        if (session.getBrowser().getBrowserApplication() == null) {
            throw new IllegalStateException(
                    "The WebBrowser has not been mocked properly");
        }

        // init Vaadin Response
        VaadinServletResponse response = new VaadinServletResponse(
                new MockResponse(), service);

        return new SessionObjects(session, request, response, httpSession);
    }

    private static void createSession(ServletContext ctx, UIFactory uiFactory) {
        VaadinServletService service = (VaadinServletService) checkNotNull(
                VaadinService.getCurrent());
        SessionObjects objs = createSessionObjects(service);

        // install thread-locals
        strongRefReq.set(objs.getRequest());
        CurrentInstance.set(VaadinRequest.class, objs.getRequest());
        VaadinSession.setCurrent(objs.getSession());
        strongRefSession.set(objs.getSession());
        strongRefRes.set(objs.getResponse());
        CurrentInstance.set(VaadinResponse.class, objs.getResponse());

        // fire session init listeners
        MockVaadin.fireSessionInitListeners(service, new SessionInitEvent(
                service, objs.getSession(), objs.getRequest()));

        // create UI
        createUI(uiFactory, objs.getSession());
    }

    public static void createUI(UIFactory uiFactory, VaadinSession session) {
        VaadinRequest request = checkNotNull(VaadinRequest.getCurrent());
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
            Field page = UI.class.getDeclaredField("page");
            page.setAccessible(true);
            page.set(ui, new MockPage(ui, uiFactory, session));
        } catch (ReflectiveOperationException e) {
            throw Utils.sneakyThrow(e);
        }
        ui.getInternals().setSession(session);

        // Assign a stable, non-null window name via ExtendedClientDetails so
        // @PreserveOnRefresh works. Flow keys its preserved-component cache on
        // window.name, and the name must (a) be non-null and (b) stay constant
        // across reloads of the same window. A reload carries the previous
        // UI's name in lastWindowName; a brand-new window gets a fresh unique
        // name. The name is consumed before the UI is initialized, so a
        // failing initialization cannot leak it into the next createUI on this
        // thread. Every other detail stays at its placeholder default, exactly
        // as in the instance Flow would have created lazily: the window name
        // is all the router reads, so the mock does not fabricate a screen or
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
                MockVaadin.RETHROW_LISTENER_FAILURE);

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
     * unclear. When looking into sources of any test method, it's really hard
     * to tell where exactly the server request ends, and where another request
     * starts.
     * <p>
     * You can establish an explicit client boundary in your test, by explicitly
     * calling this method. However, since that would be both laborous and
     * error-prone, the default operation is that Browserless Testing pretends
     * as if there was a client-server roundtrip before every component lookup
     * via the {@code _get}/{@code _find}/{@code _expectNone}/{@code _expectOne}
     * call. See {@link TestingLifecycleHook} for more details.
     * <p>
     * Calls the following:
     * <ul>
     * <li>{@link #runUIQueue()}</li>
     * <li>{@link StateTree#runExecutionsBeforeClientResponse()} which runs all
     * blocks scheduled via {@link UI#beforeClientResponse}</li>
     * <li>{@code cleanupDialogs()}</li>
     * </ul>
     * If you'd like to test your {@link ErrorHandler} then take a look at
     * {@link #runUIQueue()} instead.
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
     * <p>
     * If {@link VaadinSession#getErrorHandler()} is not set or
     * {@code propagateExceptionToHandler} is false, any exceptions thrown from
     * {@code Command}s scheduled via the {@link UI#access} will make this
     * function fail. The exceptions will be wrapped in
     * {@link ExecutionException}. Generally it's best to keep
     * {@code propagateExceptionToHandler} set to false to make any exceptions
     * fail the test; however if you're testing how your own custom
     * {@link VaadinSession#getErrorHandler()} responds to exceptions then set
     * this parameter to true.
     * <p>
     * Called automatically by {@link #clientRoundtrip()} which is by default
     * called automatically from {@link TestingLifecycleHook}. You generally
     * don't need to call this method unless you need to test your
     * {@link ErrorHandler}.
     *
     * @param propagateExceptionToHandler
     *            defaults to false. If true and
     *            {@link VaadinSession#getErrorHandler()} is set, any exceptions
     *            thrown from {@code Command}s scheduled via the
     *            {@link UI#access} will be redirected to
     *            {@link VaadinSession#getErrorHandler()} and will not be
     *            re-thrown from this method.
     * @param session
     *            the session to run the queue of, defaults to
     *            {@link VaadinSession#getCurrent()}.
     * @throws IllegalStateException
     *             if the environment is not mocked
     */
    public static void runUIQueue(boolean propagateExceptionToHandler,
            VaadinSession session) {
        // we need to set up UI error handler which will be notified for every
        // exception thrown out of the acccess{} block otherwise the exceptions
        // would simply be logged but unlock() wouldn't fail.
        List<Throwable> errors = new ArrayList<>();
        ErrorHandler oldErrorHandler = session.getErrorHandler();
        if (oldErrorHandler == null
                || oldErrorHandler instanceof DefaultErrorHandler
                || !propagateExceptionToHandler) {
            session.setErrorHandler(event -> {
                Throwable t = event.getThrowable();
                if (!(t instanceof ExecutionException)) {
                    // for some weird reason t may not be ExecutionException
                    // when it originates from a coroutine :confused: the
                    // stacktrace would point someplace random. Wrap it in
                    // ExecutionException whose stacktrace will point to the
                    // test
                    t = new ExecutionException(t.getMessage(), t);
                }
                errors.add(t);
            });
        }

        try {
            // make sure the lock is held exactly once, otherwise the
            // session.unlock() won't process all Runnables registered via
            // ui.access()
            int lockCount = ((ReentrantLock) session.getLockInstance())
                    .getHoldCount();
            if (lockCount != 1) {
                throw new AssertionError(
                        "Expected 1 lock, actual " + lockCount);
            }

            // this will process all Runnables registered via ui.access()
            session.unlock();
            // lock the session back, so that the test can continue running
            // as-if in the UI thread.
            session.lock();
        } finally {
            session.setErrorHandler(oldErrorHandler);
        }

        if (!errors.isEmpty()) {
            for (Throwable suppressed : errors.subList(1, errors.size())) {
                errors.get(0).addSuppressed(suppressed);
            }
            throw Utils.sneakyThrow(errors.get(0));
        }
    }

    /**
     * See {@link #runUIQueue(boolean, VaadinSession)}.
     */
    public static void runUIQueue(boolean propagateExceptionToHandler) {
        runUIQueue(propagateExceptionToHandler, VaadinSession.getCurrent());
    }

    /**
     * See {@link #runUIQueue(boolean, VaadinSession)}.
     */
    public static void runUIQueue() {
        runUIQueue(false);
    }

    /**
     * Internal function, do not call directly.
     * <p>
     * Only usable when you are providing your own implementation of
     * {@link VaadinSession}. See {@code MockVaadinSession} on how to call this
     * properly.
     */
    public static void afterSessionClose(VaadinSession session,
            UIFactory uiFactory) {
        // We need to simulate the actual browser + servlet container behavior
        // here. Imagine that we want a test scenario where the user logs out,
        // and we want to check that a login prompt appears.

        // To log out the user, the code typically closes the session and tells
        // the browser to reload the page (Page.getCurrent().reload() or
        // similar). Thus the page is reloaded by the browser, and since the
        // session is gone, the servlet container will create a new, fresh
        // session.

        // That's exactly what we need to do here. We need to close the current
        // UI and eradicate it, then we need to close the current session and
        // eradicate it, and then we need to create a completely fresh new UI
        // and Session.

        // A problem appears when the uiFactory accidentally doesn't create a
        // new, fresh instance of UI. Say that we call Spring injector to
        // provide us an instance of the UI, but we accidentally scoped the UI
        // to Session. Spring doesn't know that (since we haven't told Spring
        // that the Session scope is gone) and provides the previous UI
        // instance which is still attached to the session. And it blows.

        if (!currentlyClosingSession.get()) {
            // Vaadin 20.0.5+: closing session also clears the wrapped
            // VaadinSession.getSession(). Acquire the wrapped session
            // beforehand.
            MockHttpSession mockSession = Utils.getMock(session);
            clearVaadinInstances(true);
            mockSession.destroy();
            createSession(mockSession.getServletContext(), uiFactory);
        }
    }

    /**
     * Fires session init listeners on the given service. Java-friendly static
     * wrapper for the internal extension function.
     */
    public static void fireSessionInit(VaadinService service,
            VaadinSession session, VaadinRequest request) {
        MockVaadin.fireSessionInitListeners(service,
                new SessionInitEvent(service, session, request));
    }

    /**
     * Fires service destroy listeners on the given service. Java-friendly
     * static wrapper for the internal extension function.
     */
    public static void fireServiceDestroy(VaadinService service) {
        MockVaadin.fireServiceDestroyListeners(service,
                new ServiceDestroyEvent(service));
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
     * Clears the {@code lastNavigation} ThreadLocal recorded by
     * {@link #closeCurrentUI(boolean)}.
     * <p>
     * {@link #closeCurrentUI(boolean)} records the closing UI's active view
     * location into {@code lastNavigation} so that the immediately following
     * {@link #createUI} call (in the single-user reload / session-recreation
     * flow) can restore that location on the new UI. Multi-user lifecycle close
     * paths ({@code BrowserlessUIContext.close}) call
     * {@link #closeCurrentUI(boolean)} without a matching {@link #createUI}, so
     * this method must be invoked afterwards to prevent the recorded location
     * from leaking into the next unrelated {@link #createUI} on the same thread
     * (e.g. {@code user.newWindow()}). Also clears the recorded window name for
     * the same reason: a fresh window must get a new unique name, not inherit
     * the closed window's name.
     */
    public static void clearLastNavigation() {
        lastNavigation.remove();
        lastWindowName.remove();
    }

    private static UIFactory defaultUIFactory() {
        return MockedUI::new;
    }

    private static <T> T checkNotNull(T value) {
        if (value == null) {
            throw new IllegalStateException("Required value was null.");
        }
        return value;
    }

    /**
     * Hands a listener failure back to the caller instead of logging it, which
     * is what {@link VaadinServiceEventBus#fireEvent} does by default. A
     * listener that throws during a test should fail that test rather than only
     * leave a line in the log.
     */
    static final SerializableBiConsumer<EventObject, Exception> RETHROW_LISTENER_FAILURE = (
            event, error) -> {
        throw Utils.sneakyThrow(error);
    };

    public static void fireSessionInitListeners(VaadinService service,
            SessionInitEvent event) {
        service.getEventBus().fireEvent(event, RETHROW_LISTENER_FAILURE);
    }

    public static void fireServiceDestroyListeners(VaadinService service,
            ServiceDestroyEvent event) {
        service.getEventBus().fireEvent(event, RETHROW_LISTENER_FAILURE);
    }
}
