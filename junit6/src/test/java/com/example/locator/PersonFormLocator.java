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
package com.example.locator;

import com.example.locator.LocatorDemoView.PersonForm;
import com.vaadin.browserless.locator.Locator;
import com.vaadin.flow.component.button.ButtonLocator;
import com.vaadin.flow.component.textfield.TextFieldLocator;

/**
 * App-side locator for a composite. Demonstrates two reuse patterns:
 *
 * <ul>
 * <li>Subclass {@link Locator} with the recursive self-type so filter steps
 * stay chainable.</li>
 * <li>Compose built-in locators (TextField, Button) and scope them with
 * {@code inside(this)} so sub-queries only see descendants of the resolved
 * composite.</li>
 * </ul>
 */
public class PersonFormLocator extends Locator<PersonForm, PersonFormLocator> {

    public PersonFormLocator() {
        super(PersonForm.class);
    }

    public PersonFormLocator fillIn(String name, String email) {
        new TextFieldLocator<String>(String.class).withId("pf-name")
                .inside(this).setValue(name);
        new TextFieldLocator<String>(String.class).withId("pf-email")
                .inside(this).setValue(email);
        return this;
    }

    public void submit() {
        new ButtonLocator().withId("pf-submit").inside(this).click();
    }
}
