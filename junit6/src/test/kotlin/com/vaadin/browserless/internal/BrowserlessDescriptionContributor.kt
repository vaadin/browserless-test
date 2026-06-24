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
package com.vaadin.browserless.internal

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.snapshot.DescriptionContributor
import java.util.function.Consumer

/** Spike: browserless-specific per-node detail (absent from commons NodeFacts) via the SPI. */
class BrowserlessDescriptionContributor : DescriptionContributor {

    override fun supports(component: Component): Boolean =
        component is HasValue<*, *> || component is HasValidation

    override fun contribute(component: Component, sink: Consumer<String>) {
        @Suppress("UNCHECKED_CAST")
        if (component is HasValue<*, *> && (component as HasValue<HasValue.ValueChangeEvent<Any?>, Any?>).isReadOnly) {
            sink.accept("RO")
        }
        if (component is HasValidation && component.isInvalid) {
            sink.accept("INVALID")
        }
    }
}
