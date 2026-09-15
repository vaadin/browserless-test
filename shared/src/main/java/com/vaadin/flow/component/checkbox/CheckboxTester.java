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

import java.util.function.Consumer;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.MetaKeys;
import com.vaadin.browserless.Tests;

/**
 * Tester for Checkbox components.
 *
 * @param <T>
 *            component type
 * @since 1.0
 */
@Tests(Checkbox.class)
public class CheckboxTester<T extends Checkbox> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public CheckboxTester(T component) {
        super(component);
    }

    /**
     * Checks whether the checkbox is currently checked.
     *
     * @return {@code true} if the checkbox is checked, {@code false} otherwise
     */
    public boolean isChecked() {
        return getComponent().getValue();
    }

    /**
     * Sets the checkbox to the given checked state, as a user clicking it
     * would.
     * <p>
     * Does nothing if the checkbox is already in the requested state, but the
     * checkbox must be usable in either case.
     *
     * @param checked
     *            {@code true} to check the checkbox, {@code false} to uncheck
     *            it
     */
    public void setChecked(boolean checked) {
        ensureComponentIsUsable();
        if (isChecked() != checked) {
            click();
        }
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && !getComponent().isDisabledBoolean();
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        if (getComponent().isDisabledBoolean()) {
            collector.accept("disabled");
        }
    }

    @Override
    public void click(int button, MetaKeys metaKeys) {
        super.click(button, metaKeys);
        T checkbox = getComponent();
        setValueAsUser(!checkbox.getValue());
    }
}
