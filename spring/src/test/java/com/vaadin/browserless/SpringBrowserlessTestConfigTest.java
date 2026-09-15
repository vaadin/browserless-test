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

import java.security.Principal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

/**
 * Verifies that {@link BrowserlessTestConfig} is applied to the mock Vaadin
 * Spring environment.
 */
@ContextConfiguration(classes = SpringBrowserlessTestConfigTest.TestConfig.class)
@BrowserlessTestConfig(applicationProperties = "class.property=fromClass", featureFlags = "collaborationEngineBackend")
class SpringBrowserlessTestConfigTest extends SpringBrowserlessTest {

    @Test
    void classLevelConfiguration_isApplied() {
        Assertions.assertEquals("fromClass", property("class.property"));
        Assertions.assertTrue(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND));
    }

    @Test
    @BrowserlessTestConfig(applicationProperties = "method.property=fromMethod", featureFlags = "collaborationEngineBackend=false")
    void methodLevelConfiguration_isApplied() {
        Assertions.assertEquals("fromMethod", property("method.property"));
        Assertions.assertEquals("fromClass", property("class.property"));
        Assertions.assertFalse(
                FeatureFlags.get(VaadinService.getCurrent().getContext())
                        .isEnabled(FeatureFlags.COLLABORATION_ENGINE_BACKEND));
    }

    @Test
    @WithMockUser(username = "john", roles = "DEV")
    @BrowserlessTestConfig(lookupServices = TestNoOpInstantiatorFactory.class)
    void testDeclaredLookupServices_doNotReplaceFrameworkOnes() {
        Assertions.assertNotNull(VaadinService.getCurrent().getContext()
                .getAttribute(Lookup.class), "Expecting Lookup to be set up");
        // SpringSecurityRequestCustomizer is required by the Spring
        // integration: the principal is only available on the request if it is
        // still registered alongside the one declared by the test.
        Principal principal = VaadinRequest.getCurrent().getUserPrincipal();
        Assertions.assertNotNull(principal,
                "Lookup services declared by the test must not replace the Spring ones");
        Assertions.assertEquals("john", principal.getName());
    }

    private static String property(String name) {
        return VaadinService.getCurrent().getDeploymentConfiguration()
                .getStringProperty(name, null);
    }

    /**
     * An {@link InstantiatorFactory} that does not provide any instantiator, so
     * that registering it does not conflict with the Spring one.
     */
    static class TestNoOpInstantiatorFactory implements InstantiatorFactory {
        @Override
        public Instantiator createInstantitor(VaadinService vaadinService) {
            return null;
        }
    }

    // Empty configuration class used only to be able to bootstrap spring
    // ApplicationContext
    @Configuration
    static class TestConfig {
    }
}
