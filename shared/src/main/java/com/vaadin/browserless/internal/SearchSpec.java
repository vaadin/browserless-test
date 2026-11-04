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
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import kotlin.jvm.functions.Function1;
import kotlin.ranges.IntRange;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;

/**
 * A criterion for matching components. The component must match all of non-null
 * fields.
 * <p>
 * You can add more properties, simply by creating a write-only property which
 * will register a new {@link #getPredicates() predicate} on write.
 *
 * @param <T>
 *            the type of the component we are searching for.
 */
public final class SearchSpec<T extends Component> {

    private final Class<T> clazz;
    private String id;
    private String testId;
    private String caption;
    private String placeholder;
    private String text;
    private IntRange count;
    private Object value;
    private String classes;
    private String withoutClasses;
    private List<Predicate<T>> predicates;
    private String themes;
    private String withoutThemes;

    /**
     * Creates a search specification which matches all components of given
     * class.
     *
     * @param clazz
     *            the class of the component we are searching for.
     */
    public SearchSpec(Class<T> clazz) {
        this(clazz, null, null, null, null, null,
                new IntRange(0, Integer.MAX_VALUE), null, null, null,
                new ArrayList<>(), null, null);
    }

    /**
     * Creates a search specification.
     *
     * @param clazz
     *            the class of the component we are searching for.
     * @param id
     *            the required {@link Component#getId()}; if null, no particular
     *            id is matched.
     * @param testId
     *            the required {@link Component#getTestId()} (i.e. the
     *            {@code data-testid} attribute); if null, no particular test id
     *            is matched.
     * @param caption
     *            the required caption; if null, no particular caption is
     *            matched.
     * @param placeholder
     *            the required placeholder; if null, no particular placeholder
     *            is matched.
     * @param text
     *            the {@link com.vaadin.flow.dom.Element#getText()}
     * @param count
     *            expected count of matching components, defaults to
     *            {@code 0..Int.MAX_VALUE}
     * @param value
     *            expected {@link HasValue#getValue()}; if {@code null}, no
     *            particular value is matched.
     * @param classes
     *            if not null, the component must match all of these class
     *            names. Space-separated.
     * @param withoutClasses
     *            if not null, the component must NOT match any of these class
     *            names. Space-separated.
     * @param predicates
     *            the predicates the component needs to match, not null. May be
     *            empty - in such case it is ignored.
     * @param themes
     *            if not null, the component must have all theme names defined.
     *            Space-separated
     * @param withoutThemes
     *            if not null, the component must NOT have any of the theme
     *            names defined. Space-separated
     */
    public SearchSpec(Class<T> clazz, String id, String testId, String caption,
            String placeholder, String text, IntRange count, Object value,
            String classes, String withoutClasses,
            List<Predicate<T>> predicates, String themes,
            String withoutThemes) {
        this.clazz = Objects.requireNonNull(clazz, "clazz");
        this.id = id;
        this.testId = testId;
        this.caption = caption;
        this.placeholder = placeholder;
        this.text = text;
        this.count = Objects.requireNonNull(count, "count");
        this.value = value;
        this.classes = classes;
        this.withoutClasses = withoutClasses;
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        this.themes = themes;
        this.withoutThemes = withoutThemes;
    }

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

    public IntRange getCount() {
        return count;
    }

    public void setCount(IntRange count) {
        this.count = Objects.requireNonNull(count, "count");
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
        this.predicates = Objects.requireNonNull(predicates, "predicates");
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

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        list.add(UtilsKt.isBlank(clazz.getSimpleName()) ? clazz.getName()
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
        if (!UtilsKt.isBlank(classes)) {
            list.add("classes='" + classes + "'");
        }
        if (!UtilsKt.isBlank(withoutClasses)) {
            list.add("withoutClasses='" + withoutClasses + "'");
        }
        if (!UtilsKt.isBlank(themes)) {
            list.add("themes='" + themes + "'");
        }
        if (!UtilsKt.isBlank(withoutThemes)) {
            list.add("withoutThemes='" + withoutThemes + "'");
        }
        if (value != null) {
            list.add("value=" + value);
        }
        if (!count.equals(new IntRange(0, Integer.MAX_VALUE))
                && !count.equals(new IntRange(1, 1))) {
            list.add("count=" + count);
        }
        for (Predicate<T> predicate : predicates) {
            list.add(String.valueOf(predicate));
        }
        return String.join(" and ", list);
    }

    /**
     * Makes sure that the component's caption contains given {@code substring}.
     *
     * @param substring
     *            the substring the caption must contain.
     */
    public void captionContains(String substring) {
        predicates.add(new CaptionContainsPredicate<>(substring));
    }

    /**
     * Returns a predicate which matches components based on this spec. All
     * rules are matched except the {@link #getCount() count} rule. The rules
     * are matched against given component only (not against its children).
     *
     * @return the predicate, not null.
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public Function1<Component, Boolean> toPredicate() {
        List<Function1<Component, Boolean>> p = new ArrayList<>();
        p.add(component -> clazz.isInstance(component));
        if (id != null) {
            p.add(component -> Objects.equals(BasicUtilsKt.getId_(component),
                    id));
        }
        if (testId != null) {
            p.add(component -> Objects.equals(component.getTestId(), testId));
        }
        if (caption != null) {
            p.add(component -> Objects
                    .equals(ComponentUtilsKt.getCaption(component), caption));
        }
        if (placeholder != null) {
            p.add(component -> Objects.equals(
                    ComponentUtilsKt.getPlaceholder(component), placeholder));
        }
        if (!UtilsKt.isBlank(classes)) {
            p.add(component -> LocatorKt.hasAllClasses(component,
                    Objects.requireNonNull(classes)));
        }
        if (!UtilsKt.isBlank(withoutClasses)) {
            p.add(component -> LocatorKt.doesntHaveAnyClasses(component,
                    Objects.requireNonNull(withoutClasses)));
        }
        if (!UtilsKt.isBlank(themes)) {
            p.add(component -> LocatorKt.hasAllThemes(component,
                    Objects.requireNonNull(themes)));
        }
        if (!UtilsKt.isBlank(withoutThemes)) {
            p.add(component -> LocatorKt.notContainsThemes(component,
                    Objects.requireNonNull(withoutThemes)));
        }
        if (text != null) {
            p.add(component -> Objects.equals(component.getElement().getText(),
                    text));
        }
        if (value != null) {
            p.add(component -> component instanceof HasValue<?, ?> hasValue
                    && Objects.equals(hasValue.getValue(), value));
        }
        for (Predicate<T> predicate : predicates) {
            p.add(component -> clazz.isInstance(component)
                    && predicate.test((T) component));
        }
        return component -> {
            for (Function1<Component, Boolean> f : p) {
                if (!f.invoke(component)) {
                    return false;
                }
            }
            return true;
        };
    }

}
