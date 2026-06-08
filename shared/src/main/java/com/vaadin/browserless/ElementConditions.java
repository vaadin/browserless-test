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

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import org.jsoup.Jsoup;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.dom.Element;

/**
 * A collection of commons predicates to be used as {@link ComponentQuery}
 * conditions.
 *
 * @see ComponentQuery#withCondition(Predicate)
 */
public final class ElementConditions {

    private ElementConditions() {
        throw new AssertionError("Must not be instantiated");
    }

    /**
     * Checks if text content of the component contains the given text.
     *
     * Input text is compared with value obtained either by
     * {@link HasText#getText()}, {@link Element#getText()} if element is a text
     * node, or the normalized version of {@link Html#getInnerHtml()}. In all
     * other cases {@link Element#getTextRecursively()} is used, but in this
     * case text from nested elements is concatenated without space separators.
     * The comparison is case-sensitive.
     *
     * For {@link Html} components the {@literal innerHTML} tags are stripped
     * and whitespace is normalized and trimmed.
     *
     * For example, given HTML
     *
     * <pre>
     * <p>
     * Hello  <b>there</b> now!
     * </p>
     * </pre>
     *
     * the text that will be checked will be {@literal  Hello there now!}.
     *
     * @param text
     *            the text the component is expected to have as its content. Not
     *            {@literal null}.
     * @return this element query instance for chaining
     * @see HasText#getText()
     * @see Element#isTextNode()
     * @see Element#getText()
     * @see Element#getTextRecursively()
     * @see Html#getInnerHtml()
     */
    public static <T extends Component> Predicate<T> containsText(String text) {
        return containsText(text, false);
    }

    /**
     * Checks if text content of the component contains the given text.
     *
     * Input text is compared with value obtained either by
     * {@link HasText#getText()}, {@link Element#getText()} if element is a text
     * node, or {@link Html#getInnerHtml()}. In all other cases
     * {@link Element#getTextRecursively()} is used, but in this case text from
     * nested elements is concatenated without space separators.
     *
     * For {@link Html} components the {@literal innerHTML} tags are stripped
     * and whitespace is normalized and trimmed.
     *
     * For example, given HTML
     *
     * <pre>
     * <p>
     * Hello  <b>there</b> now!
     * </p>
     * </pre>
     *
     * the text that will be checked will be {@literal  Hello there now!}.
     *
     * @param text
     *            the text the component is expected to have as its content. Not
     *            {@literal null}.
     * @param ignoreCase
     *            flag to indicate if comparison must be case-insensitive.
     * @return this element query instance for chaining
     * @see HasText#getText()
     * @see Element#isTextNode()
     * @see Element#getText()
     * @see Element#getTextRecursively()
     * @see Html#getInnerHtml()
     */
    public static <T extends Component> Predicate<T> containsText(String text,
            boolean ignoreCase) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        return new TextContainsPredicate<>(text, ignoreCase);
    }

    /**
     * Checks if the given attribute has been set on the component.
     *
     * Attribute names are considered case-insensitive and all names will be
     * converted to lower case automatically.
     *
     * @param attribute
     *            the name of the attribute, not {@literal null}
     * @return {@literal true} if the attribute has been set, {@literal false}
     *         otherwise
     */
    public static <T extends Component> Predicate<T> hasAttribute(
            String attribute) {
        return component -> component.getElement().hasAttribute(attribute);
    }

    /**
     * Checks if the given attribute has been set on the component and has
     * exactly the given value.
     *
     * Attribute names are considered case-insensitive and all names will be
     * converted to lower case automatically.
     *
     * @param attribute
     *            the name of the attribute, not {@literal null}
     * @param value
     *            expected value, not {@literal null}
     * @return {@literal true} if the attribute has been set, {@literal false}
     *         otherwise
     */
    public static <T extends Component> Predicate<T> hasAttribute(
            String attribute, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        return component -> Objects
                .equals(component.getElement().getAttribute(attribute), value);
    }

    /**
     * Checks if the given attribute has not been set on the component.
     *
     * Attribute names are considered case-insensitive and all names will be
     * converted to lower case automatically.
     *
     * @param attribute
     *            the name of the attribute, not {@literal null}
     * @return {@literal true} if the attribute has not been set,
     *         {@literal false} otherwise
     */
    public static <T extends Component> Predicate<T> hasNotAttribute(
            String attribute) {
        return component -> !component.getElement().hasAttribute(attribute);
    }

    /**
     * Checks if the given attribute has been set on the component or has a
     * value different from given one.
     *
     * Attribute names are considered case-insensitive and all names will be
     * converted to lower case automatically.
     *
     * @param attribute
     *            the name of the attribute, not {@literal null}
     * @param value
     *            value expected not to be set on attribute, not {@literal null}
     * @return {@literal true} if the attribute is not set or has a value
     *         different from given one, {@literal false} otherwise
     */
    public static <T extends Component> Predicate<T> hasNotAttribute(
            String attribute, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        return component -> !Objects
                .equals(component.getElement().getAttribute(attribute), value);
    }

    /**
     * Checks if the component is labelled by exactly the given text. A
     * component is considered labelled by a text when either:
     *
     * <ul>
     * <li>its {@code label} property (read by
     * {@link com.vaadin.flow.component.HasLabel#getLabel()}) equals the text,
     * or</li>
     * <li>some {@code <label for="componentId">} element elsewhere in the UI
     * has that text as its (recursive) content.</li>
     * </ul>
     *
     * The second form covers the HTML pattern where a separate
     * {@link com.vaadin.flow.component.html.NativeLabel} (or any
     * {@code <label>} element) targets an input via the {@code for} attribute.
     *
     * @param label
     *            the expected label, not {@literal null}
     */
    public static <T extends Component> Predicate<T> hasLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("label cannot be null");
        }
        return component -> matchesLabel(component, label, false);
    }

    /**
     * Checks if the component's label contains the given text. The label is
     * read in the same way as {@link #hasLabel(String)} (component's
     * {@code label} property or a referring {@code <label for="...">} element).
     * Comparison is case-sensitive.
     *
     * @param text
     *            substring to find in the label, not {@literal null}
     */
    public static <T extends Component> Predicate<T> labelContains(
            String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        return component -> matchesLabel(component, text, true);
    }

    private static boolean matchesLabel(Component component, String expected,
            boolean substring) {
        String own = component.getElement().getProperty("label");
        if (matches(own, expected, substring)) {
            return true;
        }
        return component.getId()
                .map(id -> referringLabelText(component.getUI().get(), id))
                .filter(text -> matches(text, expected, substring)).isPresent();
    }

    private static boolean matches(String actual, String expected,
            boolean substring) {
        if (actual == null) {
            return false;
        }
        return substring ? actual.contains(expected) : actual.equals(expected);
    }

    private static String referringLabelText(Component root, String id) {
        return ComponentUtil.streamDescendants(root).map(Component::getElement)
                // Element.getTag() throws on text nodes; skip them first.
                .filter(e -> !e.isTextNode())
                .filter(e -> "label".equalsIgnoreCase(e.getTag()))
                .filter(e -> id.equals(e.getAttribute("for")))
                .map(Element::getTextRecursively).findFirst().orElse(null);
    }

    /**
     * Checks if the component identifies itself to assistive technology via
     * the given {@code aria-label}. Useful for components like {@code Button}
     * that don't expose a {@code label} property and for field components
     * (e.g. {@code TextField}, {@code TextArea}) that surface their accessible
     * name via {@link HasAriaLabel#setAriaLabel(String)}.
     * <p>
     * Resolution prefers {@link HasAriaLabel#getAriaLabel()} when the component
     * implements it, because field components back the accessible name with a
     * property (the web component reflects it to the inner input's
     * {@code aria-label} on the client). Otherwise falls back to reading the
     * server-side element's {@code aria-label} attribute.
     *
     * @param ariaLabel
     *            the expected aria-label, not {@literal null}
     */
    public static <T extends Component> Predicate<T> hasAriaLabel(
            String ariaLabel) {
        if (ariaLabel == null) {
            throw new IllegalArgumentException("ariaLabel cannot be null");
        }
        return component -> ariaLabel.equals(resolveAriaLabel(component));
    }

    /**
     * Checks if the component's aria-label contains the given text. Comparison
     * is case-sensitive. Resolution follows the same rules as
     * {@link #hasAriaLabel(String)} — prefer {@link HasAriaLabel#getAriaLabel()}
     * over the raw element attribute so that field components are matched.
     *
     * @param text
     *            substring to find in the aria-label, not {@literal null}
     */
    public static <T extends Component> Predicate<T> ariaLabelContains(
            String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        return component -> {
            String label = resolveAriaLabel(component);
            return label != null && label.contains(text);
        };
    }

    private static String resolveAriaLabel(Component component) {
        if (component instanceof HasAriaLabel hal) {
            return hal.getAriaLabel().orElse(null);
        }
        return component.getElement().getAttribute("aria-label");
    }

    private static class TextContainsPredicate<T extends Component>
            implements Predicate<T> {

        private final String text;
        private final boolean ignoreCase;

        public TextContainsPredicate(String text, boolean ignoreCase) {
            this.text = text;
            this.ignoreCase = ignoreCase;
        }

        @Override
        public boolean test(T component) {
            String componentText;
            if (component instanceof HasText) {
                componentText = ((HasText) component).getText();
            } else if (component instanceof HtmlComponent) {
                componentText = component.getElement().getTextRecursively();
            } else if (component instanceof Html) {
                // Strip tags and normalize text
                componentText = ((Html) component).getInnerHtml();
                if (componentText != null) {
                    componentText = Jsoup.parse(componentText).text();
                }
            } else if (component.getElement().isTextNode()) {
                componentText = component.getElement().getText();
            } else {
                componentText = component.getElement().getTextRecursively();
            }
            if (componentText == null) {
                return false;
            }
            // WARN: may not work correctly with unicode chars
            if (ignoreCase) {
                return componentText.toLowerCase(Locale.ROOT)
                        .contains(text.toLowerCase(Locale.ROOT));
            }
            return componentText.contains(text);
        }
    }
}
