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

import java.util.List;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("locator-demo")
public class LocatorDemoView extends VerticalLayout {

    public record Person(String name, int age) {
    }

    public LocatorDemoView() {
        TextField name = new TextField("Name");
        name.setId("name");

        Span echo = new Span("");
        echo.setId("echo");

        Button save = new Button("Save",
                e -> echo.setText("Saved: " + name.getValue()));
        save.setId("save");

        Button clear = new Button("Clear", e -> name.setValue(""));
        clear.setAriaLabel("Reset form");

        Grid<Person> people = new Grid<>(Person.class);
        people.setItems(List.of(new Person("Alice", 30), new Person("Bob", 25),
                new Person("Carol", 40)));
        people.addItemClickListener(
                event -> echo.setText("Clicked: " + event.getItem().name()));

        PersonForm personForm = new PersonForm(echo);
        personForm.setId("person-form");

        add(name, save, clear, echo, people, personForm);
    }

    /**
     * Demo composite mirroring an app-defined widget — exercises the
     * custom-locator extension point.
     */
    public static class PersonForm extends Composite<VerticalLayout> {

        public final TextField nameField = new TextField("Full name");
        public final TextField emailField = new TextField("Email address");
        public final Button submit;

        public PersonForm(Span echo) {
            nameField.setId("pf-name");
            emailField.setId("pf-email");
            submit = new Button("Submit",
                    e -> echo.setText("Submitted: " + nameField.getValue()
                            + " <" + emailField.getValue() + ">"));
            submit.setId("pf-submit");
            getContent().add(nameField, emailField, submit);
        }
    }
}
