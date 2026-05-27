/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.internal.JacksonUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public final class Shortcuts {

    private Shortcuts() {
    }

    /**
     * Take a look at `DomEventListenerWrapper.matchesFilter()` to see why this is necessary.
     * If this stuff stops working, place a breakpoint into the [getBoolean]/[hasKey] function,
     * to see what kind of keys you're receiving and whether it matches [filter].
     */
    private static final class MockFilterJsonObject extends ObjectNode {

        private static final Method mgenerateEventKeyFilter;
        private static final Method mgenerateEventModifierFilter;
        private static final Constructor<?> chashableKey;

        static {
            try {
                mgenerateEventKeyFilter =
                        ShortcutRegistration.class.getDeclaredMethod("generateEventKeyFilter", Key.class);
                mgenerateEventModifierFilter =
                        ShortcutRegistration.class.getDeclaredMethod("generateEventModifierFilter", java.util.Collection.class);
                Class<?> hashableKeyClass = ShortcutRegistration.class.getClassLoader()
                        .loadClass("com.vaadin.flow.component.ShortcutRegistration$HashableKey");
                chashableKey = hashableKeyClass.getDeclaredConstructors()[0];
                mgenerateEventKeyFilter.setAccessible(true);
                mgenerateEventModifierFilter.setAccessible(true);
                chashableKey.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private final String filter;
        private String filterString = "";

        MockFilterJsonObject(Key key, Set<Key> modifiers) {
            super(JacksonUtils.getMapper().getNodeFactory());
            try {
                // compute the filter
                List<Object> hashableModifiers = new ArrayList<>();
                for (Key modifier : modifiers) {
                    hashableModifiers.add(chashableKey.newInstance(modifier));
                }
                filter = mgenerateEventKeyFilter.invoke(null, key).toString() + " && "
                        + mgenerateEventModifierFilter.invoke(null, hashableModifiers).toString();

                // populate the json object so that KeyDownEvent can be created from it
                put("event.key", key.getKeys().get(0));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean has(String key) {
            // the "key" is a JavaScript expression which matches the key pressed.
            // we need to match it against the 'filter'
            if (!key.startsWith("([")) {
                // not a filter
                return super.get(key) != null;
            }
            return matchesFilter(key);
        }

        private boolean matchesFilter(String key) {
            String probeFilter = key;
            if (probeFilter.endsWith(" && (event.stopPropagation() || true)")) {
                probeFilter = probeFilter.substring(0,
                        probeFilter.length() - " && (event.stopPropagation() || true)".length());
            }
            if (probeFilter.endsWith(" && (event.preventDefault() || true)")) {
                probeFilter = probeFilter.substring(0,
                        probeFilter.length() - " && (event.preventDefault() || true)".length());
            }
            return probeFilter.startsWith(filter);
        }

        @Override
        public JsonNode get(String keyString) {
            filterString = keyString;
            if (keyString.startsWith("([")) {
                // For filter key we return this so we get the correct matches
                // for booleanValue as get returns null
                return this;
            }
            return super.get(keyString);
        }

        @Override
        public boolean booleanValue() {
            if (!filterString.startsWith("([")) {
                return super.booleanValue();
            }
            return matchesFilter(filterString);
        }
    }

    /**
     * Fires a shortcut event with given [key] and [modifiers] in the current UI.
     * This will in turn notify all components currently attached to the current UI
     * which subscribed for this exact key combination.
     */
    public static void fireShortcut(Key key, Key... modifiers) {
        // keep the modifiers of type Key[] instead of KeyModifier[], otherwise
        // you won't be able to call those from Kotlin: https://github.com/vaadin/flow/issues/5051
        // and https://youtrack.jetbrains.com/issue/KT-35021
        _fireShortcut(Utils.currentUI(), key, modifiers);
    }

    /**
     * Use the global `fireShortcut()` function unless you know what you're doing!
     */
    public static void _fireShortcut(Component component, Key key, Key... modifiers) {
        // all shortcut registration carry a filter with them, in order to filter out
        // pressed keys. All the filters are then transferred to the server-side
        // and compared against DomEventListenerWrapper.filter in
        // DomEventListenerWrapper.matchesFilter()

        // The `matchesFilter()` function is peculiar: it simply checks whether the data
        // contains a boolean value with key 'filter'. We need to fake the json object
        // as if it contained all filters (otherwise `matchesFilter()` would NPE on missing key)
        // and respond true only to the matching filter.
        Set<Key> modifierSet = new HashSet<>();
        for (Key modifier : modifiers) {
            modifierSet.add(modifier);
        }
        MockFilterJsonObject data = new MockFilterJsonObject(key, modifierSet);

        // the shortcut registration is only updated in [UI.beforeClientResponse]; run the registration code now.
        MockVaadin.clientRoundtrip();

        // this will fire the "keydown" DOM event, which in turn fires KeyDownEvent event,
        // which in turn invokes the ShortcutListener.
        BasicUtils._fireDomEvent(component, "keydown", data);
    }
}
