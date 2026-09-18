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

import com.example.base.WelcomeView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that setting up the Vaadin environment evaluates the
 * {@link BaseBrowserlessTest#testConfiguration()} hook only once.
 *
 * The hook is overridable, so an override rebuilding the configuration on every
 * call would otherwise do the work twice, and a non deterministic one could
 * register lookup services that do not belong to the configuration actually
 * applied.
 */
@ViewPackages(classes = WelcomeView.class)
@BrowserlessTestConfig(applicationProperties = "hook.property=fromClass")
class TestConfigurationHookTest extends BrowserlessTest {

    private int calls;

    @Override
    protected BrowserlessConfiguration testConfiguration() {
        calls++;
        return super.testConfiguration();
    }

    @Test
    void testConfiguration_isResolvedOncePerSetup() {
        Assertions.assertEquals(1, calls,
                "Setting up the environment should evaluate testConfiguration() "
                        + "exactly once, but it was called " + calls
                        + " times");
    }
}
