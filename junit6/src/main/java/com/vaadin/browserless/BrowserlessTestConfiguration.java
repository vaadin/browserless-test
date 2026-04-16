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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import com.vaadin.browserless.internal.Routes;

/**
 * Spring {@link Configuration} that provides a
 * {@link VaadinTestApplicationContext} bean for multi-user browserless testing.
 *
 * <p>
 * The bean respects {@link ViewPackages} on the test class to limit route
 * scanning. Without it, routes are auto-discovered from the full classpath.
 */
@Configuration
public class BrowserlessTestConfiguration implements TestExecutionListener {

    private static final ThreadLocal<Class<?>> currentTestClass = new ThreadLocal<>();

    @Bean
    @Lazy
    VaadinTestApplicationContext vaadinTestApplicationContext(
            ApplicationContext springCtx) {
        Class<?> testClass = currentTestClass.get();
        Routes routes;
        if (testClass != null) {
            routes = VaadinTestApplicationContext.discoverRoutes(testClass,
                    Set.of());
        } else {
            routes = VaadinTestApplicationContext.discoverRoutes(Set.of());
        }
        return VaadinTestApplicationContext.forSpring(routes, springCtx);
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        currentTestClass.set(testContext.getTestClass());
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        currentTestClass.remove();
        ApplicationContext ctx = testContext.getApplicationContext();
        ObjectProvider<VaadinTestApplicationContext> provider = ctx
                .getBeanProvider(VaadinTestApplicationContext.class);
        VaadinTestApplicationContext appCtx = provider.getIfAvailable();
        if (appCtx != null) {
            appCtx.close();
        }
    }
}
