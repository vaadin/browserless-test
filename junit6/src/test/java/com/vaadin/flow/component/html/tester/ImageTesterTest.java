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
package com.vaadin.flow.component.html.tester;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ImageTester;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class ImageTesterTest extends BrowserlessTest {

    ImageView view;
    ImageTester tester;
    Image component;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(ImageView.class);
        view = navigate(ImageView.class);
        component = view.image;
        tester = test(component);
    }

    @Test
    void click_listenerNotified() {
        tester.click();
        Assertions.assertEquals(1, view.clicks);
    }

    @Test
    void click_unusable_throws() {
        component.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> tester.click());
    }

    @Test
    void getTitle_returnsTitle() {
        Assertions.assertEquals("Vaadin logo", tester.getTitle());
    }

}
