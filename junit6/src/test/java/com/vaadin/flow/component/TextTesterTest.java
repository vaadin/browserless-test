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
package com.vaadin.flow.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class TextTesterTest extends BrowserlessTest {

    TextView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(TextView.class);
        view = navigate(TextView.class);
    }

    @Test
    void queryLocatesTextComponent() {
        Assertions.assertEquals("Hello",
                test(find(Text.class).first()).getText());
    }

    @Test
    void getText_returnsText() {
        Assertions.assertEquals("Hello", test(view.text).getText());

        view.text.setText("Updated");
        Assertions.assertEquals("Updated", test(view.text).getText());
    }

    @Test
    void getText_notUsable_throws() {
        // Text is a text node and has no visibility of its own; hiding the
        // parent subtree makes it not usable.
        view.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.text)::getText);
    }

}
