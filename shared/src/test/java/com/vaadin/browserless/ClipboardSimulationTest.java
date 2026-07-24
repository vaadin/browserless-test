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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.ClipboardSimulator;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end browserless simulation of Flow's {@code Clipboard} write/read
 * bindings: clicking a bound button fires the client-side trigger and the
 * virtual clipboard + application callbacks behave as in a browser.
 */
class ClipboardSimulationTest {

    static class ClipboardView extends Div {
        final NativeButton copy = new NativeButton("copy");
        final NativeButton paste = new NativeButton("paste");
        final Div pasteTarget = new Div();
        String copied;
        int copyCount;
        Error copyError;
        String pasted;
        Error pasteError;
        String onPasteText;
        String onPasteHtml;

        ClipboardView() {
            Clipboard.onClick(copy).writeText("hello", c -> {
                copied = c;
                copyCount++;
            }, e -> copyError = e);
            Clipboard.onClick(paste).readText(t -> pasted = t,
                    e -> pasteError = e);
            pasteTarget.setId("paste-target");
            Clipboard.onPaste(pasteTarget, e -> {
                onPasteText = e.getText();
                onPasteHtml = e.getHtml();
            });
            add(copy, paste, pasteTarget);
        }
    }

    private static BrowserlessUIContext open() {
        return BrowserlessUIContext.forComponent(ClipboardView::new);
    }

    private static ClipboardSimulator clipboard(BrowserlessUIContext window) {
        return ClipboardSimulator.forUI(window.getUI());
    }

    @Test
    void click_copyButton_writesToClipboardAndRunsOnCopied() {
        try (BrowserlessUIContext window = open()) {
            ClipboardView view = window.find(ClipboardView.class).single();

            window.test(view.copy).click();

            assertEquals("hello", clipboard(window).text());
            assertEquals("hello", view.copied);
            assertNull(view.copyError);
        }
    }

    @Test
    void click_pasteButton_readsSeededClipboardAndRunsOnPayload() {
        try (BrowserlessUIContext window = open()) {
            ClipboardView view = window.find(ClipboardView.class).single();
            clipboard(window).setText("world");

            window.test(view.paste).click();

            assertEquals("world", view.pasted);
            assertNull(view.pasteError);
        }
    }

    @Test
    void deniedRead_runsOnError() {
        try (BrowserlessUIContext window = open()) {
            ClipboardView view = window.find(ClipboardView.class).single();
            clipboard(window).setText("world");
            clipboard(window).denyRead();

            window.test(view.paste).click();

            assertNull(view.pasted);
            assertEquals("NotAllowedError", view.pasteError.name());
        }
    }

    @Test
    void deniedWrite_runsOnError() {
        try (BrowserlessUIContext window = open()) {
            ClipboardView view = window.find(ClipboardView.class).single();
            clipboard(window).denyWrite();

            window.test(view.copy).click();

            assertNull(view.copied);
            assertEquals("NotAllowedError", view.copyError.name());
            assertNull(clipboard(window).text());
        }
    }

    @Test
    void concurrentEnvironments_areScopedToTheirOwnService() {
        ClipboardView[] a = new ClipboardView[1];
        ClipboardView[] b = new ClipboardView[1];
        try (BrowserlessUIContext w1 = BrowserlessUIContext
                .forComponent(() -> a[0] = new ClipboardView());
                BrowserlessUIContext w2 = BrowserlessUIContext
                        .forComponent(() -> b[0] = new ClipboardView())) {
            // w2's view was armed while BOTH environments' arming observers
            // were
            // installed. If observers weren't scoped to their own service, both
            // would record into w2 and the action would fire twice.
            w2.test(b[0].copy).click();
            assertEquals(1, b[0].copyCount, "action must fire exactly once");
            assertEquals("hello", ClipboardSimulator.forUI(w2.getUI()).text());

            // The other environment is untouched.
            assertEquals(0, a[0].copyCount);
            assertTrue(ClipboardSimulator.forUI(w1.getUI()).isEmpty());
        }
    }

    @Test
    void pasteInto_deliversClipboardContentsToOnPasteListener() {
        try (BrowserlessUIContext window = open()) {
            ClipboardView view = window.find(ClipboardView.class).single();
            ClipboardSimulator clipboard = clipboard(window);
            clipboard.setContents("plain text", "<b>rich</b>");

            clipboard.pasteInto(view.pasteTarget);

            assertEquals("plain text", view.onPasteText);
            assertEquals("<b>rich</b>", view.onPasteHtml);
        }
    }

    @Test
    void pasteInto_withComponentQuery_resolvesTarget() {
        try (BrowserlessUIContext window = open()) {
            clipboard(window).setText("via-query");

            clipboard(window)
                    .pasteInto(window.find(Div.class).withId("paste-target"));

            ClipboardView view = window.find(ClipboardView.class).single();
            assertEquals("via-query", view.onPasteText);
        }
    }

    @Test
    void pasteInto_withExplicitContents_deliversThem() {
        try (BrowserlessUIContext window = open()) {
            ClipboardView view = window.find(ClipboardView.class).single();

            clipboard(window).pasteInto(view.pasteTarget, "explicit", null);

            assertEquals("explicit", view.onPasteText);
            assertNull(view.onPasteHtml);
        }
    }

    @Test
    void clipboardSimulator_isSerializable() {
        try (BrowserlessUIContext window = open()) {
            ClipboardSimulator clipboard = clipboard(window);
            clipboard.setContents("plain", "<b>rich</b>");
            clipboard.denyWrite();
            // Stored in the UI attribute map, so it must not break session
            // serialization.
            SerializationDebugUtil.assertSerializable(clipboard);
        }
    }
}
