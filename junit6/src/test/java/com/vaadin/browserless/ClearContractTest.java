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
package com.vaadin.browserless;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxTester;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxTester;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerTester;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePickerTester;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.InputTester;
import com.vaadin.flow.component.shared.HasClearButton;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.NumberFieldTester;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaTester;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldTester;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerTester;
import com.vaadin.flow.router.RouteConfiguration;

/**
 * The emptying contract, asserted identically for every value tester that
 * offers it, so coverage cannot drift apart per component.
 * <p>
 * {@code clear()} models deleting the contents from the keyboard: always
 * available to the user, so it needs no clear button, and it bypasses the
 * set-time validity check — every field here is required, which is exactly the
 * state {@code setValue(emptyValue)} refuses. {@code clickClearButton()} models
 * the button instead, so it additionally requires the button to be on screen.
 * Both require a usable component.
 * <p>
 * Component-specific behaviour (custom empty values, value-change events,
 * selection semantics) stays in the individual tester tests.
 */
@ViewPackages
class ClearContractTest extends BrowserlessTest {

    /**
     * One field under test: how to build it with a value, and how to invoke
     * each emptying method on its tester. {@code clear} is {@code null} for
     * testers that do not offer {@code clear()}, {@code clickClearButton} for
     * components that have no clear button.
     */
    private record Field(String name, Supplier<Component> create,
            Consumer<Component> clear, Consumer<Component> clickClearButton) {
        @Override
        public String toString() {
            return name;
        }
    }

    @BeforeEach
    void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(ClearContractView.class);
        navigate(ClearContractView.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldsWithClear")
    void clear_noClearButtonNeeded_fieldIsEmptied(Field field) {
        Component component = attach(field);
        setClearButtonVisible(component, false);

        field.clear().accept(component);

        Assertions.assertTrue(value(component).isEmpty(),
                "clear() should have emptied the field");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldsWithClear")
    void clear_notUsable_throws(Field field) {
        Component component = attach(field);
        setClearButtonVisible(component, true);
        value(component).setReadOnly(true);

        Assertions.assertThrows(IllegalStateException.class,
                () -> field.clear().accept(component),
                "clear() should require a usable component");
        Assertions.assertFalse(value(component).isEmpty(),
                "value should not have changed");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldsWithClearButton")
    void clickClearButton_buttonVisible_fieldIsEmptied(Field field) {
        Component component = attach(field);
        setClearButtonVisible(component, true);

        field.clickClearButton().accept(component);

        Assertions.assertTrue(value(component).isEmpty(),
                "clickClearButton() should have emptied the field");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldsWithClearButton")
    void clickClearButton_buttonHidden_throws(Field field) {
        Component component = attach(field);
        setClearButtonVisible(component, false);

        Assertions.assertThrows(IllegalStateException.class,
                () -> field.clickClearButton().accept(component),
                "a hidden clear button is not something the user can click");
        Assertions.assertFalse(value(component).isEmpty(),
                "value should not have changed");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldsWithClearButton")
    void clickClearButton_notUsable_throws(Field field) {
        Component component = attach(field);
        setClearButtonVisible(component, true);
        value(component).setReadOnly(true);

        Assertions.assertThrows(IllegalStateException.class,
                () -> field.clickClearButton().accept(component),
                "clickClearButton() should require a usable component");
        Assertions.assertFalse(value(component).isEmpty(),
                "value should not have changed");
    }

    /**
     * Attach the field to the current view and mark it required, so every
     * assertion below runs against the state in which {@code setValue} would
     * refuse the empty value.
     */
    private Component attach(Field field) {
        Component component = field.create().get();
        value(component).setRequiredIndicatorVisible(true);
        getCurrentView().getElement().appendChild(component.getElement());

        Assertions.assertFalse(value(component).isEmpty(),
                "fixture should start with a value");
        return component;
    }

    private static HasValue<?, ?> value(Component component) {
        return (HasValue<?, ?>) component;
    }

    private static void setClearButtonVisible(Component component,
            boolean visible) {
        if (component instanceof HasClearButton clearButton) {
            clearButton.setClearButtonVisible(visible);
        }
    }

    static Stream<Field> fieldsWithClear() {
        return fields().filter(field -> field.clear() != null);
    }

    static Stream<Field> fieldsWithClearButton() {
        return fields().filter(field -> field.clickClearButton() != null);
    }

    private static Stream<Field> fields() {
        return Stream.of(
                new Field("TextField", () -> filled(new TextField(), "text"),
                        c -> new TextFieldTester<>((TextField) c).clear(),
                        c -> new TextFieldTester<>((TextField) c)
                                .clickClearButton()),
                new Field("PasswordField",
                        () -> filled(new PasswordField(), "secret"),
                        c -> new TextFieldTester<>((PasswordField) c).clear(),
                        c -> new TextFieldTester<>((PasswordField) c)
                                .clickClearButton()),
                new Field("EmailField",
                        () -> filled(new EmailField(), "user@example.com"),
                        c -> new TextFieldTester<>((EmailField) c).clear(),
                        c -> new TextFieldTester<>((EmailField) c)
                                .clickClearButton()),
                new Field("BigDecimalField",
                        () -> filled(new BigDecimalField(), BigDecimal.ONE),
                        c -> new TextFieldTester<>((BigDecimalField) c).clear(),
                        c -> new TextFieldTester<>((BigDecimalField) c)
                                .clickClearButton()),
                new Field("TextArea", () -> filled(new TextArea(), "text"),
                        c -> new TextAreaTester<>((TextArea) c).clear(),
                        c -> new TextAreaTester<>((TextArea) c)
                                .clickClearButton()),
                new Field("NumberField", () -> filled(new NumberField(), 1d),
                        c -> new NumberFieldTester<>((NumberField) c).clear(),
                        c -> new NumberFieldTester<>((NumberField) c)
                                .clickClearButton()),
                new Field("IntegerField", () -> filled(new IntegerField(), 1),
                        c -> new NumberFieldTester<>((IntegerField) c).clear(),
                        c -> new NumberFieldTester<>((IntegerField) c)
                                .clickClearButton()),
                new Field("DatePicker",
                        () -> filled(new DatePicker(),
                                LocalDate.of(2026, 5, 28)),
                        c -> new DatePickerTester<>((DatePicker) c).clear(),
                        c -> new DatePickerTester<>((DatePicker) c)
                                .clickClearButton()),
                new Field("TimePicker",
                        () -> filled(new TimePicker(), LocalTime.NOON),
                        c -> new TimePickerTester<>((TimePicker) c).clear(),
                        c -> new TimePickerTester<>((TimePicker) c)
                                .clickClearButton()),
                // DateTimePicker does not implement HasClearButton, so it has
                // no clickClearButton() to exercise.
                new Field("DateTimePicker",
                        () -> filled(new DateTimePicker(),
                                LocalDateTime.of(LocalDate.of(2026, 5, 28),
                                        LocalTime.NOON)),
                        c -> new DateTimePickerTester<>((DateTimePicker) c)
                                .clear(),
                        null),
                // Nor does the html Input.
                new Field("Input", () -> filled(new Input(), "text"),
                        c -> new InputTester((Input) c).clear(), null),
                // The combo boxes model unconditional emptying through
                // selectItem(null) instead of clear().
                new Field("ComboBox", ClearContractTest::comboBox, null,
                        c -> new ComboBoxTester<>((ComboBox<String>) c)
                                .clickClearButton()),
                new Field("MultiSelectComboBox",
                        ClearContractTest::multiSelectComboBox, null,
                        c -> new MultiSelectComboBoxTester<>(
                                (MultiSelectComboBox<String>) c)
                                .clickClearButton()));
    }

    private static <C extends Component & HasValue<?, V>, V> C filled(C field,
            V value) {
        field.setValue(value);
        return field;
    }

    private static Component comboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems("one", "two");
        return filled(comboBox, "one");
    }

    private static Component multiSelectComboBox() {
        MultiSelectComboBox<String> comboBox = new MultiSelectComboBox<>();
        comboBox.setItems("one", "two");
        return filled(comboBox, Set.of("one"));
    }
}
