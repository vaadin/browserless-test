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

import com.testapp.clipboard.ClipboardSmokeView;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.vaadin.flow.component.clipboard.ClipboardSimulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke test that Flow trigger/action simulation works under
 * {@link SpringBrowserlessTest}: clicking a clipboard-bound button fires the
 * client-side trigger, so the {@link ClipboardSimulator} and the application's
 * callbacks behave as in a browser.
 */
@ContextConfiguration(classes = ClipboardSmokeTest.TestConfig.class)
@ViewPackages(classes = ClipboardSmokeView.class)
class ClipboardSmokeTest extends SpringBrowserlessTest {

    @Test
    void clickingCopy_firesWriteTrigger() {
        ClipboardSmokeView view = navigate(ClipboardSmokeView.class);

        test(view.copy).click();

        assertEquals(ClipboardSmokeView.COPY_TEXT,
                ClipboardSimulator.current().text());
        assertEquals(ClipboardSmokeView.COPY_TEXT, view.copied);
    }

    @Test
    void clickingPaste_firesReadTrigger() {
        ClipboardSmokeView view = navigate(ClipboardSmokeView.class);
        ClipboardSimulator.current().setText("pasted-value");

        test(view.paste).click();

        assertEquals("pasted-value", view.pasted);
    }

    @Configuration
    static class TestConfig {
    }
}
