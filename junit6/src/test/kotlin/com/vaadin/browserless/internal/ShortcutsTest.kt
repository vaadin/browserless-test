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
package com.vaadin.browserless.internal

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.karibudsl.v10.ModifierKey.Alt
import com.github.mvysny.karibudsl.v10.addClickShortcut
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.KeyModifier
import com.vaadin.flow.server.Command
import kotlin.test.expect

@DynaTestDsl
internal fun DynaNodeGroup.shortcutsTestBatch() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("smoke") {
        Shortcuts.fireShortcut(Key.ENTER) // nothing should happen
    }

    group("Button.addClickShortcut()") {
        test("simple") {
            var clicked = false
            Utils.getCurrentUI().button {
                onLeftClick { clicked = true }
                addClickShortcut(Key.ENTER)
            }
            Shortcuts.fireShortcut(Key.ENTER)
            expect(true) { clicked }
        }

        test("button not triggered on different key press") {
            var clicked = false
            Utils.getCurrentUI().button {
                onLeftClick { clicked = true }
                addClickShortcut(Key.KEY_A)
            }
            Shortcuts.fireShortcut(Key.ENTER)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE)
            expect(false) { clicked }
        }

        test("button not triggered on different modifiers") {
            var clicked = false
            Utils.getCurrentUI().button {
                onLeftClick { clicked = true }
                addClickShortcut(Alt + Key.KEY_A)
            }
            Shortcuts.fireShortcut(Key.KEY_A)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.KEY_A, KeyModifier.CONTROL)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.KEY_A, KeyModifier.CONTROL, KeyModifier.ALT)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.KEY_A, KeyModifier.ALT)
            expect(true) { clicked }
        }

        test("space") {
            // Key.SPACE has multiple key bindings, test that out.
            var clicked = false
            Utils.getCurrentUI().button {
                onLeftClick { clicked = true }
                addClickShortcut(Key.SPACE)
            }
            Shortcuts.fireShortcut(Key.SPACE)
            expect(true) { clicked }
        }

        test("space not triggered on different modifiers") {
            var clicked = false
            Utils.getCurrentUI().button {
                onLeftClick { clicked = true }
                addClickShortcut(Alt + Key.SPACE)
            }
            Shortcuts.fireShortcut(Key.ENTER)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE, KeyModifier.CONTROL)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE, KeyModifier.CONTROL, KeyModifier.ALT)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE, KeyModifier.ALT)
            expect(true) { clicked }
        }
    }

    group("Shortcuts.addShortcutListener()") {
        test("simple") {
            var clicked = false
            com.vaadin.flow.component.Shortcuts.addShortcutListener(Utils.getCurrentUI(), Command { clicked = true }, Key.ENTER)
            Shortcuts.fireShortcut(Key.ENTER)
            expect(true) { clicked }
        }

        test("button not triggered on different key press") {
            var clicked = false
            com.vaadin.flow.component.Shortcuts.addShortcutListener(Utils.getCurrentUI(), Command { clicked = true }, Key.KEY_A)
            Shortcuts.fireShortcut(Key.ENTER)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE)
            expect(false) { clicked }
        }

        test("button not triggered on different modifiers") {
            var clicked = false
            com.vaadin.flow.component.Shortcuts.addShortcutListener(Utils.getCurrentUI(), Command { clicked = true }, Key.KEY_A, KeyModifier.values()[2] /*ALT*/)
            Shortcuts.fireShortcut(Key.KEY_A)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.KEY_A, KeyModifier.CONTROL)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.KEY_A, KeyModifier.CONTROL, KeyModifier.ALT)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.KEY_A, KeyModifier.ALT)
            expect(true) { clicked }
        }

        test("space") {
            // Key.SPACE has multiple key bindings, test that out.
            var clicked = false
            com.vaadin.flow.component.Shortcuts.addShortcutListener(Utils.getCurrentUI(), Command { clicked = true }, Key.SPACE)
            Shortcuts.fireShortcut(Key.SPACE)
            expect(true) { clicked }
        }

        test("space not triggered on different modifiers") {
            var clicked = false
            com.vaadin.flow.component.Shortcuts.addShortcutListener(Utils.getCurrentUI(), Command { clicked = true }, Key.SPACE, KeyModifier.values()[2] /*ALT*/)
            Shortcuts.fireShortcut(Key.ENTER)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE, KeyModifier.CONTROL)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE, KeyModifier.CONTROL, KeyModifier.ALT)
            expect(false) { clicked }
            Shortcuts.fireShortcut(Key.SPACE, KeyModifier.ALT)
            expect(true) { clicked }
        }
    }
}
