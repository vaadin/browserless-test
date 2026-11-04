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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.littemplate.LitTemplate;

/**
 * Holds the current {@link TestingLifecycleHook} and the dialog clean-up.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class TestingLifecycleHooks {

    private static TestingLifecycleHook testingLifecycleHook = TestingLifecycleHook.Companion
            .getDefault();

    private TestingLifecycleHooks() {
    }

    /**
     * Checks whether the given component is a Lit or Polymer template.
     *
     * @param component
     *            the component.
     * @return true if the component is a template.
     */
    public static boolean isTemplate(Component component) {
        return component instanceof LitTemplate
                || Utils.isPolymerTemplate(component);
    }

    /**
     * If you need to hook into the testing lifecycle (e.g. you need to wait for
     * any async operations to finish), set your custom implementation via
     * {@link #setTestingLifecycleHook(TestingLifecycleHook)}. See
     * {@link TestingLifecycleHook} for more info on where exactly you can hook
     * into.
     *
     * @return the current hook.
     */
    public static TestingLifecycleHook getTestingLifecycleHook() {
        return testingLifecycleHook;
    }

    /**
     * Sets the testing lifecycle hook, see {@link TestingLifecycleHook}.
     *
     * @param testingLifecycleHook
     *            the hook, not null.
     */
    public static void setTestingLifecycleHook(
            TestingLifecycleHook testingLifecycleHook) {
        TestingLifecycleHooks.testingLifecycleHook = testingLifecycleHook;
    }

    /**
     * Flow Server does not close the dialog when {@code Dialog.close()} is
     * called; instead it tells client-side dialog to close, which then fires
     * event back to the server that the dialog was closed, and removes itself
     * from the DOM. Since there's no browser with browserless testing, we need
     * to cleanup closed dialogs manually, hence this method.
     */
    public static void cleanupDialogs() {
        // Starting with Vaadin 23, nested dialogs are also nested within
        // respective modal dialog within the UI. This is probably related to
        // the "server-side modality curtain" feature. Also see
        // https://github.com/mvysny/karibu-testing/issues/102
        List<Component> dialogs = StreamSupport
                .stream(DepthFirstTreeIterator.walk(UI.getCurrent())
                        .spliterator(), false)
                .filter(ComponentSupports::isClosedDialog)
                .collect(Collectors.toList());
        dialogs.forEach(it -> it.getElement().removeFromParent());
    }
}
