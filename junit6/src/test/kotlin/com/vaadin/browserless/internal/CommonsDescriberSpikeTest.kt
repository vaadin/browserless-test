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
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CommonsDescriberSpikeTest {

    @BeforeEach fun setup() { MockVaadin.setup() }
    @AfterEach fun tearDown() { MockVaadin.tearDown() }

    @Test
    fun commons_backed_dump_renders_box_tree_with_injected_detail() {
        val field = TextField().apply { setId("name"); isReadOnly = true }
        val root = VerticalLayout(Div(), field)

        val commons = root.toPrettyTreeViaCommons()
        val legacy = root.toPrettyTree() // for side-by-side comparison / diff notes

        // Box structure from the shared renderer:
        assertTrue(commons.contains("└──") || commons.contains("├──"), "box-drawing expected:\n$commons")
        // Generic facts from NodeFacts:
        assertTrue(commons.contains("VerticalLayout"), commons)
        assertTrue(commons.contains("TextField"), commons)
        assertTrue(commons.contains("#name"), commons)
        // Browserless-specific detail injected via the DescriptionContributor SPI:
        assertTrue(commons.contains("RO"), "contributor RO detail expected:\n$commons")

        // Spike finding: legacy and commons dumps differ (traversal hook, selectability,
        // detail coverage). This is documentation, not a failure — see the report.
        println("LEGACY:\n$legacy\n\nCOMMONS:\n$commons")
    }
}
