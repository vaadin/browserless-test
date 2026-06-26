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
package com.vaadin.browserless.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.junit5.auto.SetBeanDiscoveryMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.browserless.BrowserlessClassExtension;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.cdi.CdiInstantiator;
import com.vaadin.cdi.CdiVaadinServlet;
import com.vaadin.cdi.VaadinExtension;
import com.vaadin.cdi.util.BeanManagerProvider;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteConfiguration;

/**
 * Verifies that a CDI-backed {@link BrowserlessTest} can share a single Vaadin
 * environment across all test methods, set up <em>once per class</em> instead
 * of once per method.
 *
 * <p>
 * The class uses the {@link TestInstance.Lifecycle#PER_CLASS} instance
 * lifecycle and delegates the Vaadin lifecycle to a static
 * {@link BrowserlessClassExtension}, which initializes the environment in
 * {@code beforeAll} and tears it down in {@code afterAll}. The per-method
 * {@code @BeforeEach}/{@code @AfterEach} hooks declared by
 * {@link BrowserlessTest} are deliberately suppressed by overriding
 * {@link #initVaadinEnvironment()} and {@link #cleanVaadinEnvironment()}
 * <em>without</em> re-declaring those annotations, so
 * {@code MockVaadin.setup()} runs exactly once.
 *
 * <p>
 * Ordering is what makes this work with CDI: {@code @EnableAutoWeld} is a
 * declarative extension, so weld-junit5 starts the container in its
 * {@code beforeAll} (it does this for {@code PER_CLASS} classes) before the
 * programmatic {@code BrowserlessClassExtension} runs. The CDI
 * {@code BeanManagerProvider} is therefore already in place when
 * {@link #initVaadinEnvironment()} calls {@code MockVaadin.setup()}.
 */
@EnableAutoWeld
@SetBeanDiscoveryMode(BeanDiscoveryMode.ALL)
@AddBeanClasses({ GreetingService.class, GreetingView.class })
@AddPackages(CdiInstantiator.class)
@AddExtensions({ BeanManagerProvider.class, VaadinExtension.class })
@ViewPackages(classes = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WeldBrowserlessPerClassTest extends BrowserlessTest {

    // Static @RegisterExtension field, so its beforeAll/afterAll callbacks run
    // once for the whole class. The extension resolves the (PER_CLASS) test
    // instance from the ExtensionContext and drives its
    // BaseBrowserlessTest.initVaadinEnvironment()/cleanVaadinEnvironment().
    @RegisterExtension
    static final BrowserlessClassExtension vaadin = new BrowserlessClassExtension();

    @Inject
    GreetingService greetingService;

    @Produces
    @ApplicationScoped
    private final CdiVaadinServlet vaadinServlet = new CdiVaadinServlet();

    // Counts how many times the Vaadin environment is initialized; under the
    // per-class lifecycle this must stay at 1 for the whole class.
    private int setupCount;

    // The UI created at class setup. Captured once so each test method can
    // assert it is looking at the very same environment instance.
    private UI classUI;

    // No @BeforeEach here: BrowserlessClassExtension calls this once from its
    // beforeAll callback. Re-adding @BeforeEach would also run it per method.
    @Override
    protected void initVaadinEnvironment() {
        setupCount++;
        scanTesters();
        // Use the CDI servlet/service so the Vaadin Instantiator is the
        // CdiInstantiator backed by the running Weld container.
        MockVaadin.setup(MockedUI::new, vaadinServlet, lookupServices());
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(GreetingView.class);
        classUI = UI.getCurrent();
    }

    // No @AfterEach here either: cleanup runs once from afterAll.
    @Override
    protected void cleanVaadinEnvironment() {
        super.cleanVaadinEnvironment();
    }

    @Test
    void environmentSetUpOncePerClass() {
        Assertions.assertEquals(1, setupCount,
                "The Vaadin environment must be created exactly once for the "
                        + "whole class, not per test method");
        Assertions.assertNotNull(greetingService,
                "Weld should have injected the CDI bean before the test runs");
        // The live UI must be the exact instance created at class setup: if the
        // environment were rebuilt per method, this would be a different UI.
        Assertions.assertNotNull(classUI,
                "MockVaadin.setup() should have created a UI at class setup");
        Assertions.assertSame(classUI, UI.getCurrent(),
                "Each method must see the same UI created once for the class");
        Assertions.assertNotNull(
                BeanManagerProvider.getInstance().getBeanManager(),
                "BeanManagerProvider must be in place during Vaadin setup");
        Assertions.assertEquals("Hello from CDI", greetingService.greet());
    }

    @Test
    void navigationUsesCdiInstantiatorOnSharedEnvironment() {
        // Still exactly one setup, and still the same UI: this method reuses
        // the per-class environment rather than getting a fresh one.
        Assertions.assertEquals(1, setupCount,
                "The second test method must reuse the per-class environment");
        Assertions.assertSame(classUI, UI.getCurrent(),
                "The second method must see the same UI as the first");
        GreetingView view = navigate(GreetingView.class);
        Assertions.assertNotNull(view.getGreetingService(),
                "The view must be created by the CDI Instantiator with its "
                        + "dependencies injected");
        Assertions.assertEquals("Hello from CDI",
                view.getGreetingService().greet());
    }
}
