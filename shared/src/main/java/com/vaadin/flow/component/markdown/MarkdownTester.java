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
package com.vaadin.flow.component.markdown;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for Markdown components.
 *
 * @param <T>
 *            component type
 * @since 1.1
 */
@Tests(Markdown.class)
public class MarkdownTester<T extends Markdown> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public MarkdownTester(T component) {
        super(component);
    }

    /**
     * Gets the Markdown source content rendered by the component.
     *
     * @return the Markdown content, never {@code null}
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public String getContent() {
        ensureComponentIsUsable();
        return getComponent().getContent();
    }

}
