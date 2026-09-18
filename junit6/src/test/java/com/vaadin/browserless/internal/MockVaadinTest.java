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

import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.example.base.HelloWorldView;
import com.example.base.ParametrizedView;
import com.example.base.child.ChildView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.TestInitListener;
import com.vaadin.browserless.mocks.MockService;
import com.vaadin.browserless.mocks.MockVaadinServlet;
import com.vaadin.browserless.mocks.MockVaadinSession;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

import static com.vaadin.browserless.TestAssertions.expectThrows;
import static com.vaadin.browserless.TestSerialization.cloneBySerialization;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockVaadinTest {

    private static final String WELCOME_TREE = """
            └── MockedUI[]
                └── WelcomeView[@theme='spacing padding']
                    └── Text[text='Welcome!']""";

    private static Routes routes;

    @BeforeAll
    static void discoverViews() {
        routes = new Routes().autoDiscoverViews("com.example.base");
    }

    @BeforeEach
    void setUp() {
        MockVaadin.setup(routes);
        expectWelcomeTree();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Nested
    class SetupAndTearDown {

        @Test
        void setup_mocksAllTheVaadinCurrentInstances() {
            assertNotNull(UI.getCurrent());
            assertNotNull(VaadinSession.getCurrent());
            assertNotNull(VaadinService.getCurrent());
            assertNotNull(VaadinRequest.getCurrent());
            assertNotNull(VaadinResponse.getCurrent());
            assertNotNull(VaadinSession.getCurrent().getConfiguration());
            assertNotNull(VaadinSession.getCurrent().getService());
            assertNotNull(VaadinSession.getCurrent().getBrowser());
            assertNotNull(VaadinSession.getCurrent().getBrowser().getLocale());
            assertFalse(VaadinSession.getCurrent().getBrowser().isIPhone());
            assertTrue(VaadinSession.getCurrent().getBrowser().isFirefox());
            assertFalse(VaadinSession.getCurrent().getBrowser().isChrome());
            assertFalse(VaadinSession.getCurrent().getBrowser().isChromeOS());
            assertFalse(VaadinSession.getCurrent().getBrowser().isAndroid());
            assertFalse(VaadinSession.getCurrent().getBrowser().isEdge());
        }

        @Test
        void setup_currentUiHasSaneValues() {
            assertNotNull(UI.getCurrent().getLocale());
            assertNotNull(UI.getCurrent().getElement());
            assertNotNull(UI.getCurrent().getSession());
            assertSame(VaadinSession.getCurrent(),
                    UI.getCurrent().getSession());
            assertNotNull(UI.getCurrent().getSession().getSession());
            assertNotNull(UI.getCurrent().getLoadingIndicatorConfiguration());
            assertNotNull(UI.getCurrent().getPushConfiguration());
            assertNotNull(UI.getCurrent().getReconnectDialogConfiguration());
            assertNotNull(UI.getCurrent().getInternals());
            assertNotNull(UI.getCurrent().getPage());
            assertNotNull(UI.getCurrent().getInternals().getRouter());
        }

        @Test
        void setup_uiAndSessionSurviveSerialization() {
            cloneBySerialization(UI.getCurrent());
            cloneBySerialization(VaadinSession.getCurrent());
            // even though they say they are Serializable, VaadinService,
            // VaadinRequest and VaadinResponse really are not.
        }

        @Test
        void setup_calledTwiceInARow_succeeds() {
            MockVaadin.setup();
            MockVaadin.setup();
        }

        @Test
        void setup_calledAgain_createsANewUi() {
            MockVaadin.setup();
            UI ui = UI.getCurrent();
            MockVaadin.setup();
            assertNotSame(ui, UI.getCurrent());
        }

        @Test
        void tearDown_clearsAllTheVaadinCurrentInstances() {
            MockVaadin.tearDown();
            assertNull(VaadinSession.getCurrent());
            assertNull(VaadinService.getCurrent());
            assertNull(VaadinRequest.getCurrent());
            assertNull(VaadinResponse.getCurrent());
            assertNull(UI.getCurrent());
        }

        @Test
        void tearDown_calledRepeatedly_succeeds() {
            MockVaadin.tearDown();
            MockVaadin.tearDown();
            MockVaadin.tearDown();
        }

        @Test
        void tearDown_callsUiDetachListenersExactlyOnce() {
            UI ui = UI.getCurrent();
            ui.add(new VerticalLayout());
            AtomicInteger firstListenerCalls = new AtomicInteger();
            ui.addDetachListener(event -> assertEquals(1,
                    firstListenerCalls.incrementAndGet(),
                    "detach should be called only once"));
            AtomicInteger secondListenerCalls = new AtomicInteger();
            UI.getCurrent()
                    .addDetachListener(event -> assertEquals(1,
                            secondListenerCalls.incrementAndGet(),
                            "detach should be called only once"));

            MockVaadin.tearDown();

            assertEquals(1, firstListenerCalls.get(),
                    "detach should be called exactly once");
            assertEquals(1, secondListenerCalls.get(),
                    "detach should be called exactly once");
        }
    }

    @Nested
    class ProperMocking {

        @Test
        void configuration_isNotInProductionMode() {
            assertFalse(VaadinSession.getCurrent().getConfiguration()
                    .isProductionMode());
        }

        @Test
        void attachAndDetach_callBothTheOverridesAndTheListeners() {
            AtomicInteger attachCallCount = new AtomicInteger();
            AtomicInteger detachCallCount = new AtomicInteger();
            VerticalLayout vl = new VerticalLayout() {
                @Override
                protected void onAttach(AttachEvent attachEvent) {
                    super.onAttach(attachEvent);
                    attachCallCount.incrementAndGet();
                }

                @Override
                protected void onDetach(DetachEvent detachEvent) {
                    super.onDetach(detachEvent);
                    detachCallCount.incrementAndGet();
                }
            };
            vl.addAttachListener(event -> {
                assertTrue(vl.isAttached());
                attachCallCount.incrementAndGet();
            });
            vl.addDetachListener(event -> {
                // a bug in Vaadin? I'd expect the node to be detached (null
                // parent etc) at this point...
                // See https://github.com/vaadin/flow/issues/8809
                assertTrue(vl.isAttached());
                detachCallCount.incrementAndGet();
            });

            // attach
            UI.getCurrent().add(vl);
            assertEquals(2, attachCallCount.get());
            assertTrue(vl.isAttached());
            assertEquals(0, detachCallCount.get());

            // close UI - detach is not called.
            UI.getCurrent().close();
            assertEquals(2, attachCallCount.get());
            assertTrue(vl.isAttached());
            assertEquals(0, detachCallCount.get());

            // detach
            vl.removeFromParent();
            assertEquals(2, attachCallCount.get());
            assertFalse(vl.isAttached());
            assertEquals(2, detachCallCount.get());
        }

        @Test
        void closeUi_detachHappensOnlyWhenTheRequestIsDone() {
            UI ui = UI.getCurrent();
            ui.add(new VerticalLayout());
            AtomicInteger detachCalled = new AtomicInteger();
            ui.addDetachListener(event -> detachCalled.incrementAndGet());
            assertTrue(ui.isAttached());

            // close UI - detach is not called.
            UI.getCurrent().close();
            assertTrue(ui.isAttached());
            assertEquals(0, detachCalled.get());
            assertTrue(UI.getCurrent().isAttached());

            // Mock closing of UI after request handled
            BasicUtils._close(UI.getCurrent());
            assertFalse(ui.isAttached());
            assertEquals(1, detachCalled.get());
            assertFalse(UI.getCurrent().isAttached());
        }

        @Test
        void routeConfiguration_resolvesTheUrlsOfTheDiscoveredViews() {
            RouteConfiguration routeConfig = RouteConfiguration.forRegistry(
                    UI.getCurrent().getInternals().getRouter().getRegistry());
            assertEquals("helloworld",
                    routeConfig.getUrl(HelloWorldView.class));
            assertEquals("params/1",
                    routeConfig.getUrl(ParametrizedView.class, 1));
            assertEquals("parent/child", routeConfig.getUrl(ChildView.class));

            RouteConfiguration applicationScope = RouteConfiguration
                    .forApplicationScope();
            assertEquals("helloworld",
                    applicationScope.getUrl(HelloWorldView.class));
            assertEquals("params/1",
                    applicationScope.getUrl(ParametrizedView.class, 1));
            assertEquals("parent/child",
                    applicationScope.getUrl(ChildView.class));
        }
    }

    @Nested
    class Dialogs {

        @Test
        void cleanupDialogs_closedDialog_isRemovedFromTheComponentTree() {
            Dialog dialog = new Dialog(new Div(new Text("Dialog Text")));
            dialog.open();
            dialog.close();

            TestingLifecycleHooks.cleanupDialogs();

            expectWelcomeTree();
        }
    }

    @Nested
    class PageReloading {

        @Test
        void reload_recreatesTheUi() {
            UI ui = UI.getCurrent();
            AtomicInteger detachCalled = new AtomicInteger();
            ui.addDetachListener(
                    event -> assertEquals(1, detachCalled.incrementAndGet(),
                            "detach should be called only once"));

            UI.getCurrent().getPage().reload();

            // a new UI must be created; but the Session must stay the same.
            assertNotNull(UI.getCurrent());
            assertNotSame(ui, UI.getCurrent());
            // the old UI must be detached properly
            assertEquals(1, detachCalled.get());
        }

        @Test
        void reload_preservesTheSession() {
            VaadinSession session = VaadinSession.getCurrent();
            session.setAttribute("foo", "bar");

            UI.getCurrent().getPage().reload();

            assertSame(session, VaadinSession.getCurrent());
            assertEquals("bar", VaadinSession.getCurrent().getAttribute("foo"));
        }
    }

    @Test
    void sessionClose_recreatesTheSessionAndTheUi() {
        UI ui = UI.getCurrent();
        AtomicBoolean detachCalled = new AtomicBoolean();
        ui.addDetachListener(event -> detachCalled.set(true));
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute("foo", "bar");

        session.close();

        // a new UI+Session must be created
        assertNotNull(UI.getCurrent());
        assertNotNull(VaadinSession.getCurrent());
        assertNotSame(ui, UI.getCurrent());
        assertNotSame(session, VaadinSession.getCurrent());
        // the old UI must be detached properly
        assertTrue(detachCalled.get());
        // the new session must not inherit attributes from the old one
        assertNull(VaadinSession.getCurrent().getAttribute("foo"));
    }

    @Test
    void setup_reusingAUiInstance_failsWithHelpfulMessage() {
        MockedUI ui = new MockedUI();
        MockVaadin.setup(() -> ui);

        expectThrows(IllegalArgumentException.class,
                "which is already attached to a Session",
                () -> MockVaadin.setup(() -> ui));
    }

    @Nested
    class InitListeners {

        @BeforeEach
        void restartWithClearFlags() {
            MockVaadin.tearDown();
            TestInitListener.clearInitFlags();
            MockVaadin.setup(routes);
        }

        @Test
        void initListeners_areInvokedOnSetup() {
            assertTrue(TestInitListener.isServiceInitCalled());
            assertTrue(TestInitListener.isUiInitCalled());
            assertTrue(TestInitListener.isUiBeforeEnterCalled());
        }
    }

    @Nested
    class Request {

        @Test
        void cookies_addedToTheRequest_areVisibleOnTheCurrentRequest() {
            Utils.mock(Utils.currentRequest())
                    .addCookie(new Cookie("foo", "bar"));

            assertEquals(List.of("bar"),
                    Arrays.stream(Utils.currentRequest().getCookies())
                            .map(Cookie::getValue).toList());
        }
    }

    @Nested
    class Response {

        @Test
        void cookies_addedToTheResponse_areRecordedOnTheMockResponse() {
            Utils.currentResponse().addCookie(new Cookie("foo", "bar"));

            assertEquals("bar", Utils.mock(Utils.currentResponse())
                    .getCookie("foo").getValue());
        }

        @Test
        void cookies_addedBeforeUiInit_areVisibleFromUiInit() {
            MockVaadin.tearDown();
            AtomicInteger initCalled = new AtomicInteger();
            MockVaadin.setup(() -> {
                Utils.mock(Utils.currentRequest())
                        .addCookie(new Cookie("foo", "bar"));
                return new UI() {
                    @Override
                    protected void init(VaadinRequest request) {
                        assertEquals(List.of("bar"), Arrays
                                .stream(Utils.currentRequest().getCookies())
                                .map(Cookie::getValue).toList());
                        initCalled.incrementAndGet();
                    }
                };
            });

            assertEquals(1, initCalled.get());
        }
    }

    @Nested
    class Session {

        @Test
        void attributes_setOnTheHttpSession_areReadableThroughTheMock() {
            VaadinSession.getCurrent().getSession().setAttribute("foo", "bar");

            assertEquals("bar",
                    Utils.mock(VaadinSession.getCurrent()).getAttribute("foo"));
        }

        @Test
        void changeSessionId_keepsTheVaadinSessionAndItsAttributes() {
            // How an app without Spring Security protects against session
            // fixation after a successful login. Unlike reinitializeSession()
            // the very same HttpSession is kept and only its ID changes, so
            // the VaadinSession bound to it survives.
            VaadinSession session = VaadinSession.getCurrent();
            String id = session.getSession().getId();
            session.getSession().setAttribute("foo", "bar");
            String vaadinSessionAttribute = VaadinSession.class.getName() + "."
                    + VaadinService.getCurrent().getServiceName();

            VaadinServletRequest request = (VaadinServletRequest) VaadinService
                    .getCurrentRequest();
            String newId = request.getHttpServletRequest().changeSessionId();

            assertNotEquals(id, newId);
            assertEquals(newId, session.getSession().getId());
            assertEquals("bar", session.getSession().getAttribute("foo"));
            assertSame(session,
                    session.getSession().getAttribute(vaadinSessionAttribute));
            assertTrue(session.hasLock());
        }

        @Test
        void reinitializeSession_newHttpSessionIdButAttributesArePreserved() {
            String id = VaadinSession.getCurrent().getSession().getId();
            VaadinSession.getCurrent().getSession().setAttribute("foo", "bar");
            assertTrue(VaadinSession.getCurrent().hasLock());

            VaadinService.reinitializeSession(VaadinRequest.getCurrent());

            assertEquals("bar", VaadinSession.getCurrent().getSession()
                    .getAttribute("foo"));
            assertNotEquals(id,
                    VaadinSession.getCurrent().getSession().getId());
            assertTrue(VaadinSession.getCurrent().hasLock());

            id = VaadinSession.getCurrent().getSession().getId();
            // reinitialize again
            VaadinService.reinitializeSession(VaadinRequest.getCurrent());

            assertEquals("bar", VaadinSession.getCurrent().getSession()
                    .getAttribute("foo"));
            assertNotEquals(id,
                    VaadinSession.getCurrent().getSession().getId());
            assertTrue(VaadinSession.getCurrent().hasLock());
        }
    }

    @Nested
    class MultipleThreads {

        @Test
        void setup_inSeparateThreads_createsSeparateUisAndSessions()
                throws InterruptedException {
            AtomicReference<UI> firstUi = new AtomicReference<>();
            AtomicReference<VaadinSession> firstSession = new AtomicReference<>();
            newVaadinThread(firstUi, firstSession);
            AtomicReference<UI> secondUi = new AtomicReference<>();
            AtomicReference<VaadinSession> secondSession = new AtomicReference<>();
            newVaadinThread(secondUi, secondSession);

            assertNotSame(firstUi.get(), secondUi.get());
            assertNotSame(firstSession.get(), secondSession.get());
        }

        private void newVaadinThread(AtomicReference<UI> ui,
                AtomicReference<VaadinSession> session)
                throws InterruptedException {
            Thread thread = new Thread(() -> {
                MockVaadin.setup();
                ui.set(UI.getCurrent());
                session.set(VaadinSession.getCurrent());
            });
            thread.start();
            thread.join();
            assertNotNull(ui.get());
            assertNotNull(session.get());
        }
    }

    @Nested
    class CustomVaadinService {

        @Test
        void setup_customVaadinService_isTheCurrentService() {
            MockVaadin.tearDown();
            MockVaadin.setup(new MockVaadinServlet(routes) {
                @Override
                protected VaadinServletService createServletService(
                        DeploymentConfiguration deploymentConfiguration)
                        throws ServiceException {
                    VaadinServletService service = new MyMockService(this,
                            deploymentConfiguration);
                    service.init();
                    return service;
                }
            });

            assertEquals(MyMockService.class,
                    VaadinService.getCurrent().getClass());
        }

        @Test
        void setup_serviceListeners_areInvokedOnSetupAndTearDown() {
            MockVaadin.tearDown();
            AtomicInteger sessionInit = new AtomicInteger();
            AtomicInteger uiInit = new AtomicInteger();
            AtomicInteger sessionDestroy = new AtomicInteger();
            AtomicInteger serviceDestroy = new AtomicInteger();
            MockVaadin.setup(new MockVaadinServlet(routes) {
                @Override
                protected VaadinServletService createServletService(
                        DeploymentConfiguration deploymentConfiguration)
                        throws ServiceException {
                    MockService service = new MockService(this,
                            deploymentConfiguration);
                    service.init();
                    service.addSessionInitListener(
                            event -> sessionInit.incrementAndGet());
                    service.addUIInitListener(
                            event -> uiInit.incrementAndGet());
                    service.addSessionDestroyListener(
                            event -> sessionDestroy.incrementAndGet());
                    service.addServiceDestroyListener(
                            event -> serviceDestroy.incrementAndGet());
                    return service;
                }
            });

            assertEquals(1, sessionInit.get());
            assertEquals(1, uiInit.get());
            assertEquals(0, sessionDestroy.get());
            assertEquals(0, serviceDestroy.get());

            MockVaadin.tearDown();

            assertEquals(1, sessionInit.get());
            assertEquals(1, uiInit.get());
            assertEquals(1, sessionDestroy.get());
            assertEquals(1, serviceDestroy.get());
        }
    }

    private static class MyMockService extends VaadinServletService {

        MyMockService(VaadinServlet servlet,
                DeploymentConfiguration deploymentConfiguration) {
            super(servlet, deploymentConfiguration);
        }

        @Override
        public boolean isAtmosphereAvailable() {
            return false;
        }

        @Override
        public String getMainDivId(VaadinSession session,
                VaadinRequest request) {
            return "ROOT-1";
        }

        @Override
        public VaadinSession createVaadinSession(VaadinRequest request) {
            return new MockVaadinSession(this, MockedUI::new);
        }
    }

    private static void expectWelcomeTree() {
        assertEquals(withSortedThemeNames(WELCOME_TREE), withSortedThemeNames(
                PrettyPrintTree.toPrettyTree(UI.getCurrent()).trim()));
    }

    /**
     * Sorts the names within every {@code @theme='...'} of a pretty printed
     * component tree.
     * <p>
     * A component's theme names are a set, so the order they are printed in is
     * whatever order the component happened to add them in — nothing a test
     * should assert on. Sorting both sides keeps a tree assertion about the
     * tree.
     */
    private static String withSortedThemeNames(String tree) {
        Matcher matcher = Pattern.compile("@theme='([^']*)'").matcher(tree);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String sorted = Arrays.stream(matcher.group(1).split(" ")).sorted()
                    .collect(Collectors.joining(" "));
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement("@theme='" + sorted + "'"));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
