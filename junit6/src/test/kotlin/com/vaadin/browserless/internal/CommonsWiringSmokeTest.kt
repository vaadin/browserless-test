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

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.snapshot.Snapshot
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/** Confirms flow-automation-commons resolves on the test classpath and Snapshot.walk runs. */
class CommonsWiringSmokeTest {

    @Test
    fun snapshot_walk_resolves_and_visits_the_root() {
        val root = Div()
        val visited = mutableListOf<String>()
        Snapshot.walk(root) { component, _, _ ->
            visited.add(component.javaClass.simpleName)
            true
        }
        assertTrue("Div" in visited, "Snapshot.walk should visit the Div root; visited=$visited")
    }
}
