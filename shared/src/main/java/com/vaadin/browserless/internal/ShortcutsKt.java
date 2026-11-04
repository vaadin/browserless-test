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

/**
 * Forwards to {@link Shortcuts}, for code compiled against the former Kotlin
 * file facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link Shortcuts} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class ShortcutsKt {

    private ShortcutsKt() {
    }

    public static void fireShortcut(com.vaadin.flow.component.Key p0,
            com.vaadin.flow.component.Key... p1) {
        Shortcuts.fireShortcut(p0, p1);
    }

    public static void _fireShortcut(com.vaadin.flow.component.Component p0,
            com.vaadin.flow.component.Key p1,
            com.vaadin.flow.component.Key... p2) {
        Shortcuts._fireShortcut(p0, p1, p2);
    }
}
