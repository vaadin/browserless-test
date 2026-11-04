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
package com.vaadin.browserless.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.browserless.internal.ComponentSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenuBase;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.listbox.ListBoxBase;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.data.provider.DataProvider;

/**
 * The {@link ComponentSupport} for the Vaadin components: everything the mocked
 * environment needs to know about Grid, the menus, the dialogs and the other
 * components whose structure Flow alone doesn't describe.
 * <p>
 * This is internal API and may change without notice.
 */
public class FlowComponentsSupport implements ComponentSupport {

    @Override
    public List<Component> getAllChildren(Component component) {
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
        return null;
    }

    @Override
    public boolean isClosedDialog(Component component) {
        if (component instanceof Dialog dialog && !dialog.isOpened()) {
            return true;
        }
        return component instanceof ConfirmDialog dialog && !dialog.isOpened();
    }

    @Override
    public Collection<Class<? extends Component>> getDialogTypes() {
        return List.of(Dialog.class);
    }

    @Override
    public boolean firesClosedEvent(Component component) {
        return component instanceof Dialog || component instanceof ConfirmDialog
                || component instanceof LoginOverlay
                || component instanceof ContextMenuBase<?, ?, ?>
                || component instanceof Notification;
    }

    @Override
    public void describe(Component component, List<String> description) {
        if (component instanceof Button button
                && button.getIcon() instanceof Icon icon) {
            description.add(
                    "icon='" + icon.getElement().getAttribute("icon") + "'");
        }
        if (component instanceof Grid<?> grid && grid.getBeanType() != null) {
            description.add("<" + grid.getBeanType().getSimpleName() + ">");
        }
    }

    @Override
    public DataProvider<?, ?> getDataProvider(Component component) {
        // until https://github.com/vaadin/flow/issues/6296 is resolved
        if (component instanceof HasDataProvider<?>) {
            return GridKt.getDataProvider(component);
        } else if (component instanceof Grid<?> grid) {
            return grid.getDataProvider();
        } else if (component instanceof Select<?> select) {
            return select.getDataProvider();
        } else if (component instanceof ListBoxBase<?, ?, ?> listBox) {
            return listBox.getDataProvider();
        } else if (component instanceof RadioButtonGroup<?> radioButtonGroup) {
            return radioButtonGroup.getDataProvider();
        } else if (component instanceof CheckboxGroup<?> checkboxGroup) {
            return checkboxGroup.getDataProvider();
        } else if (component instanceof ComboBox<?> comboBox) {
            return comboBox.getDataProvider();
        }
        return null;
    }

    @Override
    public String getLabel(Component component) {
        if (component instanceof FormLayout.FormItem formItem) {
            return getFormItemLabel(formItem);
        } else if (component instanceof SideNav sideNav) {
            return Objects.requireNonNullElse(sideNav.getLabel(), "");
        } else if (component instanceof SideNavItem sideNavItem) {
            return Objects.requireNonNullElse(sideNavItem.getLabel(), "");
        }
        return null;
    }

    @Override
    public boolean setLabel(Component component, String label) {
        if (component instanceof FormLayout.FormItem) {
            throw new IllegalArgumentException(
                    "Setting the caption of FormItem is currently unsupported");
        } else if (component instanceof SideNav sideNav) {
            sideNav.setLabel(label);
            return true;
        } else if (component instanceof SideNavItem sideNavItem) {
            sideNavItem.setLabel(label);
            return true;
        }
        return false;
    }

    @Override
    public String getCaption(Component component) {
        if (component instanceof Button button) {
            return button.getText();
        }
        return null;
    }

    @Override
    public boolean setCaption(Component component, String caption) {
        if (component instanceof Button button) {
            button.setText(caption);
            return true;
        }
        return false;
    }

    /**
     * Concatenates texts from all elements placed in the {@code label} slot,
     * which is whatever was passed as the String label to
     * {@link FormLayout#addFormItem(Component, String)}.
     */
    private static String getFormItemLabel(FormLayout.FormItem formItem) {
        return formItem.getChildren().filter(
                it -> "label".equals(it.getElement().getAttribute("slot")))
                .map(it -> {
                    String text = it instanceof HasText hasText
                            ? hasText.getText()
                            : null;
                    return text != null ? text : "";
                }).collect(Collectors.joining(""));
    }

    private static List<Component> toList(Stream<? extends Component> stream) {
        return stream.collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Component> distinct(
            Stream<? extends Component> stream) {
        return toList(stream.distinct());
    }
}
