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
package com.vaadin.flow.component.textfield;

import com.vaadin.browserless.locator.Locator;

/**
 * Locator/tester for {@link TextField}. Composes the existing
 * {@link TextFieldTester} for action methods, so behaviour (read-only checks,
 * "value came from browser" semantics) stays identical:
 *
 * <pre>
 * getTextField().withId("email").setValue("a@b.c");
 * </pre>
 */
public class TextFieldLocator extends Locator<TextField, TextFieldLocator> {

    /** Creates a locator searching from the UI root. */
    public TextFieldLocator() {
        super(TextField.class);
    }

    /** Sets the value as if the user typed it; rejects read-only fields. */
    public void setValue(String value) {
        new TextFieldTester<TextField, String>(component()).setValue(value);
    }

    /** Returns the current value of the matched field. */
    public String getValue() {
        return component().getValue();
    }

    /** Clears the field via its clear button, when visible. */
    public void clear() {
        new TextFieldTester<TextField, String>(component()).clear();
    }
}
