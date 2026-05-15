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
package com.vaadin.flow.component.button;

import com.vaadin.browserless.Clickable;
import com.vaadin.browserless.locator.Locator;

/**
 * Locator/tester for {@link Button}. Combines the filter chain inherited from
 * {@link Locator} with the click actions inherited from {@link Clickable}, so a
 * full find-and-act sequence is one fluent chain:
 *
 * <pre>
 * getButton().withCaption("Save").click();
 * </pre>
 */
public class ButtonLocator extends Locator<Button, ButtonLocator>
        implements Clickable<Button> {

    /** Creates a locator searching from the UI root. */
    public ButtonLocator() {
        super(Button.class);
    }

    @Override
    public Button getComponent() {
        return component();
    }

    @Override
    public void ensureComponentIsUsable() {
        new ButtonTester<>(getComponent()).ensureComponentIsUsable();
    }
}
