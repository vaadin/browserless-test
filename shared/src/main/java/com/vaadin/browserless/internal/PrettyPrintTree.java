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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.data.provider.DataProvider;

/**
 * Utility class to create a pretty-printed ASCII tree of arbitrary nodes that
 * can be printed to the console. You can build the tree out of any tree
 * structure, just fill in this node {@link #getName() name} and its
 * {@link #getChildren() children}.
 * <p>
 * To create a pretty tree dump of a Vaadin component, just use
 * {@link Companion#ofVaadin(Component)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class PrettyPrintTree {

    /**
     * Provides {@link Companion#ofVaadin(Component)}.
     */
    public static final Companion Companion = new Companion();

    private final String name;
    private final List<PrettyPrintTree> children;
    private final char pipe;
    private final String branchTail;
    private final String branch;

    /**
     * Creates a tree node.
     *
     * @param name
     *            the node name.
     * @param children
     *            the node children, mutable.
     */
    public PrettyPrintTree(String name, List<PrettyPrintTree> children) {
        this.name = name;
        this.children = children;
        boolean ascii = PrettyPrintTree.getPrettyPrintUseAscii();
        this.pipe = !ascii ? '│' : '|';
        this.branchTail = !ascii ? "└── " : "\\-- ";
        this.branch = !ascii ? "├── " : "|-- ";
    }

    /**
     * Returns the node name.
     *
     * @return the node name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the node children.
     *
     * @return the mutable list of children.
     */
    public List<PrettyPrintTree> getChildren() {
        return children;
    }

    /**
     * Prints this tree.
     *
     * @return the pretty-printed tree.
     */
    public String print() {
        StringBuilder sb = new StringBuilder();
        print(sb, "", true);
        return sb.toString();
    }

    private void print(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix + (isTail ? branchTail : branch) + name + "\n");
        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).print(sb, prefix + (isTail ? "    " : pipe + "   "),
                    false);
        }
        if (children.size() > 0) {
            children.get(children.size() - 1).print(sb,
                    prefix + (isTail ? "    " : pipe + "   "), true);
        }
    }

    /**
     * Factory methods for {@link PrettyPrintTree}.
     */
    public static final class Companion {

        private Companion() {
        }

        /**
         * Creates a tree out of the given Vaadin component and all of its
         * descendants, as provided by
         * {@link TestingLifecycleHook#getAllChildren(Component)}.
         *
         * @param root
         *            the root component.
         * @return the tree.
         */
        public PrettyPrintTree ofVaadin(Component root) {
            PrettyPrintTree result = new PrettyPrintTree(
                    PrettyPrintTree.toPrettyString(root), new ArrayList<>());
            for (Component child : TestingLifecycleHooks
                    .getTestingLifecycleHook().getAllChildren(root)) {
                result.children.add(ofVaadin(child));
            }
            return result;
        }
    }

    /**
     * If true, {@link PrettyPrintTree} will use {@code \--} instead of
     * {@code └──} which tend to render on some terminals as {@code ???}.
     */
    private static boolean prettyPrintUseAscii = false;

    /**
     * Invoked by {@link #toPrettyString(Component)} to add additional
     * properties for your custom component. Add additional properties to the
     * {@code list} provided, e.g. {@code list.add("icon='" + icon + "'")}.
     * <p>
     * By default does nothing.
     */
    private static Function2<? super Component, ? super LinkedList<String>, Unit> prettyStringHook = (
            component, list) -> Unit.INSTANCE;

    /**
     * Never dump these attributes in {@link #toPrettyString(Component)}. By
     * default these attributes are ignored:
     * <ul>
     * <li>{@code disabled} - dumped separately as "DISABLED" string.</li>
     * <li>{@code id} - dumped as Component.id</li>
     * <li>{@code href} - there's special processing for {@link Anchor}'s
     * href.</li>
     * </ul>
     */
    private static Set<String> dontDumpAttributes = new LinkedHashSet<>(
            List.of("disabled", "id", "href"));

    /**
     * If true, {@link PrettyPrintTree} will use {@code \--} instead of
     * {@code └──} which tend to render on some terminals as {@code ???}.
     *
     * @return whether ASCII characters are used.
     */
    public static boolean getPrettyPrintUseAscii() {
        return prettyPrintUseAscii;
    }

    /**
     * Sets whether {@link PrettyPrintTree} uses ASCII characters only.
     *
     * @param prettyPrintUseAscii
     *            true to use ASCII characters only.
     */
    public static void setPrettyPrintUseAscii(boolean prettyPrintUseAscii) {
        PrettyPrintTree.prettyPrintUseAscii = prettyPrintUseAscii;
    }

    /**
     * Returns a pretty-printed tree of the given component and its descendants.
     *
     * @param component
     *            the root component.
     * @return the pretty-printed tree.
     */
    public static String toPrettyTree(Component component) {
        return PrettyPrintTree.Companion.ofVaadin(component).print();
    }

    /**
     * Returns the most basic properties of the component, formatted as a
     * concise string:
     * <ul>
     * <li>The component class</li>
     * <li>The {@link Component#getId()}</li>
     * <li>Whether the component is {@link Component#isVisible()}</li>
     * <li>Whether it is a {@link HasValue} that is read-only</li>
     * <li>the styles</li>
     * <li>The label and text</li>
     * <li>The {@link HasValue#getValue()}</li>
     * </ul>
     *
     * @param component
     *            the component.
     * @return the component properties as a concise string.
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public static String toPrettyString(Component component) {
        LinkedList<String> list = new LinkedList<>();
        if (component.getId().isPresent()) {
            list.add("#" + component.getId().get());
        }
        if (!BasicUtils.get_isVisible(component)) {
            list.add("INVIS");
        }
        if (component instanceof HasValue<?, ?> hasValue
                && hasValue.isReadOnly()) {
            list.add("RO");
        }
        if (!component.getElement().isEnabled()) {
            list.add("DISABLED");
        }
        String label = ComponentSupports.getLabel(component);
        if (!Utils.isBlank(label)) {
            list.add("label='" + label + "'");
        }
        String caption = ComponentSupports.getCaption(component);
        if (!label.equals(caption) && !Utils.isBlank(caption)) {
            list.add("caption='" + caption + "'");
        }
        String text = BasicUtils.get_text(component);
        if (!Utils.isBlank(text) && !text.equals(caption)) {
            list.add("text='" + text + "'");
        }
        if (component instanceof HasValue<?, ?> hasValue) {
            list.add("value='" + hasValue.getValue() + "'");
        }
        if (component instanceof HasValidation hasValidation) {
            if (hasValidation.isInvalid()) {
                list.add("INVALID");
            }
            if (!Utils.isBlank(hasValidation.getErrorMessage())) {
                list.add("errorMessage='" + hasValidation.getErrorMessage()
                        + "'");
            }
        }
        // TODO: add a system property to allow verbose pretty print with
        // ignored attributes
        List<String> ignoredAttr = List.of("value", "invalid", "openOn",
                "label", "errorMessage", "innerHTML", "i18n", "error",
                "stackTrace");
        component.getElement().getPropertyNames().forEach(it -> {
            String propertyValue = component.getElement().getProperty(it);
            if (propertyValue != null && !ignoredAttr.contains(it)
                    && !propertyValue.isEmpty() && !it.startsWith("_")) {
                list.add(it + "='" + propertyValue + "'");
            }
        });
        // Any component with href should output it not only Anchor
        Object href = hrefValue(component);
        if (href != null) {
            list.add("href='" + href + "'");
        }
        ComponentSupports.describe(component, list);
        if (component instanceof Html) {
            String outerHtml = Utils.trim(component.getElement().getOuterHTML())
                    .replaceAll("\\s+", " ");
            list.add(Utils.ellipsize(outerHtml, 100, "..."));
        }
        DataProvider<?, ?> dataProvider = ComponentSupports
                .getDataProvider(component);
        if (dataProvider != null) {
            list.add("dataprovider='" + dataProvider + "'");
        }
        component.getElement().getAttributeNames()
                .filter(it -> !dontDumpAttributes.contains(it))
                // the attributes may come in arbitrary order; make sure to
                // sort them, in order to have predictable order and repeatable
                // tests.
                .sorted().forEach(attributeName -> {
                    String value = component.getElement()
                            .getAttribute(attributeName);
                    if (!Utils.isBlank(value)) {
                        list.add("@" + attributeName + "='" + value + "'");
                    }
                });
        if (!(component instanceof Html) && !Utils
                .isBlank(component.getElement().getProperty("innerHTML"))) {
            String innerHTML = Utils
                    .trim(component.getElement().getProperty("innerHTML"))
                    .replaceAll("\\s+", " ");
            list.add("innerHTML='" + innerHTML + "'");
        }
        if (Utils.hasCustomToString(component.getClass())) {
            // by default Vaadin components do not introduce toString() at all;
            // toString() therefore defaults to Object's toString() which is
            // useless. However, if a component does introduce a toString()
            // then use it - it could provide valuable information.
            list.add(component.toString());
        }
        prettyStringHook.invoke(component, list);
        String name = component.getClass().getSimpleName();
        if (name.isEmpty()) {
            // anonymous classes
            name = component.getClass().getName();
        }
        return name + list;
    }

    /**
     * Reads the {@code href} value of the given component, so that
     * {@link #toPrettyString(Component)} can dump it for any component
     * declaring it, not just {@link Anchor}.
     * <p>
     * The first member holding a non-blank value wins, looked up in this order:
     * a no-arg {@code href()} method or an {@code href} field declared anywhere
     * in the class hierarchy, at any visibility - that is also what a Kotlin
     * {@code href} property with a backing field compiles to - and finally the
     * public no-arg {@code href()} and {@code getHref()} methods, which covers
     * bean getters, Kotlin properties without a backing field and members
     * inherited from an interface.
     * <p>
     * The Java reflection API is used on purpose since it resolves member
     * metadata lazily: some components have methods referencing classes (e.g.
     * Spring ones) which may not be present in all projects, and eagerly
     * scanning all metadata (parameters, generics, ...) would fail for those.
     *
     * @return the {@code href} value, or null if the component has none.
     */
    private static Object hrefValue(Component component) {
        // members declared by the component classes themselves, at any
        // visibility: Anchor.href e.g. is a private field
        Class<?> clazz = component.getClass();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals("href")
                        && method.getParameterCount() == 0) {
                    Object value = readHref(component, method);
                    if (value != null) {
                        return value;
                    }
                    break;
                }
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals("href")) {
                    Object value = readHref(component, field);
                    if (value != null) {
                        return value;
                    }
                    break;
                }
            }
            clazz = clazz.getSuperclass();
        }
        // bean getters, e.g. RouterLink.getHref(), and members inherited from
        // an interface. getMethods() only gets consulted here, since it is the
        // more expensive call and most components declare no href member at
        // all
        for (Method method : component.getClass().getMethods()) {
            if ((method.getName().equals("href")
                    || method.getName().equals("getHref"))
                    && method.getParameterCount() == 0) {
                Object value = readHref(component, method);
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
     * @return the value, or null if there is none or it is blank - a getter
     *         typically reports a missing {@code href} as an empty string, e.g.
     *         {@link Anchor#getHref()}.
     */
    private static Object readHref(Component component,
            AccessibleObject member) {
        member.setAccessible(true);
        Object value;
        try {
            if (member instanceof Method method) {
                value = method.invoke(component);
            } else if (member instanceof Field field) {
                value = field.get(component);
            } else {
                value = null;
            }
        } catch (ReflectiveOperationException e) {
            throw Utils.sneakyThrow(e);
        }
        return value != null && !Utils.isBlank(value.toString()) ? value : null;
    }

    /**
     * Invoked by {@link #toPrettyString(Component)} to add additional
     * properties for your custom component. Add additional properties to the
     * {@code list} provided, e.g. {@code list.add("icon='" + icon + "'")}.
     * <p>
     * By default does nothing.
     *
     * @return the hook.
     */
    @SuppressWarnings("unchecked")
    public static Function2<Component, LinkedList<String>, Unit> getPrettyStringHook() {
        return (Function2<Component, LinkedList<String>, Unit>) prettyStringHook;
    }

    /**
     * Sets the hook invoked by {@link #toPrettyString(Component)}, see
     * {@link #getPrettyStringHook()}.
     *
     * @param prettyStringHook
     *            the hook, not null.
     */
    public static void setPrettyStringHook(
            Function2<? super Component, ? super LinkedList<String>, Unit> prettyStringHook) {
        PrettyPrintTree.prettyStringHook = prettyStringHook;
    }

    /**
     * Never dump these attributes in {@link #toPrettyString(Component)}. By
     * default these attributes are ignored:
     * <ul>
     * <li>{@code disabled} - dumped separately as "DISABLED" string.</li>
     * <li>{@code id} - dumped as Component.id</li>
     * <li>{@code href} - there's special processing for {@link Anchor}'s
     * href.</li>
     * </ul>
     *
     * @return the mutable set of attribute names.
     */
    public static Set<String> getDontDumpAttributes() {
        return dontDumpAttributes;
    }

    /**
     * Sets the attributes never dumped by {@link #toPrettyString(Component)},
     * see {@link #getDontDumpAttributes()}.
     *
     * @param dontDumpAttributes
     *            the set of attribute names, not null.
     */
    public static void setDontDumpAttributes(Set<String> dontDumpAttributes) {
        PrettyPrintTree.dontDumpAttributes = dontDumpAttributes;
    }
}
