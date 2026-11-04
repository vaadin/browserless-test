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

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;

/**
 * Functions for firing keyboard shortcuts.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class Shortcuts {

    private Shortcuts() {
    }

    /**
     * Fires a shortcut event with given {@code key} and {@code modifiers} in
     * the current UI. This will in turn notify all components currently
     * attached to the current UI which subscribed for this exact key
     * combination.
     *
     * @param key
     *            the key, not null.
     * @param modifiers
     *            the modifier keys.
     */
    public static void fireShortcut(Key key, Key... modifiers) {
        // keep the modifiers of type Key[] instead of KeyModifier[], otherwise
        // you won't be able to call those from Kotlin:
        // https://github.com/vaadin/flow/issues/5051
        // and https://youtrack.jetbrains.com/issue/KT-35021
        _fireShortcut(Utils.getCurrentUI(), key, modifiers);
    }

    /**
     * Use the global {@link #fireShortcut(Key, Key...)} function unless you
     * know what you're doing!
     *
     * @param component
     *            the component to fire the shortcut on.
     * @param key
     *            the key, not null.
     * @param modifiers
     *            the modifier keys.
     */
    public static void _fireShortcut(Component component, Key key,
            Key... modifiers) {
        // all shortcut registration carry a filter with them, in order to
        // filter out pressed keys. All the filters are then transferred to the
        // server-side and compared against DomEventListenerWrapper.filter in
        // DomEventListenerWrapper.matchesFilter()

        // The `matchesFilter()` function is peculiar: it simply checks whether
        // the data contains a boolean value with key 'filter'. We need to fake
        // the json object as if it contained all filters (otherwise
        // `matchesFilter()` would NPE on missing key) and respond true only to
        // the matching filter.
        MockFilterJsonObject data = new MockFilterJsonObject(key,
                new LinkedHashSet<>(Arrays.asList(modifiers)));

        // the shortcut registration is only updated in
        // UI.beforeClientResponse; run the registration code now.
        MockVaadin.clientRoundtrip();

        // this will fire the "keydown" DOM event, which in turn fires
        // KeyDownEvent event, which in turn invokes the ShortcutListener.
        BasicUtils._fireDomEvent(component, "keydown", data);
    }
}
