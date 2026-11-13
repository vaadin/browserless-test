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

import java.util.ArrayList;
import java.util.List;

import com.example.clipboard.CopyView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.ClipboardContent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.textfield.TextField;

@ViewPackages(classes = CopyView.class)
class SimulatedClipboardTest extends BrowserlessTest {

    @Test
    void click_copiesText() {
        navigate(CopyView.class);
        Button copy = $(Button.class).single();
        Assertions.assertNull(SimulatedClipboard.get().getText());

        test(copy).middleClick();
        Assertions.assertNull(SimulatedClipboard.get().getText(),
                "Only a primary button click fires the click event");

        test(copy).click();
        Assertions.assertEquals("Hello", SimulatedClipboard.get().getText());
        Assertions.assertNull(SimulatedClipboard.get().getHtml());
    }

    @Test
    void copy_replacesWholeContent() {
        Button copyText = new Button("Text");
        Clipboard.onClick(copyText).writeText("Hello");
        Button copyHtml = new Button("Html");
        Clipboard.onClick(copyHtml).writeHtml("<b>Hi</b>");
        add(copyText, copyHtml);

        test(copyText).click();
        test(copyHtml).click();

        Assertions.assertNull(SimulatedClipboard.get().getText());
        Assertions.assertEquals("<b>Hi</b>",
                SimulatedClipboard.get().getHtml());
    }

    @Test
    void copyOutcome_reportedAfterServerClick() {
        List<String> events = new ArrayList<>();
        Button copy = new Button("Copy",
                event -> events.add("click " + event.isFromClient()));
        Clipboard.onClick(copy).writeText("Hello",
                copied -> events.add("copied " + copied),
                error -> events.add("error " + error));
        add(copy);

        test(copy).click();

        Assertions.assertEquals(List.of("click true", "copied Hello"), events);
    }

    @Test
    void reattachedButton_stillCopies() {
        List<String> copies = new ArrayList<>();
        Button copy = new Button("Copy");
        Clipboard.onClick(copy).writeText("Hello", copies::add,
                error -> Assertions.fail(error.toString()));
        Div container = new Div(copy);
        add(container);
        test(copy).click();

        container.remove(copy);
        roundTrip();
        container.add(copy);
        test(copy).click();

        Assertions.assertEquals(List.of("Hello", "Hello"), copies);
    }

    @Test
    void triggerThatIsNotSimulated_isIgnored() {
        TextField source = new TextField();
        source.setValue("Hello");
        List<Boolean> clicks = new ArrayList<>();
        Button copy = new Button("Copy",
                event -> clicks.add(event.isFromClient()));
        Clipboard.onClick(copy).writeText(source);
        add(source, copy);

        test(copy).click();

        Assertions.assertNull(SimulatedClipboard.get().getText());
        Assertions.assertEquals(List.of(true), clicks);
    }

    @Test
    void imageCopy_isIgnoredWithoutWritingText() {
        Image image = new Image("logo.png", "Logo");
        List<Boolean> clicks = new ArrayList<>();
        Button copy = new Button("Copy",
                event -> clicks.add(event.isFromClient()));
        Clipboard.onClick(copy)
                .write(ClipboardContent.create().text("Hello").image(image));
        add(image, copy);

        test(copy).click();

        Assertions.assertNull(SimulatedClipboard.get().getText());
        Assertions.assertEquals(List.of(true), clicks);
    }

    @Test
    void menuItemClick_copiesText() {
        MenuBar menuBar = new MenuBar();
        Clipboard.onClick(menuBar.addItem("Copy link")).writeText("Hello");
        add(menuBar);

        test(MenuBarTester.class, menuBar).clickItem("Copy link");

        Assertions.assertEquals("Hello", SimulatedClipboard.get().getText());
    }

    private static void add(Component... components) {
        UI.getCurrent().add(components);
    }
}
