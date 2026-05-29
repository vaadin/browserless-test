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

import com.example.routerstate.RouterStateLayout;
import com.example.routerstate.RouterStateView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Reproduces <a href="https://github.com/vaadin/flow/issues/24471">
 * vaadin/flow#24471</a>: reading {@code RouterState.navigationTarget()} from
 * a layout constructor (e.g. via
 * {@code bindText(routerStateSignal().map(...))}) sees the initial
 * {@code RouterState} whose {@code navigationTarget} is {@code null}, because
 * the navigation pipeline constructs the layout chain <em>before</em>
 * {@code handleAfterNavigationEvents()} updates the signal.
 * <p>
 * The NPE is thrown from the {@code ElementEffect} "probe" run that
 * {@code bindText} kicks off in the unattached layout's constructor. Flow's
 * {@code Effect} wrapper catches exceptions from the action and forwards them
 * to {@link Thread#getUncaughtExceptionHandler()} rather than re-throwing,
 * so the navigation completes and the test thread is not interrupted — the
 * NPE is only visible as a stderr dump. This test installs a custom uncaught
 * exception handler to turn that stderr dump into an actual test failure.
 * <p>
 * The fix must land in Flow's navigation pipeline (the signal should be
 * updated earlier in the flow). Until then, this test fails. Once Flow
 * orders the signal update before chain construction, the probe will see a
 * valid {@code RouterState} and the assertion will pass.
 *
 * @see RouterStateLayout
 */
@ViewPackages(packages = "com.example.routerstate")
public class RouterStateSignalLayoutTest extends BrowserlessTest {

    @Test
    void routerStateSignal_readDuringLayoutConstructor_doesNotNPE() {
        List<Throwable> uncaught = new ArrayList<>();
        Thread current = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = current
                .getUncaughtExceptionHandler();
        current.setUncaughtExceptionHandler((t, e) -> uncaught.add(e));
        try {
            navigate(RouterStateView.class);
        } finally {
            current.setUncaughtExceptionHandler(previous);
        }

        Assertions.assertTrue(uncaught.isEmpty(),
                "No exceptions should be forwarded to the uncaught handler "
                        + "during navigation. Got: " + uncaught);
    }
}
