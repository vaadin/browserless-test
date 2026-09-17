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

import com.example.reload.PlainCounterView;
import com.example.reload.PreservedCounterView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

/**
 * Mirror of {@link ReloadPreserveOnRefreshTest} exercising
 * {@link BrowserlessExtension#reload()} and
 * {@link BrowserlessExtension#reload(Class)} through the extension entry point,
 * which is a published surface of its own.
 */
@ViewPackages(classes = { PreservedCounterView.class, PlainCounterView.class })
class ReloadOnExtensionTest {

    @RegisterExtension
    BrowserlessExtension ext = new BrowserlessExtension();

    @Test
    void reloadWithExpectedTarget_preservedView_keepsInstanceAndState() {
        PreservedCounterView view = ext.navigate(PreservedCounterView.class);
        ext.test(ext.find(Button.class).withId("increment").single()).click();

        PreservedCounterView afterReload = ext
                .reload(PreservedCounterView.class);

        Assertions.assertSame(view, afterReload,
                "@PreserveOnRefresh view must keep the same instance across reload");
        Assertions.assertEquals(1, afterReload.getCount(),
                "@PreserveOnRefresh view must retain its state across reload");
    }

    @Test
    void reload_plainView_createsFreshUIAndView() {
        PlainCounterView view = ext.navigate(PlainCounterView.class);
        UI uiBefore = UI.getCurrent();

        Assertions.assertNotSame(view, ext.reload(),
                "A plain view must be recreated on reload");
        Assertions.assertNotSame(uiBefore, UI.getCurrent(),
                "Reload must create a fresh UI, also on the extension surface");
    }
}
