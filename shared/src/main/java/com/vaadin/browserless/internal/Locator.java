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
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.ranges.IntRange;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Location;

/**
 * Functions for looking up components in the component tree.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class Locator {

    private Locator() {
    }

    /**
     * Returns the non-null, non-blank strings of given iterable.
     *
     * @param strings
     *            the strings to filter, not null.
     * @return the non-null, non-blank strings.
     */
    public static List<String> filterNotBlank(Iterable<String> strings) {
        List<String> result = new ArrayList<>();
        for (String s : strings) {
            if (!Utils.isBlank(s)) {
                result.add(s);
            }
        }
        return result;
    }

    private static List<String> splitBySpace(String s) {
        return filterNotBlank(Arrays.asList(s.split(" ", -1)));
    }

    static boolean hasAllClasses(Component component, String classes) {
        if (classes.contains(" ")) {
            return splitBySpace(classes).stream()
                    .allMatch(it -> hasAllClasses(component, it));
        }
        if (!(component instanceof HasStyle hasStyle)) {
            return false;
        }
        return hasStyle.getClassNames().contains(classes);
    }

    static boolean doesntHaveAnyClasses(Component component, String classes) {
        if (classes.contains(" ")) {
            return splitBySpace(classes).stream()
                    .allMatch(it -> !hasAllClasses(component, it));
        }
        if (!(component instanceof HasStyle hasStyle)) {
            return true;
        }
        return !hasStyle.getClassNames().contains(classes);
    }

    static boolean hasAllThemes(Component component, String themes) {
        return splitBySpace(themes).stream().allMatch(
                it -> component.getElement().getThemeList().contains(it));
    }

    static boolean notContainsThemes(Component component, String themes) {
        return splitBySpace(themes).stream().allMatch(
                it -> !component.getElement().getThemeList().contains(it));
    }

    /**
     * Finds a VISIBLE component of given {@code clazz} which matches given
     * {@code block}. This component and all of its descendants are searched.
     *
     * @param receiver
     *            the component to search in.
     * @param clazz
     *            the component must be of this class.
     * @param block
     *            the search specification
     * @return the only matching component, never null.
     * @throws AssertionError
     *             if no component matched, or if more than one component
     *             matches.
     */
    public static <T extends Component> T _get(Component receiver,
            Class<T> clazz, Function1<? super SearchSpec<T>, Unit> block) {
        List<T> result = _find(receiver, clazz, spec -> {
            spec.setCount(new IntRange(1, 1));
            block.invoke(spec);
            if (!spec.getCount().equals(new IntRange(1, 1))) {
                throw new IllegalStateException(
                        "You're calling _get which is supposed to return exactly 1 component, yet you tried to specify the count of "
                                + spec.getCount());
            }
            return Unit.INSTANCE;
        });
        return clazz.cast(single(result));
    }

    /**
     * Finds a VISIBLE component in the current UI of given {@code clazz} which
     * matches given {@code block}. The current UI and all of its descendants
     * are searched.
     *
     * @param clazz
     *            the component must be of this class.
     * @param block
     *            the search specification
     * @return the only matching component, never null.
     * @throws AssertionError
     *             if no component matched, or if more than one component
     *             matches.
     */
    public static <T extends Component> T _get(Class<T> clazz,
            Function1<? super SearchSpec<T>, Unit> block) {
        return _get(Utils.getCurrentUI(), clazz, block);
    }

    /**
     * Finds a list of VISIBLE components of given {@code clazz} which matches
     * {@code block}. This component and all of its descendants are searched.
     *
     * @param receiver
     *            the component to search in.
     * @param clazz
     *            the component must be of this class.
     * @param block
     *            the search specification
     * @return the list of matching components, may be empty.
     * @throws AssertionError
     *             if the number of matching components is not within the
     *             expected count.
     */
    public static <T extends Component> List<T> _find(Component receiver,
            Class<T> clazz, Function1<? super SearchSpec<T>, Unit> block) {
        SearchSpec<T> spec = new SearchSpec<>(clazz);
        block.invoke(spec);
        List<Component> result = find(receiver, spec.toPredicate());
        IntRange count = spec.getCount();
        if (!count.contains(result.size())) {
            String path = getCurrentPath();
            String loc = path != null ? path : "?";

            String message;
            if (result.isEmpty()) {
                message = "/" + loc + ": No visible " + clazz.getSimpleName();
            } else if (result.size() < count.getFirst()) {
                message = "/" + loc + ": Too few (" + result.size()
                        + ") visible " + clazz.getSimpleName() + "s";
            } else {
                message = "/" + loc + ": Too many visible "
                        + clazz.getSimpleName() + "s (" + result.size() + ")";
            }
            message = message + " in "
                    + PrettyPrintTree.toPrettyString(receiver) + " matching "
                    + spec + ": ["
                    + result.stream().map(PrettyPrintTree::toPrettyString)
                            .collect(Collectors.joining(", "))
                    + "]. Component tree:\n"
                    + PrettyPrintTree.toPrettyTree(receiver);

            // if there's a PolymerTemplate, warn that Browserless Testing
            // can't really locate components in there:
            // https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates
            // fixes https://github.com/mvysny/karibu-testing/issues/35
            boolean hasPolymerTemplates = false;
            if (Utils.hasPolymerTemplates()) {
                for (Component c : _walkAll(receiver)) {
                    if (Utils.isPolymerTemplate(c)) {
                        hasPolymerTemplates = true;
                        break;
                    }
                }
            }
            if (hasPolymerTemplates) {
                message = message
                        + "\nWarning: Browserless Testing is not able to look up components from inside of PolymerTemplate. Please see https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates for more details.";
            }

            // find() used to fail with IllegalArgumentException which makes
            // sense for a general-purpose utility method. However, since
            // find() is used in tests overwhelmingly, not finding the correct
            // set of components is generally treated as an assertion error.
            throw new AssertionError(message);
        }
        List<T> filtered = new ArrayList<>();
        for (Component c : result) {
            if (clazz.isInstance(c)) {
                filtered.add(clazz.cast(c));
            }
        }
        return filtered;
    }

    /**
     * Finds a list of VISIBLE components of given {@code clazz} which matches
     * {@code block}. The {@link UI#getCurrent()} and all of its descendants are
     * searched.
     *
     * @param clazz
     *            the component must be of this class.
     * @param block
     *            the search specification
     * @return the list of matching components, may be empty.
     */
    public static <T extends Component> List<T> _find(Class<T> clazz,
            Function1<? super SearchSpec<T>, Unit> block) {
        return _find(Utils.getCurrentUI(), clazz, block);
    }

    private static List<Component> find(Component receiver,
            Function1<Component, Boolean> predicate) {
        TestingLifecycleHooks.getTestingLifecycleHook().awaitBeforeLookup();
        List<Component> descendants = new ArrayList<>();
        _walkAll(receiver).forEach(descendants::add);
        TestingLifecycleHooks.getTestingLifecycleHook().awaitAfterLookup();
        InternalServerError error = firstInternalServerError(descendants);
        if (error != null) {
            throw new AssertionError(
                    "An internal server error occurred; please check log for the actual stack-trace. Error text: "
                            + BasicUtils.getErrorMessage(error) + "\n"
                            + PrettyPrintTree
                                    .toPrettyTree(Utils.getCurrentUI()));
        }
        List<Component> result = new ArrayList<>();
        for (Component c : descendants) {
            if (BasicUtils.isEffectivelyVisible(c) && predicate.invoke(c)) {
                result.add(c);
            }
        }
        return result;
    }

    private static InternalServerError firstInternalServerError(
            List<Component> components) {
        for (Component c : components) {
            if (c instanceof InternalServerError error) {
                return error;
            }
        }
        return null;
    }

    private static <T> T single(List<T> list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException("List is empty.");
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException(
                    "List has more than one element.");
        }
        return list.get(0);
    }

    /**
     * Walks the component child/descendant tree, depth-first: first the
     * component, then its descendants, then its next sibling.
     *
     * @param receiver
     *            the root component.
     * @return the iterable over the component and all of its descendants.
     */
    public static Iterable<Component> _walkAll(Component receiver) {
        return () -> new DepthFirstTreeIterator<Component>(receiver,
                component -> TestingLifecycleHooks.getTestingLifecycleHook()
                        .getAllChildren(component));
    }

    /**
     * Expects that there are no VISIBLE components of given {@code clazz} which
     * matches {@code block}. This component and all of its descendants are
     * searched.
     *
     * @param receiver
     *            the component to search in.
     * @param clazz
     *            the component class.
     * @param block
     *            the search specification
     * @throws AssertionError
     *             if one or more components matched.
     */
    public static <T extends Component> void _expectNone(Component receiver,
            Class<T> clazz, Function1<? super SearchSpec<T>, Unit> block) {
        List<T> result = _find(receiver, clazz, spec -> {
            spec.setCount(new IntRange(0, 0));
            block.invoke(spec);
            if (!spec.getCount().equals(new IntRange(0, 0))) {
                throw new IllegalStateException(
                        "You're calling _expectNone which expects 0 component, yet you tried to specify the count of "
                                + spec.getCount());
            }
            return Unit.INSTANCE;
        });
        // safety check that _find works as expected
        if (!result.isEmpty()) {
            throw new IllegalStateException("Check failed.");
        }
    }

    /**
     * Expects that there are no dialogs shown.
     */
    public static void _expectNoDialogs() {
        List<Class<? extends Component>> dialogTypes = ComponentSupports
                .getDialogTypes();
        if (dialogTypes.isEmpty()) {
            throw new IllegalStateException(
                    "No ComponentSupport reports any dialog types, so there "
                            + "is nothing to check");
        }
        for (Class<? extends Component> dialogType : dialogTypes) {
            _expectNone(dialogType, spec -> Unit.INSTANCE);
        }
    }

    /**
     * Expects that there are no VISIBLE components in the current UI of given
     * {@code clazz} which matches {@code block}. The current UI and all of its
     * descendants are searched.
     *
     * @param clazz
     *            the component class.
     * @param block
     *            the search specification
     * @throws AssertionError
     *             if one or more components matched.
     */
    public static <T extends Component> void _expectNone(Class<T> clazz,
            Function1<? super SearchSpec<T>, Unit> block) {
        _expectNone(Utils.getCurrentUI(), clazz, block);
    }

    /**
     * Expects that there is exactly one VISIBLE components of given
     * {@code clazz} which matches {@code block}. This component and all of its
     * descendants are searched.
     *
     * @param receiver
     *            the component to search in.
     * @param clazz
     *            the component class.
     * @param block
     *            the search specification
     * @throws AssertionError
     *             if none, or more than one components matched.
     */
    public static <T extends Component> void _expectOne(Component receiver,
            Class<T> clazz, Function1<? super SearchSpec<T>, Unit> block) {
        // technically _expectOne is the same as _get, but the semantics differ
        // - with _get() we're "just" doing a lookup (and asserting on the
        // component later). _expectOne() explicitly declares in the test
        // sources that we want to check that there is exactly one such
        // component.
        _get(receiver, clazz, block);
    }

    /**
     * Expects that there is exactly one VISIBLE components in the current UI of
     * given {@code clazz} which matches {@code block}. The current UI and all
     * of its descendants are searched.
     *
     * @param clazz
     *            the component class.
     * @param block
     *            the search specification
     * @throws AssertionError
     *             if none, or more than one components matched.
     */
    public static <T extends Component> void _expectOne(Class<T> clazz,
            Function1<? super SearchSpec<T>, Unit> block) {
        _expectOne(Utils.getCurrentUI(), clazz, block);
    }

    /**
     * Expects that there are exactly {@code count} VISIBLE components of given
     * {@code clazz} match {@code block}. This component and all of its
     * descendants are searched.
     * <p>
     * Special cases: for asserting one component use {@code _expectOne}. For
     * asserting no components use {@code _expectNone}.
     *
     * @param receiver
     *            the component to search in.
     * @param clazz
     *            the component class.
     * @param count
     *            the expected number of matching components.
     * @param block
     *            the search specification
     * @throws AssertionError
     *             if incorrect count of component matched.
     */
    public static <T extends Component> void _expect(Component receiver,
            Class<T> clazz, int count,
            Function1<? super SearchSpec<T>, Unit> block) {
        // technically _expect is the same as _find, but the semantics differ -
        // with _find() we're "just" doing a lookup (and asserting on the
        // components later). _expect() explicitly declares in the test sources
        // that we want to check that there are exactly x components that match
        // given spec.
        _find(receiver, clazz, spec -> {
            spec.setCount(new IntRange(count, count));
            block.invoke(spec);
            return Unit.INSTANCE;
        });
    }

    /**
     * Expects that there are exactly {@code count} VISIBLE components in the
     * current UI with given {@code clazz} match {@code block}. The current UI
     * and all of its descendants are searched.
     * <p>
     * Special cases: for asserting one component use {@code _expectOne}. For
     * asserting no components use {@code _expectNone}. They provide a better
     * error message in case of a failure, and also communicate your intent as a
     * test writer better in the test code.
     *
     * @param clazz
     *            the component class.
     * @param count
     *            the expected number of matching components.
     * @param block
     *            the search specification
     * @throws AssertionError
     *             if incorrect count of component matched.
     */
    public static <T extends Component> void _expect(Class<T> clazz, int count,
            Function1<? super SearchSpec<T>, Unit> block) {
        _expect(Utils.getCurrentUI(), clazz, count, block);
    }

    /**
     * Asserts that the {@link InternalServerError} page is currently being
     * shown, optionally with given {@code expectedErrorMessage}.
     *
     * @param expectedErrorMessage
     *            the text the error message must contain; an empty string
     *            matches any message.
     */
    public static void _expectInternalServerError(String expectedErrorMessage) {
        TestingLifecycleHooks.getTestingLifecycleHook().awaitBeforeLookup();
        List<Component> descendants = new ArrayList<>();
        _walkAll(Utils.getCurrentUI()).forEach(descendants::add);
        TestingLifecycleHooks.getTestingLifecycleHook().awaitAfterLookup();
        InternalServerError error = firstInternalServerError(descendants);
        if (error == null) {
            throw new AssertionError(
                    "Expected an internal server error but none happened. Component tree:\n"
                            + PrettyPrintTree
                                    .toPrettyTree(Utils.getCurrentUI()));
        }
        String errorMessage = BasicUtils.getErrorMessage(error);
        if (!errorMessage.contains(expectedErrorMessage)) {
            throw new AssertionError(
                    "Expected InternalServerError with message '"
                            + expectedErrorMessage + "' but was '"
                            + errorMessage + "'. Component tree:\n"
                            + PrettyPrintTree
                                    .toPrettyTree(Utils.getCurrentUI()));
        }
    }

    /**
     * Returns the browser's current path. Returns null if there is no current
     * UI.
     *
     * @return the current path with query parameters, or null.
     */
    public static String getCurrentPath() {
        UI ui = UI.getCurrent();
        if (ui == null || ui.getInternals() == null) {
            return null;
        }
        Location location = ui.getInternals().getActiveViewLocation();
        return location == null ? null : location.getPathWithQueryParameters();
    }
}
