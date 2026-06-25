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
 * View that binds a request-scoped local signal, used to reproduce
 * <a href="https://github.com/vaadin/browserless-test/issues/110">#110</a> for
 * {@code @RequestScope}.
 */
@Route("repro-request")
public class RequestScopeView extends Div {

    private final ValueSignal<String> boundSignal;

    public RequestScopeView(RequestPrefs prefs) {
        // Resolve the request-scoped signal once. With per-user request-context
        // binding this is the active user's own request signal; without it the
        // second user reuses the first user's signal and the binding below
        // throws a cross-session IllegalStateException.
        this.boundSignal = prefs.color();
        getStyle().bind("background-color", boundSignal);
    }

    /**
     * The request-scoped {@link ValueSignal} resolved for this view's user.
     */
    public ValueSignal<String> boundSignal() {
        return boundSignal;
    }
}
