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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;

/**
 * If you need to hook into the testing lifecycle (e.g. you need to wait for any
 * async operations to finish), provide your own custom implementation of this
 * interface, then set it via
 * {@link TestingLifecycleHooks#setTestingLifecycleHook(TestingLifecycleHook)}.
 *
 * <h2>Mocking server request end</h2>
 *
 * Since Browserless Testing runs in the same JVM as the server and there is no
 * browser, the boundaries between the client and the server become unclear.
 * When looking into sources of any test method, it's really hard to tell where
 * exactly the server request ends, and where another request starts.
 * <p>
 * You can establish an explicit client boundary in your test, by explicitly
 * calling {@link MockVaadin#clientRoundtrip()}. However, since that would be
 * both laborous and error-prone, the default operation is that Browserless
 * Testing pretends as if there was a client-server roundtrip before every
 * component lookup via the {@code _get}/{@code _find}/{@code _expectNone}/
 * {@code _expectOne} call. Therefore, {@link MockVaadin#clientRoundtrip()} is
 * called from {@link #awaitBeforeLookup()} by default.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public interface TestingLifecycleHook {

    /**
     * Provides the default lifecycle hook.
     */
    Companion Companion = new Companion();

    /**
     * Invoked before every component lookup. You can e.g. wait for any async
     * operations to finish and for the server to settle down.
     * <p>
     * The default implementation calls the {@link MockVaadin#clientRoundtrip()}
     * method. When implementing this method, you should also call
     * {@link MockVaadin#clientRoundtrip()} (or simply call super).
     */
    default void awaitBeforeLookup() {
        defaultAwaitBeforeLookup();
    }

    /**
     * Invoked after every component lookup. You can e.g. wait for any async
     * operations to finish and for the server to settle down. Invoked even if
     * the {@code _get()}/{@code _find()}/{@code _expectNone()} function fails.
     */
    default void awaitAfterLookup() {
    }

    /**
     * Provides all children of given component. Provides workarounds for
     * certain components:
     * <ul>
     * <li>For {@link Grid} the function will also return the header and footer
     * cell components of all of its columns, plus the column editor
     * components.</li>
     * <li>For {@link MenuItemBase} the function returns all items of a
     * sub-menu.</li>
     * </ul>
     *
     * @param component
     *            the component whose children to return.
     * @return the children of the component.
     */
    default List<Component> getAllChildren(Component component) {
        return defaultGetAllChildren(component);
    }

    private static void defaultAwaitBeforeLookup() {
        if (UI.getCurrent() != null) {
            MockVaadin.clientRoundtrip();
        }
    }

    private static List<Component> defaultGetAllChildren(Component component) {
        if (component instanceof Grid<?> grid) {
            // Header/footer components live as virtual children of the Column
            // (or ColumnGroup) they belong to, and both of those branches drop
            // virtual children on purpose: a component in a merged cell is a
            // virtual child of every column it spans and would otherwise show
            // up more than once. Collect them here instead, once per Grid.
            // see https://github.com/mvysny/karibu-testing/issues/52
            List<Component> result = new ArrayList<>();
            grid.getHeaderRows().stream()
                    .flatMap(row -> row.getCells().stream()
                            .map(cell -> cell.getComponent()))
                    .filter(Objects::nonNull).forEach(result::add);
            grid.getFooterRows().stream()
                    .flatMap(row -> row.getCells().stream()
                            .map(cell -> cell.getComponent()))
                    .filter(Objects::nonNull).forEach(result::add);
            grid.getColumns().stream()
                    .map(column -> column.getEditorComponent())
                    .filter(Objects::nonNull).forEach(result::add);
            component.getChildren().forEach(result::add);
            ComponentUtil.getAllChildren(component).forEach(result::add);
            return distinct(result.stream());
        }
        if (component instanceof MenuItemBase<?, ?, ?> menuItem) {
            // also include component.children:
            // https://github.com/mvysny/karibu-testing/issues/76
            return distinct(Stream.concat(component.getChildren(),
                    menuItem.getSubMenu().getItems().stream()));
        }
        if (component instanceof MenuBar) {
            // don't include virtual children since that would make the
            // MenuItems appear two times.
            return toList(component.getChildren());
        }
        if (TestingLifecycleHooks.isTemplate(component)) {
            // don't include virtual children since those will include nested
            // components. however, those components are only they are only
            // "shallow shells" of components constructed server-side - almost
            // none of their properties are transferred to the server-side.
            // Listing those components with null captions and other
            // properties would only be confusing. Therefore, let's leave the
            // virtual children out for now. See
            // https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates--lit-templates
            return toList(component.getChildren());
        }
        if (component.getClass().getName()
                .equals("com.vaadin.flow.component.grid.ColumnGroup")) {
            // don't include virtual children since that would include the
            // header/footer components, which the Grid branch already reports
            return toList(component.getChildren());
        }
        if (component instanceof Grid.Column<?>) {
            // don't include virtual children since that would include the
            // header/footer components, which the Grid branch already reports
            return toList(component.getChildren());
        }
        if (component instanceof Composite<?>) {
            // The Composite class overrides getChildren() to return a stream
            // with the wrapped component, but also getElement() returning the
            // Element of the wrapped component. The latter causes the virtual
            // child to be fetched as Composite direct child, thus duplicating
            // any virtual children the child component might have.
            return toList(component.getChildren());
        }
        // Union the component's own reported children with Flow's
        // element-tree traversal. Neither source alone is complete:
        // getChildren() may hide element children (Dialog and Card exclude
        // slotted content) while ComponentUtil.getAllChildren, which walks
        // the element hierarchy (regular and virtual children, descending
        // through non-component wrapper elements), misses children that are
        // not part of the element tree (ContextMenu keeps its items in a
        // server-side MenuManager list that getChildren() delegates to).
        // getAllChildren also keeps virtual children, see
        // https://github.com/mvysny/karibu-testing/issues/85.
        return distinct(Stream.concat(component.getChildren(),
                ComponentUtil.getAllChildren(component)));
    }

    private static List<Component> toList(Stream<? extends Component> stream) {
        return stream.collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Component> distinct(
            Stream<? extends Component> stream) {
        return toList(stream.distinct());
    }

    /**
     * Provides the default lifecycle hook.
     */
    final class Companion {

        private Companion() {
        }

        /**
         * A default lifecycle hook that simply runs default implementations of
         * the hook functions.
         *
         * @return a new default lifecycle hook.
         */
        public TestingLifecycleHook getDefault() {
            return new TestingLifecycleHook() {
            };
        }
    }

    /**
     * Static entry points to the default implementations, kept for binary
     * compatibility with implementations compiled from Kotlin.
     */
    final class DefaultImpls {

        private DefaultImpls() {
        }

        /**
         * Runs the default implementation of
         * {@link TestingLifecycleHook#awaitBeforeLookup()}.
         *
         * @param hook
         *            the hook.
         */
        public static void awaitBeforeLookup(TestingLifecycleHook hook) {
            defaultAwaitBeforeLookup();
        }

        /**
         * Runs the default implementation of
         * {@link TestingLifecycleHook#awaitAfterLookup()}.
         *
         * @param hook
         *            the hook.
         */
        public static void awaitAfterLookup(TestingLifecycleHook hook) {
        }

        /**
         * Runs the default implementation of
         * {@link TestingLifecycleHook#getAllChildren(Component)}.
         *
         * @param hook
         *            the hook.
         * @param component
         *            the component whose children to return.
         * @return the children of the component.
         */
        public static List<Component> getAllChildren(TestingLifecycleHook hook,
                Component component) {
            return defaultGetAllChildren(component);
        }
    }
}
