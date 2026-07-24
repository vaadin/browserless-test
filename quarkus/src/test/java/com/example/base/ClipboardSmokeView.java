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
package com.example.base;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Test fixture wiring both a copy ({@code writeText}) and a read
 * ({@code readText}) clipboard binding to buttons, so a browserless smoke test
 * can verify trigger/action simulation end to end.
 */
@Route("clipboard-smoke")
public class ClipboardSmokeView extends Div {

    public static final String COPY_TEXT = "smoke-copied-value";

    public final NativeButton copy = new NativeButton("Copy");
    public final NativeButton paste = new NativeButton("Paste");

    public String copied;
    public String pasted;

    public ClipboardSmokeView() {
        Clipboard.onClick(copy).writeText(COPY_TEXT, c -> copied = c, e -> {
        });
        Clipboard.onClick(paste).readText(t -> pasted = t, e -> {
        });
        add(copy, paste);
    }
}
