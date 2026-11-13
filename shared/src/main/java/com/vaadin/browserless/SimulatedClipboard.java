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

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.server.VaadinSession;

/**
 * The clipboard of a simulated user's browser in a browserless test.
 * <p>
 * A copy the application wires with {@link Clipboard#onClick} writes here when
 * the test clicks the component, the way the browser writes to the system
 * clipboard, so a test can assert what the user would be able to paste:
 *
 * <pre>
 * assertNull(SimulatedClipboard.get().getText());
 * test(copyButton).click();
 * assertEquals("Hello", SimulatedClipboard.get().getText());
 * </pre>
 * <p>
 * Like a browser's clipboard, it belongs to the user, not to a window: every
 * window of the same user shares it, and each user has their own. A write
 * replaces the whole content, so writing only text clears earlier HTML.
 *
 * @see BrowserlessUserContext#getClipboard()
 */
public final class SimulatedClipboard implements Serializable {

    private @Nullable String text;
    private @Nullable String html;

    private SimulatedClipboard() {
    }

    /**
     * Gets the clipboard of the current user, that is of the current
     * {@link VaadinSession}.
     *
     * @return the clipboard, not {@code null}
     * @throws IllegalStateException
     *             if there is no current Vaadin session
     */
    public static SimulatedClipboard get() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            throw new IllegalStateException(
                    "There is no current Vaadin session. Get the clipboard"
                            + " in a browserless test, or from"
                            + " BrowserlessUserContext.getClipboard()");
        }
        return of(session);
    }

    static SimulatedClipboard of(VaadinSession session) {
        SimulatedClipboard clipboard = session
                .getAttribute(SimulatedClipboard.class);
        if (clipboard == null) {
            clipboard = new SimulatedClipboard();
            session.setAttribute(SimulatedClipboard.class, clipboard);
        }
        return clipboard;
    }

    /**
     * Gets the plain text on the clipboard.
     *
     * @return the text, or {@code null} if the clipboard holds no plain text
     */
    public @Nullable String getText() {
        return text;
    }

    /**
     * Gets the HTML on the clipboard.
     *
     * @return the HTML, or {@code null} if the clipboard holds no HTML
     */
    public @Nullable String getHtml() {
        return html;
    }

    /**
     * Replaces the content of the clipboard, as a write by the browser does.
     *
     * @param text
     *            the plain text, or {@code null} for none
     * @param html
     *            the HTML, or {@code null} for none
     */
    void write(@Nullable String text, @Nullable String html) {
        this.text = text;
        this.html = html;
    }
}
