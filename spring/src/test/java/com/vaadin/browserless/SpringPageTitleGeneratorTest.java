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

import java.util.Map;

import com.testapp.pagetitle.OrderTitleGenerator;
import com.testapp.pagetitle.OrderView;
import com.testapp.pagetitle.PlainOrderView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * An application-wide {@link PageTitleGenerator} registered only as a Spring
 * bean must be applied in a browserless test the same way it is in a running
 * application, so that a test can assert the title an application computes.
 */
@ContextConfiguration(classes = SpringPageTitleGeneratorTest.TestConfig.class)
@ViewPackages(packages = "com.testapp.pagetitle")
class SpringPageTitleGeneratorTest extends SpringBrowserlessTest {

    @Test
    void pageTitleGeneratorBean_resolvesTitleOfViewWithPageTitleValue() {
        navigate(OrderView.class, Map.of("orderId", "ORD-1"));

        Assertions.assertEquals("generated:order.title:ORD-1", title(),
                "The PageTitleGenerator bean should have computed the title from the @PageTitle value and route parameters");
    }

    @Test
    void pageTitleGeneratorBean_resolvesTitleOfViewWithoutPageTitle() {
        navigate(PlainOrderView.class);

        Assertions.assertEquals("generated:PlainOrderView", title(),
                "The PageTitleGenerator bean should have computed the title of a view that declares none");
    }

    private static String title() {
        return UI.getCurrent().getInternals().getTitle();
    }

    @Configuration
    @ComponentScan(basePackageClasses = OrderTitleGenerator.class)
    static class TestConfig {
        // Registers the test views, which are not registered at startup so
        // that they only exist for this test
        @Bean
        VaadinServiceInitListener registerTitleViews() {
            return event -> {
                RouteConfiguration routeConfiguration = RouteConfiguration
                        .forApplicationScope();
                routeConfiguration.setAnnotatedRoute(OrderView.class);
                routeConfiguration.setAnnotatedRoute(PlainOrderView.class);
            };
        }
    }
}
