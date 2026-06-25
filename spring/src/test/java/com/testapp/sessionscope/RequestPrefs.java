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

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A Spring request-scoped bean holding a per-request local signal, used to
 * reproduce
 * <a href="https://github.com/vaadin/browserless-test/issues/110">#110</a> for
 * {@code @RequestScope}.
 */
@Component
@RequestScope
public class RequestPrefs {
    private final ValueSignal<String> color = new ValueSignal<>("white");

    public ValueSignal<String> color() {
        return color;
    }
}
