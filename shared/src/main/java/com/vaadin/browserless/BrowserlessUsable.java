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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.automation.CapabilityDescriptor;
import com.vaadin.flow.automation.UsabilityReason;
import com.vaadin.flow.automation.Usable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;

/**
 * Browserless's full-fidelity {@link Usable}: ports the rules that used to live
 * in {@link ComponentTester#isUsable()} /
 * {@link ComponentTester#notUsableReasons}. Adds three rules the commons
 * {@code DefaultUsable} lacks — effective (subtree) visibility, element-level
 * enablement (ancestor-disabled propagation), and the inert/modality-curtain
 * check — plus read-only as a not-usable reason. Resolved for every component
 * via {@link BrowserlessUsableProvider}.
 */
final class BrowserlessUsable implements Usable {

    private final Component component;

    BrowserlessUsable(Component component) {
        this.component = component;
    }

    private static boolean isEffectivelyVisible(Component c) {
        return c.isVisible() && (c.getParent().isEmpty()
                || isEffectivelyVisible(c.getParent().get()));
    }

    @Override
    public boolean visible() {
        return isEffectivelyVisible(component);
    }

    @Override
    public boolean enabled() {
        return component.getElement().isEnabled();
    }

    @Override
    public boolean attached() {
        return component.isAttached();
    }

    @Override
    public boolean editable() {
        return !(component instanceof HasValue<?, ?> hv) || !hv.isReadOnly();
    }

    private boolean inert() {
        return component.getElement().getNode().isInert();
    }

    @Override
    public boolean isUsable() {
        return enabled() && attached() && visible() && editable() && !inert();
    }

    @Override
    public List<UsabilityReason> reasons() {
        List<UsabilityReason> r = new ArrayList<>();
        if (!component.getElement().isEnabled()) {
            r.add(UsabilityReason.of(UsabilityReason.DISABLED, "not enabled"));
        }
        if (!component.isAttached()) {
            r.add(UsabilityReason.of(UsabilityReason.DETACHED, "not attached"));
        }
        if (!component.isVisible()) {
            r.add(UsabilityReason.of(UsabilityReason.NOT_VISIBLE,
                    "not visible"));
        } else if (!isEffectivelyVisible(component)) {
            r.add(UsabilityReason.of(UsabilityReason.NOT_VISIBLE,
                    "part of a not visible subtree"));
        }
        if (inert()) {
            r.add(UsabilityReason.of("inert", "behind a modality curtain"));
        }
        if (component instanceof HasValue<?, ?> hv && hv.isReadOnly()) {
            r.add(UsabilityReason.of(UsabilityReason.READ_ONLY, "read only"));
        }
        return r;
    }

    @Override
    public Usable usability() {
        return this;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return CapabilityDescriptor.of(Usable.class);
    }
}
