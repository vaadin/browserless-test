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
package com.vaadin.browserless;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.mocks.MockSpringServlet;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.browserless.mocks.SpringSecurityRequestCustomizer;

/**
 * Base JUnit 6 class for browserless testing of applications based on Spring
 * Framework.
 *
 * This class provides functionality of the Spring TestContext Framework, in
 * addition to set up a mock Vaadin Spring environment, so that views and
 * components built upon dependency injection and AOP can be correctly be
 * handled during unit testing.
 *
 * Usually when unit testing a UI view it is not needed to bootstrap the whole
 * application. Subclasses can therefore be annotated
 * with @{@link org.springframework.test.context.ContextConfiguration} or other
 * Spring Testing annotations to load only required component or to provide mock
 * services implementations.
 *
 * <pre>
 * {@code
 * &#64;ContextConfiguration(classes = ViewTestConfig.class)
 * class ViewTest extends SpringBrowserlessTest {
 *
 * }
 * 
 * &#64;Configuration
 * class ViewTestConfig {
 *     &#64;Bean
 *     MyService myService() {
 *         return new MockMyService();
 *     }
 * }
 * }
 * </pre>
 */
@ExtendWith({ SpringExtension.class })
@TestExecutionListeners(listeners = BrowserlessTestSpringLookupInitializer.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public abstract class SpringBrowserlessTest extends BaseBrowserlessTest
        implements TesterWrappers {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected final String testingEngine() {
        return "JUnit 6";
    }

    @Override
    protected Set<Class<?>> lookupServices() {
        return Set.of(BrowserlessTestSpringLookupInitializer.class,
                SpringSecurityRequestCustomizer.class);
    }

    /**
     * Sets up the mock Vaadin Spring environment before each test. Runs as a
     * {@code @BeforeEach} method so that it fires <em>after</em> all JUnit 5
     * extension {@code beforeEach} callbacks — in particular after
     * {@code SpringExtension.beforeEach()}, which populates the Spring Security
     * context for annotations such as {@code @WithMockUser}.
     */
    @BeforeEach
    @Override
    protected void initVaadinEnvironment() {
        scanTesters();
        MockSpringServlet servlet = new MockSpringServlet(discoverRoutes(),
                applicationContext, MockedUI::new);
        MockVaadin.setup(MockedUI::new, servlet, lookupServices());
    }

    @AfterEach
    @Override
    protected void cleanVaadinEnvironment() {
        super.cleanVaadinEnvironment();
    }
}
