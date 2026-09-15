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
package com.vaadin.flow.component.checkbox;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class CheckboxTesterTest extends BrowserlessTest {

    CheckboxView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(CheckboxView.class);
        view = navigate(CheckboxView.class);
    }

    @Test
    void readOnlyCheckbox_isNotUsable() {
        view.checkbox.setReadOnly(true);
        Assertions.assertFalse(test(view.checkbox).isUsable(),
                "Readonly checkbox should not be usable");
    }

    @Test
    void click_usable_valueChanges() {
        Assertions.assertFalse(view.checkbox.getValue(),
                "Expecting checkbox initial state not to be checked");

        test(view.checkbox).click();
        Assertions.assertTrue(view.checkbox.getValue(),
                "Expecting checkbox to be checked, but was not");

        test(view.checkbox).click();
        Assertions.assertFalse(view.checkbox.getValue(),
                "Expecting checkbox not to be checked, but was");

    }

    @Test
    void click_usable_checkedChangeFired() {
        AtomicBoolean checkedChange = new AtomicBoolean();
        view.checkbox.getElement().addPropertyChangeListener("checked",
                ev -> checkedChange.set(true));

        Assertions.assertFalse(view.checkbox.getValue(),
                "Expecting checkbox not to be checked, but was");

        test(view.checkbox).click();
        Assertions.assertTrue(checkedChange.get(),
                "Expected checked change event to be fired, but was not");
        Assertions.assertTrue(view.checkbox.getValue(),
                "Expecting checkbox not to be checked, but was");
    }

    @Test
    void isChecked_reflectsValue() {
        Assertions.assertFalse(test(view.checkbox).isChecked(),
                "Expecting checkbox initial state not to be checked");

        test(view.checkbox).click();
        Assertions.assertTrue(test(view.checkbox).isChecked(),
                "Expecting checkbox to be checked after click");
    }

    @Test
    void setChecked_onlyChangesStateWhenNeeded() {
        AtomicInteger changes = new AtomicInteger();
        AtomicBoolean fromClient = new AtomicBoolean();
        view.checkbox.addValueChangeListener(ev -> {
            changes.incrementAndGet();
            fromClient.set(ev.isFromClient());
        });

        test(view.checkbox).setChecked(true);
        Assertions.assertTrue(view.checkbox.getValue(),
                "Expecting checkbox to be checked, but was not");
        Assertions.assertEquals(1, changes.get(),
                "Expecting a single value change event");
        Assertions.assertTrue(fromClient.get(),
                "Expecting the value change to come from the client");

        test(view.checkbox).setChecked(true);
        Assertions.assertTrue(view.checkbox.getValue(),
                "Expecting checkbox to stay checked, but was not");
        Assertions.assertEquals(1, changes.get(),
                "Expecting no value change event when already checked");

        test(view.checkbox).setChecked(false);
        Assertions.assertFalse(view.checkbox.getValue(),
                "Expecting checkbox not to be checked, but was");
        Assertions.assertEquals(2, changes.get(),
                "Expecting a value change event when unchecking");
    }

    @Test
    void setChecked_notUsableAndAlreadyInRequestedState_throws() {
        test(view.checkbox).setChecked(true);
        view.checkbox.setEnabled(false);

        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.checkbox).setChecked(true),
                "Expecting a disabled checkbox not to be settable, "
                        + "even to the state it is already in");
    }

    @Test
    void click_disabled_throws() {
        view.checkbox.setEnabled(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.checkbox)::click);
    }

    @Test
    void click_disabledByProperty_throws() {
        view.checkbox.setDisabled(true);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.checkbox)::click);
    }

    @Test
    void click_invisible_throws() {
        view.checkbox.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.checkbox)::click);
    }

    @Test
    void click_readOnly_throws() {
        view.checkbox.setReadOnly(true);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.checkbox)::click);
    }

}
