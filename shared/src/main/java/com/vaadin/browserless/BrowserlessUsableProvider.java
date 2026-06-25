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

import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.automation.Capability;
import com.vaadin.flow.automation.CapabilityProvider;
import com.vaadin.flow.automation.Usable;
import com.vaadin.flow.component.Component;

/**
 * Supplies browserless's {@link BrowserlessUsable} for every component,
 * overriding the commons {@code DefaultUsable} fallback. Registered
 * programmatically on {@link BrowserlessAutomation}'s registry (not via
 * {@code ServiceLoader}) so the fidelity rules stay scoped to browserless.
 * Priority 0 — a future component-specific {@code Usable} provider (priority
 * 100) still wins.
 */
final class BrowserlessUsableProvider implements CapabilityProvider {

    @Override
    public boolean supports(Component component) {
        return true;
    }

    @Override
    public Set<Class<? extends Capability>> capabilities(Component component) {
        return Set.of(Usable.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Capability> Optional<C> resolve(Component component,
            Class<C> type) {
        return type == Usable.class
                ? Optional.of((C) new BrowserlessUsable(component))
                : Optional.empty();
    }

    @Override
    public int priority() {
        return 0;
    }
}
