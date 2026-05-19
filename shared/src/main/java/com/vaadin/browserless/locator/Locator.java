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
package com.vaadin.browserless.locator;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.flow.component.Component;

/**
 * Prototype base class for the {@code get*} tester API. A locator is a fluent
 * combination of a {@link ComponentQuery} filter chain and a tester: the
 * subclass exposes both the filter methods inherited from this class and the
 * action methods specific to the component type.
 * <p>
 * Resolution is deferred to the first action call ({@link #component()}) and
 * cached. Calling any filter method after resolution invalidates the cache so
 * the next action re-resolves. This means a single locator instance can be
 * reused across an asynchronous boundary (e.g. {@code roundTrip()}) without
 * holding on to a stale component reference.
 *
 * @param <C>
 *            the component type
 * @param <SELF>
 *            the concrete locator subtype, used for fluent chaining
 */
public abstract class Locator<C extends Component, SELF extends Locator<C, SELF>> {

    private final ComponentQuery<C> query;
    private C resolved;
    private int pickIndex;

    /**
     * Creates a locator that searches for components of the given type from the
     * current {@code UI} root.
     *
     * @param componentType
     *            the component type to match
     */
    protected Locator(Class<C> componentType) {
        this.query = new ComponentQuery<>(componentType);
    }

    /** Requires the matched component to have the given id. */
    public SELF withId(String id) {
        invalidate();
        query.withId(id);
        return self();
    }

    /**
     * Requires the matched component to have a caption equal to the given text.
     */
    public SELF withCaption(String caption) {
        invalidate();
        query.withCaption(caption);
        return self();
    }

    /**
     * Requires the matched component to have a caption containing the given
     * text.
     */
    public SELF withCaptionContaining(String text) {
        invalidate();
        query.withCaptionContaining(text);
        return self();
    }

    /** Requires the text content of the component to equal the given text. */
    public SELF withText(String text) {
        invalidate();
        query.withText(text);
        return self();
    }

    /** Requires the text content of the component to contain the given text. */
    public SELF withTextContaining(String text) {
        invalidate();
        query.withTextContaining(text);
        return self();
    }

    /** Requires the matched component to have all the given CSS class names. */
    public SELF withClassName(String... className) {
        invalidate();
        query.withClassName(className);
        return self();
    }

    /** Requires the matched component to have none of the given CSS class names. */
    public SELF withoutClassName(String... className) {
        invalidate();
        query.withoutClassName(className);
        return self();
    }

    /** Requires the matched component to have the given theme set. */
    public SELF withTheme(String theme) {
        invalidate();
        query.withTheme(theme);
        return self();
    }

    /** Requires the matched component to not have the given theme set. */
    public SELF withoutTheme(String theme) {
        invalidate();
        query.withoutTheme(theme);
        return self();
    }

    /** Requires the matched component to have the given attribute set. */
    public SELF withAttribute(String attribute) {
        invalidate();
        query.withAttribute(attribute);
        return self();
    }

    /**
     * Requires the matched component to have the given attribute with the
     * expected value.
     */
    public SELF withAttribute(String attribute, String value) {
        invalidate();
        query.withAttribute(attribute, value);
        return self();
    }

    /** Requires the matched component not to have the given attribute. */
    public SELF withoutAttribute(String attribute) {
        invalidate();
        query.withoutAttribute(attribute);
        return self();
    }

    /**
     * Requires the matched component not to have the given attribute value
     * (or not to have the attribute at all).
     */
    public SELF withoutAttribute(String attribute, String value) {
        invalidate();
        query.withoutAttribute(attribute, value);
        return self();
    }

    /**
     * Requires the matched component to implement {@code HasValue} and to
     * have the given value. Has no effect when {@code expectedValue} is
     * {@code null}.
     */
    public <V> SELF withValue(V expectedValue) {
        invalidate();
        query.withValue(expectedValue);
        return self();
    }

    /** Requires the matched component to satisfy the given predicate. */
    public SELF withCondition(Predicate<C> condition) {
        invalidate();
        query.withCondition(condition);
        return self();
    }

    /**
     * Escape hatch for filters not directly exposed on Locator. Applies the
     * given operator to the underlying {@link ComponentQuery}, letting users
     * compose any filter the query supports without subclassing.
     *
     * <pre>
     * findButton().with(q -&gt; q.withPropertyValue(Button::getText, "Save"))
     *         .click();
     * </pre>
     */
    public SELF with(UnaryOperator<ComponentQuery<C>> op) {
        invalidate();
        op.apply(query);
        return self();
    }

    /** Scopes the search to descendants of the given component. */
    public SELF inside(Component parent) {
        invalidate();
        query.from(parent);
        return self();
    }

    /**
     * Scopes the search to descendants of the component matched by the given
     * locator. The parent locator is resolved on demand.
     */
    public SELF inside(Locator<?, ?> parent) {
        return inside(parent.component());
    }

    /**
     * Picks the n-th match (1-based) when the filter chain yields multiple
     * matches. Without this, the default expectation is exactly one match.
     */
    public SELF atIndex(int index) {
        invalidate();
        this.pickIndex = index;
        return self();
    }

    /**
     * Resolves the locator to a single matching component, caching the result.
     * Subclasses call this from action methods (e.g. {@code click}).
     *
     * @return the matched component
     * @throws java.util.NoSuchElementException
     *             if no component matches or more than one matches (and no
     *             {@link #atIndex(int)} was provided)
     */
    public C component() {
        if (resolved == null) {
            resolved = pickIndex > 0 ? query.atIndex(pickIndex)
                    : query.single();
        }
        return resolved;
    }

    /**
     * Returns all matching components, bypassing the cache. Useful for
     * assertions on counts without committing to a single match.
     */
    public List<C> components() {
        return query.all();
    }

    /**
     * Returns {@code true} if the filter chain matches at least one component.
     */
    public boolean exists() {
        return query.exists();
    }

    /**
     * Discards any cached resolution. Call after a UI change that may have
     * replaced or detached the previously resolved component.
     */
    public SELF invalidate() {
        resolved = null;
        return self();
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }
}
