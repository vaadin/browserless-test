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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonTester;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class DialogTesterTest extends BrowserlessTest {

    DialogView view;
    DialogTester dialog_;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(DialogView.class);
        view = navigate(DialogView.class);
        dialog_ = test(view.dialog);
    }

    @Test
    void openAndClose_dialogIsAttachedAndDetached() {
        dialog_.open();
        Assertions.assertTrue(dialog_.isUsable(),
                "Dialog should be attached on open");

        dialog_.close();
        Assertions.assertFalse(dialog_.isUsable(),
                "Dialog should not be usable after close");
        Assertions.assertFalse(view.dialog.isAttached(),
                "Dialog should be detached on close");
    }

    @Test
    void programmaticallyClose_dialogIsDetached() {
        dialog_.open();

        view.dialog.close();

        Assertions.assertFalse(view.dialog.isAttached(),
                "Dialog should be detached on close");
    }

    @Test
    void modalDialog_visual_doNotBlockUIComponents() {
        view.dialog.setModality(ModalityMode.VISUAL);
        dialog_.open();
        ButtonTester<Button> button_ = test(view.button);
        Assertions.assertTrue(button_.isUsable(),
                "Default VISUAL modal dialog should not block button");
    }

    @Test
    void modalDialog_strict_blocksUIComponents() {
        view.dialog.setModality(ModalityMode.STRICT);
        dialog_.open();
        ButtonTester<Button> button_ = test(view.button);
        Assertions.assertFalse(button_.isUsable(),
                "Dialog should be modal by default blocking button");

        dialog_.close();

        Assertions.assertTrue(button_.isUsable(),
                "Closing dialog should enable button");
    }

    @Test
    void nonModalDialog_UIComponentsUsable() {
        view.dialog.setModality(ModalityMode.MODELESS);
        dialog_.open();
        ButtonTester<Button> button_ = test(view.button);
        Assertions.assertTrue(button_.isUsable(),
                "Non-modal dialog should not block button");
    }

    @Test
    void pressEscape_closesDialogAsUser() {
        dialog_.open();
        AtomicBoolean closedFromClient = new AtomicBoolean();
        view.dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                closedFromClient.set(event.isFromClient());
            }
        });

        dialog_.pressEscape();

        Assertions.assertFalse(dialog_.isOpen(),
                "Dialog should be closed by pressing Escape");
        Assertions.assertTrue(closedFromClient.get(),
                "Escape should close the dialog as a client-side close");
    }

    @Test
    void pressEscape_closeOnEscDisabled_throws() {
        view.dialog.setCloseOnEsc(false);
        dialog_.open();

        Assertions.assertThrows(IllegalStateException.class,
                () -> dialog_.pressEscape(),
                "Escape should not close a dialog with close-on-Esc disabled");
        Assertions.assertTrue(dialog_.isOpen(), "Dialog should stay open");
    }

    @Test
    void pressEscape_dialogNotOpen_throws() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> dialog_.pressEscape(),
                "Escape should not be possible on a closed dialog");
    }

    @Test
    void pressEscape_closeActionListenerRegistered_firesEventWithoutClosing() {
        AtomicInteger closeActions = new AtomicInteger();
        view.dialog.addDialogCloseActionListener(
                event -> closeActions.incrementAndGet());
        dialog_.open();

        dialog_.pressEscape();

        Assertions.assertEquals(1, closeActions.get(),
                "Escape should fire a DialogCloseActionEvent");
        Assertions.assertTrue(dialog_.isOpen(),
                "Dialog should stay open until the close action listener closes it");
    }

    @Test
    void clickOutside_closesDialogAsUser() {
        dialog_.open();

        dialog_.clickOutside();

        Assertions.assertFalse(dialog_.isOpen(),
                "Dialog should be closed by clicking outside");
    }

    @Test
    void clickOutside_closeOnOutsideClickDisabled_throws() {
        view.dialog.setCloseOnOutsideClick(false);
        dialog_.open();

        Assertions.assertThrows(IllegalStateException.class,
                () -> dialog_.clickOutside(),
                "Clicking outside should not close a dialog with close-on-outside-click disabled");
        Assertions.assertTrue(dialog_.isOpen(), "Dialog should stay open");
    }

    @Test
    void close_closesFromServerIgnoringUserRestrictions() {
        AtomicInteger closeActions = new AtomicInteger();
        view.dialog.setCloseOnEsc(false);
        view.dialog.setCloseOnOutsideClick(false);
        view.dialog.addDialogCloseActionListener(
                event -> closeActions.incrementAndGet());
        dialog_.open();

        dialog_.close();

        Assertions.assertFalse(dialog_.isOpen(),
                "close() should close the dialog from the server side");
        Assertions.assertEquals(0, closeActions.get(),
                "close() should not fire a DialogCloseActionEvent");
    }

    @Test
    void pressEscape_dialogBehindStrictModalDialog_throws() {
        dialog_.open();
        Dialog blocking = new Dialog();
        blocking.setModality(ModalityMode.STRICT);
        test(blocking).open();

        Assertions.assertThrows(IllegalStateException.class,
                () -> dialog_.pressEscape(),
                "Escape should not reach a dialog behind a strict modal dialog");
        Assertions.assertTrue(dialog_.isOpen(), "Dialog should stay open");
    }

}
