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

import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;

/**
 * The restrictions a text input applies to what the user can type, shared by
 * the testers of the text based fields.
 * <p>
 * Only the two constraints the browser physically enforces belong here:
 * {@literal maxlength} truncates the extra characters and
 * {@literal allowedCharPattern} filters the keystrokes it does not match, so
 * neither one can end up in the value of a field the user typed into. The
 * validation-only constraints — {@literal minLength}, {@literal pattern},
 * required — are deliberately absent: the browser commits a value that breaks
 * them and leaves the field invalid, which is the state a validation test needs
 * to reach.
 */
final class TextInputConstraints {

    private TextInputConstraints() {
    }

    /**
     * Fails when the given value contains characters the user could not have
     * typed into the component.
     *
     * @param component
     *            the component the value is typed into
     * @param value
     *            the value to type
     * @throws IllegalArgumentException
     *             if the value is longer than {@literal maxlength} or contains
     *             a character {@literal allowedCharPattern} filters out
     */
    static void ensureValueCanBeTyped(Component component, String value) {
        if (value == null || value.isEmpty()) {
            // Emptying a field is always something the user can do.
            return;
        }
        ensureWithinMaxLength(component, value);
        ensureCharactersAreAllowed(component, value);
    }

    private static void ensureWithinMaxLength(Component component,
            String value) {
        // The maxlength property is read off the element rather than through
        // getMaxLength(), which TextField, TextArea, PasswordField and
        // EmailField each declare separately instead of TextFieldBase, and
        // which reports zero both for an unset limit and for a limit of zero.
        // The element property is what the component itself branches on, in a
        // private hasMaxLength(); a getter telling the two apart on
        // TextFieldBase belongs upstream in vaadin/flow-components.
        if (component.getElement().getProperty("maxlength") == null) {
            return;
        }
        final int maxLength = (int) component.getElement()
                .getProperty("maxlength", 0d);
        // The browser ignores a negative maxlength as it ignores an unset one.
        if (maxLength >= 0 && value.length() > maxLength) {
            throw new IllegalArgumentException("Value '" + value + "' is "
                    + value.length() + " characters long, but the field takes "
                    + "at most " + maxLength
                    + ". The browser truncates the characters over the limit, "
                    + "so the user cannot produce this value.");
        }
    }

    private static void ensureCharactersAreAllowed(Component component,
            String value) {
        if (!(component instanceof HasAllowedCharPattern field)) {
            return;
        }
        final String allowedCharPattern = field.getAllowedCharPattern();
        if (allowedCharPattern.isEmpty()) {
            return;
        }
        final Pattern allowedChar;
        try {
            allowedChar = Pattern.compile(allowedCharPattern);
        } catch (PatternSyntaxException e) {
            // The browser only warns about a pattern it cannot compile and
            // then lets every character through.
            return;
        }
        // The pattern matches one character at a time, so it is applied per
        // code point, the way the web component filters the keystrokes.
        final OptionalInt rejected = value.codePoints()
                .filter(codePoint -> !allowedChar.matcher(toString(codePoint))
                        .matches())
                .findFirst();
        if (rejected.isPresent()) {
            throw new IllegalArgumentException("Value '" + value
                    + "' contains the character '"
                    + toString(rejected.getAsInt())
                    + "', which the allowed char pattern '" + allowedCharPattern
                    + "' does not match. The browser "
                    + "filters out such a keystroke, so the user cannot "
                    + "produce this value.");
        }
    }

    private static String toString(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
