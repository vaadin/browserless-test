/**
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
@file:Suppress("FunctionName")

package com.vaadin.browserless.internal

import java.util.function.Consumer
import com.vaadin.flow.component.Component

/**
 * Kotlin DSL conveniences over the Java [Locator] engine. Preserves the
 * `_get<Reified> { id = "foo" }` syntax for Kotlin callers. These helpers
 * live in test scope only — production code calls [Locator] directly.
 */

/**
 * Checks whether this component matches given spec. All rules are matched except the [SearchSpec.count] rule. The
 * rules are matched against given component only (not against its children).
 */
public fun Component.matches(spec: SearchSpec<Component>.() -> Unit): Boolean =
    SearchSpec(Component::class.java).apply(spec).toPredicate().test(this)

// ---------------------------------------------------------------------------
// Inline reified + DSL block conveniences over the Java [Locator] engine.
// ---------------------------------------------------------------------------

public inline fun <reified T : Component> Component._get(noinline block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(this, T::class.java, Consumer { it.block() })

public inline fun <reified T : Component> _get(noinline block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(T::class.java, Consumer { it.block() })

public inline fun <reified T : Component> Component._find(noinline block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(this, T::class.java, Consumer { it.block() })

public inline fun <reified T : Component> _find(noinline block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(T::class.java, Consumer { it.block() })

public inline fun <reified T : Component> Component._expectNone(noinline block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectNone(this, T::class.java, Consumer { it.block() })
}

public inline fun <reified T : Component> _expectNone(noinline block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectNone(T::class.java, Consumer { it.block() })
}

public inline fun <reified T : Component> Component._expectOne(noinline block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectOne(this, T::class.java, Consumer { it.block() })
}

public inline fun <reified T : Component> _expectOne(noinline block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectOne(T::class.java, Consumer { it.block() })
}

public inline fun <reified T : Component> Component._expect(count: Int = 1, noinline block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expect(this, T::class.java, count, Consumer { it.block() })
}

public inline fun <reified T : Component> _expect(count: Int = 1, noinline block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expect(T::class.java, count, Consumer { it.block() })
}

// ---------------------------------------------------------------------------
// Non-reified Class-receiving extension overloads. These mirror the original
// Kotlin signatures so existing Kotlin callers like `button._get(Button::class.java)`
// keep compiling.
// ---------------------------------------------------------------------------

public fun <T : Component> Component._get(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(this, clazz, Consumer { it.block() })

public fun <T : Component> _get(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): T =
    Locator._get(clazz, Consumer { it.block() })

public fun <T : Component> Component._find(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(this, clazz, Consumer { it.block() })

public fun <T : Component> _find(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}): List<T> =
    Locator._find(clazz, Consumer { it.block() })

public fun <T : Component> Component._expectNone(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectNone(this, clazz, Consumer { it.block() })
}

public fun <T : Component> _expectNone(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectNone(clazz, Consumer { it.block() })
}

public fun <T : Component> Component._expectOne(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectOne(this, clazz, Consumer { it.block() })
}

public fun <T : Component> _expectOne(clazz: Class<T>, block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expectOne(clazz, Consumer { it.block() })
}

public fun <T : Component> Component._expect(clazz: Class<T>, count: Int = 1, block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expect(this, clazz, count, Consumer { it.block() })
}

public fun <T : Component> _expect(clazz: Class<T>, count: Int = 1, block: SearchSpec<T>.() -> Unit = {}) {
    Locator._expect(clazz, count, Consumer { it.block() })
}

/**
 * Re-exports a few non-reified helpers under their familiar names so that
 * Kotlin callers don't need to import `Locator`.
 */
public fun _expectNoDialogs(): Unit = Locator._expectNoDialogs()

public fun _expectInternalServerError(expectedErrorMessage: String = ""): Unit =
    Locator._expectInternalServerError(expectedErrorMessage)

public val currentPath: String? get() = Locator.currentPath()

public fun Component._walkAll(): Iterable<Component> = Locator._walkAll(this)
