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
package com.vaadin.flow.component;

import java.util.function.Consumer;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for Text components.
 * <p>
 * {@link Text} renders as a text node rather than an HTML element, so it has no
 * visibility of its own ({@link Text#isVisible()} throws). Usability is
 * therefore derived from the component being attached and enabled and from the
 * effective visibility of its parent subtree.
 *
 * @param <T>
 *            component type
 */
@Tests(Text.class)
public class TextTester<T extends Text> extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public TextTester(T component) {
        super(component);
    }

    /**
     * Gets the text of the component.
     *
     * @return the text, never {@code null}
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public String getText() {
        ensureComponentIsUsable();
        return getComponent().getText();
    }

    @Override
    public boolean isUsable() {
        T text = getComponent();
        return text.getElement().isEnabled() && text.isAttached()
                && isParentEffectivelyVisible(text)
                && !text.getElement().getNode().isInert();
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        T text = getComponent();
        if (!text.getElement().isEnabled()) {
            collector.accept("not enabled");
        }
        if (!text.isAttached()) {
            collector.accept("not attached");
        }
        if (!isParentEffectivelyVisible(text)) {
            collector.accept("part of a not visible subtree");
        }
        if (text.getElement().getNode().isInert()) {
            collector.accept("behind a modality curtain");
        }
    }

    private static boolean isParentEffectivelyVisible(Component component) {
        return component.getParent().map(parent -> parent.isVisible()
                && isParentEffectivelyVisible(parent)).orElse(true);
    }

}
