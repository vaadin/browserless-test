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
package com.vaadin.browserless.internal;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.dom.Element;

/**
 * A pretty-printed ASCII tree of arbitrary nodes, for printing to a console.
 * <p>
 * Build it out of any tree structure by giving each node a name and its
 * children; {@link #ofVaadin(Component)} does that for a Vaadin component tree,
 * which is what {@code TreeOnFailureExtension} prints when a test fails and
 * what a failed lookup appends to its error message.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class PrettyPrintTree {

    private static boolean prettyPrintUseAscii = false;

    private static BiConsumer<Component, LinkedList<String>> prettyStringHook = (
            c, l) -> {
    };

    private static Set<String> dontDumpAttributes = new HashSet<>(
            Arrays.asList("disabled", "id", "href"));

    /**
     * Tells whether the tree is drawn with ASCII characters.
     *
     * @return {@code true} if ASCII is used
     */
    public static boolean getPrettyPrintUseAscii() {
        return prettyPrintUseAscii;
    }

    /**
     * Draws the tree with {@code \--} instead of {@code └──}, which some
     * terminals render as {@code ???}.
     *
     * @param prettyPrintUseAscii
     *            {@code true} to use ASCII
     */
    public static void setPrettyPrintUseAscii(boolean prettyPrintUseAscii) {
        PrettyPrintTree.prettyPrintUseAscii = prettyPrintUseAscii;
    }

    /**
     * Returns the hook that adds extra properties to a dumped component.
     *
     * @return the hook, never {@code null}
     */
    public static BiConsumer<Component, LinkedList<String>> getPrettyStringHook() {
        return prettyStringHook;
    }

    /**
     * Installs a hook invoked by {@link #toPrettyString(Component)} to add
     * properties of a custom component to the dump, e.g.
     * {@code list.add("icon='" + icon + "'")}. Does nothing by default.
     *
     * @param prettyStringHook
     *            the hook to install
     */
    public static void setPrettyStringHook(
            BiConsumer<Component, LinkedList<String>> prettyStringHook) {
        PrettyPrintTree.prettyStringHook = prettyStringHook;
    }

    /**
     * Returns the attribute names {@link #toPrettyString(Component)} never
     * dumps. The set is live and directly modifiable.
     * <p>
     * Ignored by default: {@code disabled}, which is dumped separately as
     * {@code DISABLED}; {@code id}, dumped as {@code #id}; and {@code href},
     * which has its own handling so that it is reported for any component
     * declaring it, not just an {@link Anchor}.
     *
     * @return the ignored attribute names
     */
    public static Set<String> getDontDumpAttributes() {
        return dontDumpAttributes;
    }

    /**
     * Replaces the attribute names {@link #toPrettyString(Component)} never
     * dumps.
     *
     * @param dontDumpAttributes
     *            the attribute names to ignore
     */
    public static void setDontDumpAttributes(Set<String> dontDumpAttributes) {
        PrettyPrintTree.dontDumpAttributes = dontDumpAttributes;
    }

    private final String name;

    private final List<PrettyPrintTree> children;

    /**
     * Creates a node.
     *
     * @param name
     *            the text to print for this node
     * @param children
     *            this node's children, in print order
     */
    public PrettyPrintTree(String name, List<PrettyPrintTree> children) {
        this.name = name;
        this.children = children;
    }

    /**
     * Returns the text printed for this node.
     *
     * @return the node name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns this node's children, live and directly modifiable.
     *
     * @return the children, in print order
     */
    public List<PrettyPrintTree> getChildren() {
        return children;
    }

    /**
     * Renders this node and everything below it.
     *
     * @return the rendered tree, one node per line
     */
    public String print() {
        StringBuilder sb = new StringBuilder();
        print(sb, "", true);
        return sb.toString();
    }

    private void print(StringBuilder sb, String prefix, boolean isTail) {
        char pipe = !prettyPrintUseAscii ? '│' : '|';
        String branchTail = !prettyPrintUseAscii ? "└── " : "\\-- ";
        String branch = !prettyPrintUseAscii ? "├── " : "|-- ";
        sb.append(prefix).append(isTail ? branchTail : branch).append(name)
                .append("\n");
        String childPrefix = prefix + (isTail ? "    " : pipe + "   ");
        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).print(sb, childPrefix, false);
        }
        if (!children.isEmpty()) {
            children.get(children.size() - 1).print(sb, childPrefix, true);
        }
    }

    public static PrettyPrintTree ofVaadin(Component root) {
        PrettyPrintTree result = new PrettyPrintTree(toPrettyString(root),
                new ArrayList<>());
        for (Component child : TestingLifecycleHooks.getCurrent()
                .getAllChildren(root)) {
            result.children.add(ofVaadin(child));
        }
        return result;
    }

    public static String toPrettyTree(Component c) {
        return PrettyPrintTree.ofVaadin(c).print();
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Set<String> IGNORED_PROPERTIES = new HashSet<>(
            Arrays.asList("value", "invalid", "openOn", "label", "errorMessage",
                    "innerHTML", "i18n", "error", "stackTrace"));

    /**
     * Returns the most basic properties of the component, formatted as a
     * concise string: * The component class * The {@link Component#getId} *
     * Whether the component is {@link Component#isVisible} * Whether it is a
     * {@link HasValue} that is read-only * the styles * The
     * {@link Component#label} and text * The {@link HasValue#getValue}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String toPrettyString(Component c) {
        LinkedList<String> list = new LinkedList<>();
        if (c.getId().isPresent()) {
            list.add("#" + c.getId().get());
        }
        if (!BasicUtils._isVisible(c)) {
            list.add("INVIS");
        }
        if (c instanceof HasValue && ((HasValue) c).isReadOnly()) {
            list.add("RO");
        }
        Element element = c.getElement();
        if (!element.isEnabled()) {
            list.add("DISABLED");
        }
        String labelText = ComponentUtils.label(c);
        if (labelText != null && !labelText.isBlank()) {
            list.add("label='" + labelText + "'");
        }
        String captionText = ComponentUtils.caption(c);
        if (!java.util.Objects.equals(labelText, captionText)
                && captionText != null && !captionText.isBlank()) {
            list.add("caption='" + captionText + "'");
        }
        String textValue = BasicUtils._text(c);
        if (textValue != null && !textValue.isBlank()
                && !java.util.Objects.equals(textValue, captionText)) {
            list.add("text='" + textValue + "'");
        }
        if (c instanceof HasValue) {
            list.add("value='" + ((HasValue) c).getValue() + "'");
        }
        if (c instanceof HasValidation) {
            HasValidation hv = (HasValidation) c;
            if (hv.isInvalid()) {
                list.add("INVALID");
            }
            String errorMessage = hv.getErrorMessage();
            if (errorMessage != null && !errorMessage.isBlank()) {
                list.add("errorMessage='" + errorMessage + "'");
            }
        }
        /*
         * TODO: uncomment when importing Grid stuff if (c instanceof
         * Grid.Column<?>) { if (this.header2.isNotBlank()) {
         * list.add("header='${this.header2}'") } if (!this.key.isNullOrBlank())
         * { list.add("key='${this.key}'") } }
         */
        // TODO: add a system property to allow verbose pretty print with
        // ignored attributes
        for (String propName : element.getPropertyNames().toList()) {
            String propertyValue = element.getProperty(propName);
            if (propertyValue != null && !IGNORED_PROPERTIES.contains(propName)
                    && !propertyValue.isEmpty() && !propName.startsWith("_")) {
                list.add(propName + "='" + propertyValue + "'");
            }
        }
        // Any component with href should output it not only Anchor
        Object href = hrefValue(c);
        if (href != null) {
            list.add("href='" + href + "'");
        }
        if (c instanceof Button && ((Button) c).getIcon() instanceof Icon) {
            Icon icon = (Icon) ((Button) c).getIcon();
            list.add("icon='" + icon.getElement().getAttribute("icon") + "'");
        }
        if (c instanceof Html) {
            String outerHtml = WHITESPACE.matcher(element.getOuterHTML().trim())
                    .replaceAll(" ");
            list.add(Utils.ellipsize(outerHtml, 100));
        }
        if (c instanceof Grid<?> && ((Grid<?>) c).getBeanType() != null) {
            list.add("<" + ((Grid<?>) c).getBeanType().getSimpleName() + ">");
        }
        DataProvider<?, ?> dp = ComponentUtils.dataProvider(c);
        if (dp != null) {
            list.add("dataprovider='" + dp + "'");
        }
        // the attributes may come in arbitrary order; make sure to sort them,
        // in order to have predictable order and repeatable tests.
        TreeSet<String> sortedAttrs = new TreeSet<>();
        element.getAttributeNames().forEach(a -> {
            if (!dontDumpAttributes.contains(a)) {
                sortedAttrs.add(a);
            }
        });
        for (String attributeName : sortedAttrs) {
            String value = element.getAttribute(attributeName);
            if (value != null && !value.isBlank()) {
                list.add("@" + attributeName + "='" + value + "'");
            }
        }
        if (!(c instanceof Html)) {
            String innerHTML = element.getProperty("innerHTML");
            if (innerHTML != null && !innerHTML.isBlank()) {
                String compacted = WHITESPACE.matcher(innerHTML.trim())
                        .replaceAll(" ");
                list.add("innerHTML='" + compacted + "'");
            }
        }
        if (Utils.hasCustomToString(c.getClass())) {
            // by default Vaadin components do not introduce toString() at all;
            // toString() therefore defaults to Object's toString() which is
            // useless. However,
            // if a component does introduce a toString() then use it - it could
            // provide
            // valuable information.
            list.add(c.toString());
        }
        prettyStringHook.accept(c, list);
        String name = c.getClass().getSimpleName();
        if (name.isEmpty()) {
            // anonymous classes
            name = c.getClass().getName();
        }
        return name + list;
    }

    /**
     * Reads the {@code href} value of the given component, so that
     * {@link #toPrettyString(Component)} can dump it for any component
     * declaring it, not just an {@link Anchor}.
     * <p>
     * The first member holding a non-blank value wins, looked up in this order:
     * a no-arg {@code href()} method or an {@code href} field declared anywhere
     * in the class hierarchy, at any visibility — that is also what a Kotlin
     * {@code href} property with a backing field compiles to — and finally the
     * public no-arg {@code href()} and {@code getHref()} methods, which covers
     * bean getters, Kotlin properties without a backing field and members
     * inherited from an interface.
     * <p>
     * The Java reflection API is used on purpose since it resolves member
     * metadata lazily: some components have methods referencing classes (e.g.
     * Spring ones) which may not be present in all projects, and eagerly
     * scanning all metadata (parameters, generics, ...) would fail for those.
     *
     * @param c
     *            the component to read from
     * @return the {@code href} value, or {@code null} if this component has
     *         none
     */
    private static Object hrefValue(Component c) {
        // members declared by the component classes themselves, at any
        // visibility: Anchor.href e.g. is a private field
        for (Class<?> clazz = c.getClass(); clazz != null; clazz = clazz
                .getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if ("href".equals(method.getName())
                        && method.getParameterCount() == 0) {
                    Object value = readHref(c, method);
                    if (value != null) {
                        return value;
                    }
                    break;
                }
            }
            for (Field field : clazz.getDeclaredFields()) {
                if ("href".equals(field.getName())) {
                    Object value = readHref(c, field);
                    if (value != null) {
                        return value;
                    }
                    break;
                }
            }
        }
        // bean getters, e.g. RouterLink.getHref(), and members inherited from
        // an interface. getMethods() only gets consulted here, since it is the
        // more expensive call and most components declare no href member at all
        for (Method method : c.getClass().getMethods()) {
            if (("href".equals(method.getName())
                    || "getHref".equals(method.getName()))
                    && method.getParameterCount() == 0) {
                Object value = readHref(c, method);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Reads the value the given {@code href} member holds for the component.
     *
     * @param c
     *            the component to read from
     * @param member
     *            the {@code href} method or field
     * @return the value, or {@code null} if there is none or it is blank — a
     *         getter typically reports a missing {@code href} as an empty
     *         string, e.g. {@link Anchor#getHref()}
     */
    private static Object readHref(Component c, AccessibleObject member) {
        try {
            member.setAccessible(true);
            Object value = null;
            if (member instanceof Method) {
                value = ((Method) member).invoke(c);
            } else if (member instanceof Field) {
                value = ((Field) member).get(c);
            }
            return value != null && !value.toString().isBlank() ? value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // href is best-effort: a member that cannot be read, or whose
            // metadata references a class this project does not have, simply
            // does not contribute to the dump
            return null;
        }
    }
}
