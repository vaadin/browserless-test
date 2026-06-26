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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * Regression test for
 * <a href="https://github.com/vaadin/browserless-test/issues/112">#112</a>.
 *
 * <p>
 * Combines {@link BrowserlessTest} with a CDI container managed by
 * weld-junit5's {@code @EnableAutoWeld}. Weld starts the container from a
 * subclass-registered extension; the Vaadin environment must therefore be set
 * up <em>after</em> it, so that {@code MockVaadin.setup()} can resolve the CDI
 * {@code BeanManagerProvider}.
 *
 * <p>
 * In 1.1.0 the setup ran from a {@code BeforeEachCallback} inherited from the
 * {@code BrowserlessTest} superclass, which JUnit always boots before the
 * subclass's Weld extension, so every test failed with
 * {@code IllegalStateException: No com.vaadin.cdi.util.BeanManagerProvider in
 * place!}. The fix moves per-method setup back to an instance
 * {@code @BeforeEach} method; re-declaring {@code @BeforeEach} on the
 * {@link #initVaadinEnvironment()} override below registers the hook at this
 * concrete test class level, so it runs after Weld has started the container.
 */
@EnableAutoWeld
@SetBeanDiscoveryMode(BeanDiscoveryMode.ALL)
@AddBeanClasses({ GreetingService.class, GreetingView.class })
@AddPackages(CdiInstantiator.class)
@AddExtensions({ BeanManagerProvider.class, VaadinExtension.class })
@ViewPackages(classes = {})
class WeldBrowserlessRegressionTest extends BrowserlessTest {

    @Inject
    GreetingService greetingService;

    @Produces
    @ApplicationScoped
    private final CdiVaadinServlet vaadinServlet = new CdiVaadinServlet();

    // @BeforeEach is re-added on the override so this hook is registered at
    // the concrete test class level — i.e. after weld-junit5 has started the
    // CDI container. See BrowserlessTest's class documentation.
    @BeforeEach
    @Override
    protected void initVaadinEnvironment() {
        scanTesters();
        // Use the CDI servlet/service so the Vaadin Instantiator is the
        // CdiInstantiator backed by the running Weld container.
        MockVaadin.setup(MockedUI::new, vaadinServlet, lookupServices());
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(GreetingView.class);
    }

    @Test
    void cdiContainerReadyBeforeVaadinSetup() {
        Assertions.assertNotNull(greetingService,
                "Weld should have injected the CDI bean before the test runs");
        Assertions.assertNotNull(UI.getCurrent(),
                "MockVaadin.setup() should have created a UI");
        Assertions.assertNotNull(
                BeanManagerProvider.getInstance().getBeanManager(),
                "BeanManagerProvider must be in place during Vaadin setup "
                        + "(this is what regressed in 1.1.0)");
        Assertions.assertEquals("Hello from CDI", greetingService.greet());
    }

    @Test
    void navigationUsesCdiInstantiator() {
        GreetingView view = navigate(GreetingView.class);
        Assertions.assertNotNull(view.getGreetingService(),
                "The view must be created by the CDI Instantiator with its "
                        + "dependencies injected");
        Assertions.assertEquals("Hello from CDI",
                view.getGreetingService().greet());
    }
}
