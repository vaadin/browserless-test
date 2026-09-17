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

import java.util.List;

import com.example.reload.ParameterizedCounterView;
import com.example.reload.PlainCounterView;
import com.example.reload.PreservedCounterView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    void reload_replaysRouteParameterAndQueryString() {
        ParameterizedCounterView view = navigate(
                "param-counter/order-1?tab=history",
                ParameterizedCounterView.class);
        Assertions.assertEquals("order-1", view.getParameter());

        ParameterizedCounterView afterReload = reload(
                ParameterizedCounterView.class);

        Assertions.assertNotSame(view, afterReload,
                "A plain view must be recreated on reload");
        Assertions.assertEquals("order-1", afterReload.getParameter(),
                "The route parameter must be replayed on reload");
        Assertions.assertEquals(List.of("history"),
                afterReload.getQueryParameters().getParameters().get("tab"),
                "The query string must be replayed on reload");
    }

    @Test
    void closeSessionThenReload_recreatesSessionAndUI() {
        PlainCounterView view = navigate(PlainCounterView.class);
        UI uiBefore = UI.getCurrent();
        VaadinSession sessionBefore = VaadinSession.getCurrent();
        sessionBefore.setAttribute("marker", "gone");

        // The logout idiom: close the session, then tell the browser to
        // reload. Closing the session already rendered a fresh UI, so the
        // reload has nothing left to do and must not fail.
        uiBefore.getSession().close();
        uiBefore.getPage().reload();

        Assertions.assertNotSame(sessionBefore, VaadinSession.getCurrent(),
                "Closing the session must create a fresh one");
        Assertions.assertNull(VaadinSession.getCurrent().getAttribute("marker"),
                "Session-scoped state must not survive the logout");
        Assertions.assertNotSame(uiBefore, UI.getCurrent(),
                "Closing the session must create a fresh UI");
        Assertions.assertNotSame(view, getCurrentView(),
                "The view must be recreated in the new UI");
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
