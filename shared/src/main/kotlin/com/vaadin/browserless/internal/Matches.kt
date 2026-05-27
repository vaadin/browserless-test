/**
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal

import com.vaadin.flow.component.Component

/**
 * Checks whether this component matches given spec. All rules are matched except the [count] rule. The
 * rules are matched against given component only (not against its children).
 */
fun Component.matches(spec: SearchSpec<Component>.() -> Unit): Boolean =
    SearchSpec(Component::class.java).apply { spec() }.toPredicate().invoke(this)

/**
 * Size of the [IntRange], used by the `Grid._dump()` implementation.
 */
val IntRange.size: Int get() = (endInclusive + 1 - start).coerceAtLeast(0)
