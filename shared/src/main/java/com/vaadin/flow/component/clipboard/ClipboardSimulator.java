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
package com.vaadin.flow.component.clipboard;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;

/**
 * Browserless test driver for the {@link Clipboard} API: a per-window stand-in
 * for the browser's system clipboard. Tests describe the clipboard's contents
 * and access state, then exercise the application, which reads and writes it
 * through {@code Clipboard} bindings.
 * <p>
 * A copy binding ({@code Clipboard.onClick(button).writeText(field)}) is
 * exercised by clicking the button through its component tester: the click
 * fires the underlying trigger, so the value lands here and can be asserted
 * with {@link #text()} / {@link #html()}. A read binding
 * ({@code Clipboard.onClick(button).readText(...)}) is exercised the same way
 * after seeding the clipboard with {@link #setText}, {@link #setHtml}, or
 * {@link #setContents}; the application's callback then receives the contents.
 * <p>
 * {@link #denyRead()}, {@link #denyWrite()}, and {@link #denyAccess()} simulate
 * the browser refusing clipboard access, so the application's {@code onError}
 * callback receives a {@code NotAllowedError}. Image clipboard payloads are not
 * supported.
 * <p>
 * Obtain via {@link #current()} or {@link #forUI(UI)}: idempotent, both create
 * the simulator on the first call and return the same instance afterward. The
 * clipboard is scoped to a single window (UI); windows do not share it.
 *
 * @since 1.1
 */
public final class ClipboardSimulator implements Serializable {

    private @Nullable String text;
    private @Nullable String html;
    private boolean readDenied;
    private boolean writeDenied;

    private ClipboardSimulator() {
    }

    /**
     * Returns the simulator bound to {@link UI#getCurrent()}.
     *
     * @return the simulator for the current UI, never {@code null}
     */
    public static ClipboardSimulator current() {
        return forUI(UI.getCurrent());
    }

    /**
     * Returns the simulator bound to the given UI, creating and storing one on
     * first access.
     *
     * @param ui
     *            the UI to query, not {@code null}
     * @return the UI's clipboard simulator, never {@code null}
     */
    public static ClipboardSimulator forUI(UI ui) {
        ClipboardSimulator simulator = ComponentUtil.getData(ui,
                ClipboardSimulator.class);
        if (simulator == null) {
            simulator = new ClipboardSimulator();
            ComponentUtil.setData(ui, ClipboardSimulator.class, simulator);
        }
        return simulator;
    }

    /**
     * The current {@code text/plain} contents, or {@code null} if none.
     *
     * @return the plain-text contents, or {@code null}
     */
    @Nullable
    public String text() {
        return text;
    }

    /**
     * The current {@code text/html} contents, or {@code null} if none.
     *
     * @return the HTML contents, or {@code null}
     */
    @Nullable
    public String html() {
        return html;
    }

    /**
     * Whether the clipboard holds no contents.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return text == null && html == null;
    }

    /**
     * Seeds the {@code text/plain} contents, leaving the HTML slot unchanged.
     *
     * @param text
     *            the plain text, or {@code null} to clear the slot
     */
    public void setText(@Nullable String text) {
        this.text = text;
    }

    /**
     * Seeds the {@code text/html} contents, leaving the plain-text slot
     * unchanged.
     *
     * @param html
     *            the HTML, or {@code null} to clear the slot
     */
    public void setHtml(@Nullable String html) {
        this.html = html;
    }

    /**
     * Seeds both {@code text/plain} and {@code text/html} contents at once.
     *
     * @param text
     *            the plain text, or {@code null}
     * @param html
     *            the HTML, or {@code null}
     */
    public void setContents(@Nullable String text, @Nullable String html) {
        this.text = text;
        this.html = html;
    }

    /**
     * Clears all contents.
     */
    public void clear() {
        text = null;
        html = null;
    }

    /**
     * Makes subsequent clipboard reads fail with a {@code NotAllowedError},
     * simulating a denied {@code clipboard-read} permission.
     */
    public void denyRead() {
        readDenied = true;
    }

    /**
     * Makes subsequent clipboard writes fail with a {@code NotAllowedError}.
     */
    public void denyWrite() {
        writeDenied = true;
    }

    /**
     * Makes subsequent clipboard reads and writes fail with a
     * {@code NotAllowedError}.
     */
    public void denyAccess() {
        denyRead();
        denyWrite();
    }

    /**
     * Restores clipboard access after a {@code deny*} call.
     */
    public void grantAccess() {
        readDenied = false;
        writeDenied = false;
    }

    /**
     * Whether reads are currently denied.
     *
     * @return {@code true} if reads are denied
     */
    public boolean isReadDenied() {
        return readDenied;
    }

    /**
     * Whether writes are currently denied.
     *
     * @return {@code true} if writes are denied
     */
    public boolean isWriteDenied() {
        return writeDenied;
    }
}
