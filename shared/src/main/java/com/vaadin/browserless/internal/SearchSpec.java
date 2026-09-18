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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValue;

/**
 * A criterion for matching components. The component must match all of non-null
 * fields.
 *
 * You can add more properties, simply by creating a write-only property which
 * will register a new predicate on write. See <a href=
 * "https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#adding-support-for-custom-search-criteria">Adding
 * support for custom search criteria</a> for more details.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class SearchSpec<T extends Component> {

    /**
     * The class of the component we are searching for.
     */
    private final Class<T> clazz;

    /**
     * The required {@link Component#getId()}; if null, no particular id is
     * matched.
     */
    private String id;

    /**
     * The required test id, i.e. the {@code data-testid} attribute; if null, no
     * particular test id is matched.
     */
    private String testId;

    /**
     * The required {@code caption}; if null, no particular caption is matched.
     */
    private String caption;

    /**
     * The required {@code placeholder}; if null, no particular placeholder is
     * matched.
     */
    private String placeholder;

    /**
     * The {@code com.vaadin.flow.dom.Element.getText}
     */
    private String text;

    /**
     * Expected count of matching components, defaults to
     * {@code 0..Int.MAX_VALUE}.
     */
    private CountRange count = CountRange.ANY;

    /**
     * Expected {@code com.vaadin.flow.component.HasValue.getValue}; if
     * {@code null}, no particular value is matched.
     */
    private Object value;

    /**
     * If not null, the component must match all of these class names.
     * Space-separated.
     */
    private String classes;

    /**
     * If not null, the component must NOT match any of these class names.
     * Space-separated.
     */
    private String withoutClasses;

    /**
     * The predicates the component needs to match, not null. May be empty - in
     * such case it is ignored. By default empty.
     */
    private List<Predicate<T>> predicates = new ArrayList<>();

    /**
     * If not null, the component must have all theme names defined.
     * Space-separated.
     */
    private String themes;

    /**
     * If not null, the component must NOT have any of the theme names defined.
     * Space-separated.
     */
    private String withoutThemes;

    public SearchSpec(Class<T> clazz) {
        this.clazz = clazz;
    }

    // Accessor methods, kept for Java-style usage and to preserve the Kotlin
    // `is`/getter/setter shape that other code relied on.

    public Class<T> getClazz() {
        return clazz;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public CountRange getCount() {
        return count;
    }

    public void setCount(CountRange count) {
        this.count = count;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public String getWithoutClasses() {
        return withoutClasses;
    }

    public void setWithoutClasses(String withoutClasses) {
        this.withoutClasses = withoutClasses;
    }

    public List<Predicate<T>> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate<T>> predicates) {
        this.predicates = predicates;
    }

    public String getThemes() {
        return themes;
    }

    public void setThemes(String themes) {
        this.themes = themes;
    }

    public String getWithoutThemes() {
        return withoutThemes;
    }

    public void setWithoutThemes(String withoutThemes) {
        this.withoutThemes = withoutThemes;
    }

    /**
     * Makes sure that the component's caption contains given {@code substring}.
     */
    public void captionContains(String substring) {
        predicates.add(new CaptionContainsPredicate<>(substring));
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        list.add(clazz.getSimpleName().isBlank() ? clazz.getName()
                : clazz.getSimpleName());
        if (id != null) {
            list.add("id='" + id + "'");
        }
        if (testId != null) {
            list.add("testId='" + testId + "'");
        }
        if (caption != null) {
            list.add("caption='" + caption + "'");
        }
        if (placeholder != null) {
            list.add("placeholder='" + placeholder + "'");
        }
        if (text != null) {
            list.add("text='" + text + "'");
        }
        if (classes != null && !classes.isBlank()) {
            list.add("classes='" + classes + "'");
        }
        if (withoutClasses != null && !withoutClasses.isBlank()) {
            list.add("withoutClasses='" + withoutClasses + "'");
        }
        if (themes != null && !themes.isBlank()) {
            list.add("themes='" + themes + "'");
        }
        if (withoutThemes != null && !withoutThemes.isBlank()) {
            list.add("withoutThemes='" + withoutThemes + "'");
        }
        if (value != null) {
            list.add("value=" + value);
        }
        CountRange any = CountRange.ANY;
        CountRange one = CountRange.ONE;
        if (!count.equals(any) && !count.equals(one)) {
            list.add("count=" + count);
        }
        for (Predicate<T> p : predicates) {
            list.add(p.toString());
        }
        return String.join(" and ", list);
    }

    /**
     * Returns a predicate which matches components based on this spec. All
     * rules are matched except the count rule. The rules are matched against
     * given component only (not against its children).
     */
    @SuppressWarnings("unchecked")
    public Predicate<Component> toPredicate() {
        List<Predicate<Component>> p = new ArrayList<>();
        p.add(clazz::isInstance);
        if (id != null) {
            p.add(component -> java.util.Objects
                    .equals(BasicUtils.id_(component), id));
        }
        if (testId != null) {
            p.add(component -> java.util.Objects.equals(component.getTestId(),
                    testId));
        }
        if (caption != null) {
            p.add(component -> java.util.Objects
                    .equals(ComponentUtils.caption(component), caption));
        }
        if (placeholder != null) {
            p.add(component -> java.util.Objects.equals(
                    ComponentUtils.placeholder(component), placeholder));
        }
        if (classes != null && !classes.isBlank()) {
            final String required = classes;
            p.add(component -> hasAllClasses(component, required));
        }
        if (withoutClasses != null && !withoutClasses.isBlank()) {
            final String forbidden = withoutClasses;
            p.add(component -> doesntHaveAnyClasses(component, forbidden));
        }
        if (themes != null && !themes.isBlank()) {
            final String required = themes;
            p.add(component -> hasAllThemes(component, required));
        }
        if (withoutThemes != null && !withoutThemes.isBlank()) {
            final String forbidden = withoutThemes;
            p.add(component -> notContainsThemes(component, forbidden));
        }
        if (text != null) {
            p.add(component -> java.util.Objects
                    .equals(component.getElement().getText(), text));
        }
        if (value != null) {
            p.add(component -> {
                if (component instanceof HasValue<?, ?>) {
                    return java.util.Objects.equals(
                            ((HasValue<?, ?>) component).getValue(), value);
                }
                return false;
            });
        }
        for (final Predicate<T> predicate : predicates) {
            p.add(component -> clazz.isInstance(component)
                    && predicate.test((T) component));
        }
        return component -> {
            for (Predicate<Component> pred : p) {
                if (!pred.test(component)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static boolean hasAllClasses(Component component, String classes) {
        if (classes.contains(" ")) {
            for (String single : Locator
                    .filterNotBlank(Arrays.asList(classes.split(" ")))) {
                if (!hasAllClasses(component, single)) {
                    return false;
                }
            }
            return true;
        }
        if (!(component instanceof HasStyle)) {
            return false;
        }
        return ((HasStyle) component).getClassNames().contains(classes);
    }

    private static boolean doesntHaveAnyClasses(Component component,
            String classes) {
        if (classes.contains(" ")) {
            for (String single : Locator
                    .filterNotBlank(Arrays.asList(classes.split(" ")))) {
                if (hasAllClasses(component, single)) {
                    return false;
                }
            }
            return true;
        }
        if (!(component instanceof HasStyle)) {
            return true;
        }
        return !((HasStyle) component).getClassNames().contains(classes);
    }

    private static boolean hasAllThemes(Component component, String themes) {
        for (String t : Locator
                .filterNotBlank(Arrays.asList(themes.split(" ")))) {
            if (!component.getElement().getThemeList().contains(t)) {
                return false;
            }
        }
        return true;
    }

    private static boolean notContainsThemes(Component component,
            String themes) {
        for (String t : Locator
                .filterNotBlank(Arrays.asList(themes.split(" ")))) {
            if (component.getElement().getThemeList().contains(t)) {
                return false;
            }
        }
        return true;
    }

    private static final class CaptionContainsPredicate<T extends Component>
            implements Predicate<T> {
        private final String substring;

        CaptionContainsPredicate(String substring) {
            this.substring = substring;
        }

        @Override
        public boolean test(T t) {
            return ComponentUtils.caption(t).contains(substring);
        }

        @Override
        public String toString() {
            return "captionContains('" + substring + "')";
        }
    }
}
