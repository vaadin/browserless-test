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

import com.example.reload.PlainCounterView;
import com.example.reload.PreservedCounterView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinSession;

/**
 * Confirms the {@link #reload()} DSL honors
 * {@link com.vaadin.flow.router.PreserveOnRefresh}: a preserved view keeps its
 * instance and state across a refresh, while a plain view is recreated. Also
 * verifies that session-scoped state survives the reload.
 */
@ViewPackages(classes = { PreservedCounterView.class, PlainCounterView.class })
class ReloadPreserveOnRefreshTest extends BrowserlessTest {

    @Test
    void preserveOnRefresh_reusesInstanceAndState() {
        PreservedCounterView view = navigate(PreservedCounterView.class);
        test(find(Button.class).withId("increment").single()).click();
        test(find(Button.class).withId("increment").single()).click();
        Assertions.assertEquals(2, view.getCount());

        PreservedCounterView afterReload = reload(PreservedCounterView.class);

        Assertions.assertSame(view, afterReload,
                "@PreserveOnRefresh view must keep the same instance across reload");
        Assertions.assertEquals(2, afterReload.getCount(),
                "@PreserveOnRefresh view must retain its state across reload");
    }

    @Test
    void plainView_isRecreatedOnRefresh() {
        PlainCounterView view = navigate(PlainCounterView.class);
        test(find(Button.class).withId("increment").single()).click();
        Assertions.assertEquals(1, view.getCount());

        PlainCounterView afterReload = reload(PlainCounterView.class);

        Assertions.assertNotSame(view, afterReload,
                "A plain view must be recreated on reload");
        Assertions.assertEquals(0, afterReload.getCount(),
                "A plain view's state must reset on reload");
    }

    @Test
    void reload_keepsSameSessionAndScopedState() {
        navigate(PlainCounterView.class);
        VaadinSession sessionBefore = VaadinSession.getCurrent();
        sessionBefore.setAttribute("marker", "kept");
        UI uiBefore = UI.getCurrent();

        reload();

        Assertions.assertSame(sessionBefore, VaadinSession.getCurrent(),
                "Reload must keep the same Vaadin session");
        Assertions.assertEquals("kept",
                VaadinSession.getCurrent().getAttribute("marker"),
                "Session-scoped state must survive a reload");
        Assertions.assertNotSame(uiBefore, UI.getCurrent(),
                "Reload must create a fresh UI");
    }
}
