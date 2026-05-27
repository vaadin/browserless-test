/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

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
 * Utility class to create a pretty-printed ASCII tree of arbitrary nodes that can be printed to the console.
 * You can build the tree out of any tree structure, just fill in this node [name] and its [children].
 *
 * To create a pretty tree dump of a Vaadin component, just use [ofVaadin].
 */
public class PrettyPrintTree {

    /**
     * If true, [PrettyPrintTree] will use `\--` instead of `└──` which tend to render on some terminals as `???`.
     */
    public static boolean prettyPrintUseAscii = false;

    /**
     * Invoked by [toPrettyString] to add additional properties for your custom component.
     * Add additional properties to the `list` provided, e.g. `list.add("icon='$icon'")`.
     *
     * By default does nothing.
     */
    public static BiConsumer<Component, LinkedList<String>> prettyStringHook = (c, l) -> {
    };

    /**
     * Never dump these attributes in [toPrettyString]. By default these attributes are ignored:
     *
     * * `disabled` - dumped separately as "DISABLED" string.
     * * `id` - dumped as Component.id
     * * `href` - there's special processing for [Anchor._href].
     */
    public static Set<String> dontDumpAttributes = new HashSet<>(Arrays.asList("disabled", "id", "href"));

    public final String name;
    public final List<PrettyPrintTree> children;

    public PrettyPrintTree(String name, List<PrettyPrintTree> children) {
        this.name = name;
        this.children = children;
    }

    public String print() {
        StringBuilder sb = new StringBuilder();
        print(sb, "", true);
        return sb.toString();
    }

    private void print(StringBuilder sb, String prefix, boolean isTail) {
        char pipe = !prettyPrintUseAscii ? '│' : '|';
        String branchTail = !prettyPrintUseAscii ? "└── " : "\\-- ";
        String branch = !prettyPrintUseAscii ? "├── " : "|-- ";
        sb.append(prefix).append(isTail ? branchTail : branch).append(name).append("\n");
        String childPrefix = prefix + (isTail ? "    " : pipe + "   ");
        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).print(sb, childPrefix, false);
        }
        if (!children.isEmpty()) {
            children.get(children.size() - 1).print(sb, childPrefix, true);
        }
    }

    public static PrettyPrintTree ofVaadin(Component root) {
        PrettyPrintTree result = new PrettyPrintTree(toPrettyString(root), new ArrayList<>());
        for (Component child : TestingLifecycleHooks.current.getAllChildren(root)) {
            result.children.add(ofVaadin(child));
        }
        return result;
    }

    public static String toPrettyTree(Component c) {
        return PrettyPrintTree.ofVaadin(c).print();
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Set<String> IGNORED_PROPERTIES = new HashSet<>(Arrays.asList(
            "value", "invalid", "openOn", "label", "errorMessage", "innerHTML", "i18n", "error", "stackTrace"));

    /**
     * Returns the most basic properties of the component, formatted as a concise string:
     * * The component class
     * * The [Component.getId]
     * * Whether the component is [Component.isVisible]
     * * Whether it is a [HasValue] that is read-only
     * * the styles
     * * The [Component.label] and text
     * * The [HasValue.getValue]
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
        if (!java.util.Objects.equals(labelText, captionText) && captionText != null && !captionText.isBlank()) {
            list.add("caption='" + captionText + "'");
        }
        String textValue = BasicUtils._text(c);
        if (textValue != null && !textValue.isBlank() && !java.util.Objects.equals(textValue, captionText)) {
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
        /* TODO: uncomment when importing Grid stuff
        if (c instanceof Grid.Column<?>) {
            if (this.header2.isNotBlank()) {
                list.add("header='${this.header2}'")
            }
            if (!this.key.isNullOrBlank()) {
                list.add("key='${this.key}'")
            }
        }
         */
        // TODO: add a system property to allow verbose pretty print with ignored attributes
        for (String propName : element.getPropertyNames().toList()) {
            String propertyValue = element.getProperty(propName);
            if (propertyValue != null && !IGNORED_PROPERTIES.contains(propName)
                    && !propertyValue.isEmpty() && !propName.startsWith("_")) {
                list.add(propName + "='" + propertyValue + "'");
            }
        }
        // Any component with href should output it not only Anchor
        try {
            Method hrefMethod = null;
            for (Method m : c.getClass().getMethods()) {
                if (m.getParameterCount() == 0
                        && ("href".equals(m.getName()) || "getHref".equals(m.getName()))) {
                    hrefMethod = m;
                    break;
                }
            }
            if (hrefMethod != null) {
                Object value = hrefMethod.invoke(c);
                if (value != null && !value.toString().isBlank()) {
                    list.add("href='" + value + "'");
                }
            }
        } catch (TypeNotPresentException e) {
            // Some components have methods referencing Spring classes that may not
            // be present for all project. Method lookup or invocation may trigger
            // metadata resolution that fails when those classes are missing.
        } catch (ReflectiveOperationException e) {
            // ignore - href is best-effort
        }
        if (c instanceof Button && ((Button) c).getIcon() instanceof Icon) {
            Icon icon = (Icon) ((Button) c).getIcon();
            list.add("icon='" + icon.getElement().getAttribute("icon") + "'");
        }
        if (c instanceof Html) {
            String outerHtml = WHITESPACE.matcher(element.getOuterHTML().trim()).replaceAll(" ");
            list.add(Utils.ellipsize(outerHtml, 100));
        }
        if (c instanceof Grid<?> && ((Grid<?>) c).getBeanType() != null) {
            list.add("<" + ((Grid<?>) c).getBeanType().getSimpleName() + ">");
        }
        DataProvider<?, ?> dp = ComponentUtils.dataProvider(c);
        if (dp != null) {
            list.add("dataprovider='" + dp + "'");
        }
        // the attributes may come in arbitrary order; make sure to sort them, in order to have predictable order and repeatable tests.
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
                String compacted = WHITESPACE.matcher(innerHTML.trim()).replaceAll(" ");
                list.add("innerHTML='" + compacted + "'");
            }
        }
        if (Utils.hasCustomToString(c.getClass())) {
            // by default Vaadin components do not introduce toString() at all;
            // toString() therefore defaults to Object's toString() which is useless. However,
            // if a component does introduce a toString() then use it - it could provide
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
}
