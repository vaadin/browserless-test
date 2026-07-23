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
package com.vaadin.flow.component.checkbox;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinService;

@ViewPackages
class SwitchTesterTest extends BrowserlessTest {

    SwitchView view;

    @BeforeEach
    public void registerView() {
        // Switch is experimental and throws on attach unless the feature flag
        // is enabled, so enable it before navigating to the view.
        FeatureFlags.get(VaadinService.getCurrent().getContext()).setEnabled(
                SwitchFeatureFlagProvider.SWITCH_COMPONENT.getId(), true);

        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(SwitchView.class);
        view = navigate(SwitchView.class);
    }

    @Test
    void readOnlySwitch_isNotUsable() {
        view.field.setReadOnly(true);
        Assertions.assertFalse(test(view.field).isUsable(),
                "Readonly switch should not be usable");
    }

    @Test
    void click_usable_valueChanges() {
        Assertions.assertFalse(view.field.getValue(),
                "Expecting switch initial state not to be on");

        test(view.field).click();
        Assertions.assertTrue(view.field.getValue(),
                "Expecting switch to be on, but was not");

        test(view.field).click();
        Assertions.assertFalse(view.field.getValue(),
                "Expecting switch not to be on, but was");
    }

    @Test
    void click_usable_checkedChangeFired() {
        AtomicBoolean checkedChange = new AtomicBoolean();
        view.field.getElement().addPropertyChangeListener("checked",
                ev -> checkedChange.set(true));

        Assertions.assertFalse(view.field.getValue(),
                "Expecting switch not to be on, but was");

        test(view.field).click();
        Assertions.assertTrue(checkedChange.get(),
                "Expected checked change event to be fired, but was not");
        Assertions.assertTrue(view.field.getValue(),
                "Expecting switch to be on, but was not");
    }

    @Test
    void click_disabled_throws() {
        view.field.setEnabled(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.field)::click);
    }

    @Test
    void click_invisible_throws() {
        view.field.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.field)::click);
    }

    @Test
    void click_readOnly_throws() {
        view.field.setReadOnly(true);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.field)::click);
    }

}
