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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessAutomationTestSupport;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.automation.Activatable;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxTester;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
public class AutomationCrossCuttingTest extends BrowserlessTest {

    private ButtonView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(ButtonView.class);
        view = navigate(ButtonView.class);
    }

    @Test
    public void resolution_is_through_the_provider_spi_not_a_cast() {
        // no instanceof / cast of a concrete component or tester: capability
        // comes from the registry
        Assertions.assertTrue(
                BrowserlessAutomationTestSupport.driving(view.button)
                        .of(view.button).has(Activatable.class),
                "Activatable must resolve via the provider SPI");
    }

    @Test
    public void interceptor_supplies_round_trip_without_explicit_call() {
        AtomicInteger clicks = new AtomicInteger();
        view.button.addClickListener(e -> clicks.incrementAndGet());

        // act through the migrated tester; the test never calls roundTrip()
        test(ButtonTester.class, view.button).click();

        Assertions.assertEquals(1, clicks.get(),
                "interceptor must have flushed without the tester/test calling roundTrip()");
    }

    @Test
    public void unmigrated_tester_still_works() {
        Checkbox checkbox = new Checkbox();
        view.add(checkbox);
        CheckboxTester tester = test(CheckboxTester.class, checkbox);
        tester.click();
        Assertions.assertTrue(checkbox.getValue(),
                "legacy CheckboxTester path unchanged and still functional");
    }
}
