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

import com.example.base.ParentView;
import com.example.base.child.ChildView;
import com.example.layout.AutoLayoutView;
import com.example.layout.AutoMainLayout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;

@ViewPackages(packages = { "com.example.layout", "com.example.base" })
public class BrowserlessLayoutNavigationTest extends BrowserlessTest {

    @Test
    public void navigate_withExplicitLayout_layoutIsCreated() {
        navigate(ChildView.class);
        ParentView layout = getRouterLayout(ParentView.class);
        Assertions.assertNotNull(layout);
        Assertions.assertNotNull(layout.getElement().getNode().getParent());
    }

    @Test
    public void navigate_withAutoLayout_layoutIsCreated() {
        navigate(AutoLayoutView.class);
        AutoMainLayout layout = getRouterLayout(AutoMainLayout.class);
        Assertions.assertNotNull(layout);
        Assertions.assertNotNull(layout.getElement().getNode().getParent());
    }

    @Test
    public void navigate_withAutoLayout_componentsOnLayoutAccessible() {
        navigate(AutoLayoutView.class);
        Button layoutButton = $(Button.class).first();
        Assertions.assertNotNull(layoutButton);
        Assertions.assertEquals("Layout Button", layoutButton.getText());
    }

    @Test
    public void getCurrentView_withLayout_returnsViewNotLayout() {
        navigate(ChildView.class);
        Assertions.assertInstanceOf(ChildView.class, getCurrentView());
    }
}
