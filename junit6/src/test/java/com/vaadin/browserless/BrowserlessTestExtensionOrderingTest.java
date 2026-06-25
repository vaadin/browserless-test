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

import java.util.ArrayList;
import java.util.List;

import com.example.base.WelcomeView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.vaadin.flow.component.UI;

/**
 * Regression test for
 * <a href= "https://github.com/vaadin/browserless-test/issues/112">#112</a>:
 * the Vaadin environment set up by {@link BrowserlessTest} must run
 * <em>after</em> extensions registered on the concrete subclass (such as
 * weld-junit5's {@code @EnableAutoWeld}, which starts the CDI container its
 * {@code MockVaadin.setup()} depends on).
 *
 * <p>
 * A {@link BeforeEachCallback} declared on the subclass stands in for such a
 * container-starting extension. Because {@link BrowserlessTest} drives its
 * per-method setup from an instance {@code @BeforeEach} method (not from a
 * {@code BeforeEachCallback} on the inherited extension), JUnit 5 always runs
 * the subclass callback first.
 */
@ViewPackages(classes = WelcomeView.class)
@ExtendWith(BrowserlessTestExtensionOrderingTest.RecordingExtension.class)
class BrowserlessTestExtensionOrderingTest extends BrowserlessTest {

    static final List<String> ORDER = new ArrayList<>();

    /**
     * Stand-in for an extension (e.g. {@code WeldJunit5AutoExtension}) that the
     * test relies on being initialized before {@code MockVaadin.setup()}.
     */
    static class RecordingExtension implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) {
            ORDER.add("extension.beforeEach");
        }
    }

    @Override
    protected void initVaadinEnvironment() {
        ORDER.add("initVaadinEnvironment");
        super.initVaadinEnvironment();
    }

    @Test
    void subclassExtensionRunsBeforeVaadinSetup() {
        Assertions.assertEquals(
                List.of("extension.beforeEach", "initVaadinEnvironment"), ORDER,
                "Subclass BeforeEachCallback must run before BrowserlessTest "
                        + "sets up the Vaadin environment");
        Assertions.assertNotNull(UI.getCurrent(),
                "Vaadin environment must still be initialized for the test");
    }
}
