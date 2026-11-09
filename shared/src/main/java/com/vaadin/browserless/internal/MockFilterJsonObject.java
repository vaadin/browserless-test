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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Take a look at {@code DomEventListenerWrapper.matchesFilter()} to see why
 * this is necessary. If this stuff stops working, place a breakpoint into the
 * {@link #get(String)}/{@link #has(String)} function, to see what kind of keys
 * you're receiving and whether it matches the filter.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class MockFilterJsonObject extends ObjectNode {

    private static final Method GENERATE_EVENT_KEY_FILTER;
    private static final Method GENERATE_EVENT_MODIFIER_FILTER;
    private static final Constructor<?> HASHABLE_KEY;

    static {
        try {
            GENERATE_EVENT_KEY_FILTER = ShortcutRegistration.class
                    .getDeclaredMethod("generateEventKeyFilter", Key.class);
            GENERATE_EVENT_MODIFIER_FILTER = ShortcutRegistration.class
                    .getDeclaredMethod("generateEventModifierFilter",
                            Collection.class);
            HASHABLE_KEY = ShortcutRegistration.class.getClassLoader()
                    .loadClass(
                            "com.vaadin.flow.component.ShortcutRegistration$HashableKey")
                    .getDeclaredConstructors()[0];
            GENERATE_EVENT_KEY_FILTER.setAccessible(true);
            GENERATE_EVENT_MODIFIER_FILTER.setAccessible(true);
            HASHABLE_KEY.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final String filter;

    MockFilterJsonObject(Key key, Set<? extends Key> modifiers) {
        super(JacksonUtils.getMapper().getNodeFactory());
        try {
            List<Object> hashableModifiers = new ArrayList<>();
            for (Key modifier : modifiers) {
                hashableModifiers.add(getHashableKey(modifier));
            }
            // compute the filter
            filter = GENERATE_EVENT_KEY_FILTER.invoke(null, key).toString()
                    + " && " +
                    // we call the private method with Hashable keys
                    // implementations as the ShortcutRegistration class is
                    // called by these objects/classes in regular cases.
                    GENERATE_EVENT_MODIFIER_FILTER
                            .invoke(null, hashableModifiers).toString();
        } catch (ReflectiveOperationException e) {
            throw Utils.sneakyThrow(e);
        }

        // populate the json object so that KeyDownEvent can be created from it
        put("event.key", key.getKeys().get(0));
    }

    /**
     * Returns a HashableKey instance for the given key modifier.
     *
     * @param keyModifier
     *            the key modifier
     * @return the HashableKey instance
     */
    private static Object getHashableKey(Key keyModifier)
            throws ReflectiveOperationException {
        return HASHABLE_KEY.newInstance(keyModifier);
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
        // the "key" is a JavaScript expression which matches the key pressed.
        // we need to match it against the 'filter'
        String probeFilter = removeSuffix(
                removeSuffix(key, " && (event.stopPropagation() || true)"),
                " && (event.preventDefault() || true)");
        return probeFilter.startsWith(filter);
    }

    private static String removeSuffix(String s, String suffix) {
        return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length())
                : s;
    }

    @Override
    public JsonNode get(String keyString) {
        if (keyString.startsWith("([")) {
            // Flow evaluates the value sent for a filter with JavaScript
            // truthiness, so answer with the result the filter would have
            // returned in the browser
            return BooleanNode.valueOf(matchesFilter(keyString));
        }
        return super.get(keyString);
    }
}
