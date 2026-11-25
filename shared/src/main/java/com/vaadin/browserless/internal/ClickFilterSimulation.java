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
package com.vaadin.browserless.internal;

import com.vaadin.flow.component.ClickNotifier.ClickFiltersJs;
import com.vaadin.flow.dom.DomEventFilter;

/**
 * Simulates the click filters of {@code ClickNotifier}, used by its single
 * click and double click listeners.
 * <p>
 * For internal use only.
 */
final class ClickFilterSimulation extends DeclaredFunctionSimulation
        implements ClickFiltersJs {

    ClickFilterSimulation(ClientDomEvent event) {
        super(event);
    }

    @Override
    public DomEventFilter isClickCountBetween(int min, int max) {
        // event.detail >= min && event.detail <= max, where a comparison with
        // undefined is false
        return answer(event().property("detail") instanceof Double detail
                && detail >= min && detail <= max);
    }
}
