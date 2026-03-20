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
package com.vaadin.browserless;

import java.util.List;

import com.example.autolayout.AutoLayout;
import com.example.autolayout.AutoLayoutView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;

@ViewPackages(classes = AutoLayoutView.class)
public class AutoLayoutTest extends BrowserlessTest {

    @Test
    void navigate_viewWithAutoLayout_layoutIsApplied() {
        navigate(AutoLayoutView.class);

        List<HasElement> chain = UI.getCurrent().getInternals()
                .getActiveRouterTargetsChain();

        Assertions.assertEquals(2, chain.size(),
                "Router targets chain should contain both the view and its @Layout, but got: "
                        + chain.stream().map(e -> e.getClass().getSimpleName())
                                .toList());
        Assertions.assertInstanceOf(AutoLayoutView.class, chain.get(0),
                "First element in chain should be the view");
        Assertions.assertInstanceOf(AutoLayout.class, chain.get(1),
                "Second element in chain should be the @Layout");
    }

    @Test
    void navigate_viewWithAutoLayout_layoutIsAttachedToUI() {
        navigate(AutoLayoutView.class);

        List<HasElement> chain = UI.getCurrent().getInternals()
                .getActiveRouterTargetsChain();

        // The layout (outermost element) must be attached to the UI tree
        HasElement outermost = chain.get(chain.size() - 1);
        Assertions.assertInstanceOf(AutoLayout.class, outermost);
        Assertions.assertTrue(outermost.getElement().getNode().isAttached(),
                "The @Layout should be attached to the UI tree");
    }

    @Test
    void navigate_viewWithAutoLayout_canFindChildComponentInLayout() {
        navigate(AutoLayoutView.class);

        Span span = $(Span.class).first();

        Assertions.assertNotNull(span,
                "Span added by @Layout should be findable via $()");
        Assertions.assertEquals("Layout Header", span.getText());
    }
}
