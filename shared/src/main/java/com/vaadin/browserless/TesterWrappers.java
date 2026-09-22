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
package com.vaadin.browserless;

import java.math.BigDecimal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.TextTester;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionTester;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupTester;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonTester;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardTester;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupTester;
import com.vaadin.flow.component.checkbox.CheckboxTester;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.checkbox.SwitchTester;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxTester;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxTester;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.confirmdialog.ConfirmDialogTester;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.ContextMenuTester;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerTester;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePickerTester;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsTester;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogTester;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenuTester;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTester;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.DescriptionListTester;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.DivTester;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.EmphasisTester;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H1Tester;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H2Tester;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H3Tester;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H4Tester;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.H5Tester;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.H6Tester;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.HrTester;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ImageTester;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.InputTester;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.ListItemTester;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeButtonTester;
import com.vaadin.flow.component.html.NativeDetails;
import com.vaadin.flow.component.html.NativeDetailsTester;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.NativeLabelTester;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.OrderedListTester;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.ParagraphTester;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.PreTester;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.component.html.RangeInputTester;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.SpanTester;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.UnorderedListTester;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.ListBoxTester;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBoxTester;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginFormTester;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.login.LoginOverlayTester;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.markdown.MarkdownTester;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayoutTester;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInputTester;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListTester;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationTester;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverTester;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioButtonGroupTester;
import com.vaadin.flow.component.routerlink.RouterLinkTester;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectTester;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavTester;
import com.vaadin.flow.component.slider.DecimalRangeSlider;
import com.vaadin.flow.component.slider.DecimalRangeSliderTester;
import com.vaadin.flow.component.slider.DecimalSlider;
import com.vaadin.flow.component.slider.DecimalSliderTester;
import com.vaadin.flow.component.slider.IntegerRangeSlider;
import com.vaadin.flow.component.slider.IntegerRangeSliderTester;
import com.vaadin.flow.component.slider.IntegerSlider;
import com.vaadin.flow.component.slider.IntegerSliderTester;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutTester;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetTester;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsTester;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.NumberFieldTester;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaTester;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldTester;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerTester;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.treegrid.TreeGridTester;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadTester;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.component.virtuallist.VirtualListTester;
import com.vaadin.flow.router.RouterLink;

/**
 * Wrapper methods that hand out a tester for a component instance, mixed into
 * the test base classes so a test can write {@code test(component)}.
 *
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public interface TesterWrappers {

    /**
     * Wraps the given component in a tester.
     *
     * @param accordion
     *            the component to wrap
     * @return a tester for the given component
     */
    default AccordionTester<Accordion> test(Accordion accordion) {
        return BaseBrowserlessTest.internalWrap(AccordionTester.class,
                accordion);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param avatarGroup
     *            the component to wrap
     * @return a tester for the given component
     * @since 25.3
     */
    default AvatarGroupTester<AvatarGroup> test(AvatarGroup avatarGroup) {
        return BaseBrowserlessTest.internalWrap(AvatarGroupTester.class,
                avatarGroup);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param breadcrumbs
     *            the component to wrap
     * @return a tester for the given component
     */
    default BreadcrumbsTester<Breadcrumbs> test(Breadcrumbs breadcrumbs) {
        return BaseBrowserlessTest.internalWrap(BreadcrumbsTester.class,
                breadcrumbs);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param button
     *            the component to wrap
     * @return a tester for the given component
     */
    default ButtonTester<Button> test(Button button) {
        return BaseBrowserlessTest.internalWrap(ButtonTester.class, button);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param card
     *            the component to wrap
     * @return a tester for the given component
     * @since 25.3
     */
    default CardTester<Card> test(Card card) {
        return BaseBrowserlessTest.internalWrap(CardTester.class, card);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param checkbox
     *            the component to wrap
     * @return a tester for the given component
     */
    default CheckboxTester<Checkbox> test(Checkbox checkbox) {
        return BaseBrowserlessTest.internalWrap(CheckboxTester.class, checkbox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param checkboxGroup
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> CheckboxGroupTester<CheckboxGroup<V>, V> test(
            CheckboxGroup<V> checkboxGroup) {
        return BaseBrowserlessTest.internalWrap(CheckboxGroupTester.class,
                checkboxGroup);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param checkboxGroup
     *            the component to wrap
     * @param valueType
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> CheckboxGroupTester<CheckboxGroup<V>, V> test(
            CheckboxGroup checkboxGroup, Class<V> valueType) {
        return BaseBrowserlessTest.internalWrap(CheckboxGroupTester.class,
                checkboxGroup);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param field
     *            the component to wrap
     * @return a tester for the given component
     */
    default SwitchTester<Switch> test(Switch field) {
        return BaseBrowserlessTest.internalWrap(SwitchTester.class, field);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param comboBox
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> ComboBoxTester<ComboBox<V>, V> test(ComboBox<V> comboBox) {
        return BaseBrowserlessTest.internalWrap(ComboBoxTester.class, comboBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param comboBox
     *            the component to wrap
     * @param valueType
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> ComboBoxTester<ComboBox<V>, V> test(ComboBox comboBox,
            Class<V> valueType) {
        return BaseBrowserlessTest.internalWrap(ComboBoxTester.class, comboBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param comboBox
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> MultiSelectComboBoxTester<MultiSelectComboBox<V>, V> test(
            MultiSelectComboBox<V> comboBox) {
        return BaseBrowserlessTest.internalWrap(MultiSelectComboBoxTester.class,
                comboBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param comboBox
     *            the component to wrap
     * @param valueType
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> MultiSelectComboBoxTester<MultiSelectComboBox<V>, V> test(
            MultiSelectComboBox comboBox, Class<V> valueType) {
        return BaseBrowserlessTest.internalWrap(MultiSelectComboBoxTester.class,
                comboBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param confirmDialog
     *            the component to wrap
     * @return a tester for the given component
     */
    default ConfirmDialogTester test(ConfirmDialog confirmDialog) {
        return BaseBrowserlessTest.internalWrap(ConfirmDialogTester.class,
                confirmDialog);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param contextMenu
     *            the component to wrap
     * @return a tester for the given component
     */
    default ContextMenuTester<ContextMenu> test(ContextMenu contextMenu) {
        return BaseBrowserlessTest.internalWrap(ContextMenuTester.class,
                contextMenu);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param datePicker
     *            the component to wrap
     * @return a tester for the given component
     */
    default DatePickerTester<DatePicker> test(DatePicker datePicker) {
        return BaseBrowserlessTest.internalWrap(DatePickerTester.class,
                datePicker);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param dateTimePicker
     *            the component to wrap
     * @return a tester for the given component
     */
    default DateTimePickerTester<DateTimePicker> test(
            DateTimePicker dateTimePicker) {
        return BaseBrowserlessTest.internalWrap(DateTimePickerTester.class,
                dateTimePicker);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param details
     *            the component to wrap
     * @return a tester for the given component
     */
    default DetailsTester<Details> test(Details details) {
        return BaseBrowserlessTest.internalWrap(DetailsTester.class, details);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param dialog
     *            the component to wrap
     * @return a tester for the given component
     */
    default DialogTester test(Dialog dialog) {
        return BaseBrowserlessTest.internalWrap(DialogTester.class, dialog);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param grid
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> GridTester<Grid<V>, V> test(Grid<V> grid) {
        return BaseBrowserlessTest.internalWrap(GridTester.class, grid);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param grid
     *            the component to wrap
     * @param itemType
     *            the type of the items in the component
     * @return a tester for the given component
     */
    default <V> GridTester<Grid<V>, V> test(Grid grid, Class<V> itemType) {
        return BaseBrowserlessTest.internalWrap(GridTester.class, grid);
    }

    /**
     * Create a tester for the given GridContextMenu instance.
     *
     * @param gridContextMenu
     *            the GridContextMenu instance to be tested
     * @param <V>
     *            the type of the items in the grid the menu is attached to
     * @return a GridContextMenuTester instance wrapping the given
     *         GridContextMenu
     * @since 25.3
     */
    default <V> GridContextMenuTester<GridContextMenu<V>, V> test(
            GridContextMenu<V> gridContextMenu) {
        return BaseBrowserlessTest.internalWrap(GridContextMenuTester.class,
                gridContextMenu);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param listBox
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> ListBoxTester<ListBox<V>, V> test(ListBox<V> listBox) {
        return BaseBrowserlessTest.internalWrap(ListBoxTester.class, listBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param multiSelectListBox
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> MultiSelectListBoxTester<MultiSelectListBox<V>, V> test(
            MultiSelectListBox<V> multiSelectListBox) {
        return BaseBrowserlessTest.internalWrap(MultiSelectListBoxTester.class,
                multiSelectListBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param multiSelectListBox
     *            the component to wrap
     * @param valueType
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> MultiSelectListBoxTester<MultiSelectListBox<V>, V> test(
            MultiSelectListBox multiSelectListBox, Class<V> valueType) {
        return BaseBrowserlessTest.internalWrap(MultiSelectListBoxTester.class,
                multiSelectListBox);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param loginForm
     *            the component to wrap
     * @return a tester for the given component
     */
    default LoginFormTester<LoginForm> test(LoginForm loginForm) {
        return BaseBrowserlessTest.internalWrap(LoginFormTester.class,
                loginForm);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param loginOverlay
     *            the component to wrap
     * @return a tester for the given component
     */
    default LoginOverlayTester<LoginOverlay> test(LoginOverlay loginOverlay) {
        return BaseBrowserlessTest.internalWrap(LoginOverlayTester.class,
                loginOverlay);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param markdown
     *            the component to wrap
     * @return a tester for the given component
     */
    default MarkdownTester<Markdown> test(Markdown markdown) {
        return BaseBrowserlessTest.internalWrap(MarkdownTester.class, markdown);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param masterDetailLayout
     *            the component to wrap
     * @return a tester for the given component
     */
    default MasterDetailLayoutTester<MasterDetailLayout> test(
            MasterDetailLayout masterDetailLayout) {
        return BaseBrowserlessTest.internalWrap(MasterDetailLayoutTester.class,
                masterDetailLayout);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param messageInput
     *            the component to wrap
     * @return a tester for the given component
     */
    default MessageInputTester<MessageInput> test(MessageInput messageInput) {
        return BaseBrowserlessTest.internalWrap(MessageInputTester.class,
                messageInput);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param messageList
     *            the component to wrap
     * @return a tester for the given component
     */
    default MessageListTester<MessageList> test(MessageList messageList) {
        return BaseBrowserlessTest.internalWrap(MessageListTester.class,
                messageList);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param popover
     *            the component to wrap
     * @return a tester for the given component
     */
    default PopoverTester test(Popover popover) {
        return BaseBrowserlessTest.internalWrap(PopoverTester.class, popover);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param target
     *            the component to wrap
     * @return a tester for the given component
     */
    default PopoverTester popoverFor(Component target) {
        return PopoverTester.forTarget(target);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param query
     *            the component to wrap
     * @return a tester for the given component
     */
    default PopoverTester popoverFor(
            ComponentQuery<? extends Component> query) {
        return PopoverTester.forTarget(query);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param notification
     *            the component to wrap
     * @return a tester for the given component
     */
    default NotificationTester<Notification> test(Notification notification) {
        return BaseBrowserlessTest.internalWrap(NotificationTester.class,
                notification);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param radioButtonGroup
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> RadioButtonGroupTester<RadioButtonGroup<V>, V> test(
            RadioButtonGroup<V> radioButtonGroup) {
        return BaseBrowserlessTest.internalWrap(RadioButtonGroupTester.class,
                radioButtonGroup);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param radioButtonGroup
     *            the component to wrap
     * @param valueType
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> RadioButtonGroupTester<RadioButtonGroup<V>, V> test(
            RadioButtonGroup radioButtonGroup, Class<V> valueType) {
        return BaseBrowserlessTest.internalWrap(RadioButtonGroupTester.class,
                radioButtonGroup);
    }

    // RadioButton is package protected so no autowrap.

    /**
     * Wraps the given component in a tester.
     *
     * @param routerLink
     *            the component to wrap
     * @return a tester for the given component
     */
    default RouterLinkTester<RouterLink> test(RouterLink routerLink) {
        return BaseBrowserlessTest.internalWrap(RouterLinkTester.class,
                routerLink);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param select
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> SelectTester<Select<V>, V> test(Select<V> select) {
        return BaseBrowserlessTest.internalWrap(SelectTester.class, select);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param select
     *            the component to wrap
     * @param valueType
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> SelectTester<Select<V>, V> test(Select select,
            Class<V> valueType) {
        return BaseBrowserlessTest.internalWrap(SelectTester.class, select);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param sideNav
     *            the component to wrap
     * @return a tester for the given component
     */
    default SideNavTester<SideNav> test(SideNav sideNav) {
        return BaseBrowserlessTest.internalWrap(SideNavTester.class, sideNav);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param rangeSlider
     *            the component to wrap
     * @return a tester for the given component
     * @since 1.1
     */
    default DecimalRangeSliderTester<DecimalRangeSlider> test(
            DecimalRangeSlider rangeSlider) {
        return BaseBrowserlessTest.internalWrap(DecimalRangeSliderTester.class,
                rangeSlider);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param integerRangeSlider
     *            the component to wrap
     * @return a tester for the given component
     * @since 1.1
     */
    default IntegerRangeSliderTester<IntegerRangeSlider> test(
            IntegerRangeSlider integerRangeSlider) {
        return BaseBrowserlessTest.internalWrap(IntegerRangeSliderTester.class,
                integerRangeSlider);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param slider
     *            the component to wrap
     * @return a tester for the given component
     * @since 1.1
     */
    default DecimalSliderTester<DecimalSlider> test(DecimalSlider slider) {
        return BaseBrowserlessTest.internalWrap(DecimalSliderTester.class,
                slider);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param integerSlider
     *            the component to wrap
     * @return a tester for the given component
     * @since 1.1
     */
    default IntegerSliderTester<IntegerSlider> test(
            IntegerSlider integerSlider) {
        return BaseBrowserlessTest.internalWrap(IntegerSliderTester.class,
                integerSlider);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param splitLayout
     *            the component to wrap
     * @return a tester for the given component
     * @since 25.3
     */
    default SplitLayoutTester<SplitLayout> test(SplitLayout splitLayout) {
        return BaseBrowserlessTest.internalWrap(SplitLayoutTester.class,
                splitLayout);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param tabs
     *            the component to wrap
     * @return a tester for the given component
     */
    default TabsTester<Tabs> test(Tabs tabs) {
        return BaseBrowserlessTest.internalWrap(TabsTester.class, tabs);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param tabSheet
     *            the component to wrap
     * @return a tester for the given component
     */
    default TabSheetTester<TabSheet> test(TabSheet tabSheet) {
        return BaseBrowserlessTest.internalWrap(TabSheetTester.class, tabSheet);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param text
     *            the component to wrap
     * @return a tester for the given component
     */
    default TextTester<Text> test(Text text) {
        return BaseBrowserlessTest.internalWrap(TextTester.class, text);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param integerField
     *            the component to wrap
     * @return a tester for the given component
     */
    default NumberFieldTester<IntegerField, Integer> test(
            IntegerField integerField) {
        return BaseBrowserlessTest.internalWrap(NumberFieldTester.class,
                integerField);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param numberField
     *            the component to wrap
     * @return a tester for the given component
     */
    default NumberFieldTester<NumberField, Double> test(
            NumberField numberField) {
        return BaseBrowserlessTest.internalWrap(NumberFieldTester.class,
                numberField);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param textArea
     *            the component to wrap
     * @return a tester for the given component
     */
    default TextAreaTester<TextArea> test(TextArea textArea) {
        return BaseBrowserlessTest.internalWrap(TextAreaTester.class, textArea);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param textField
     *            the component to wrap
     * @return a tester for the given component
     */
    default TextFieldTester<TextField, String> test(TextField textField) {
        return BaseBrowserlessTest.internalWrap(TextFieldTester.class,
                textField);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param passwordField
     *            the component to wrap
     * @return a tester for the given component
     */
    default TextFieldTester<PasswordField, String> test(
            PasswordField passwordField) {
        return BaseBrowserlessTest.internalWrap(TextFieldTester.class,
                passwordField);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param emailField
     *            the component to wrap
     * @return a tester for the given component
     */
    default TextFieldTester<EmailField, String> test(EmailField emailField) {
        return BaseBrowserlessTest.internalWrap(TextFieldTester.class,
                emailField);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param bigDecimalField
     *            the component to wrap
     * @return a tester for the given component
     */
    default TextFieldTester<BigDecimalField, BigDecimal> test(
            BigDecimalField bigDecimalField) {
        return BaseBrowserlessTest.internalWrap(TextFieldTester.class,
                bigDecimalField);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param timePicker
     *            the component to wrap
     * @return a tester for the given component
     */
    default TimePickerTester<TimePicker> test(TimePicker timePicker) {
        return BaseBrowserlessTest.internalWrap(TimePickerTester.class,
                timePicker);
    }

    /**
     * Create a tester for the given TreeGrid instance.
     * <p>
     * This overload is more specific than {@link #test(Grid)}, so a
     * {@code TreeGrid} argument now yields a {@link TreeGridTester} rather than
     * a {@link GridTester}. Code that assigned the result to an explicitly
     * typed {@code GridTester<Grid<V>, V>} no longer compiles and has to widen
     * the declaration, use {@code var}, or chain the call directly.
     *
     * @param <V>
     *            the type of the items in the TreeGrid
     * @param treeGrid
     *            the TreeGrid instance to be tested
     * @return a TreeGridTester instance wrapping the given TreeGrid
     * @since 25.3
     */
    default <V> TreeGridTester<TreeGrid<V>, V> test(TreeGrid<V> treeGrid) {
        return BaseBrowserlessTest.internalWrap(TreeGridTester.class, treeGrid);
    }

    /**
     * Create a tester for the given TreeGrid instance.
     * <p>
     * This overload is more specific than {@link #test(Grid, Class)}, so a
     * {@code TreeGrid} argument now yields a {@link TreeGridTester} rather than
     * a {@link GridTester}. Code that assigned the result to an explicitly
     * typed {@code GridTester<Grid<V>, V>} no longer compiles and has to widen
     * the declaration, use {@code var}, or chain the call directly.
     *
     * @param <V>
     *            the type of the items in the TreeGrid
     * @param treeGrid
     *            the TreeGrid instance to be tested
     * @param itemType
     *            the type of the items in the TreeGrid
     * @return a TreeGridTester instance wrapping the given TreeGrid
     * @since 25.3
     */
    default <V> TreeGridTester<TreeGrid<V>, V> test(TreeGrid treeGrid,
            Class<V> itemType) {
        return BaseBrowserlessTest.internalWrap(TreeGridTester.class, treeGrid);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param upload
     *            the component to wrap
     * @return a tester for the given component
     */
    default UploadTester<Upload> test(Upload upload) {
        return BaseBrowserlessTest.internalWrap(UploadTester.class, upload);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param virtualList
     *            the component to wrap
     * @return a tester for the given component
     */
    default <V> VirtualListTester<VirtualList<V>, V> test(
            VirtualList<V> virtualList) {
        return BaseBrowserlessTest.internalWrap(VirtualListTester.class,
                virtualList);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param <V>
     *            the type of the items in the component
     * @param virtualList
     *            the component to wrap
     * @param itemType
     *            the type of the items in the component
     * @return a tester for the given component
     */
    default <V> VirtualListTester<VirtualList<V>, V> test(
            VirtualList virtualList, Class<V> itemType) {
        return BaseBrowserlessTest.internalWrap(VirtualListTester.class,
                virtualList);
    }

    /* HTML components */

    /**
     * Wraps the given component in a tester.
     *
     * @param anchor
     *            the component to wrap
     * @return a tester for the given component
     */
    default AnchorTester test(Anchor anchor) {
        return BaseBrowserlessTest.internalWrap(AnchorTester.class, anchor);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param descriptionList
     *            the component to wrap
     * @return a tester for the given component
     */
    default DescriptionListTester test(DescriptionList descriptionList) {
        return BaseBrowserlessTest.internalWrap(DescriptionListTester.class,
                descriptionList);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param div
     *            the component to wrap
     * @return a tester for the given component
     */
    default DivTester test(Div div) {
        return BaseBrowserlessTest.internalWrap(DivTester.class, div);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param emphasis
     *            the component to wrap
     * @return a tester for the given component
     */
    default EmphasisTester test(Emphasis emphasis) {
        return BaseBrowserlessTest.internalWrap(EmphasisTester.class, emphasis);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param h1
     *            the component to wrap
     * @return a tester for the given component
     */
    default H1Tester test(H1 h1) {
        return BaseBrowserlessTest.internalWrap(H1Tester.class, h1);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param h2
     *            the component to wrap
     * @return a tester for the given component
     */
    default H2Tester test(H2 h2) {
        return BaseBrowserlessTest.internalWrap(H2Tester.class, h2);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param h3
     *            the component to wrap
     * @return a tester for the given component
     */
    default H3Tester test(H3 h3) {
        return BaseBrowserlessTest.internalWrap(H3Tester.class, h3);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param h4
     *            the component to wrap
     * @return a tester for the given component
     */
    default H4Tester test(H4 h4) {
        return BaseBrowserlessTest.internalWrap(H4Tester.class, h4);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param h5
     *            the component to wrap
     * @return a tester for the given component
     */
    default H5Tester test(H5 h5) {
        return BaseBrowserlessTest.internalWrap(H5Tester.class, h5);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param h6
     *            the component to wrap
     * @return a tester for the given component
     */
    default H6Tester test(H6 h6) {
        return BaseBrowserlessTest.internalWrap(H6Tester.class, h6);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param hr
     *            the component to wrap
     * @return a tester for the given component
     */
    default HrTester test(Hr hr) {
        return BaseBrowserlessTest.internalWrap(HrTester.class, hr);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param image
     *            the component to wrap
     * @return a tester for the given component
     */
    default ImageTester test(Image image) {
        return BaseBrowserlessTest.internalWrap(ImageTester.class, image);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param input
     *            the component to wrap
     * @return a tester for the given component
     */
    default InputTester test(Input input) {
        return BaseBrowserlessTest.internalWrap(InputTester.class, input);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param input
     *            the component to wrap
     * @return a tester for the given component
     */
    default RangeInputTester test(RangeInput input) {
        return BaseBrowserlessTest.internalWrap(RangeInputTester.class, input);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param label
     *            the component to wrap
     * @return a tester for the given component
     */
    default NativeLabelTester test(NativeLabel label) {
        return BaseBrowserlessTest.internalWrap(NativeLabelTester.class, label);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param listItem
     *            the component to wrap
     * @return a tester for the given component
     */
    default ListItemTester test(ListItem listItem) {
        return BaseBrowserlessTest.internalWrap(ListItemTester.class, listItem);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param nativeButton
     *            the component to wrap
     * @return a tester for the given component
     */
    default NativeButtonTester test(NativeButton nativeButton) {
        return BaseBrowserlessTest.internalWrap(NativeButtonTester.class,
                nativeButton);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param nativeDetails
     *            the component to wrap
     * @return a tester for the given component
     */
    default NativeDetailsTester test(NativeDetails nativeDetails) {
        return BaseBrowserlessTest.internalWrap(NativeDetailsTester.class,
                nativeDetails);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param orderedList
     *            the component to wrap
     * @return a tester for the given component
     */
    default OrderedListTester test(OrderedList orderedList) {
        return BaseBrowserlessTest.internalWrap(OrderedListTester.class,
                orderedList);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param paragraph
     *            the component to wrap
     * @return a tester for the given component
     */
    default ParagraphTester test(Paragraph paragraph) {
        return BaseBrowserlessTest.internalWrap(ParagraphTester.class,
                paragraph);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param pre
     *            the component to wrap
     * @return a tester for the given component
     */
    default PreTester test(Pre pre) {
        return BaseBrowserlessTest.internalWrap(PreTester.class, pre);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param span
     *            the component to wrap
     * @return a tester for the given component
     */
    default SpanTester test(Span span) {
        return BaseBrowserlessTest.internalWrap(SpanTester.class, span);
    }

    /**
     * Wraps the given component in a tester.
     *
     * @param unorderedList
     *            the component to wrap
     * @return a tester for the given component
     */
    default UnorderedListTester test(UnorderedList unorderedList) {
        return BaseBrowserlessTest.internalWrap(UnorderedListTester.class,
                unorderedList);
    }
}
