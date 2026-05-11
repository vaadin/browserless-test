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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.browserless.internal.MockRequestCustomizer;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.mocks.SpringSecurityRequestCustomizer;
import com.vaadin.flow.di.Lookup;

/**
 * Verifies that {@link SpringBrowserlessApplicationContext} bootstraps cleanly
 * when Spring Security is not on the test app's classpath. The detector is
 * overridden to simulate the absent classpath without actually changing the
 * Maven-resolved dependencies.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringSecurityAbsentTest.TestConfig.class)
class SpringSecurityAbsentTest {

    @Autowired
    private ApplicationContext applicationContext;

    @AfterEach
    void tearDown() {
        SpringSecuritySupport.overrideDetector(null);
        // The lookup initializer registers SpringSecurityRequestCustomizer
        // as a singleton in the shared Spring context whenever Spring
        // Security is present. Unregister it so each test starts with a
        // clean bean registry and the "without Spring Security" assertions
        // are not poisoned by an earlier "with Spring Security" test.
        if (applicationContext instanceof ConfigurableApplicationContext cac) {
            DefaultListableBeanFactory bf = (DefaultListableBeanFactory) cac
                    .getBeanFactory();
            String beanName = SpringSecurityRequestCustomizer.class.getName();
            if (bf.containsSingleton(beanName)) {
                bf.destroySingleton(beanName);
            }
        }
    }

    @Test
    void create_withoutSpringSecurity_doesNotInstallSecurityHandler() {
        SpringSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        try (var app = SpringBrowserlessApplicationContext.create(routes,
                applicationContext)) {
            Assertions.assertNull(app.getSecurityContextHandler(),
                    "SpringSecurityContextHandler must not be installed when"
                            + " Spring Security is absent from the classpath");
        }
    }

    @Test
    void create_withoutSpringSecurity_doesNotRegisterRequestCustomizerBean() {
        SpringSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        try (var app = SpringBrowserlessApplicationContext.create(routes,
                applicationContext)) {
            Assertions.assertFalse(
                    applicationContext.containsBean(
                            SpringSecurityRequestCustomizer.class.getName()),
                    "SpringSecurityRequestCustomizer bean must not be"
                            + " registered when Spring Security is absent");
        }
    }

    @Test
    void create_withoutSpringSecurity_doesNotInstallRequestCustomizerLookup() {
        SpringSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        try (var app = SpringBrowserlessApplicationContext.create(routes,
                applicationContext)) {
            Lookup lookup = app.getService().getContext()
                    .getAttribute(Lookup.class);
            MockRequestCustomizer customizer = lookup
                    .lookup(MockRequestCustomizer.class);
            Assertions.assertFalse(
                    customizer instanceof SpringSecurityRequestCustomizer,
                    "SpringSecurityRequestCustomizer must not be registered as"
                            + " a lookup service when Spring Security is"
                            + " absent");
        }
    }

    @Test
    void newUser_withoutSpringSecurity_doesNotThrow() {
        SpringSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        try (var app = SpringBrowserlessApplicationContext.create(routes,
                applicationContext)) {
            Assertions.assertDoesNotThrow(() -> {
                var user = app.newUser();
                user.newWindow();
            });
        }
    }

    @Test
    void createSecured_withSpringSecurity_installsHandlerAndCustomizer() {
        // Default detector: actual classpath probe (Spring Security IS present
        // in the spring module's test classpath)
        Routes routes = new Routes();
        try (var app = SpringBrowserlessApplicationContext.createSecured(routes,
                applicationContext)) {
            Assertions.assertInstanceOf(SpringSecurityContextHandler.class,
                    app.getSecurityContextHandler(),
                    "Handler must be installed when Spring Security is present");
            Assertions.assertTrue(
                    applicationContext.containsBean(
                            SpringSecurityRequestCustomizer.class.getName()),
                    "Customizer bean must be registered when Spring Security"
                            + " is present");
            Lookup lookup = app.getService().getContext()
                    .getAttribute(Lookup.class);
            Assertions.assertInstanceOf(SpringSecurityRequestCustomizer.class,
                    lookup.lookup(MockRequestCustomizer.class),
                    "Customizer must be registered as lookup service when"
                            + " Spring Security is present");
        }
    }

    @Test
    void createSecured_withoutSpringSecurity_throws() {
        SpringSecuritySupport.overrideDetector(() -> false);

        Routes routes = new Routes();
        var ex = Assertions.assertThrows(IllegalStateException.class,
                () -> SpringBrowserlessApplicationContext.createSecured(routes,
                        applicationContext),
                "createSecured(...) must reject calls when Spring Security is"
                        + " absent from the classpath");
        Assertions.assertTrue(ex.getMessage().contains("Spring Security"),
                "ISE message must mention Spring Security");
    }

    @Configuration
    static class TestConfig {
    }
}
