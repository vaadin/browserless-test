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

import com.example.TestApplication;
import com.example.base.SimpleViewWithSharedState;
import com.example.base.WelcomeView;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 */
@SpringBootTest
@ContextConfiguration(classes = {TestApplication.class})
@ViewPackages(classes = SimpleViewWithSharedState.class)
class MultiTabTest {

    @RegisterExtension
    BrowserlessExtension ext = new BrowserlessExtension();

    @Test
    void firstTest_recordsUI() {
        UI ui = UI.getCurrent();
        Assertions.assertNotNull(ui, "Expecting current UI to be available");
        ext.navigate(SimpleViewWithSharedState.class);
        Assertions.assertInstanceOf(SimpleViewWithSharedState.class, ext.getCurrentView());
    }

}
