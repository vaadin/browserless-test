/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;

/**
 * Mutable holder for the global [TestingLifecycleHook] and lifecycle-related helpers.
 *
 * If you need to hook into the testing lifecycle (e.g. you need to wait for any async operations to finish),
 * set your custom implementation here.
 */
public final class TestingLifecycleHooks {

    private TestingLifecycleHooks() {
    }

    /**
     * The global [TestingLifecycleHook] currently in effect.
     */
    public static TestingLifecycleHook current = TestingLifecycleHook.DEFAULT;

    private static final Class<?> _ConfirmDialog_Class = Utils.findClass(
            "com.vaadin.flow.component.confirmdialog.ConfirmDialog");

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
     * Checks whether given [component] is a dialog and needs to be removed from the UI.
     * See [cleanupDialogs] for more info.
     */
    private static boolean isDialogAndNeedsRemoval(Component component) {
        if (component instanceof Dialog && !((Dialog) component).isOpened()) {
            return true;
        }
        // also support ConfirmDialog. But be careful - this is a Pro component and may not be on classpath.
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
     * Flow Server does not close the dialog when [Dialog.close] is called; instead it tells client-side dialog to close,
     * which then fires event back to the server that the dialog was closed, and removes itself from the DOM.
     * Since there's no browser with browserless testing, we need to cleanup closed dialogs manually, hence this method.
     */
    public static void cleanupDialogs() {
        // Starting with Vaadin 23, nested dialogs are also nested within respective
        // modal dialog within the UI. This is probably related to the "server-side
        // modality curtain" feature. Also see https://github.com/mvysny/karibu-testing/issues/102
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
