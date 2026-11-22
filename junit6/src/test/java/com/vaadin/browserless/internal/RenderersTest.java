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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderersTest {

    @Test
    void getPresentationValue_declaredTemplate_rendersTemplateText() {
        // The hierarchy column renders with a template declared by a
        // @JsDefinition method rather than with a template string
        Renderer<String> renderer = new TreeGrid<String>()
                .addHierarchyColumn(item -> item.toUpperCase()).getRenderer();

        assertEquals("ROOT A",
                Renderers._getPresentationValue(renderer, "Root A"));
    }

    @Test
    void getPresentationValue_templateString_rendersTemplateText() {
        Renderer<String> renderer = LitRenderer
                .<String> of("<span>Name: ${item.name}</span>")
                .withProperty("name", item -> item);

        assertEquals("Name: Root A",
                Renderers._getPresentationValue(renderer, "Root A"));
    }
}
