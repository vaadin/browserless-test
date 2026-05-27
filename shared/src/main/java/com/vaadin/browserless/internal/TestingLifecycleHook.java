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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.menubar.MenuBar;

/**
 * If you need to hook into the testing lifecycle (e.g. you need to wait for any async operations to finish),
 * provide your own custom implementation of this interface, then set it into [TestingLifecycleHooks#current].
 *
 * ### Mocking server request end
 *
 * Since Browserless Testing runs in the same JVM as the server and there is no browser, the boundaries between the client and
 * the server become unclear. When looking into sources of any test method, it's really hard to tell where exactly the server request ends, and
 * where another request starts.
 *
 * You can establish an explicit client boundary in your test, by explicitly calling [MockVaadin.clientRoundtrip]. However, since that
 * would be both laborous and error-prone, the default operation is that Browserless Testing pretends as if there was a client-server
 * roundtrip before every component lookup
 * via the [_get]/[_find]/[_expectNone]/[_expectOne] call. Therefore, [MockVaadin.clientRoundtrip] is called from [awaitBeforeLookup] by default.
 */
public interface TestingLifecycleHook {

    /**
     * A default lifecycle hook that simply runs default implementations of the hook functions.
     */
    TestingLifecycleHook DEFAULT = new TestingLifecycleHook() {
    };

    /**
     * Invoked before every component lookup. You can e.g. wait for any async operations to finish and for the server to settle down.
     *
     * The default implementation calls the [MockVaadin.clientRoundtrip] method. When implementing this method, you should
     * also call [MockVaadin.clientRoundtrip] (or simply call super).
     */
    default void awaitBeforeLookup() {
        if (UI.getCurrent() != null) {
            MockVaadin.clientRoundtrip();
        }
    }

    /**
     * Invoked after every component lookup. You can e.g. wait for any async operations to finish and for the server to settle down.
     * Invoked even if the `_get()`/`_find()`/`_expectNone()` function fails.
     */
    default void awaitAfterLookup() {
    }

    /**
     * Provides all children of given component. Provides workarounds for certain components:
     * * For [Grid.Column] the function will also return cell components nested in all headers and footers for that particular column.
     * * For [MenuItemBase] the function returns all items of a sub-menu.
     */
    default List<Component> getAllChildren(Component component) {
        if (component instanceof MenuItemBase) {
            // also include component.children: https://github.com/mvysny/karibu-testing/issues/76
            MenuItemBase<?, ?, ?> menuItem = (MenuItemBase<?, ?, ?>) component;
            LinkedHashSet<Component> distinct = new LinkedHashSet<>();
            distinct.addAll(component.getChildren().collect(Collectors.toList()));
            distinct.addAll(menuItem.getSubMenu().getItems());
            return new ArrayList<>(distinct);
        }
        if (component instanceof MenuBar) {
            // don't include virtual children since that would make the MenuItems appear two times.
            return component.getChildren().collect(Collectors.toList());
        }
        if (isTemplate(component)) {
            // don't include virtual children since those will include nested components.
            // however, those components are only they are only "shallow shells" of components constructed
            // server-side - almost none of their properties are transferred to the server-side.
            // Listing those components with null captions and other properties would only be confusing.
            // Therefore, let's leave the virtual children out for now.
            // See https://github.com/mvysny/karibu-testing/tree/master/karibu-testing-v10#polymer-templates--lit-templates
            return component.getChildren().collect(Collectors.toList());
        }
        if ("com.vaadin.flow.component.grid.ColumnGroup".equals(component.getClass().getName())) {
            // don't include virtual children since that would include the header/footer components
            // which would clash with Grid.Column later on
            return component.getChildren().collect(Collectors.toList());
        }
        if (component instanceof Grid.Column) {
            // don't include virtual children since that would include the header/footer components
            // which would clash with Grid.Column later on
            return component.getChildren().collect(Collectors.toList());
        }
        if (component instanceof Composite) {
            // The Composite class overrides getChildren() to return a stream with the wrapped component,
            // but also getElement() returning the Element of the wrapped component.
            // The latter causes the virtual child to be fetched as Composite direct child,
            // thus duplicating any virtual children the child component might have.
            return component.getChildren().collect(Collectors.toList());
        }
        // Also include virtual children.
        // Issue: https://github.com/mvysny/karibu-testing/issues/85
        LinkedHashSet<Component> distinct = new LinkedHashSet<>();
        distinct.addAll(component.getChildren().collect(Collectors.toList()));
        distinct.addAll(BasicUtils._getVirtualChildren(component));
        return new ArrayList<>(distinct);
    }

    static boolean isTemplate(Component component) {
        return component instanceof LitTemplate || ComponentUtils.isPolymerTemplate(component);
    }
}
