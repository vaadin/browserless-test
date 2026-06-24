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
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
public class RoundTripInterceptorTest extends BrowserlessTest {

    private ButtonView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(ButtonView.class);
        view = navigate(ButtonView.class);
    }

    @Test
    public void driving_resolves_invokes_and_round_trips() {
        AtomicInteger clicks = new AtomicInteger();
        view.button.addClickListener(e -> clicks.incrementAndGet());

        // Drive through the browserless Automation: resolves Activatable via
        // the provider SPI,
        // invokes it, and the interceptor runs BrowserlessDSL.roundTrip
        // afterwards — the test
        // never calls roundTrip() itself. (clicks==1 proves proceed() ran; the
        // round-trip ran
        // without throwing. A flush-only-observable assertion is covered by the
        // parity tests.)
        BrowserlessAutomationTestSupport.driving(view.button).of(view.button)
                .as(Activatable.class).activate();

        Assertions.assertEquals(1, clicks.get(),
                "activate() should fire the click through the provider");
    }
}
