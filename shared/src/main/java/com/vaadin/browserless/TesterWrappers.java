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
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadButtonTester;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadDropZoneTester;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.component.upload.UploadFileListTester;
import com.vaadin.flow.component.upload.UploadTester;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.component.virtuallist.VirtualListTester;
import com.vaadin.flow.router.RouterLink;

/**
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public interface TesterWrappers {

    /**
     * Hook for context-bound implementations to install Vaadin thread-locals
     * before a component is wrapped in a tester. Called by every typed
     * {@code test(...)} overload of this interface. Default is a no-op.
     */
    default void activateTesterContext() {
    }

    default AccordionTester<Accordion> test(Accordion accordion) {
        return wrapInTester(AccordionTester.class, accordion);
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
        return wrapInTester(AvatarGroupTester.class, avatarGroup);
    }

    default BreadcrumbsTester<Breadcrumbs> test(Breadcrumbs breadcrumbs) {
        return wrapInTester(BreadcrumbsTester.class, breadcrumbs);
    }

    default ButtonTester<Button> test(Button button) {
        return wrapInTester(ButtonTester.class, button);
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
        return wrapInTester(CardTester.class, card);
    }

    default CheckboxTester<Checkbox> test(Checkbox checkbox) {
        return wrapInTester(CheckboxTester.class, checkbox);
    }

    default <V> CheckboxGroupTester<CheckboxGroup<V>, V> test(
            CheckboxGroup<V> checkboxGroup) {
        return wrapInTester(CheckboxGroupTester.class, checkboxGroup);
    }

    default <V> CheckboxGroupTester<CheckboxGroup<V>, V> test(
            CheckboxGroup checkboxGroup, Class<V> valueType) {
        return wrapInTester(CheckboxGroupTester.class, checkboxGroup);
    }

    default SwitchTester<Switch> test(Switch field) {
        return wrapInTester(SwitchTester.class, field);
    }

    default <V> ComboBoxTester<ComboBox<V>, V> test(ComboBox<V> comboBox) {
        return wrapInTester(ComboBoxTester.class, comboBox);
    }

    default <V> ComboBoxTester<ComboBox<V>, V> test(ComboBox comboBox,
            Class<V> valueType) {
        return wrapInTester(ComboBoxTester.class, comboBox);
    }

    default <V> MultiSelectComboBoxTester<MultiSelectComboBox<V>, V> test(
            MultiSelectComboBox<V> comboBox) {
        return wrapInTester(MultiSelectComboBoxTester.class, comboBox);
    }

    default <V> MultiSelectComboBoxTester<MultiSelectComboBox<V>, V> test(
            MultiSelectComboBox comboBox, Class<V> valueType) {
        return wrapInTester(MultiSelectComboBoxTester.class, comboBox);
    }

    default ConfirmDialogTester test(ConfirmDialog confirmDialog) {
        return wrapInTester(ConfirmDialogTester.class, confirmDialog);
    }

    default ContextMenuTester<ContextMenu> test(ContextMenu contextMenu) {
        return wrapInTester(ContextMenuTester.class, contextMenu);
    }

    default DatePickerTester<DatePicker> test(DatePicker datePicker) {
        return wrapInTester(DatePickerTester.class, datePicker);
    }

    default DateTimePickerTester<DateTimePicker> test(
            DateTimePicker dateTimePicker) {
        return wrapInTester(DateTimePickerTester.class, dateTimePicker);
    }

    default DetailsTester<Details> test(Details details) {
        return wrapInTester(DetailsTester.class, details);
    }

    default DialogTester test(Dialog dialog) {
        return wrapInTester(DialogTester.class, dialog);
    }

    default <V> GridTester<Grid<V>, V> test(Grid<V> grid) {
        return wrapInTester(GridTester.class, grid);
    }

    default <V> GridTester<Grid<V>, V> test(Grid grid, Class<V> itemType) {
        return wrapInTester(GridTester.class, grid);
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
        return wrapInTester(GridContextMenuTester.class, gridContextMenu);
    }

    default <V> ListBoxTester<ListBox<V>, V> test(ListBox<V> listBox) {
        return wrapInTester(ListBoxTester.class, listBox);
    }

    default <V> MultiSelectListBoxTester<MultiSelectListBox<V>, V> test(
            MultiSelectListBox<V> multiSelectListBox) {
        return wrapInTester(MultiSelectListBoxTester.class, multiSelectListBox);
    }

    default <V> MultiSelectListBoxTester<MultiSelectListBox<V>, V> test(
            MultiSelectListBox multiSelectListBox, Class<V> valueType) {
        return wrapInTester(MultiSelectListBoxTester.class, multiSelectListBox);
    }

    default LoginFormTester<LoginForm> test(LoginForm loginForm) {
        return wrapInTester(LoginFormTester.class, loginForm);
    }

    default LoginOverlayTester<LoginOverlay> test(LoginOverlay loginOverlay) {
        return wrapInTester(LoginOverlayTester.class, loginOverlay);
    }

    default MarkdownTester<Markdown> test(Markdown markdown) {
        return wrapInTester(MarkdownTester.class, markdown);
    }

    default MasterDetailLayoutTester<MasterDetailLayout> test(
            MasterDetailLayout masterDetailLayout) {
        return wrapInTester(MasterDetailLayoutTester.class, masterDetailLayout);
    }

    default MessageInputTester<MessageInput> test(MessageInput messageInput) {
        return wrapInTester(MessageInputTester.class, messageInput);
    }

    default MessageListTester<MessageList> test(MessageList messageList) {
        return wrapInTester(MessageListTester.class, messageList);
    }

    default PopoverTester test(Popover popover) {
        return wrapInTester(PopoverTester.class, popover);
    }

    default PopoverTester popoverFor(Component target) {
        return PopoverTester.forTarget(target);
    }

    default PopoverTester popoverFor(
            ComponentQuery<? extends Component> query) {
        return PopoverTester.forTarget(query);
    }

    default NotificationTester<Notification> test(Notification notification) {
        return wrapInTester(NotificationTester.class, notification);
    }

    default <V> RadioButtonGroupTester<RadioButtonGroup<V>, V> test(
            RadioButtonGroup<V> radioButtonGroup) {
        return wrapInTester(RadioButtonGroupTester.class, radioButtonGroup);
    }

    default <V> RadioButtonGroupTester<RadioButtonGroup<V>, V> test(
            RadioButtonGroup radioButtonGroup, Class<V> valueType) {
        return wrapInTester(RadioButtonGroupTester.class, radioButtonGroup);
    }

    // RadioButton is package protected so no autowrap.

    default RouterLinkTester<RouterLink> test(RouterLink routerLink) {
        return wrapInTester(RouterLinkTester.class, routerLink);
    }

    default <V> SelectTester<Select<V>, V> test(Select<V> select) {
        return wrapInTester(SelectTester.class, select);
    }

    default <V> SelectTester<Select<V>, V> test(Select select,
            Class<V> valueType) {
        return wrapInTester(SelectTester.class, select);
    }

    default SideNavTester<SideNav> test(SideNav sideNav) {
        return wrapInTester(SideNavTester.class, sideNav);
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
        return wrapInTester(DecimalRangeSliderTester.class, rangeSlider);
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
        return wrapInTester(IntegerRangeSliderTester.class, integerRangeSlider);
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
        return wrapInTester(DecimalSliderTester.class, slider);
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
        return wrapInTester(IntegerSliderTester.class, integerSlider);
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
        return wrapInTester(SplitLayoutTester.class, splitLayout);
    }

    default TabsTester<Tabs> test(Tabs tabs) {
        return wrapInTester(TabsTester.class, tabs);
    }

    default TabSheetTester<TabSheet> test(TabSheet tabSheet) {
        return wrapInTester(TabSheetTester.class, tabSheet);
    }

    default TextTester<Text> test(Text text) {
        return wrapInTester(TextTester.class, text);
    }

    default NumberFieldTester<IntegerField, Integer> test(
            IntegerField integerField) {
        return wrapInTester(NumberFieldTester.class, integerField);
    }

    default NumberFieldTester<NumberField, Double> test(
            NumberField numberField) {
        return wrapInTester(NumberFieldTester.class, numberField);
    }

    default TextAreaTester<TextArea> test(TextArea textArea) {
        return wrapInTester(TextAreaTester.class, textArea);
    }

    default TextFieldTester<TextField, String> test(TextField textField) {
        return wrapInTester(TextFieldTester.class, textField);
    }

    default TextFieldTester<PasswordField, String> test(
            PasswordField passwordField) {
        return wrapInTester(TextFieldTester.class, passwordField);
    }

    default TextFieldTester<EmailField, String> test(EmailField emailField) {
        return wrapInTester(TextFieldTester.class, emailField);
    }

    default TextFieldTester<BigDecimalField, BigDecimal> test(
            BigDecimalField bigDecimalField) {
        return wrapInTester(TextFieldTester.class, bigDecimalField);
    }

    default TimePickerTester<TimePicker> test(TimePicker timePicker) {
        return wrapInTester(TimePickerTester.class, timePicker);
    }

    /**
     * Create a tester for the given TreeGrid instance.
     * <p/>
     * This overload is more specific than {@link #test(Grid)}, so a
     * {@code TreeGrid} argument now yields a {@link TreeGridTester} rather than
     * a {@link GridTester}. Code that assigned the result to an explicitly
     * typed {@code GridTester<Grid<V>, V>} no longer compiles and has to widen
     * the declaration, use {@code var}, or chain the call directly.
     *
     * @param treeGrid
     *            the TreeGrid instance to be tested
     * @param <V>
     *            the type of the items in the TreeGrid
     * @return a TreeGridTester instance wrapping the given TreeGrid
     * @since 25.3
     */
    default <V> TreeGridTester<TreeGrid<V>, V> test(TreeGrid<V> treeGrid) {
        return wrapInTester(TreeGridTester.class, treeGrid);
    }

    /**
     * Create a tester for the given TreeGrid instance.
     * <p/>
     * This overload is more specific than {@link #test(Grid, Class)}, so a
     * {@code TreeGrid} argument now yields a {@link TreeGridTester} rather than
     * a {@link GridTester}. Code that assigned the result to an explicitly
     * typed {@code GridTester<Grid<V>, V>} no longer compiles and has to widen
     * the declaration, use {@code var}, or chain the call directly.
     *
     * @param treeGrid
     *            the TreeGrid instance to be tested
     * @param itemType
     *            the type of the items in the TreeGrid
     * @param <V>
     *            the type of the items in the TreeGrid
     * @return a TreeGridTester instance wrapping the given TreeGrid
     * @since 25.3
     */
    default <V> TreeGridTester<TreeGrid<V>, V> test(TreeGrid treeGrid,
            Class<V> itemType) {
        return wrapInTester(TreeGridTester.class, treeGrid);
    }

    default UploadTester<Upload> test(Upload upload) {
        return wrapInTester(UploadTester.class, upload);
    }

    /**
     * Create a tester for the given UploadButton instance.
     * <p/>
     * This overload is more specific than {@link #test(Button)}, so an
     * {@code UploadButton} argument yields an {@link UploadButtonTester} rather
     * than a {@link ButtonTester}. Code that assigned the result to an
     * explicitly typed {@code ButtonTester<Button>} no longer compiles and has
     * to widen the declaration, use {@code var}, or chain the call directly.
     *
     * @param uploadButton
     *            the UploadButton instance to be tested
     * @return an UploadButtonTester instance wrapping the given UploadButton
     */
    default UploadButtonTester<UploadButton> test(UploadButton uploadButton) {
        return wrapInTester(UploadButtonTester.class, uploadButton);
    }

    default UploadDropZoneTester<UploadDropZone> test(
            UploadDropZone uploadDropZone) {
        return wrapInTester(UploadDropZoneTester.class, uploadDropZone);
    }

    default UploadFileListTester<UploadFileList> test(
            UploadFileList uploadFileList) {
        return wrapInTester(UploadFileListTester.class, uploadFileList);
    }

    default <V> VirtualListTester<VirtualList<V>, V> test(
            VirtualList<V> virtualList) {
        return wrapInTester(VirtualListTester.class, virtualList);
    }

    default <V> VirtualListTester<VirtualList<V>, V> test(
            VirtualList virtualList, Class<V> itemType) {
        return wrapInTester(VirtualListTester.class, virtualList);
    }

    /* HTML components */

    default AnchorTester test(Anchor anchor) {
        return wrapInTester(AnchorTester.class, anchor);
    }

    default DescriptionListTester test(DescriptionList descriptionList) {
        return wrapInTester(DescriptionListTester.class, descriptionList);
    }

    default DivTester test(Div div) {
        return wrapInTester(DivTester.class, div);
    }

    default EmphasisTester test(Emphasis emphasis) {
        return wrapInTester(EmphasisTester.class, emphasis);
    }

    default H1Tester test(H1 h1) {
        return wrapInTester(H1Tester.class, h1);
    }

    default H2Tester test(H2 h2) {
        return wrapInTester(H2Tester.class, h2);
    }

    default H3Tester test(H3 h3) {
        return wrapInTester(H3Tester.class, h3);
    }

    default H4Tester test(H4 h4) {
        return wrapInTester(H4Tester.class, h4);
    }

    default H5Tester test(H5 h5) {
        return wrapInTester(H5Tester.class, h5);
    }

    default H6Tester test(H6 h6) {
        return wrapInTester(H6Tester.class, h6);
    }

    default HrTester test(Hr hr) {
        return wrapInTester(HrTester.class, hr);
    }

    default ImageTester test(Image image) {
        return wrapInTester(ImageTester.class, image);
    }

    default InputTester test(Input input) {
        return wrapInTester(InputTester.class, input);
    }

    default RangeInputTester test(RangeInput input) {
        return wrapInTester(RangeInputTester.class, input);
    }

    default NativeLabelTester test(NativeLabel label) {
        return wrapInTester(NativeLabelTester.class, label);
    }

    default ListItemTester test(ListItem listItem) {
        return wrapInTester(ListItemTester.class, listItem);
    }

    default NativeButtonTester test(NativeButton nativeButton) {
        return wrapInTester(NativeButtonTester.class, nativeButton);
    }

    default NativeDetailsTester test(NativeDetails nativeDetails) {
        return wrapInTester(NativeDetailsTester.class, nativeDetails);
    }

    default OrderedListTester test(OrderedList orderedList) {
        return wrapInTester(OrderedListTester.class, orderedList);
    }

    default ParagraphTester test(Paragraph paragraph) {
        return wrapInTester(ParagraphTester.class, paragraph);
    }

    default PreTester test(Pre pre) {
        return wrapInTester(PreTester.class, pre);
    }

    default SpanTester test(Span span) {
        return wrapInTester(SpanTester.class, span);
    }

    default UnorderedListTester test(UnorderedList unorderedList) {
        return wrapInTester(UnorderedListTester.class, unorderedList);
    }

    private <T extends ComponentTester<Y>, Y extends Component> T wrapInTester(
            Class<T> tester, Y component) {
        activateTesterContext();
        return BaseBrowserlessTest.internalWrap(tester, component);
    }
}
