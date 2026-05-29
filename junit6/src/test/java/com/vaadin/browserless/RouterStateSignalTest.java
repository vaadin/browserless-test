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

import com.example.base.HelloWorldView;
import com.example.base.WelcomeView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.signals.Signal;

/**
 * Verifies that {@link UI#routerStateSignal()} is wired up correctly in
 * browserless tests. The signal is owned by Flow's navigation pipeline, so
 * these tests mainly guard against regressions in the way MockVaadin drives
 * navigation through {@code UI.navigate}.
 */
@ViewPackages(packages = "com.example.base")
public class RouterStateSignalTest extends BrowserlessTest {

    @Test
    void routerStateSignal_afterDefaultNavigation_hasNavigationTarget() {
        // BaseBrowserlessTest navigates to the default route during setup,
        // so the signal should already reflect that navigation.
        RouterState state = UI.getCurrent().routerStateSignal().peek();
        Assertions.assertNotNull(state, "RouterState should not be null");
        Assertions.assertEquals(WelcomeView.class, state.navigationTarget(),
                "navigationTarget should be set after default navigation");
    }

    @Test
    void routerStateSignal_afterExplicitNavigation_hasNavigationTarget() {
        navigate(HelloWorldView.class);

        RouterState state = UI.getCurrent().routerStateSignal().peek();
        Assertions.assertNotNull(state, "RouterState should not be null");
        Assertions.assertEquals(HelloWorldView.class, state.navigationTarget(),
                "navigationTarget should reflect the latest navigation");
    }

    @Test
    void routerStateSignal_effectFromView_seesUpdatedStateOnAttach() {
        // Read the signal reactively from an effect owned by the
        // already-navigated view to make sure observers see the updated
        // state. (At this point the navigation pipeline has finished, so
        // handleAfterNavigationEvents() has already updated the signal.)
        WelcomeView welcome = navigate(WelcomeView.class);
        List<Class<? extends Component>> observed = new ArrayList<>();
        Signal.effect(welcome, () -> observed.add(
                UI.getCurrent().routerStateSignal().get().navigationTarget()));

        Assertions.assertEquals(1, observed.size(),
                "Effect should run once on registration");
        Assertions.assertEquals(WelcomeView.class, observed.get(0));
    }
}
