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
package com.testapp.pagetitle;

import org.springframework.stereotype.Component;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * An application-wide {@link PageTitleGenerator} registered only as a Spring
 * bean, the way an application gets one without annotating every route with
 * {@code @DynamicPageTitle}.
 */
@Component
public class OrderTitleGenerator implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        String orderId = context.routeParameters().get("orderId").orElse("");
        String value = context.value().isEmpty()
                ? context.navigationTarget().getSimpleName()
                : context.value();
        return orderId.isEmpty() ? "generated:" + value
                : "generated:" + value + ":" + orderId;
    }
}
