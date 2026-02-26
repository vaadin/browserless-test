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
package com.vaadin.flow.component.popover;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages
class PopoverTesterTest extends BrowserlessTest {

    PopoverView view;
    PopoverTester popover_;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(PopoverView.class);
        view = navigate(PopoverView.class);
        popover_ = test(view.popover);
    }

    @Test
    void openAndClose_popoverIsUsableAndNotUsable() {
        popover_.open();
        assertTrue(popover_.isUsable(), "Popover should be usable when open");

        popover_.close();
        assertFalse(popover_.isUsable(),
                "Popover should not be usable after close");
    }

    @Test
    void modalPopover_blocksUIComponents() {
        view.popover.setModal(true);
        popover_.open();
        ComponentTester<NativeButton> button_ = test(view.button);
        assertFalse(button_.isUsable(), "Modal popover should block button");

        popover_.close();

        assertTrue(button_.isUsable(), "Closing popover should enable button");
    }

    @Test
    void nonModalPopover_UIComponentsUsable() {
        view.popover.setModal(false);
        popover_.open();
        ComponentTester<NativeButton> button_ = test(view.button);
        assertTrue(button_.isUsable(),
                "Non-modal popover should not block button");
    }

    @Test
    void isOpen_returnsCorrectState() {
        assertFalse(popover_.isOpen(), "Popover should be closed initially");

        popover_.open();
        assertTrue(popover_.isOpen(), "Popover should be open after open()");

        popover_.close();
        assertFalse(popover_.isOpen(),
                "Popover should be closed after close()");
    }

    @Test
    void clickTarget_opensPopover() {
        popover_.clickTarget();
        assertTrue(popover_.isOpen(),
                "Popover should be open after clickTarget()");
    }

    @Test
    void clickTarget_throwsWhenClickTriggerDisabled() {
        view.popover.setOpenOnClick(false);
        assertThrows(IllegalStateException.class, () -> popover_.clickTarget());
    }

    @Test
    void hoverTarget_opensPopover() {
        view.popover.setOpenOnHover(true);
        popover_.hoverTarget();
        assertTrue(popover_.isOpen(),
                "Popover should be open after hoverTarget()");
    }

    @Test
    void hoverTarget_throwsWhenHoverTriggerDisabled() {
        assertThrows(IllegalStateException.class, () -> popover_.hoverTarget());
    }

    @Test
    void focusTarget_opensPopover() {
        view.popover.setOpenOnFocus(true);
        popover_.focusTarget();
        assertTrue(popover_.isOpen(),
                "Popover should be open after focusTarget()");
    }

    @Test
    void focusTarget_throwsWhenFocusTriggerDisabled() {
        assertThrows(IllegalStateException.class, () -> popover_.focusTarget());
    }

    @Test
    void triggerMethods_throwWhenNoTarget() {
        view.popover.setTarget(null);
        view.popover.setOpenOnClick(true);
        view.popover.setOpenOnHover(true);
        view.popover.setOpenOnFocus(true);

        assertThrows(IllegalStateException.class, () -> popover_.clickTarget());
        assertThrows(IllegalStateException.class, () -> popover_.hoverTarget());
        assertThrows(IllegalStateException.class, () -> popover_.focusTarget());
    }

    @Test
    void pressEscape_closesPopover() {
        popover_.open();
        popover_.pressEscape();
        assertFalse(popover_.isOpen(),
                "Popover should be closed after pressEscape()");
    }

    @Test
    void pressEscape_throwsWhenCloseOnEscDisabled() {
        view.popover.setCloseOnEsc(false);
        popover_.open();
        assertThrows(IllegalStateException.class, () -> popover_.pressEscape());
    }

    @Test
    void pressEscape_throwsWhenNotOpen() {
        assertThrows(IllegalStateException.class, () -> popover_.pressEscape());
    }

    @Test
    void clickOutside_closesPopover() {
        popover_.open();
        popover_.clickOutside();
        assertFalse(popover_.isOpen(),
                "Popover should be closed after clickOutside()");
    }

    @Test
    void clickOutside_throwsWhenCloseOnOutsideClickDisabled() {
        view.popover.setCloseOnOutsideClick(false);
        popover_.open();
        assertThrows(IllegalStateException.class,
                () -> popover_.clickOutside());
    }

    @Test
    void clickOutside_throwsWhenNotOpen() {
        assertThrows(IllegalStateException.class,
                () -> popover_.clickOutside());
    }
}
