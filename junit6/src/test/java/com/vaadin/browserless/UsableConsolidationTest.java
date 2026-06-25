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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.automation.Usable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;

@ViewPackages
public class UsableConsolidationTest extends BrowserlessTest {

    private Usable usableOf(com.vaadin.flow.component.Component c) {
        return BrowserlessAutomationTestSupport.driving(c).of(c)
                .as(Usable.class);
    }

    @Test
    public void childInInvisibleSubtree_isNotUsable() {
        Div parent = new Div();
        NativeButton child = new NativeButton("x");
        parent.add(child);
        parent.setVisible(false); // child.isVisible() stays true; subtree is
                                  // hidden
        UI.getCurrent().add(parent);

        Assertions.assertFalse(usableOf(child).isUsable(),
                "a visible child inside an invisible parent must be not-usable (subtree visibility)");
    }

    @Test
    public void childOfDisabledParent_isNotUsable() {
        Div parent = new Div();
        NativeButton child = new NativeButton("x");
        parent.add(child);
        parent.setEnabled(false); // element.isEnabled() propagates to child
        UI.getCurrent().add(parent);

        Assertions.assertFalse(usableOf(child).isUsable(),
                "a child of a disabled parent must be not-usable (element-level enablement)");
    }

    @Test
    public void readOnlyHasValue_testerIsNotUsable_withReason() {
        com.vaadin.flow.component.textfield.TextField tf = new com.vaadin.flow.component.textfield.TextField();
        tf.setReadOnly(true);
        UI.getCurrent().add(tf);

        ComponentTester<com.vaadin.flow.component.textfield.TextField> tester = new ComponentTester<>(
                tf);

        Assertions.assertFalse(tester.isUsable(),
                "read-only field is not usable via the consolidated Usable");
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class, tester::ensureComponentIsUsable);
        Assertions.assertTrue(ex.getMessage().contains("read only"),
                "message should explain read-only: " + ex.getMessage());
    }
}
