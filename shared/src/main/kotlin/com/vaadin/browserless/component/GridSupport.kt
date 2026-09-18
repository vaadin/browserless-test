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
package com.vaadin.browserless.component

/**
 * The last Kotlin-only helpers `Grid.kt` still needs. Both go away with it in
 * the final phase of the port.
 */

/**
 * Size of the [IntRange], used by the `Grid._dump()` implementation.
 */
internal val IntRange.size: Int get() = (endInclusive + 1 - start).coerceAtLeast(0)

/**
 * Removes nulls and blank strings from this iterable.
 */
internal fun Iterable<String?>.filterNotBlank(): List<String> =
    filterNotNull().filter { it.isNotBlank() }
