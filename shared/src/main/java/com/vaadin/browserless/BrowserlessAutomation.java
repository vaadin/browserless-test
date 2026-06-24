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

import com.vaadin.flow.automation.Automation;
import com.vaadin.flow.automation.CapabilityRegistry;
import com.vaadin.flow.automation.InterceptScope;
import com.vaadin.flow.component.Component;

/**
 * Holds the single browserless-owned {@link CapabilityRegistry} — NOT
 * {@code global()} — carrying the {@link RoundTripInterceptor}, so the
 * round-trip lifecycle stays scoped to browserless. The registry's
 * {@code ServiceLoader} discovers the flow-automation-components providers;
 * browserless registers no component knowledge of its own.
 */
final class BrowserlessAutomation {

    private static final CapabilityRegistry REGISTRY = new CapabilityRegistry()
            .intercept(InterceptScope.global(), new RoundTripInterceptor());

    private BrowserlessAutomation() {
    }

    /**
     * The shared immutable tool context rooted at the target's UI (or the
     * target itself if detached — driving via {@code of(...)} does not use the
     * root). Reuses the one interceptor- bearing registry.
     */
    static Automation forDriving(Component target) {
        Component root = target.getUI().map(ui -> (Component) ui)
                .orElse(target);
        return Automation.in(root).using(REGISTRY);
    }
}
