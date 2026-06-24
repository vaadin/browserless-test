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
package com.vaadin.flow.component.button;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
public class ButtonTesterAutomationTest extends BrowserlessTest {

    private ButtonView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(ButtonView.class);
        view = navigate(ButtonView.class);
    }

    @Test
    public void click_firesFromClientEvent_andIsHandled() {
        AtomicReference<ClickEvent<?>> seen = new AtomicReference<>();
        view.button.addClickListener(seen::set);

        test(ButtonTester.class, view.button).click();

        Assertions.assertNotNull(seen.get(),
                "click should be handled (A/B parity)");
        Assertions.assertTrue(seen.get().isFromClient(),
                "migrated click must keep user fidelity (fromClient=true)");
    }
}
