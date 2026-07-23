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
 * Tester for Switch components.
 *
 * @param <T>
 *            component type
 */
@Tests(Switch.class)
public class SwitchTester<T extends Switch> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public SwitchTester(T component) {
        super(component);
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && !getComponent().isReadOnly();
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        if (getComponent().isReadOnly()) {
            collector.accept("read only");
        }
    }

    @Override
    public void click(int button, MetaKeys metaKeys) {
        super.click(button, metaKeys);
        T field = getComponent();
        setValueAsUser(!field.getValue());
    }
}
