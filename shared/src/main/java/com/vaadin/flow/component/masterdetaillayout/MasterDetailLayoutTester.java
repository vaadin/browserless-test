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
package com.vaadin.flow.component.masterdetaillayout;

import org.jetbrains.annotations.Nullable;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;

/**
 * Tester for MasterDetailLayout components.
 * <p>
 * Provides access to the state of the layout as a user would observe it: the
 * component shown in the master area, the component shown in the detail area,
 * and the detail placeholder that is displayed when no detail content is set.
 *
 * @param <T>
 *            component type
 * @since 1.1
 */
@Tests(MasterDetailLayout.class)
public class MasterDetailLayoutTester<T extends MasterDetailLayout>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public MasterDetailLayoutTester(T component) {
        super(component);
    }

    /**
     * Gets the component currently shown in the master area.
     *
     * @return the component in the master area, or {@code null} if there is no
     *         component in the master area
     * @throws IllegalStateException
     *             if the layout is not visible to the user
     */
    @Nullable
    public Component getMaster() {
        ensureVisible();
        return getComponent().getMaster();
    }

    /**
     * Gets the component currently shown in the detail area.
     *
     * @return the component in the detail area, or {@code null} if no detail
     *         content is set
     * @throws IllegalStateException
     *             if the layout is not visible to the user
     */
    @Nullable
    public Component getDetail() {
        ensureVisible();
        return getComponent().getDetail();
    }

    /**
     * Gets the placeholder component currently shown in the detail area. The
     * placeholder is only shown to the user while no detail content is set.
     *
     * @return the placeholder component shown in the detail area, or
     *         {@code null} if no placeholder is set
     * @throws IllegalStateException
     *             if the layout is not visible to the user, or if detail
     *             content is set so the placeholder is not shown
     */
    @Nullable
    public Component getDetailPlaceholder() {
        ensureVisible();
        if (getComponent().getDetail() != null) {
            throw new IllegalStateException(
                    "Detail content is set so the placeholder is not shown to the user");
        }
        return getComponent().getDetailPlaceholder();
    }

    /**
     * Checks whether the detail placeholder is currently shown to the user. A
     * placeholder is shown when one is set and there is no detail content.
     *
     * @return {@code true} if the placeholder is shown, {@code false} otherwise
     * @throws IllegalStateException
     *             if the layout is not visible to the user
     */
    public boolean isDetailPlaceholderVisible() {
        ensureVisible();
        return getComponent().getDetail() == null
                && getComponent().getDetailPlaceholder() != null;
    }
}
