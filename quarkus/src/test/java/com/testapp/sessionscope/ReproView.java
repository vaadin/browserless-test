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
package com.testapp.sessionscope;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * View that binds a session-scoped local signal, used to guard
 * <a href="https://github.com/vaadin/browserless-test/issues/110">#110</a> for
 * Quarkus.
 */
@Route("repro")
public class ReproView extends Div {

    private final ValueSignal<String> boundSignal;

    public ReproView(Prefs prefs) {
        // Resolve the session-scoped signal once. Because @VaadinSessionScoped
        // resolves against the active user's VaadinSession, this is that user's
        // own signal; a regression that leaked one user's session-scoped bean
        // to another would surface here as a cross-session
        // IllegalStateException on the binding below.
        this.boundSignal = prefs.color();
        getStyle().bind("background-color", boundSignal);
    }

    /**
     * The session-scoped {@link ValueSignal} resolved for this view's user.
     */
    public ValueSignal<String> boundSignal() {
        return boundSignal;
    }
}
