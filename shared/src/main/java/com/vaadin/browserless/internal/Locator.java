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
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Location;

/**
 * Java port of the original Kotlin {@code Locator.kt}. Java callers should
 * invoke these static methods directly; Kotlin callers may continue to use the
 * conveniences in {@code LocatorDsl.kt}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class Locator {

    private static final Consumer<?> NOOP = spec -> {
    };

    @SuppressWarnings("unchecked")
    private static <T extends Component> Consumer<SearchSpec<T>> noop() {
        return (Consumer<SearchSpec<T>>) NOOP;
    }

    private Locator() {
    }

    /**
     * Finds a VISIBLE component of given {@code clazz} which matches given
     * {@code block}. This component and all of its descendants are searched.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the root component to search under.
     * @param clazz
     *            the component must be of this class.
     * @param block
     *            the search specification
     * @return the only matching component, never null.
     * @throws AssertionError
     *             if no component matched, or if more than one component
     *             matches.
     */
    public static <T extends Component> T _get(Component container,
            Class<T> clazz, Consumer<SearchSpec<T>> block) {
        final CountRange one = CountRange.ONE;
        List<T> result = _find(container, clazz, spec -> {
            spec.setCount(one);
            block.accept(spec);
            if (!spec.getCount().equals(one)) {
                throw new IllegalStateException(
                        "You're calling _get which is supposed to return exactly 1 component, yet you tried to specify the count of "
                                + spec.getCount());
            }
        });
        if (result.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly 1 result, got " + result.size());
        }
        return clazz.cast(result.get(0));
    }

    /**
     * Finds a VISIBLE component of given {@code clazz}; see
     * {@link #_get(Component, Class, Consumer)}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @return a VISIBLE component of given {@code clazz}; see
     *         {@link #_get(Component, Class, Consumer)}
     */
    public static <T extends Component> T _get(Component container,
            Class<T> clazz) {
        return _get(container, clazz, Locator.<T> noop());
    }

    /**
     * Finds a VISIBLE component in the current UI of given {@code clazz} which
     * matches given {@code block}. The current UI and all of its descendants
     * are searched.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     * @return a VISIBLE component in the current UI of given {@code clazz}
     *         which matches given {@code block}
     */
    public static <T extends Component> T _get(Class<T> clazz,
            Consumer<SearchSpec<T>> block) {
        return _get(Utils.currentUI(), clazz, block);
    }

    /**
     * Finds a VISIBLE component in the current UI of given {@code clazz}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @return a VISIBLE component in the current UI of given {@code clazz}
     */
    public static <T extends Component> T _get(Class<T> clazz) {
        return _get(Utils.currentUI(), clazz, Locator.<T> noop());
    }

    /**
     * Finds a list of VISIBLE components of given {@code clazz} which matches
     * {@code block}. This component and all of its descendants are searched.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     * @return the list of matching components, may be empty.
     */
    public static <T extends Component> List<T> _find(Component container,
            Class<T> clazz, Consumer<SearchSpec<T>> block) {
        SearchSpec<T> spec = new SearchSpec<>(clazz);
        block.accept(spec);
        List<Component> result = find(container, spec.toPredicate());
        CountRange count = spec.getCount();
        int size = result.size();
        if (size < count.getStart() || size > count.getEndInclusive()) {
            String loc = currentPath();
            if (loc == null) {
                loc = "?";
            }
            String message;
            if (result.isEmpty()) {
                message = "/" + loc + ": No visible " + clazz.getSimpleName();
            } else if (size < count.getStart()) {
                message = "/" + loc + ": Too few (" + size + ") visible "
                        + clazz.getSimpleName() + "s";
            } else {
                message = "/" + loc + ": Too many visible "
                        + clazz.getSimpleName() + "s (" + size + ")";
            }
            String matched = result.stream()
                    .map(PrettyPrintTree::toPrettyString)
                    .collect(Collectors.joining(", "));
            message = message + " in "
                    + PrettyPrintTree.toPrettyString(container) + " matching "
                    + spec + ": [" + matched + "]. Component tree:\n"
                    + PrettyPrintTree.toPrettyTree(container);

            // if there's a PolymerTemplate, warn that Browserless Testing can't
            // really locate components in there:
            // https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates
            // fixes https://github.com/mvysny/karibu-testing/issues/35
            boolean hasPolymerTemplates = Utils.hasPolymerTemplates()
                    && containsPolymerTemplate(container);
            if (hasPolymerTemplates) {
                message = message
                        + "\nWarning: Browserless Testing is not able to look up components from inside of PolymerTemplate."
                        + " Please see https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates for more details.";
            }

            // find() used to fail with IllegalArgumentException which makes
            // sense for a general-purpose utility method. However,
            // since find() is used in tests overwhelmingly, not finding the
            // correct set of components is generally treated as an assertion
            // error.
            throw new AssertionError(message);
        }
        List<T> filtered = new ArrayList<>(result.size());
        for (Component c : result) {
            if (clazz.isInstance(c)) {
                filtered.add(clazz.cast(c));
            }
        }
        return filtered;
    }

    /**
     * Finds a list of VISIBLE components of given {@code clazz}; see
     * {@link #_find(Component, Class, Consumer)}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @return a list of VISIBLE components of given {@code clazz}; see
     *         {@link #_find(Component, Class, Consumer)}
     */
    public static <T extends Component> List<T> _find(Component container,
            Class<T> clazz) {
        return _find(container, clazz, Locator.<T> noop());
    }

    /**
     * Finds a list of VISIBLE components in the current UI of given
     * {@code clazz} which matches {@code block}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     * @return a list of VISIBLE components in the current UI of given
     *         {@code clazz} which matches {@code block}
     */
    public static <T extends Component> List<T> _find(Class<T> clazz,
            Consumer<SearchSpec<T>> block) {
        return _find(Utils.currentUI(), clazz, block);
    }

    /**
     * Finds a list of VISIBLE components in the current UI of given
     * {@code clazz}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @return a list of VISIBLE components in the current UI of given
     *         {@code clazz}
     */
    public static <T extends Component> List<T> _find(Class<T> clazz) {
        return _find(Utils.currentUI(), clazz, Locator.<T> noop());
    }

    private static List<Component> find(Component root,
            Predicate<Component> predicate) {
        TestingLifecycleHooks.getCurrent().awaitBeforeLookup();
        List<Component> descendants = new ArrayList<>();
        for (Component c : _walkAll(root)) {
            descendants.add(c);
        }
        TestingLifecycleHooks.getCurrent().awaitAfterLookup();
        InternalServerError error = firstInternalServerError(descendants);
        if (error != null) {
            throw new AssertionError(
                    "An internal server error occurred; please check log for the actual stack-trace. Error text: "
                            + BasicUtils.errorMessage(error) + "\n"
                            + PrettyPrintTree.toPrettyTree(Utils.currentUI()));
        }
        List<Component> result = new ArrayList<>();
        for (Component c : descendants) {
            if (BasicUtils.isEffectivelyVisible(c) && predicate.test(c)) {
                result.add(c);
            }
        }
        return result;
    }

    private static InternalServerError firstInternalServerError(
            List<Component> components) {
        for (Component c : components) {
            if (c instanceof InternalServerError) {
                return (InternalServerError) c;
            }
        }
        return null;
    }

    private static boolean containsPolymerTemplate(Component root) {
        for (Component c : _walkAll(root)) {
            if (ComponentUtils.isPolymerTemplate(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the component child/descendant tree, depth-first: first the
     * component, then its descendants, then its next sibling.
     *
     * @param root
     *            the component to start from
     * @return the components, in depth-first order
     */
    public static Iterable<Component> _walkAll(final Component root) {
        return new Iterable<Component>() {
            @Override
            public Iterator<Component> iterator() {
                return new DepthFirstTreeIterator<>(root,
                        component -> TestingLifecycleHooks.getCurrent()
                                .getAllChildren(component));
            }
        };
    }

    /**
     * Expects that there are no VISIBLE components of given {@code clazz} which
     * matches {@code block}. This component and all of its descendants are
     * searched.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     * @throws AssertionError
     *             if one or more components matched.
     */
    public static <T extends Component> void _expectNone(Component container,
            Class<T> clazz, Consumer<SearchSpec<T>> block) {
        final CountRange zero = CountRange.exactly(0);
        List<T> result = _find(container, clazz, spec -> {
            spec.setCount(zero);
            block.accept(spec);
            if (!spec.getCount().equals(zero)) {
                throw new IllegalStateException(
                        "You're calling _expectNone which expects 0 component, yet you tried to specify the count of "
                                + spec.getCount());
            }
        });
        if (!result.isEmpty()) {
            // safety check that _find works as expected
            throw new IllegalStateException(
                    "_find returned non-empty result: " + result);
        }
    }

    /**
     * Equivalent to {@code _expectNone(container, clazz, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     */
    public static <T extends Component> void _expectNone(Component container,
            Class<T> clazz) {
        _expectNone(container, clazz, Locator.<T> noop());
    }

    /**
     * Expects that there are no VISIBLE components in the current UI of given
     * {@code clazz} which matches {@code block}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     */
    public static <T extends Component> void _expectNone(Class<T> clazz,
            Consumer<SearchSpec<T>> block) {
        _expectNone(Utils.currentUI(), clazz, block);
    }

    /**
     * Equivalent to
     * {@code _expectNone(Utils.currentUI(), clazz, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     */
    public static <T extends Component> void _expectNone(Class<T> clazz) {
        _expectNone(Utils.currentUI(), clazz, Locator.<T> noop());
    }

    /**
     * Expects that there are no dialogs shown.
     */
    public static void _expectNoDialogs() {
        _expectNone(Dialog.class);
    }

    /**
     * Expects that there is exactly one VISIBLE component of given
     * {@code clazz} which matches {@code block}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     */
    public static <T extends Component> void _expectOne(Component container,
            Class<T> clazz, Consumer<SearchSpec<T>> block) {
        // technically _expectOne is the same as _get, but the semantics differ
        // - with _get() we're "just" doing a lookup (and asserting on
        // the component later). _expectOne() explicitly declares in the test
        // sources that we want to check that there is exactly one such
        // component.
        _get(container, clazz, block);
    }

    /**
     * Equivalent to {@code _expectOne(container, clazz, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     */
    public static <T extends Component> void _expectOne(Component container,
            Class<T> clazz) {
        _expectOne(container, clazz, Locator.<T> noop());
    }

    /**
     * Equivalent to {@code _expectOne(Utils.currentUI(), clazz, block)}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @param block
     *            configures the search spec before the lookup runs
     */
    public static <T extends Component> void _expectOne(Class<T> clazz,
            Consumer<SearchSpec<T>> block) {
        _expectOne(Utils.currentUI(), clazz, block);
    }

    /**
     * Equivalent to
     * {@code _expectOne(Utils.currentUI(), clazz, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     */
    public static <T extends Component> void _expectOne(Class<T> clazz) {
        _expectOne(Utils.currentUI(), clazz, Locator.<T> noop());
    }

    /**
     * Expects that there are exactly {@code count} VISIBLE components of given
     * {@code clazz} match {@code block}. This component and all of its
     * descendants are searched.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @param count
     *            the expected number of items
     * @param block
     *            configures the search spec before the lookup runs
     */
    public static <T extends Component> void _expect(Component container,
            Class<T> clazz, int count, Consumer<SearchSpec<T>> block) {
        // technically _expect is the same as _find, but the semantics differ -
        // with _find() we're "just" doing a lookup (and asserting on
        // the components later). _expect() explicitly declares in the test
        // sources that we want to check that there are exactly x components
        // that match given spec.
        _find(container, clazz, spec -> {
            spec.setCount(CountRange.exactly(count));
            block.accept(spec);
        });
    }

    /**
     * Equivalent to
     * {@code _expect(container, clazz, count, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     * @param count
     *            the expected number of items
     */
    public static <T extends Component> void _expect(Component container,
            Class<T> clazz, int count) {
        _expect(container, clazz, count, Locator.<T> noop());
    }

    /**
     * Equivalent to {@code _expect(container, clazz, 1, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param container
     *            the component to search, itself included
     * @param clazz
     *            the component type to look for
     */
    public static <T extends Component> void _expect(Component container,
            Class<T> clazz) {
        _expect(container, clazz, 1, Locator.<T> noop());
    }

    /**
     * Equivalent to {@code _expect(Utils.currentUI(), clazz, count, block)}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @param count
     *            the expected number of items
     * @param block
     *            configures the search spec before the lookup runs
     */
    public static <T extends Component> void _expect(Class<T> clazz, int count,
            Consumer<SearchSpec<T>> block) {
        _expect(Utils.currentUI(), clazz, count, block);
    }

    /**
     * Equivalent to
     * {@code _expect(Utils.currentUI(), clazz, count, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     * @param count
     *            the expected number of items
     */
    public static <T extends Component> void _expect(Class<T> clazz,
            int count) {
        _expect(Utils.currentUI(), clazz, count, Locator.<T> noop());
    }

    /**
     * Equivalent to
     * {@code _expect(Utils.currentUI(), clazz, 1, Locator.<T> noop())}.
     *
     * @param <T>
     *            the item type
     * @param clazz
     *            the component type to look for
     */
    public static <T extends Component> void _expect(Class<T> clazz) {
        _expect(Utils.currentUI(), clazz, 1, Locator.<T> noop());
    }

    /**
     * Asserts that the {@link InternalServerError} page is currently being
     * shown, optionally with given {@code expectedErrorMessage}.
     *
     * @param expectedErrorMessage
     *            the message the error must contain
     */
    public static void _expectInternalServerError(String expectedErrorMessage) {
        TestingLifecycleHooks.getCurrent().awaitBeforeLookup();
        List<Component> descendants = new ArrayList<>();
        for (Component c : _walkAll(Utils.currentUI())) {
            descendants.add(c);
        }
        TestingLifecycleHooks.getCurrent().awaitAfterLookup();
        InternalServerError error = firstInternalServerError(descendants);
        if (error == null) {
            throw new AssertionError(
                    "Expected an internal server error but none happened. Component tree:\n"
                            + PrettyPrintTree.toPrettyTree(Utils.currentUI()));
        }
        if (!BasicUtils.errorMessage(error).contains(expectedErrorMessage)) {
            throw new AssertionError(
                    "Expected InternalServerError with message '"
                            + expectedErrorMessage + "' but was '"
                            + BasicUtils.errorMessage(error)
                            + "'. Component tree:\n"
                            + PrettyPrintTree.toPrettyTree(Utils.currentUI()));
        }
    }

    /**
     * Equivalent to {@code _expectInternalServerError("")}.
     */
    public static void _expectInternalServerError() {
        _expectInternalServerError("");
    }

    /**
     * Returns the browser's current path. Returns null if there is no current
     * UI.
     *
     * @return the browser's current path
     */
    public static String currentPath() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return null;
        }
        Location loc = ui.getInternals().getActiveViewLocation();
        if (loc == null) {
            return null;
        }
        return loc.getPathWithQueryParameters();
    }

    /**
     * Filters out nulls and blank strings, returning a list of non-blank
     * strings.
     */
    static List<String> filterNotBlank(Iterable<String> source) {
        List<String> result = new ArrayList<>();
        for (String s : source) {
            if (s != null && !s.isBlank()) {
                result.add(s);
            }
        }
        return result;
    }
}
