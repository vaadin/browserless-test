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

/**
 * Holds the current {@link TestingLifecycleHook}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class TestingLifecycleHooks {

    private static TestingLifecycleHook testingLifecycleHook = TestingLifecycleHook.Companion
            .getDefault();

    private TestingLifecycleHooks() {
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
}
