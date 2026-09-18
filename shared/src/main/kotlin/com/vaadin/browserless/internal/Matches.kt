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

/**
 * Checks whether this component matches given spec. All rules are matched except the [count] rule. The
 * rules are matched against given component only (not against its children).
 *
 * Stays Kotlin until [SearchSpec] is ported, since it takes a Kotlin DSL block.
 */
fun Component.matches(spec: SearchSpec<Component>.() -> Unit): Boolean =
    SearchSpec(Component::class.java).apply { spec() }.toPredicate().invoke(this)

/**
 * Size of the [IntRange], used by the `Grid._dump()` implementation.
 *
 * Stays Kotlin until `Grid.kt` is ported.
 */
val IntRange.size: Int get() = (endInclusive + 1 - start).coerceAtLeast(0)
