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
package com.vaadin.flow.component.dialog;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * @since 1.0
 */
@Tests(Dialog.class)
public class DialogTester extends ComponentTester<Dialog> {
    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public DialogTester(Dialog component) {
        super(component);
    }

    /**
     * Open the dialog.
     */
    public void open() {
        getComponent().open();
        roundTrip();
    }

    /**
     * Close the dialog from the server side, like the application calling
     * {@link Dialog#close()}.
     * <p>
     * This is not something the user can do: the dialog is closed regardless of
     * {@link Dialog#isCloseOnEsc()} and {@link Dialog#isCloseOnOutsideClick()},
     * and no {@link Dialog.DialogCloseActionEvent} is fired, so a close action
     * listener cannot veto it. To simulate the user dismissing the dialog, use
     * {@link #pressEscape()} or {@link #clickOutside()} instead.
     */
    public void close() {
        getComponent().close();
        roundTrip();
    }

    /**
     * Simulates pressing Escape to close the dialog.
     * <p>
     * If the application has registered a close action listener, a
     * {@link Dialog.DialogCloseActionEvent} is fired instead of closing the
     * dialog, leaving it up to the listener whether to close it.
     *
     * @throws IllegalStateException
     *             if the dialog is not open or close-on-Esc is disabled
     */
    public void pressEscape() {
        ensureDialogIsOpen();
        if (!getComponent().isCloseOnEsc()) {
            throw new IllegalStateException(
                    "close-on-Esc is disabled for this dialog");
        }
        closeFromClient();
    }

    /**
     * Simulates clicking outside the dialog to close it.
     * <p>
     * If the application has registered a close action listener, a
     * {@link Dialog.DialogCloseActionEvent} is fired instead of closing the
     * dialog, leaving it up to the listener whether to close it.
     *
     * @throws IllegalStateException
     *             if the dialog is not open or close-on-outside-click is
     *             disabled
     */
    public void clickOutside() {
        ensureDialogIsOpen();
        if (!getComponent().isCloseOnOutsideClick()) {
            throw new IllegalStateException(
                    "close-on-outside-click is disabled for this dialog");
        }
        closeFromClient();
    }

    /**
     * Check if the dialog is open.
     *
     * @return true if the dialog is open
     */
    public boolean isOpen() {
        return getComponent().isOpened();
    }

    private void closeFromClient() {
        // The very entry point the web component invokes when the user
        // dismisses the dialog: it fires DialogCloseActionEvent when a close
        // action listener is registered, and closes the dialog otherwise.
        getComponent().handleClientClose();
        roundTrip();
    }

    private void ensureDialogIsOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("dialog is not open");
        }
    }
}
