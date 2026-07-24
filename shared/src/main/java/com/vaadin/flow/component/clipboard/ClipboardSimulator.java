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
import java.util.Map;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.internal.UploadSimulation;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.JsonConstants;

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
    private long nextPasteId = 1;

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
     * Simulates a paste gesture onto the given component, delivering this
     * clipboard's current contents to any {@link Clipboard#onPaste} listener
     * registered on it (or a descendant, since {@code paste} bubbles).
     *
     * @param target
     *            the component to paste onto, not {@code null}
     */
    public void pasteInto(Component target) {
        firePaste(target, text, html);
    }

    /**
     * Simulates a paste gesture onto the given component with explicit
     * contents, without changing this clipboard's stored contents. Useful for a
     * one-off paste distinct from the clipboard's current state.
     *
     * @param target
     *            the component to paste onto, not {@code null}
     * @param text
     *            the {@code text/plain} contents to paste, or {@code null}
     * @param html
     *            the {@code text/html} contents to paste, or {@code null}
     */
    public void pasteInto(Component target, @Nullable String text,
            @Nullable String html) {
        firePaste(target, text, html);
    }

    /**
     * Dispatches a {@code paste} DOM event to the component, using the exact
     * event-data keys {@link Clipboard#onPaste} reads and mapping the paste
     * target to the component so {@code PasteEvent#getTargetElement()}
     * resolves. The client-side skip-editable filter of {@code onPaste} is not
     * evaluated — the listener always receives the event.
     */
    private static void firePaste(Component target, @Nullable String text,
            @Nullable String html) {
        Element element = target.getElement();
        ObjectNode eventData = JacksonUtils.createObjectNode();
        putNullable(eventData, Clipboard.PASTE_TEXT_EXPR, text);
        putNullable(eventData, Clipboard.PASTE_HTML_EXPR, html);
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                element.getNode().getId());
        DomEvent event = new DomEvent(element, "paste", eventData);
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(event);
    }

    private static void putNullable(ObjectNode data, String key,
            @Nullable String value) {
        if (value == null) {
            data.putNull(key);
        } else {
            data.put(key, value);
        }
    }

    /**
     * Simulates a paste gesture onto the single component matched by the given
     * query, delivering this clipboard's current contents to its
     * {@link Clipboard#onPaste} listener. Convenience for the common
     * find-then-paste flow.
     *
     * @param target
     *            a query resolving to exactly one component to paste onto, not
     *            {@code null}
     * @throws java.util.NoSuchElementException
     *             if the query does not match exactly one component
     */
    public void pasteInto(ComponentQuery<? extends Component> target) {
        pasteInto(target.single());
    }

    /**
     * Simulates a paste gesture onto the single component matched by the given
     * query with explicit contents, without changing this clipboard's stored
     * contents.
     *
     * @param target
     *            a query resolving to exactly one component to paste onto, not
     *            {@code null}
     * @param text
     *            the {@code text/plain} contents to paste, or {@code null}
     * @param html
     *            the {@code text/html} contents to paste, or {@code null}
     * @throws java.util.NoSuchElementException
     *             if the query does not match exactly one component
     */
    public void pasteInto(ComponentQuery<? extends Component> target,
            @Nullable String text, @Nullable String html) {
        pasteInto(target.single(), text, html);
    }

    /**
     * Simulates a file paste onto the given component, delivering each file to
     * its {@link Clipboard#onFilePaste} handler as one upload (carrying the
     * paste-id and file-count headers), then firing the paste-finished event so
     * queued UI changes flush.
     *
     * @param target
     *            the component to paste onto, not {@code null}
     * @param files
     *            the pasted files, at least one
     * @throws IllegalArgumentException
     *             if no files are given
     * @throws IllegalStateException
     *             if no {@code onFilePaste} handler is registered on the
     *             component
     */
    public void pasteFilesInto(Component target, PastedFile... files) {
        firePastedFiles(target, files);
    }

    /**
     * Simulates a file paste onto the single component matched by the given
     * query. Convenience for the common find-then-paste flow.
     *
     * @param target
     *            a query resolving to exactly one component to paste onto, not
     *            {@code null}
     * @param files
     *            the pasted files, at least one
     * @throws java.util.NoSuchElementException
     *             if the query does not match exactly one component
     */
    public void pasteFilesInto(ComponentQuery<? extends Component> target,
            PastedFile... files) {
        firePastedFiles(target.single(), files);
    }

    private void firePastedFiles(Component target, PastedFile... files) {
        if (files.length == 0) {
            throw new IllegalArgumentException("At least one file is required");
        }
        Element element = target.getElement();
        String url = element.getAttributeNames()
                .filter(name -> name
                        .startsWith(Clipboard.PASTE_UPLOAD_ATTRIBUTE_PREFIX))
                .map(element::getAttribute).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No Clipboard.onFilePaste registration found on "
                                + target));
        UploadHandler handler = UploadSimulation.resolveUploadHandler(url);
        Map<String, String> headers = Map.of(Clipboard.PASTE_ID_HEADER,
                Long.toString(nextPasteId++), Clipboard.PASTE_FILE_COUNT_HEADER,
                Integer.toString(files.length));
        // Each file is a separate upload carrying the paste headers, exactly as
        // the client posts them; the handler (or its PasteFileHandler wrapper)
        // reads the headers to correlate the paste.
        UploadSimulation.withRequestHeaders(headers, () -> {
            for (PastedFile file : files) {
                UploadSimulation.invokeUpload(handler, element,
                        VaadinRequest.getCurrent(), file.fileName(),
                        file.contentType(), file.content());
            }
        });
        RuntimeException caught = UploadSimulation.runUIQueue();
        // The client dispatches this once all uploads settle; the server
        // listener forces the round trip that flushes UI.access changes.
        element.getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(element,
                        Clipboard.FILE_PASTE_FINISHED_EVENT,
                        JacksonUtils.createObjectNode()));
        if (caught != null) {
            throw caught;
        }
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
