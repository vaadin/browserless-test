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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;

/**
 * Holds the global {@link TestingLifecycleHook} and the lifecycle helpers that
 * do not belong on the interface itself.
 * <p>
 * To hook into the testing lifecycle, e.g. to wait for an asynchronous
 * operation to finish before a lookup, install an implementation with
 * {@link #setCurrent(TestingLifecycleHook)}. The hook is global and is
 * <em>not</em> reset between tests, so a test that installs one has to restore
 * the previous hook afterwards.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class TestingLifecycleHooks {

    private TestingLifecycleHooks() {
    }

    private static TestingLifecycleHook current = TestingLifecycleHook.DEFAULT;

    /**
     * Returns the hook currently in effect. Defaults to
     * {@link TestingLifecycleHook#DEFAULT}.
     *
     * @return the current hook, never {@code null}
     */
    public static TestingLifecycleHook getCurrent() {
        return current;
    }

    /**
     * Installs the hook to use for every following lookup.
     *
     * @param current
     *            the hook to install
     */
    public static void setCurrent(TestingLifecycleHook current) {
        TestingLifecycleHooks.current = current;
    }

    private static final Class<?> _ConfirmDialog_Class = Utils
            .findClass("com.vaadin.flow.component.confirmdialog.ConfirmDialog");

    private static final Method _ConfirmDialog_isOpened;
    static {
        Method m = null;
        if (_ConfirmDialog_Class != null) {
            try {
                m = _ConfirmDialog_Class.getMethod("isOpened");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        _ConfirmDialog_isOpened = m;
    }

    /**
     * Checks whether given {@code component} is a dialog and needs to be
     * removed from the UI. See {@link #cleanupDialogs} for more info.
     */
    private static boolean isDialogAndNeedsRemoval(Component component) {
        if (component instanceof Dialog && !((Dialog) component).isOpened()) {
            return true;
        }
        // also support ConfirmDialog. But be careful - this is a Pro component
        // and may not be on classpath.
        if (_ConfirmDialog_Class != null && _ConfirmDialog_isOpened != null
                && _ConfirmDialog_Class.isInstance(component)) {
            try {
                if (!(Boolean) _ConfirmDialog_isOpened.invoke(component)) {
                    return true;
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * Flow Server does not close the dialog when {@link Dialog#close} is
     * called; instead it tells client-side dialog to close, which then fires
     * event back to the server that the dialog was closed, and removes itself
     * from the DOM. Since there's no browser with browserless testing, we need
     * to cleanup closed dialogs manually, hence this method.
     */
    public static void cleanupDialogs() {
        // Starting with Vaadin 23, nested dialogs are also nested within
        // respective
        // modal dialog within the UI. This is probably related to the
        // "server-side
        // modality curtain" feature. Also see
        // https://github.com/mvysny/karibu-testing/issues/102
        List<Component> toRemove = new ArrayList<>();
        for (Component c : DepthFirstTreeIterator.walk(UI.getCurrent())) {
            if (isDialogAndNeedsRemoval(c)) {
                toRemove.add(c);
            }
        }
        for (Component c : toRemove) {
            c.getElement().removeFromParent();
        }
    }
}
