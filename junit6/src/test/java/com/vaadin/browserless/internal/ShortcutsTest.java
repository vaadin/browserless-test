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

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;

import static com.vaadin.browserless.internal.Shortcuts.fireShortcut;
import static com.vaadin.browserless.internal.Utils.currentUI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutsTest {

    private final AtomicBoolean clicked = new AtomicBoolean();

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void fireShortcut_noListenerRegistered_doesNothing() {
        fireShortcut(Key.ENTER);
    }

    /**
     * Adds a button to the current UI which records a click, and binds the
     * given click shortcut to it.
     */
    private void buttonWithClickShortcut(Key key, KeyModifier... modifiers) {
        Button button = new Button();
        button.addClickListener(e -> clicked.set(true));
        button.addClickShortcut(key, modifiers);
        currentUI().add(button);
    }

    private void shortcutListener(Key key, KeyModifier... modifiers) {
        com.vaadin.flow.component.Shortcuts.addShortcutListener(currentUI(),
                () -> clicked.set(true), key, modifiers);
    }

    @Nested
    class ButtonAddClickShortcut {

        @Test
        void matchingKey_clicksTheButton() {
            buttonWithClickShortcut(Key.ENTER);
            fireShortcut(Key.ENTER);
            assertTrue(clicked.get());
        }

        @Test
        void differentKey_doesNotClickTheButton() {
            buttonWithClickShortcut(Key.KEY_A);
            fireShortcut(Key.ENTER);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE);
            assertFalse(clicked.get());
        }

        @Test
        void differentModifiers_doesNotClickTheButton() {
            buttonWithClickShortcut(Key.KEY_A, KeyModifier.ALT);
            fireShortcut(Key.KEY_A);
            assertFalse(clicked.get());
            fireShortcut(Key.KEY_A, KeyModifier.CONTROL);
            assertFalse(clicked.get());
            fireShortcut(Key.KEY_A, KeyModifier.CONTROL, KeyModifier.ALT);
            assertFalse(clicked.get());
            fireShortcut(Key.KEY_A, KeyModifier.ALT);
            assertTrue(clicked.get());
        }

        @Test
        void space_multipleKeyBindings_clicksTheButton() {
            // Key.SPACE has multiple key bindings, test that out.
            buttonWithClickShortcut(Key.SPACE);
            fireShortcut(Key.SPACE);
            assertTrue(clicked.get());
        }

        @Test
        void spaceWithDifferentModifiers_doesNotClickTheButton() {
            buttonWithClickShortcut(Key.SPACE, KeyModifier.ALT);
            fireShortcut(Key.ENTER);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE, KeyModifier.CONTROL);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE, KeyModifier.CONTROL, KeyModifier.ALT);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE, KeyModifier.ALT);
            assertTrue(clicked.get());
        }
    }

    @Nested
    class ShortcutsAddShortcutListener {

        @Test
        void matchingKey_runsTheCommand() {
            shortcutListener(Key.ENTER);
            fireShortcut(Key.ENTER);
            assertTrue(clicked.get());
        }

        @Test
        void differentKey_doesNotRunTheCommand() {
            shortcutListener(Key.KEY_A);
            fireShortcut(Key.ENTER);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE);
            assertFalse(clicked.get());
        }

        @Test
        void differentModifiers_doesNotRunTheCommand() {
            shortcutListener(Key.KEY_A, KeyModifier.ALT);
            fireShortcut(Key.KEY_A);
            assertFalse(clicked.get());
            fireShortcut(Key.KEY_A, KeyModifier.CONTROL);
            assertFalse(clicked.get());
            fireShortcut(Key.KEY_A, KeyModifier.CONTROL, KeyModifier.ALT);
            assertFalse(clicked.get());
            fireShortcut(Key.KEY_A, KeyModifier.ALT);
            assertTrue(clicked.get());
        }

        @Test
        void space_multipleKeyBindings_runsTheCommand() {
            // Key.SPACE has multiple key bindings, test that out.
            shortcutListener(Key.SPACE);
            fireShortcut(Key.SPACE);
            assertTrue(clicked.get());
        }

        @Test
        void spaceWithDifferentModifiers_doesNotRunTheCommand() {
            shortcutListener(Key.SPACE, KeyModifier.ALT);
            fireShortcut(Key.ENTER);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE, KeyModifier.CONTROL);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE, KeyModifier.CONTROL, KeyModifier.ALT);
            assertFalse(clicked.get());
            fireShortcut(Key.SPACE, KeyModifier.ALT);
            assertTrue(clicked.get());
        }
    }
}
