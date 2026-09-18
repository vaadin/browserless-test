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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.menubar.MenuBar;

/**
 * The hook a test can implement to take part in the lookup lifecycle, e.g. to
 * wait for an asynchronous operation to finish before a component is looked up.
 * Install an implementation through {@link TestingLifecycleHooks#current}.
 * <h2>Where the server request ends</h2> Browserless Test runs in the same JVM
 * as the server and there is no browser, so the boundary between one request
 * and the next is not visible in the source of a test method.
 * <p>
 * A test can mark that boundary explicitly by calling
 * {@link MockVaadin#clientRoundtrip()}, but doing so before every lookup would
 * be laborious and easy to forget. Instead the environment behaves as if a
 * client-server round trip happened before every component lookup:
 * {@link #awaitBeforeLookup()} calls {@code clientRoundtrip()} by default.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public interface TestingLifecycleHook {

    /**
     * A hook that does nothing beyond the default implementations below.
     */
    TestingLifecycleHook DEFAULT = new TestingLifecycleHook() {
    };

    /**
     * Invoked before every component lookup, e.g. to wait for an asynchronous
     * operation to finish and the server to settle down.
     * <p>
     * The default implementation calls {@link MockVaadin#clientRoundtrip()}. An
     * implementation that replaces it should call that too, or call
     * {@code super}.
     */
    default void awaitBeforeLookup() {
        if (UI.getCurrent() != null) {
            MockVaadin.clientRoundtrip();
        }
    }

    /**
     * Invoked after every component lookup, e.g. to wait for an asynchronous
     * operation to finish and the server to settle down. Invoked even when the
     * lookup itself failed.
     */
    default void awaitAfterLookup() {
    }

    /**
     * Provides all children of the given component, with workarounds for the
     * components whose children Flow does not report the way a lookup needs:
     * <ul>
     * <li>for a {@link Grid}, the header and footer cell components of all of
     * its columns, plus the column editor components;</li>
     * <li>for a {@link MenuItemBase}, all items of its sub-menu.</li>
     * </ul>
     *
     * @param component
     *            the component whose children to provide
     * @return the children, never {@code null}
     */
    default List<Component> getAllChildren(Component component) {
        if (component instanceof Grid) {
            // Header/footer components live as virtual children of the Column
            // (or ColumnGroup) they belong to, and both of those branches drop
            // virtual children on purpose: a component in a merged cell is a
            // virtual child of every column it spans and would otherwise show
            // up more than once. Collect them here instead, once per Grid.
            // see https://github.com/mvysny/karibu-testing/issues/52
            Grid<?> grid = (Grid<?>) component;
            LinkedHashSet<Component> distinct = new LinkedHashSet<>();
            grid.getHeaderRows().stream()
                    .flatMap(row -> row.getCells().stream())
                    .map(HeaderRow.HeaderCell::getComponent)
                    .filter(Objects::nonNull).forEach(distinct::add);
            grid.getFooterRows().stream()
                    .flatMap(row -> row.getCells().stream())
                    .map(FooterRow.FooterCell::getComponent)
                    .filter(Objects::nonNull).forEach(distinct::add);
            grid.getColumns().stream().map(Grid.Column::getEditorComponent)
                    .filter(Objects::nonNull).forEach(distinct::add);
            component.getChildren().forEach(distinct::add);
            ComponentUtil.getAllChildren(component).forEach(distinct::add);
            return new ArrayList<>(distinct);
        }
        if (component instanceof MenuItemBase) {
            // also include component.children:
            // https://github.com/mvysny/karibu-testing/issues/76
            MenuItemBase<?, ?, ?> menuItem = (MenuItemBase<?, ?, ?>) component;
            LinkedHashSet<Component> distinct = new LinkedHashSet<>();
            distinct.addAll(
                    component.getChildren().collect(Collectors.toList()));
            distinct.addAll(menuItem.getSubMenu().getItems());
            return new ArrayList<>(distinct);
        }
        if (component instanceof MenuBar) {
            // don't include virtual children since that would make the
            // MenuItems appear two times.
            return component.getChildren().collect(Collectors.toList());
        }
        if (isTemplate(component)) {
            // don't include virtual children since those will include nested
            // components.
            // however, those components are only they are only "shallow shells"
            // of components constructed
            // server-side - almost none of their properties are transferred to
            // the server-side.
            // Listing those components with null captions and other properties
            // would only be confusing.
            // Therefore, let's leave the virtual children out for now.
            // See
            // https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates--lit-templates
            return component.getChildren().collect(Collectors.toList());
        }
        if ("com.vaadin.flow.component.grid.ColumnGroup"
                .equals(component.getClass().getName())) {
            // don't include virtual children since that would include the
            // header/footer components, which the Grid branch already reports
            return component.getChildren().collect(Collectors.toList());
        }
        if (component instanceof Grid.Column) {
            // don't include virtual children since that would include the
            // header/footer components, which the Grid branch already reports
            return component.getChildren().collect(Collectors.toList());
        }
        if (component instanceof Composite) {
            // The Composite class overrides getChildren() to return a stream
            // with the wrapped component,
            // but also getElement() returning the Element of the wrapped
            // component.
            // The latter causes the virtual child to be fetched as Composite
            // direct child,
            // thus duplicating any virtual children the child component might
            // have.
            return component.getChildren().collect(Collectors.toList());
        }
        // Union the component's own reported children with Flow's element-tree
        // traversal. Neither source alone is complete: getChildren() may hide
        // element children (Dialog and Card exclude slotted content) while
        // ComponentUtil.getAllChildren, which walks the element hierarchy
        // (regular and virtual children, descending through non-component
        // wrapper elements), misses children that are not part of the element
        // tree (ContextMenu keeps its items in a server-side MenuManager list
        // that getChildren() delegates to). getAllChildren also keeps virtual
        // children, see https://github.com/mvysny/karibu-testing/issues/85.
        LinkedHashSet<Component> distinct = new LinkedHashSet<>();
        component.getChildren().forEach(distinct::add);
        ComponentUtil.getAllChildren(component).forEach(distinct::add);
        return new ArrayList<>(distinct);
    }

    /**
     * Tells whether the component is a template, i.e. a Lit or Polymer template
     * whose children only exist client-side.
     *
     * @param component
     *            the component to check
     * @return {@code true} if the component is a template
     */
    static boolean isTemplate(Component component) {
        return component instanceof LitTemplate
                || ComponentUtils.isPolymerTemplate(component);
    }
}
