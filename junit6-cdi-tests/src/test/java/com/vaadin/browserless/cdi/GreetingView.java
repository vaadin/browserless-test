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

import jakarta.inject.Inject;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * A view instantiated by the CDI {@code Instantiator}, with a CDI bean injected
 * into it. Navigating to this view exercises the full
 * {@code BeanManagerProvider} chain that regressed in 1.1.0.
 */
@Route("greeting")
public class GreetingView extends VerticalLayout {

    private final transient GreetingService greetingService;

    @Inject
    public GreetingView(GreetingService greetingService) {
        this.greetingService = greetingService;
        add(new Span(greetingService.greet()));
    }

    public GreetingService getGreetingService() {
        return greetingService;
    }
}
