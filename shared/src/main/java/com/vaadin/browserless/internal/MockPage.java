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
package com.vaadin.browserless.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.VaadinSession;

/**
 * A {@link Page} which records the external navigations and recreates the UI on
 * {@link #reload()}, to simulate the browser's F5.
 */
public final class MockPage extends Page {

    private static final Set<String> SELF_NAMES = Set.of("_self", "_parent",
            "_top", "");

    private final UI ui;
    private final UIFactory uiFactory;
    private final VaadinSession session;
    private final Map<String, List<String>> navigations = new LinkedHashMap<>();

    public MockPage(UI ui, UIFactory uiFactory, VaadinSession session) {
        super(ui);
        this.ui = ui;
        this.uiFactory = uiFactory;
        this.session = session;
    }

    public String getLastExternalNavigationURL() {
        return lastOrNull(navigations.get("_self"));
    }

    public String getExternalNavigationURL(String windowName) {
        return lastOrNull(navigations.get(normalizeWindowName(windowName)));
    }

    public Map<String, List<String>> getOpenedWindows() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        navigations.forEach((name, urls) -> {
            if (!name.equals("_self")) {
                result.put(name, new ArrayList<>(urls));
            }
        });
        return result;
    }

    @Override
    public void open(String url, String windowName) {
        String normalized = normalizeWindowName(windowName);
        if (normalized.equals("_blank")) {
            navigations.computeIfAbsent(normalized, k -> new ArrayList<>())
                    .add(url);
        } else {
            List<String> urls = new ArrayList<>();
            urls.add(url);
            navigations.put(normalized, urls);
        }
        super.open(url, windowName);
    }

    @Override
    public void reload() {
        UI current = UI.getCurrent();
        if (current != ui) {
            if (ui.getSession() == null) {
                // This UI lost its session, so it is the logout idiom -
                // capture the UI, close its session, then ask its page to
                // reload. Closing the session already tore this UI down and
                // rendered a fresh one for its window, which is what the
                // reload is asking for, so there is nothing left to do. Not
                // even the superclass call, which would fail: it schedules
                // JavaScript on a UI whose session is gone.
                return;
            }
            // A live UI, but not the current one. Recreating the UI runs on
            // the thread-locals of the current UI, so going on would detach
            // and recreate whichever window is current. Fail before anything
            // is changed.
            throw new IllegalStateException("Cannot reload the page of UI " + ui
                    + ", because the current UI is "
                    + (current != null ? String.valueOf(current) : "not set")
                    + ". Reloading recreates the current UI, so the "
                    + "reloaded window must be the current one. Either this "
                    + "UI belongs to another window - activate it first, "
                    + "e.g. through its BrowserlessUIContext or "
                    + "UI.access() - or an earlier reload already replaced "
                    + "it, in which case reload the current UI instead.");
        }

        // recreate the UI on reload(), to simulate browser's F5
        super.reload();
        MockVaadin.closeCurrentUI(true);
        MockVaadin.createUI(uiFactory, session);
        // Record the swap so holders of the detached UI (BrowserlessUIContext)
        // can follow the window to the new UI. This path is also taken when
        // application code calls Page.reload() itself, not only from the
        // reload() DSL.
        UI created = UI.getCurrent();
        if (created != null && created != ui) {
            MockVaadin.recordReloadReplacement(ui, created);
        }
    }

    private static String normalizeWindowName(String windowName) {
        return windowName == null || SELF_NAMES.contains(windowName) ? "_self"
                : windowName;
    }

    private static String lastOrNull(List<String> list) {
        return list == null || list.isEmpty() ? null
                : list.get(list.size() - 1);
    }
}
